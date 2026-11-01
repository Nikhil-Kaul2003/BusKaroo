import './Button.css';
export const Button = ({ children, variant = 'primary', size = 'md', loading = false, fullWidth = false, icon, className = '', disabled, ...props }) => {
    return (<button className={`btn btn-${variant} btn-${size} ${fullWidth ? 'btn-full' : ''} ${className}`} disabled={disabled || loading} {...props}>
      {loading ? (<span className="btn-spinner"/>) : (<>
          {icon && <span className="btn-icon">{icon}</span>}
          {children}
        </>)}
    </button>);
};
export default Button;
