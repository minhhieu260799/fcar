package com.fcar.service;

import com.fcar.domain.Brand;
import com.fcar.domain.CarDefinition;
import com.fcar.domain.CarInventory;
import com.fcar.domain.CarModel;
import com.fcar.domain.Segment;
import com.fcar.repository.BrandRepository;
import com.fcar.repository.CarDefinitionRepository;
import com.fcar.repository.CarInventoryRepository;
import com.fcar.repository.CarModelRepository;
import com.fcar.repository.SegmentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phân công: Hiếu — bật/tắt hoạt động cascade (thương hiệu → … → tồn kho).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CatalogActiveCascadeService {

    private final BrandRepository brandRepository;
    private final CarModelRepository carModelRepository;
    private final SegmentRepository segmentRepository;
    private final CarDefinitionRepository carDefinitionRepository;
    private final CarInventoryRepository carInventoryRepository;

    public void disableBrand(Long brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));
        brand.setActive(false);
        brandRepository.save(brand);
        for (CarModel m : carModelRepository.findByBrandAndDeletedFalse(brand)) {
            disableModelSubtree(m);
        }
    }

    public void enableBrand(Long brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));
        brand.setActive(true);
        brandRepository.save(brand);
        for (CarModel m : carModelRepository.findByBrandAndDeletedFalse(brand)) {
            enableModelSubtree(m);
        }
    }

    public void disableCarModel(Long modelId) {
        CarModel m = carModelRepository.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found"));
        disableModelSubtree(m);
    }

    /** @return thông báo lỗi tiếng Việt, hoặc {@code null} nếu thành công */
    public String enableCarModel(Long modelId) {
        CarModel m = carModelRepository.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found"));
        Brand b = m.getBrand();
        if (b == null || !b.isActive()) {
            String bn = b != null && b.getName() != null && !b.getName().isBlank() ? b.getName().trim() : "—";
            return "Dòng xe không thể hoạt động khi thương hiệu «" + bn + "» đang tạm ngưng";
        }
        enableModelSubtree(m);
        return null;
    }

    public void disableSegment(Long segmentId) {
        Segment s = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new IllegalArgumentException("Segment not found"));
        s.setActive(false);
        segmentRepository.save(s);
        for (CarDefinition d : carDefinitionRepository.findBySegmentAndDeletedFalse(s)) {
            deactivateDefinitionAndInventory(d);
        }
    }

    /** @return thông báo lỗi, hoặc {@code null} nếu thành công */
    public String enableSegment(Long segmentId) {
        Segment s = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new IllegalArgumentException("Segment not found"));
        CarModel m = s.getModel();
        if (m == null || !m.isActive()) {
            String mn = m != null && m.getName() != null && !m.getName().isBlank() ? m.getName().trim() : "—";
            return "Phân khúc không thể hoạt động khi dòng xe «" + mn + "» đang tạm ngưng";
        }
        s.setActive(true);
        segmentRepository.save(s);
        for (CarDefinition d : carDefinitionRepository.findBySegmentAndDeletedFalse(s)) {
            activateDefinitionAndInventory(d);
        }
        return null;
    }

    public void disableCarDefinition(Long definitionId) {
        CarDefinition d = carDefinitionRepository.findById(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("Car definition not found"));
        deactivateDefinitionAndInventory(d);
    }

    /** @return thông báo lỗi, hoặc {@code null} nếu thành công */
    public String enableCarDefinition(Long definitionId) {
        CarDefinition def = carDefinitionRepository.findById(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("Car definition not found"));
        Segment seg = def.getSegment();
        if (seg != null) {
            if (!seg.isActive()) {
                String sn = seg.getName() != null && !seg.getName().isBlank() ? seg.getName().trim() : "—";
                return "Mẫu xe không thể hoạt động khi phân khúc «" + sn + "» đang tạm ngưng";
            }
        } else {
            CarModel m = def.getModel();
            if (m == null || !m.isActive()) {
                String mn = m != null && m.getName() != null && !m.getName().isBlank() ? m.getName().trim() : "—";
                return "Mẫu xe không thể hoạt động khi dòng xe «" + mn + "» đang tạm ngưng";
            }
        }
        activateDefinitionAndInventory(def);
        return null;
    }

    private void disableModelSubtree(CarModel m) {
        m.setActive(false);
        carModelRepository.save(m);
        for (Segment s : segmentRepository.findByModelAndDeletedFalse(m)) {
            s.setActive(false);
            segmentRepository.save(s);
            for (CarDefinition d : carDefinitionRepository.findBySegmentAndDeletedFalse(s)) {
                deactivateDefinitionAndInventory(d);
            }
        }
        for (CarDefinition d : carDefinitionRepository.findByModelAndDeletedFalse(m)) {
            if (d.getSegment() == null) {
                deactivateDefinitionAndInventory(d);
            }
        }
    }

    /** Bật dòng xe và toàn bộ phân khúc / mẫu / tồn kho phía dưới (đã đảm bảo thương hiệu hoạt động nếu gọi từ enableCarModel). */
    private void enableModelSubtree(CarModel m) {
        m.setActive(true);
        carModelRepository.save(m);
        for (Segment s : segmentRepository.findByModelAndDeletedFalse(m)) {
            s.setActive(true);
            segmentRepository.save(s);
            for (CarDefinition d : carDefinitionRepository.findBySegmentAndDeletedFalse(s)) {
                activateDefinitionAndInventory(d);
            }
        }
        for (CarDefinition d : carDefinitionRepository.findByModelAndDeletedFalse(m)) {
            if (d.getSegment() == null) {
                activateDefinitionAndInventory(d);
            }
        }
    }

    private void deactivateDefinitionAndInventory(CarDefinition d) {
        d.setActive(false);
        carDefinitionRepository.save(d);
        List<CarInventory> rows = carInventoryRepository.findByCarDefinition(d);
        for (CarInventory inv : rows) {
            inv.setDisabled(true);
            carInventoryRepository.save(inv);
        }
    }

    private void activateDefinitionAndInventory(CarDefinition d) {
        d.setActive(true);
        carDefinitionRepository.save(d);
        List<CarInventory> rows = carInventoryRepository.findByCarDefinition(d);
        for (CarInventory inv : rows) {
            inv.setDisabled(false);
            carInventoryRepository.save(inv);
        }
    }
}
