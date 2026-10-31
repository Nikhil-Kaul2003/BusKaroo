import api from './client';

export interface BusCategory {
  id: number;
  name: string;
  description: string | null;
}

export interface BusRecord {
  id: number;
  registrationNumber: string;
  model: string;
  manufacturer: string;
  year: number;
  totalSeats: number;
  status: 'ACTIVE' | 'MAINTENANCE' | 'RETIRED';
  categoryId: number | null;
  categoryName: string | null;
}

export interface BusPayload {
  registrationNumber: string;
  model: string;
  manufacturer: string;
  year: number;
  totalSeats: number;
  seatLayout: string;
  status: BusRecord['status'];
  categoryId: number;
}

export const adminApi = {
  async getCategories(): Promise<BusCategory[]> {
    return (await api.get<BusCategory[]>('/bus-categories')).data;
  },
  async createCategory(payload: Pick<BusCategory, 'name' | 'description'>): Promise<BusCategory> {
    return (await api.post<BusCategory>('/bus-categories', payload)).data;
  },
  async updateCategory(id: number, payload: Pick<BusCategory, 'name' | 'description'>): Promise<BusCategory> {
    return (await api.put<BusCategory>(`/bus-categories/${id}`, payload)).data;
  },
  async deleteCategory(id: number): Promise<void> {
    await api.delete(`/bus-categories/${id}`);
  },
  async getBuses(): Promise<BusRecord[]> {
    return (await api.get<BusRecord[]>('/buses')).data;
  },
  async createBus(payload: BusPayload): Promise<BusRecord> {
    return (await api.post<BusRecord>('/buses', payload)).data;
  },
  async updateBus(id: number, payload: BusPayload): Promise<BusRecord> {
    return (await api.put<BusRecord>(`/buses/${id}`, payload)).data;
  },
  async deleteBus(id: number): Promise<void> {
    await api.delete(`/buses/${id}`);
  },
};
