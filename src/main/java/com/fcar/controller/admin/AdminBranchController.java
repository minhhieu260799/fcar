package com.fcar.controller.admin;

import com.fcar.domain.Branch;
import com.fcar.domain.enums.BranchStatus;
import com.fcar.repository.BranchRepository;
import com.fcar.repository.CarInventoryRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Phân công: Hiếu — quản lý chi nhánh (admin). */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/branches")
public class AdminBranchController {

    private final BranchRepository branchRepository;
    private final CarInventoryRepository carInventoryRepository;

    @GetMapping
    public String list(Model model) {
        List<Branch> branches = branchRepository.findByDeletedFalseOrderByNameAsc();
        model.addAttribute("branches", branches);
        model.addAttribute("form", new BranchForm());
        return "admin/branches/list";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") BranchForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        String newName = form.getName() == null ? "" : form.getName().trim();
        String newAddress = form.getAddress() == null ? "" : form.getAddress().trim();
        String newPhone = form.getPhone() == null ? "" : form.getPhone().trim();

        for (Branch existing : branchRepository.findAll()) {
            String existName = existing.getName() == null ? "" : existing.getName().trim();
            String existAddress = existing.getAddress() == null ? "" : existing.getAddress().trim();
            String existPhone = existing.getPhone() == null ? "" : existing.getPhone().trim();

            if (!existName.isEmpty() && !newName.isEmpty()
                    && existName.equalsIgnoreCase(newName)) {
                bindingResult.rejectValue("name", "name.exists", "Tên chi nhánh đã tồn tại");
            }
            if (!existAddress.isEmpty() && !newAddress.isEmpty()
                    && existAddress.equalsIgnoreCase(newAddress)) {
                bindingResult.rejectValue("address", "address.exists", "Địa chỉ chi nhánh đã tồn tại");
            }
            if (!existPhone.isEmpty() && !newPhone.isEmpty()
                    && existPhone.equals(newPhone)) {
                bindingResult.rejectValue("phone", "phone.exists", "SĐT chi nhánh đã tồn tại");
            }
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("branches", branchRepository.findByDeletedFalseOrderByNameAsc());
            return "admin/branches/list";
        }
        Branch branch = new Branch();
        branch.setStatus(BranchStatus.ACTIVE);
        branch.setName(form.getName());
        branch.setAddress(form.getAddress());
        branch.setPhone(form.getPhone());
        branchRepository.save(branch);
        redirectAttributes.addFlashAttribute("success", "Thêm chi nhánh thành công.");
        return "redirect:/admin/branches";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
        BranchForm form = new BranchForm();
        form.setName(branch.getName());
        form.setAddress(branch.getAddress());
        form.setPhone(branch.getPhone());
        model.addAttribute("branch", branch);
        model.addAttribute("form", form);
        return "admin/branches/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") BranchForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
        String newName = form.getName() == null ? "" : form.getName().trim();
        String newAddress = form.getAddress() == null ? "" : form.getAddress().trim();
        String newPhone = form.getPhone() == null ? "" : form.getPhone().trim();

        for (Branch existing : branchRepository.findAll()) {
            if (existing.getId().equals(id)) {
                continue;
            }
            String existName = existing.getName() == null ? "" : existing.getName().trim();
            String existAddress = existing.getAddress() == null ? "" : existing.getAddress().trim();
            String existPhone = existing.getPhone() == null ? "" : existing.getPhone().trim();

            if (!existName.isEmpty() && !newName.isEmpty()
                    && existName.equalsIgnoreCase(newName)) {
                bindingResult.rejectValue("name", "name.exists", "Tên chi nhánh đã tồn tại");
            }
            if (!existAddress.isEmpty() && !newAddress.isEmpty()
                    && existAddress.equalsIgnoreCase(newAddress)) {
                bindingResult.rejectValue("address", "address.exists", "Địa chỉ chi nhánh đã tồn tại");
            }
            if (!existPhone.isEmpty() && !newPhone.isEmpty()
                    && existPhone.equals(newPhone)) {
                bindingResult.rejectValue("phone", "phone.exists", "SĐT chi nhánh đã tồn tại");
            }
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("branches", branchRepository.findByDeletedFalseOrderByNameAsc());
            model.addAttribute("form", new BranchForm());
            model.addAttribute("error", "Vui lòng kiểm tra lại thông tin chi nhánh (SĐT phải gồm 10 chữ số).");
            return "admin/branches/list";
        }
        boolean inUse = carInventoryRepository
                .existsByBranchAndDisabledFalseAndQuantityGreaterThan(branch, 0);
        if (inUse) {
            model.addAttribute("branches", branchRepository.findByDeletedFalseOrderByNameAsc());
            model.addAttribute("form", new BranchForm());
            model.addAttribute("error",
                    "Không thể sửa chi nhánh đang được dùng bởi xe còn hoạt động");
            return "admin/branches/list";
        }
        branch.setName(form.getName());
        branch.setAddress(form.getAddress());
        branch.setPhone(form.getPhone());
        branchRepository.save(branch);
        redirectAttributes.addFlashAttribute("success", "Cập nhật chi nhánh thành công.");
        return "redirect:/admin/branches";
    }

    @PostMapping("/{id}/disable")
    public String disable(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
        boolean inUse = carInventoryRepository
                .existsByBranchAndDisabledFalseAndQuantityGreaterThan(branch, 0);
        if (inUse) {
            model.addAttribute("branches", branchRepository.findByDeletedFalseOrderByNameAsc());
            model.addAttribute("form", new BranchForm());
            model.addAttribute("error",
                    "Không thể vô hiệu hóa chi nhánh đang được dùng bởi xe còn hoạt động");
            return "admin/branches/list";
        }
        branch.setStatus(BranchStatus.SUSPENDED);
        branchRepository.save(branch);
        redirectAttributes.addFlashAttribute("success", "Đã tạm ngưng chi nhánh.");
        return "redirect:/admin/branches";
    }

    @PostMapping("/{id}/enable")
    public String enable(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
        branch.setStatus(BranchStatus.ACTIVE);
        branchRepository.save(branch);
        redirectAttributes.addFlashAttribute("success", "Đã bật hoạt động chi nhánh.");
        return "redirect:/admin/branches";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
        boolean inUse = carInventoryRepository
                .existsByBranchAndDisabledFalseAndQuantityGreaterThan(branch, 0);
        if (inUse) {
            // Chi nhánh đang được dùng bởi xe còn hoạt động -> chỉ soft delete
            model.addAttribute("branches", branchRepository.findByDeletedFalseOrderByNameAsc());
            model.addAttribute("form", new BranchForm());
            model.addAttribute("error",
                    "Không thể xóa chi nhánh đang được dùng bởi xe còn hoạt động");
            return "admin/branches/list";
        }
        // Không còn xe đang dùng chi nhánh -> hard delete
        branchRepository.delete(branch);
        redirectAttributes.addFlashAttribute("success", "Đã xóa chi nhánh.");
        return "redirect:/admin/branches";
    }

    @Data
    public static class BranchForm {
        @NotBlank
        private String name;

        @NotBlank
        private String address;

        @NotBlank
        @Pattern(regexp = "\\d{10}", message = "SĐT phải gồm 10 chữ số")
        private String phone;
    }
}

