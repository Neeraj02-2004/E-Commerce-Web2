import { useContext, useEffect, useState } from "react";
import AppContext from "../Context/Context";
import axios from "../axios";
import { getWishlist, removeFromWishlist } from "../api/wishlistApi";

const Wishlist = () => {
  const { addToCart } = useContext(AppContext);

  const [wishlist, setWishlist] = useState([]);
  const [loading, setLoading] = useState(true);
  const [popup, setPopup] = useState({
    show: false,
    message: "",
    type: "success",
  });

  const API_ORIGIN = axios.defaults.baseURL.replace(/\/api\/?$/, "");

  useEffect(() => {
    loadWishlist();
  }, []);

  const showPopup = (message, type = "success") => {
    setPopup({ show: true, message, type });

    setTimeout(() => {
      setPopup({ show: false, message: "", type: "success" });
    }, 2500);
  };

  const loadWishlist = async () => {
    try {
      setLoading(true);
      const response = await getWishlist();
      setWishlist(response.data);
    } catch (error) {
      showPopup(
        error.response?.data?.message || "Please login to view wishlist",
        "error"
      );
    } finally {
      setLoading(false);
    }
  };

  const handleAddToCart = (item) => {
    addToCart(item);
    showPopup(`${item.name} added to cart`, "success");
  };

  const handleRemove = async (productId) => {
    try {
      await removeFromWishlist(productId);
      const removedItem = wishlist.find((item) => item.id === productId);
      setWishlist((prev) => prev.filter((item) => item.id !== productId));

      showPopup(
        removedItem
          ? `${removedItem.name} removed from wishlist`
          : "Removed from wishlist",
        "success"
      );
    } catch (error) {
      showPopup(error.response?.data?.message || "Failed to remove item", "error");
    }
  };

  const getImageUrl = (item) => {
    if (!item.imageUrl) {
      return "/placeholder-image.png";
    }

    if (item.imageUrl.startsWith("http")) {
      return item.imageUrl;
    }

    return `${API_ORIGIN}${item.imageUrl}`;
  };

  if (loading) {
    return (
      <h2 className="text-center" style={{ padding: "10rem" }}>
        Loading wishlist...
      </h2>
    );
  }

  return (
    <div className="wishlist-page" style={{ position: "relative" }}>
      {popup.show && (
        <div
          style={{
            position: "fixed",
            top: "80px",
            right: "20px",
            zIndex: 9999,
            minWidth: "260px",
            maxWidth: "360px",
            padding: "14px 18px",
            borderRadius: "10px",
            color: "#fff",
            fontWeight: "600",
            boxShadow: "0 8px 24px rgba(0,0,0,0.2)",
            background: popup.type === "success" ? "#198754" : "#dc3545",
          }}
        >
          {popup.message}
        </div>
      )}

      <h2>My Wishlist</h2>

      {wishlist.length === 0 ? (
        <div style={{ padding: "2rem 0" }}>
          <h4>Your wishlist is empty</h4>
        </div>
      ) : (
        <div className="wishlist-grid">
          {wishlist.map((item) => (
            <div className="wishlist-card" key={item.id}>
              <img
                src={getImageUrl(item)}
                alt={item.name}
                className="wishlist-image"
                onError={(e) => {
                  e.currentTarget.src = "/placeholder-image.png";
                }}
              />

              <div className="wishlist-info">
                <h5>{item.name}</h5>
                <p>{item.brand}</p>
                <strong>₹{item.price}</strong>

                <div className="wishlist-actions">
                  <button
                    className="btn btn-primary"
                    disabled={!item.productAvailable || item.stockQuantity <= 0}
                    onClick={() => handleAddToCart(item)}
                  >
                    Add to Cart
                  </button>

                  <button
                    className="btn btn-outline-danger"
                    onClick={() => handleRemove(item.id)}
                  >
                    Remove
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default Wishlist;