package com.neeraj.SpringEcom.repo;

import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderRepo extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
    Optional<Order> findByOrderId(String orderId);

    @Query(
            value = """
                    select o.id
                    from Order o
                    where o.user.id = :userId
                    order by o.orderDate desc, o.id desc
                    """,
            countQuery = """
                    select count(o)
                    from Order o
                    where o.user.id = :userId
                    """
    )
    Page<Long> findOrderIdsByUserId(
            @Param("userId") Integer userId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
    @Query("select o from Order o where o.id in :ids")
    List<Order> findAllWithItemsByIdIn(@Param("ids") List<Long> ids);

    Optional<Order> findByGatewayOrderId(String gatewayOrderId);

    Optional<Order> findByGatewayPaymentId(String gatewayPaymentId);

    List<Order> findByStatus(AppConstants.OrderStatus status);

    List<Order> findByStatusAndOrderDateBefore(
            AppConstants.OrderStatus status,
            LocalDate orderDate
    );
}