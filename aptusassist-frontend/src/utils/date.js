// utils/date.js
export const formatDate = (dateStr, format) => {
    const date = new Date(dateStr);
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
        'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    if (format === 'dd MMM') {
        return `${date.getDate()} ${months[date.getMonth()]}`;
    }
    return dateStr;
};

export const getDefaultPassDate = () => {
    const today = new Date();
    const day = today.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    today.setDate(today.getDate() + diff);
    return today.toISOString().split('T')[0];
};
