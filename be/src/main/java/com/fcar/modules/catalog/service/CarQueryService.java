package com.fcar.modules.catalog.service;

import com.fcar.modules.catalog.entity.Brand;
import com.fcar.modules.catalog.entity.CarDefinition;
import com.fcar.modules.catalog.entity.CarInventory;
import com.fcar.modules.catalog.entity.CarModel;
import com.fcar.modules.catalog.entity.Segment;
import com.fcar.modules.catalog.repository.BrandRepository;
import com.fcar.modules.catalog.repository.CarDefinitionRepository;
import com.fcar.modules.catalog.repository.CarInventoryRepository;
import com.fcar.modules.catalog.repository.CarModelRepository;
import com.fcar.modules.catalog.repository.SegmentRepository;
import com.fcar.modules.catalog.service.storefront.StorefrontCarDefinitionSpecification;
import com.fcar.modules.catalog.service.storefront.StorefrontFilterOptions;
import com.fcar.modules.catalog.service.storefront.StorefrontFilterOptions.StorefrontBrandOption;
import com.fcar.modules.catalog.service.storefront.StorefrontFilterOptions.StorefrontModelOption;
import com.fcar.modules.catalog.service.storefront.StorefrontFilterOptions.StorefrontSegmentOption;
import com.fcar.modules.catalog.service.storefront.StorefrontListingFilter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Phân công: Hiệp Hiếu — truy vấn xe niêm yết, tồn kho, bộ lọc storefront. */
@Service
@RequiredArgsConstructor
public class CarQueryService {

    /** Mẫu xe niêm yết + tổng số lượng tồn (tất cả dòng kho không disabled). */
    public record ListedCarDefinition(CarDefinition definition, long totalStock) {}

    private final CarDefinitionRepository carDefinitionRepository;
    private final CarInventoryRepository carInventoryRepository;
    private final BrandRepository brandRepository;
    private final CarModelRepository carModelRepository;
    private final SegmentRepository segmentRepository;

    @Transactional(readOnly = true)
    public Page<ListedCarDefinition> listDefinitionsForStorefront(int page, int size) {
        return listDefinitionsForStorefront(page, size, StorefrontListingFilter.empty());
    }

    /** @deprecated dùng {@link #listDefinitionsForStorefront(int, int, StorefrontListingFilter)} */
    @Deprecated
    @Transactional(readOnly = true)
    public Page<ListedCarDefinition> listDefinitionsForStorefront(int page, int size, String q) {
        StorefrontListingFilter f =
                (q == null || q.isBlank()) ? StorefrontListingFilter.empty() : new StorefrontListingFilter(q, List.of(), List.of(), List.of(), false);
        return listDefinitionsForStorefront(page, size, f);
    }

    @Transactional(readOnly = true)
    public Page<ListedCarDefinition> listDefinitionsForStorefront(int page, int size, StorefrontListingFilter filter) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        StorefrontListingFilter f = filter == null ? StorefrontListingFilter.empty() : filter;

        if (!f.hasEffectiveFilters()) {
            Page<Long> idPage = carDefinitionRepository.findListedIds(pageable);
            return mapIdsToListedPage(idPage);
        }

