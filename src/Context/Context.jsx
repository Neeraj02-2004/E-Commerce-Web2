import axios from "../axios";
import { useState, useEffect, createContext } from "react";

const AppContext = createContext({
  data: [],
  isError: "",
  cart: [],
  isAdmin: false,
  token: null,
  role: "USER",
  addToCart: () => {},
  removeFromCart: () => {},
  updateCartItemQuantity: () => {},
  refreshData: () => {},
  clearCart: () => {},
  removeMultipleFromCart: () => {},
  refreshAuth: () => {},
});

const getUserCartKey = () => {
  const token = localStorage.getItem("token");

  if (!token) {
    return "cart:guest";
  }

  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    const userId = payload.sub || payload.email || payload.username;

    return userId ? `cart:${userId}` : "cart:guest";
  } catch {
    return "cart:guest";
  }
};

const getStoredCart = () => {
  try {
    return JSON.parse(localStorage.getItem(getUserCartKey())) || [];
  } catch {
    return [];
  }
};

export const AppProvider = ({ children }) => {
  const [data, setData] = useState([]);
  const [isError, setIsError] = useState("");

  const [token, setToken] = useState(localStorage.getItem("token"));
  const [role, setRole] = useState(localStorage.getItem("role") || "USER");
  const [cartKey, setCartKey] = useState(getUserCartKey());
  const [cart, setCart] = useState(getStoredCart);

  const isAdmin = role === "ADMIN";

  const refreshAuth = () => {
    const nextToken = localStorage.getItem("token");
    const nextRole = localStorage.getItem("role") || "USER";
    const nextCartKey = getUserCartKey();

    setToken(nextToken);
    setRole(nextRole);
    setCartKey(nextCartKey);

    try {
      setCart(JSON.parse(localStorage.getItem(nextCartKey)) || []);
    } catch {
      setCart([]);
    }
  };

  const saveCart = (updatedCart) => {
    setCart(updatedCart);
    localStorage.setItem(cartKey, JSON.stringify(updatedCart));
  };

  const addToCart = (product) => {
    const existingIndex = cart.findIndex(
      (item) => Number(item.id) === Number(product.id)
    );

    let updatedCart;

    if (existingIndex !== -1) {
      updatedCart = cart.map((item, i) =>
        i === existingIndex
          ? {
              ...item,
              quantity: Math.min(
                item.quantity + 1,
                item.stockQuantity || item.quantity + 1
              ),
            }
          : item
      );
    } else {
      updatedCart = [...cart, { ...product, quantity: 1 }];
    }

    saveCart(updatedCart);
  };

  const removeFromCart = (productId) => {
    const updatedCart = cart.filter(
      (item) => Number(item.id) !== Number(productId)
    );

    saveCart(updatedCart);
  };

  const removeMultipleFromCart = (ids) => {
    const normalizedIds = ids.map(Number);

    const updatedCart = cart.filter(
      (item) => !normalizedIds.includes(Number(item.id))
    );

    saveCart(updatedCart);
  };

  const updateCartItemQuantity = (productId, quantity) => {
    const updatedCart = cart.map((item) =>
      Number(item.id) === Number(productId)
        ? { ...item, quantity: Math.max(1, Number(quantity)) }
        : item
    );

    saveCart(updatedCart);
  };

  const clearCart = () => {
    setCart([]);
    localStorage.removeItem(cartKey);
  };

  const refreshData = async () => {
    try {
      const response = await axios.get("/products?page=0&size=20");

      const products = Array.isArray(response.data)
        ? response.data
        : response.data?.content || [];

      setData(products);
      setIsError("");
    } catch (error) {
      setIsError(error.message);
    }
  };

  useEffect(() => {
    refreshData();
    refreshAuth();
  }, []);

  useEffect(() => {
    const handleStorageChange = () => {
      refreshAuth();
    };

    window.addEventListener("storage", handleStorageChange);
    window.addEventListener("authChanged", handleStorageChange);

    return () => {
      window.removeEventListener("storage", handleStorageChange);
      window.removeEventListener("authChanged", handleStorageChange);
    };
  }, []);

  useEffect(() => {
    localStorage.setItem(cartKey, JSON.stringify(cart));
  }, [cart, cartKey]);

  return (
    <AppContext.Provider
      value={{
        data,
        isError,
        cart,
        token,
        role,
        isAdmin,
        addToCart,
        removeFromCart,
        updateCartItemQuantity,
        refreshData,
        clearCart,
        removeMultipleFromCart,
        refreshAuth,
      }}
    >
      {children}
    </AppContext.Provider>
  );
};

export default AppContext;