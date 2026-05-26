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

    public enum OrderStatus {
        PLACED,
        SHIPPED,
        DELIVERED,
        CANCELLED,
        FAILED
    }

    public enum PaymentMode {
        CASH_ON_DELIVERY,
        ONLINE
    }

    public enum PaymentStatus {
        PENDING,
        PAID,
        FAILED
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