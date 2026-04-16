package com.fcar.controller.admin;

import com.fcar.domain.Brand;
import com.fcar.domain.CarAttribute;
import com.fcar.domain.CarColor;
import com.fcar.domain.CarDefinition;
import com.fcar.domain.enums.BodyType;
import com.fcar.domain.CarImage;
import com.fcar.domain.CarModel;
import com.fcar.domain.Segment;
import com.fcar.repository.BrandRepository;
import com.fcar.repository.CarAttributeRepository;
import com.fcar.repository.CarColorRepository;
import com.fcar.repository.CarDefinitionRepository;
import com.fcar.repository.CarImageRepository;
import com.fcar.repository.CarInventoryRepository;
import com.fcar.repository.CarModelRepository;
import com.fcar.repository.SegmentRepository;
import com.fcar.service.CatalogActiveCascadeService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Year;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping
    public String list(
            @org.springframework.web.bind.annotation.RequestParam(value = "success", required = false) String success,
            @org.springframework.web.bind.annotation.RequestParam(value = "brandId", required = false) Long brandId,
            @org.springframework.web.bind.annotation.RequestParam(value = "modelId", required = false) Long modelId,
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
        // Dropdown: chỉ lấy các thực thể còn sử dụng được (active + chưa xóa)
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
                         @RequestParam(value = "galleryImages", required = false) List<MultipartFile> galleryImages,
                         BindingResult bindingResult) {
        if (coverImage != null) form.setCoverImage(coverImage);
        if (galleryImages != null) form.setGalleryImages(galleryImages);
        if (form.getBodyType() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Vui lòng chọn kiểu dáng"));
        }

        CarDefinition def = carDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Car definition not found"));

        // Xóa màu được đánh dấu xóa (chỉ khi còn hơn 1 màu; kiểm tra phía client)
        if (colorIdsToDelete != null && !colorIdsToDelete.isEmpty()) {
            List<CarColor> remaining = carColorRepository.findByCarDefinition(def);
            if (remaining.size() > colorIdsToDelete.size()) {
                for (Long colorId : colorIdsToDelete) {
                    carColorRepository.findById(colorId).ifPresent(cc -> {
                        if (cc.getCarDefinition() != null && cc.getCarDefinition().getId().equals(id)) {
                            carColorRepository.delete(cc);
                        }
                    });
                }
            }
        }

        // Xóa ảnh được đánh dấu xóa (file trên disk + bản ghi DB)
        if (imageIdsToDelete != null && !imageIdsToDelete.isEmpty()) {
            for (Long imageId : imageIdsToDelete) {
                carImageRepository.findById(imageId).ifPresent(img -> {
                    deleteImageFile(img.getImageUrl());
                    carImageRepository.delete(img);
                });
            }
        }

        // Kiểm tra màu mới không trùng với màu đã có (từ bảng car_color) và không trùng nhau
        Set<String> existingNormalized = carColorRepository.findByCarDefinition(def).stream()
                .map(c -> c.getColorValue() != null ? c.getColorValue().trim().toLowerCase() : "")
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (form.getNewColors() != null && !form.getNewColors().isEmpty()) {
            for (String c : form.getNewColors()) {
                String val = c == null ? "" : c.trim();
                if (val.isEmpty()) continue;
                String norm = val.toLowerCase();
                if (existingNormalized.contains(norm)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Bạn vừa thêm màu sắc trùng với màu đã có"));
                }
                existingNormalized.add(norm);
            }
        }

        def.setBodyType(form.getBodyType());
        def.setFuelType(form.getFuelType());
        def.setSeats(form.getSeats());
        def.setSalePrice(form.getSalePrice());
        def.setPromoPrice(form.getPromoPrice());
        def.setHorsepower(form.getHorsepower());
        def.setDescription(form.getDescription());
        carDefinitionRepository.save(def);

        try {
            // Nếu có ảnh đại diện mới: xóa ảnh đại diện cũ (file + DB) rồi lưu ảnh mới
            if (form.getCoverImage() != null && !form.getCoverImage().isEmpty()) {
                carImageRepository.findByCarDefinition(def).stream()
                        .filter(CarImage::isCover)
                        .forEach(img -> {
                            deleteImageFile(img.getImageUrl());
                            carImageRepository.delete(img);
                        });
                storeImages(def, form.getCoverImage(), null);
            }
            // Ảnh gallery mới: chỉ thêm (không xóa gallery cũ trừ khi user bấm Xóa từng ảnh)
            if (form.getGalleryImages() != null && !form.getGalleryImages().isEmpty()) {
                for (MultipartFile file : form.getGalleryImages()) {
                    String url = storeFile(file);
                    if (url == null) continue;
                    CarImage img = new CarImage();
                    img.setCarDefinition(def);
                    img.setImageUrl(url);
                    img.setCover(false);
                    carImageRepository.save(img);
                }
            }
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Không thể lưu ảnh, vui lòng thử lại."));
        }

        // Thêm màu mới vào bảng car_color
        if (form.getNewColors() != null) {
            for (String color : form.getNewColors()) {
                String val = color == null ? "" : color.trim();
                if (val.isEmpty()) continue;
                CarColor carColor = new CarColor();
                carColor.setCarDefinition(def);
                carColor.setColorValue(val);
                carColorRepository.save(carColor);
            }
        }

        // Thông số khác (car_attributes): xóa hết rồi thêm lại theo form
        List<CarAttribute> attrs = carAttributeRepository.findByCarDefinition(def);
        for (CarAttribute a : attrs) {
            carAttributeRepository.delete(a);
        }
        if (form.getExtraAttributeNames() != null && form.getExtraAttributeValues() != null) {
            int size = Math.min(form.getExtraAttributeNames().size(), form.getExtraAttributeValues().size());
            for (int i = 0; i < size; i++) {
                String name = form.getExtraAttributeNames().get(i) == null ? "" : form.getExtraAttributeNames().get(i).trim();
                String value = form.getExtraAttributeValues().get(i) == null ? "" : form.getExtraAttributeValues().get(i).trim();
                if (name.isEmpty() && value.isEmpty()) continue;
                if (name.length() > 50 || value.length() > 100) continue;
                CarAttribute attr = new CarAttribute();
                attr.setCarDefinition(def);
                attr.setName(name);
                attr.setValue(value);
                carAttributeRepository.save(attr);
            }
        }

        return ResponseEntity.ok(Map.of("success", true,
                "redirectUrl", "/admin/cars/definitions?success=updated"));
    }

    /** Xóa file ảnh trên disk theo URL (dạng /uploads/cars/filename). */
    private void deleteImageFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return;
        try {
            String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
            Path base = Paths.get("uploads", "cars").toAbsolutePath().normalize();
            Path filePath = base.resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (Exception ignored) { }
    }

    @PostMapping
    public String create(@ModelAttribute("form") CarDefinitionForm form,
                         @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
                         @RequestParam(value = "galleryImages", required = false) List<MultipartFile> galleryImages,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (coverImage != null) form.setCoverImage(coverImage);
        if (galleryImages != null) form.setGalleryImages(galleryImages);
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
                if (val.isEmpty()) continue;
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
            storeImages(savedDef, form);
        } catch (IOException e) {
            bindingResult.reject("image.upload", "Không thể lưu ảnh, vui lòng thử lại.");
            return list(null, null, null, model);
        }

        // Màu sắc: lưu vào bảng car_color
        if (form.getColors() != null) {
            for (String color : form.getColors()) {
                String val = color == null ? "" : color.trim();
                if (val.isEmpty()) continue;
                CarColor carColor = new CarColor();
                carColor.setCarDefinition(savedDef);
                carColor.setColorValue(val);
                carColorRepository.save(carColor);
            }
        }

        // Thông số khác: từng cặp tên / giá trị (attr_name 50, attr_value 100)
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

    private String storeFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        Path uploadRoot = Paths.get("uploads", "cars").toAbsolutePath().normalize();
        Files.createDirectories(uploadRoot);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String ext = "";
        int dot = originalFilename.lastIndexOf('.');
        if (dot != -1) {
            ext = originalFilename.substring(dot);
        }
        String filename = UUID.randomUUID() + ext;
        Path target = uploadRoot.resolve(filename);
        file.transferTo(target.toFile());
        // Đường dẫn dùng để hiển thị trong web
        return "/uploads/cars/" + filename;
    }

    private void storeImages(CarDefinition savedDef, CarDefinitionForm form) throws IOException {
        // Ảnh đại diện
        String coverUrl = storeFile(form.getCoverImage());
        if (coverUrl != null) {
            CarImage cover = new CarImage();
            cover.setCarDefinition(savedDef);
            cover.setImageUrl(coverUrl);
            cover.setCover(true);
            carImageRepository.save(cover);
        }

        // Bộ ảnh gallery
        if (form.getGalleryImages() != null) {
            for (MultipartFile file : form.getGalleryImages()) {
                String url = storeFile(file);
                if (url == null) {
                    continue;
                }
                CarImage img = new CarImage();
                img.setCarDefinition(savedDef);
                img.setImageUrl(url);
                img.setCover(false);
                carImageRepository.save(img);
            }
        }
    }

    private void storeImages(CarDefinition def, MultipartFile cover, List<MultipartFile> gallery) throws IOException {
        String coverUrl = storeFile(cover);
        if (coverUrl != null) {
            CarImage coverImg = new CarImage();
            coverImg.setCarDefinition(def);
            coverImg.setImageUrl(coverUrl);
            coverImg.setCover(true);
            carImageRepository.save(coverImg);
        }
        if (gallery != null) {
            for (MultipartFile file : gallery) {
                String url = storeFile(file);
                if (url == null) continue;
                CarImage img = new CarImage();
                img.setCarDefinition(def);
                img.setImageUrl(url);
                img.setCover(false);
                carImageRepository.save(img);
            }
        }
    }

    @Data
    public static class CarDefinitionEditForm {

        @NotNull
        private Long brandId;

        @NotNull
        private Long modelId;

        @NotNull
        private Long segmentId;

        @NotNull
        @Min(1900)
        private Integer productionYear;

        private BodyType bodyType;

        private String fuelType;

        @NotNull
        private Integer seats;

        @NotNull
        private BigDecimal salePrice;

        private BigDecimal promoPrice;

        private Integer horsepower;

        private String description;

        private List<String> newColors;
        private List<String> extraAttributeNames;
        private List<String> extraAttributeValues;
        private MultipartFile coverImage;
        private List<MultipartFile> galleryImages;
    }

    @Data
    public static class CarDefinitionForm {

        private Long brandId;

        private Long modelId;

        private Long segmentId;

        private Integer productionYear;

        private BodyType bodyType;

        private String fuelType;

        @NotNull
        private Integer seats;

        @NotNull
        private BigDecimal salePrice;

        private BigDecimal promoPrice;

        private Integer horsepower;

        private String description;

        // Ảnh: 1 ảnh đại diện + nhiều ảnh gallery
        private MultipartFile coverImage;

        private List<MultipartFile> galleryImages;

        // Màu sắc: danh sách màu (mỗi màu là một input color)
        private List<String> colors;

        // Thuộc tính bổ sung: danh sách tên / giá trị
        private List<String> extraAttributeNames;

        private List<String> extraAttributeValues;
    }
}

