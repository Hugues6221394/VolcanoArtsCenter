package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByReservedUntilBefore(LocalDateTime dateTime);
    @EntityGraph(attributePaths = "category")
    List<Product> findByAvailableTrueOrderByFeaturedDescNameAsc();

    @EntityGraph(attributePaths = "category")
    List<Product> findByAvailableTrueAndCategory_SlugOrderByFeaturedDescNameAsc(String slug);

<<<<<<< HEAD
    @EntityGraph(attributePaths = {"category", "collection", "additionalImages"})
    Optional<Product> findBySlugAndAvailableTrueAndArtworkStatus(String slug, Product.ArtworkStatus artworkStatus);
=======
    @EntityGraph(attributePaths = "category")
    Optional<Product> findBySlugAndAvailableTrue(String slug);
>>>>>>> f8e8bc756db02040ef57e12be3260849005b05ac

    @EntityGraph(attributePaths = "category")
    Page<Product> findByAvailableTrue(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByAvailableTrueAndCategory_Slug(String slug, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Query("""
            SELECT p FROM Product p
            WHERE p.available = true
<<<<<<< HEAD
              AND p.artworkStatus = :artworkStatus
=======
>>>>>>> f8e8bc756db02040ef57e12be3260849005b05ac
              AND (:category IS NULL OR p.category.slug = :category)
              AND (:q IS NULL OR :q = '' OR
                   LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
                   LOWER(COALESCE(p.shortDescription, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                   LOWER(COALESCE(p.artistName, '')) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
            """)
    Page<Product> searchCatalog(@Param("category") String category,
                                @Param("q") String q,
                                @Param("minPrice") java.math.BigDecimal minPrice,
                                @Param("maxPrice") java.math.BigDecimal maxPrice,
<<<<<<< HEAD
                                @Param("artworkStatus") Product.ArtworkStatus artworkStatus,
=======
>>>>>>> f8e8bc756db02040ef57e12be3260849005b05ac
                                Pageable pageable);

    boolean existsBySlug(String slug);

<<<<<<< HEAD
    @EntityGraph(attributePaths = {"category", "collection", "additionalImages"})
=======
    @EntityGraph(attributePaths = {"category", "collection"})
>>>>>>> f8e8bc756db02040ef57e12be3260849005b05ac
    @Query("""
            SELECT p FROM Product p
            WHERE (:available IS NULL OR p.available = :available)
              AND (:featured IS NULL OR p.featured = :featured)
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:collectionId IS NULL OR p.collection.id = :collectionId)
              AND (:q IS NULL OR :q = '' OR
                    LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
                    LOWER(COALESCE(p.shortDescription, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                    LOWER(COALESCE(p.artistName, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                    LOWER(COALESCE(p.slug, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY p.featured DESC, p.name ASC
            """)
    List<Product> searchForCms(@Param("available") Boolean available,
                               @Param("featured") Boolean featured,
                               @Param("categoryId") Long categoryId,
                               @Param("collectionId") Long collectionId,
                               @Param("q") String q);
}
