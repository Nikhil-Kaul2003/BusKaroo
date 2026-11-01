import api from './client';
export const authApi = {
    login: async (data) => {
        const response = await api.post('/auth/login', data);
        return response.data;
    },
    register: async (data) => {
        const response = await api.post('/auth/register', data);
        return response.data;
    },
    registerAdmin: async (data) => {
        const response = await api.post('/auth/register-admin', data);
        return response.data;
    },
};
export default authApi;
