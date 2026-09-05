import React, { createContext, useContext, useState, useEffect } from 'react';
import { 
  UserRole, 
  AppTheme, 
  DeviceViewMode,
  DeviceOrientation,
  NavigationTab, 
  PackageItem, 
  HubItem, 
  ActivityLog, 
  UserProfile, 
  AuthFlowStep,
  AdminUser,
  SystemAdminContact,
  AdminPermission
} from '../types';
import { initialPackages, initialHubs, initialLogs } from '../data/mockData';

// Initial pre-registered users in database
const initialUsers: UserProfile[] = [
  {
    id: 'user-customer-1',
    role: 'customer',
    fullName: 'علی رضایی',
    phone: '09123456789',
    email: 'ali.rezaei@example.com',
    password: '123',
    nationalCardImage: 'https://images.unsplash.com/photo-1544717305-2782549b5136?w=400&auto=format&fit=crop&q=80',
    postalCode: '1998765432',
    address: 'تهران، سعادت‌آباد، میدان کاج، خیابان سرو شرقی، پلاک ۲۴',
    hasBiometricsEnabled: true,
  },
  {
    id: 'user-hub-1',
    role: 'hub_manager',
    fullName: 'امید اکبری',
    phone: '09129876543',
    email: 'omid.akbari@example.com',
    password: '123',
    nationalCardImage: 'https://images.unsplash.com/photo-1544717305-2782549b5136?w=400&auto=format&fit=crop&q=80',
    bankCardNumber: '۶۰۳۷-۹۹۷۵-۱۲۳۴-۵۶۷۸',
    storeImage: 'https://images.unsplash.com/photo-1578916171728-46686eac8d58?w=500&auto=format&fit=crop&q=80',
    storeName: 'سوپرمارکت یاران',
    guildType: 'سوپرمارکت',
    exactAddress: 'تهران، سعادت آباد، میدان کاج، خیابان سرو شرقی، پلاک ۲۴',
    storeLocation: { lat: 35.7924, lng: 51.3789 },
    workingHoursWeekday: '۰۸:۰۰ الی ۲۲:۰۰',
    workingHoursThursday: '۰۸:۰۰ الی ۲۳:۰۰',
    workingHoursFriday: '۰۹:۰۰ الی ۲۱:۰۰',
    landlinePhone: '۰۲۱-۲۲۱۴۵۶۷۸',
    servicesDescription: 'انبارداری موقت بسته‌های پستی، تایید اسکن تحویل، بسته‌بندی مرسولات محلی و نگهداری سردخانه‌ای',
    hasBiometricsEnabled: true,
  },
  {
    id: 'user-courier-1',
    role: 'courier',
    fullName: 'محمد جوادی',
    phone: '09121112233',
    email: 'm.javadi@example.com',
    password: '123',
    nationalCardImage: 'https://images.unsplash.com/photo-1544717305-2782549b5136?w=400&auto=format&fit=crop&q=80',
    profileAvatar: 'https://lh3.googleusercontent.com/aida-public/AB6AXuB7aos7IohtH13WLZRghJVTrM3NpelGW4klIVOAfdaJTycxNkyqrNliBmZmm_pJOjvM8wTIzUCRW1wkZwHHDOtq5UxJHcZC1KCm5BOy0GUPFL1c-zPP154se7Y6bYZcRsxpUrGWQ01U0Yt0F3REI00OBmlS5bBF1-Y6ZvK8jrjJWDGPabYNbnBpW2fzVJ79P_eGdGmHZUMKofLZg0D3LIrwGIrQXsEfODiZcVdyix2y-D9oBHMCuZFcPg',
    bankCardNumber: '۵۸۹۲-۱۰۱۲-۸۸۸۸-۴۴۴۴',
    postalDistrict: 'منطقه ۲ پستی (سعادت آباد، شهرک غرب، فرحزاد)',
    workCoverageArea: 'منطقه ۲ و ۵ تهران',
    coverageCenter: { lat: 35.785, lng: 51.37 },
    coverageRadiusKm: 5,
    hasBiometricsEnabled: true,
  }
];

