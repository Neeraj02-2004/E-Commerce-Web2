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
import Wishlist from "./components/Wishlist";


import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";

function App() {
  const [selectedCategory, setSelectedCategory] = useState("");
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  useEffect(() => {
    const checkLogin = () => {
      const token = localStorage.getItem("token");
      setIsLoggedIn(!!token);
    };

    checkLogin();

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
    localStorage.removeItem("role");
    setIsLoggedIn(false);
  };

  const ProtectedRoute = ({ children }) => {
    const token = localStorage.getItem("token");
    return token ? children : <Navigate to="/login" replace />;
  };

  const AdminRoute = ({ children }) => {
    const token = localStorage.getItem("token");
    const role = localStorage.getItem("role");

    if (!token) {
      return <Navigate to="/login" replace />;
    }

    if (role !== "ADMIN") {
      return <Navigate to="/" replace />;
    }

    return children;
  };

  return (
    <AppProvider>
      <BrowserRouter>
        {isLoggedIn && (
          <Navbar
            onSelectCategory={handleCategorySelect}
            onLogout={handleLogout}
          />
        )}

        <Routes>
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

          <Route
            path="/register"
            element={
              localStorage.getItem("token") ? <Navigate to="/" /> : <Register />
            }
          />

          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Home selectedCategory={selectedCategory} />
              </ProtectedRoute>
            }
          />

          <Route
            path="/cart"
            element={
              <ProtectedRoute>
                <Cart />
              </ProtectedRoute>
            }
          />

          <Route
            path="/add_product"
            element={
              <AdminRoute>
                <AddProduct />
              </AdminRoute>
            }
          />

          <Route
            path="/wishlist"
            element={
              <ProtectedRoute>
                  <Wishlist />
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
              <AdminRoute>
                <UpdateProduct />
              </AdminRoute>
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
