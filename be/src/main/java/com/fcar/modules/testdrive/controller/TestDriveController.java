package com.fcar.modules.testdrive.controller;

import com.fcar.modules.branch.entity.Branch;
import com.fcar.modules.catalog.entity.CarDefinition;
import com.fcar.modules.catalog.entity.CarInventory;
import com.fcar.modules.testdrive.entity.TestDriveBooking;
import com.fcar.modules.user.entity.User;
import com.fcar.modules.testdrive.entity.enums.TestDriveStatus;
import com.fcar.modules.catalog.repository.CarInventoryRepository;
import com.fcar.modules.testdrive.repository.TestDriveBookingRepository;
import com.fcar.modules.user.security.FcarUserDetails;
import com.fcar.modules.catalog.service.CarQueryService;
import com.fcar.modules.testdrive.service.ContactTestDriveEmailHtmlService;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Phân công: Bảo — đăng ký lái thử. */
@Controller
@RequiredArgsConstructor
@RequestMapping("/test-drives")
public class TestDriveController {

    private final CarInventoryRepository carInventoryRepository;
    private final CarQueryService carQueryService;
    private final TestDriveBookingRepository testDriveBookingRepository;
    private final ContactTestDriveEmailHtmlService contactTestDriveEmailHtmlService;

    @GetMapping("/register/{definitionId}")
    public String showRegisterForm(@PathVariable("definitionId") Long definitionId,
                                   @RequestParam(value = "forceNew", required = false, defaultValue = "false")
                                   boolean forceNew,
                                   @AuthenticationPrincipal FcarUserDetails principal,
                                   Model model) {
        if (principal == null) {
            return "redirect:/auth/login?redirect=/test-drives/register/" + definitionId;
        }
        User user = principal.getUser();
        CarInventory car = carQueryService.resolveRepresentativeInventory(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("Car not found"));

        List<TestDriveBooking> existingForUserAndCar =
                testDriveBookingRepository.findByUserAndCarInventory(user, car);
        boolean hasExisting = existingForUserAndCar.stream()
                .anyMatch(b -> b.getStatus() != TestDriveStatus.CANCELED);
        if (hasExisting && !forceNew) {
            model.addAttribute("car", car);
            return "testdrives/already";
        }

        List<Branch> branches = distinctBranchesForDefinition(car.getCarDefinition());

        RegisterForm form = new RegisterForm();
        form.setCarId(car.getId());

        model.addAttribute("car", car);
        model.addAttribute("branches", branches);
        model.addAttribute("form", form);
        model.addAttribute("registerDateMin", LocalDate.now().toString());
        return "testdrives/register";
    }

    @PostMapping("/register")
    @Transactional
    public String submitRegister(@ModelAttribute("form") RegisterForm form,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal FcarUserDetails principal,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/auth/login";
        }
        if (bindingResult.hasErrors()) {
            repopulateRegisterView(model, form);
            return "testdrives/register";
        }
        User user = principal.getUser();
        CarInventory car = carInventoryRepository.findById(form.getCarId())
                .orElseThrow(() -> new IllegalArgumentException("Car not found"));

        LocalDateTime testDateTime = LocalDateTime.of(form.getDate(), form.getTime());

        List<TestDriveBooking> existingSameSlot =
                testDriveBookingRepository.findByCarInventoryAndTestDateTime(car, testDateTime);
        boolean conflictWithOther = existingSameSlot.stream()
                .anyMatch(b -> !b.getUser().getId().equals(user.getId()));
        if (conflictWithOther) {
            model.addAttribute("car", car);
            model.addAttribute("branches", distinctBranchesForDefinition(car.getCarDefinition()));
            model.addAttribute("form", form);
            model.addAttribute("registerDateMin", LocalDate.now().toString());
            model.addAttribute("error",
                    "Đã có khách hàng khác đặt lái thử xe này vào thời gian đó, vui lòng chọn ngày/giờ khác.");
            return "testdrives/register";
        }

        TestDriveBooking booking = new TestDriveBooking();
        booking.setUser(user);
        booking.setCarInventory(car);
        booking.setBranch(car.getBranch());
        booking.setTestDateTime(testDateTime);
        booking.setStatus(TestDriveStatus.PENDING);
        testDriveBookingRepository.save(booking);

        contactTestDriveEmailHtmlService.sendTestDriveBookingSubmitted(booking);

        redirectAttributes.addFlashAttribute("flashSuccess",
                "Bạn đã đăng ký lái thử thành công, chúng tôi sẽ liên hệ với bạn ngay.");
        return "redirect:/account/history?tab=testdrives";
    }

    @GetMapping("/cancel/{id}")
    @Transactional
    public String cancel(@PathVariable("id") Long id,
                         @AuthenticationPrincipal FcarUserDetails principal) {
        if (principal == null) {
            return "redirect:/auth/login";
        }
        User user = principal.getUser();
        TestDriveBooking booking = testDriveBookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!booking.getUser().getId().equals(user.getId())) {
            return "redirect:/account/history";
        }
        if (!(booking.getStatus() == TestDriveStatus.PENDING
                || booking.getStatus() == TestDriveStatus.APPROVED)) {
            return "redirect:/account/history";
        }
        booking.setStatus(TestDriveStatus.CANCELED);
        testDriveBookingRepository.save(booking);

        contactTestDriveEmailHtmlService.sendTestDriveBookingCanceledByCustomer(booking);
        return "redirect:/account/history";
    }

    private List<Branch> distinctBranchesForDefinition(CarDefinition def) {
        List<CarInventory> inventories =
                carInventoryRepository.findByCarDefinitionAndDisabledFalseFetchBranch(def);
        Map<Long, Branch> byId = new LinkedHashMap<>();
        for (CarInventory inv : inventories) {
            Branch b = inv.getBranch();
            if (b != null) {
                byId.putIfAbsent(b.getId(), b);
            }
        }
        List<Branch> list = new ArrayList<>(byId.values());
        list.sort(Comparator.comparing(Branch::getName, Comparator.nullsLast(String::compareToIgnoreCase)));
        return list;
    }

    private void repopulateRegisterView(Model model, RegisterForm form) {
        model.addAttribute("registerDateMin", LocalDate.now().toString());
        if (form.getCarId() == null) {
            return;
        }
        carInventoryRepository.findById(form.getCarId()).ifPresent(car -> {
            model.addAttribute("car", car);
            model.addAttribute("branches", distinctBranchesForDefinition(car.getCarDefinition()));
        });
    }

    @Data
    public static class RegisterForm {
        @NotNull
        private Long carId;

        @NotNull
        private LocalDate date;

        @NotNull
        private LocalTime time;
    }
}

