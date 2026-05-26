package com.neeraj.SpringEcom.service;

import com.neeraj.SpringEcom.exception.ProductNotFoundException;
import com.neeraj.SpringEcom.exception.UserNotAuthenticatedException;
import com.neeraj.SpringEcom.exception.WishlistItemAlreadyExistsException;
import com.neeraj.SpringEcom.model.Product;
import com.neeraj.SpringEcom.model.User;
import com.neeraj.SpringEcom.model.WishlistItem;
import com.neeraj.SpringEcom.model.dto.WishlistResponse;
import com.neeraj.SpringEcom.repo.ProductRepo;
import com.neeraj.SpringEcom.repo.UserRepo;
import com.neeraj.SpringEcom.repo.WishlistRepo;
import com.neeraj.SpringEcom.security.CurrentUserProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@Service
public class WishlistService {

    private static final int WISHLIST_CACHE_KEY_HEX_LENGTH = 32;

    private final WishlistRepo wishlistRepo;
    private final ProductRepo productRepo;
    private final UserRepo userRepo;
    private final CurrentUserProvider currentUserProvider;

    public WishlistService(
            WishlistRepo wishlistRepo,
            ProductRepo productRepo,
            UserRepo userRepo,
            CurrentUserProvider currentUserProvider
    ) {
        this.wishlistRepo = wishlistRepo;
        this.productRepo = productRepo;
        this.userRepo = userRepo;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "wishlist", key = "'user:' + #root.target.getWishlistCacheKey()")
    public List<WishlistResponse> getWishlist() {
        User user = getCurrentUser();

        return wishlistRepo.findByUserId(user.getId())
                .stream()
                .map(item -> toResponse(item.getProduct()))
                .toList();
    }

    @Transactional
    @CacheEvict(value = "wishlist", key = "'user:' + #root.target.getWishlistCacheKey()")
    public WishlistResponse addToWishlist(Long productId) {
        User user = getCurrentUser();

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        WishlistItem item = new WishlistItem();
        item.setUser(user);
        item.setUserEmail(user.getEmail());
        item.setProduct(product);

        try {
            wishlistRepo.saveAndFlush(item);
        } catch (DataIntegrityViolationException e) {
            throw new WishlistItemAlreadyExistsException(productId);
        }

        return toResponse(product);
    }

    @Transactional
    @CacheEvict(value = "wishlist", key = "'user:' + #root.target.getWishlistCacheKey()")
    public void removeFromWishlist(Long productId) {
        User user = getCurrentUser();
        wishlistRepo.deleteByUserIdAndProductId(user.getId(), productId);
    }

    public String getWishlistCacheKey() {
        String email = currentUserProvider.getAuthenticatedEmail();
        return sha256Hex(email).substring(0, WISHLIST_CACHE_KEY_HEX_LENGTH);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create wishlist cache key", e);
        }
    }

    private User getCurrentUser() {
        String userEmail = currentUserProvider.getAuthenticatedEmail();

        return userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));
    }

    private WishlistResponse toResponse(Product product) {
        return new WishlistResponse(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory(),
                product.getImageUrl(),
                product.isProductAvailable(),
                product.getStockQuantity()
        );
    }
}