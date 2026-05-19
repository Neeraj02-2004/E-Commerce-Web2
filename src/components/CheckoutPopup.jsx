// import { useState } from "react";
// import { Modal, Button } from "react-bootstrap";
// import axios from "../axios";

// const CheckoutPopup = ({
//   show,
//   handleClose,
//   cartItems,
//   totalPrice,
//   onOrderSuccess,
// }) => {
//   const [name, setName] = useState("");
//   const [email, setEmail] = useState("");
//   const [mobile, setMobile] = useState("");
//   const [address, setAddress] = useState("");
//   const [error, setError] = useState("");

//   const [popup, setPopup] = useState({
//     show: false,
//     message: "",
//     type: "success",
//   });

//   const showPopup = (message, type = "success") => {
//     setPopup({
//       show: true,
//       message,
//       type,
//     });

//     setTimeout(() => {
//       setPopup({
//         show: false,
//         message: "",
//         type: "success",
//       });
//     }, 2500);
//   };

//   const resetForm = () => {
//     setName("");
//     setEmail("");
//     setMobile("");
//     setAddress("");
//     setError("");
//   };

//   const handleConfirm = async () => {
//     const cleanName = name.trim();
//     const cleanEmail = email.trim().toLowerCase();
//     const cleanMobile = mobile.trim();
//     const cleanAddress = address.trim();

//     if (!cleanName || !cleanEmail || !cleanMobile || !cleanAddress) {
//       setError("All fields are required!");
//       showPopup("All fields are required", "error");
//       return;
//     }

//     if (cleanName.length < 2 || cleanName.length > 80) {
//       setError("Name must be between 2 and 80 characters");
//       showPopup("Name must be between 2 and 80 characters", "error");
//       return;
//     }

//     if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(cleanEmail)) {
//       setError("Enter valid email address");
//       showPopup("Enter valid email address", "error");
//       return;
//     }

//     if (!/^[6-9]\d{9}$/.test(cleanMobile)) {
//       setError("Enter valid mobile number");
//       showPopup("Enter valid mobile number", "error");
//       return;
//     }

//     if (cleanAddress.length < 10 || cleanAddress.length > 500) {
//       setError("Address must be between 10 and 500 characters");
//       showPopup("Address must be between 10 and 500 characters", "error");
//       return;
//     }

//     setError("");

//     const orderData = {
//       customerName: cleanName,
//       email: cleanEmail,
//       mobileNo: cleanMobile,
//       address: cleanAddress,
//       paymentMode: "CASH_ON_DELIVERY",
//       items: cartItems.map((item) => ({
//         productId: item.id,
//         quantity: item.quantity,
//       })),
//     };

//     try {
//       const res = await axios.post("/place", orderData);
//       console.log("Order placed successfully:", res.data);

//       showPopup("Order placed successfully", "success");

//       setTimeout(() => {
//         resetForm();
//         onOrderSuccess();
//       }, 900);
//     } catch (err) {
//       console.error("Checkout failed:", err.response?.data || err.message);

//       const message =
//         err.response?.data?.message || "Checkout failed. Please try again.";

//       setError(message);
//       showPopup(message, "error");
//     }
//   };

//   return (
//     <div className="checkoutPopup">
//       {popup.show && (
//         <div style={popupStyle(popup.type)}>
//           <div style={popupIconStyle}>
//             {popup.type === "success" ? "✓" : "!"}
//           </div>
//           <div>{popup.message}</div>
//         </div>
//       )}

//       <Modal show={show} onHide={handleClose}>
//         <Modal.Header closeButton>
//           <Modal.Title>Checkout</Modal.Title>
//         </Modal.Header>

//         <Modal.Body>
//           <div className="checkout-items">
//             {cartItems.map((item) => (
//               <div
//                 key={item.id}
//                 style={{ display: "flex", marginBottom: "10px" }}
//               >
//                 <img
//                   src={item.imageUrl}
//                   alt={item.name}
//                   style={{ width: "150px", marginRight: "10px" }}
//                 />

