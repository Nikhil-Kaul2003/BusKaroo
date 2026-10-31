import axios from 'axios';

// Use the same-origin /api path by default. Vite proxies it to Spring Boot in
// development, which avoids browser CORS and lets the target port be configured
// in one place. Set VITE_API_BASE_URL only when the frontend is hosted separately.
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '');

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const getApiErrorMessage = (error: unknown, fallback: string): string => {
  if (axios.isAxiosError(error)) {
    if (error.code === 'ECONNABORTED') {
      return 'The backend did not respond in time. Make sure Spring Boot is running on port 8082, then try again.';
    }
    if (!error.response) {
      return 'Cannot reach the backend. Start Spring Boot on port 8082, then try again.';
    }
    if (error.response.status === 401) {
      return 'Invalid email or password.';
    }
    const responseMessage = (error.response.data as { message?: string } | undefined)?.message;
    if (responseMessage) return responseMessage;
  }

  return error instanceof Error && error.message ? error.message : fallback;
};

// Request interceptor to add JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for handling errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Don't clear auth on login attempts — 401 there means wrong password
      const url = error.config?.url || '';
      if (!url.includes('/auth/login')) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.dispatchEvent(new Event('unauthorized-logout'));
      }
    }
    return Promise.reject(error);
  }
);

export default api;
