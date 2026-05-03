
import React, { useState } from 'react';
import { Modal, Button } from 'react-bootstrap';
import axios from "../axios"; 

const CheckoutPopup = ({ show, handleClose, cartItems, totalPrice }) => {

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [mobile, setMobile] = useState("");
  const [error, setError] = useState("");

  const [otp, setOtp] = useState("");
  const [otpSent, setOtpSent] = useState(false);
  const [verified, setVerified] = useState(false);

  const sendOtp = async () => {
    if (!/^[6-9]\d{9}$/.test(mobile)) {
      setError("Enter valid mobile number");
      return;
    }

    try {
      await axios.post("/send-otp", { mobile: "+91" + mobile });
      setOtpSent(true);
      setError("");
    } catch (err) {
      setError("Failed to send OTP");
    }
  };

  const verifyOtp = async () => {
    try {
      const res = await axios.post("/verify-otp", {
        mobile: "+91" + mobile,
        otp: otp
      });

      if (res.data === "VERIFIED") {
        setVerified(true);
        setError("");
      } else {
        setError("Invalid OTP");
      }
    } catch (err) {
      setError("OTP verification failed");
    }
  };

  const handleConfirm = async () => {
    if (!name || !email || !mobile) {
      setError("All fields are required!");
      return;
    }

    if (!verified) {
      setError("Please verify OTP first");
      return;
    }

    setError("");

    const orderData = {
      customerName: name,
      email: email,
      mobileNo: mobile,
      items: cartItems.map(item => ({
        productId: item.id,
        quantity: item.quantity
      }))
    };

    try {
      const res = await axios.post("/place", orderData); // ✅ FIXED
      console.log("Order placed successfully:", res.data);
      alert("Order placed successfully!");
      handleClose();
    } catch (err) {
      console.error("Checkout failed:", err.response?.data || err.message);
      setError("Checkout failed. Please try again.");
    }
  };

  return (
    <div className="checkoutPopup">
      <Modal show={show} onHide={handleClose}>
        <Modal.Header closeButton>
          <Modal.Title>Checkout</Modal.Title>
        </Modal.Header>

        <Modal.Body>
          <div className="checkout-items">
            {cartItems.map((item) => (
              <div key={item.id} style={{ display: 'flex', marginBottom: '10px' }}>
                <img src={item.imageUrl} alt={item.name} style={{ width: '150px', marginRight: '10px' }} />
                <div>
                  <b><p>{item.name}</p></b>
                  <p>Quantity: {item.quantity}</p>
                  <p>Price: ₹{item.price * item.quantity}</p>
                </div>
              </div>
            ))}

            <h5 style={{ textAlign: 'center' }}>
              Total: ₹{totalPrice}
            </h5>
          </div>

          <div style={{ marginTop: "15px" }}>
            <h6>Enter Details</h6>

            <input className="form-control mb-2" placeholder="Name" value={name} onChange={(e) => setName(e.target.value)} />
            <input className="form-control mb-2" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} />
            <input className="form-control mb-2" placeholder="Mobile" maxLength={10}
              value={mobile}
              onChange={(e) => setMobile(e.target.value.replace(/\D/g, ""))}
            />

            {!otpSent && <Button onClick={sendOtp}>Send OTP</Button>}

            {otpSent && !verified && (
              <>
                <input className="form-control mb-2" placeholder="OTP" value={otp} onChange={(e) => setOtp(e.target.value)} />
                <Button onClick={verifyOtp}>Verify OTP</Button>
              </>
            )}

            {verified && <p style={{ color: "green" }}>✅ Verified</p>}
            {error && <p style={{ color: "red" }}>{error}</p>}
          </div>
        </Modal.Body>

        <Modal.Footer>
          <Button onClick={handleClose}>Close</Button>
          <Button onClick={handleConfirm}>Confirm</Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default CheckoutPopup;