//                 <div>
//                   <b>
//                     <p>{item.name}</p>
//                   </b>
//                   <p>Quantity: {item.quantity}</p>
//                   <p>Price: ₹{item.price * item.quantity}</p>
//                 </div>
//               </div>
//             ))}

//             <h5 style={{ textAlign: "center" }}>Total: ₹{totalPrice}</h5>
//           </div>

//           <div style={{ marginTop: "15px" }}>
//             <h6>Enter Details</h6>

//             <input
//               className="form-control mb-2"
//               placeholder="Name"
//               value={name}
//               onChange={(e) => setName(e.target.value)}
//             />

//             <input
//               className="form-control mb-2"
//               placeholder="Email"
//               value={email}
//               onChange={(e) => setEmail(e.target.value)}
//             />

//             <input
//               className="form-control mb-2"
//               placeholder="Mobile"
//               maxLength={10}
//               value={mobile}
//               onChange={(e) => setMobile(e.target.value.replace(/\D/g, ""))}
//             />

//             <textarea
//               className="form-control mb-2"
//               placeholder="Delivery Address"
//               rows={3}
//               maxLength={500}
//               value={address}
//               onChange={(e) => setAddress(e.target.value)}
//             />

//             <input
//               className="form-control mb-2"
//               value="Cash on Delivery"
//               readOnly
//             />

//             {error && <p style={{ color: "red" }}>{error}</p>}
//           </div>
//         </Modal.Body>

//         <Modal.Footer>
//           <Button onClick={handleClose}>Close</Button>
//           <Button onClick={handleConfirm}>Confirm</Button>
//         </Modal.Footer>
//       </Modal>
//     </div>
//   );
// };

// const popupStyle = (type) => ({
//   position: "fixed",
//   top: "80px",
//   right: "24px",
//   zIndex: 99999,
//   minWidth: "280px",
//   maxWidth: "380px",
//   padding: "14px 18px",
//   borderRadius: "14px",
//   color: "#fff",
//   fontWeight: "600",
//   display: "flex",
//   alignItems: "center",
//   gap: "12px",
//   boxShadow: "0 12px 30px rgba(0,0,0,0.25)",
//   background:
//     type === "success"
//       ? "linear-gradient(135deg, #16a34a, #22c55e)"
//       : "linear-gradient(135deg, #dc2626, #f97316)",
// });

// const popupIconStyle = {
//   width: "28px",
//   height: "28px",
//   borderRadius: "50%",
//   background: "rgba(255,255,255,0.25)",
//   display: "flex",
//   alignItems: "center",
//   justifyContent: "center",
//   fontWeight: "bold",
//   flexShrink: 0,
// };

// export default CheckoutPopup;




import { useState } from "react";
import { Modal, Button } from "react-bootstrap";
import axios from "../axios";
import openRazorpayCheckout from "./RazorpayPayment";

