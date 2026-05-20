import API from "../axios";

export const createReturnExchangeRequest = (orderId, data) => {
  return API.post(`/orders/${orderId}/return-exchange`, data);
};

export const getMyReturnExchangeRequests = () => {
  return API.get("/orders/return-exchange");
};

export const getAllReturnExchangeRequests = () => {
  return API.get("/admin/return-exchange");
};

export const approveReturnExchangeRequest = (requestId, data) => {
  return API.put(`/admin/return-exchange/${requestId}/approve`, data);
};

export const rejectReturnExchangeRequest = (requestId, data) => {
  return API.put(`/admin/return-exchange/${requestId}/reject`, data);
};

export const completeReturnExchangeRequest = (requestId, data) => {
  return API.put(`/admin/return-exchange/${requestId}/complete`, data);
};