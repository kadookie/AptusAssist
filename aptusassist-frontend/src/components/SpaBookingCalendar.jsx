import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import axios from 'axios';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { formatDate, getDefaultPassDate } from '../utils/date';

const SpaBookingCalendar = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [passDate, setPassDate] = useState(() => {
    const paramDate = searchParams.get('passDate');
    const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
    if (paramDate && dateRegex.test(paramDate)) {
      return paramDate;
    }
    return getDefaultPassDate();
  });
  const freeOnly = searchParams.get('freeOnly') === 'true';
  const [data, setData] = useState(null);
  const [isInitialLoad, setIsInitialLoad] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    // Sync passDate with searchParams
    if (searchParams.get('passDate') !== passDate) {
      setSearchParams({ passDate, freeOnly });
    }

    const fetchData = async () => {
      setError(null);
      try {
        const url = `/slots?passDate=${encodeURIComponent(passDate)}`;
        const res = await axios.get(url);
        setData(res.data);
      } catch (err) {
        console.error('Error fetching data:', err);
        setError('Failed to load spa slots. Please try again.');
      } finally {
        setIsInitialLoad(false);
      }
    };
    fetchData();
  }, [passDate, setSearchParams]);

  const handleSlotClick = async (passNo, passDate) => {
    try {
      const response = await axios.post('/book', null, {
        params: { passNo, passDate }
      });

      if (response.data.status === 'success') {
        toast.success('Slot booked successfully!');
        const url = `/slots?passDate=${encodeURIComponent(passDate)}`;
        const res = await axios.get(url);
        setData(res.data);
      } else {
        toast.error('Error: ' + response.data.message);
      }
    } catch (error) {
      toast.error('Failed to book slot: ' + error.message);
    }
  };

  const toggleFreeOnly = () => {
    setSearchParams({ passDate, freeOnly: !freeOnly });
  };

  const navigateWeek = (newPassDate) => {
    const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
    if (!dateRegex.test(newPassDate)) {
      console.warn(`Invalid newPassDate: ${newPassDate}. Using current passDate.`);
      return;
    }
    setPassDate(newPassDate);
  };

  if (isInitialLoad && !data) return (
    <div className="text-center p-2 text-gray-500">Loading slots...</div>
  );
  if (error) return <div className="text-center p-2 text-red-500">{error}</div>;

  return (
    <div className="container mx-auto p-2">
      <div className="mb-4">
        <div className="flex justify-between items-center mb-2">
          <h1 className="text-xl font-bold">Calendar</h1>
          <label className="relative inline-flex items-center cursor-pointer">
            <input
              type="checkbox"
              className="sr-only peer"
              checked={freeOnly}
              onChange={toggleFreeOnly}
            />
            <div className="w-9 h-5 bg-gray-200 rounded-full peer peer-checked:bg-blue-500 transition-colors"></div>
            <div className="absolute w-3 h-3 bg-white rounded-full top-1 left-1 peer-checked:translate-x-4 transition-transform"></div>
          </label>
        </div>
        <div className="flex justify-between items-center">
          <button
            onClick={() => navigateWeek(data?.prevWeekDate)}
            className={`px-3 py-1 rounded-md font-medium text-gray-700 bg-gray-300 hover:bg-gray-400 transition-colors ${!data?.prevWeekDate ? 'opacity-50 cursor-not-allowed' : ''
              }`}
            disabled={!data?.prevWeekDate}
          >
            Previous Week
          </button>
          <span className="text-base font-semibold">Week {data?.currentWeek}</span>
          <button
            onClick={() => navigateWeek(data?.nextWeekDate)}
            className={`px-3 py-1 rounded-md font-medium text-gray-700 bg-gray-300 hover:bg-gray-400 transition-colors ${!data?.nextWeekDate ? 'opacity-50 cursor-not-allowed' : ''
              }`}
            disabled={!data?.nextWeekDate}
          >
            Next Week
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-2">
        {data?.weekDays.map((day) => (
          <div key={day.date} className="border p-2 bg-white rounded shadow">
            <h2 className="text-base font-semibold">{`${day.dayName} ${formatDate(day.date, 'dd MMM')}`}</h2>
            {day.slots
              .filter((slot) => !freeOnly || slot.status === 'free')
              .map((slot) => (
                <div
                  key={slot.passNo}
                  className={`mt-2 p-2 rounded cursor-pointer flex justify-center items-center ${slot.status === 'free'
                    ? 'bg-green-500 text-white hover:bg-green-600'
                    : slot.status === 'own'
                      ? 'bg-blue-500 text-white'
                      : 'bg-gray-300 text-gray-700 cursor-not-allowed'
                    }`}
                  onClick={() => slot.status === 'free' && handleSlotClick(slot.passNo, slot.passDate)}
                >
                  <span>{slot.time}</span>
                </div>
              ))}
          </div>
        ))}
      </div>

      <ToastContainer position="top-right" autoClose={2000} />
    </div>
  );
};

export default SpaBookingCalendar;