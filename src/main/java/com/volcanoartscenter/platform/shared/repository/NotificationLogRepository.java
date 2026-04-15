package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
}
