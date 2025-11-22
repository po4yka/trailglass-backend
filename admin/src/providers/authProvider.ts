import { AuthBindings } from "@refinedev/core";

const API_URL = import.meta.env.VITE_API_URL?.replace('/api/v1', '') || 'http://localhost:8080';

export const authProvider: AuthBindings = {
  login: async ({ email, password }) => {
    try {
      const response = await fetch(`${API_URL}/auth/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email,
          password,
          deviceInfo: {
            deviceId: "admin-panel",
            deviceName: "Admin Panel",
            platform: "web",
            osVersion: "1.0",
            appVersion: "1.0.0",
          },
        }),
      });

      if (response.ok) {
        const data = await response.json();
        localStorage.setItem("accessToken", data.tokens.accessToken);
        localStorage.setItem("refreshToken", data.tokens.refreshToken);
        localStorage.setItem("user", JSON.stringify({
          userId: data.user.userId,
          email: data.user.email,
          displayName: data.user.displayName,
        }));

        return {
          success: true,
          redirectTo: "/",
        };
      }

      const error = await response.json();
      return {
        success: false,
        error: {
          name: "Login Error",
          message: error.message || "Invalid email or password",
        },
      };
    } catch (error) {
      return {
        success: false,
        error: {
          name: "Login Error",
          message: "Network error. Please check if the backend is running.",
        },
      };
    }
  },

  logout: async () => {
    try {
      const accessToken = localStorage.getItem("accessToken");
      if (accessToken) {
        await fetch(`${API_URL}/auth/logout`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${accessToken}`,
            "X-Device-ID": "admin-panel",
          },
          body: JSON.stringify({ deviceId: "admin-panel" }),
        });
      }
    } catch (error) {
      console.error("Logout error:", error);
    }

    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("user");

    return {
      success: true,
      redirectTo: "/login",
    };
  },

  check: async () => {
    const token = localStorage.getItem("accessToken");

    if (!token) {
      return {
        authenticated: false,
        redirectTo: "/login",
      };
    }

    return {
      authenticated: true,
    };
  },

  getPermissions: async () => {
    return null;
  },

  getIdentity: async () => {
    const userStr = localStorage.getItem("user");
    if (userStr) {
      const user = JSON.parse(userStr);
      return {
        id: user.userId,
        name: user.displayName,
        email: user.email,
      };
    }
    return null;
  },

  onError: async (error) => {
    if (error.statusCode === 401) {
      return {
        logout: true,
        redirectTo: "/login",
      };
    }

    return { error };
  },
};
