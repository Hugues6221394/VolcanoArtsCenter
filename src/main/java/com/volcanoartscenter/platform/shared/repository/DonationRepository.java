package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.Donation;
import com.volcanoartscenter.platform.shared.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DonationRepository extends JpaRepository<Donation, Long> {
    List<Donation> findByUserOrderByCreatedAtDesc(User user);
}
