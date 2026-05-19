package com.neeraj.SpringEcom.repo;

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
                    where o.userEmail = :userEmail
                    order by o.orderDate desc, o.id desc
                    """,
            countQuery = """
                    select count(o)
                    from Order o
                    where o.userEmail = :userEmail
                    """
    )
    Page<Long> findOrderIdsByUserEmail(
            @Param("userEmail") String userEmail,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
    @Query("select o from Order o where o.id in :ids")
    List<Order> findAllWithItemsByIdIn(@Param("ids") List<Long> ids);

    Optional<Order> findByGatewayOrderId(String gatewayOrderId);


    List<Order> findByStatus(String status);

    List<Order> findByStatusAndOrderDateBefore(String status, LocalDate orderDate);

}
