package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.BlogPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    List<BlogPost> findByPublishedTrueOrderByPublishedAtDesc();
    Optional<BlogPost> findBySlugAndPublishedTrue(String slug);
    Optional<BlogPost> findBySlug(String slug);
    boolean existsBySlug(String slug);
    @Query("""
            SELECT p FROM BlogPost p
            WHERE (:q IS NULL OR :q = '' OR
                   LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%')) OR
                   LOWER(COALESCE(p.excerpt, '')) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:category IS NULL OR p.category = :category)
            ORDER BY p.published DESC, p.publishedAt DESC, p.createdAt DESC
            """)
    List<BlogPost> searchForCms(@Param("q") String q, @Param("category") BlogPost.BlogCategory category);
}
