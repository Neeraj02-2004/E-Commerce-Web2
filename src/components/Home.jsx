import { useContext, useEffect, useState } from "react";
import { Link } from "react-router-dom";

import AppContext from "../Context/Context";
import unplugged from "../assets/unplugged.png";
import { API_ORIGIN } from "../axios";

const Home = ({ selectedCategory }) => {
  const { data, isError, addToCart, refreshData } = useContext(AppContext);
  const [products, setProducts] = useState([]);
  const [isDataFetched, setIsDataFetched] = useState(false);

  // ✅ ADDED FOR PAGINATION
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [totalPages, setTotalPages] = useState(1);

  const [popup, setPopup] = useState({
    show: false,
    message: "",
    type: "success",
  });

  const isAdmin = localStorage.getItem("role") === "ADMIN";

  const showPopup = (message, type = "success") => {
    setPopup({
      show: true,
      message,
      type,
    });

    setTimeout(() => {
      setPopup({
        show: false,
        message: "",
        type: "success",
      });
    }, 2500);
  };

  useEffect(() => {
    if (!isDataFetched) {
      refreshData(page, size);
      setIsDataFetched(true);
    }
  }, [refreshData, isDataFetched, page, size]);

  // ✅ ADDED: fetch data again when page changes
  useEffect(() => {
    if (isDataFetched) {
      refreshData(page, size);
    }
  }, [page, size]);

  useEffect(() => {
    if (Array.isArray(data)) {
      setProducts(data);
      setTotalPages(1);
      return;
    }

    if (data?.content && Array.isArray(data.content)) {
      setProducts(data.content);
      setTotalPages(data.totalPages || 1);
      return;
    }

    setProducts([]);
    setTotalPages(1);
  }, [data]);

  const getImageUrl = (product) => {
    if (!product.imageUrl) {
      return unplugged;
    }

    if (product.imageUrl.startsWith("http")) {
      return product.imageUrl;
    }

    return `${API_ORIGIN}${product.imageUrl}`;
  };

  const handleAddToCart = (product) => {
    if (!product.productAvailable) {
      showPopup("Product is out of stock", "error");
      return;
    }

    addToCart(product);
    showPopup(`${product.name} added to cart`, "success");
  };

  const filteredProducts = selectedCategory
    ? products.filter((product) => product.category === selectedCategory)
    : products;

  if (isError) {
    return (
      <h2 className="text-center" style={{ padding: "18rem" }}>
        <img
          src={unplugged}
          alt="Error"
          style={{ width: "100px", height: "100px" }}
        />
      </h2>
    );
  }

  return (
    <>
      {popup.show && (
        <div style={popupStyle(popup.type)}>
          <div style={popupIconStyle}>
            {popup.type === "success" ? "✓" : "!"}
          </div>
          <div>{popup.message}</div>
        </div>
      )}

      <div
        className="grid"
        style={{
          marginTop: "64px",
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(250px, 1fr))",
          gap: "20px",
          padding: "20px",
        }}
      >
        {filteredProducts.length === 0 ? (
          <h2
            className="text-center"
            style={{
              display: "flex",
              justifyContent: "center",
              alignItems: "center",
            }}
          >
            No Products Available
          </h2>
        ) : (
          filteredProducts.map((product) => {
            const { id, brand, name, price, productAvailable } = product;

            return (
              <div
                className="card mb-3"
                style={{
                  width: "250px",
                  height: "360px",
                  boxShadow: "0 4px 8px rgba(0,0,0,0.1)",
                  borderRadius: "10px",
                  overflow: "hidden",
                  backgroundColor: productAvailable ? "#fff" : "#ccc",
                  display: "flex",
                  flexDirection: "column",
                  justifyContent: "flex-start",
                  alignItems: "stretch",
                }}
                key={id}
              >
                <Link
                  to={`/product/${id}`}
                  style={{ textDecoration: "none", color: "inherit" }}
                >
                  <img
                    src={getImageUrl(product)}
                    alt={name}
                    style={{
                      width: "100%",
                      height: "150px",
                      objectFit: "cover",
                      padding: "5px",
                      margin: "0",
                      borderRadius: "10px",
                    }}
                  />

                  <div
                    className="card-body"
                    style={{
                      flexGrow: 1,
                      display: "flex",
                      flexDirection: "column",
                      justifyContent: "space-between",
                      padding: "10px",
                    }}
                  >
                    <div>
                      <h5
                        className="card-title"
                        style={{ margin: "0 0 10px 0", fontSize: "1.2rem" }}
                      >
                        {name.toUpperCase()}
                      </h5>

                      <i
                        className="card-brand"
                        style={{ fontStyle: "italic", fontSize: "0.8rem" }}
                      >
                        {"~ " + brand}
                      </i>
                    </div>

                    <hr className="hr-line" style={{ margin: "10px 0" }} />

                    <div className="home-cart-price">
                      <h5
                        className="card-text"
                        style={{
                          fontWeight: "600",
                          fontSize: "1.1rem",
                          marginBottom: "5px",
                        }}
                      >
                        <i className="bi bi-currency-rupee"></i>
                        {price}
                      </h5>
                    </div>

                    {!isAdmin && (
                      <button
                        className="btn-hover color-9"
                        style={{ margin: "10px 25px 0px" }}
                        onClick={(e) => {
                          e.preventDefault();
                          handleAddToCart(product);
                        }}
                        disabled={!productAvailable}
                      >
                        {productAvailable ? "Add to Cart" : "Out of Stock"}
                      </button>
                    )}

                    {isAdmin && (
                      <button
                        className="btn-hover color-9"
                        style={{ margin: "10px 25px 0px" }}
                        onClick={(e) => {
                          e.preventDefault();
                          window.location.href = `/product/${id}`;
                        }}
                      >
                        Manage Product
                      </button>
                    )}
                  </div>
                </Link>
              </div>
            );
          })
        )}
      </div>

      {/* ✅ ADDED PAGINATION BUTTONS */}
      <div style={paginationStyle}>
        <button
          className="btn btn-dark"
          disabled={page === 0}
          onClick={() => setPage((prev) => prev - 1)}
        >
          Previous
        </button>

        <span style={{ fontWeight: "600" }}>
          Page {page + 1} of {totalPages}
        </span>

        <button
          className="btn btn-dark"
          disabled={page + 1 >= totalPages}
          onClick={() => setPage((prev) => prev + 1)}
        >
          Next
        </button>
      </div>
    </>
  );
};

// ✅ ADDED
const paginationStyle = {
  display: "flex",
  justifyContent: "center",
  alignItems: "center",
  gap: "20px",
  margin: "20px 0 40px",
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
};

export default Home;