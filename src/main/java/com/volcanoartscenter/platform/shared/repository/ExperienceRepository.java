package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.Experience;
<<<<<<< HEAD
import org.springframework.data.jpa.repository.EntityGraph;
=======
>>>>>>> f8e8bc756db02040ef57e12be3260849005b05ac
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExperienceRepository extends JpaRepository<Experience, Long> {
<<<<<<< HEAD
    @EntityGraph(attributePaths = {"additionalImages"})
    List<Experience> findByActiveTrueOrderByFeaturedDescTitleAsc();

    @EntityGraph(attributePaths = {"additionalImages"})
    Optional<Experience> findBySlugAndActiveTrue(String slug);

    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = {"additionalImages"})
=======
    List<Experience> findByActiveTrueOrderByFeaturedDescTitleAsc();
    Optional<Experience> findBySlugAndActiveTrue(String slug);
    boolean existsBySlug(String slug);
>>>>>>> f8e8bc756db02040ef57e12be3260849005b05ac
    @Query("""
            SELECT e FROM Experience e
            WHERE (:active IS NULL OR e.active = :active)
              AND (:q IS NULL OR :q = '' OR
                   LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%')) OR
                   LOWER(COALESCE(e.shortDescription, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                   LOWER(COALESCE(e.location, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY e.featured DESC, e.title ASC
            """)
    List<Experience> searchForCms(@Param("active") Boolean active, @Param("q") String q);
}
