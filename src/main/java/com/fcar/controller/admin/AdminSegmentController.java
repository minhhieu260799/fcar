package com.fcar.controller.admin;

import com.fcar.domain.Brand;
import com.fcar.domain.CarModel;
import com.fcar.domain.Segment;
import com.fcar.repository.BrandRepository;
import com.fcar.repository.CarDefinitionRepository;
import com.fcar.repository.CarModelRepository;
import com.fcar.repository.SegmentRepository;
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

/** Phân công: Hiếu — quản lý phân khúc xe (admin). */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/segments")
public class AdminSegmentController {

    private final SegmentRepository segmentRepository;
    private final CarDefinitionRepository carDefinitionRepository;
    private final BrandRepository brandRepository;
    private final CarModelRepository carModelRepository;
    private final CatalogActiveCascadeService catalogActiveCascadeService;

    @GetMapping
    public String list(
            @org.springframework.web.bind.annotation.RequestParam(value = "brandId", required = false) Long brandId,
            @org.springframework.web.bind.annotation.RequestParam(value = "modelId", required = false) Long modelId,
            Model model) {
        // Bộ lọc: hiển thị cả thương hiệu/dòng xe đang tạm ngưng (trừ khi đã xóa)
        List<Brand> brands = brandRepository.findByDeletedFalseOrderByNameAsc();
        List<CarModel> models = carModelRepository.findByDeletedFalseWithBrandOrderByName();
        List<Segment> segments = segmentRepository.findByDeletedFalseWithModelAndBrandOrderByNameAsc();

        if (brandId != null) {
            segments = segments.stream()
                    .filter(s -> s.getModel() != null
                            && s.getModel().getBrand() != null
                            && brandId.equals(s.getModel().getBrand().getId()))
                    .toList();
        }
        if (modelId != null) {
            segments = segments.stream()
                    .filter(s -> s.getModel() != null && modelId.equals(s.getModel().getId()))
                    .toList();
        }

        model.addAttribute("brands", brands);
        model.addAttribute("models", models);
        model.addAttribute("brandId", brandId);
        model.addAttribute("modelId", modelId);
        model.addAttribute("segments", segments);
        model.addAttribute("form", new SegmentForm());
        return "admin/segments/list";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") SegmentForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (form.getModelId() == null) {
            bindingResult.rejectValue("modelId", "model.required", "Vui lòng chọn dòng xe");
        }
        CarModel carModel = form.getModelId() != null
                ? carModelRepository.findById(form.getModelId()).orElse(null)
                : null;
        if (carModel != null && segmentRepository.existsByModelAndName(carModel, form.getName())) {
            bindingResult.rejectValue("name", "name.exists", "Phân khúc đã tồn tại trong dòng xe này");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("brands", brandRepository.findByDeletedFalseAndActiveTrueOrderByNameAsc());
            model.addAttribute("models", carModelRepository.findUsableModelsForSelection());
            model.addAttribute("segments", segmentRepository.findByDeletedFalseWithModelAndBrandOrderByNameAsc());
            model.addAttribute("form", form);
            model.addAttribute("brandId", form.getBrandId());
            model.addAttribute("modelId", form.getModelId());
            return "admin/segments/list";
        }
        if (carModel == null) {
            throw new IllegalArgumentException("Model not found");
        }
        Segment segment = new Segment();
        segment.setName(form.getName());
        segment.setModel(carModel);
        // Nếu dòng xe đang tạm ngưng, phân khúc mới cũng mặc định tạm ngưng
        segment.setActive(carModel.isActive());
        segmentRepository.save(segment);
        redirectAttributes.addFlashAttribute("success", "Thêm phân khúc thành công.");
        return "redirect:/admin/segments";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Segment segment = segmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Segment not found"));
        SegmentForm form = new SegmentForm();
        form.setName(segment.getName());
        model.addAttribute("segment", segment);
        model.addAttribute("form", form);
        model.addAttribute("brands", brandRepository.findByDeletedFalseAndActiveTrueOrderByNameAsc());
        model.addAttribute("models", carModelRepository.findUsableModelsForSelection());
        return "admin/segments/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") SegmentForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Segment segment = segmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Segment not found"));
        if (segment.getModel() != null && segmentRepository.existsByModelAndNameAndIdNot(segment.getModel(), form.getName(), id)) {
            bindingResult.rejectValue("name", "name.exists", "Phân khúc đã tồn tại trong dòng xe này");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("segment", segment);
            model.addAttribute("brands", brandRepository.findByDeletedFalseAndActiveTrueOrderByNameAsc());
            model.addAttribute("models", carModelRepository.findUsableModelsForSelection());
            return "admin/segments/edit";
        }
        segment.setName(form.getName());
        segmentRepository.save(segment);
        redirectAttributes.addFlashAttribute("success", "Cập nhật phân khúc thành công.");
        return "redirect:/admin/segments";
    }

    @PostMapping("/{id}/disable")
    public String disable(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        catalogActiveCascadeService.disableSegment(id);
        redirectAttributes.addFlashAttribute("success", "Đã tạm ngưng phân khúc và các mục liên quan.");
        return "redirect:/admin/segments";
    }

    @PostMapping("/{id}/enable")
    public String enable(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        String err = catalogActiveCascadeService.enableSegment(id);
        if (err != null) {
            model.addAttribute("brands", brandRepository.findByDeletedFalseOrderByNameAsc());
            model.addAttribute("models", carModelRepository.findByDeletedFalseWithBrandOrderByName());
            model.addAttribute("segments", segmentRepository.findByDeletedFalseWithModelAndBrandOrderByNameAsc());
            model.addAttribute("form", new SegmentForm());
            model.addAttribute("error", err);
            return "admin/segments/list";
        }
        redirectAttributes.addFlashAttribute("success", "Đã bật hoạt động phân khúc và các mục liên quan.");
        return "redirect:/admin/segments";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Segment segment = segmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Segment not found"));
        if (carDefinitionRepository.existsBySegmentAndDeletedFalse(segment)) {
            redirectAttributes.addFlashAttribute("error",
                    "Phân khúc không thể xóa khi đã tồn tại mẫu xe trực tiếp.");
            return "redirect:/admin/segments";
        }
        segmentRepository.delete(segment);
        redirectAttributes.addFlashAttribute("success", "Đã xóa phân khúc.");
        return "redirect:/admin/segments";
    }

    @Data
    public static class SegmentForm {
        @NotBlank
        private String name;

        private Long brandId;

        private Long modelId;
    }
}

