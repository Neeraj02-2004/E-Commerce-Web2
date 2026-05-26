package com.neeraj.SpringEcom.payment;

import java.util.Optional;

public interface RazorpayGateway {

    GatewayOrder createOrder(long amountInPaise, String currency, String receipt);

    boolean verifyPaymentSignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature);

    GatewayRefund createRefund(String paymentId, long amountInPaise, String refundReceipt, String idempotencyKey);

    Optional<GatewayRefund> findRefundByIdempotencyKey(
            String paymentId,
            String idempotencyKey,
            long amountInPaise
    );

    record GatewayOrder(String id) {
    }

    record GatewayRefund(String id, long amountInPaise) {
    }
}