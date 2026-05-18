import { useState } from "react";
import { useNavigate } from "react-router-dom";
import GoogleLogin from "./GoogleLogin";

function Login({ onLogin }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [popup, setPopup] = useState({
    show: false,
    message: "",
    type: "success",
  });

  const navigate = useNavigate();

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

  const saveLoginData = (data) => {
    localStorage.setItem("token", data.token);
    localStorage.setItem("role", data.role || "USER");
    localStorage.setItem("username", data.username || data.name || "User");
    localStorage.setItem("email", data.email || email);

    if (onLogin) {
      onLogin(data.token);
    }

    showPopup("Login successful", "success");

    setTimeout(() => {
      navigate("/");
    }, 900);
  };

  const handleLogin = async (e) => {
    e.preventDefault();

    try {
      const res = await fetch("${API_ORIGIN}/api/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });

      if (!res.ok) {
        const errorText = await res.text();
        throw new Error(errorText || "Invalid credentials");
      }

      const data = await res.json();
      saveLoginData(data);
    } catch (err) {
      showPopup(err.message || "Login failed", "error");
    }
  };

  return (
    <div style={styles.container}>
      {popup.show && (
        <div style={popupStyle(popup.type)}>
          <div style={popupIconStyle}>
            {popup.type === "success" ? "✓" : "!"}
          </div>
          <div>{popup.message}</div>
        </div>
      )}

      <div style={styles.card}>
        <h2 style={styles.title}>Welcome Back</h2>

        <form onSubmit={handleLogin} style={styles.form}>
          <input
            style={styles.input}
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />

          <input
            style={styles.input}
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />

          <button style={styles.button} type="submit">
            Login
          </button>
        </form>

        <button
          style={styles.signupButton}
          onClick={() => navigate("/register")}
        >
          New user? Sign Up
        </button>

        <div style={styles.divider}>OR</div>

        <div style={styles.googleBox}>
          <GoogleLogin
            onSuccess={(data) => {
              saveLoginData(data);
            }}
            onFailure={(err) => console.error(err)}
          />
        </div>
      </div>
    </div>
  );
}

const styles = {
  container: {
    height: "100vh",
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    background: "linear-gradient(135deg, #43cea2, #185a9d)",
  },
  card: {
    width: "90%",
    maxWidth: "380px",
    background: "#fff",
    padding: "30px",
    borderRadius: "12px",
    boxShadow: "0 10px 30px rgba(0,0,0,0.2)",
    textAlign: "center",
  },
  title: {
    marginBottom: "20px",
    color: "#333",
  },
  form: {
    display: "flex",
    flexDirection: "column",
    gap: "15px",
  },
  input: {
    padding: "12px",
    borderRadius: "8px",
    border: "1px solid #ccc",
  },
  button: {
    padding: "12px",
    borderRadius: "8px",
    border: "none",
    background: "#185a9d",
    color: "#fff",
    fontWeight: "bold",
    cursor: "pointer",
  },
  signupButton: {
    marginTop: "12px",
    padding: "12px",
    width: "100%",
    borderRadius: "10px",
    border: "none",
    background: "linear-gradient(135deg, #ff6a00, #ee0979)",
    color: "#fff",
    fontWeight: "bold",
    fontSize: "14px",
    cursor: "pointer",
    transition: "all 0.3s ease",
    boxShadow: "0 4px 12px rgba(238, 9, 121, 0.3)",
  },
  divider: {
    margin: "15px 0",
    fontWeight: "bold",
    color: "#888",
  },
  googleBox: {
    display: "flex",
    justifyContent: "center",
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

export default Login;
