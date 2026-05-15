package com.neeraj.SpringEcom.model.dto;

import java.time.LocalDate;
import java.util.List;

public record OrderResponse(
        String orderId,
        String customerName,
        String email,
        String mobileNo,
        String status,
        LocalDate orderDate,
        List<OrderItemResponse> items
) {

}
