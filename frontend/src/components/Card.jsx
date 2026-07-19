export default function Card({ children, className = "" }) {
  return (
    <div
      className={`rounded-xl border border-line bg-panel p-5 ${className}`}
    >
      {children}
    </div>
  );
}
