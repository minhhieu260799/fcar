package com.fcar.modules.catalog.service.admin;

import com.fcar.modules.catalog.dto.CarDefinitionEditForm;
import com.fcar.modules.catalog.dto.CarDefinitionForm;
import com.fcar.modules.catalog.entity.CarAttribute;
import com.fcar.modules.catalog.entity.CarColor;
import com.fcar.modules.catalog.entity.CarDefinition;
import com.fcar.modules.catalog.entity.CarImage;
import com.fcar.modules.catalog.repository.CarAttributeRepository;
import com.fcar.modules.catalog.repository.CarColorRepository;
import com.fcar.modules.catalog.repository.CarDefinitionRepository;
import com.fcar.modules.catalog.repository.CarImageRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Mutations and file storage for car definitions (admin). Extracted from {@code AdminCarDefinitionController}.
 */
@Service
@RequiredArgsConstructor
public class CarDefinitionAdminService {

    private final CarDefinitionRepository carDefinitionRepository;
    private final CarImageRepository carImageRepository;
    private final CarAttributeRepository carAttributeRepository;
    private final CarColorRepository carColorRepository;

    public String storeFile(MultipartFile file) throws IOException {
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
        return "/uploads/cars/" + filename;
    }

    public void deleteImageFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }
        try {
            String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
            Path base = Paths.get("uploads", "cars").toAbsolutePath().normalize();
            Path filePath = base.resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (Exception ignored) {
        }
    }

    public void storeImagesForCreate(CarDefinition savedDef, CarDefinitionForm form) throws IOException {
        String coverUrl = storeFile(form.getCoverImage());
        if (coverUrl != null) {
            CarImage cover = new CarImage();
            cover.setCarDefinition(savedDef);
            cover.setImageUrl(coverUrl);
            cover.setCover(true);
            carImageRepository.save(cover);
        }

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

    public void storeCoverAndGallery(CarDefinition def, MultipartFile cover, List<MultipartFile> gallery) throws IOException {
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
                if (url == null) {
                    continue;
                }
                CarImage img = new CarImage();
                img.setCarDefinition(def);
                img.setImageUrl(url);
                img.setCover(false);
                carImageRepository.save(img);
            }
        }
    }

    public ResponseEntity<Map<String, Object>> updateDefinitionJson(
            Long id,
            CarDefinitionEditForm form,
            List<Long> imageIdsToDelete,
            List<Long> colorIdsToDelete) {
        if (form.getBodyType() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Vui lòng chọn kiểu dáng"));
        }

        CarDefinition def = carDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Car definition not found"));

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

        if (imageIdsToDelete != null && !imageIdsToDelete.isEmpty()) {
            for (Long imageId : imageIdsToDelete) {
                carImageRepository.findById(imageId).ifPresent(img -> {
                    deleteImageFile(img.getImageUrl());
                    carImageRepository.delete(img);
                });
            }
        }

        Set<String> existingNormalized = carColorRepository.findByCarDefinition(def).stream()
                .map(c -> c.getColorValue() != null ? c.getColorValue().trim().toLowerCase() : "")
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (form.getNewColors() != null && !form.getNewColors().isEmpty()) {
            for (String c : form.getNewColors()) {
                String val = c == null ? "" : c.trim();
                if (val.isEmpty()) {
                    continue;
                }
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
            if (form.getCoverImage() != null && !form.getCoverImage().isEmpty()) {
                carImageRepository.findByCarDefinition(def).stream()
                        .filter(CarImage::isCover)
                        .forEach(img -> {
                            deleteImageFile(img.getImageUrl());
                            carImageRepository.delete(img);
                        });
                storeCoverAndGallery(def, form.getCoverImage(), null);
            }
            if (form.getGalleryImages() != null && !form.getGalleryImages().isEmpty()) {
                for (MultipartFile file : form.getGalleryImages()) {
                    String url = storeFile(file);
                    if (url == null) {
                        continue;
                    }
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

        if (form.getNewColors() != null) {
            for (String color : form.getNewColors()) {
                String val = color == null ? "" : color.trim();
                if (val.isEmpty()) {
                    continue;
                }
                CarColor carColor = new CarColor();
                carColor.setCarDefinition(def);
                carColor.setColorValue(val);
                carColorRepository.save(carColor);
            }
        }

        List<CarAttribute> attrs = carAttributeRepository.findByCarDefinition(def);
        for (CarAttribute a : attrs) {
            carAttributeRepository.delete(a);
        }
        if (form.getExtraAttributeNames() != null && form.getExtraAttributeValues() != null) {
            int size = Math.min(form.getExtraAttributeNames().size(), form.getExtraAttributeValues().size());
            for (int i = 0; i < size; i++) {
                String name = form.getExtraAttributeNames().get(i) == null ? "" : form.getExtraAttributeNames().get(i).trim();
                String value = form.getExtraAttributeValues().get(i) == null ? "" : form.getExtraAttributeValues().get(i).trim();
                if (name.isEmpty() && value.isEmpty()) {
                    continue;
                }
                if (name.length() > 50 || value.length() > 100) {
                    continue;
                }
                CarAttribute attr = new CarAttribute();
                attr.setCarDefinition(def);
                attr.setName(name);
                attr.setValue(value);
                carAttributeRepository.save(attr);
            }
        }

        Map<String, Object> ok = new LinkedHashMap<>();
        ok.put("success", true);
        ok.put("redirectUrl", "/admin/cars/definitions?success=updated");
        return ResponseEntity.ok(ok);
    }
}
