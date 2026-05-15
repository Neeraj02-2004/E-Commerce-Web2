//package com.neeraj.SpringEcom.controller;
//
//import com.neeraj.SpringEcom.Service.WishlistService;
//import com.neeraj.SpringEcom.model.dto.WishlistResponse;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/wishlist")
//public class WishlistController {
//
//    private final WishlistService wishlistService;
//
//    public WishlistController(WishlistService wishlistService) {
//        this.wishlistService = wishlistService;
//    }
//
//    @GetMapping
//    public ResponseEntity<List<WishlistResponse>> getWishlist() {
//        return ResponseEntity.ok(wishlistService.getWishlist());
//    }
//
//    @PostMapping("/{productId}")
//    public ResponseEntity<WishlistResponse> addToWishlist(@PathVariable int productId) {
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(wishlistService.addToWishlist(productId));
//    }
//
//    @DeleteMapping("/{productId}")
//    public ResponseEntity<Map<String, String>> removeFromWishlist(@PathVariable int productId) {
//        wishlistService.removeFromWishlist(productId);
//        return ResponseEntity.ok(Map.of("message", "Removed from wishlist"));
//    }
//}






package com.neeraj.SpringEcom.controller;

import com.neeraj.SpringEcom.Service.WishlistService;
import com.neeraj.SpringEcom.model.dto.WishlistResponse;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public ResponseEntity<List<WishlistResponse>> getWishlist() {
        return ResponseEntity.ok(wishlistService.getWishlist());
    }

    @PostMapping("/{productId}")
    public ResponseEntity<WishlistResponse> addToWishlist(
            @PathVariable @Min(value = 1, message = "Product id must be valid") int productId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(wishlistService.addToWishlist(productId));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Map<String, String>> removeFromWishlist(
            @PathVariable @Min(value = 1, message = "Product id must be valid") int productId
    ) {
        wishlistService.removeFromWishlist(productId);
        return ResponseEntity.ok(Map.of("message", "Removed from wishlist"));
    }
}

