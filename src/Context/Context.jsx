import axios from "../axios";
import { useState, useEffect, createContext } from "react";

const AppContext = createContext({
  data: [],
  isError: "",
  cart: [],
  isAdmin: false,
  token: null,
  role: "USER",
  addToCart: (product) => {},
  removeFromCart: (productId) => {},
  refreshData: () => {},
  clearCart: () => {},
  removeMultipleFromCart: (ids) => {},
  refreshAuth: () => {},
});

export const AppProvider = ({ children }) => {
  const [data, setData] = useState([]);
  const [isError, setIsError] = useState("");

  const [token, setToken] = useState(localStorage.getItem("token"));
  const [role, setRole] = useState(localStorage.getItem("role") || "USER");

  const isAdmin = role === "ADMIN";

  const [cart, setCart] = useState(
    JSON.parse(localStorage.getItem("cart")) || []
  );

  const refreshAuth = () => {
    setToken(localStorage.getItem("token"));
    setRole(localStorage.getItem("role") || "USER");
  };

  const addToCart = (product) => {
    const existingIndex = cart.findIndex((item) => item.id === product.id);

    let updatedCart;

    if (existingIndex !== -1) {
      updatedCart = cart.map((item, i) =>
        i === existingIndex ? { ...item, quantity: item.quantity + 1 } : item
      );
    } else {
      updatedCart = [...cart, { ...product, quantity: 1 }];
    }

    setCart(updatedCart);
    localStorage.setItem("cart", JSON.stringify(updatedCart));
  };

  const removeFromCart = (productId) => {
    const updatedCart = cart.filter((item) => item.id !== productId);
    setCart(updatedCart);
    localStorage.setItem("cart", JSON.stringify(updatedCart));
  };

  const removeMultipleFromCart = (ids) => {
    const updatedCart = cart.filter((item) => !ids.includes(item.id));
    setCart(updatedCart);
    localStorage.setItem("cart", JSON.stringify(updatedCart));
  };

  const clearCart = () => {
    setCart([]);
    localStorage.removeItem("cart");
  };

  const refreshData = async () => {
    try {
      const response = await axios.get("/products");
      setData([...response.data]);
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
    localStorage.setItem("cart", JSON.stringify(cart));
  }, [cart]);

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
