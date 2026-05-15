import API from "../axios";

export const getWishlist = () => {
  return API.get("/wishlist");
};

export const addToWishlist = (productId) => {
  return API.post(`/wishlist/${productId}`);
};

export const removeFromWishlist = (productId) => {
  return API.delete(`/wishlist/${productId}`);
};
