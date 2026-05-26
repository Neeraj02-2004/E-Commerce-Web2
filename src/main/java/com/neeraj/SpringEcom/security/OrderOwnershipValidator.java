package com.neeraj.SpringEcom.security;

import com.neeraj.SpringEcom.model.Order;
import com.neeraj.SpringEcom.util.EmailNormalizer;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class OrderOwnershipValidator {

    private final EmailNormalizer emailNormalizer;

    public OrderOwnershipValidator(EmailNormalizer emailNormalizer) {
        this.emailNormalizer = emailNormalizer;
    }

    public void assertOrderCanBeCancelledBy(Order order, String userEmail) {
        if (!isOwner(order, userEmail)) {
            throw new AccessDeniedException("You cannot cancel this order");
        }
    }

    public void assertOrderCanBePaidBy(Order order, String userEmail) {
        if (!isOwner(order, userEmail)) {
            throw new AccessDeniedException("You cannot pay for this order");
        }
    }

    public void assertOrderPaymentCanBeVerifiedBy(Order order, String userEmail) {
        if (!isOwner(order, userEmail)) {
            throw new AccessDeniedException("You cannot verify this payment");
        }
    }

    public void assertReturnExchangeCanBeCreatedBy(Order order, String userEmail) {
        if (!isOwner(order, userEmail)) {
            throw new AccessDeniedException("You cannot create request for this order");
        }
    }

    private boolean isOwner(Order order, String userEmail) {
        return order != null && emailNormalizer.equalsNormalized(userEmail, order.getUserEmail());
    }
}