package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.Booking;
import com.volcanoartscenter.platform.shared.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserOrderByCreatedAtDesc(User user);
    List<Booking> findByExperienceIdAndPreferredDateAndStatusNot(Long experienceId, java.time.LocalDate preferredDate, Booking.BookingStatus status);
}
