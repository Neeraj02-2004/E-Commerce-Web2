package com.neeraj.SpringEcom.Service;

import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.ReturnExchangeRequest;
import com.neeraj.SpringEcom.repo.ReturnExchangeRepo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReturnExchangeScheduler {

    private final ReturnExchangeRepo returnExchangeRepo;
    private final ReturnExchangeService returnExchangeService;

    public ReturnExchangeScheduler(
            ReturnExchangeRepo returnExchangeRepo,
            ReturnExchangeService returnExchangeService
    ) {
        this.returnExchangeRepo = returnExchangeRepo;
        this.returnExchangeService = returnExchangeService;
    }

    @Scheduled(cron = "0 30 1 * * *")
    @Transactional
    public void completeApprovedReturnExchangeRequests() {
        LocalDateTime sixDaysAgo = LocalDateTime.now().minusDays(6);

        List<ReturnExchangeRequest> approvedRequests =
                returnExchangeRepo.findByStatusAndUpdatedAtBefore(
                        AppConstants.ReturnExchangeStatus.APPROVED,
                        sixDaysAgo
                );

        if (approvedRequests.isEmpty()) {
            return;
        }

        for (ReturnExchangeRequest request : approvedRequests) {
            returnExchangeService.completeApprovedRequest(request, null);
        }

        returnExchangeRepo.saveAll(approvedRequests);
    }
}