import { useEffect, useState } from "react";
import axios, { API_ORIGIN } from "../axios";

const Navbar = ({ onSelectCategory, onLogout }) => {
  const getInitialTheme = () => {
    const storedTheme = localStorage.getItem("theme");
    return storedTheme ? storedTheme : "light-theme";
  };

  const getUserInfo = () => {
    const username = localStorage.getItem("username") || "User";
    const email = localStorage.getItem("email") || "No email found";

    return { username, email };
  };

  const [, setSelectedCategory] = useState("");
  const [theme, setTheme] = useState(getInitialTheme());
  const [input, setInput] = useState("");
  const [searchResults, setSearchResults] = useState([]);
  const [noResults, setNoResults] = useState(false);
  const [showSearchResults, setShowSearchResults] = useState(false);
  const [showDropdown, setShowDropdown] = useState(false);
  const [showProfileDropdown, setShowProfileDropdown] = useState(false);
  const [userInfo, setUserInfo] = useState(getUserInfo());
  const [popup, setPopup] = useState({
    show: false,
    message: "",
    type: "success",
  });

  const isAdmin = localStorage.getItem("role") === "ADMIN";
  const isLoggedIn = Boolean(localStorage.getItem("token"));

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

  const fetchData = async () => {
    try {
      const response = await axios.get(`${API_ORIGIN}/api/products`);
      setSearchResults(response.data);
    } catch (error) {
      console.error("Error fetching data:", error);
      showPopup("Failed to load products", "error");
    }
  };

  useEffect(() => {
    fetchData();
    setUserInfo(getUserInfo());
  }, []);

  useEffect(() => {
    document.body.className = theme;
  }, [theme]);

  const handleChange = async (value) => {
    setInput(value);

    if (value.length >= 1) {
      setShowSearchResults(true);

      try {
        const response = await axios.get(
          `${API_ORIGIN}/api/products/search?keyword=${value}`
        );

        setSearchResults(response.data);
        setNoResults(response.data.length === 0);
      } catch (error) {
        console.error("Error searching:", error);
        setSearchResults([]);
        setNoResults(true);
        showPopup("Search failed. Please try again", "error");
      }
    } else {
      setShowSearchResults(false);
      setSearchResults([]);
      setNoResults(false);
    }
  };

  const handleCategorySelect = (category) => {
    setSelectedCategory(category);

    if (onSelectCategory) {
      onSelectCategory(category);
    }

    setShowDropdown(false);
  };

  const toggleTheme = () => {
    const newTheme = theme === "dark-theme" ? "light-theme" : "dark-theme";
    setTheme(newTheme);
    localStorage.setItem("theme", newTheme);
  };

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    localStorage.removeItem("username");
    localStorage.removeItem("email");

    if (onLogout) {
      onLogout();
    }

    showPopup("Logged out successfully", "success");

    setTimeout(() => {
      window.location.href = "/login";
    }, 900);
  };

  const categories = [
    "Laptop",
    "Headphone",
    "Mobile",
    "Electronics",
    "Toys",
    "Fashion",
    "Boys Footwear",
    "Girls Footwear",
    "Mens Suit",
    "Mens Shirt",
    "Mens pants",
    "Boys Wear",
    "Girls Wear",
    "Girls Top",
    "Girls pants",
    "Womens Suit",
    "Womens Saree",
    "Child Wear",
    "Grocery",
  ];

  return (
    <header>
      {popup.show && (
        <div style={popupStyle(popup.type)}>
          <div style={popupIconStyle}>
            {popup.type === "success" ? "✓" : "!"}
          </div>
          <div>{popup.message}</div>
        </div>
      )}

      <nav className={`navbar navbar-expand-lg fixed-top ${theme}`}>
        <div className="container-fluid">
          <span className="navbar-brand">MyStore</span>

          <button
            className="navbar-toggler"
            type="button"
            data-bs-toggle="collapse"
            data-bs-target="#navbarSupportedContent"
          >
            <span className="navbar-toggler-icon"></span>
          </button>

          <div className="collapse navbar-collapse" id="navbarSupportedContent">
            <ul className="navbar-nav me-auto mb-2 mb-lg-0">
              <li className="nav-item">
                <a className="nav-link active" href="/">
                  Home
                </a>
              </li>

              {isAdmin && (
                <li className="nav-item">
                  <a className="nav-link" href="/add_product">
                    Add Product
                  </a>
                </li>
              )}

              {isLoggedIn && (
                <li className="nav-item">
                  <a className="nav-link" href="/orders">
                    Orders
                  </a>
                </li>
              )}

              <li className="nav-item dropdown">
                <a
                  className="nav-link dropdown-toggle"
                  href="/"
                  role="button"
                  onClick={(e) => {
                    e.preventDefault();
                    setShowDropdown(!showDropdown);
                    setShowProfileDropdown(false);
                  }}
                >
                  Categories
                </a>

                <ul className={`dropdown-menu ${showDropdown ? "show" : ""}`}>
                  {categories.map((category) => (
                    <li key={category}>
                      <button
                        type="button"
                        className="dropdown-item"
                        onClick={() => handleCategorySelect(category)}
                      >
                        {category}
                      </button>
                    </li>
                  ))}
                </ul>
              </li>
            </ul>

            <div className="d-flex align-items-center gap-2">
              <button
                type="button"
                className="btn btn-sm btn-outline-dark"
                onClick={toggleTheme}
              >
                {theme === "dark-theme" ? "🌙" : "☀️"}
              </button>

              {isLoggedIn && (
                <a href="/wishlist" className="btn btn-sm btn-outline-danger">
                  ♡ Wishlist
                </a>
              )}

              {isLoggedIn && (
                <a href="/cart" className="btn btn-sm btn-outline-primary">
                  🛒 Cart
                </a>
              )}

              <input
                className="form-control"
                style={{
                  width: "180px",
                  background: theme === "dark-theme" ? "#1e1e1e" : "#fff",
                  color: theme === "dark-theme" ? "#fff" : "#000",
                  border:
                    theme === "dark-theme"
                      ? "1px solid #444"
                      : "1px solid #ced4da",
                }}
                type="search"
                placeholder="Search"
                value={input}
                onChange={(e) => handleChange(e.target.value)}
              />

              {isLoggedIn ? (
                <div className="dropdown position-relative">
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-secondary dropdown-toggle"
                    onClick={() => {
                      setShowProfileDropdown(!showProfileDropdown);
                      setShowDropdown(false);
                    }}
                  >
                    Profile
                  </button>

                  <div
                    className={`dropdown-menu dropdown-menu-end p-3 ${
                      showProfileDropdown ? "show" : ""
                    }`}
                    style={{
                      minWidth: "240px",
                      right: 0,
                      left: "auto",
                      background: theme === "dark-theme" ? "#1e1e1e" : "#fff",
                      color: theme === "dark-theme" ? "#fff" : "#000",
                      border:
                        theme === "dark-theme"
                          ? "1px solid #444"
                          : "1px solid #ddd",
                    }}
                  >
                    <div style={{ marginBottom: "10px" }}>
                      <strong>{userInfo.username}</strong>
                    </div>

                    <div
                      style={{
                        fontSize: "14px",
                        color: theme === "dark-theme" ? "#ccc" : "#666",
                        marginBottom: "12px",
                        wordBreak: "break-word",
                      }}
                    >
                      {userInfo.email}
                    </div>

                    <button
                      type="button"
                      onClick={handleLogout}
                      className="btn btn-sm btn-danger w-100"
                    >
                      Logout
                    </button>
                  </div>
                </div>
              ) : (
                <a href="/login" className="btn btn-sm btn-success">
                  Login
                </a>
              )}
            </div>
          </div>
        </div>

        {showSearchResults && (
          <div
            className="position-absolute shadow-lg"
            style={{
              top: "60px",
              right: "20px",
              width: "280px",
              background: theme === "dark-theme" ? "#1e1e1e" : "#fff",
              color: theme === "dark-theme" ? "#fff" : "#000",
              borderRadius: "12px",
              padding: "10px",
              maxHeight: "300px",
              overflowY: "auto",
              zIndex: 9999,
              border:
                theme === "dark-theme" ? "1px solid #444" : "1px solid #eee",
            }}
          >
            {searchResults.length > 0 ? (
              searchResults.map((result) => (
                <div
                  key={result.id}
                  style={{
                    padding: "10px",
                    borderRadius: "8px",
                    cursor: "pointer",
                    transition: "0.2s",
                  }}
                  onMouseEnter={(e) =>
                    (e.currentTarget.style.background =
                      theme === "dark-theme" ? "#333" : "#f5f5f5")
                  }
                  onMouseLeave={(e) =>
                    (e.currentTarget.style.background = "transparent")
                  }
                >
                  <a
                    href={`/product/${result.id}`}
                    style={{
                      textDecoration: "none",
                      color: theme === "dark-theme" ? "#fff" : "#333",
                      fontWeight: 500,
                    }}
                  >
                    {result.name}
                  </a>
                </div>
              ))
            ) : (
              noResults && (
                <p style={{ margin: 0, textAlign: "center", color: "gray" }}>
                  No Product Found
                </p>
              )
            )}
          </div>
        )}
      </nav>
    </header>
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
};

export default Navbar;