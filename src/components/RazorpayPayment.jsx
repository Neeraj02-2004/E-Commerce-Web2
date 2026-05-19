import axios from "../axios";

const openRazorpayCheckout = async ({
  order,
  customerName,
  email,
  mobileNo,
  onSuccess,
  onFailure,
}) => {
  if (!window.Razorpay) {
    onFailure("Razorpay is not loaded. Please refresh and try again.");
    return;
  }

  try {
    const paymentResponse = await axios.post("/payments/create", {
      orderId: order.orderId,
    });

    const paymentData = paymentResponse.data;

    const options = {
      key: paymentData.keyId,
      amount: paymentData.amount,
      currency: paymentData.currency,
      name: "MyStore",
      description: `Payment for order ${order.orderId}`,
      order_id: paymentData.razorpayOrderId,

      prefill: {
        name: customerName,
        email,
        contact: mobileNo,
      },

      handler: async (response) => {
        try {
          await axios.post("/payments/verify", {
            orderId: order.orderId,
            razorpayOrderId: response.razorpay_order_id,
            razorpayPaymentId: response.razorpay_payment_id,
            razorpaySignature: response.razorpay_signature,
          });

          onSuccess();
        } catch (error) {
          const message =
            error.response?.data?.message || "Payment verification failed";
          onFailure(message);
        }
      },

      modal: {
        ondismiss: () => {
          onFailure("Payment cancelled by user");
        },
      },

      theme: {
        color: "#0d6efd",
      },
    };

    const razorpay = new window.Razorpay(options);

    razorpay.on("payment.failed", (response) => {
      onFailure(response.error?.description || "Payment failed");
    });

    razorpay.open();
  } catch (error) {
    const message =
      error.response?.data?.message || "Unable to start payment";
    onFailure(message);
  }
};

export default openRazorpayCheckout;