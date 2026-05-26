package com.neeraj.SpringEcom.controller;

import com.neeraj.SpringEcom.service.ReturnExchangeService;
import com.neeraj.SpringEcom.model.dto.ReturnExchangeCreateRequest;
import com.neeraj.SpringEcom.model.dto.ReturnExchangeDecisionRequest;
import com.neeraj.SpringEcom.model.dto.ReturnExchangeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ReturnExchangeController {

    private final ReturnExchangeService returnExchangeService;

    public ReturnExchangeController(ReturnExchangeService returnExchangeService) {
        this.returnExchangeService = returnExchangeService;
    }

    @PostMapping("/api/orders/{orderId}/return-exchange")
    public ResponseEntity<ReturnExchangeResponse> createRequest(
            @PathVariable String orderId,
            @Valid @RequestBody ReturnExchangeCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(returnExchangeService.createRequest(orderId, request));
    }

    @GetMapping("/api/orders/return-exchange")
    public ResponseEntity<List<ReturnExchangeResponse>> getMyRequests() {
        return ResponseEntity.ok(returnExchangeService.getMyRequests());
    }

    @GetMapping("/api/admin/return-exchange")
    public ResponseEntity<List<ReturnExchangeResponse>> getAllRequests() {
        return ResponseEntity.ok(returnExchangeService.getAllRequests());
    }

    @PutMapping("/api/admin/return-exchange/{requestId}/approve")
    public ResponseEntity<ReturnExchangeResponse> approveRequest(
            @PathVariable String requestId,
            @Valid @RequestBody ReturnExchangeDecisionRequest request
    ) {
        return ResponseEntity.ok(returnExchangeService.approveRequest(requestId, request));
    }

    @PutMapping("/api/admin/return-exchange/{requestId}/reject")
    public ResponseEntity<ReturnExchangeResponse> rejectRequest(
            @PathVariable String requestId,
            @Valid @RequestBody ReturnExchangeDecisionRequest request
    ) {
        return ResponseEntity.ok(returnExchangeService.rejectRequest(requestId, request));
    }

    @PutMapping("/api/admin/return-exchange/{requestId}/complete")
    public ResponseEntity<ReturnExchangeResponse> completeRequest(
            @PathVariable String requestId,
            @Valid @RequestBody ReturnExchangeDecisionRequest request
    ) {
        return ResponseEntity.ok(returnExchangeService.completeRequest(requestId, request));
    }
}