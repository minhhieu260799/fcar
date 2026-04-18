package com.fcar.modules.catalog.controller.admin;

import com.fcar.modules.catalog.dto.CarDefinitionEditForm;
import com.fcar.modules.catalog.dto.CarDefinitionForm;
import com.fcar.modules.catalog.entity.Brand;
import com.fcar.modules.catalog.entity.CarAttribute;
import com.fcar.modules.catalog.entity.CarColor;
import com.fcar.modules.catalog.entity.CarDefinition;
import com.fcar.modules.catalog.entity.enums.BodyType;
import com.fcar.modules.catalog.entity.CarModel;
import com.fcar.modules.catalog.entity.Segment;
import com.fcar.modules.catalog.repository.BrandRepository;
import com.fcar.modules.catalog.repository.CarAttributeRepository;
import com.fcar.modules.catalog.repository.CarColorRepository;
import com.fcar.modules.catalog.repository.CarDefinitionRepository;
import com.fcar.modules.catalog.repository.CarImageRepository;
import com.fcar.modules.catalog.repository.CarInventoryRepository;
import com.fcar.modules.catalog.repository.CarModelRepository;
import com.fcar.modules.catalog.repository.SegmentRepository;
import com.fcar.modules.catalog.service.CatalogActiveCascadeService;
import com.fcar.modules.catalog.service.admin.CarDefinitionAdminService;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Phân công: Hiếu — quản lý định nghĩa xe / biến thể (admin). */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/cars/definitions")
public class AdminCarDefinitionController {

    private final BrandRepository brandRepository;
    private final CarModelRepository carModelRepository;
    private final SegmentRepository segmentRepository;
    private final CarDefinitionRepository carDefinitionRepository;
    private final CarImageRepository carImageRepository;
    private final CarAttributeRepository carAttributeRepository;
    private final CarColorRepository carColorRepository;
    private final CarInventoryRepository carInventoryRepository;
    private final CatalogActiveCascadeService catalogActiveCascadeService;
    private final CarDefinitionAdminService carDefinitionAdminService;

    @GetMapping
    public String list(
            @RequestParam(value = "success", required = false) String success,
            @RequestParam(value = "brandId", required = false) Long brandId,
            @RequestParam(value = "modelId", required = false) Long modelId,
            Model model) {
        List<CarDefinition> definitions = carDefinitionRepository.findByDeletedFalseWithRelations();
        if (brandId != null) {
            definitions = definitions.stream()
                    .filter(d -> d.getBrand() != null && Objects.equals(d.getBrand().getId(), brandId))
                    .toList();
        }
        if (modelId != null) {
            definitions = definitions.stream()
                    .filter(d -> d.getModel() != null && Objects.equals(d.getModel().getId(), modelId))
                    .toList();
        }
        model.addAttribute("definitions", definitions);
        model.addAttribute("brands", brandRepository.findByDeletedFalseAndActiveTrueOrderByNameAsc());
        model.addAttribute("models", carModelRepository.findUsableModelsForSelection());
        List<Segment> segments = segmentRepository.findUsableSegmentsForSelection();
        Long otherSegmentId = segments.stream()
                .filter(s -> s.getName() != null && s.getName().equalsIgnoreCase("OTHER"))
                .map(Segment::getId)
                .findFirst()
                .orElse(null);
        model.addAttribute("segments", segments);
        model.addAttribute("otherSegmentId", otherSegmentId);
        model.addAttribute("bodyTypes", BodyType.values());
        model.addAttribute("brandId", brandId);
        model.addAttribute("modelId", modelId);
        int currentYear = Year.now().getValue();
        model.addAttribute("productionYears",
                IntStream.rangeClosed(currentYear - 50, currentYear).boxed()
                        .sorted(Comparator.reverseOrder()).toList());
        model.addAttribute("form", new CarDefinitionForm());
        if (!model.containsAttribute("success") && success != null) {
            String message = switch (success) {
                case "created" -> "Thêm mẫu xe thành công.";
                case "updated" -> "Đã cập nhật mẫu xe thành công.";
                default -> null;
            };
            if (message != null) {
                model.addAttribute("success", message);
            }
        }
        return "admin/cars/definitions";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        CarDefinition def = carDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Car definition not found"));

        CarDefinitionEditForm form = new CarDefinitionEditForm();
        form.setBrandId(def.getBrand().getId());
        form.setModelId(def.getModel().getId());
        form.setSegmentId(def.getSegment() != null ? def.getSegment().getId() : null);
        form.setProductionYear(def.getProductionYear());
        form.setFuelType(def.getFuelType());
        form.setSeats(def.getSeats());
        form.setHorsepower(def.getHorsepower());
        form.setDescription(def.getDescription());

        model.addAttribute("definition", def);
        model.addAttribute("form", form);
        model.addAttribute("brands", brandRepository.findByDeletedFalse());
        model.addAttribute("models", carModelRepository.findByDeletedFalseWithBrand());
        model.addAttribute("segments", segmentRepository.findByDeletedFalse());
        return "admin/cars/definitions-edit";
    }

