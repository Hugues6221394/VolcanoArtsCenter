package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.Booking;
import com.volcanoartscenter.platform.shared.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserOrderByCreatedAtDesc(User user);
    List<Booking> findByExperienceIdAndPreferredDateAndStatusNot(Long experienceId, java.time.LocalDate preferredDate, Booking.BookingStatus status);

    /** Review eligibility: has this user completed a booking for this experience? */
    @Query("SELECT COUNT(b) > 0 FROM Booking b " +
           "WHERE b.user.id = :userId AND b.experience.id = :experienceId " +
           "AND b.status = com.volcanoartscenter.platform.shared.model.Booking.BookingStatus.COMPLETED")
    boolean hasCompletedBooking(Long userId, Long experienceId);
}
