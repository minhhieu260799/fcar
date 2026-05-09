package com.fcar.modules.catalog.service.storefront;

import com.fcar.modules.catalog.entity.CarDefinition;
import com.fcar.modules.catalog.entity.CarInventory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Phân công: Hiệp Hiếu — JPA Specification niêm yết + tìm kiếm + lọc. */
public final class StorefrontCarDefinitionSpecification {

    private StorefrontCarDefinitionSpecification() {}

    public static Specification<CarDefinition> build(StorefrontListingFilter f) {
        return (Root<CarDefinition> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            // Không dùng DISTINCT: trên SQL Server, SELECT DISTINCT + ORDER BY cột từ JOIN
            // (brand/model) gây lỗi "ORDER BY items must appear in the select list".
            // Chỉ JOIN ManyToOne từ CarDefinition → không phát sinh bản trùng hàng.

            List<Predicate> predicates = new ArrayList<>();

            Join<Object, Object> brand = root.join("brand", JoinType.INNER);
            Join<Object, Object> model = root.join("model", JoinType.INNER);
            Join<Object, Object> segment = root.join("segment", JoinType.LEFT);

            predicates.add(cb.isFalse(root.get("deleted")));
            predicates.add(cb.isTrue(root.get("active")));
            predicates.add(cb.isTrue(brand.get("active")));
            predicates.add(cb.isFalse(brand.get("deleted")));
            predicates.add(cb.isTrue(model.get("active")));
            predicates.add(cb.isFalse(model.get("deleted")));

            predicates.add(
                    cb.or(
                            cb.isNull(root.get("segment")),
                            cb.and(cb.isTrue(segment.get("active")), cb.isFalse(segment.get("deleted")))));

            String q = f.q();
            if (!q.isEmpty()) {
                String safeInner = q.toLowerCase(Locale.ROOT).replace("%", "").replace("_", "").trim();
                if (!safeInner.isEmpty()) {
                    String pattern = "%" + safeInner + "%";
                    Expression<String> segName = segment.get("name");
                    Predicate segLike = cb.like(cb.lower(cb.coalesce(segName, cb.literal(""))), pattern);
                    Expression<String> yearStr = cb.function("str", String.class, root.get("productionYear"));
                    Predicate yearLike = cb.like(cb.lower(yearStr), pattern);
                    Predicate search = cb.or(
                            cb.like(cb.lower(brand.get("name")), pattern),
                            cb.like(cb.lower(model.get("name")), pattern),
                            segLike,
                            yearLike);
                    predicates.add(search);
                }
            }

            if (!f.brandIds().isEmpty()) {
                predicates.add(brand.get("id").in(f.brandIds()));
            }
            if (!f.modelIds().isEmpty()) {
                predicates.add(model.get("id").in(f.modelIds()));
            }

            if (!f.segmentIds().isEmpty()) {
                predicates.add(segment.get("id").in(f.segmentIds()));
            }

            if (f.inStockOnly()) {
                Subquery<Integer> sq = query.subquery(Integer.class);
                Root<CarInventory> inv = sq.from(CarInventory.class);
                sq.select(cb.literal(1));
                sq.where(
                        cb.equal(inv.get("carDefinition").get("id"), root.get("id")),
                        cb.isFalse(inv.get("disabled")),
                        cb.gt(inv.get("quantity"), 0));
                predicates.add(cb.exists(sq));
            }

            if (query != null) {
                query.orderBy(cb.asc(brand.get("name")), cb.asc(model.get("name")), cb.asc(root.get("productionYear")));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
