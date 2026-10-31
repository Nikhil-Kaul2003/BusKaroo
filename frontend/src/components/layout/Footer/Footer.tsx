import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { BrandLogo } from '../../common/BrandLogo/BrandLogo';
import './Footer.css';

// Define the custom element for the footer
if (typeof window !== 'undefined' && !customElements.get('buskaroo-footer-info')) {
  class BusKarooFooterInfo extends HTMLElement {
    connectedCallback() {
      this.textContent = 'BusKaroo • Your journey, made simple';
      this.style.display = 'block';
      this.style.fontSize = 'var(--text-sm, 0.875rem)';
      this.style.color = 'rgba(255, 255, 255, 0.6)';
      this.style.marginTop = 'var(--space-xs, 0.25rem)';
      this.style.textAlign = 'center';
    }
  }
  customElements.define('buskaroo-footer-info', BusKarooFooterInfo);
}

export const Footer: React.FC = () => {
  const navigate = useNavigate();

  const handleHashLinkClick = (e: React.MouseEvent<HTMLAnchorElement>, targetId: string) => {
    const element = document.getElementById(targetId);
    if (element) {
      e.preventDefault();
      element.scrollIntoView({ behavior: 'smooth', block: 'center' });
      window.history.pushState(null, '', `${window.location.pathname}#${targetId}`);
    } else {
      e.preventDefault();
      navigate(`/#${targetId}`);
    }
  };

  useEffect(() => {
    const container = document.getElementById('footer-custom-info-container');
    if (container) {
      container.innerHTML = '';
      const customEl = document.createElement('buskaroo-footer-info');
      container.appendChild(customEl);
    }
  }, []);

  return (
    <footer className="footer">
      <div className="footer-container">
        <div className="footer-brand">
          <BrandLogo />
          <p className="footer-tagline">Travel by bus all across India</p>
        </div>

        <nav className="footer-links" id="footer-links" aria-label="Footer Navigation">
          <section className="footer-section">
            <h4>Travel</h4>
            <a href="/#search-panel" onClick={(e) => handleHashLinkClick(e, 'search-panel')}>Find Trips</a>
            <a href="/#popular-routes" onClick={(e) => handleHashLinkClick(e, 'popular-routes')}>Popular Routes</a>
          </section>

          <section className="footer-section">
            <h4>Support</h4>
            <a href="/help">Help Center</a>
            <a href="/contact">Contact Us</a>
          </section>

          <section className="footer-section">
            <h4>Legal</h4>
            <a href="/terms">Terms of Service</a>
            <a href="/privacy">Privacy Policy</a>
          </section>
        </nav>

        <div className="footer-bottom">
          <div id="footer-custom-info-container"></div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
