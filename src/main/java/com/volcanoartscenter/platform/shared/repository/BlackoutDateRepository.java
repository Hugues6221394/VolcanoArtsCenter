package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.BlackoutDate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BlackoutDateRepository extends JpaRepository<BlackoutDate, Long> {
    Optional<BlackoutDate> findByExperienceIdAndDateValue(Long experienceId, LocalDate dateValue);
    List<BlackoutDate> findByExperienceIdOrderByDateValueAsc(Long experienceId);
}
