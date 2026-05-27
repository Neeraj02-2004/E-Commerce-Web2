import { useNavigate, useParams } from "react-router-dom";
import { useContext, useEffect, useState } from "react";
import AppContext from "../Context/Context";
import axios, { API_ORIGIN } from "../axios";
import {
  addToWishlist,
  getWishlist,
  removeFromWishlist,
} from "../api/wishlistApi";

const Product = () => {
  const { id } = useParams();
  const { addToCart, removeFromCart, refreshData } = useContext(AppContext);

  const [product, setProduct] = useState(null);
  const [imageUrl, setImageUrl] = useState("");
  const [isWishlisted, setIsWishlisted] = useState(false);
  const [wishlistLoading, setWishlistLoading] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [popup, setPopup] = useState({
    show: false,
    message: "",
    type: "success",
  });

  const navigate = useNavigate();

  const token = localStorage.getItem("token");
  const isAdmin = localStorage.getItem("role") === "ADMIN";
  const isLoggedIn = Boolean(token);
  const isCustomer = isLoggedIn && !isAdmin;

  const showPopup = (message, type = "success") => {
    setPopup({ show: true, message, type });

    setTimeout(() => {
      setPopup({ show: false, message: "", type: "success" });
    }, 2500);
  };

  useEffect(() => {
    const fetchProduct = async () => {
      try {
        const response = await axios.get(`/product/${id}`);
        setProduct(response.data);

        if (response.data.imageUrl) {
          if (response.data.imageUrl.startsWith("http")) {
            setImageUrl(response.data.imageUrl);
          } else {
            setImageUrl(`${API_ORIGIN}${response.data.imageUrl}`);
          }
        }
      } catch (error) {
        console.error("Error fetching product:", error);
        showPopup(
          error.response?.data?.message || "Failed to load product",
          "error"
        );
      }
    };

    const checkWishlist = async () => {
      if (!isCustomer) return;

      try {
        const response = await getWishlist();
        const found = response.data.some(
          (item) => Number(item.id) === Number(id)
        );
        setIsWishlisted(found);
      } catch (error) {
        console.error("Error checking wishlist:", error);
      }
    };

    fetchProduct();
    checkWishlist();
  }, [id, isCustomer]);

  const openDeleteConfirm = () => {
    if (!token) {
      showPopup("Please login first", "error");
      return;
    }

    if (!isAdmin) {
      showPopup("Only admin can delete products", "error");
      return;
    }

    setShowDeleteConfirm(true);
  };

  const deleteProduct = async () => {
    try {
      setShowDeleteConfirm(false);

      await axios.delete(`/admin/product/${id}`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      removeFromCart(Number(id));
      showPopup("Product disabled successfully", "success");
      refreshData();

      setTimeout(() => {
        navigate("/");
      }, 900);
    } catch (error) {
      console.error("Error deleting product:", error);

      const message =
        error.response?.data?.message ||
        error.response?.data?.error ||
        error.response?.data ||
        "Error deleting product";

      showPopup(String(message), "error");
    }
  };

  const handleEditClick = () => {
    if (!token || !isAdmin) {
      showPopup("Only admin can update products", "error");
      return;
    }

    navigate(`/product/update/${id}`);
  };

  const handleAddToCart = () => {
    if (!isCustomer) {
      showPopup("Admin cannot order products", "error");
      return;
    }

    if (!product.productAvailable) {
      showPopup("Product is out of stock", "error");
      return;
    }

    addToCart(product);
    showPopup(`${product.name} added to cart`, "success");
  };

  const handleWishlistClick = async () => {
    if (!isCustomer) {
      showPopup("Admin cannot use wishlist", "error");
      return;
    }

    if (!isLoggedIn) {
      showPopup("Please login to use wishlist", "error");

      setTimeout(() => {
        navigate("/login");
      }, 900);

      return;
    }

    if (wishlistLoading) return;

    try {
      setWishlistLoading(true);

      if (isWishlisted) {
        await removeFromWishlist(product.id);
        setIsWishlisted(false);
        showPopup(`${product.name} removed from wishlist`, "success");
      } else {
        await addToWishlist(product.id);
        setIsWishlisted(true);
        showPopup(`${product.name} added to wishlist`, "success");
      }
    } catch (error) {
      console.error("Wishlist error:", error);
      showPopup(
        error.response?.data?.message || "Wishlist action failed",
        "error"
      );
    } finally {
      setWishlistLoading(false);
    }
  };

  if (!product) {
    return (
      <>
        {popup.show && (
          <div style={popupStyle(popup.type)}>
            <div style={popupIconStyle}>
              {popup.type === "success" ? "OK" : "!"}
            </div>
            <div>{popup.message}</div>
          </div>
        )}

        <h2 className="text-center" style={{ padding: "10rem" }}>
          Loading...
        </h2>
      </>
    );
  }

  return (
    <>
      {popup.show && (
        <div style={popupStyle(popup.type)}>
          <div style={popupIconStyle}>
            {popup.type === "success" ? "OK" : "!"}
          </div>
          <div>{popup.message}</div>
        </div>
      )}

      {showDeleteConfirm && (
        <div style={confirmOverlayStyle}>
          <div style={confirmBoxStyle}>
            <div style={confirmIconStyle}>!</div>

            <h3 style={{ marginBottom: "10px" }}>Delete Product?</h3>

            <p style={{ color: "#666", marginBottom: "22px" }}>
              Are you sure you want to delete{" "}
              <strong>{product.name}</strong>? This action cannot be undone.
            </p>

            <div
              style={{
                display: "flex",
                gap: "12px",
                justifyContent: "center",
              }}
            >
              <button
                type="button"
                onClick={() => setShowDeleteConfirm(false)}
                style={cancelButtonStyle}
              >
                Cancel
              </button>

              <button
                type="button"
                onClick={deleteProduct}
                style={deleteButtonStyle}
              >
                Yes, Delete
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="containers" style={{ display: "flex" }}>
        {imageUrl && (
          <img
            className="left-column-img"
            src={imageUrl}
            alt={product.imageName || product.name}
            style={{ width: "50%", height: "auto" }}
            onError={(e) => {
              e.currentTarget.src = "/placeholder-image.png";
            }}
          />
        )}

        <div className="right-column" style={{ width: "50%" }}>
          <div className="product-description">
            <div style={{ display: "flex", justifyContent: "space-between" }}>
              <span style={{ fontSize: "1.2rem", fontWeight: "lighter" }}>
                {product.category}
              </span>

              <div className="release-date" style={{ marginBottom: "2rem" }}>
                <h6>
                  Listed :{" "}
                  <span>
                    <i>
                      {product.releaseDate
                        ? new Date(product.releaseDate).toLocaleDateString()
                        : "Not set"}
                    </i>
                  </span>
                </h6>
              </div>
            </div>

            <h1
              style={{
                fontSize: "2rem",
                marginBottom: "0.5rem",
                textTransform: "capitalize",
                letterSpacing: "1px",
              }}
            >
              {product.name}
            </h1>

            <i style={{ marginBottom: "3rem" }}>{product.brand}</i>

            <p
              style={{
                fontWeight: "bold",
                fontSize: "1rem",
                margin: "10px 0px 0px",
              }}
            >
              PRODUCT DESCRIPTION :
            </p>

            <p style={{ marginBottom: "1rem" }}>{product.description}</p>
          </div>

          <div className="product-price">
            <span style={{ fontSize: "2rem", fontWeight: "bold" }}>
              {"₹" + product.price}
            </span>

            {isCustomer && (
              <div style={{ display: "flex", gap: "1rem", flexWrap: "wrap" }}>
                <button
                  className={`cart-btn ${
                    !product.productAvailable ? "disabled-btn" : ""
                  }`}
                  onClick={handleAddToCart}
                  disabled={!product.productAvailable}
                  style={{
                    padding: "1rem 2rem",
                    fontSize: "1rem",
                    backgroundColor: "#007bff",
                    color: "white",
                    border: "none",
                    borderRadius: "5px",
                    cursor: product.productAvailable
                      ? "pointer"
                      : "not-allowed",
                    marginBottom: "1rem",
                  }}
                >
                  {product.productAvailable ? "Add to cart" : "Out of Stock"}
                </button>

                <button
                  type="button"
                  onClick={handleWishlistClick}
                  disabled={wishlistLoading}
                  style={{
                    padding: "1rem 2rem",
                    fontSize: "1rem",
                    fontWeight: "600",
                    backgroundColor: isWishlisted ? "#e91e63" : "#ffe4ef",
                    color: isWishlisted ? "white" : "#c2185b",
                    border: "1px solid #e91e63",
                    borderRadius: "8px",
                    cursor: wishlistLoading ? "not-allowed" : "pointer",
                    marginBottom: "1rem",
                    boxShadow: "0 4px 12px rgba(233, 30, 99, 0.25)",
                    transition: "all 0.2s ease",
                  }}
                >
                  {wishlistLoading
                    ? "Please wait..."
                    : isWishlisted
                    ? "♥ Wishlisted"
                    : "♡ Add to Wishlist"}
                </button>
              </div>
            )}

            {isAdmin && (
              <div
                style={{
                  padding: "12px",
                  background: "#f8f9fa",
                  borderRadius: "8px",
                  margin: "12px 0",
                  color: "#555",
                  border: "1px solid #e5e7eb",
                }}
              >
                Admin mode: manage product details, stock, image, or availability.
              </div>
            )}

            <h6 style={{ marginBottom: "1rem" }}>
              Stock Available :{" "}
              <i style={{ color: "green", fontWeight: "bold" }}>
                {product.stockQuantity}
              </i>
            </h6>
          </div>

          {isAdmin && (
            <div
              className="update-button"
              style={{ display: "flex", gap: "1rem" }}
            >
              <button
                className="btn btn-primary"
                type="button"
                onClick={handleEditClick}
                style={{
                  padding: "1rem 2rem",
                  fontSize: "1rem",
                  backgroundColor: "#007bff",
                  color: "white",
                  border: "none",
                  borderRadius: "5px",
                  cursor: "pointer",
                }}
              >
                Update
              </button>

              <button
                className="btn btn-primary"
                type="button"
                onClick={openDeleteConfirm}
                style={{
                  padding: "1rem 2rem",
                  fontSize: "1rem",
                  backgroundColor: "#dc3545",
                  color: "white",
                  border: "none",
                  borderRadius: "5px",
                  cursor: "pointer",
                }}
              >
                Delete
              </button>
            </div>
          )}
        </div>
      </div>
    </>
  );
};

const popupStyle = (type) => ({
  position: "fixed",
  top: "80px",
  right: "24px",
  zIndex: 99999,
  minWidth: "280px",
  maxWidth: "380px",
  padding: "14px 18px",
  borderRadius: "14px",
  color: "#fff",
  fontWeight: "600",
  display: "flex",
  alignItems: "center",
  gap: "12px",
  boxShadow: "0 12px 30px rgba(0,0,0,0.25)",
  background:
    type === "success"
      ? "linear-gradient(135deg, #16a34a, #22c55e)"
      : "linear-gradient(135deg, #dc2626, #f97316)",
});

const popupIconStyle = {
  width: "28px",
  height: "28px",
  borderRadius: "50%",
  background: "rgba(255,255,255,0.25)",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  fontWeight: "bold",
  flexShrink: 0,
  fontSize: "12px",
};

const confirmOverlayStyle = {
  position: "fixed",
  inset: 0,
  background: "rgba(0,0,0,0.45)",
  zIndex: 99998,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  padding: "20px",
};

const confirmBoxStyle = {
  width: "100%",
  maxWidth: "420px",
  background: "#fff",
  borderRadius: "18px",
  padding: "28px",
  textAlign: "center",
  boxShadow: "0 20px 60px rgba(0,0,0,0.35)",
};

const confirmIconStyle = {
  width: "54px",
  height: "54px",
  borderRadius: "50%",
  background: "linear-gradient(135deg, #dc2626, #f97316)",
  color: "#fff",
  fontSize: "28px",
  fontWeight: "bold",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  margin: "0 auto 16px",
};

const cancelButtonStyle = {
  padding: "10px 18px",
  borderRadius: "10px",
  border: "1px solid #ddd",
  background: "#f8f9fa",
  color: "#333",
  fontWeight: "600",
  cursor: "pointer",
};

const deleteButtonStyle = {
  padding: "10px 18px",
  borderRadius: "10px",
  border: "none",
  background: "linear-gradient(135deg, #dc2626, #ef4444)",
  color: "#fff",
  fontWeight: "700",
  cursor: "pointer",
};

export default Product;