import './BrandLogo.css';
export const BrandLogo = ({ orangeKaroo = false }) => (<span className={`brand-logo${orangeKaroo ? ' brand-logo-header' : ''}`} role="img" aria-label="BusKaroo — Book, travel, explore">
    <span className="brand-logo-crop brand-logo-mark" aria-hidden="true">
      <img src="/buskaroo-logo.png" alt=""/>
    </span>
    <span className="brand-logo-crop brand-logo-wordmark" aria-hidden="true">
      <img src="/buskaroo-logo.png" alt=""/>
      {orangeKaroo && <img className="brand-logo-wordmark-orange" src="/buskaroo-logo.png" alt=""/>}
    </span>
  </span>);
