import { useEffect, useState } from "react";
import API from "../axios";
import { getMyReturnExchangeRequests } from "../api/returnExchangeApi";
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

  const formatDateTime = (value) => {
    if (!value) {
      return null;
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return date.toLocaleString();
  };

  const formatAmount = (value) => {
    if (value === null || value === undefined || value === "") {
      return null;
    }

    return `Rs. ${Number(value).toFixed(2)}`;
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

  const getRefundStatusColor = (refundStatus) => {
    if (refundStatus === "REFUNDED") {
      return "green";
    }

    if (refundStatus === "REFUND_FAILED") {
      return "red";
    }

    if (
      refundStatus === "REFUND_PROCESSING" ||
      refundStatus === "MANUAL_REFUND_REQUIRED"
    ) {
      return "orange";
    }

    return "#555";
  };

  const getRefundStatusMessage = (request) => {
    if (!request) {
      return "";
    }

    if (request.requestType === "EXCHANGE") {
      if (request.status === "APPROVED") {
        return "Exchange approved. New product delivery will complete after 6 days.";
      }

      if (request.status === "COMPLETED") {
        return "Exchange completed successfully.";
      }

      return "";
    }

    if (request.refundStatus === "REFUND_PROCESSING") {
      return "Refund is processing. It will be completed automatically after 6 days.";
    }

    if (request.refundStatus === "MANUAL_REFUND_REQUIRED") {
      return "Manual refund is required for this Cash on Delivery order.";
    }

    if (request.refundStatus === "REFUNDED") {
      return "Refund completed successfully.";
    }

    if (request.refundStatus === "REFUND_FAILED") {
      return "Refund failed. Admin will retry or process it manually.";
    }

    return "";
  };

  const renderReturnExchangeDetails = (request) => {
    const refundMessage = getRefundStatusMessage(request);
    const approvedAt = formatDateTime(request.approvedAt);
    const completedAt = formatDateTime(request.completedAt);
    const refundProcessedAt = formatDateTime(request.refundProcessedAt);
    const refundAmount = formatAmount(request.refundAmount);

    return (
      <div
        style={{
          padding: "12px",
          background: "#f8f9fa",
          borderRadius: "8px",
          marginBottom: "10px",
          border: "1px solid #e5e7eb",
        }}
      >
        <p style={{ marginBottom: "5px" }}>
          <strong>{request.requestType} Request:</strong>{" "}
          <span
            style={{
              color: getReturnExchangeStatusColor(request.status),
              fontWeight: "bold",
            }}
          >
            {request.status}
          </span>
        </p>

        {request.requestId && (
          <p style={{ marginBottom: "5px" }}>
            <strong>Request ID:</strong> {request.requestId}
          </p>
        )}

        <p style={{ marginBottom: "5px" }}>
          <strong>Refund Status:</strong>{" "}
          <span
            style={{
              color: getRefundStatusColor(request.refundStatus),
              fontWeight: "bold",
            }}
          >
            {request.refundStatus || "NOT_REQUIRED"}
          </span>
        </p>

        {refundMessage && (
          <p style={{ marginBottom: "5px", color: "#555" }}>{refundMessage}</p>
        )}

        {refundAmount && (
          <p style={{ marginBottom: "5px" }}>
            <strong>Refund Amount:</strong> {refundAmount}
          </p>
        )}

        {request.gatewayRefundId && (
          <p style={{ marginBottom: "5px" }}>
            <strong>Razorpay Refund ID:</strong> {request.gatewayRefundId}
          </p>
        )}

        {approvedAt && (
          <p style={{ marginBottom: "5px" }}>
            <strong>Approved At:</strong> {approvedAt}
          </p>
        )}

        {completedAt && (
          <p style={{ marginBottom: "5px" }}>
            <strong>Completed At:</strong> {completedAt}
          </p>
        )}

        {refundProcessedAt && (
          <p style={{ marginBottom: "5px" }}>
            <strong>Refund Processed At:</strong> {refundProcessedAt}
          </p>
        )}

        {request.refundFailureReason && (
          <p style={{ marginBottom: "5px", color: "#dc3545" }}>
            <strong>Refund Failure Reason:</strong>{" "}
            {request.refundFailureReason}
          </p>
        )}

        {request.adminNote && (
          <p style={{ marginBottom: 0 }}>
            <strong>Admin Note:</strong> {request.adminNote}
          </p>
        )}
      </div>
    );
  };

  if (loading) {
    return (
      <>
        {popup.show && (
          <div style={popupStyle(popup.type)}>
            <div style={popupIconStyle}>
              {popup.type === "success" ? "OK" : "!"}
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
            {popup.type === "success" ? "OK" : "!"}
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

                {order.deliveredAt && (
                  <p>
                    <strong>Delivered At:</strong>{" "}
                    {formatDateTime(order.deliveredAt)}
                  </p>
                )}

                {returnExchangeRequest &&
                  renderReturnExchangeDetails(returnExchangeRequest)}

                <p>
                  <strong>Total Price:</strong> Rs. {manualTotal.toFixed(2)}
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
                          <p>Price: Rs. {Number(item.totalPrice || 0).toFixed(2)}</p>
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
  fontSize: "12px",
};

export default OrderStatus;