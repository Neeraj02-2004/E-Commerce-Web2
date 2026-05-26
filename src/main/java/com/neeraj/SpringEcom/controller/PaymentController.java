package com.neeraj.SpringEcom.controller;

import com.neeraj.SpringEcom.service.PaymentService;
import com.neeraj.SpringEcom.model.dto.PaymentCreateRequest;
import com.neeraj.SpringEcom.model.dto.PaymentCreateResponse;
import com.neeraj.SpringEcom.model.dto.PaymentVerifyRequest;
import com.neeraj.SpringEcom.model.dto.PaymentVerifyResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    public ResponseEntity<PaymentCreateResponse> createPaymentOrder(
            @Valid @RequestBody PaymentCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(paymentService.createPaymentOrder(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentVerifyResponse> verifyPayment(
            @Valid @RequestBody PaymentVerifyRequest request
    ) {
        return ResponseEntity.ok(paymentService.verifyPayment(request));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature
    ) {
        paymentService.handleRazorpayWebhook(rawBody, signature);
        return ResponseEntity.ok("ok");
    }
}