import { useEffect, useState } from "react";
import { API_ORIGIN } from "../axios";

function GoogleLogin({ onSuccess, onFailure }) {
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

  const handleCredentialResponse = async (response) => {
    try {
      const res = await fetch(`${API_ORIGIN}/api/login/google`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          idToken: response.credential,
        }),
      });

      if (!res.ok) {
        const errorText = await res.text();
        throw new Error(errorText || "Google login failed");
      }

      const data = await res.json();

      if (onSuccess) {
        onSuccess(data);
      }
    } catch (err) {
      showPopup(err.message || "Google login failed", "error");

      if (onFailure) {
        onFailure(err);
      }
    }
  };

  useEffect(() => {
    if (!window.google) {
      showPopup("Google login is not available", "error");
      return;
    }

    window.google.accounts.id.initialize({
      client_id:
        "531093496003-5trvu29u047dh091n2pdj35flpvdnpmq.apps.googleusercontent.com",
      callback: handleCredentialResponse,
    });

    window.google.accounts.id.renderButton(document.getElementById("googleBtn"), {
      theme: "outline",
      size: "large",
      width: 280,
    });
  }, []);

  return (
    <div style={styles.googleBtn}>
      {popup.show && (
        <div style={popupStyle(popup.type)}>
          <div style={popupIconStyle}>
            {popup.type === "success" ? "✓" : "!"}
          </div>
          <div>{popup.message}</div>
        </div>
      )}

      <div id="googleBtn"></div>
    </div>
  );
}

const styles = {
  googleBtn: {
    display: "flex",
    justifyContent: "center",
    marginTop: "10px",
  },
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

export default GoogleLogin;