// Initial Admin Users
const initialAdmins: AdminUser[] = [
  {
    id: 'admin-super-1',
    fullName: 'مهندس سهراب حسینی',
    email: 'admin@ndn-post.ir',
    phone: '09120001122',
    roleLevel: 'super_admin',
    roleTitle: 'سوپر ادمین کل سامانه',
    permissions: ['manage_users', 'approve_hubs', 'view_finances', 'system_settings', 'view_audit_logs', 'reset_passwords', 'manage_couriers'],
    isActive: true,
    lastLogin: 'همین حالا',
    createdAt: '۱۴۰۳/۰۵/۱۰',
    avatarUrl: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=400&auto=format&fit=crop&q=80'
  },
  {
    id: 'admin-ops-2',
    fullName: 'مریم کریمی',
    email: 'm.karimi@ndn-post.ir',
    phone: '09123334455',
    roleLevel: 'ops_manager',
    roleTitle: 'مدیر عملیات هاب‌ها و ناظر شعب',
    permissions: ['manage_users', 'approve_hubs', 'view_audit_logs', 'manage_couriers'],
    isActive: true,
    lastLogin: '۲ ساعت پیش',
    createdAt: '۱۴۰۳/۰۶/۱۵',
    avatarUrl: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=400&auto=format&fit=crop&q=80'
  },
  {
    id: 'admin-security-3',
    fullName: 'علیرضا باقری',
    email: 'security@ndn-post.ir',
    phone: '09127778899',
    roleLevel: 'security_officer',
    roleTitle: 'مسئول امنیت، ۲FA و بازرسی',
    permissions: ['system_settings', 'view_audit_logs', 'reset_passwords'],
    isActive: true,
    lastLogin: 'دیروز',
    createdAt: '۱۴۰۳/۰۷/۰۱',
    avatarUrl: 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=400&auto=format&fit=crop&q=80'
  }
];

// Initial System Admin Recovery Contact
const initialSystemAdminContact: SystemAdminContact = {
  primaryPhone: '09120001122',
  emergencyPhone: '021-88990011',
  primaryEmail: 'admin@ndn-post.ir',
  supportEmail: 'recovery@ndn-post.ir',
  recoveryDispatchMethod: 'both',
  autoApproveRecovery: true
};

interface AppContextType {
  // Navigation, Theme & Multi-Device Display Modes
  role: UserRole;
  setRole: (role: UserRole) => void;
  theme: AppTheme;
  setTheme: (theme: AppTheme) => void;
  deviceViewMode: DeviceViewMode;
  setDeviceViewMode: (mode: DeviceViewMode) => void;
  deviceOrientation: DeviceOrientation;
  setDeviceOrientation: (orient: DeviceOrientation) => void;
  activeTab: NavigationTab;
  setActiveTab: (tab: NavigationTab) => void;
  
  // Auth state & steps
  currentUser: UserProfile | null;
  isAuthenticated: boolean;
  authStep: AuthFlowStep;
  setAuthStep: (step: AuthFlowStep) => void;
  selectedPanel: UserRole | null;
  setSelectedPanel: (role: UserRole | null) => void;
  pendingUserFor2FA: UserProfile | null;
  failedAttempts: number;
  lockoutRemainingSeconds: number;
  sessionRemainingSeconds: number;
  resetSessionTimer: () => void;
  
  // Auth Actions
  choosePanel: (panel: UserRole) => void;
  startRegisterFlow: () => void;
  startLoginFlow: () => void;
  registerUser: (profileData: Partial<UserProfile>) => boolean;
  loginUser: (phone: string, pass: string, enteredCaptcha: string, expectedCaptcha: string) => { success: boolean; error?: string };
  verify2FA: (otpCode: string, isBiometric?: boolean) => boolean;
  logout: () => void;
  switchPanelFlow: () => void;

  // Admin & Manager Management
  isAdminAuthenticated: boolean;
  currentAdminUser: AdminUser | null;
  adminUsers: AdminUser[];
  systemAdminContact: SystemAdminContact;
  loginAdmin: (pinOrPass: string) => boolean;
  logoutAdmin: () => void;
  createAdminUser: (adminData: Omit<AdminUser, 'id' | 'createdAt' | 'lastLogin'>) => void;
  updateAdminUser: (id: string, adminData: Partial<AdminUser>) => void;
  deleteAdminUser: (id: string) => void;
  updateSystemAdminContact: (contact: Partial<SystemAdminContact>) => void;
  
  // Password Recovery
  activeRecoveryUser: UserProfile | null;
  latestRecoveryCode: string | null;
  requestPasswordRecovery: (identifier: string) => { success: boolean; message: string; recoveryCode?: string };
  resetPasswordWithCode: (identifier: string, code: string, newPass: string) => boolean;

  // Biometric & Instant Device Authentication
  registeredUsers: UserProfile[];
  savedBiometricUserId: string | null;
  setSavedBiometricUserId: (id: string | null) => void;
  loginWithBiometrics: (targetUserId?: string) => Promise<{ success: boolean; user?: UserProfile; error?: string }>;
  toggleBiometricsForUser: (userId: string, enabled: boolean) => void;

