
import "./App.css";
import React, { useState, useEffect } from "react";
import Home from "./components/Home";
import Navbar from "./components/Navbar";
import Cart from "./components/Cart";
import AddProduct from "./components/AddProduct";
import Product from "./components/Product";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AppProvider } from "./Context/Context";
import UpdateProduct from "./components/UpdateProduct";
import OrderStatus from "./components/OrderStatus";
import Register from "./components/Register";
import Login from "./components/Login";

import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";

function App() {
  const [selectedCategory, setSelectedCategory] = useState("");
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  // ✅ FIX: Always sync with localStorage
  useEffect(() => {
    const checkLogin = () => {
      const token = localStorage.getItem("token");
      setIsLoggedIn(!!token);
    };

    checkLogin();

    // ✅ listen for changes (important)
    window.addEventListener("storage", checkLogin);

    return () => window.removeEventListener("storage", checkLogin);
  }, []);

  const handleCategorySelect = (category) => {
    setSelectedCategory(category);
  };

  const handleLoginSuccess = (token) => {
    localStorage.setItem("token", token);
    setIsLoggedIn(true);
  };

  const handleLogout = () => {
    localStorage.removeItem("token");
    setIsLoggedIn(false);
  };

  // ✅ FIX: check token directly (NOT state only)
  const ProtectedRoute = ({ children }) => {
    const token = localStorage.getItem("token");
    return token ? children : <Navigate to="/login" replace />;
  };

  return (
    <AppProvider>
      <BrowserRouter>

        {/* ✅ Navbar */}
        {isLoggedIn && (
          <Navbar
            onSelectCategory={handleCategorySelect}
            onLogout={handleLogout}
          />
        )}

        <Routes>

          {/* LOGIN */}
          <Route
            path="/login"
            element={
              localStorage.getItem("token") ? (
                <Navigate to="/" />
              ) : (
                <Login onLogin={handleLoginSuccess} />
              )
            }
          />

          {/* REGISTER */}
          <Route
            path="/register"
            element={
              localStorage.getItem("token") ? (
                <Navigate to="/" />
              ) : (
                <Register />
              )
            }
          />

          {/* HOME */}
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Home selectedCategory={selectedCategory} />
              </ProtectedRoute>
            }
          />

          {/* CART */}
          <Route
            path="/cart"
            element={
              <ProtectedRoute>
                <Cart />
              </ProtectedRoute>
            }
          />

          {/* OTHER ROUTES */}
          <Route
            path="/add_product"
            element={
              <ProtectedRoute>
                <AddProduct />
              </ProtectedRoute>
            }
          />

          <Route
            path="/product/:id"
            element={
              <ProtectedRoute>
                <Product />
              </ProtectedRoute>
            }
          />

          <Route
            path="/product/update/:id"
            element={
              <ProtectedRoute>
                <UpdateProduct />
              </ProtectedRoute>
            }
          />

          <Route
            path="/orders"
            element={
              <ProtectedRoute>
                <OrderStatus />
              </ProtectedRoute>
            }
          />

        </Routes>

      </BrowserRouter>
    </AppProvider>
  );
}

export default App;