    @GetMapping(value = "/{id}/edit-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> editData(@PathVariable Long id) {
        CarDefinition def = carDefinitionRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new IllegalArgumentException("Car definition not found"));
        List<Map<String, Object>> existingColorsWithId = carColorRepository.findByCarDefinition(def).stream()
                .filter(c -> c.getColorValue() != null && !c.getColorValue().trim().isEmpty())
                .map(c -> Map.<String, Object>of("id", c.getId(), "colorValue", c.getColorValue()))
                .toList();
        List<CarAttribute> attrs = carAttributeRepository.findByCarDefinition(def);
        List<Map<String, String>> extraAttributes = new ArrayList<>();
        for (CarAttribute a : attrs) {
            Map<String, String> pair = new LinkedHashMap<>();
            pair.put("name", a.getName());
            pair.put("value", a.getValue());
            extraAttributes.add(pair);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", def.getId());
        data.put("brandId", def.getBrand().getId());
        data.put("brandName", def.getBrand().getName());
        data.put("modelId", def.getModel().getId());
        data.put("modelName", def.getModel().getName());
        data.put("segmentId", def.getSegment() != null ? def.getSegment().getId() : null);
        data.put("segmentName", def.getSegment() != null ? def.getSegment().getName() : "");
        data.put("productionYear", def.getProductionYear());
        data.put("fuelType", def.getFuelType());
        data.put("seats", def.getSeats());
        data.put("horsepower", def.getHorsepower());
        data.put("description", def.getDescription());
        data.put("salePrice", def.getSalePrice());
        data.put("promoPrice", def.getPromoPrice());
        data.put("bodyType", def.getBodyType() != null ? def.getBodyType().name() : "");
        data.put("bodyTypes", Arrays.stream(BodyType.values())
                .map(bt -> Map.<String, String>of("name", bt.name(), "displayName", bt.getDisplayName()))
                .toList());
        List<Map<String, Object>> existingImages = carImageRepository.findByCarDefinition(def).stream()
                .map(img -> Map.<String, Object>of("id", img.getId(), "imageUrl", img.getImageUrl() != null ? img.getImageUrl() : "", "cover", img.isCover()))
                .toList();
        data.put("existingImages", existingImages);
        data.put("existingColors", existingColorsWithId);
        data.put("extraAttributes", extraAttributes);
        return data;
    }

    @PostMapping("/{id}/disable")
    public String disable(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        catalogActiveCascadeService.disableCarDefinition(id);
        redirectAttributes.addFlashAttribute("success", "Đã tạm ngưng mẫu xe và các dòng tồn kho liên quan.");
        return "redirect:/admin/cars/definitions";
    }

    @PostMapping("/{id}/enable")
    public String enable(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        String err = catalogActiveCascadeService.enableCarDefinition(id);
        if (err != null) {
            model.addAttribute("error", err);
            return list(null, null, null, model);
        }
        redirectAttributes.addFlashAttribute("success", "Đã bật hoạt động mẫu xe và các dòng tồn kho liên quan.");
        return "redirect:/admin/cars/definitions";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        CarDefinition def = carDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Car definition not found"));
        if (carInventoryRepository.existsByCarDefinition(def)) {
            redirectAttributes.addFlashAttribute("error",
                    "Mẫu xe không thể xóa khi đã tồn tại bản ghi tồn kho trực tiếp.");
            return "redirect:/admin/cars/definitions";
        }
        carColorRepository.findByCarDefinition(def).forEach(carColorRepository::delete);
        carAttributeRepository.findByCarDefinition(def).forEach(carAttributeRepository::delete);
        carImageRepository.findByCarDefinition(def).forEach(carImageRepository::delete);
        carDefinitionRepository.delete(def);
        redirectAttributes.addFlashAttribute("success", "Đã xóa mẫu xe.");
        return "redirect:/admin/cars/definitions";
    }

