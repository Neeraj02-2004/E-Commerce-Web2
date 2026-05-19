import API from "../axios";

export const createReturnExchangeRequest = (orderId, data) => {
  return API.post(`/orders/${orderId}/return-exchange`, data);
};

export const getMyReturnExchangeRequests = () => {
  return API.get("/orders/return-exchange");
};