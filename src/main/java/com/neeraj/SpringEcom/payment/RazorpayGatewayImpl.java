package com.neeraj.SpringEcom.payment;

import com.neeraj.SpringEcom.exception.InvalidOrderException;
import com.razorpay.RazorpayClient;
import com.razorpay.Refund;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class RazorpayGatewayImpl implements RazorpayGateway {

    private static final Logger log = LoggerFactory.getLogger(RazorpayGatewayImpl.class);
    private static final String REFUND_IDEMPOTENCY_NOTE_KEY = "idem_key";

    private final RazorpayClient razorpayClient;
    private final String razorpayKeySecret;

    public RazorpayGatewayImpl(
            @Value("${razorpay.key-id}") String razorpayKeyId,
            @Value("${razorpay.key-secret}") String razorpayKeySecret
    ) throws Exception {
        this.razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        this.razorpayKeySecret = razorpayKeySecret;
    }

    @Override
    public GatewayOrder createOrder(long amountInPaise, String currency, String receipt) {
        long start = System.currentTimeMillis();

        try {
            JSONObject options = new JSONObject();
            options.put("amount", amountInPaise);
            options.put("currency", currency);
            options.put("receipt", receipt);
            options.put("payment_capture", 1);

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(options);
            return new GatewayOrder(razorpayOrder.get("id"));
        } catch (Exception e) {
            throw new InvalidOrderException("Unable to create payment order");
        } finally {
            log.info("Razorpay create order took {} ms for receipt {}", elapsedMillis(start), receipt);
        }
    }

    @Override
    public boolean verifyPaymentSignature(
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature
    ) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", razorpayOrderId);
            attributes.put("razorpay_payment_id", razorpayPaymentId);
            attributes.put("razorpay_signature", razorpaySignature);

            return Utils.verifyPaymentSignature(attributes, razorpayKeySecret);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public GatewayRefund createRefund(
            String paymentId,
            long amountInPaise,
            String refundReceipt,
            String idempotencyKey
    ) {
        long start = System.currentTimeMillis();

        try {
            JSONObject options = new JSONObject();
            options.put("amount", amountInPaise);
            options.put("speed", "normal");
            options.put("receipt", refundReceipt);

            JSONObject notes = new JSONObject();
            notes.put(REFUND_IDEMPOTENCY_NOTE_KEY, idempotencyKey);
            notes.put("return_exchange_request_id", refundReceipt);
            options.put("notes", notes);

            Refund refund = razorpayClient.payments.refund(paymentId, options);
            return new GatewayRefund(refund.get("id"), getRefundAmountInPaise(refund));
        } catch (Exception e) {
            throw new InvalidOrderException("Unable to process Razorpay refund");
        } finally {
            log.info("Razorpay create refund took {} ms for receipt {}", elapsedMillis(start), refundReceipt);
        }
    }

    @Override
    public Optional<GatewayRefund> findRefundByIdempotencyKey(
            String paymentId,
            String idempotencyKey,
            long amountInPaise
    ) {
        long start = System.currentTimeMillis();

        try {
            List<Refund> refunds = razorpayClient.payments.fetchAllRefunds(paymentId);

            return refunds.stream()
                    .filter(refund -> idempotencyKey.equals(getRefundIdempotencyKey(refund)))
                    .filter(refund -> amountInPaise == getRefundAmountInPaise(refund))
                    .findFirst()
                    .map(refund -> new GatewayRefund(refund.get("id"), getRefundAmountInPaise(refund)));
        } catch (Exception e) {
            return Optional.empty();
        } finally {
            log.info("Razorpay fetch refunds took {} ms", elapsedMillis(start));
        }
    }

    private String getRefundIdempotencyKey(Refund refund) {
        Object notesValue = refund.get("notes");

        if (notesValue instanceof JSONObject notes) {
            return notes.optString(REFUND_IDEMPOTENCY_NOTE_KEY, null);
        }

        return null;
    }

    private long getRefundAmountInPaise(Refund refund) {
        Object amountValue = refund.get("amount");

        if (amountValue instanceof Number number) {
            return number.longValue();
        }

        if (amountValue instanceof String amount) {
            return Long.parseLong(amount);
        }

        throw new InvalidOrderException("Razorpay refund amount is missing");
    }

    private long elapsedMillis(long start) {
        return System.currentTimeMillis() - start;
    }
}
