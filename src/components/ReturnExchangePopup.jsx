import { useState } from "react";
import { Modal, Button } from "react-bootstrap";
import { createReturnExchangeRequest } from "../api/returnExchangeApi";

const ReturnExchangePopup = ({
  show,
  handleClose,
  order,
  requestType,
  onRequestSuccess,
}) => {
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const cleanType = requestType === "EXCHANGE" ? "EXCHANGE" : "RETURN";
  const title = cleanType === "EXCHANGE" ? "Exchange Product" : "Return Product";

  const resetForm = () => {
    setReason("");
    setError("");
    setLoading(false);
  };

  const closePopup = () => {
    resetForm();
    handleClose();
  };

  const handleSubmit = async () => {
    const cleanReason = reason.trim();

    if (cleanReason.length < 10 || cleanReason.length > 1000) {
      setError("Reason must be between 10 and 1000 characters");
      return;
    }

    if (!order?.orderId) {
      setError("Order information missing");
      return;
    }

    try {
      setLoading(true);
      setError("");

      await createReturnExchangeRequest(order.orderId, {
        requestType: cleanType,
        reason: cleanReason,
      });

      resetForm();

      onRequestSuccess(
        cleanType === "EXCHANGE"
          ? "Exchange request submitted successfully"
          : "Return request submitted successfully"
      );
    } catch (err) {
      const message =
        err.response?.data?.message ||
        err.response?.data?.error ||
        "Request failed. Please try again.";

      setError(String(message));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal show={show} onHide={closePopup} centered>
      <Modal.Header closeButton>
        <Modal.Title>{title}</Modal.Title>
      </Modal.Header>

      <Modal.Body>
        <p style={{ marginBottom: "8px" }}>
          <strong>Order ID:</strong> {order?.orderId || "Not available"}
        </p>

        <p style={{ marginBottom: "12px", color: "#666" }}>
          Please explain why you want to{" "}
          {cleanType === "EXCHANGE" ? "exchange" : "return"} this order.
        </p>

        <textarea
          className="form-control"
          rows={5}
          maxLength={1000}
          placeholder="Enter reason"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          disabled={loading}
        />

        <div
          style={{
            marginTop: "6px",
            fontSize: "13px",
            color: reason.trim().length < 10 ? "#dc3545" : "#666",
          }}
        >
          {reason.trim().length}/1000 characters
        </div>

        {error && (
          <p style={{ color: "#dc3545", marginTop: "10px", marginBottom: 0 }}>
            {error}
          </p>
        )}

        <div
          style={{
            marginTop: "12px",
            padding: "10px",
            background: "#f8f9fa",
            borderRadius: "8px",
            fontSize: "14px",
            color: "#555",
          }}
        >
          Requests are allowed within 7 days after delivery. After admin
          approval, return refunds and exchange requests are automatically
          completed after 6 days.
        </div>
      </Modal.Body>

      <Modal.Footer>
        <Button variant="secondary" onClick={closePopup} disabled={loading}>
          Cancel
        </Button>

        <Button variant="primary" onClick={handleSubmit} disabled={loading}>
          {loading ? "Submitting..." : "Submit Request"}
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default ReturnExchangePopup;