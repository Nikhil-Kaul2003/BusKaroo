import api from './client';
export const routesApi = {
    getAll: async () => {
        const response = await api.get('/routes');
        return response.data;
    },
    getById: async (id) => {
        const response = await api.get(`/routes/${id}`);
        return response.data;
    },
    getPopular: async (country) => {
        const response = await api.get('/routes/popular', {
            params: country ? { country } : undefined
        });
        return response.data;
    },
};
export default routesApi;
