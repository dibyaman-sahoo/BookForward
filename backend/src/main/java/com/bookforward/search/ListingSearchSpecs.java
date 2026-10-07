package com.bookforward.search;

import com.bookforward.dto.ListingDtos.SearchCriteria;
import com.bookforward.entity.Listing;
import com.bookforward.entity.ListingStatus;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** PostgreSQL-backed search. SearchService is the boundary where OpenSearch/Elasticsearch could replace this. */
public final class ListingSearchSpecs {
    private ListingSearchSpecs() {}

    public static boolean hasText(String s) { return s != null && !s.isBlank(); }

    static String escape(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public static Specification<Listing> from(SearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            boolean all = "all".equalsIgnoreCase(c.availability());
            p.add(root.get("status").in(all ? List.of(ListingStatus.ACTIVE, ListingStatus.RESERVED, ListingStatus.SOLD)
                    : List.of(ListingStatus.ACTIVE)));

            String q = hasText(c.query()) ? c.query().trim().toLowerCase() : null;
            if (q != null) {
                for (String token : q.split("\\s+")) {
                    String like = "%" + escape(token) + "%";
                    p.add(cb.or(
                            cb.like(cb.lower(root.<String>get("title")), like, '\\'),
                            cb.like(cb.lower(root.<String>get("author")), like, '\\'),
                            cb.like(cb.lower(root.<String>get("publisher")), like, '\\'),
                            cb.like(cb.lower(root.<String>get("isbn")), like, '\\'),
                            cb.like(cb.lower(root.<String>get("board")), like, '\\'),
                            cb.like(cb.lower(root.<String>get("city")), like, '\\'),
                            cb.like(cb.lower(root.<String>get("area")), like, '\\'),
                            cb.like(cb.lower(root.get("category").<String>get("name")), like, '\\')));
                }
            }
            if (hasText(c.category())) p.add(cb.equal(root.get("category").get("slug"), c.category()));
            if (c.level() != null) p.add(cb.equal(root.get("academicLevel"), c.level()));
            if (hasText(c.board())) p.add(cb.equal(cb.lower(root.<String>get("board")), c.board().trim().toLowerCase()));
            if (hasText(c.city())) p.add(cb.equal(cb.lower(root.<String>get("city")), c.city().trim().toLowerCase()));
            if (c.ncert() != null) p.add(cb.equal(root.get("ncertApplicable"), c.ncert()));
            if (c.condition() != null) p.add(cb.equal(root.get("bookCondition"), c.condition()));
            if (c.minPrice() != null) p.add(cb.greaterThanOrEqualTo(root.get("price"), c.minPrice()));
            if (c.maxPrice() != null) p.add(cb.lessThanOrEqualTo(root.get("price"), c.maxPrice()));

            boolean relevance = q != null && (c.sort() == null || c.sort().equalsIgnoreCase("relevance"));
            if (relevance && query.getResultType() != Long.class && query.getResultType() != long.class) {
                Expression<String> title = cb.lower(root.<String>get("title"));
                Expression<Integer> rank = cb.<Integer>selectCase()
                        .when(cb.equal(title, q), 0)
                        .when(cb.like(title, escape(q) + "%", '\\'), 1)
                        .when(cb.like(title, "%" + escape(q) + "%", '\\'), 2)
                        .otherwise(3);
                query.orderBy(cb.asc(rank), cb.desc(root.get("createdAt")));
            }
            boolean nearest = "nearest".equalsIgnoreCase(c.sort()) && c.nearLat() != null && c.nearLon() != null;
            if (nearest && query.getResultType() != Long.class && query.getResultType() != long.class) {
                double cos = Math.cos(Math.toRadians(c.nearLat()));
                Expression<Double> dLat = cb.diff(root.<Double>get("latitude"), c.nearLat());
                Expression<Double> dLon = cb.prod(cb.diff(root.<Double>get("longitude"), c.nearLon()), cos);
                Expression<Double> dist = cb.sum(cb.prod(dLat, dLat), cb.prod(dLon, dLon));
                Expression<Integer> noCoords = cb.<Integer>selectCase().when(cb.isNull(root.get("latitude")), 1).otherwise(0);
                query.orderBy(cb.asc(noCoords), cb.asc(dist), cb.desc(root.get("createdAt")));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}
