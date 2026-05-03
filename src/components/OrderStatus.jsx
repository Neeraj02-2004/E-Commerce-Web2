
import React, { useEffect, useState } from "react";
import API from "../axios";

const OrderStatus = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [openOrderId, setOpenOrderId] = useState(null);

  // Fetch orders from backend
  const fetchOrders = async () => {
    try {
      const response = await API.get("/orders");
      setOrders(response.data);
    } catch (error) {
      console.error("Error fetching orders:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, []);

  const toggleDetails = (id) => {
    setOpenOrderId((prev) => (prev === id ? null : id));
  };

  // Cancel order
  const cancelOrder = async (orderId) => {
    try {
      await API.put(`/cancel/${orderId}`);

      alert("Order Cancelled Successfully");

      // Update UI instantly (avoid full reload flicker)
      setOrders((prev) =>
        prev.map((order) =>
          order.orderId === orderId
            ? { ...order, status: "CANCELLED" }
            : order
        )
      );
    } catch (error) {
      console.error("Error cancelling order:", error);
      alert("Failed to cancel order");
    }
  };

  if (loading) {
    return (
      <h2 style={{ marginTop: "100px", textAlign: "center" }}>
        Loading Orders...
      </h2>
    );
  }

  return (
    <div style={{ marginTop: "80px", padding: "20px" }}>
      <h2 style={{ textAlign: "center", marginBottom: "20px" }}>
        Your Orders
      </h2>

      {orders.length === 0 ? (
        <h4 style={{ textAlign: "center" }}>No Orders Found</h4>
      ) : (
        orders.map((order) => {
          const manualTotal =
            order.items?.reduce(
              (sum, item) => sum + Number(item.totalPrice || 0),
              0
            ) || 0;

          return (
            <div
              key={order.orderId}
              style={{
                border: "1px solid #ddd",
                borderRadius: "10px",
                padding: "15px",
                marginBottom: "15px",
                boxShadow: "0 2px 6px rgba(0,0,0,0.1)",
              }}
            >
              <h5>Order ID: {order.orderId}</h5>

              <p>
                <strong>Name:</strong> {order.customerName}
              </p>
              <p>
                <strong>Email:</strong> {order.email}
              </p>
              <p>
                <strong>Mobile:</strong> {order.mobileNo}
              </p>

              <p>
                <strong>Status: </strong>
                <span
                  style={{
                    color:
                      order.status === "DELIVERED"
                        ? "green"
                        : order.status === "SHIPPED"
                        ? "blue"
                        : order.status === "CANCELLED"
                        ? "red"
                        : "orange",
                    fontWeight: "bold",
                  }}
                >
                  {order.status}
                </span>
              </p>

              <p>
                <strong>Total Price:</strong> ₹{manualTotal}
              </p>

              {/* BUTTONS */}
              <div
                style={{
                  display: "flex",
                  gap: "10px",
                  marginBottom: "10px",
                }}
              >
                <button
                  className="btn btn-sm btn-primary"
                  onClick={() => toggleDetails(order.orderId)}
                >
                  {openOrderId === order.orderId
                    ? "Hide Details"
                    : "View Details"}
                </button>

                {order.status !== "CANCELLED" && (
                  <button
                    className="btn btn-sm btn-danger"
                    onClick={() => cancelOrder(order.orderId)}
                  >
                    Cancel Order
                  </button>
                )}
              </div>

              {/* PRODUCTS */}
              {openOrderId === order.orderId && (
                <div style={{ marginTop: "10px" }}>
                  <hr />
                  <h6>Products:</h6>

                  {order.items?.length > 0 ? (
                    order.items.map((item, index) => (
                      <div key={index} style={{ marginBottom: "8px" }}>
                        <p>
                          <strong>{item.productName}</strong>
                        </p>
                        <p>Quantity: {item.quantity}</p>
                        <p>Price: ₹{item.totalPrice}</p>
                        <hr />
                      </div>
                    ))
                  ) : (
                    <p>No items found</p>
                  )}
                </div>
              )}
            </div>
          );
        })
      )}
    </div>
  );
};

export default OrderStatus;


