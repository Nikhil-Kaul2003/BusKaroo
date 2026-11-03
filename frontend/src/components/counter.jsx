import { useEffect, useRef, useState } from "react";

const Counter = ({ end, suffix = "" }) => {
  const [count, setCount] = useState(0);
  const [isVisible, setIsVisible] = useState(false);
  const counterRef = useRef(null);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsVisible(true);
          observer.disconnect();
        }
      },
      { threshold: 0.5 }
    );

    if (counterRef.current) {
      observer.observe(counterRef.current);
    }

    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    if (!isVisible) return;

    let current = 0;
    const duration = 800;
    const steps = 40;
    const increment = end / steps;

    const timer = setInterval(() => {
      current += increment;

      if (current >= end) {
        current = end;
        clearInterval(timer);
      }

      setCount(Math.floor(current));
    }, duration / steps);

    return () => clearInterval(timer);
  }, [isVisible, end]);

  return (
    <span ref={counterRef}>
      {count}{suffix}
    </span>
  );
};

export default Counter;