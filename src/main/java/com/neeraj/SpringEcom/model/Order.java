package com.neeraj.SpringEcom.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_user_email", columnList = "userEmail")
        }
)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String orderId;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number")
    @Size(min = 10, max = 10, message = "Mobile number must be 10 digits")
    @Column(nullable = false, length = 10)
    private String mobileNo;

    @NotBlank(message = "Address is required")
    @Size(min = 10, max = 500, message = "Address must be between 10 and 500 characters")
    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false, length = 50)
    private String paymentMode;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDate orderDate;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String paymentStatus;

    private String gatewayOrderId;

    private String gatewayPaymentId;

    @Column(length = 500)
    private String gatewaySignature;

    private LocalDateTime paidAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;
}