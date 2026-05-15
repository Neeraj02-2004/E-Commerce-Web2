package com.neeraj.SpringEcom.repo;

import com.neeraj.SpringEcom.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepo extends JpaRepository<WishlistItem, Long> {

    List<WishlistItem> findByUserEmail(String userEmail);

    Optional<WishlistItem> findByUserEmailAndProductId(String userEmail, int productId);

    boolean existsByUserEmailAndProductId(String userEmail, int productId);

    void deleteByUserEmailAndProductId(String userEmail, int productId);
}