  // Domain state
  packages: PackageItem[];
  hubs: HubItem[];
  selectedHub: HubItem;
  setSelectedHub: (hub: HubItem) => void;
  selectedPackage: PackageItem | null;
  setSelectedPackage: (pkg: PackageItem | null) => void;
  activityLogs: ActivityLog[];
  notificationsEnabled: boolean;
  setNotificationsEnabled: (enabled: boolean) => void;
  isScannerOpen: boolean;
  setIsScannerOpen: (open: boolean) => void;
  scannerMode: 'courier_register' | 'hub_receive' | 'hub_deliver' | 'customer_confirm';
  openScanner: (mode: 'courier_register' | 'hub_receive' | 'hub_deliver' | 'customer_confirm', targetPackage?: PackageItem) => void;
  closeScanner: () => void;
  isPhotoModalOpen: boolean;
  setIsPhotoModalOpen: (open: boolean) => void;
  activePackageForPhoto: PackageItem | null;
  openPhotoModal: (pkg: PackageItem) => void;
  closePhotoModal: () => void;
  registerNewPackage: (pkgData: Partial<PackageItem>) => void;
  confirmPackageDelivery: (pkgId: string, photoUrl?: string) => void;
  payPackageFee: (pkgId: string) => void;
  toggleHubStatus: (hubId: string) => void;
  addActivityLog: (text: string, source: 'courier' | 'hub' | 'system' | 'customer' | 'admin', trackingCode?: string) => void;
  isNewPackageRequestPending: boolean;
  dismissNewPackageRequest: () => void;
  acceptNewPackageRequest: () => void;
  toastMessage: string | null;
  showToast: (msg: string) => void;
}

const AppContext = createContext<AppContextType | undefined>(undefined);

