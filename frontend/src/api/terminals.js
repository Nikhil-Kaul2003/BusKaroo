import api from './client';
export const terminalsApi = {
    getAll: async () => {
        const response = await api.get('/terminals');
        return response.data;
    }
};
export default terminalsApi;