const CheckoutPopup = ({
  show,
  handleClose,
  cartItems,
  totalPrice,
  onOrderSuccess,
}) => {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [mobile, setMobile] = useState("");
  const [address, setAddress] = useState("");
  const [paymentMode, setPaymentMode] = useState("CASH_ON_DELIVERY");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

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

  const resetForm = () => {
    setName("");
    setEmail("");
    setMobile("");
    setAddress("");
    setPaymentMode("CASH_ON_DELIVERY");
    setError("");
  };

  const validateForm = () => {
    const cleanName = name.trim();
    const cleanEmail = email.trim().toLowerCase();
    const cleanMobile = mobile.trim();
    const cleanAddress = address.trim();

    if (!cleanName || !cleanEmail || !cleanMobile || !cleanAddress) {
      return "All fields are required";
    }

    if (cleanName.length < 2 || cleanName.length > 80) {
      return "Name must be between 2 and 80 characters";
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(cleanEmail)) {
      return "Enter valid email address";
    }

    if (!/^[6-9]\d{9}$/.test(cleanMobile)) {
      return "Enter valid mobile number";
    }

    if (cleanAddress.length < 10 || cleanAddress.length > 500) {
      return "Address must be between 10 and 500 characters";
    }

    return "";
  };

  const handleSuccess = (message) => {
    showPopup(message, "success");

    setTimeout(() => {
      resetForm();
      onOrderSuccess();
    }, 900);
  };

  const handleConfirm = async () => {
    const validationError = validateForm();

    if (validationError) {
      setError(validationError);
      showPopup(validationError, "error");
      return;
    }

    setError("");
    setLoading(true);

    const cleanName = name.trim();
    const cleanEmail = email.trim().toLowerCase();
    const cleanMobile = mobile.trim();
    const cleanAddress = address.trim();

    const orderData = {
      customerName: cleanName,
      email: cleanEmail,
      mobileNo: cleanMobile,
      address: cleanAddress,
      paymentMode,
      items: cartItems.map((item) => ({
        productId: item.id,
        quantity: item.quantity,
      })),
    };

    try {
      const orderResponse = await axios.post("/place", orderData);
      const order = orderResponse.data;

      if (paymentMode === "CASH_ON_DELIVERY") {
        handleSuccess("Order placed successfully");
        return;
      }

      openRazorpayCheckout({
        order,
        customerName: cleanName,
        email: cleanEmail,
        mobileNo: cleanMobile,
        onSuccess: () => {
          setLoading(false);
          handleSuccess("Payment successful. Order placed successfully");
        },
        onFailure: (message) => {
          setLoading(false);
          setError(message);
          showPopup(message, "error");
        },
      });
    } catch (err) {
      const message =
        err.response?.data?.message || "Checkout failed. Please try again.";

      setError(message);
      showPopup(message, "error");
      setLoading(false);
    }
  };

  return (
    <div className="checkoutPopup">
      {popup.show && (
        <div style={popupStyle(popup.type)}>
          <div style={popupIconStyle}>
            {popup.type === "success" ? "✓" : "!"}
          </div>
          <div>{popup.message}</div>
        </div>
      )}

      <Modal show={show} onHide={handleClose}>
        <Modal.Header closeButton>
          <Modal.Title>Checkout</Modal.Title>
        </Modal.Header>

        <Modal.Body>
          <div className="checkout-items">
            {cartItems.map((item) => (
              <div
                key={item.id}
                style={{ display: "flex", marginBottom: "10px" }}
              >
                <img
                  src={item.imageUrl}
                  alt={item.name}
                  style={{ width: "150px", marginRight: "10px" }}
                />

                <div>
                  <b>
                    <p>{item.name}</p>
                  </b>
                  <p>Quantity: {item.quantity}</p>
                  <p>Price: ₹{item.price * item.quantity}</p>
                </div>
              </div>
            ))}

            <h5 style={{ textAlign: "center" }}>Total: ₹{totalPrice}</h5>
          </div>

          <div style={{ marginTop: "15px" }}>
            <h6>Enter Details</h6>

            <input
              className="form-control mb-2"
              placeholder="Name"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />

            <input
              className="form-control mb-2"
              placeholder="Email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />

            <input
              className="form-control mb-2"
              placeholder="Mobile"
              maxLength={10}
              value={mobile}
              onChange={(e) => setMobile(e.target.value.replace(/\D/g, ""))}
            />

            <textarea
              className="form-control mb-2"
              placeholder="Delivery Address"
              rows={3}
              maxLength={500}
              value={address}
              onChange={(e) => setAddress(e.target.value)}
            />

            <select
              className="form-control mb-2"
              value={paymentMode}
              onChange={(e) => setPaymentMode(e.target.value)}
            >
              <option value="CASH_ON_DELIVERY">Cash on Delivery</option>
              <option value="ONLINE">Online Payment</option>
            </select>

            {error && <p style={{ color: "red" }}>{error}</p>}
          </div>
        </Modal.Body>

        <Modal.Footer>
          <Button onClick={handleClose} disabled={loading}>
            Close
          </Button>

          <Button onClick={handleConfirm} disabled={loading}>
            {loading
              ? "Please wait..."
              : paymentMode === "ONLINE"
              ? "Pay Now"
              : "Confirm"}
          </Button>
        </Modal.Footer>
      </Modal>
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

export default CheckoutPopup;