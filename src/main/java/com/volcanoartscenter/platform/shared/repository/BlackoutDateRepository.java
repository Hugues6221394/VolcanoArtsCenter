package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.BlackoutDate;
<<<<<<< HEAD
import org.springframework.data.jpa.repository.EntityGraph;
=======
>>>>>>> f8e8bc756db02040ef57e12be3260849005b05ac
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BlackoutDateRepository extends JpaRepository<BlackoutDate, Long> {
    Optional<BlackoutDate> findByExperienceIdAndDateValue(Long experienceId, LocalDate dateValue);
<<<<<<< HEAD

    @EntityGraph(attributePaths = {"experience"})
    List<BlackoutDate> findByExperienceIdOrderByDateValueAsc(Long experienceId);

    @EntityGraph(attributePaths = {"experience"})
    List<BlackoutDate> findAllByOrderByDateValueAsc();

    @Override
    @EntityGraph(attributePaths = {"experience"})
    Optional<BlackoutDate> findById(Long id);
=======
    List<BlackoutDate> findByExperienceIdOrderByDateValueAsc(Long experienceId);
>>>>>>> f8e8bc756db02040ef57e12be3260849005b05ac
}
