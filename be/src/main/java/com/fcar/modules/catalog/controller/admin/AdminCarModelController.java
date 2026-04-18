package com.fcar.modules.catalog.controller.admin;

import com.fcar.modules.catalog.entity.Brand;
import com.fcar.modules.catalog.entity.CarModel;
import com.fcar.modules.catalog.repository.BrandRepository;
import com.fcar.modules.catalog.repository.CarDefinitionRepository;
import com.fcar.modules.catalog.repository.CarInventoryRepository;
import com.fcar.modules.catalog.repository.CarModelRepository;
import com.fcar.modules.catalog.repository.SegmentRepository;
import com.fcar.modules.catalog.service.CatalogActiveCascadeService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Phân công: Hiếu — quản lý dòng xe (admin). */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/models")
public class AdminCarModelController {

    private final CarModelRepository carModelRepository;
    private final BrandRepository brandRepository;
    private final SegmentRepository segmentRepository;
    private final CarDefinitionRepository carDefinitionRepository;
    private final CarInventoryRepository carInventoryRepository;
    private final CatalogActiveCascadeService catalogActiveCascadeService;

    @GetMapping
    public String list(@RequestParam(value = "brandId", required = false) Long brandId, Model model) {
        // Bộ lọc: hiển thị cả thương hiệu đang tạm ngưng (trừ khi đã xóa)
        List<Brand> brands = brandRepository.findByDeletedFalseOrderByNameAsc();
        List<CarModel> models = carModelRepository.findByDeletedFalseWithBrandOrderByName();
        if (brandId != null) {
            models = models.stream().filter(m -> m.getBrand().getId().equals(brandId)).toList();
        }
        model.addAttribute("brands", brands);
        model.addAttribute("models", models);
        model.addAttribute("brandId", brandId);
        model.addAttribute("form", new CarModelForm());
        return "admin/models/list";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CarModelForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Brand brand = brandRepository.findById(form.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));
        if (carModelRepository.existsByBrandAndName(brand, form.getName())) {
            bindingResult.rejectValue("name", "name.exists", "Dòng xe đã tồn tại");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("brands", brandRepository.findByDeletedFalse());
            model.addAttribute("models", carModelRepository.findByDeletedFalseWithBrandOrderByName());
            model.addAttribute("form", form);
            return "admin/models/list";
        }
        CarModel modelEntity = new CarModel();
        modelEntity.setBrand(brand);
        modelEntity.setName(form.getName());
        // Nếu thương hiệu đang tạm ngưng, dòng xe mới cũng mặc định tạm ngưng
        modelEntity.setActive(brand.isActive());
        carModelRepository.save(modelEntity);
        redirectAttributes.addFlashAttribute("success", "Thêm dòng xe thành công.");
        return "redirect:/admin/models";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        CarModel carModel = carModelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Model not found"));
        CarModelForm form = new CarModelForm();
        form.setBrandId(carModel.getBrand().getId());
        form.setName(carModel.getName());
        model.addAttribute("carModel", carModel);
        model.addAttribute("brands", brandRepository.findByDeletedFalse());
        model.addAttribute("form", form);
        return "admin/models/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") CarModelForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        CarModel carModel = carModelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Model not found"));
        // Không cho phép đổi thương hiệu của dòng xe đã tồn tại, chỉ thay đổi tên
        Brand brand = carModel.getBrand();
        if (carModelRepository.existsByBrandAndNameAndIdNot(brand, form.getName(), id)) {
            bindingResult.rejectValue("name", "name.exists", "Dòng xe đã tồn tại");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("carModel", carModel);
            model.addAttribute("brands", brandRepository.findByDeletedFalse());
            return "admin/models/edit";
        }
        boolean inUse = carInventoryRepository
                .existsByCarDefinition_ModelAndDisabledFalseAndQuantityGreaterThan(carModel, 0);
        if (inUse) {
            model.addAttribute("brands", brandRepository.findByDeletedFalse());
            model.addAttribute("models", carModelRepository.findByDeletedFalseWithBrandOrderByName());
            model.addAttribute("form", new CarModelForm());
            model.addAttribute("error",
                    "Không thể sửa dòng xe đang được dùng bởi xe còn hoạt động");
            return "admin/models/list";
        }
        carModel.setName(form.getName());
        carModelRepository.save(carModel);
        redirectAttributes.addFlashAttribute("success", "Cập nhật dòng xe thành công.");
        return "redirect:/admin/models";
    }

    @PostMapping("/{id}/disable")
    public String disable(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        catalogActiveCascadeService.disableCarModel(id);
        redirectAttributes.addFlashAttribute("success", "Đã tạm ngưng dòng xe và các mục liên quan.");
        return "redirect:/admin/models";
    }

    @PostMapping("/{id}/enable")
    public String enable(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        String err = catalogActiveCascadeService.enableCarModel(id);
        if (err != null) {
            model.addAttribute("brands", brandRepository.findByDeletedFalse());
            model.addAttribute("models", carModelRepository.findByDeletedFalseWithBrandOrderByName());
            model.addAttribute("form", new CarModelForm());
            model.addAttribute("error", err);
            return "admin/models/list";
        }
        redirectAttributes.addFlashAttribute("success", "Đã bật hoạt động dòng xe và các mục liên quan.");
        return "redirect:/admin/models";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        CarModel carModel = carModelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Model not found"));
        if (segmentRepository.existsByModelAndDeletedFalse(carModel)
                || carDefinitionRepository.existsByModelAndDeletedFalse(carModel)) {
            redirectAttributes.addFlashAttribute("error",
                    "Dòng xe không thể xóa khi đã tồn tại con trực tiếp (phân khúc hoặc mẫu xe).");
            return "redirect:/admin/models";
        }
        carModelRepository.delete(carModel);
        redirectAttributes.addFlashAttribute("success", "Đã xóa dòng xe.");
        return "redirect:/admin/models";
    }

    @Data
    public static class CarModelForm {
        @NotBlank
        private String name;

        private Long brandId;
    }
}

