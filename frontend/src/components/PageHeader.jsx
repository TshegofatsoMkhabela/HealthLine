// Every screen opens with the same title + subtitle block — extracted here
// instead of copy-pasted per page. `size` defaults to what every screen but
// Home uses; Home passes size="2xl" to preserve its existing look.
//
// Tailwind's JIT scanner needs literal class strings, not interpolated ones
// (`text-${size}` would silently produce no CSS) — hence the lookup map
// instead of a template string.
const TITLE_SIZE_CLASS = {
  xl: "text-xl",
  "2xl": "text-2xl",
};

export default function PageHeader({ title, subtitle, size = "xl" }) {
  return (
    <div>
      <h1 className={`font-display ${TITLE_SIZE_CLASS[size]} font-semibold`}>{title}</h1>
      <p className="text-mist text-sm mt-1">{subtitle}</p>
    </div>
  );
}
