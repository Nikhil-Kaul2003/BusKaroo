import React from 'react';
import './Input.css';
export const Input = React.forwardRef(({ label, error, icon, className = '', id, ...props }, ref) => {
    const generatedId = React.useId();
    const inputId = id || generatedId;
    return (<div className={`input-wrapper ${error ? 'input-error' : ''} ${className}`}>
      {label && (<label htmlFor={inputId} className="input-label">
          {label}
        </label>)}
      <div className="input-container">
        {icon && <span className="input-icon">{icon}</span>}
        <input id={inputId} ref={ref} className={`input ${icon ? 'input-with-icon' : ''}`} {...props}/>
      </div>
      {error && <span className="input-error-message">{error}</span>}
    </div>);
});
Input.displayName = 'Input';
export default Input;
