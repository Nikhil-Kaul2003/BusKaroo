import { createContext, useContext, useState, useEffect } from 'react';
import { useAuth } from './AuthContext';
import api from '../api/client';
const CurrencyContext = createContext(undefined);
const conversionRates = { INR: 1 };
export const CurrencyProvider = ({ children }) => {
    const { user, isAuthenticated, updateUser } = useAuth();
    const [currency, setCurrencyState] = useState(() => {
        return 'INR';
    });
    useEffect(() => {
        if (user && user.preferredCurrency !== 'INR') {
            setTimeout(() => {
                setCurrencyState('INR');
            }, 0);
            localStorage.setItem('preferred_currency', 'INR');
        }
    }, [user]);
    const changeCurrency = async (newCurrency) => {
        setCurrencyState(newCurrency);
        localStorage.setItem('preferred_currency', newCurrency);
        if (isAuthenticated && user) {
            try {
                const updatedUser = {
                    ...user,
                    preferredCurrency: newCurrency,
                };
                // Background API call to save user preferred currency
                await api.put(`/users/${user.id}`, {
                    firstName: user.firstName,
                    lastName: user.lastName,
                    phone: user.phone || null,
                    preferredCurrency: newCurrency,
                    country: user.country || null,
                    avatarData: user.avatarData || null,
                });
                // Sync local auth context user object
                updateUser(updatedUser);
            }
            catch (err) {
                console.error('Failed to save preferred currency to database', err);
            }
        }
    };
    const formatPrice = (amountInINR) => {
        const rate = conversionRates[currency];
        return new Intl.NumberFormat('en-IN', {
            style: 'currency', currency, maximumFractionDigits: 0,
        }).format(amountInINR * rate);
    };
    return (<CurrencyContext.Provider value={{ currency, changeCurrency, formatPrice }}>
      {children}
    </CurrencyContext.Provider>);
};
// eslint-disable-next-line react-refresh/only-export-components
export const useCurrency = () => {
    const context = useContext(CurrencyContext);
    if (context === undefined) {
        throw new Error('useCurrency must be used within a CurrencyProvider');
    }
    return context;
};
export default CurrencyContext;
