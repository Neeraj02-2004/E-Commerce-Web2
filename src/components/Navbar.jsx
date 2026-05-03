
import React, { useEffect, useState } from "react";
import axios from "axios";

const Navbar = ({ onSelectCategory, onSearch, onLogout }) => {
  const getInitialTheme = () => {
    const storedTheme = localStorage.getItem("theme");
    return storedTheme ? storedTheme : "light-theme";
  };

  const [selectedCategory, setSelectedCategory] = useState("");
  const [theme, setTheme] = useState(getInitialTheme());
  const [input, setInput] = useState("");
  const [searchResults, setSearchResults] = useState([]);
  const [noResults, setNoResults] = useState(false);
  const [showSearchResults, setShowSearchResults] = useState(false);
  const [showDropdown, setShowDropdown] = useState(false);

  useEffect(() => {
    fetchData();
  }, []);

  useEffect(() => {
    document.body.className = theme;
  }, [theme]);

  const fetchData = async () => {
    try {
      const response = await axios.get("http://localhost:8080/api/products");
      setSearchResults(response.data);
    } catch (error) {
      console.error("Error fetching data:", error);
    }
  };

  const handleChange = async (value) => {
    setInput(value);

    if (value.length >= 1) {
      setShowSearchResults(true);

      try {
        const response = await axios.get(
          `http://localhost:8080/api/products/search?keyword=${value}`
        );
        setSearchResults(response.data);
        setNoResults(response.data.length === 0);
      } catch (error) {
        console.error("Error searching:", error);
      }
    } else {
      setShowSearchResults(false);
      setSearchResults([]);
      setNoResults(false);
    }
  };

  const handleCategorySelect = (category) => {
    setSelectedCategory(category);
    onSelectCategory(category);
    setShowDropdown(false);
  };

  const toggleTheme = () => {
    const newTheme = theme === "dark-theme" ? "light-theme" : "dark-theme";
    setTheme(newTheme);
    localStorage.setItem("theme", newTheme);
  };

  const categories = [
    "Laptop",
    "Headphone",
    "Mobile",
    "Electronics",
    "Toys",
    "Fashion",
  ];

  return (
    <header>
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
                <a className="nav-link active" href="/">Home</a>
              </li>

              <li className="nav-item">
                <a className="nav-link" href="/add_product">Add Product</a>
              </li>

              <li className="nav-item">
                <a className="nav-link" href="/orders">Orders</a>
              </li>

              <li className="nav-item dropdown">
                <a
                  className="nav-link dropdown-toggle"
                  href="#"
                  onClick={(e) => {
                    e.preventDefault();
                    setShowDropdown(!showDropdown);
                  }}
                >
                  Categories
                </a>

                <ul className={`dropdown-menu ${showDropdown ? "show" : ""}`}>
                  {categories.map((category) => (
                    <li key={category}>
                      <button
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

              <button className="btn btn-sm btn-outline-dark" onClick={toggleTheme}>
                {theme === "dark-theme" ? "🌙" : "☀️"}
              </button>

              <a href="/cart" className="btn btn-sm btn-outline-primary">
                🛒 Cart
              </a>

              <input
                className="form-control"
                style={{
                  width: "180px",
                  background: theme === "dark-theme" ? "#1e1e1e" : "#fff",
                  color: theme === "dark-theme" ? "#fff" : "#000",
                  border: theme === "dark-theme" ? "1px solid #444" : "1px solid #ced4da",
                }}
                type="search"
                placeholder="Search"
                value={input}
                onChange={(e) => handleChange(e.target.value)}
              />

              <button
                onClick={() => {
                  localStorage.removeItem("token");
                  window.location.href = "/login";
                  if (onLogout) onLogout();
                }}
                className="btn btn-sm btn-danger"
              >
                Logout
              </button>

            </div>
          </div>
        </div>

        {/* SEARCH DROPDOWN (FIXED DARK MODE) */}
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
              border: theme === "dark-theme" ? "1px solid #444" : "1px solid #eee",
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

export default Navbar;