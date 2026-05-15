package com.neeraj.SpringEcom.exception;

public class WishlistItemAlreadyExistsException extends RuntimeException {

    public WishlistItemAlreadyExistsException(int productId) {
        super("Product is already in wishlist: " + productId);
    }
}
