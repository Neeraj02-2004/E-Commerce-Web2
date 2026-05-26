package com.neeraj.SpringEcom.exception;

public class WishlistItemAlreadyExistsException extends RuntimeException {

    public WishlistItemAlreadyExistsException(Long productId) {
        super("Product is already in wishlist: " + productId);
    }
}