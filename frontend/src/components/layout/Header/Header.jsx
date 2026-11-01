import { useState, useEffect, useRef } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../context/AuthContext';
import { useCurrency } from '../../../context/CurrencyContext';
import { useTheme } from '../../../context/ThemeContext';
import { Button } from '../../common';
import { BrandLogo } from '../../common/BrandLogo/BrandLogo';
import './Header.css';
export const Header = () => {
    const { user, isAuthenticated, logout } = useAuth();
    const { currency, changeCurrency } = useCurrency();
    const { theme, toggleTheme } = useTheme();
    const location = useLocation();
    const navigate = useNavigate();
    // Dropdown states
    const [isCurrencyOpen, setIsCurrencyOpen] = useState(false);
    const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
    const [isLogoutModalOpen, setIsLogoutModalOpen] = useState(false);
    // Refs for click outside
    const currencyRef = useRef(null);
    const userMenuRef = useRef(null);
    // Client auth history check
    const [hasPrevLogin, setHasPrevLogin] = useState(() => {
        return localStorage.getItem('has_previously_logged_in') === 'true';
    });
    useEffect(() => {
        const checkLoginHistory = () => {
            const history = localStorage.getItem('has_previously_logged_in');
            setTimeout(() => {
                setHasPrevLogin(history === 'true');
            }, 0);
        };
        checkLoginHistory();
        // Listen for storage updates in case they change login state in another tab
        window.addEventListener('storage', checkLoginHistory);
        return () => window.removeEventListener('storage', checkLoginHistory);
    }, [isAuthenticated]);
    // Set the login history flag when user becomes authenticated
    useEffect(() => {
        if (isAuthenticated) {
            localStorage.setItem('has_previously_logged_in', 'true');
            setTimeout(() => {
                setHasPrevLogin(true);
            }, 0);
        }
    }, [isAuthenticated]);
    // Click outside listener
    useEffect(() => {
        const handleClickOutside = (e) => {
            if (currencyRef.current && !currencyRef.current.contains(e.target)) {
                setIsCurrencyOpen(false);
            }
            if (userMenuRef.current && !userMenuRef.current.contains(e.target)) {
                setIsUserMenuOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);
    const handleLogoutConfirm = () => {
        setIsLogoutModalOpen(false);
        setIsUserMenuOpen(false);
        logout();
        // If the user is on a protected route, redirect to home page "/"
        const protectedRoutes = ['/profile', '/reservations', '/admin', '/driver'];
        const isProtectedRoute = protectedRoutes.some(route => location.pathname.startsWith(route));
        if (isProtectedRoute) {
            navigate('/');
        }
    };
    const isHomepage = location.pathname === '/';
    // Get user initials for default avatar fallback
    const getUserInitials = () => {
        if (!user)
            return '?';
        const first = user.firstName ? user.firstName[0] : '';
        const last = user.lastName ? user.lastName[0] : '';
        return (first + last).toUpperCase() || user.email[0].toUpperCase();
    };
    const currencyNames = {
        INR: 'INR (₹)',
    };
    return (<>
      <header className={`header ${isHomepage ? 'header-transparent' : 'header-solid'} ${location.pathname.startsWith('/profile') ? 'header-static' : ''}`}>
        <div className="header-container">
          <Link to="/" className="header-logo">
            <BrandLogo />
          </Link>

          <nav className="header-nav" aria-label="Main navigation">
            <Link to="/" className="header-nav-link">Home</Link>
            <Link to="/#popular-routes" className="header-nav-link">Routes</Link>
            <Link to="/#features" className="header-nav-link">About</Link>
            <Link to="/#footer-links" className="header-nav-link">Contact</Link>
          </nav>

          <div className="header-actions">
            <button type="button" className="theme-toggle" onClick={toggleTheme} aria-label={`Switch to ${theme === 'light' ? 'dark' : 'light'} mode`} aria-pressed={theme === 'dark'} title={`Switch to ${theme === 'light' ? 'dark' : 'light'} mode`}>
              {theme === 'light' ? (<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <path d="M20.2 15.4A8.4 8.4 0 0 1 8.6 3.8 8.5 8.5 0 1 0 20.2 15.4Z"/>
                </svg>) : (<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <circle cx="12" cy="12" r="4"/>
                  <path d="M12 2v2m0 16v2M4.93 4.93l1.42 1.42m11.3 11.3 1.42 1.42M2 12h2m16 0h2M4.93 19.07l1.42-1.42m11.3-11.3 1.42-1.42"/>
                </svg>)}
            </button>

            {/* Keep the homepage header focused on navigation and account actions. */}
            {!isHomepage && <div className="currency-selector" ref={currencyRef}>
              <button type="button" className={`currency-trigger ${isCurrencyOpen ? 'open' : ''}`} aria-haspopup="true" aria-expanded={isCurrencyOpen} onClick={() => setIsCurrencyOpen(prev => !prev)}>
                <span>{currency}</span>
                <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" strokeWidth="2.5" fill="none" className={`chevron-icon ${isCurrencyOpen ? 'open' : ''}`}>
                  <polyline points="6 9 12 15 18 9"/>
                </svg>
              </button>

              {isCurrencyOpen && (<div className="currency-dropdown">
                  {Object.keys(currencyNames).map((cur) => (<button key={cur} type="button" className={`currency-option ${cur === currency ? 'active' : ''}`} onClick={() => {
                        changeCurrency(cur);
                        setIsCurrencyOpen(false);
                    }}>
                      {currencyNames[cur]}
                    </button>))}
                </div>)}
            </div>}

            {/* Auth / Profile Area */}
            {isAuthenticated && user ? (<>
                {user.role === 'admin' && (<Link to="/admin" className="nav-link">Admin</Link>)}
                {user.role === 'driver' && (<Link to="/driver" className="nav-link">My Trips</Link>)}

                {/* My Reservations - styled exactly like the original Login button */}
                <Link to="/reservations" className={`reservations-header-btn${location.pathname.startsWith('/reservations') ? ' active' : ''}`} aria-current={location.pathname.startsWith('/reservations') ? 'page' : undefined}>
                  <Button variant="ghost" size="md">My Reservations</Button>
                </Link>

                {/* Authenticated Avatar Menu */}
                <div className="user-profile-menu" ref={userMenuRef}>
                  <button type="button" className="profile-trigger" onClick={() => setIsUserMenuOpen(prev => !prev)}>
                    <div className="avatar-circle">
                      {user.avatarData ? (<img src={user.avatarData} alt={user.firstName} className="avatar-img"/>) : (<span className="avatar-initials">{getUserInitials()}</span>)}
                    </div>
                    <span className="user-name-text">{user.firstName}</span>
                    <svg viewBox="0 0 24 24" width="12" height="12" stroke="currentColor" strokeWidth="2.5" fill="none" className={`chevron-icon ${isUserMenuOpen ? 'open' : ''}`}>
                      <polyline points="6 9 12 15 18 9"/>
                    </svg>
                  </button>

                  {isUserMenuOpen && (<div className="profile-dropdown">
                      <Link to="/reservations" className="dropdown-item dropdown-reservations-link" onClick={() => setIsUserMenuOpen(false)}>
                        <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" strokeWidth="2" fill="none" className="item-icon">
                          <rect x="3" y="4" width="18" height="16" rx="2"/>
                          <path d="M16 2v4M8 2v4M3 10h18"/>
                        </svg>
                        My Reservations
                      </Link>
                      <Link to="/profile" className="dropdown-item" onClick={() => setIsUserMenuOpen(false)}>
                        <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" strokeWidth="2" fill="none" className="item-icon">
                          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                          <circle cx="12" cy="7" r="4"/>
                        </svg>
                        My Profile
                      </Link>
                      <button type="button" className="dropdown-item logout-item" onClick={() => setIsLogoutModalOpen(true)}>
                        <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" strokeWidth="2" fill="none" className="item-icon">
                          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                          <polyline points="16 17 21 12 16 7"/>
                          <line x1="21" y1="12" x2="9" y2="12"/>
                        </svg>
                        Logout
                      </button>
                    </div>)}
                </div>
              </>) : (<>
                {/* Single Auth Button based on client browser history */}
                <Link to={hasPrevLogin ? '/login' : '/register'}>
                  <Button variant="primary" size="md">
                    {hasPrevLogin ? 'Sign In' : 'Sign Up'}
                  </Button>
                </Link>
              </>)}
          </div>
        </div>
      </header>

      {/* Logout Confirmation Modal Overlay */}
      {isLogoutModalOpen && (<div className="logout-modal-overlay" onClick={() => setIsLogoutModalOpen(false)}>
          <div className="logout-modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-icon">
              <svg viewBox="0 0 24 24" width="28" height="28" stroke="var(--primary)" strokeWidth="2" fill="none">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                <polyline points="16 17 21 12 16 7"/>
                <line x1="21" y1="12" x2="9" y2="12"/>
              </svg>
            </div>
            <h2>Confirm Logout</h2>
            <p>Are you sure you want to log out of your BusKaroo account?</p>
            <div className="modal-buttons">
              <Button variant="ghost" onClick={() => {
                setIsLogoutModalOpen(false);
                setIsUserMenuOpen(false);
            }}>
                Cancel
              </Button>
              <Button variant="primary" onClick={handleLogoutConfirm} className="btn-danger">
                Logout
              </Button>
            </div>
          </div>
        </div>)}
    </>);
};
export default Header;
