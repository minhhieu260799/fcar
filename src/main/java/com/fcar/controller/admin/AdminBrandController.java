package com.fcar.controller.admin;

import com.fcar.domain.Brand;
import com.fcar.repository.BrandRepository;
import com.fcar.repository.CarInventoryRepository;
import com.fcar.repository.CarModelRepository;
import com.fcar.service.CatalogActiveCascadeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

/** Phân công: Hiếu — quản lý thương hiệu (admin). */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/brands")
public class AdminBrandController {

    private final BrandRepository brandRepository;
    private final CarModelRepository carModelRepository;
    private final CarInventoryRepository carInventoryRepository;
    private final CatalogActiveCascadeService catalogActiveCascadeService;

    @GetMapping
    public String list(Model model) {
        List<Brand> brands = brandRepository.findByDeletedFalseOrderByNameAsc();
        model.addAttribute("brands", brands);
        model.addAttribute("form", new BrandForm());
        return "admin/brands/list";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") BrandForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        String newName = form.getName() == null ? "" : form.getName().trim();
        for (Brand existing : brandRepository.findAll()) {
            String existName = existing.getName() == null ? "" : existing.getName().trim();
            if (!existName.isEmpty() && !newName.isEmpty() && existName.equalsIgnoreCase(newName)) {
                bindingResult.rejectValue("name", "name.exists", "Tên thương hiệu đã tồn tại");
                break;
            }
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("brands", brandRepository.findByDeletedFalseOrderByNameAsc());
            return "admin/brands/list";
        }
        Brand brand = new Brand();
        brand.setName(form.getName());
        brandRepository.save(brand);
        redirectAttributes.addFlashAttribute("success", "Thêm thương hiệu thành công.");
        return "redirect:/admin/brands";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));
        BrandForm form = new BrandForm();
        form.setName(brand.getName());
        model.addAttribute("brand", brand);
        model.addAttribute("form", form);
        return "admin/brands/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") BrandForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));
        if (bindingResult.hasErrors()) {
            model.addAttribute("brands", brandRepository.findByDeletedFalseOrderByNameAsc());
            model.addAttribute("form", new BrandForm());
            model.addAttribute("error", "Vui lòng kiểm tra lại thông tin.");
            return "admin/brands/list";
        }

        boolean inUse = carInventoryRepository
                .existsByCarDefinition_BrandAndDisabledFalseAndQuantityGreaterThan(brand, 0);
        if (inUse) {
            model.addAttribute("brands", brandRepository.findByDeletedFalseOrderByNameAsc());
            model.addAttribute("form", new BrandForm());
            model.addAttribute("error",
                    "Không thể thay đổi thương hiệu vì có xe đang hoạt động");
            return "admin/brands/list";
        }

        String newName = form.getName() == null ? "" : form.getName().trim();
        for (Brand existing : brandRepository.findByDeletedFalseOrderByNameAsc()) {
            if (existing.getId().equals(id)) continue;
            String existName = existing.getName() == null ? "" : existing.getName().trim();
            if (!existName.isEmpty() && !newName.isEmpty() && existName.equalsIgnoreCase(newName)) {
                model.addAttribute("brands", brandRepository.findByDeletedFalseOrderByNameAsc());
                model.addAttribute("form", new BrandForm());
                model.addAttribute("error", "Tên thương hiệu đã tồn tại");
                return "admin/brands/list";
            }
        }

        brand.setName(form.getName());
        brandRepository.save(brand);
        redirectAttributes.addFlashAttribute("success", "Cập nhật thương hiệu thành công.");
        return "redirect:/admin/brands";
    }

    @PostMapping("/{id}/disable")
    public String disable(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        catalogActiveCascadeService.disableBrand(id);
        redirectAttributes.addFlashAttribute("success", "Đã tạm ngưng thương hiệu và các mục liên quan.");
        return "redirect:/admin/brands";
    }

    @PostMapping("/{id}/enable")
    public String enable(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        catalogActiveCascadeService.enableBrand(id);
        redirectAttributes.addFlashAttribute("success", "Đã bật hoạt động thương hiệu và các mục liên quan.");
        return "redirect:/admin/brands";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));
        if (carModelRepository.existsByBrandAndDeletedFalse(brand)) {
            redirectAttributes.addFlashAttribute("error",
                    "Thương hiệu không thể xóa khi đã tồn tại dòng xe trực tiếp.");
            return "redirect:/admin/brands";
        }
        brandRepository.delete(brand);
        redirectAttributes.addFlashAttribute("success", "Đã xóa thương hiệu.");
        return "redirect:/admin/brands";
    }

    @Data
    public static class BrandForm {
        @NotBlank
        private String name;
    }
}

