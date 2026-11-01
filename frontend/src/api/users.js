import api from './client';
export const usersApi = {
    getCurrentUser: async () => {
        const response = await api.get('/users/me');
        return response.data;
    },
    updateUser: async (id, data) => {
        const response = await api.put(`/users/${id}`, data);
        return response.data;
    },
};
export default usersApi;
