import { createContext, useContext, useLayoutEffect, useMemo, useState } from 'react';
const ThemeContext = createContext(undefined);
const getInitialTheme = () => {
    try {
        const savedTheme = window.localStorage.getItem('buskaroo-theme');
        if (savedTheme === 'light' || savedTheme === 'dark')
            return savedTheme;
    }
    catch {
        // Continue with the system preference if storage is unavailable.
    }
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
};
export const ThemeProvider = ({ children }) => {
    const [theme, setTheme] = useState(getInitialTheme);
    useLayoutEffect(() => {
        document.documentElement.dataset.theme = theme;
        document.documentElement.style.colorScheme = theme;
        try {
            window.localStorage.setItem('buskaroo-theme', theme);
        }
        catch {
            // The current theme still applies for this session when storage is blocked.
        }
    }, [theme]);
    const value = useMemo(() => ({
        theme,
        toggleTheme: () => setTheme(current => current === 'light' ? 'dark' : 'light'),
    }), [theme]);
    return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
};
// Fast Refresh supports hook exports alongside components.
// eslint-disable-next-line react-refresh/only-export-components
export const useTheme = () => {
    const context = useContext(ThemeContext);
    if (!context)
        throw new Error('useTheme must be used within ThemeProvider');
    return context;
};
