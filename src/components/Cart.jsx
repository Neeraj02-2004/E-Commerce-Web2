
import React, { useContext, useState, useEffect } from "react";
import AppContext from "../Context/Context";
import axios from "../axios";
import CheckoutPopup from "./CheckoutPopup";
import { Button } from "react-bootstrap";

const Cart = () => {
 

  const { cart, removeFromCart, clearCart, removeMultipleFromCart } =
    useContext(AppContext);

  const [cartItems, setCartItems] = useState([]);
  const [totalPrice, setTotalPrice] = useState(0);
  const [showModal, setShowModal] = useState(false);


  useEffect(() => {
    const fetchImages = async () => {
      try {
        if (!cart || cart.length === 0) {
          setCartItems([]);
          return;
        }

        const updatedCart = await Promise.all(
          cart.map(async (item) => {
            try {
              const response = await axios.get(
                `/product/${item.id}/image`,
                { responseType: "blob" }
              );

              const imageUrl = URL.createObjectURL(response.data);

              return { ...item, imageUrl };
            } catch {
              return { ...item, imageUrl: "placeholder-image-url" };
            }
          })
        );

        setCartItems(updatedCart);
      } catch (error) {
        console.error("Cart load error:", error);
      }
    };

    fetchImages();
  }, [cart]);


  useEffect(() => {
    const total = cartItems.reduce(
      (acc, item) => acc + item.price * item.quantity,
      0
    );
    setTotalPrice(total);
  }, [cartItems]);



  const handleIncreaseQuantity = (itemId) => {
    const updated = cartItems.map((item) => {
      if (item.id === itemId) {
        if (item.quantity < item.stockQuantity) {
          return { ...item, quantity: item.quantity + 1 };
        } else {
          alert("Cannot add more than available stock");
        }
      }
      return item;
    });

    setCartItems(updated);
  };


  const handleDecreaseQuantity = (itemId) => {
    const updated = cartItems.map((item) =>
      item.id === itemId
        ? { ...item, quantity: Math.max(item.quantity - 1, 1) }
        : item
    );

    setCartItems(updated);
  };


  const handleRemoveFromCart = (itemId) => {
    removeFromCart(itemId);
    setCartItems((prev) => prev.filter((item) => item.id !== itemId));
  };


  const handleCheckout = async () => {
    try {
      const purchasedIds = cartItems.map((item) => item.id);

      for (const item of cartItems) {
        const { imageUrl, quantity, ...rest } = item;

        const updatedStockQuantity = item.stockQuantity - item.quantity;

        const updatedProductData = {
          ...rest,
          stockQuantity: updatedStockQuantity,
        };

        const formData = new FormData();

        formData.append(
          "product",
          new Blob([JSON.stringify(updatedProductData)], {
            type: "application/json",
          })
        );

        await axios.put(`/product/${item.id}`, formData, {
          headers: {
            "Content-Type": "multipart/form-data",
          },
        });
      }


      removeMultipleFromCart(purchasedIds);

      setCartItems([]);
      setTotalPrice(0);

      setShowModal(false);

      alert("Order placed successfully!");
    } catch (error) {
      console.log("error during checkout", error);
    }
  };

 
  return (
    <div className="cart-container">
      <div className="shopping-cart">
        <div className="title">Shopping Bag</div>

        {cartItems.length === 0 ? (
          <div className="empty" style={{ textAlign: "left", padding: "2rem" }}>
            <h4>Your cart is empty</h4>
          </div>
        ) : (
          <>
            {cartItems.map((item) => (
              <li key={item.id} className="cart-item">
                <div
                  className="item"
                  style={{ display: "flex", alignContent: "center" }}
                >
                  <div>
                    <img
                      src={item.imageUrl}
                      alt={item.name}
                      className="cart-item-image"
                    />
                  </div>

                  <div className="description">
                    <span>{item.brand}</span>
                    <span>{item.name}</span>
                  </div>

                  <div className="quantity">
                    <button
                      className="plus-btn"
                      onClick={() => handleIncreaseQuantity(item.id)}
                    >
                      <i className="bi bi-plus-square-fill"></i>
                    </button>

                    <input value={item.quantity} readOnly />

                    <button
                      className="minus-btn"
                      onClick={() => handleDecreaseQuantity(item.id)}
                    >
                      <i className="bi bi-dash-square-fill"></i>
                    </button>
                  </div>

                  <div className="total-price">
                    ₹{item.price * item.quantity}
                  </div>

                  <button
                    className="remove-btn"
                    onClick={() => handleRemoveFromCart(item.id)}
                  >
                    <i className="bi bi-trash3-fill"></i>
                  </button>
                </div>
              </li>
            ))}

            <div className="total">Total: ₹{totalPrice}</div>

            <Button
              className="btn btn-primary"
              style={{ width: "100%" }}
              onClick={() => setShowModal(true)}
            >
              Checkout
            </Button>
          </>
        )}
      </div>

      {
        
      }
      <CheckoutPopup
        show={showModal}
        handleClose={() => setShowModal(false)}
        cartItems={cartItems}
        totalPrice={totalPrice}
        handleCheckout={handleCheckout}
      />
    </div>
  );
};

export default Cart;