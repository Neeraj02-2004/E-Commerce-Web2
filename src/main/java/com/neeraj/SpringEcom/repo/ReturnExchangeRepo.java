package com.neeraj.SpringEcom.repo;

import com.neeraj.SpringEcom.model.ReturnExchangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReturnExchangeRepo extends JpaRepository<ReturnExchangeRequest, Long> {

    Optional<ReturnExchangeRequest> findByRequestId(String requestId);

    Optional<ReturnExchangeRequest> findByGatewayRefundId(String gatewayRefundId);

    List<ReturnExchangeRequest> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    List<ReturnExchangeRequest> findByStatusOrderByCreatedAtDesc(String status);

    List<ReturnExchangeRequest> findAllByOrderByCreatedAtDesc();

    boolean existsByOrderIdAndUserEmailAndStatusIn(
            String orderId,
            String userEmail,
            List<String> statuses
    );

    List<ReturnExchangeRequest> findByStatusAndUpdatedAtBefore(
            String status,
            LocalDateTime updatedAt
    );
}