        var spec = StorefrontCarDefinitionSpecification.build(f);
        Page<CarDefinition> defPage = carDefinitionRepository.findAll(spec, pageable);
        List<Long> ids = defPage.getContent().stream().map(CarDefinition::getId).toList();
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), defPage.getPageable(), defPage.getTotalElements());
        }
        List<CarDefinition> loaded = carDefinitionRepository.findListedByIdsWithImages(ids);
        Map<Long, CarDefinition> byId = loaded.stream()
                .collect(Collectors.toMap(CarDefinition::getId, d -> d, (a, b) -> a, LinkedHashMap::new));
        List<CarDefinition> ordered = ids.stream().map(byId::get).filter(Objects::nonNull).toList();
        Map<Long, Long> stockMap = stockMapForDefinitionIds(ids);
        List<ListedCarDefinition> content = ordered.stream()
                .map(d -> new ListedCarDefinition(d, stockMap.getOrDefault(d.getId(), 0L)))
                .toList();
        return new PageImpl<>(content, defPage.getPageable(), defPage.getTotalElements());
    }

    private Page<ListedCarDefinition> mapIdsToListedPage(Page<Long> idPage) {
        if (idPage.isEmpty()) {
            return new PageImpl<>(List.of(), idPage.getPageable(), idPage.getTotalElements());
        }
        List<Long> ids = idPage.getContent();
        List<CarDefinition> loaded = carDefinitionRepository.findListedByIdsWithImages(ids);
        Map<Long, CarDefinition> byId = loaded.stream()
                .collect(Collectors.toMap(CarDefinition::getId, d -> d, (a, b) -> a, LinkedHashMap::new));
        List<CarDefinition> ordered = ids.stream().map(byId::get).filter(Objects::nonNull).toList();
        Map<Long, Long> stockMap = stockMapForDefinitionIds(ids);
        List<ListedCarDefinition> content = ordered.stream()
                .map(d -> new ListedCarDefinition(d, stockMap.getOrDefault(d.getId(), 0L)))
                .toList();
        return new PageImpl<>(content, idPage.getPageable(), idPage.getTotalElements());
    }

    /** Hãng / dòng / phân khúc đang có ít nhất một mẫu niêm yết (cho checkbox lọc). */
    @Transactional(readOnly = true)
    public StorefrontFilterOptions loadStorefrontFilterOptions() {
        List<Long> brandIdList = carDefinitionRepository.findDistinctListedBrandIds();
        List<Brand> brands =
                brandIdList.isEmpty()
                        ? List.of()
                        : brandRepository.findAllById(brandIdList).stream()
                                .sorted(Comparator.comparing(Brand::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                                .toList();
        List<StorefrontBrandOption> brandOpts =
                brands.stream().map(b -> new StorefrontBrandOption(b.getId(), b.getName())).toList();

        List<Long> modelIdList = carDefinitionRepository.findDistinctListedModelIds();
        List<StorefrontModelOption> modelOpts;
        if (modelIdList.isEmpty()) {
            modelOpts = List.of();
        } else {
            List<CarModel> models = carModelRepository.findAllByIdInWithBrand(modelIdList);
            modelOpts = models.stream()
                    .map(m -> new StorefrontModelOption(m.getId(), m.getName(), m.getBrand().getId()))
                    .toList();
        }

        List<Long> segIdList = carDefinitionRepository.findDistinctListedSegmentIds();
        List<StorefrontSegmentOption> segOpts;
        if (segIdList.isEmpty()) {
            segOpts = List.of();
        } else {
            List<Segment> segs = segmentRepository.findAllByIdInWithModelAndBrand(segIdList);
            segOpts = segs.stream()
                    .map(s -> new StorefrontSegmentOption(s.getId(), s.getName(), s.getModel().getId()))
                    .toList();
        }

        return new StorefrontFilterOptions(brandOpts, modelOpts, segOpts);
    }

    /** Gợi ý xe để thêm vào so sánh (giới hạn số lượng, cùng thứ tự sắp xếp với danh sách). */
    @Transactional(readOnly = true)
    public List<ListedCarDefinition> listDefinitionsForComparePicker(int maxSize) {
        int size = Math.min(Math.max(maxSize, 1), 200);
        Page<ListedCarDefinition> page = listDefinitionsForStorefront(0, size);
        return page.getContent();
    }

    private Map<Long, Long> stockMapForDefinitionIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = carInventoryRepository.sumQuantityByDefinitionIds(ids);
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }

    @Transactional(readOnly = true)
    public Optional<CarDefinition> findListedDefinitionForDetail(Long id) {
        Optional<CarDefinition> opt = carDefinitionRepository.findByIdWithDetailsForStorefront(id);
        return opt.map(def -> {
            def.getColors().size(); // nạp colors trong cùng tx (không join fetch cùng images)
            def.getAttributes().size(); // nạp thông số khác — view có th:if/th:each (open-in-view tắt)
            return def;
        }).filter(this::isListedDefinition);
    }

    private boolean isListedDefinition(CarDefinition d) {
        if (d == null || d.isDeleted() || !d.isActive()) {
            return false;
        }
        if (d.getBrand() == null || !d.getBrand().isActive() || d.getBrand().isDeleted()) {
            return false;
        }
        if (d.getModel() == null || !d.getModel().isActive() || d.getModel().isDeleted()) {
            return false;
        }
        if (d.getSegment() != null && (!d.getSegment().isActive() || d.getSegment().isDeleted())) {
            return false;
        }
        return true;
    }

    public long totalStockForDefinition(Long definitionId) {
        List<Object[]> rows = carInventoryRepository.sumQuantityByDefinitionIds(List.of(definitionId));
        if (rows.isEmpty()) {
            return 0L;
        }
        return ((Number) rows.get(0)[1]).longValue();
    }

    /**
     * Một dòng kho đại diện (đặt mua / yêu thích / liên hệ khi URL theo mẫu xe).
     * Luôn trả về bản đã join-fetch đủ quan hệ để render view (tránh LazyInitializationException).
     */
    public Optional<CarInventory> resolveRepresentativeInventory(Long definitionId) {
        Optional<CarDefinition> defOpt = carDefinitionRepository.findById(definitionId);
        if (defOpt.isEmpty()) {
            return Optional.empty();
        }
        CarDefinition def = defOpt.get();
        List<CarInventory> enabled = carInventoryRepository.findByCarDefinitionAndDisabledFalse(def);
        Optional<CarInventory> withStock = enabled.stream()
                .filter(ci -> ci.getQuantity() != null && ci.getQuantity() > 0)
                .findFirst();
        Optional<CarInventory> picked;
        if (withStock.isPresent()) {
            picked = withStock;
        } else if (!enabled.isEmpty()) {
            picked = Optional.of(enabled.get(0));
        } else {
            List<CarInventory> any = carInventoryRepository.findByCarDefinition(def);
            picked = any.stream().findFirst();
        }
        return picked.flatMap(ci -> carInventoryRepository.findByIdWithRelations(ci.getId()));
    }
}
