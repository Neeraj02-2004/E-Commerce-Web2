
import React, { useEffect } from "react";

function GoogleLogin({ onSuccess, onFailure }) {
  useEffect(() => {
    if (!window.google) return;

    window.google.accounts.id.initialize({
      client_id: "741978131897-iupho5b0al1glgdub15t1iuk0khbd2t7.apps.googleusercontent.com", // ✅ added directly
      callback: handleCredentialResponse,
    });

    window.google.accounts.id.renderButton(
      document.getElementById("googleBtn"),
      {
        theme: "outline",
        size: "large",
        width: 280,
      }
    );
  }, []);

  const handleCredentialResponse = async (response) => {
    try {
      const res = await fetch("http://localhost:8080/api/login/google", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          idToken: response.credential,
        }),
      });

      if (!res.ok) {
        throw new Error("Google login failed");
      }

      const data = await res.json();

      localStorage.setItem("token", data.token);

      onSuccess(data.token);
    } catch (err) {
      onFailure(err);
    }
  };

  return (
    <div style={styles.googleBtn}>
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

export default GoogleLogin;