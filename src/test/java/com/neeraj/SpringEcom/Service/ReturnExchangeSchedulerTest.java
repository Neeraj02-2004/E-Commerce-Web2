package com.neeraj.SpringEcom.Service;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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
    void completeApprovedReturnExchangeRequests_shouldCompleteOldApprovedReturn() {
        ReturnExchangeRequest request = request(
                AppConstants.ReturnExchangeType.RETURN,
                AppConstants.ReturnExchangeStatus.APPROVED,
                AppConstants.RefundStatus.REFUND_PROCESSING
        );

        when(returnExchangeRepo.findByStatusAndUpdatedAtBefore(
                eq(AppConstants.ReturnExchangeStatus.APPROVED),
                any(LocalDateTime.class)
        )).thenReturn(List.of(request));

        returnExchangeScheduler.completeApprovedReturnExchangeRequests();

        verify(returnExchangeService).completeApprovedRequest(request, null);
        verify(returnExchangeRepo).saveAll(List.of(request));
    }

    @Test
    void completeApprovedReturnExchangeRequests_shouldCompleteOldApprovedExchange() {
        ReturnExchangeRequest request = request(
                AppConstants.ReturnExchangeType.EXCHANGE,
                AppConstants.ReturnExchangeStatus.APPROVED,
                AppConstants.RefundStatus.NOT_REQUIRED
        );

        when(returnExchangeRepo.findByStatusAndUpdatedAtBefore(
                eq(AppConstants.ReturnExchangeStatus.APPROVED),
                any(LocalDateTime.class)
        )).thenReturn(List.of(request));

        returnExchangeScheduler.completeApprovedReturnExchangeRequests();

        verify(returnExchangeService).completeApprovedRequest(request, null);
        verify(returnExchangeRepo).saveAll(List.of(request));
    }

    @Test
    void completeApprovedReturnExchangeRequests_whenNoOldApprovedRequests_shouldNotSave() {
        when(returnExchangeRepo.findByStatusAndUpdatedAtBefore(
                eq(AppConstants.ReturnExchangeStatus.APPROVED),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        returnExchangeScheduler.completeApprovedReturnExchangeRequests();

        verify(returnExchangeService, never()).completeApprovedRequest(any(ReturnExchangeRequest.class), any());
        verify(returnExchangeRepo, never()).saveAll(anyList());
    }

    private ReturnExchangeRequest request(
            String requestType,
            String status,
            String refundStatus
    ) {
        ReturnExchangeRequest request = new ReturnExchangeRequest();
        request.setRequestId("REX123");
        request.setRequestType(requestType);
        request.setStatus(status);
        request.setRefundStatus(refundStatus);
        request.setUpdatedAt(LocalDateTime.now().minusDays(7));
        return request;
    }
}