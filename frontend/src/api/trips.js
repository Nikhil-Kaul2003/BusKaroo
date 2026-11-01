import api from './client';
export const tripsApi = {
    search: async (params, signal) => {
        const response = await api.get('/trips/search', { params, signal });
        return response.data;
    },
    connections: async (params, signal) => {
        const response = await api.get('/connections/search', { params, signal });
        return response.data;
    },
    getById: async (id, params) => {
        const response = await api.get(`/trips/${id}`, { params });
        return response.data;
    },
    getAvailableSeats: async (id) => {
        const response = await api.get(`/trips/${id}/available-seats`);
        return response.data;
    },
    getAll: async () => {
        const response = await api.get('/trips');
        return response.data;
    },
};
export default tripsApi;
