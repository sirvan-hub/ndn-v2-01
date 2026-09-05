export type UserRole = 'customer' | 'courier' | 'hub_manager';

export type AppTheme = 'luxury' | 'classic'; // 'luxury' (Obsidian & Gold), 'classic' (Corporate Blue & Orange)

export type DeviceViewMode = 'responsive' | 'mobile' | 'tablet' | 'desktop';
export type DeviceOrientation = 'portrait' | 'landscape';

export type NavigationTab = 'dashboard' | 'packages' | 'scan' | 'drive' | 'settings' | 'map' | 'auth' | 'profile' | 'admin';

export type AuthFlowStep = 
  | 'panel_select' 
  | 'auth_action_select' 
  | 'register_form' 
  | 'login_form' 
  | 'two_factor_form' 
  | 'password_recovery'
  | 'locked_out';

export interface UserProfile {
  id: string;
  role: UserRole;
  fullName: string;
  phone: string;
  email?: string;
  password?: string;
  nationalCardImage?: string;
  // Customer fields
  postalCode?: string;
  address?: string;
  // Hub Manager fields
  bankCardNumber?: string;
  storeImage?: string;
  storeName?: string;
  exactAddress?: string;
  storeLocation?: { lat: number; lng: number };
  workingHoursWeekday?: string;
  workingHoursThursday?: string;
  workingHoursFriday?: string;
  landlinePhone?: string;
  guildType?: string;
  servicesDescription?: string;
  // Courier fields
  profileAvatar?: string;
  postalDistrict?: string;
  workCoverageArea?: string;
  coverageCenter?: { lat: number; lng: number };
  coverageRadiusKm?: number;
  hasBiometricsEnabled?: boolean;
}

export type AdminRoleLevel = 
  | 'super_admin' 
  | 'system_manager' 
  | 'ops_manager' 
  | 'security_officer' 
  | 'financial_auditor';

export type AdminPermission = 
  | 'manage_users' 
  | 'approve_hubs' 
  | 'view_finances' 
  | 'system_settings' 
  | 'view_audit_logs' 
  | 'reset_passwords' 
  | 'manage_couriers';

export interface AdminUser {
  id: string;
  fullName: string;
  email: string;
  phone: string;
  roleLevel: AdminRoleLevel;
  roleTitle: string;
  permissions: AdminPermission[];
  isActive: boolean;
  lastLogin: string;
  createdAt: string;
  avatarUrl?: string;
}

export interface SystemAdminContact {
  primaryPhone: string;
  emergencyPhone: string;
  primaryEmail: string;
  supportEmail: string;
  recoveryDispatchMethod: 'both' | 'sms' | 'email';
  autoApproveRecovery: boolean;
}

export type PackageStatus = 'in_transit' | 'at_hub' | 'delivered' | 'pending_confirmation';

export type PackageSize = 'small' | 'medium' | 'large';

export interface PackageItem {
  id: string;
  trackingCode: string;
  title: string;
  sender: string;
  receiver: string;
  receiverPhone?: string;
  hubId: string;
  hubName: string;
  hubAddress: string;
  status: PackageStatus;
  statusText: string;
  dimensions: string;
  weight: string;
  size: PackageSize;
  lastUpdated: string;
  baseFee: number;
  tax: number;
  storageFee: number;
  totalFee: number;
  isPaid: boolean;
  photoUrl?: string;
  courierId?: string;
  courierName?: string;
  courierAvatar?: string;
  qrCode?: string;
  deliveryDate?: string;
  history: {
    status: string;
    timestamp: string;
    description: string;
  }[];
}

export interface HubItem {
  id: string;
  name: string;
  type: 'supermarket' | 'stationery' | 'netcafe' | 'pharmacy';
  typeName: string;
  managerName: string;
  phone: string;
  licenseNumber: string;
  address: string;
  rating: number;
  reviewCount: number;
  workingHours: string;
  isOpen: boolean;
  coordinates: {
    lat: number;
    lng: number;
  };
  currentPackagesCount: number;
  image?: string;
}

export interface ActivityLog {
  id: string;
  text: string;
  timestamp: string;
  source: 'courier' | 'hub' | 'system' | 'customer' | 'admin';
  trackingCode?: string;
}

export interface DriveUser {
  displayName?: string;
  emailAddress?: string;
  photoLink?: string;
  me?: boolean;
}

export interface DriveStorageQuota {
  limit?: string;
  usage?: string;
  usageInDrive?: string;
  usageInDriveTrash?: string;
}

export interface DriveAboutInfo {
  user?: DriveUser;
  storageQuota?: DriveStorageQuota;
}

export interface DriveFileItem {
  id: string;
  name: string;
  mimeType: string;
  size?: string;
  modifiedTime?: string;
  createdTime?: string;
  webViewLink?: string;
  webContentLink?: string;
  thumbnailLink?: string;
  iconLink?: string;
  shared?: boolean;
  starred?: boolean;
  trashed?: boolean;
  parents?: string[];
  description?: string;
}

export interface DriveFolderBreadcrumb {
  id: string;
  name: string;
}

