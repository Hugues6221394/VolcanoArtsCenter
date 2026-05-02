package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.Booking;
import com.volcanoartscenter.platform.shared.model.User;
<<<<<<< HEAD
import org.springframework.data.jpa.repository.EntityGraph;
=======
>>>>>>> f8e8bc756db02040ef57e12be3260849005b05ac
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
<<<<<<< HEAD
    @EntityGraph(attributePaths = {"experience", "user"})
    List<Booking> findByUserOrderByCreatedAtDesc(User user);

    @EntityGraph(attributePaths = {"experience"})
    List<Booking> findByExperienceIdAndPreferredDateAndStatusNot(Long experienceId, java.time.LocalDate preferredDate, Booking.BookingStatus status);

    @Override
    @EntityGraph(attributePaths = {"experience", "user"})
    List<Booking> findAll();

    @Override
    @EntityGraph(attributePaths = {"experience", "user"})
    java.util.Optional<Booking> findById(Long id);

=======
    List<Booking> findByUserOrderByCreatedAtDesc(User user);
    List<Booking> findByExperienceIdAndPreferredDateAndStatusNot(Long experienceId, java.time.LocalDate preferredDate, Booking.BookingStatus status);

>>>>>>> f8e8bc756db02040ef57e12be3260849005b05ac
    /** Review eligibility: has this user completed a booking for this experience? */
    @Query("SELECT COUNT(b) > 0 FROM Booking b " +
           "WHERE b.user.id = :userId AND b.experience.id = :experienceId " +
           "AND b.status = com.volcanoartscenter.platform.shared.model.Booking.BookingStatus.COMPLETED")
    boolean hasCompletedBooking(Long userId, Long experienceId);
}
