import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import SpaBookingCalendar from './components/SpaBookingCalendar';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<SpaBookingCalendar />} />
      </Routes>
    </Router>
  );
}

export default App;