    @PostMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                         @ModelAttribute("form") CarDefinitionEditForm form,
                         @RequestParam(value = "imageIdsToDelete", required = false) List<Long> imageIdsToDelete,
                         @RequestParam(value = "colorIdsToDelete", required = false) List<Long> colorIdsToDelete,
                         @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
                         @RequestParam(value = "galleryImages", required = false) List<MultipartFile> galleryImages) {
        if (coverImage != null) {
            form.setCoverImage(coverImage);
        }
        if (galleryImages != null) {
            form.setGalleryImages(galleryImages);
        }
        return carDefinitionAdminService.updateDefinitionJson(id, form, imageIdsToDelete, colorIdsToDelete);
    }

    @PostMapping
    public String create(@ModelAttribute("form") CarDefinitionForm form,
                         @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
                         @RequestParam(value = "galleryImages", required = false) List<MultipartFile> galleryImages,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (coverImage != null) {
            form.setCoverImage(coverImage);
        }
        if (galleryImages != null) {
            form.setGalleryImages(galleryImages);
        }
        int currentYear = Year.now().getValue();
        if (form.getBrandId() == null) {
            bindingResult.rejectValue("brandId", "brand.required", "Thương hiệu không được để trống");
        }
        if (form.getModelId() == null) {
            bindingResult.rejectValue("modelId", "model.required", "Dòng xe không được để trống");
        }
        if (form.getSegmentId() == null) {
            bindingResult.rejectValue("segmentId", "segment.required", "Phân khúc không được để trống");
        }
        if (form.getBodyType() == null) {
            bindingResult.rejectValue("bodyType", "bodyType.required", "Kiểu dáng không được để trống");
        }
        if (form.getProductionYear() == null || form.getProductionYear() > currentYear) {
            bindingResult.rejectValue("productionYear", "year.invalid", "Năm sản xuất phải <= năm hiện tại");
        }
        if (form.getSalePrice() == null || form.getSalePrice().compareTo(BigDecimal.ZERO) <= 0) {
            bindingResult.rejectValue("salePrice", "salePrice.invalid", "Giá bán phải > 0");
        }
        if (form.getColors() != null) {
            Set<String> seen = new HashSet<>();
            for (String c : form.getColors()) {
                String val = c == null ? "" : c.trim();
                if (val.isEmpty()) {
                    continue;
                }
                String norm = val.toLowerCase();
                if (seen.contains(norm)) {
                    bindingResult.reject("colors.duplicate", "Màu sắc không được trùng");
                    model.addAttribute("error", "Màu sắc không được trùng");
                    return list(null, null, null, model);
                }
                seen.add(norm);
            }
        }
        if (bindingResult.hasErrors()) {
            return list(null, null, null, model);
        }

        Brand brand = brandRepository.findById(form.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));
        CarModel modelEntity = carModelRepository.findById(form.getModelId())
                .orElseThrow(() -> new IllegalArgumentException("Model not found"));
        Segment segment = segmentRepository.findById(form.getSegmentId())
                .orElseThrow(() -> new IllegalArgumentException("Segment not found"));

        if (carDefinitionRepository
                .findByBrandAndModelAndSegmentAndProductionYear(brand, modelEntity, segment, form.getProductionYear())
                .isPresent()) {
            bindingResult.reject("duplicate",
                    "Xe này đã tồn tại, vui lòng vào \"Nhập xe\" để thêm số lượng");
            model.addAttribute("error", "Mẫu xe đã tồn tại (trùng Thương hiệu, Dòng xe, Phân khúc, Năm sản xuất). Vui lòng vào \"Nhập xe\" để thêm số lượng.");
            return list(null, null, null, model);
        }

        CarDefinition def = new CarDefinition();
        def.setBrand(brand);
        def.setModel(modelEntity);
        def.setSegment(segment);
        def.setBodyType(form.getBodyType());
        def.setProductionYear(form.getProductionYear());
        def.setFuelType(form.getFuelType());
        def.setSeats(form.getSeats());
        def.setSalePrice(form.getSalePrice());
        def.setPromoPrice(form.getPromoPrice());
        def.setHorsepower(form.getHorsepower());
        def.setDescription(form.getDescription());
        def = carDefinitionRepository.save(def);
        final CarDefinition savedDef = def;

        try {
            carDefinitionAdminService.storeImagesForCreate(savedDef, form);
        } catch (IOException e) {
            bindingResult.reject("image.upload", "Không thể lưu ảnh, vui lòng thử lại.");
            return list(null, null, null, model);
        }

        if (form.getColors() != null) {
            for (String color : form.getColors()) {
                String val = color == null ? "" : color.trim();
                if (val.isEmpty()) {
                    continue;
                }
                CarColor carColor = new CarColor();
                carColor.setCarDefinition(savedDef);
                carColor.setColorValue(val);
                carColorRepository.save(carColor);
            }
        }

        if (form.getExtraAttributeNames() != null && form.getExtraAttributeValues() != null) {
            int size = Math.min(form.getExtraAttributeNames().size(),
                    form.getExtraAttributeValues().size());
            for (int i = 0; i < size; i++) {
                String name = form.getExtraAttributeNames().get(i);
                String value = form.getExtraAttributeValues().get(i);
                name = name == null ? "" : name.trim();
                value = value == null ? "" : value.trim();
                if (name.isEmpty() && value.isEmpty()) {
                    continue;
                }
                if (name.length() > 50 || value.length() > 100) {
                    continue;
                }
                CarAttribute attr = new CarAttribute();
                attr.setCarDefinition(savedDef);
                attr.setName(name);
                attr.setValue(value);
                carAttributeRepository.save(attr);
            }
        }

        redirectAttributes.addFlashAttribute("success", "Thêm mẫu xe thành công.");
        return "redirect:/admin/cars/definitions";
    }
}
