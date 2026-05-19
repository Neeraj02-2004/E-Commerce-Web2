import { useEffect, useState } from "react";
import API from "../axios";
import {
  getMyReturnExchangeRequests,
} from "../api/returnExchangeApi";
import ReturnExchangePopup from "./ReturnExchangePopup";

const OrderStatus = () => {
  const [orders, setOrders] = useState([]);
  const [returnExchangeRequests, setReturnExchangeRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [openOrderId, setOpenOrderId] = useState(null);

  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [selectedOrder, setSelectedOrder] = useState(null);
  const [selectedRequestType, setSelectedRequestType] = useState("RETURN");
  const [showReturnExchangePopup, setShowReturnExchangePopup] = useState(false);

  const [popup, setPopup] = useState({
    show: false,
    message: "",
    type: "success",
  });

  const pageSize = 10;

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

  const fetchOrders = async () => {
    try {
      setLoading(true);

      const [ordersResponse, returnExchangeResponse] = await Promise.all([
        API.get(`/orders?page=${page}&size=${pageSize}`),
        getMyReturnExchangeRequests(),
      ]);

      setOrders(ordersResponse.data.content || []);
      setTotalPages(ordersResponse.data.totalPages || 0);
      setReturnExchangeRequests(returnExchangeResponse.data || []);
    } catch (error) {
      showPopup(
        error.response?.data?.message || "Failed to load orders",
        "error"
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, [page]);

  const toggleDetails = (id) => {
    setOpenOrderId((prev) => (prev === id ? null : id));
  };

  const cancelOrder = async (orderId) => {
    try {
      await API.put(`/cancel/${orderId}`);

      showPopup("Order cancelled successfully", "success");

      setOrders((prev) =>
        prev.map((order) =>
          order.orderId === orderId ? { ...order, status: "CANCELLED" } : order
        )
      );
    } catch (error) {
      showPopup(
        error.response?.data?.message || "Failed to cancel order",
        "error"
      );
    }
  };

  const openReturnExchange = (order, requestType) => {
    setSelectedOrder(order);
    setSelectedRequestType(requestType);
    setShowReturnExchangePopup(true);
  };

  const closeReturnExchange = () => {
    setSelectedOrder(null);
    setSelectedRequestType("RETURN");
    setShowReturnExchangePopup(false);
  };

  const handleReturnExchangeSuccess = async (message) => {
    closeReturnExchange();
    showPopup(message, "success");
    await fetchOrders();
  };

  const getRequestForOrder = (orderId) => {
    return returnExchangeRequests.find((request) => request.orderId === orderId);
  };

  const canRequestReturnExchange = (order) => {
    return order.status === "DELIVERED" && !getRequestForOrder(order.orderId);
  };

  const formatPaymentMode = (paymentMode) => {
    if (paymentMode === "CASH_ON_DELIVERY") {
      return "Cash on Delivery";
    }

    if (paymentMode === "ONLINE") {
      return "Online Payment";
    }

    return paymentMode || "Not available";
  };

  const getPaymentStatusColor = (paymentStatus) => {
    if (paymentStatus === "PAID") {
      return "green";
    }

    if (paymentStatus === "FAILED") {
      return "red";
    }

    return "orange";
  };

  const getOrderStatusColor = (status) => {
    if (status === "DELIVERED") {
      return "green";
    }

    if (status === "SHIPPED") {
      return "blue";
    }

    if (status === "CANCELLED" || status === "FAILED") {
      return "red";
    }

    return "orange";
  };

  const getReturnExchangeStatusColor = (status) => {
    if (status === "COMPLETED") {
      return "green";
    }

    if (status === "REJECTED") {
      return "red";
    }

    if (status === "APPROVED") {
      return "blue";
    }

    return "orange";
  };

  if (loading) {
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

        <h2 style={{ marginTop: "100px", textAlign: "center" }}>
          Loading Orders...
        </h2>
      </>
    );
  }

  return (
    <div style={{ marginTop: "80px", padding: "20px" }}>
      {popup.show && (
        <div style={popupStyle(popup.type)}>
          <div style={popupIconStyle}>
            {popup.type === "success" ? "✓" : "!"}
          </div>
          <div>{popup.message}</div>
        </div>
      )}

      <h2 style={{ textAlign: "center", marginBottom: "20px" }}>
        Your Orders
      </h2>

      {orders.length === 0 ? (
        <h4 style={{ textAlign: "center" }}>No Orders Found</h4>
      ) : (
        <>
          {orders.map((order) => {
            const manualTotal =
              order.items?.reduce(
                (sum, item) => sum + Number(item.totalPrice || 0),
                0
              ) || 0;

            const returnExchangeRequest = getRequestForOrder(order.orderId);

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
                  <strong>Address:</strong> {order.address || "Not available"}
                </p>

                <p>
                  <strong>Payment Mode:</strong>{" "}
                  {formatPaymentMode(order.paymentMode)}
                </p>

                <p>
                  <strong>Payment Status: </strong>
                  <span
                    style={{
                      color: getPaymentStatusColor(order.paymentStatus),
                      fontWeight: "bold",
                    }}
                  >
                    {order.paymentStatus || "PENDING"}
                  </span>
                </p>

                {order.gatewayPaymentId && (
                  <p>
                    <strong>Payment ID:</strong> {order.gatewayPaymentId}
                  </p>
                )}

                <p>
                  <strong>Order Status: </strong>
                  <span
                    style={{
                      color: getOrderStatusColor(order.status),
                      fontWeight: "bold",
                    }}
                  >
                    {order.status}
                  </span>
                </p>

                {returnExchangeRequest && (
                  <div
                    style={{
                      padding: "10px",
                      background: "#f8f9fa",
                      borderRadius: "8px",
                      marginBottom: "10px",
                    }}
                  >
                    <p style={{ marginBottom: "5px" }}>
                      <strong>{returnExchangeRequest.requestType} Request:</strong>{" "}
                      <span
                        style={{
                          color: getReturnExchangeStatusColor(
                            returnExchangeRequest.status
                          ),
                          fontWeight: "bold",
                        }}
                      >
                        {returnExchangeRequest.status}
                      </span>
                    </p>

                    <p style={{ marginBottom: "5px" }}>
                      <strong>Refund Status:</strong>{" "}
                      {returnExchangeRequest.refundStatus}
                    </p>

                    {returnExchangeRequest.adminNote && (
                      <p style={{ marginBottom: 0 }}>
                        <strong>Admin Note:</strong>{" "}
                        {returnExchangeRequest.adminNote}
                      </p>
                    )}
                  </div>
                )}

                <p>
                  <strong>Total Price:</strong> ₹{manualTotal}
                </p>

                <div
                  style={{
                    display: "flex",
                    gap: "10px",
                    marginBottom: "10px",
                    flexWrap: "wrap",
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

                  {order.status !== "CANCELLED" &&
                    order.paymentStatus !== "PAID" && (
                      <button
                        className="btn btn-sm btn-danger"
                        onClick={() => cancelOrder(order.orderId)}
                      >
                        Cancel Order
                      </button>
                    )}

                  {canRequestReturnExchange(order) && (
                    <>
                      <button
                        className="btn btn-sm btn-warning"
                        onClick={() => openReturnExchange(order, "RETURN")}
                      >
                        Return
                      </button>

                      <button
                        className="btn btn-sm btn-info"
                        onClick={() => openReturnExchange(order, "EXCHANGE")}
                      >
                        Exchange
                      </button>
                    </>
                  )}
                </div>

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
          })}

          <div
            style={{
              display: "flex",
              justifyContent: "center",
              gap: "10px",
              marginTop: "20px",
            }}
          >
            <button
              className="btn btn-secondary"
              disabled={page === 0}
              onClick={() => setPage((prev) => prev - 1)}
            >
              Previous
            </button>

            <span style={{ alignSelf: "center" }}>
              Page {page + 1} of {totalPages || 1}
            </span>

            <button
              className="btn btn-secondary"
              disabled={page + 1 >= totalPages}
              onClick={() => setPage((prev) => prev + 1)}
            >
              Next
            </button>
          </div>
        </>
      )}

      <ReturnExchangePopup
        show={showReturnExchangePopup}
        handleClose={closeReturnExchange}
        order={selectedOrder}
        requestType={selectedRequestType}
        onRequestSuccess={handleReturnExchangeSuccess}
      />
    </div>
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

export default OrderStatus;