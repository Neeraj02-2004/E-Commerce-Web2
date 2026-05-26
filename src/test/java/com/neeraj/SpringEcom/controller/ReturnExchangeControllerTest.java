package com.neeraj.SpringEcom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neeraj.SpringEcom.exception.GlobalExceptionHandler;
import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.dto.ReturnExchangeCreateRequest;
import com.neeraj.SpringEcom.model.dto.ReturnExchangeDecisionRequest;
import com.neeraj.SpringEcom.model.dto.ReturnExchangeResponse;
import com.neeraj.SpringEcom.service.JwtService;
import com.neeraj.SpringEcom.service.MyUserDetailsService;
import com.neeraj.SpringEcom.service.ReturnExchangeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReturnExchangeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReturnExchangeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReturnExchangeService returnExchangeService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private MyUserDetailsService myUserDetailsService;

    @Test
    void createRequest_shouldReturnCreatedRequest() throws Exception {
        when(returnExchangeService.createRequest(any(), any(ReturnExchangeCreateRequest.class)))
                .thenReturn(response(AppConstants.ReturnExchangeStatus.REQUESTED));

        ReturnExchangeCreateRequest request = new ReturnExchangeCreateRequest(
                AppConstants.ReturnExchangeType.RETURN,
                "Product is defective and not working"
        );

        mockMvc.perform(post("/api/orders/ORD123/return-exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").value("REX123"))
                .andExpect(jsonPath("$.orderId").value("ORD123"))
                .andExpect(jsonPath("$.requestType").value("RETURN"))
                .andExpect(jsonPath("$.status").value("REQUESTED"));

        verify(returnExchangeService).createRequest(any(), any(ReturnExchangeCreateRequest.class));
    }

    @Test
    void getMyRequests_shouldReturnCurrentUserRequests() throws Exception {
        when(returnExchangeService.getMyRequests())
                .thenReturn(List.of(response(AppConstants.ReturnExchangeStatus.REQUESTED)));

        mockMvc.perform(get("/api/orders/return-exchange"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value("REX123"))
                .andExpect(jsonPath("$[0].orderId").value("ORD123"));

        verify(returnExchangeService).getMyRequests();
    }

    @Test
    void getAllRequests_shouldReturnAllRequestsForAdminRoute() throws Exception {
        when(returnExchangeService.getAllRequests())
                .thenReturn(List.of(response(AppConstants.ReturnExchangeStatus.REQUESTED)));

        mockMvc.perform(get("/api/admin/return-exchange"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value("REX123"));

        verify(returnExchangeService).getAllRequests();
    }

    @Test
    void approveRequest_shouldReturnApprovedRequest() throws Exception {
        when(returnExchangeService.approveRequest(any(), any(ReturnExchangeDecisionRequest.class)))
                .thenReturn(response(AppConstants.ReturnExchangeStatus.APPROVED));

        mockMvc.perform(put("/api/admin/return-exchange/REX123/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReturnExchangeDecisionRequest("Approved"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("REX123"))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(returnExchangeService).approveRequest(any(), any(ReturnExchangeDecisionRequest.class));
    }

    @Test
    void rejectRequest_shouldReturnRejectedRequest() throws Exception {
        when(returnExchangeService.rejectRequest(any(), any(ReturnExchangeDecisionRequest.class)))
                .thenReturn(response(AppConstants.ReturnExchangeStatus.REJECTED));

        mockMvc.perform(put("/api/admin/return-exchange/REX123/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReturnExchangeDecisionRequest("Rejected"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("REX123"))
                .andExpect(jsonPath("$.status").value("REJECTED"));

        verify(returnExchangeService).rejectRequest(any(), any(ReturnExchangeDecisionRequest.class));
    }

    @Test
    void completeRequest_shouldReturnCompletedRequest() throws Exception {
        when(returnExchangeService.completeRequest(any(), any(ReturnExchangeDecisionRequest.class)))
                .thenReturn(response(AppConstants.ReturnExchangeStatus.COMPLETED));

        mockMvc.perform(put("/api/admin/return-exchange/REX123/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReturnExchangeDecisionRequest("Completed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("REX123"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(returnExchangeService).completeRequest(any(), any(ReturnExchangeDecisionRequest.class));
    }

    private ReturnExchangeResponse response(String status) {
        return new ReturnExchangeResponse(
                "REX123",
                "ORD123",
                "buyer@example.com",
                AppConstants.ReturnExchangeType.RETURN,
                "Product is defective and not working",
                status,
                AppConstants.RefundStatus.NOT_REQUIRED,
                "Admin note",
                "rfnd_test_123",
                new BigDecimal("100.00"),
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}