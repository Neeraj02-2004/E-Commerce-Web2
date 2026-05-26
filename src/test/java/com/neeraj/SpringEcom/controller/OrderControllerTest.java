package com.neeraj.SpringEcom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neeraj.SpringEcom.exception.GlobalExceptionHandler;
import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.dto.OrderItemRequest;
import com.neeraj.SpringEcom.model.dto.OrderItemResponse;
import com.neeraj.SpringEcom.model.dto.OrderRequest;
import com.neeraj.SpringEcom.model.dto.OrderResponse;
import com.neeraj.SpringEcom.model.dto.PageResponse;
import com.neeraj.SpringEcom.service.JwtService;
import com.neeraj.SpringEcom.service.MyUserDetailsService;
import com.neeraj.SpringEcom.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private MyUserDetailsService myUserDetailsService;

    @Test
    void placeOrder_withValidRequest_shouldReturnCreatedOrder() throws Exception {
        OrderResponse response = orderResponse("ORD123", AppConstants.OrderStatus.PLACED);

        when(orderService.placeOrder(any(OrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/place")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("ORD123"))
                .andExpect(jsonPath("$.customerName").value("Neeraj Kumar"))
                .andExpect(jsonPath("$.paymentMode").value("CASH_ON_DELIVERY"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.items[0].productName").value("Test Phone"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        verify(orderService).placeOrder(any(OrderRequest.class));
    }

    @Test
    void getAllOrders_shouldReturnPagedOrders() throws Exception {
        PageResponse<OrderResponse> page = new PageResponse<>(
                List.of(orderResponse("ORD123", AppConstants.OrderStatus.PLACED)),
                0,
                10,
                1,
                1
        );

        when(orderService.getAllOrderResponses(0, 10)).thenReturn(page);

        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value("ORD123"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(orderService).getAllOrderResponses(0, 10);
    }

    @Test
    void cancelOrder_shouldReturnCancelledOrder() throws Exception {
        when(orderService.cancelOrder("ORD123"))
                .thenReturn(orderResponse("ORD123", AppConstants.OrderStatus.CANCELLED));

        mockMvc.perform(put("/api/cancel/ORD123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORD123"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(orderService).cancelOrder("ORD123");
    }

    @Test
    void getAllOrders_withInvalidPageSize_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private OrderRequest orderRequest() {
        return new OrderRequest(
                "Neeraj Kumar",
                "buyer@example.com",
                "9876543210",
                "123 Main Road, Delhi, India",
                "CASH_ON_DELIVERY",
                List.of(new OrderItemRequest(1L, 2))
        );
    }

    private OrderResponse orderResponse(String orderId, AppConstants.OrderStatus status) {
        return new OrderResponse(
                orderId,
                "Neeraj Kumar",
                "buyer@example.com",
                "9876543210",
                "123 Main Road, Delhi, India",
                AppConstants.PaymentMode.CASH_ON_DELIVERY,
                AppConstants.PaymentStatus.PENDING,
                null,
                null,
                status,
                LocalDate.now(),
                List.of(new OrderItemResponse(
                        "Test Phone",
                        2,
                        new BigDecimal("50000.00")
                ))
        );
    }
}