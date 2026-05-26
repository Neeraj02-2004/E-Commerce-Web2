package com.neeraj.SpringEcom.service;

import com.neeraj.SpringEcom.exception.InvalidOrderException;
import com.neeraj.SpringEcom.exception.OrderNotFoundException;
import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.Order;
import com.neeraj.SpringEcom.model.ReturnExchangeRequest;
import com.neeraj.SpringEcom.model.dto.ReturnExchangeCreateRequest;
import com.neeraj.SpringEcom.model.dto.ReturnExchangeDecisionRequest;
import com.neeraj.SpringEcom.model.dto.ReturnExchangeResponse;
import com.neeraj.SpringEcom.repo.OrderRepo;
import com.neeraj.SpringEcom.repo.ReturnExchangeRepo;
import com.neeraj.SpringEcom.security.CurrentUserProvider;
import com.neeraj.SpringEcom.security.OrderOwnershipValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReturnExchangeService {

    private final ReturnExchangeRepo returnExchangeRepo;
    private final OrderRepo orderRepo;
    private final PaymentService paymentService;
    private final CurrentUserProvider currentUserProvider;
    private final OrderOwnershipValidator orderOwnershipValidator;
    private final TransactionTemplate requiresNewTransactionTemplate;

    public ReturnExchangeService(
            ReturnExchangeRepo returnExchangeRepo,
            OrderRepo orderRepo,
            PaymentService paymentService,
            PlatformTransactionManager transactionManager,
            CurrentUserProvider currentUserProvider,
            OrderOwnershipValidator orderOwnershipValidator
    ) {
        this.returnExchangeRepo = returnExchangeRepo;
        this.orderRepo = orderRepo;
        this.paymentService = paymentService;
        this.currentUserProvider = currentUserProvider;
        this.orderOwnershipValidator = orderOwnershipValidator;
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public ReturnExchangeResponse createRequest(String orderId, ReturnExchangeCreateRequest request) {
        String userEmail = currentUserProvider.getAuthenticatedEmail();

        Order order = orderRepo.findByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        orderOwnershipValidator.assertReturnExchangeCanBeCreatedBy(order, userEmail);

        validateOrderEligible(order);
        String requestType = validateRequestType(request.requestType());
        String reason = validateReason(request.reason());

        boolean activeRequestExists = returnExchangeRepo.existsByOrderIdAndUserEmailAndStatusIn(
                order.getOrderId(),
                userEmail,
                List.of(
                        AppConstants.ReturnExchangeStatus.REQUESTED,
                        AppConstants.ReturnExchangeStatus.APPROVED
                )
        );

        if (activeRequestExists) {
            throw new InvalidOrderException("A return or exchange request already exists for this order");
        }

        ReturnExchangeRequest entity = new ReturnExchangeRequest();
        entity.setRequestId("REX" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        entity.setOrder(order);
        entity.setOrderId(order.getOrderId());
        entity.setUserEmail(userEmail);
        entity.setRequestType(requestType);
        entity.setReason(reason);
        entity.setStatus(AppConstants.ReturnExchangeStatus.REQUESTED);
        entity.setRefundStatus(AppConstants.RefundStatus.NOT_REQUIRED);

        return toResponse(returnExchangeRepo.save(entity));
    }

    @Transactional(readOnly = true)
    public List<ReturnExchangeResponse> getMyRequests() {
        String userEmail = currentUserProvider.getAuthenticatedEmail();

        return returnExchangeRepo.findByUserEmailOrderByCreatedAtDesc(userEmail)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReturnExchangeResponse> getAllRequests() {
        return returnExchangeRepo.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReturnExchangeResponse approveRequest(String requestId, ReturnExchangeDecisionRequest request) {
        ReturnExchangeRequest entity = getRequest(requestId);

        if (!AppConstants.ReturnExchangeStatus.REQUESTED.equals(entity.getStatus())) {
            throw new InvalidOrderException("Only requested return/exchange can be approved");
        }

        entity.setStatus(AppConstants.ReturnExchangeStatus.APPROVED);
        entity.setAdminNote(cleanAdminNote(request.adminNote()));
        entity.setApprovedAt(LocalDateTime.now());
        entity.setRefundFailureReason(null);

        if (AppConstants.ReturnExchangeType.RETURN.equals(entity.getRequestType())) {
            if (isOnlinePaidOrder(entity.getOrder())) {
                entity.setRefundStatus(AppConstants.RefundStatus.REFUND_PROCESSING);
            } else {
                entity.setRefundStatus(AppConstants.RefundStatus.MANUAL_REFUND_REQUIRED);
            }
        }

        return toResponse(returnExchangeRepo.save(entity));
    }

    @Transactional
    public ReturnExchangeResponse rejectRequest(String requestId, ReturnExchangeDecisionRequest request) {
        ReturnExchangeRequest entity = getRequest(requestId);

        if (!AppConstants.ReturnExchangeStatus.REQUESTED.equals(entity.getStatus())) {
            throw new InvalidOrderException("Only requested return/exchange can be rejected");
        }

        entity.setStatus(AppConstants.ReturnExchangeStatus.REJECTED);
        entity.setAdminNote(cleanAdminNote(request.adminNote()));
        entity.setRefundStatus(AppConstants.RefundStatus.NOT_REQUIRED);
        entity.setRefundFailureReason(null);

        return toResponse(returnExchangeRepo.save(entity));
    }

    @Transactional
    public ReturnExchangeResponse completeRequest(String requestId, ReturnExchangeDecisionRequest request) {
        ReturnExchangeRequest entity = getRequest(requestId);

        if (!AppConstants.ReturnExchangeStatus.APPROVED.equals(entity.getStatus())) {
            throw new InvalidOrderException("Only approved return/exchange can be completed");
        }

        completeApprovedRequest(entity, cleanAdminNote(request.adminNote()));

        return toResponse(returnExchangeRepo.save(entity));
    }

    public void completeApprovedRequest(ReturnExchangeRequest entity, String adminNote) {
        if (adminNote != null) {
            entity.setAdminNote(adminNote);
        }

        if (AppConstants.ReturnExchangeType.EXCHANGE.equals(entity.getRequestType())) {
            entity.setStatus(AppConstants.ReturnExchangeStatus.COMPLETED);
            entity.setCompletedAt(LocalDateTime.now());
            return;
        }

        if (!AppConstants.ReturnExchangeType.RETURN.equals(entity.getRequestType())) {
            throw new InvalidOrderException("Invalid return/exchange request type");
        }

        if (isOnlinePaidOrder(entity.getOrder())) {
            processOnlineRefund(entity);
            return;
        }

        entity.setStatus(AppConstants.ReturnExchangeStatus.COMPLETED);
        entity.setRefundStatus(AppConstants.RefundStatus.REFUNDED);
        entity.setCompletedAt(LocalDateTime.now());
        entity.setRefundProcessedAt(LocalDateTime.now());
        entity.setRefundFailureReason(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeApprovedRequestById(String requestId, String adminNote) {
        ReturnExchangeRequest entity = getRequest(requestId);

        if (!AppConstants.ReturnExchangeStatus.APPROVED.equals(entity.getStatus())) {
            return;
        }

        completeApprovedRequest(entity, adminNote);
        returnExchangeRepo.save(entity);
    }

    private void processOnlineRefund(ReturnExchangeRequest entity) {
        try {
            if (entity.getGatewayRefundId() != null && !entity.getGatewayRefundId().isBlank()) {
                markRefundCompleted(entity);
                return;
            }

            prepareRefundForProcessing(entity);

            PaymentService.RefundResult refundResult = paymentService.refundOnlinePayment(
                    entity.getOrder(),
                    entity.getRequestId(),
                    entity.getRefundIdempotencyKey()
            );

            entity.setGatewayRefundId(refundResult.gatewayRefundId());
            entity.setRefundAmount(refundResult.refundAmount());
            entity.setRefundProcessedAt(refundResult.refundProcessedAt());
            markRefundCompleted(entity);
        } catch (InvalidOrderException e) {
            entity.setRefundStatus(AppConstants.RefundStatus.REFUND_FAILED);
            entity.setRefundFailureReason(e.getMessage());
        }
    }

    private void prepareRefundForProcessing(ReturnExchangeRequest entity) {
        if (entity.getRefundIdempotencyKey() == null || entity.getRefundIdempotencyKey().isBlank()) {
            entity.setRefundIdempotencyKey(UUID.randomUUID().toString());
        }

        entity.setRefundStatus(AppConstants.RefundStatus.REFUND_PROCESSING);
        entity.setRefundFailureReason(null);

        requiresNewTransactionTemplate.executeWithoutResult(status -> returnExchangeRepo.saveAndFlush(entity));
    }

    private void markRefundCompleted(ReturnExchangeRequest entity) {
        entity.setRefundStatus(AppConstants.RefundStatus.REFUNDED);
        entity.setStatus(AppConstants.ReturnExchangeStatus.COMPLETED);
        entity.setCompletedAt(LocalDateTime.now());
        entity.setRefundFailureReason(null);
    }

    private boolean isOnlinePaidOrder(Order order) {
        return order.getPaymentMode() == AppConstants.PaymentMode.ONLINE
                && order.getPaymentStatus() == AppConstants.PaymentStatus.PAID;
    }

    private ReturnExchangeRequest getRequest(String requestId) {
        return returnExchangeRepo.findByRequestId(requestId)
                .orElseThrow(() -> new InvalidOrderException("Return/exchange request not found"));
    }

    private void validateOrderEligible(Order order) {
        if (order.getStatus() != AppConstants.OrderStatus.DELIVERED) {
            throw new InvalidOrderException("Return/exchange is allowed only for delivered orders");
        }

        if (order.getDeliveredAt() == null) {
            throw new InvalidOrderException("Delivery date is missing for this order");
        }

        LocalDateTime returnWindowEnd = order.getDeliveredAt().plusDays(7);

        if (LocalDateTime.now().isAfter(returnWindowEnd)) {
            throw new InvalidOrderException("Return/exchange window has expired");
        }
    }

    private String validateRequestType(String requestType) {
        if (requestType == null || requestType.isBlank()) {
            throw new InvalidOrderException("Request type is required");
        }

        String cleanType = requestType.trim().toUpperCase();

        if (!AppConstants.ReturnExchangeType.RETURN.equals(cleanType)
                && !AppConstants.ReturnExchangeType.EXCHANGE.equals(cleanType)) {
            throw new InvalidOrderException("Request type must be RETURN or EXCHANGE");
        }

        return cleanType;
    }

    private String validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new InvalidOrderException("Reason is required");
        }

        String cleanReason = reason.trim().replaceAll("\\s+", " ");

        if (cleanReason.length() < 10 || cleanReason.length() > 1000) {
            throw new InvalidOrderException("Reason must be between 10 and 1000 characters");
        }

        return cleanReason;
    }

    private String cleanAdminNote(String adminNote) {
        if (adminNote == null || adminNote.isBlank()) {
            return null;
        }

        return adminNote.trim().replaceAll("\\s+", " ");
    }

    private ReturnExchangeResponse toResponse(ReturnExchangeRequest entity) {
        return new ReturnExchangeResponse(
                entity.getRequestId(),
                entity.getOrderId(),
                entity.getUserEmail(),
                entity.getRequestType(),
                entity.getReason(),
                entity.getStatus(),
                entity.getRefundStatus(),
                entity.getAdminNote(),
                entity.getGatewayRefundId(),
                entity.getRefundAmount(),
                entity.getApprovedAt(),
                entity.getCompletedAt(),
                entity.getRefundProcessedAt(),
                entity.getRefundFailureReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}