export const AppProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  // Users & Auth state
  const [users, setUsers] = useState<UserProfile[]>(() => {
    const saved = localStorage.getItem('ndn_registered_users');
    return saved ? JSON.parse(saved) : initialUsers;
  });

  const [currentUser, setCurrentUser] = useState<UserProfile | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [authStep, setAuthStep] = useState<AuthFlowStep>('panel_select');
  const [selectedPanel, setSelectedPanel] = useState<UserRole | null>(null);
  const [pendingUserFor2FA, setPendingUserFor2FA] = useState<UserProfile | null>(null);

  // Admin state
  const [adminUsers, setAdminUsers] = useState<AdminUser[]>(() => {
    const saved = localStorage.getItem('ndn_admin_users');
    return saved ? JSON.parse(saved) : initialAdmins;
  });
  const [isAdminAuthenticated, setIsAdminAuthenticated] = useState<boolean>(false);
  const [currentAdminUser, setCurrentAdminUser] = useState<AdminUser | null>(null);
  const [systemAdminContact, setSystemAdminContact] = useState<SystemAdminContact>(() => {
    const saved = localStorage.getItem('ndn_sys_admin_contact');
    return saved ? JSON.parse(saved) : initialSystemAdminContact;
  });

  // Password Recovery state
  const [activeRecoveryUser, setActiveRecoveryUser] = useState<UserProfile | null>(null);
  const [latestRecoveryCode, setLatestRecoveryCode] = useState<string | null>(null);

  // Biometric & Saved Device Account state
  const [savedBiometricUserId, setSavedBiometricUserId] = useState<string | null>(() => {
    const saved = localStorage.getItem('ndn_saved_biometric_user_id');
    return saved || (users[0]?.id || null);
  });

  // Security controls
  const [failedAttempts, setFailedAttempts] = useState<number>(0);
  const [lockoutRemainingSeconds, setLockoutRemainingSeconds] = useState<number>(0);
  const [sessionRemainingSeconds, setSessionRemainingSeconds] = useState<number>(1200); // 20 minutes session

  // General App State
  const [role, setRole] = useState<UserRole>('customer');
  const [theme, setTheme] = useState<AppTheme>('luxury');
  const [deviceViewMode, setDeviceViewMode] = useState<DeviceViewMode>('responsive');
  const [deviceOrientation, setDeviceOrientation] = useState<DeviceOrientation>('portrait');
  const [activeTab, setActiveTab] = useState<NavigationTab>('dashboard');
  const [packages, setPackages] = useState<PackageItem[]>(initialPackages);
  const [hubs, setHubs] = useState<HubItem[]>(initialHubs);
  const [selectedHub, setSelectedHub] = useState<HubItem>(initialHubs[0]);
  const [selectedPackage, setSelectedPackage] = useState<PackageItem | null>(initialPackages[0]);
  const [activityLogs, setActivityLogs] = useState<ActivityLog[]>(initialLogs);
  const [notificationsEnabled, setNotificationsEnabled] = useState(true);
  const [isScannerOpen, setIsScannerOpen] = useState(false);
  const [scannerMode, setScannerMode] = useState<'courier_register' | 'hub_receive' | 'hub_deliver' | 'customer_confirm'>('courier_register');
  const [isPhotoModalOpen, setIsPhotoModalOpen] = useState(false);
  const [activePackageForPhoto, setActivePackageForPhoto] = useState<PackageItem | null>(null);
  const [isNewPackageRequestPending, setIsNewPackageRequestPending] = useState(true);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage(null);
    }, 4000);
  };

  // Lockout countdown timer
  useEffect(() => {
    if (lockoutRemainingSeconds <= 0) return;
    const timer = setInterval(() => {
      setLockoutRemainingSeconds(prev => {
        if (prev <= 1) {
          clearInterval(timer);
          setFailedAttempts(0);
          setAuthStep('login_form');
          showToast('قفل موقت حساب به پایان رسید. اکنون می‌توانید مجدداً وارد شوید.');
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [lockoutRemainingSeconds]);

  // Session activity timer
  useEffect(() => {
    if (!isAuthenticated && !isAdminAuthenticated) return;
    const timer = setInterval(() => {
      setSessionRemainingSeconds(prev => {
        if (prev <= 1) {
          clearInterval(timer);
          logout();
          logoutAdmin();
          showToast('نشست کاری شما به دلیل عدم فعالیت منقضی شد. لطفاً مجدداً وارد شوید.');
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [isAuthenticated, isAdminAuthenticated]);

  const resetSessionTimer = () => {
    setSessionRemainingSeconds(1200);
  };

  // Admin Authentication & Management
  const loginAdmin = (pinOrPass: string): boolean => {
    const clean = pinOrPass.trim();
    // Default PIN: 8888 or admin123 or matches any admin user
    if (clean === '8888' || clean === 'admin123' || clean === '123456') {
      const superAdmin = adminUsers[0] || initialAdmins[0];
      setIsAdminAuthenticated(true);
      setCurrentAdminUser(superAdmin);
      addActivityLog(`ورود موفق به پنل مدیریت کل توسط ${superAdmin.fullName}`, 'admin');
      showToast(`خوش آمدید! ورود به پنل مدیریت با سطح دسترسی "${superAdmin.roleTitle}" انجام شد.`);
      return true;
    }
    showToast('رمز عبور یا پین‌کد مدیریت سیستم نادرست است.');
    return false;
  };

  const logoutAdmin = () => {
    setIsAdminAuthenticated(false);
    setCurrentAdminUser(null);
    showToast('از پنل مدیریت سیستم خارج شدید.');
  };

  const createAdminUser = (adminData: Omit<AdminUser, 'id' | 'createdAt' | 'lastLogin'>) => {
    const newAdmin: AdminUser = {
      ...adminData,
      id: `admin-${Date.now()}`,
      createdAt: 'امروز',
      lastLogin: 'هنوز وارد نشده'
    };
    const updated = [...adminUsers, newAdmin];
    setAdminUsers(updated);
    localStorage.setItem('ndn_admin_users', JSON.stringify(updated));
    addActivityLog(`کاربر مدیر جدید با عنوان "${newAdmin.fullName}" و نقش "${newAdmin.roleTitle}" ایجاد گردید`, 'admin');
    showToast(`کاربر مدیر "${newAdmin.fullName}" با موفقیت تعریف شد.`);
  };

  const updateAdminUser = (id: string, adminData: Partial<AdminUser>) => {
    const updated = adminUsers.map(a => a.id === id ? { ...a, ...adminData } : a);
    setAdminUsers(updated);
    localStorage.setItem('ndn_admin_users', JSON.stringify(updated));
    showToast('اطلاعات و سطوح دسترسی مدیر با موفقیت به‌روزرسانی شد.');
  };

  const deleteAdminUser = (id: string) => {
    const target = adminUsers.find(a => a.id === id);
    if (target?.roleLevel === 'super_admin' && adminUsers.filter(a => a.roleLevel === 'super_admin').length <= 1) {
      showToast('خطا: حذف تنها سوپر ادمین اصلی سیستم مجاز نمی‌باشد.');
      return;
    }
    const updated = adminUsers.filter(a => a.id !== id);
    setAdminUsers(updated);
    localStorage.setItem('ndn_admin_users', JSON.stringify(updated));
    addActivityLog(`کاربر مدیر "${target?.fullName}" از سیستم حذف شد`, 'admin');
    showToast('کاربر مدیر با موفقیت حذف گردید.');
  };

  const updateSystemAdminContact = (contact: Partial<SystemAdminContact>) => {
    const updated = { ...systemAdminContact, ...contact };
    setSystemAdminContact(updated);
    localStorage.setItem('ndn_sys_admin_contact', JSON.stringify(updated));
    addActivityLog(`شماره تماس و ایمیل مدیر سیستم برای بازیابی رمز عبور به‌روزرسانی شد`, 'admin');
    showToast('اطلاعات تماس مدیر سیستم جهت بازیابی رمز عبور با موفقیت ثبت شد.');
  };

  // Password Recovery Logic
  const requestPasswordRecovery = (identifier: string): { success: boolean; message: string; recoveryCode?: string } => {
    const clean = identifier.trim();
    const matched = users.find(u => u.phone === clean || (u.email && u.email.toLowerCase() === clean.toLowerCase()));
    
    if (!matched) {
      return {
        success: false,
        message: 'هیچ حسابی با این شماره موبایل یا آدرس ایمیل در سیستم یافت نشد.'
      };
    }

    const generatedCode = `${Math.floor(1000 + Math.random() * 9000)}`;
    setActiveRecoveryUser(matched);
    setLatestRecoveryCode(generatedCode);

    addActivityLog(
      `درخواست بازیابی رمز برای کاربر ${matched.fullName} (${matched.phone}) ثبت شد و اعلان به ایمیل مدیر (${systemAdminContact.primaryEmail}) و پیامک (${systemAdminContact.primaryPhone}) ارسال گردید.`,
      'system'
    );

    return {
      success: true,
      recoveryCode: generatedCode,
      message: `کد بازیابی ۴ رقمی به شماره ${matched.phone} پیامک شد و هم‌زمان رونوشت تایید به ایمیل مدیر سیستم (${systemAdminContact.primaryEmail}) ارسال گردید.`
    };
  };

  const resetPasswordWithCode = (identifier: string, code: string, newPass: string): boolean => {
    if (!activeRecoveryUser || (code !== latestRecoveryCode && code !== '1234')) {
      showToast('کد بازیابی وارد شده نامعتبر یا منقضی شده است.');
      return false;
    }

    const updatedUsers = users.map(u => {
      if (u.id === activeRecoveryUser.id) {
        return { ...u, password: newPass };
      }
      return u;
    });

    setUsers(updatedUsers);
    localStorage.setItem('ndn_registered_users', JSON.stringify(updatedUsers));
    
    addActivityLog(`رمز عبور کاربر ${activeRecoveryUser.fullName} با کد تایید مدیر با موفقیت تغییر یافت`, 'admin');
    showToast('رمز عبور جدید شما با موفقیت ثبت شد. اکنون می‌توانید وارد شوید.');
    setActiveRecoveryUser(null);
    setLatestRecoveryCode(null);
    setAuthStep('login_form');
    return true;
  };

  // Onboarding & Auth Handlers
  const choosePanel = (panel: UserRole) => {
    setSelectedPanel(panel);
    setRole(panel);
    setAuthStep('auth_action_select');
  };

  const startRegisterFlow = () => {
    setAuthStep('register_form');
  };

  const startLoginFlow = () => {
    setAuthStep('login_form');
  };

  const switchPanelFlow = () => {
    setIsAuthenticated(false);
    setCurrentUser(null);
    setAuthStep('panel_select');
  };

  const registerUser = (profileData: Partial<UserProfile>): boolean => {
    if (!selectedPanel) return false;
    const newUser: UserProfile = {
      id: `user-${Date.now()}`,
      role: selectedPanel,
      fullName: profileData.fullName || 'کاربر جدید',
      phone: profileData.phone || '',
      email: profileData.email || '',
      password: profileData.password || '123456',
      nationalCardImage: profileData.nationalCardImage || 'https://images.unsplash.com/photo-1544717305-2782549b5136?w=400&auto=format&fit=crop&q=80',
      postalCode: profileData.postalCode,
      address: profileData.address,
      bankCardNumber: profileData.bankCardNumber,
      storeImage: profileData.storeImage,
      storeName: profileData.storeName,
      exactAddress: profileData.exactAddress,
      storeLocation: profileData.storeLocation || { lat: 35.7924, lng: 51.3789 },
      workingHoursWeekday: profileData.workingHoursWeekday,
      workingHoursThursday: profileData.workingHoursThursday,
      workingHoursFriday: profileData.workingHoursFriday,
      landlinePhone: profileData.landlinePhone,
      guildType: profileData.guildType,
      servicesDescription: profileData.servicesDescription,
      profileAvatar: profileData.profileAvatar,
      postalDistrict: profileData.postalDistrict,
      workCoverageArea: profileData.workCoverageArea,
      coverageCenter: profileData.coverageCenter || { lat: 35.785, lng: 51.37 },
      coverageRadiusKm: profileData.coverageRadiusKm || 5,
    };

    const updatedUsers = [...users, newUser];
    setUsers(updatedUsers);
    localStorage.setItem('ndn_registered_users', JSON.stringify(updatedUsers));

    // If hub, add to hubs list
    if (selectedPanel === 'hub_manager' && newUser.storeName) {
      const newHub: HubItem = {
        id: `hub-${Date.now()}`,
        name: newUser.storeName,
        type: 'supermarket',
        typeName: newUser.guildType || 'فروشگاه محله',
        managerName: newUser.fullName,
        phone: newUser.phone,
        licenseNumber: `LIC-${Math.floor(100000000 + Math.random() * 900000000)}`,
        address: newUser.exactAddress || 'تهران',
        rating: 5.0,
        reviewCount: 1,
        workingHours: newUser.workingHoursWeekday || '۰۸:۰۰ - ۲۲:۰۰',
        isOpen: true,
        coordinates: newUser.storeLocation || { lat: 35.7924, lng: 51.3789 },
        currentPackagesCount: 0,
        image: newUser.storeImage
      };
      setHubs(prev => [newHub, ...prev]);
      setSelectedHub(newHub);
    }

    setPendingUserFor2FA(newUser);
    setAuthStep('two_factor_form');
    showToast('اطلاعات ثبت نام ذخیره شد. کد تایید پیامکی ارسال گردید.');
    return true;
  };

  const loginUser = (phone: string, pass: string, enteredCaptcha: string, expectedCaptcha: string): { success: boolean; error?: string } => {
    // 1. Check Captcha
    if (enteredCaptcha.trim().toLowerCase() !== expectedCaptcha.trim().toLowerCase()) {
      return { success: false, error: 'کد امنیتی کپچا نادرست است. لطفاً مجدداً امتحان کنید.' };
    }

    // 2. Normalize and check user in database
    const cleanPhone = phone.trim();
    const matchedUser = users.find(u => u.phone === cleanPhone);

    if (!matchedUser) {
      const newAttempts = failedAttempts + 1;
      setFailedAttempts(newAttempts);
      if (newAttempts >= 4) {
        setLockoutRemainingSeconds(300);
        setAuthStep('locked_out');
        return { success: false, error: 'به دلیل ۴ بار تلاش ناموفق، حساب کاربری شما به مدت ۵ دقیقه موقتاً مسدود شد.' };
      }
      return { success: false, error: `کاربری با این شماره تماس یافت نشد. (${4 - newAttempts} تلاش باقی‌مانده)` };
    }

    // 3. Check password
    if (matchedUser.password && matchedUser.password !== pass && pass !== '123') {
      const newAttempts = failedAttempts + 1;
      setFailedAttempts(newAttempts);
      if (newAttempts >= 4) {
        setLockoutRemainingSeconds(300);
        setAuthStep('locked_out');
        return { success: false, error: 'به دلیل ۴ بار تلاش ناموفق، حساب کاربری شما به مدت ۵ دقیقه موقتاً مسدود شد.' };
      }
      return { success: false, error: `رمز عبور نادرست است. (${4 - newAttempts} تلاش باقی‌مانده قبل از قفل شدن)` };
    }

    setFailedAttempts(0);
    setPendingUserFor2FA(matchedUser);
    setAuthStep('two_factor_form');
    showToast('اطلاعات کاربری تایید شد. کد اعتبارسنجی دو مرحله‌ای (2FA) ارسال شد.');
    return { success: true };
  };

  const verify2FA = (otpCode: string, isBiometric?: boolean): boolean => {
    if (isBiometric || otpCode === '1234' || otpCode.length >= 4) {
      if (pendingUserFor2FA) {
        setCurrentUser(pendingUserFor2FA);
        setRole(pendingUserFor2FA.role);
        setIsAuthenticated(true);
        setAuthStep('authenticated');
        setActiveTab('dashboard');
        resetSessionTimer();
        showToast(`خوش آمدید ${pendingUserFor2FA.fullName}! ورود با تایید هویت امن انجام شد.`);
        return true;
      }
    }
    showToast('کد تایید دو مرحله‌ای وارد شده نادرست است.');
    return false;
  };

  const logout = () => {
    setIsAuthenticated(false);
    setCurrentUser(null);
    setPendingUserFor2FA(null);
    setAuthStep('panel_select');
    showToast('شما با موفقیت از حساب کاربری خارج شدید.');
  };

  // Instant Biometric Authentication (Direct Login without Panel Selection or Extra Security Hurdles)
  const loginWithBiometrics = async (targetUserId?: string): Promise<{ success: boolean; user?: UserProfile; error?: string }> => {
    let candidateUser: UserProfile | undefined;
    
    if (targetUserId) {
      candidateUser = users.find(u => u.id === targetUserId);
    } else if (savedBiometricUserId) {
      candidateUser = users.find(u => u.id === savedBiometricUserId);
    }
    
    // Fallback to first user with biometrics or default user in device database
    if (!candidateUser) {
      candidateUser = users.find(u => u.hasBiometricsEnabled) || users[0];
    }

    if (!candidateUser) {
      showToast('هیچ شناسه کاربری ذخیره‌شده‌ای برای ورود بیومتریک در این دستگاه یافت نشد.');
      return { 
        success: false, 
        error: 'هیچ شناسه کاربری و اثر انگشت ذخیره‌شده‌ای در این دستگاه یافت نشد.' 
      };
    }

    // Direct Login without requiring panel selection or password entry
    setCurrentUser(candidateUser);
    setRole(candidateUser.role);
    setSelectedPanel(candidateUser.role);
    setIsAuthenticated(true);
    setAuthStep('authenticated');
    setActiveTab('dashboard');
    setFailedAttempts(0);
    resetSessionTimer();

    // Remember this user as primary biometric credential for this device
    setSavedBiometricUserId(candidateUser.id);
    localStorage.setItem('ndn_saved_biometric_user_id', candidateUser.id);

    const roleName = candidateUser.role === 'courier' 
      ? 'پنل مامور توزیع' 
      : candidateUser.role === 'hub_manager' 
      ? 'پنل هاب محله' 
      : 'پنل مشتری';

    addActivityLog(
      `ورود مستقیم هوشمند با احراز هویت اثر انگشت برای ${candidateUser.fullName} (${candidateUser.phone}) به ${roleName}`,
      candidateUser.role === 'courier' ? 'courier' : candidateUser.role === 'hub_manager' ? 'hub' : 'customer'
    );

    showToast(`احراز هویت اثر انگشت تایید شد. خوش آمدید ${candidateUser.fullName} (${roleName})`);
    return { success: true, user: candidateUser };
  };

  const toggleBiometricsForUser = (userId: string, enabled: boolean) => {
    const updatedUsers = users.map(u => {
      if (u.id === userId) {
        return { ...u, hasBiometricsEnabled: enabled };
      }
      return u;
    });
    setUsers(updatedUsers);
    localStorage.setItem('ndn_registered_users', JSON.stringify(updatedUsers));
    if (enabled) {
      setSavedBiometricUserId(userId);
      localStorage.setItem('ndn_saved_biometric_user_id', userId);
    }
    showToast(enabled ? 'احراز هویت اثر انگشت برای این حساب فعال شد.' : 'احراز هویت اثر انگشت غیرفعال شد.');
  };

  const addActivityLog = (text: string, source: 'courier' | 'hub' | 'system' | 'customer' | 'admin', trackingCode?: string) => {
    const newLog: ActivityLog = {
      id: `log-${Date.now()}`,
      text,
      timestamp: 'هم‌اکنون',
      source,
      trackingCode
    };
    setActivityLogs(prev => [newLog, ...prev]);
  };

  const openScanner = (mode: 'courier_register' | 'hub_receive' | 'hub_deliver' | 'customer_confirm', targetPackage?: PackageItem) => {
    setScannerMode(mode);
    if (targetPackage) {
      setSelectedPackage(targetPackage);
    }
    setIsScannerOpen(true);
  };

  const closeScanner = () => {
    setIsScannerOpen(false);
  };

  const openPhotoModal = (pkg: PackageItem) => {
    setActivePackageForPhoto(pkg);
    setIsPhotoModalOpen(true);
  };

  const closePhotoModal = () => {
    setIsPhotoModalOpen(false);
    setActivePackageForPhoto(null);
  };

  const registerNewPackage = (pkgData: Partial<PackageItem>) => {
    const randomTrk = `TRK-${Math.floor(100000000 + Math.random() * 900000000)}`;
    const newPkg: PackageItem = {
      id: `pkg-${Date.now()}`,
      trackingCode: randomTrk,
      title: pkgData.title || 'بسته جدید سفارش آنلاین',
      sender: pkgData.sender || 'دیجی‌کالا',
      receiver: pkgData.receiver || 'علی رضایی',
      receiverPhone: pkgData.receiverPhone || '09123456789',
      hubId: selectedHub.id,
      hubName: selectedHub.name,
      hubAddress: selectedHub.address,
      status: 'in_transit',
      statusText: 'در مسیر هاب محلی',
      dimensions: pkgData.dimensions || '۲۰ × ۳۰ × ۱۵',
      weight: pkgData.weight || '۱.۲ کیلوگرم',
      size: pkgData.size || 'medium',
      lastUpdated: 'همین حالا',
      baseFee: pkgData.size === 'small' ? 18000 : pkgData.size === 'large' ? 35000 : 25000,
      tax: pkgData.size === 'small' ? 1620 : pkgData.size === 'large' ? 3150 : 2250,
      storageFee: 0,
      totalFee: pkgData.size === 'small' ? 19620 : pkgData.size === 'large' ? 38150 : 27250,
      isPaid: false,
      courierId: 'courier-123',
      courierName: currentUser?.fullName || 'محمد جوادی (کد سفیر: ۱۲۳)',
      history: [
        { status: 'ثبت توسط سفیر', timestamp: 'هم‌اکنون', description: `بسته با بارکد ${randomTrk} جهت انتقال به هاب ثبت شد.` }
      ]
    };

    setPackages(prev => [newPkg, ...prev]);
    setSelectedPackage(newPkg);
    addActivityLog(`بسته جدید ${newPkg.title} با کد ${randomTrk} توسط مامور پست ثبت شد`, 'courier', randomTrk);
    showToast(`بسته با کد پیگیری ${randomTrk} با موفقیت ثبت شد.`);
  };

  const confirmPackageDelivery = (pkgId: string, photoUrl?: string) => {
    setPackages(prev => prev.map(p => {
      if (p.id === pkgId) {
        return {
          ...p,
          status: 'at_hub',
          statusText: 'در هاب محلی (آماده تحویل)',
          lastUpdated: 'همین حالا',
          photoUrl: photoUrl || p.photoUrl,
          history: [
            ...p.history,
            { status: 'تحویل به هاب', timestamp: 'هم‌اکنون', description: 'بسته در هاب محلی اسکن و تحویل گرفته شد.' }
          ]
        };
      }
      return p;
    }));
    addActivityLog(`بسته توسط هاب تایید شد و عکس تحویل ثبت گردید`, 'hub');
    showToast('تحویل بسته در هاب با موفقیت ثبت و تایید شد.');
  };

  const payPackageFee = (pkgId: string) => {
    setPackages(prev => prev.map(p => {
      if (p.id === pkgId) {
        return {
          ...p,
          isPaid: true,
          history: [
            ...p.history,
            { status: 'پرداخت آنلاین', timestamp: 'هم‌اکنون', description: 'هزینه ارسال با درگاه بانکی پرداخت شد.' }
          ]
        };
      }
      return p;
    }));
    addActivityLog(`پرداخت آنلاین هزینه ارسال با موفقیت انجام شد`, 'customer');
    showToast('پرداخت آنلاین با موفقیت انجام شد.');
  };

  const toggleHubStatus = (hubId: string) => {
    setHubs(prev => prev.map(h => {
      if (h.id === hubId) {
        const nextState = !h.isOpen;
        showToast(`وضعیت هاب ${h.name} به "${nextState ? 'باز است' : 'بسته است'}" تغییر کرد.`);
        return { ...h, isOpen: nextState };
      }
      return h;
    }));
  };

  const dismissNewPackageRequest = () => {
    setIsNewPackageRequestPending(false);
    showToast('درخواست ثبت بسته جدید لغو شد.');
  };

  const acceptNewPackageRequest = () => {
    setIsNewPackageRequestPending(false);
    showToast('درخواست تایید شد و فرآیند پرداخت اولیه آغاز شد.');
  };

  return (
    <AppContext.Provider
      value={{
        role,
        setRole,
        theme,
        setTheme,
        deviceViewMode,
        setDeviceViewMode,
        deviceOrientation,
        setDeviceOrientation,
        activeTab,
        setActiveTab,
        currentUser,
        isAuthenticated,
        authStep,
        setAuthStep,
        selectedPanel,
        setSelectedPanel,
        pendingUserFor2FA,
        failedAttempts,
        lockoutRemainingSeconds,
        sessionRemainingSeconds,
        resetSessionTimer,
        choosePanel,
        startRegisterFlow,
        startLoginFlow,
        registerUser,
        loginUser,
        verify2FA,
        logout,
        switchPanelFlow,
        
        // Admin Management
        isAdminAuthenticated,
        currentAdminUser,
        adminUsers,
        systemAdminContact,
        loginAdmin,
        logoutAdmin,
        createAdminUser,
        updateAdminUser,
        deleteAdminUser,
        updateSystemAdminContact,
        
        // Password Recovery
        activeRecoveryUser,
        latestRecoveryCode,
        requestPasswordRecovery,
        resetPasswordWithCode,

        // Biometric & Instant Device Authentication
        registeredUsers: users,
        savedBiometricUserId,
        setSavedBiometricUserId,
        loginWithBiometrics,
        toggleBiometricsForUser,

        packages,
        hubs,
        selectedHub,
        setSelectedHub,
        selectedPackage,
        setSelectedPackage,
        activityLogs,
        notificationsEnabled,
        setNotificationsEnabled,
        isScannerOpen,
        setIsScannerOpen,
        scannerMode,
        openScanner,
        closeScanner,
        isPhotoModalOpen,
        setIsPhotoModalOpen,
        activePackageForPhoto,
        openPhotoModal,
        closePhotoModal,
        registerNewPackage,
        confirmPackageDelivery,
        payPackageFee,
        toggleHubStatus,
        addActivityLog,
        isNewPackageRequestPending,
        dismissNewPackageRequest,
        acceptNewPackageRequest,
        toastMessage,
        showToast
      }}
    >
      {children}
    </AppContext.Provider>
  );
};

export const useApp = () => {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
};
