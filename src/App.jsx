import "./App.css";
import { useState, useEffect } from "react";
import Home from "./components/Home";
import Navbar from "./components/Navbar";
import Cart from "./components/Cart";
import AddProduct from "./components/AddProduct";
import Product from "./components/Product";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import UpdateProduct from "./components/UpdateProduct";
import OrderStatus from "./components/OrderStatus";
import Register from "./components/Register";
import Login from "./components/Login";
import Wishlist from "./components/Wishlist";
import AdminReturnExchange from "./components/AdminReturnExchange";

import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";

function App() {
  const [selectedCategory, setSelectedCategory] = useState("");
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  const getToken = () => localStorage.getItem("token");
  const getRole = () => localStorage.getItem("role");
  const isAdmin = () => getRole() === "ADMIN";
  const isUser = () => getRole() === "USER";

  useEffect(() => {
    const checkLogin = () => {
      setIsLoggedIn(Boolean(getToken()));
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
    localStorage.removeItem("username");
    localStorage.removeItem("email");
    setIsLoggedIn(false);
  };

  const ProtectedRoute = ({ children }) => {
    return getToken() ? children : <Navigate to="/login" replace />;
  };

  const UserRoute = ({ children }) => {
    if (!getToken()) {
      return <Navigate to="/login" replace />;
    }

    if (!isUser()) {
      return <Navigate to="/" replace />;
    }

    return children;
  };

  const AdminRoute = ({ children }) => {
    if (!getToken()) {
      return <Navigate to="/login" replace />;
    }

    if (!isAdmin()) {
      return <Navigate to="/" replace />;
    }

    return children;
  };

  return (
    <BrowserRouter
      future={{
        v7_startTransition: true,
        v7_relativeSplatPath: true,
      }}
    >
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
            getToken() ? (
              <Navigate to="/" replace />
            ) : (
              <Login onLogin={handleLoginSuccess} />
            )
          }
        />

        <Route
          path="/register"
          element={
            getToken() ? <Navigate to="/" replace /> : <Register />
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
            <UserRoute>
              <Cart />
            </UserRoute>
          }
        />

        <Route
          path="/wishlist"
          element={
            <UserRoute>
              <Wishlist />
            </UserRoute>
          }
        />

        <Route
          path="/orders"
          element={
            <UserRoute>
              <OrderStatus />
            </UserRoute>
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
          path="/add_product"
          element={
            <AdminRoute>
              <AddProduct />
            </AdminRoute>
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
          path="/admin/return-exchange"
          element={
            <AdminRoute>
              <AdminReturnExchange />
            </AdminRoute>
          }
        />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;