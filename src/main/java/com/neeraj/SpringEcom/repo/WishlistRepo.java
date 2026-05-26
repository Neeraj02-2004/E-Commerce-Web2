package com.neeraj.SpringEcom.repo;

import com.neeraj.SpringEcom.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepo extends JpaRepository<WishlistItem, Long> {

    List<WishlistItem> findByUserId(Integer userId);

    Optional<WishlistItem> findByUserIdAndProductId(Integer userId, Long productId);

    void deleteByUserIdAndProductId(Integer userId, Long productId);
}