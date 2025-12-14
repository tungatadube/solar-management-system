// frontend/src/services/api.ts

import axios from 'axios';
import { Job, JobImage, TravelLog, LocationTracking, StockItem, Location, User, WorkLog, Invoice } from '../types';
import keycloak from '../keycloak';

// Use relative URL so nginx can proxy to backend
const API_BASE_URL = '/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add Keycloak token to requests
api.interceptors.request.use(
  async (config) => {
    if (keycloak.token) {
      // Ensure token is fresh
      try {
        await keycloak.updateToken(30);
        config.headers.Authorization = `Bearer ${keycloak.token}`;
      } catch (error) {
        console.error('Failed to refresh token', error);
        keycloak.login();
      }
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Job APIs
export const jobApi = {
  getAll: () => api.get<Job[]>('/jobs'),
  getById: (id: number) => api.get<Job>(`/jobs/${id}`),
  getByStatus: (status: string) => api.get<Job[]>(`/jobs/status/${status}`),
  getByUser: (userId: number) => api.get<Job[]>(`/jobs/user/${userId}`),
  create: (job: Partial<Job>) => api.post<Job>('/jobs', job),
  update: (id: number, job: Partial<Job>) => api.put<Job>(`/jobs/${id}`, job),
  updateStatus: (id: number, status: string) => api.patch<Job>(`/jobs/${id}/status?status=${status}`),
  delete: (id: number) => api.delete(`/jobs/${id}`),
  
  // Job Images
  uploadImage: (jobId: number, formData: FormData) => 
    api.post<JobImage>(`/jobs/${jobId}/images`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }),
  getImages: (jobId: number) => api.get<JobImage[]>(`/jobs/${jobId}/images`),
  
  // Travel Logs
  logTravel: (jobId: number, travelLog: Partial<TravelLog>) => 
    api.post<TravelLog>(`/jobs/${jobId}/travel`, travelLog),
  getTravelLogs: (jobId: number) => api.get<TravelLog[]>(`/jobs/${jobId}/travel`),
  
  // Stock Cost
  getStockCost: (jobId: number) => api.get<number>(`/jobs/${jobId}/stock-cost`),
};

// Location Tracking APIs
export const locationTrackingApi = {
  recordLocation: (tracking: Partial<LocationTracking>) => 
    api.post<LocationTracking>('/location-tracking', tracking),
  recordLocationSimple: (userId: number, params: {
    latitude: number;
    longitude: number;
    accuracy?: number;
    altitude?: number;
    speed?: number;
    heading?: number;
    deviceId?: string;
  }) => api.post<LocationTracking>(`/location-tracking/user/${userId}`, null, { params }),
  getLatest: (userId: number) => api.get<LocationTracking>(`/location-tracking/user/${userId}/latest`),
  getHistory: (userId: number) => api.get<LocationTracking[]>(`/location-tracking/user/${userId}/history`),
  getHistoryRange: (userId: number, start: string, end: string) => 
    api.get<LocationTracking[]>(`/location-tracking/user/${userId}/history/range`, {
      params: { start, end }
    }),
  calculateDistance: (lat1: number, lon1: number, lat2: number, lon2: number) =>
    api.get<number>('/location-tracking/distance', {
      params: { lat1, lon1, lat2, lon2 }
    }),
};

// Stock APIs (you would create a StockController for these)
export const stockApi = {
  getAll: () => api.get<StockItem[]>('/stock'),
  getById: (id: number) => api.get<StockItem>(`/stock/${id}`),
  getLowStock: () => api.get<StockItem[]>('/stock/low-stock'),
  create: (stock: Partial<StockItem>) => api.post<StockItem>('/stock', stock),
  update: (id: number, stock: Partial<StockItem>) => api.put<StockItem>(`/stock/${id}`, stock),
  delete: (id: number) => api.delete(`/stock/${id}`),
};

// Location APIs
export const locationApi = {
  getAll: () => api.get<Location[]>('/locations'),
  getById: (id: number) => api.get<Location>(`/locations/${id}`),
  getByType: (type: string) => api.get<Location[]>(`/locations/type/${type}`),
  create: (location: Partial<Location>) => api.post<Location>('/locations', location),
  update: (id: number, location: Partial<Location>) => api.put<Location>(`/locations/${id}`, location),
  delete: (id: number) => api.delete(`/locations/${id}`),
};

// User APIs
export const userApi = {
  getAll: () => api.get<User[]>('/users'),
  getById: (id: number) => api.get<User>(`/users/${id}`),
  getCurrent: () => api.get<User>('/users/me'),
  create: (user: Partial<User>) => api.post<User>('/users', user),
  update: (id: number, user: Partial<User>) => api.put<User>(`/users/${id}`, user),
  delete: (id: number) => api.delete(`/users/${id}`),
};

// Work Log APIs
export const workLogApi = {
  getAll: () => api.get<WorkLog[]>('/worklogs'),
  getById: (id: number) => api.get<WorkLog>(`/worklogs/${id}`),
  getByUser: (userId: number) => api.get<WorkLog[]>(`/worklogs/user/${userId}`),
  getByJob: (jobId: number) => api.get<WorkLog[]>(`/worklogs/job/${jobId}`),
  getUninvoiced: (userId: number) => api.get<WorkLog[]>(`/worklogs/user/${userId}/uninvoiced`),
  getByDateRange: (userId: number, startDate: string, endDate: string) => 
    api.get<WorkLog[]>(`/worklogs/user/${userId}/date-range`, { params: { startDate, endDate } }),
  create: (workLog: Partial<WorkLog>) => api.post<WorkLog>('/worklogs', workLog),
  update: (id: number, workLog: Partial<WorkLog>) => api.put<WorkLog>(`/worklogs/${id}`, workLog),
  delete: (id: number) => api.delete(`/worklogs/${id}`),
};

// Invoice APIs
export const invoiceApi = {
  getAll: () => api.get<Invoice[]>('/invoices'),
  getById: (id: number) => api.get<Invoice>(`/invoices/${id}`),
  getByTechnician: (technicianId: number) => api.get<Invoice[]>(`/invoices/technician/${technicianId}`),
  generate: (technicianId: number, startDate: string, endDate: string) =>
    api.post<Invoice>('/invoices/generate', null, { params: { technicianId, startDate, endDate } }),
  generateExcel: (id: number) => api.post<string>(`/invoices/${id}/generate-excel`),
  download: (id: number) => api.get(`/invoices/${id}/download`, { responseType: 'blob' }),
  update: (id: number, invoice: Partial<Invoice>) => api.put<Invoice>(`/invoices/${id}`, invoice),
  generateWeekly: () => api.post<Invoice[]>('/invoices/generate-weekly'),
  getCurrentWeekRange: () => api.get<{ startDate: string; endDate: string }>('/invoices/current-week-range'),
};

// Parameter APIs
export const parameterApi = {
  getHourlyRate: () => api.get<number>('/parameters/hourly-rate'),
};

// Solar Optimizer APIs
export interface SolarAnalysisRequest {
  jobId?: number;
  address?: string;
  latitude: number;
  longitude: number;
  roofArea: number;
  targetCapacity: number;
  roofType: string;
}

export interface SolarAnalysis {
  id: number;
  jobId?: number;
  address?: string;
  latitude: number;
  longitude: number;
  roofArea: number;
  usableArea: number;
  roofPitch: number;
  roofOrientation: string;
  shadingFactor: number;
  optimalAzimuth: number;
  optimalTilt: number;
  numberOfPanels: number;
  systemCapacity: number;
  panelWattage: number;
  annualProduction: number;
  dailyAverage: number;
  peakSunHours: number;
  layoutRows: number;
  layoutColumns: number;
  panelSpacing: number;
  materials?: any;
  analyzedAt: string;
  createdAt: string;
  updatedAt: string;
}

export const solarOptimizerApi = {
  analyze: (request: SolarAnalysisRequest) => api.post<SolarAnalysis>('/solar-optimizer/analyze', request),
  calculate: (request: Partial<SolarAnalysisRequest>) => api.post<SolarAnalysis>('/solar-optimizer/calculate', request),
  getById: (id: number) => api.get<SolarAnalysis>(`/solar-optimizer/${id}`),
  getByJobId: (jobId: number) => api.get<SolarAnalysis>(`/solar-optimizer/job/${jobId}`),
  update: (id: number, updates: Partial<SolarAnalysis>) => api.put<SolarAnalysis>(`/solar-optimizer/${id}`, updates),
  delete: (id: number) => api.delete(`/solar-optimizer/${id}`),
};

export default api;
