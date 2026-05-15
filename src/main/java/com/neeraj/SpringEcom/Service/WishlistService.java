package com.neeraj.SpringEcom.Service;

import com.neeraj.SpringEcom.exception.ProductNotFoundException;
import com.neeraj.SpringEcom.exception.UserNotAuthenticatedException;
import com.neeraj.SpringEcom.exception.WishlistItemAlreadyExistsException;
import com.neeraj.SpringEcom.model.Product;
import com.neeraj.SpringEcom.model.WishlistItem;
import com.neeraj.SpringEcom.model.dto.WishlistResponse;
import com.neeraj.SpringEcom.repo.ProductRepo;
import com.neeraj.SpringEcom.repo.WishlistRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.DigestUtils;
import java.nio.charset.StandardCharsets;

import java.util.List;

@Service
public class WishlistService {

    private final WishlistRepo wishlistRepo;
    private final ProductRepo productRepo;

    public WishlistService(WishlistRepo wishlistRepo, ProductRepo productRepo) {
        this.wishlistRepo = wishlistRepo;
        this.productRepo = productRepo;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "wishlist", key = "'user:' + #root.target.getWishlistCacheKey()")
    public List<WishlistResponse> getWishlist() {
        String userEmail = getAuthenticatedEmail();

        return wishlistRepo.findByUserEmail(userEmail)
                .stream()
                .map(item -> toResponse(item.getProduct()))
                .toList();
    }

    @Transactional
    @CacheEvict(value = "wishlist", key = "'user:' + #root.target.getWishlistCacheKey()")
    public WishlistResponse addToWishlist(int productId) {
        String userEmail = getAuthenticatedEmail();

        if (wishlistRepo.existsByUserEmailAndProductId(userEmail, productId)) {
            throw new WishlistItemAlreadyExistsException(productId);
        }

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        WishlistItem item = new WishlistItem();
        item.setUserEmail(userEmail);
        item.setProduct(product);

        wishlistRepo.save(item);

        return toResponse(product);
    }

    @Transactional
    @CacheEvict(value = "wishlist", key = "'user:' + #root.target.getWishlistCacheKey()")
    public void removeFromWishlist(int productId) {
        String userEmail = getAuthenticatedEmail();
        wishlistRepo.deleteByUserEmailAndProductId(userEmail, productId);
    }

    public String getWishlistCacheKey() {
        String email = getAuthenticatedEmail();
        return DigestUtils.md5DigestAsHex(email.getBytes(StandardCharsets.UTF_8));
    }

    private String getAuthenticatedEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new UserNotAuthenticatedException("User not authenticated");
        }

        return auth.getName().toLowerCase().trim();
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
