import api from './client';
export const adminApi = {
    async getCategories() {
        return (await api.get('/bus-categories')).data;
    },
    async createCategory(payload) {
        return (await api.post('/bus-categories', payload)).data;
    },
    async updateCategory(id, payload) {
        return (await api.put(`/bus-categories/${id}`, payload)).data;
    },
    async deleteCategory(id) {
        await api.delete(`/bus-categories/${id}`);
    },
    async getBuses() {
        return (await api.get('/buses')).data;
    },
    async createBus(payload) {
        return (await api.post('/buses', payload)).data;
    },
    async updateBus(id, payload) {
        return (await api.put(`/buses/${id}`, payload)).data;
    },
    async deleteBus(id) {
        await api.delete(`/buses/${id}`);
    },
};
