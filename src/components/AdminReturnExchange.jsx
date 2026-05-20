import { useEffect, useMemo, useState } from "react";
import {
  approveReturnExchangeRequest,
  completeReturnExchangeRequest,
  getAllReturnExchangeRequests,
  rejectReturnExchangeRequest,
} from "../api/returnExchangeApi";

const AdminReturnExchange = () => {
  const [requests, setRequests] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [typeFilter, setTypeFilter] = useState("ALL");
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [decisionType, setDecisionType] = useState("");
  const [adminNote, setAdminNote] = useState("");
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  const [popup, setPopup] = useState({
    show: false,
    message: "",
    type: "success",
  });

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

  const fetchRequests = async () => {
    try {
      setLoading(true);
      const response = await getAllReturnExchangeRequests();
      setRequests(response.data || []);
    } catch (error) {
      showPopup(
        error.response?.data?.message || "Failed to load return/exchange requests",
        "error"
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRequests();
  }, []);

  const filteredRequests = useMemo(() => {
    return requests.filter((request) => {
      const statusMatches =
        statusFilter === "ALL" || request.status === statusFilter;

      const typeMatches =
        typeFilter === "ALL" || request.requestType === typeFilter;

      return statusMatches && typeMatches;
    });
  }, [requests, statusFilter, typeFilter]);

  const summary = useMemo(() => {
    return {
      total: requests.length,
      requested: requests.filter((item) => item.status === "REQUESTED").length,
      approved: requests.filter((item) => item.status === "APPROVED").length,
      rejected: requests.filter((item) => item.status === "REJECTED").length,
      completed: requests.filter((item) => item.status === "COMPLETED").length,
      refundFailed: requests.filter(
        (item) => item.refundStatus === "REFUND_FAILED"
      ).length,
    };
  }, [requests]);

  const openDecisionModal = (request, type) => {
    setSelectedRequest(request);
    setDecisionType(type);
    setAdminNote("");
  };

  const closeDecisionModal = () => {
    setSelectedRequest(null);
    setDecisionType("");
    setAdminNote("");
    setActionLoading(false);
  };

  const handleDecision = async () => {
    if (!selectedRequest?.requestId || !decisionType) {
      return;
    }

    try {
      setActionLoading(true);

      const payload = {
        adminNote: adminNote.trim() || null,
      };

      if (decisionType === "APPROVE") {
        await approveReturnExchangeRequest(selectedRequest.requestId, payload);
        showPopup("Request approved successfully", "success");
      }

      if (decisionType === "REJECT") {
        await rejectReturnExchangeRequest(selectedRequest.requestId, payload);
        showPopup("Request rejected successfully", "success");
      }

      if (decisionType === "COMPLETE") {
        await completeReturnExchangeRequest(selectedRequest.requestId, payload);
        showPopup("Request completed successfully", "success");
      }

      closeDecisionModal();
      await fetchRequests();
    } catch (error) {
      showPopup(
        error.response?.data?.message || "Action failed. Please try again.",
        "error"
      );
      setActionLoading(false);
    }
  };

  const formatDateTime = (value) => {
    if (!value) {
      return "Not available";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return date.toLocaleString();
  };

  const formatAmount = (value) => {
    if (value === null || value === undefined || value === "") {
      return "Not available";
    }

    return `Rs. ${Number(value).toFixed(2)}`;
  };

  const getStatusColor = (status) => {
    if (status === "COMPLETED") {
      return "#15803d";
    }

    if (status === "APPROVED") {
      return "#1d4ed8";
    }

    if (status === "REJECTED") {
      return "#dc2626";
    }

    return "#f59e0b";
  };

  const getRefundColor = (refundStatus) => {
    if (refundStatus === "REFUNDED") {
      return "#15803d";
    }

    if (refundStatus === "REFUND_FAILED") {
      return "#dc2626";
    }

    if (
      refundStatus === "REFUND_PROCESSING" ||
      refundStatus === "MANUAL_REFUND_REQUIRED"
    ) {
      return "#f59e0b";
    }

    return "#4b5563";
  };

  const getDecisionTitle = () => {
    if (decisionType === "APPROVE") {
      return "Approve Request";
    }

    if (decisionType === "REJECT") {
      return "Reject Request";
    }

    if (decisionType === "COMPLETE") {
      return "Complete Request";
    }

    return "Request Action";
  };

  const getDecisionButtonClass = () => {
    if (decisionType === "APPROVE") {
      return "btn-success";
    }

    if (decisionType === "REJECT") {
      return "btn-danger";
    }

    return "btn-primary";
  };

  if (loading) {
    return (
      <div style={{ marginTop: "100px", textAlign: "center" }}>
        <h3>Loading return/exchange requests...</h3>
      </div>
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

      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          gap: "12px",
          alignItems: "center",
          flexWrap: "wrap",
          marginBottom: "18px",
        }}
      >
        <div>
          <h2 style={{ marginBottom: "4px" }}>Return / Exchange Admin</h2>
          <p style={{ margin: 0, color: "#6b7280" }}>
            Review, approve, reject, and complete customer return/exchange requests.
          </p>
        </div>

        <button className="btn btn-outline-primary" onClick={fetchRequests}>
          Refresh
        </button>
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))",
          gap: "12px",
          marginBottom: "18px",
        }}
      >
        <SummaryCard label="Total" value={summary.total} />
        <SummaryCard label="Requested" value={summary.requested} />
        <SummaryCard label="Approved" value={summary.approved} />
        <SummaryCard label="Rejected" value={summary.rejected} />
        <SummaryCard label="Completed" value={summary.completed} />
        <SummaryCard label="Refund Failed" value={summary.refundFailed} danger />
      </div>

      <div
        style={{
          display: "flex",
          gap: "12px",
          flexWrap: "wrap",
          marginBottom: "16px",
        }}
      >
        <select
          className="form-select"
          style={{ maxWidth: "220px" }}
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value)}
        >
          <option value="ALL">All Statuses</option>
          <option value="REQUESTED">Requested</option>
          <option value="APPROVED">Approved</option>
          <option value="REJECTED">Rejected</option>
          <option value="COMPLETED">Completed</option>
        </select>

        <select
          className="form-select"
          style={{ maxWidth: "220px" }}
          value={typeFilter}
          onChange={(event) => setTypeFilter(event.target.value)}
        >
          <option value="ALL">All Types</option>
          <option value="RETURN">Return</option>
          <option value="EXCHANGE">Exchange</option>
        </select>
      </div>

      {filteredRequests.length === 0 ? (
        <div
          style={{
            padding: "30px",
            textAlign: "center",
            border: "1px solid #e5e7eb",
            borderRadius: "10px",
            background: "#fff",
          }}
        >
          <h5>No return/exchange requests found</h5>
        </div>
      ) : (
        <div style={{ overflowX: "auto" }}>
          <table className="table table-bordered table-hover align-middle">
            <thead className="table-light">
              <tr>
                <th>Request</th>
                <th>Order</th>
                <th>User</th>
                <th>Type</th>
                <th>Status</th>
                <th>Refund</th>
                <th>Reason</th>
                <th>Dates</th>
                <th>Actions</th>
              </tr>
            </thead>

            <tbody>
              {filteredRequests.map((request) => (
                <tr key={request.requestId}>
                  <td>
                    <strong>{request.requestId}</strong>
                  </td>

                  <td>{request.orderId}</td>

                  <td style={{ minWidth: "180px" }}>{request.userEmail}</td>

                  <td>
                    <span
                      className={
                        request.requestType === "RETURN"
                          ? "badge bg-warning text-dark"
                          : "badge bg-info text-dark"
                      }
                    >
                      {request.requestType}
                    </span>
                  </td>

                  <td>
                    <span
                      style={{
                        color: getStatusColor(request.status),
                        fontWeight: "700",
                      }}
                    >
                      {request.status}
                    </span>
                  </td>

                  <td style={{ minWidth: "190px" }}>
                    <div
                      style={{
                        color: getRefundColor(request.refundStatus),
                        fontWeight: "700",
                      }}
                    >
                      {request.refundStatus || "NOT_REQUIRED"}
                    </div>

                    {request.refundAmount !== null &&
                      request.refundAmount !== undefined && (
                        <div style={{ fontSize: "13px", color: "#555" }}>
                          {formatAmount(request.refundAmount)}
                        </div>
                      )}

                    {request.gatewayRefundId && (
                      <div style={{ fontSize: "12px", color: "#555" }}>
                        {request.gatewayRefundId}
                      </div>
                    )}

                    {request.refundFailureReason && (
                      <div style={{ fontSize: "12px", color: "#dc2626" }}>
                        {request.refundFailureReason}
                      </div>
                    )}
                  </td>

                  <td style={{ minWidth: "260px" }}>
                    <div>{request.reason}</div>
                    {request.adminNote && (
                      <div style={{ marginTop: "6px", color: "#555" }}>
                        <strong>Admin:</strong> {request.adminNote}
                      </div>
                    )}
                  </td>

                  <td style={{ minWidth: "220px", fontSize: "13px" }}>
                    <div>
                      <strong>Created:</strong> {formatDateTime(request.createdAt)}
                    </div>
                    <div>
                      <strong>Approved:</strong> {formatDateTime(request.approvedAt)}
                    </div>
                    <div>
                      <strong>Completed:</strong>{" "}
                      {formatDateTime(request.completedAt)}
                    </div>
                    <div>
                      <strong>Refund:</strong>{" "}
                      {formatDateTime(request.refundProcessedAt)}
                    </div>
                  </td>

                  <td style={{ minWidth: "210px" }}>
                    <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
                      {request.status === "REQUESTED" && (
                        <>
                          <button
                            className="btn btn-sm btn-success"
                            onClick={() => openDecisionModal(request, "APPROVE")}
                          >
                            Approve
                          </button>

                          <button
                            className="btn btn-sm btn-danger"
                            onClick={() => openDecisionModal(request, "REJECT")}
                          >
                            Reject
                          </button>
                        </>
                      )}

                      {request.status === "APPROVED" && (
                        <button
                          className="btn btn-sm btn-primary"
                          onClick={() => openDecisionModal(request, "COMPLETE")}
                        >
                          Complete
                        </button>
                      )}

                      {request.status !== "REQUESTED" &&
                        request.status !== "APPROVED" && (
                          <span style={{ color: "#6b7280" }}>No action</span>
                        )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {selectedRequest && (
        <div style={modalBackdropStyle}>
          <div style={modalStyle}>
            <h4 style={{ marginBottom: "10px" }}>{getDecisionTitle()}</h4>

            <p style={{ marginBottom: "6px" }}>
              <strong>Request ID:</strong> {selectedRequest.requestId}
            </p>

            <p style={{ marginBottom: "6px" }}>
              <strong>Order ID:</strong> {selectedRequest.orderId}
            </p>

            <p style={{ marginBottom: "12px" }}>
              <strong>Type:</strong> {selectedRequest.requestType}
            </p>

            <label className="form-label">
              Admin Note
            </label>

            <textarea
              className="form-control"
              rows={4}
              maxLength={1000}
              value={adminNote}
              onChange={(event) => setAdminNote(event.target.value)}
              placeholder="Optional note for this action"
              disabled={actionLoading}
            />

            <div
              style={{
                display: "flex",
                justifyContent: "flex-end",
                gap: "10px",
                marginTop: "16px",
              }}
            >
              <button
                className="btn btn-secondary"
                onClick={closeDecisionModal}
                disabled={actionLoading}
              >
                Cancel
              </button>

              <button
                className={`btn ${getDecisionButtonClass()}`}
                onClick={handleDecision}
                disabled={actionLoading}
              >
                {actionLoading ? "Processing..." : getDecisionTitle()}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const SummaryCard = ({ label, value, danger = false }) => {
  return (
    <div
      style={{
        padding: "14px",
        border: "1px solid #e5e7eb",
        borderRadius: "10px",
        background: "#fff",
        boxShadow: "0 2px 6px rgba(0,0,0,0.06)",
      }}
    >
      <div style={{ color: "#6b7280", fontSize: "14px" }}>{label}</div>
      <div
        style={{
          fontSize: "24px",
          fontWeight: "800",
          color: danger ? "#dc2626" : "#111827",
        }}
      >
        {value}
      </div>
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

const modalBackdropStyle = {
  position: "fixed",
  inset: 0,
  background: "rgba(0,0,0,0.45)",
  zIndex: 9999,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  padding: "16px",
};

const modalStyle = {
  width: "100%",
  maxWidth: "520px",
  background: "#fff",
  borderRadius: "12px",
  padding: "20px",
  boxShadow: "0 20px 50px rgba(0,0,0,0.3)",
};

export default AdminReturnExchange;