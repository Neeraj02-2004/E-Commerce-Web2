package com.neeraj.SpringEcom.service;

import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.ReturnExchangeRequest;
import com.neeraj.SpringEcom.repo.ReturnExchangeRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnExchangeSchedulerTest {

    @Mock
    private ReturnExchangeRepo returnExchangeRepo;

    @Mock
    private ReturnExchangeService returnExchangeService;

    @InjectMocks
    private ReturnExchangeScheduler returnExchangeScheduler;

    @Test
    void completeApprovedReturnExchangeRequests_shouldCompleteOldApprovedReturnById() {
        ReturnExchangeRequest request = request(
                AppConstants.ReturnExchangeType.RETURN,
                AppConstants.ReturnExchangeStatus.APPROVED,
                AppConstants.RefundStatus.REFUND_PROCESSING,
                "REX123"
        );

        when(returnExchangeRepo.findByStatusAndUpdatedAtBefore(
                eq(AppConstants.ReturnExchangeStatus.APPROVED),
                any(LocalDateTime.class)
        )).thenReturn(List.of(request));

        returnExchangeScheduler.completeApprovedReturnExchangeRequests();

        verify(returnExchangeService).completeApprovedRequestById("REX123", null);
        verify(returnExchangeRepo, never()).saveAll(any());
    }

    @Test
    void completeApprovedReturnExchangeRequests_shouldCompleteOldApprovedExchangeById() {
        ReturnExchangeRequest request = request(
                AppConstants.ReturnExchangeType.EXCHANGE,
                AppConstants.ReturnExchangeStatus.APPROVED,
                AppConstants.RefundStatus.NOT_REQUIRED,
                "REX123"
        );

        when(returnExchangeRepo.findByStatusAndUpdatedAtBefore(
                eq(AppConstants.ReturnExchangeStatus.APPROVED),
                any(LocalDateTime.class)
        )).thenReturn(List.of(request));

        returnExchangeScheduler.completeApprovedReturnExchangeRequests();

        verify(returnExchangeService).completeApprovedRequestById("REX123", null);
        verify(returnExchangeRepo, never()).saveAll(any());
    }

    @Test
    void completeApprovedReturnExchangeRequests_whenNoOldApprovedRequests_shouldNotComplete() {
        when(returnExchangeRepo.findByStatusAndUpdatedAtBefore(
                eq(AppConstants.ReturnExchangeStatus.APPROVED),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        returnExchangeScheduler.completeApprovedReturnExchangeRequests();

        verify(returnExchangeService, never()).completeApprovedRequestById(any(), any());
        verify(returnExchangeRepo, never()).saveAll(any());
    }

    @Test
    void completeApprovedReturnExchangeRequests_whenOneRequestFails_shouldContinueNextRequest() {
        ReturnExchangeRequest firstRequest = request(
                AppConstants.ReturnExchangeType.RETURN,
                AppConstants.ReturnExchangeStatus.APPROVED,
                AppConstants.RefundStatus.REFUND_PROCESSING,
                "REX123"
        );

        ReturnExchangeRequest secondRequest = request(
                AppConstants.ReturnExchangeType.EXCHANGE,
                AppConstants.ReturnExchangeStatus.APPROVED,
                AppConstants.RefundStatus.NOT_REQUIRED,
                "REX456"
        );

        when(returnExchangeRepo.findByStatusAndUpdatedAtBefore(
                eq(AppConstants.ReturnExchangeStatus.APPROVED),
                any(LocalDateTime.class)
        )).thenReturn(List.of(firstRequest, secondRequest));

        doThrow(new RuntimeException("Temporary failure"))
                .when(returnExchangeService)
                .completeApprovedRequestById("REX123", null);

        returnExchangeScheduler.completeApprovedReturnExchangeRequests();

        verify(returnExchangeService).completeApprovedRequestById("REX123", null);
        verify(returnExchangeService).completeApprovedRequestById("REX456", null);
        verify(returnExchangeRepo, never()).saveAll(any());
    }

    private ReturnExchangeRequest request(
            String requestType,
            String status,
            String refundStatus,
            String requestId
    ) {
        ReturnExchangeRequest request = new ReturnExchangeRequest();
        request.setRequestId(requestId);
        request.setRequestType(requestType);
        request.setStatus(status);
        request.setRefundStatus(refundStatus);
        request.setUpdatedAt(LocalDateTime.now().minusDays(7));
        return request;
    }
}