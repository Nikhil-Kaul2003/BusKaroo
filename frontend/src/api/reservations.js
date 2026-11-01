import api from './client';
export const reservationsApi = {
    create: async (data) => {
        const response = await api.post('/reservations', data);
        return response.data;
    },
    getMyReservations: async () => {
        // Hosted Spring services can take longer to wake up than ordinary API calls.
        const response = await api.get('/reservations', { timeout: 60000 });
        return response.data;
    },
    getById: async (id) => {
        const response = await api.get(`/reservations/${id}`);
        return response.data;
    },
    cancel: async (id) => {
        await api.delete(`/reservations/${id}`);
    },
    pay: async (id) => {
        await api.post(`/reservations/${id}/pay`);
    },
};
export default reservationsApi;
