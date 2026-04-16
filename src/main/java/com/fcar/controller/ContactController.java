package com.fcar.controller;

import com.fcar.domain.Branch;
import com.fcar.domain.CarDefinition;
import com.fcar.domain.CarInventory;
import com.fcar.domain.ContactRequest;
import com.fcar.domain.User;
import com.fcar.domain.enums.ContactStatus;
import com.fcar.repository.CarInventoryRepository;
import com.fcar.repository.ContactRequestRepository;
import com.fcar.security.FcarUserDetails;
import com.fcar.service.CarQueryService;
import com.fcar.service.ContactTestDriveEmailHtmlService;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Phân công: Bảo — liên hệ tư vấn theo xe. */
@Controller
@RequiredArgsConstructor
@RequestMapping("/contacts")
public class ContactController {

    private final CarInventoryRepository carInventoryRepository;
    private final CarQueryService carQueryService;
    private final ContactRequestRepository contactRequestRepository;
    private final ContactTestDriveEmailHtmlService contactTestDriveEmailHtmlService;

    @GetMapping("/request/{definitionId}")
    public String showContactPage(@PathVariable("definitionId") Long definitionId,
                                  @RequestParam(value = "forceNew", required = false, defaultValue = "false")
                                  boolean forceNew,
                                  @AuthenticationPrincipal FcarUserDetails principal,
                                  Model model) {
        if (principal == null) {
            return "redirect:/auth/login?redirect=/contacts/request/" + definitionId;
        }
        User user = principal.getUser();
        CarInventory car = carQueryService.resolveRepresentativeInventory(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("Car not found"));

        Optional<ContactRequest> existing = contactRequestRepository
                .findFirstByUserAndCarInventoryAndStatus(user, car, ContactStatus.PENDING);
        if (existing.isPresent() && !forceNew) {
            model.addAttribute("car", car);
            return "contacts/already";
        }

        CarDefinition def = car.getCarDefinition();
        List<CarInventory> inventoriesForDefinition =
                carInventoryRepository.findByCarDefinitionAndDisabledFalseFetchBranch(def);
        Map<Long, Branch> branchById = new LinkedHashMap<>();
        for (CarInventory inv : inventoriesForDefinition) {
            Branch b = inv.getBranch();
            if (b != null) {
                branchById.putIfAbsent(b.getId(), b);
            }
        }
        List<Branch> branches = new ArrayList<>(branchById.values());
        branches.sort(Comparator.comparing(Branch::getName, Comparator.nullsLast(String::compareToIgnoreCase)));

        model.addAttribute("car", car);
        model.addAttribute("branches", branches);
        return "contacts/request";
    }

    @PostMapping("/request/{definitionId}")
    @Transactional
    public String submitContact(@PathVariable("definitionId") Long definitionId,
                                @AuthenticationPrincipal FcarUserDetails principal,
                                RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/auth/login?redirect=/contacts/request/" + definitionId;
        }
        User user = principal.getUser();
        CarInventory car = carQueryService.resolveRepresentativeInventory(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("Car not found"));

        ContactRequest contact = new ContactRequest();
        contact.setUser(user);
        contact.setCarInventory(car);
        contact.setStatus(ContactStatus.PENDING);
        contactRequestRepository.save(contact);

        contactTestDriveEmailHtmlService.sendContactRequestSubmitted(contact);

        redirectAttributes.addFlashAttribute("flashSuccess",
                "Bạn đã đăng ký liên hệ thành công, chúng tôi sẽ liên hệ với bạn ngay.");
        return "redirect:/account/history?tab=contacts";
    }

    @GetMapping("/cancel/{id}")
    @Transactional
    public String cancel(@PathVariable("id") Long id,
                         @AuthenticationPrincipal FcarUserDetails principal) {
        if (principal == null) {
            return "redirect:/auth/login";
        }
        User user = principal.getUser();
        ContactRequest contact = contactRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found"));
        if (!contact.getUser().getId().equals(user.getId())) {
            return "redirect:/account/history";
        }
        if (contact.getStatus() != ContactStatus.PENDING) {
            return "redirect:/account/history";
        }
        contact.setStatus(ContactStatus.CANCELED);
        contactRequestRepository.save(contact);

        contactTestDriveEmailHtmlService.sendContactRequestCanceledByCustomer(contact);
        return "redirect:/account/history";
    }
}

