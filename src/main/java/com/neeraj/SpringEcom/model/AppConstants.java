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
        public static final String CANCELLED = "CANCELLED";
        public static final String DELIVERED = "DELIVERED";

        private OrderStatus() {
        }
    }

    public static final class PaymentMode {
        public static final String CASH_ON_DELIVERY = "CASH_ON_DELIVERY";

        private PaymentMode() {
        }
    }
}