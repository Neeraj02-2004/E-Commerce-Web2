package com.neeraj.SpringEcom.repo;

import com.neeraj.SpringEcom.model.RazorpayWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RazorpayWebhookEventRepo extends JpaRepository<RazorpayWebhookEvent, Long> {

    boolean existsByEventId(String eventId);
}