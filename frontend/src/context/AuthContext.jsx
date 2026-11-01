import { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api/auth';
const AuthContext = createContext(undefined);
const isTokenExpired = (token) => {
    try {
        const parts = token.split('.');
        if (parts.length !== 3)
            return true;
        const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(window.atob(base64)
            .split('')
            .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
            .join(''));
        const payload = JSON.parse(jsonPayload);
        return payload.exp ? payload.exp * 1000 < Date.now() : true;
    }
    catch {
        return true;
    }
};
export const AuthProvider = ({ children }) => {
    const [token, setToken] = useState(() => {
        const storedToken = localStorage.getItem('token');
        if (storedToken) {
            if (isTokenExpired(storedToken)) {
                localStorage.removeItem('token');
                localStorage.removeItem('user');
                return null;
            }
            return storedToken;
        }
        return null;
    });
    const [user, setUser] = useState(() => {
        const storedToken = localStorage.getItem('token');
        if (!storedToken)
            return null; // If token was cleared as expired
        const storedUser = localStorage.getItem('user');
        if (storedUser) {
            try {
                return JSON.parse(storedUser);
            }
            catch {
                localStorage.removeItem('token');
                localStorage.removeItem('user');
            }
        }
        return null;
    });
    const [isLoading] = useState(false);
    // Sync React state when Axios interceptor clears localStorage on 401
    useEffect(() => {
        const handleStorageChange = (e) => {
            if (e.key === 'token' && e.newValue === null) {
                setToken(null);
                setUser(null);
            }
        };
        const handleUnauthorizedLogout = () => {
            setToken(null);
            setUser(null);
        };
        window.addEventListener('storage', handleStorageChange);
        window.addEventListener('unauthorized-logout', handleUnauthorizedLogout);
        return () => {
            window.removeEventListener('storage', handleStorageChange);
            window.removeEventListener('unauthorized-logout', handleUnauthorizedLogout);
        };
    }, []);
    const login = async (email, password, expectedRole) => {
        const response = await authApi.login({ email, password });
        if (expectedRole && response.user.role !== expectedRole) {
            throw new Error(expectedRole === 'admin'
                ? 'This account does not have administrator access.'
                : 'This account is registered as an administrator. Choose Admin sign in.');
        }
        setToken(response.accessToken);
        setUser(response.user);
        localStorage.setItem('token', response.accessToken);
        localStorage.setItem('user', JSON.stringify(response.user));
        return response.user.role;
    };
    const registerAdmin = async (email, password, firstName, lastName, registrationKey, phone) => {
        const response = await authApi.registerAdmin({
            email, password, firstName, lastName, phone, registrationKey,
        });
        setToken(response.accessToken);
        setUser(response.user);
        localStorage.setItem('token', response.accessToken);
        localStorage.setItem('user', JSON.stringify(response.user));
    };
    const register = async (email, password, firstName, lastName, phone, preferredCurrency, country, avatarData) => {
        const response = await authApi.register({
            email,
            password,
            firstName,
            lastName,
            phone,
            preferredCurrency,
            country,
            avatarData,
        });
        setToken(response.accessToken);
        setUser(response.user);
        localStorage.setItem('token', response.accessToken);
        localStorage.setItem('user', JSON.stringify(response.user));
    };
    const logout = () => {
        setToken(null);
        setUser(null);
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    };
    const updateUser = (userData) => {
        setUser(userData);
        localStorage.setItem('user', JSON.stringify(userData));
    };
    return (<AuthContext.Provider value={{
            user,
            token,
            isAuthenticated: !!token,
            isLoading,
            login,
            register,
            registerAdmin,
            logout,
            updateUser,
        }}>
      {children}
    </AuthContext.Provider>);
};
// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};
export default AuthContext;
