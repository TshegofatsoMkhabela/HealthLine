import { Outlet } from "react-router-dom";
import StatusRail from "../components/StatusRail";

export default function AppLayout() {
  return (
    <div className="min-h-screen bg-ink text-paper flex flex-col">
      <StatusRail />
      <main className="flex-1 w-full max-w-md mx-auto px-4 py-6 sm:max-w-lg">
        <Outlet />
      </main>
    </div>
  );
}
