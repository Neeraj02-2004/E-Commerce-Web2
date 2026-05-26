package com.neeraj.SpringEcom.service;

import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.ReturnExchangeRequest;
import com.neeraj.SpringEcom.repo.ReturnExchangeRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReturnExchangeScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReturnExchangeScheduler.class);

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
    public void completeApprovedReturnExchangeRequests() {
        LocalDateTime sixDaysAgo = LocalDateTime.now().minusDays(6);

        List<ReturnExchangeRequest> approvedRequests =
                returnExchangeRepo.findByStatusAndUpdatedAtBefore(
                        AppConstants.ReturnExchangeStatus.APPROVED,
                        sixDaysAgo
                );

        for (ReturnExchangeRequest request : approvedRequests) {
            try {
                returnExchangeService.completeApprovedRequestById(request.getRequestId(), null);
            } catch (Exception e) {
                log.error(
                        "Failed to complete approved return/exchange request {} during scheduled batch",
                        request.getRequestId(),
                        e
                );
            }
        }
    }
}