import './Card.css';
export const Card = ({ children, className = '', variant = 'default', hoverable = false, onClick, }) => {
    return (<div className={`card card-${variant} ${hoverable ? 'card-hoverable' : ''} ${className}`} onClick={onClick} role={onClick ? 'button' : undefined} tabIndex={onClick ? 0 : undefined}>
      {children}
    </div>);
};
export default Card;
