import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AppProvider } from "./state/AppContext";
import AppLayout from "./layouts/AppLayout";
import Home from "./pages/Home";
import Onboarding from "./pages/Onboarding";
import ProfileBuilder from "./pages/ProfileBuilder";
import DoctorSign from "./pages/DoctorSign";
import Locate from "./pages/Locate";
import Triage from "./pages/Triage";
import Dispatch from "./pages/Dispatch";
import HospitalBoard from "./pages/HospitalBoard";
import Handoff from "./pages/Handoff";

export default function App() {
  return (
    <AppProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="/" element={<Home />} />
            <Route path="/onboarding" element={<Onboarding />} />
            <Route path="/profile" element={<ProfileBuilder />} />
            <Route path="/doctor-sign" element={<DoctorSign />} />
            <Route path="/locate" element={<Locate />} />
            <Route path="/triage" element={<Triage />} />
            <Route path="/dispatch" element={<Dispatch />} />
            <Route path="/hospital" element={<HospitalBoard />} />
            <Route path="/handoff" element={<Handoff />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AppProvider>
  );
}
