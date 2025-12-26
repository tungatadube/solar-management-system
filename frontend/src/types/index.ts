// frontend/src/types/index.ts

export enum UserRole {
  ADMIN = 'ADMIN',
  MANAGER = 'MANAGER',
  TECHNICIAN = 'TECHNICIAN',
  ASSISTANT = 'ASSISTANT'
}

export enum JobStatus {
  SCHEDULED = 'SCHEDULED',
  IN_PROGRESS = 'IN_PROGRESS',
  ON_HOLD = 'ON_HOLD',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

export enum JobType {
  NEW_INSTALLATION = 'NEW_INSTALLATION',
  MAINTENANCE = 'MAINTENANCE',
  REPAIR = 'REPAIR',
  INSPECTION = 'INSPECTION',
  UPGRADE = 'UPGRADE'
}

export enum ImageType {
  BEFORE_INSTALLATION = 'BEFORE_INSTALLATION',
  DURING_INSTALLATION = 'DURING_INSTALLATION',
  AFTER_INSTALLATION = 'AFTER_INSTALLATION',
  ELECTRICAL_WORK = 'ELECTRICAL_WORK',
  MOUNTING = 'MOUNTING',
  INVERTER = 'INVERTER',
  BATTERY = 'BATTERY',
  METER = 'METER',
  DOCUMENTATION = 'DOCUMENTATION',
  OTHER = 'OTHER'
}

export enum StockCategory {
  SOLAR_PANEL = 'SOLAR_PANEL',
  INVERTER = 'INVERTER',
  BATTERY = 'BATTERY',
  MOUNTING_HARDWARE = 'MOUNTING_HARDWARE',
  ELECTRICAL_COMPONENTS = 'ELECTRICAL_COMPONENTS',
  CABLES_WIRING = 'CABLES_WIRING',
  TOOLS = 'TOOLS',
  SAFETY_EQUIPMENT = 'SAFETY_EQUIPMENT',
  CONSUMABLES = 'CONSUMABLES',
  OTHER = 'OTHER'
}

export interface User {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  abn?: string;
  role: UserRole;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Location {
  id: number;
  name: string;
  type: 'WAREHOUSE' | 'VEHICLE' | 'JOB_SITE' | 'OFFICE';
  address: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
  latitude: number;
  longitude: number;
  contactPerson?: string;
  contactPhone?: string;
  notes?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Job {
  id: number;
  jobNumber: string;
  clientName: string;
  clientPhone?: string;
  clientEmail?: string;
  location: Location;
  assignedTechnicians: User[];
  status: JobStatus;
  type: JobType;
  description?: string;
  startTime?: string;
  endTime?: string;
  estimatedCost?: number;
  actualCost?: number;
  systemSize?: number;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface JobImage {
  id: number;
  job: Job;
  imageUrl: string;
  fileName: string;
  imageType: ImageType;
  caption?: string;
  latitude?: number;
  longitude?: number;
  fileSize?: number;
  mimeType?: string;
  uploadedAt: string;
}

export interface StockItem {
  id: number;
  sku: string;
  name: string;
  description?: string;
  category: StockCategory;
  unit: string;
  unitPrice: number;
  minimumQuantity: number;
  reorderLevel: number;
  totalQuantity?: number;
  barcode?: string;
  imageUrl?: string;
  createdAt: string;
  updatedAt: string;
}

export interface TravelLog {
  id: number;
  job: Job;
  user: User;
  startLatitude: number;
  startLongitude: number;
  startAddress?: string;
  endLatitude: number;
  endLongitude: number;
  endAddress?: string;
  departureTime: string;
  arrivalTime?: string;
  distance?: number;
  duration?: number;
  fuelCost?: number;
  vehicleRegistration?: string;
  notes?: string;
  createdAt: string;
}

export interface LocationTracking {
  id: number;
  user: User;
  latitude: number;
  longitude: number;
  accuracy?: number;
  altitude?: number;
  speed?: number;
  heading?: number;
  address?: string;
  timestamp: string;
  deviceId?: string;
  createdAt: string;
}

export enum WorkType {
  BATTERY_INSTALLATION = 'BATTERY_INSTALLATION',
  INVERTER_INSTALLATION = 'INVERTER_INSTALLATION',
  CONDUIT_INSTALLATION = 'CONDUIT_INSTALLATION',
  PREWIRE = 'PREWIRE',
  PANEL_INSTALLATION = 'PANEL_INSTALLATION',
  ELECTRICAL_WORK = 'ELECTRICAL_WORK',
  WAREHOUSE_WORK = 'WAREHOUSE_WORK',
  INSPECTION = 'INSPECTION',
  MAINTENANCE = 'MAINTENANCE',
  OTHER = 'OTHER'
}

export enum InvoiceStatus {
  DRAFT = 'DRAFT',
  SENT = 'SENT',
  PAID = 'PAID',
  OVERDUE = 'OVERDUE',
  CANCELLED = 'CANCELLED'
}

export interface WorkLog {
  id: number;
  user: User;
  job: Job;
  workDate: string;
  startTime: string;
  endTime: string;
  hoursWorked: number;
  hourlyRate: number;
  totalAmount: number;
  workDescription: string;
  jobAddress: string;
  workType: WorkType;
  invoiced: boolean;
  invoice?: Invoice;
  createdAt: string;
  updatedAt: string;
}

export interface Invoice {
  id: number;
  invoiceNumber: string;
  technician: User;
  invoiceDate: string;
  periodStartDate: string;
  periodEndDate: string;
  weekNumber: number;
  billToName: string;
  billToAddress: string;
  billToPhone?: string;
  billToEmail?: string;
  technicianName: string;
  technicianABN?: string;
  technicianAddress: string;
  technicianEmail?: string;
  technicianPhone?: string;
  bsb?: string;
  accountNumber?: string;
  workLogs: WorkLog[];
  subtotal: number;
  gstRate: number;
  gstAmount: number;
  totalAmount: number;
  status: InvoiceStatus;
  paidDate?: string;
  fileUrl?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AddressResult {
  formattedAddress: string;
  streetNumber?: string;
  route?: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  lat: number;
  lng: number;
}

export interface StationaryLocation {
  lat: number;
  lng: number;
  address: string;
  dismissedAt?: number;
}

export interface LocationPoint {
  lat: number;
  lng: number;
  timestamp: number;
  accuracy: number;
}
