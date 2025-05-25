import { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import SpaBookingCalendar from './components/SpaBookingCalendar';
import { registerPush } from './utils/push';

function App() {
  useEffect(() => {
    registerPush();
  }, []);

  return (
    <Router>
      <Routes>
        <Route path="/" element={<SpaBookingCalendar />} />
      </Routes>
    </Router>
  );
}

export default App;
