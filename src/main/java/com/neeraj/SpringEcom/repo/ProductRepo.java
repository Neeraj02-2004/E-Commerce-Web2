package com.neeraj.SpringEcom.repo;

import com.neeraj.SpringEcom.model.Product;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepo extends JpaRepository<Product, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")
    })
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") int id);

    @Query(
            value = """
                    SELECT *
                    FROM product
                    WHERE product_available = true
                      AND search_vector @@ websearch_to_tsquery('english', :keyword)
                    ORDER BY ts_rank(search_vector, websearch_to_tsquery('english', :keyword)) DESC, id DESC
                    """,
            nativeQuery = true
    )
    List<Product> searchProducts(@Param("keyword") String keyword);
}