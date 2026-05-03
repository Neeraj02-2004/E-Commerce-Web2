
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import GoogleLogin from "./GoogleLogin";

function Login({ onLogin }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();

    try {
      const res = await fetch("http://localhost:8080/api/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });

      if (!res.ok) throw new Error("Invalid credentials");

      const data = await res.json();

      localStorage.setItem("token", data.token);
      onLogin(data.token);

      navigate("/");

    } catch (err) {
      alert(err.message);
    }
  };

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <h2 style={styles.title}>Welcome Back</h2>

        <form onSubmit={handleLogin} style={styles.form}>
          <input
            style={styles.input}
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />

          <input
            style={styles.input}
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          <button style={styles.button} type="submit">
            Login
          </button>
        </form>

        {/* 🔥 NEW SIGNUP BUTTON */}
        <button
          style={styles.signupButton}
          onClick={() => navigate("/register")}
        >
          New user? Sign Up
        </button>

        <div style={styles.divider}>OR</div>

        <div style={styles.googleBox}>
          <GoogleLogin
            onSuccess={(token) => {
              localStorage.setItem("token", token);
              onLogin(token);
              navigate("/");
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

  // 🔥 NEW BUTTON STYLE
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

export default Login;