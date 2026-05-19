package com.neeraj.SpringEcom.model;

public final class AppConstants {

    private AppConstants() {
    }

    public static final class Role {
        public static final String USER = "USER";
        public static final String ADMIN = "ADMIN";

        private Role() {
        }
    }

    public static final class Provider {
        public static final String LOCAL = "LOCAL";
        public static final String GOOGLE = "GOOGLE";

        private Provider() {
        }
    }

    public static final class OrderStatus {
        public static final String PLACED = "PLACED";
        public static final String SHIPPED = "SHIPPED";
        public static final String DELIVERED = "DELIVERED";
        public static final String CANCELLED = "CANCELLED";
        public static final String FAILED = "FAILED";

        private OrderStatus() {
        }
    }

    public static final class PaymentMode {
        public static final String CASH_ON_DELIVERY = "CASH_ON_DELIVERY";
        public static final String ONLINE = "ONLINE";

        private PaymentMode() {
        }
    }

    public static final class PaymentStatus {
        public static final String PENDING = "PENDING";
        public static final String PAID = "PAID";
        public static final String FAILED = "FAILED";

        private PaymentStatus() {
        }
    }

    public static final class ReturnExchangeType {
        public static final String RETURN = "RETURN";
        public static final String EXCHANGE = "EXCHANGE";

        private ReturnExchangeType() {
        }
    }

    public static final class ReturnExchangeStatus {
        public static final String REQUESTED = "REQUESTED";
        public static final String APPROVED = "APPROVED";
        public static final String REJECTED = "REJECTED";
        public static final String COMPLETED = "COMPLETED";

        private ReturnExchangeStatus() {
        }
    }

    public static final class RefundStatus {
        public static final String NOT_REQUIRED = "NOT_REQUIRED";
        public static final String REFUND_PROCESSING = "REFUND_PROCESSING";
        public static final String MANUAL_REFUND_REQUIRED = "MANUAL_REFUND_REQUIRED";
        public static final String REFUNDED = "REFUNDED";
        public static final String REFUND_FAILED = "REFUND_FAILED";

        private RefundStatus() {
        }
    }
}