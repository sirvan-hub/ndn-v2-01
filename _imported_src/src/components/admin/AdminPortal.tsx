import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import { AdminUser, AdminRoleLevel, AdminPermission } from '../../types';

export const AdminPortal: React.FC<{ onClose: () => void }> = ({ onClose }) => {
  const {
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
    activityLogs,
    theme,
    showToast
  } = useApp();

  const isLuxury = theme === 'luxury';

  const [adminPin, setAdminPin] = useState('');
  const [activeAdminTab, setActiveAdminTab] = useState<'admins_list' | 'create_admin' | 'recovery_settings' | 'audit_logs'>('admins_list');
  const [editingAdmin, setEditingAdmin] = useState<AdminUser | null>(null);

  // New Admin Form State
  const [formData, setFormData] = useState<{
    fullName: string;
    email: string;
    phone: string;
    roleLevel: AdminRoleLevel;
    roleTitle: string;
    permissions: AdminPermission[];
    isActive: boolean;
  }>({
    fullName: '',
    email: '',
    phone: '',
    roleLevel: 'ops_manager',
    roleTitle: 'مدیر عملیات هاب‌ها',
    permissions: ['manage_users', 'approve_hubs', 'manage_couriers'],
    isActive: true
  });

  // System Admin Contact Form State
  const [contactData, setContactData] = useState({
    primaryPhone: systemAdminContact.primaryPhone,
    emergencyPhone: systemAdminContact.emergencyPhone,
    primaryEmail: systemAdminContact.primaryEmail,
    supportEmail: systemAdminContact.supportEmail,
    recoveryDispatchMethod: systemAdminContact.recoveryDispatchMethod,
    autoApproveRecovery: systemAdminContact.autoApproveRecovery
  });

  const availablePermissions: { id: AdminPermission; title: string; desc: string; icon: string }[] = [
    { id: 'manage_users', title: 'مدیریت کاربران و مشتریان', desc: 'مشاهده، ویرایش و مدیریت حساب‌های مشتریان', icon: 'group' },
    { id: 'approve_hubs', title: 'تایید و صدور مجوز هاب‌ها', desc: 'بررسی مدارک، پروانه کسب و فعال‌سازی فروشگاه‌های PUDO', icon: 'storefront' },
    { id: 'manage_couriers', title: 'نظارت بر سفیران توزیع', desc: 'تعیین محدوده پوشش و تایید مدارک ماموران پخش', icon: 'local_shipping' },
    { id: 'view_finances', title: 'دسترسی به تراکنش‌های مالی', desc: 'مشاهده کارمزدها، تسویه‌حساب با هاب‌ها و درآمدها', icon: 'payments' },
    { id: 'system_settings', title: 'تنظیمات کلی و امنیتی سیستم', desc: 'پیکربندی پارامترهای پروتکل، گواهی‌ها و تم سامانه', icon: 'settings_suggest' },
    { id: 'view_audit_logs', title: 'مشاهده لاگ‌های ممیزی و نظارت', desc: 'بررسی سوابق ورود، تغییر وضعیت بسته‌ها و هشدارهای امنیتی', icon: 'history_edu' },
    { id: 'reset_passwords', title: 'بازیابی و ریست رمز عبور', desc: 'دریافت درخواست‌های بازیابی و صدور مجوز تغییر رمز', icon: 'lock_reset' }
  ];

  const roleLevelOptions: { id: AdminRoleLevel; title: string; defaultPermissions: AdminPermission[] }[] = [
    { 
      id: 'super_admin', 
      title: 'سوپر ادمین کل سامانه', 
      defaultPermissions: ['manage_users', 'approve_hubs', 'manage_couriers', 'view_finances', 'system_settings', 'view_audit_logs', 'reset_passwords'] 
    },
    { 
      id: 'system_manager', 
      title: 'مدیر کل فنی و سیستم', 
      defaultPermissions: ['manage_users', 'system_settings', 'view_audit_logs', 'reset_passwords'] 
    },
    { 
      id: 'ops_manager', 
      title: 'مدیر عملیات هاب‌ها و توزیع', 
      defaultPermissions: ['manage_users', 'approve_hubs', 'manage_couriers', 'view_audit_logs'] 
    },
    { 
      id: 'security_officer', 
      title: 'مسئول امنیت، ۲FA و بازرسی', 
      defaultPermissions: ['system_settings', 'view_audit_logs', 'reset_passwords'] 
    },
    { 
      id: 'financial_auditor', 
      title: 'حسابدار و ممیز امور مالی', 
      defaultPermissions: ['view_finances', 'view_audit_logs'] 
    }
  ];

  const handleRoleLevelChange = (level: AdminRoleLevel) => {
    const selectedOpt = roleLevelOptions.find(o => o.id === level);
    setFormData(prev => ({
      ...prev,
      roleLevel: level,
      roleTitle: selectedOpt?.title || prev.roleTitle,
      permissions: selectedOpt?.defaultPermissions || prev.permissions
    }));
  };

  const togglePermission = (perm: AdminPermission) => {
    setFormData(prev => {
      const exists = prev.permissions.includes(perm);
      return {
        ...prev,
        permissions: exists 
          ? prev.permissions.filter(p => p !== perm) 
          : [...prev.permissions, perm]
      };
    });
  };

  const handleAdminLogin = (e: React.FormEvent) => {
    e.preventDefault();
    loginAdmin(adminPin);
  };

  const handleCreateAdmin = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.fullName || !formData.email || !formData.phone) {
      showToast('لطفاً تمامی فیلدهای الزامی را تکمیل نمایید.');
      return;
    }
    createAdminUser(formData);
    setFormData({
      fullName: '',
      email: '',
      phone: '',
      roleLevel: 'ops_manager',
      roleTitle: 'مدیر عملیات هاب‌ها',
      permissions: ['manage_users', 'approve_hubs', 'manage_couriers'],
      isActive: true
    });
    setActiveAdminTab('admins_list');
  };

  const handleSaveContactSettings = (e: React.FormEvent) => {
    e.preventDefault();
    updateSystemAdminContact(contactData);
  };

  // If Admin is not authenticated, show secure Admin PIN login screen
  if (!isAdminAuthenticated) {
    return (
      <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4">
        <div className={`w-full max-w-md p-6 sm:p-8 rounded-2xl border shadow-2xl space-y-6 text-right animate-in fade-in zoom-in-95 ${
          isLuxury ? 'bg-[#0e0e0e] border-[#d4af37]' : 'bg-white border-blue-600'
        }`}>
          
          <div className="flex items-center justify-between border-b pb-4 border-gray-700/40">
            <div className="flex items-center gap-2 text-amber-400">
              <span className="material-symbols-outlined text-2xl">admin_panel_settings</span>
              <h2 className="text-lg font-bold text-white">ورود اختصاصی به پنل مدیریت کل (Admin)</h2>
            </div>
            <button
              onClick={onClose}
              className="p-1.5 rounded-lg text-gray-400 hover:text-white hover:bg-white/10"
            >
              <span className="material-symbols-outlined text-lg">close</span>
            </button>
          </div>

          <div className="p-3.5 rounded-xl bg-amber-400/10 border border-amber-400/30 text-xs text-amber-300 space-y-1">
            <div className="flex items-center gap-1.5 font-bold">
              <span className="material-symbols-outlined text-sm">lock</span>
              <span>احراز هویت مدیریت سامانه NDN</span>
            </div>
            <p className="text-[11px] leading-relaxed text-gray-300">
              این بخش مخصوص سوپر ادمین، مدیران سیستم، ناظران هاب و مسئولین بازیابی رمز عبور است.
            </p>
          </div>

          <form onSubmit={handleAdminLogin} className="space-y-4">
            <div>
              <div className="flex justify-between items-center mb-1.5">
                <label className="text-xs font-semibold text-gray-300">
                  پین‌کد یا رمز عبور ادمین <span className="text-red-400">*</span>
                </label>
                <span className="text-[10px] text-gray-400 font-mono">
                  پین پیش‌فرض: <code className="text-amber-400 font-bold">8888</code>
                </span>
              </div>
              <div className="relative">
                <input
                  type="password"
                  required
                  dir="ltr"
                  value={adminPin}
                  onChange={(e) => setAdminPin(e.target.value)}
                  placeholder="پین‌کد یا رمز مدیریت را وارد کنید"
                  className={`w-full px-3.5 py-3 pl-10 rounded-xl border text-center text-sm font-mono tracking-widest outline-none ${
                    isLuxury ? 'bg-[#181818] border-amber-400/40 text-amber-300 focus:border-[#d4af37]' : 'bg-gray-50 border-blue-400 text-blue-900 focus:border-blue-600'
                  }`}
                />
                <span className="material-symbols-outlined absolute left-3 top-3 text-gray-400 text-base">
                  key
                </span>
              </div>
            </div>

            <div className="flex gap-2">
              <button
                type="submit"
                className={`flex-1 py-3 px-4 rounded-xl font-bold text-xs shadow-lg transition-all active:scale-95 flex items-center justify-center gap-2 ${
                  isLuxury ? 'bg-[#d4af37] text-black hover:bg-[#e9c349]' : 'bg-[#1e3a8a] text-white hover:bg-[#00236f]'
                }`}
              >
                <span>ورود به پنل مدیریت</span>
                <span className="material-symbols-outlined text-base">login</span>
              </button>
              
              <button
                type="button"
                onClick={() => { setAdminPin('8888'); }}
                className="px-3 py-3 rounded-xl border border-white/10 bg-white/5 hover:bg-white/10 text-xs text-gray-300"
                title="درج پین سریع"
              >
                پین تستی
              </button>
            </div>
          </form>

        </div>
      </div>
    );
  }

  // Admin Authenticated View: Full Portal Modal
  return (
    <div className="fixed inset-0 z-50 bg-black/85 backdrop-blur-md flex items-center justify-center p-2 sm:p-4 overflow-y-auto">
      <div className={`w-full max-w-5xl my-auto rounded-2xl border shadow-2xl flex flex-col max-h-[92vh] overflow-hidden text-right animate-in fade-in zoom-in-95 ${
        isLuxury ? 'bg-[#0c0c0c] border-[#d4af37]/60 text-gray-100' : 'bg-white border-blue-500 text-gray-900'
      }`}>
        
        {/* Top Portal Header */}
        <div className={`p-4 sm:p-5 border-b flex flex-wrap items-center justify-between gap-3 ${
          isLuxury ? 'bg-[#141414] border-white/10' : 'bg-blue-50 border-blue-200'
        }`}>
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-amber-400/20 border border-amber-400/40 flex items-center justify-center text-amber-400">
              <span className="material-symbols-outlined text-2xl">admin_panel_settings</span>
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base sm:text-lg font-bold text-white">
                  پنل مدیریت ارشد و تعریف سطوح دسترسی (NDN Admin)
                </h2>
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                  احراز هویت شده
                </span>
              </div>
              <span className="text-xs text-gray-400">
                مدیر فعال: <strong className="text-amber-400">{currentAdminUser?.fullName}</strong> ({currentAdminUser?.roleTitle})
              </span>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={logoutAdmin}
              className="px-3 py-1.5 rounded-lg border border-red-500/30 bg-red-500/10 hover:bg-red-500/20 text-red-400 text-xs flex items-center gap-1.5 transition-colors"
            >
              <span className="material-symbols-outlined text-sm">logout</span>
              <span>خروج از پنل ادمین</span>
            </button>
            <button
              onClick={onClose}
              className="p-1.5 rounded-lg text-gray-400 hover:text-white hover:bg-white/10 transition-colors"
            >
              <span className="material-symbols-outlined text-xl">close</span>
            </button>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className={`flex border-b px-4 gap-2 overflow-x-auto ${
          isLuxury ? 'bg-[#111111] border-white/10' : 'bg-gray-100 border-gray-200'
        }`}>
          {[
            { id: 'admins_list', label: 'مدیران و سطوح دسترسی', icon: 'badge', count: adminUsers.length },
            { id: 'create_admin', label: 'تعریف ادمین / مدیر جدید', icon: 'person_add' },
            { id: 'recovery_settings', label: 'تماس مدیر و بازیابی رمز عبور', icon: 'support_agent' },
            { id: 'audit_logs', label: 'لاگ‌های ممیزی و امنیت', icon: 'history', count: activityLogs.length }
          ].map((tab) => {
            const isActive = activeAdminTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveAdminTab(tab.id as any)}
                className={`py-3 px-4 text-xs font-bold flex items-center gap-2 border-b-2 whitespace-nowrap transition-colors ${
                  isActive
                    ? (isLuxury ? 'border-[#d4af37] text-[#f2ca50] bg-white/5' : 'border-blue-600 text-blue-800 bg-white')
                    : 'border-transparent text-gray-400 hover:text-white'
                }`}
              >
                <span className="material-symbols-outlined text-base">{tab.icon}</span>
                <span>{tab.label}</span>
                {tab.count !== undefined && (
                  <span className={`px-1.5 py-0.2 rounded-full text-[10px] ${
                    isActive ? 'bg-amber-400 text-black font-bold' : 'bg-white/10 text-gray-300'
                  }`}>
                    {tab.count}
                  </span>
                )}
              </button>
            );
          })}
        </div>

        {/* Body Content Area */}
        <div className="p-4 sm:p-6 overflow-y-auto flex-1 space-y-6">
          
          {/* TAB 1: ADMINS LIST */}
          {activeAdminTab === 'admins_list' && (
            <div className="space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <h3 className="text-sm font-bold text-white flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-amber-500">manage_accounts</span>
                    <span>لیست کاربران ادمین و سطوح دسترسی فعال در سیستم</span>
                  </h3>
                  <p className="text-xs text-gray-400 mt-0.5">
                    مدیران بر اساس ماتریس دسترسی (RBAC) فقط به بخش‌های مجاز دسترسی خواهند داشت.
                  </p>
                </div>
                <button
                  onClick={() => setActiveAdminTab('create_admin')}
                  className={`px-3 py-2 rounded-xl text-xs font-bold flex items-center gap-1.5 shadow transition-all ${
                    isLuxury ? 'bg-[#d4af37] text-black hover:bg-[#e9c349]' : 'bg-[#1e3a8a] text-white'
                  }`}
                >
                  <span className="material-symbols-outlined text-sm">add</span>
                  <span>افزودن مدیر جدید</span>
                </button>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {adminUsers.map((admin) => (
                  <div
                    key={admin.id}
                    className={`p-4 rounded-2xl border flex flex-col justify-between gap-4 transition-all ${
                      isLuxury ? 'bg-[#141414] border-white/10 hover:border-amber-400/40' : 'bg-white border-gray-200'
                    }`}
                  >
                    <div>
                      {/* Header info */}
                      <div className="flex items-start justify-between">
                        <div className="flex items-center gap-2.5">
                          <div className="w-11 h-11 rounded-xl overflow-hidden bg-amber-400/20 flex items-center justify-center border border-amber-400/30">
                            {admin.avatarUrl ? (
                              <img src={admin.avatarUrl} alt={admin.fullName} className="w-full h-full object-cover" />
                            ) : (
                              <span className="material-symbols-outlined text-2xl text-amber-400">person</span>
                            )}
                          </div>
                          <div>
                            <h4 className="text-xs font-bold text-white">{admin.fullName}</h4>
                            <span className="text-[10px] text-amber-400 block font-semibold">{admin.roleTitle}</span>
                          </div>
                        </div>

                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                          admin.isActive 
                            ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' 
                            : 'bg-red-500/20 text-red-400 border border-red-500/30'
                        }`}>
                          {admin.isActive ? 'فعال' : 'غیرفعال'}
                        </span>
                      </div>

                      {/* Contact Info */}
                      <div className="mt-3 pt-3 border-t border-white/5 space-y-1.5 text-[11px] text-gray-300">
                        <div className="flex items-center justify-between">
                          <span className="text-gray-500">تلفن:</span>
                          <span className="font-mono text-white">{admin.phone}</span>
                        </div>
                        <div className="flex items-center justify-between">
                          <span className="text-gray-500">ایمیل:</span>
                          <span className="font-mono text-gray-300">{admin.email}</span>
                        </div>
                        <div className="flex items-center justify-between">
                          <span className="text-gray-500">آخرین فعالیت:</span>
                          <span className="text-[10px] text-gray-400">{admin.lastLogin}</span>
                        </div>
                      </div>

                      {/* Permissions Badges */}
                      <div className="mt-3">
                        <span className="text-[10px] font-bold text-gray-400 block mb-1.5">مجوزهای فعال ({admin.permissions.length}):</span>
                        <div className="flex flex-wrap gap-1">
                          {admin.permissions.map((p) => {
                            const pObj = availablePermissions.find(ap => ap.id === p);
                            return (
                              <span
                                key={p}
                                className="px-2 py-0.5 rounded-md bg-white/5 border border-white/10 text-[9px] text-gray-300 flex items-center gap-1"
                              >
                                <span className="material-symbols-outlined text-[10px] text-amber-400">
                                  {pObj?.icon || 'check'}
                                </span>
                                <span>{pObj?.title.split(' ')[0] || p}</span>
                              </span>
                            );
                          })}
                        </div>
                      </div>
                    </div>

                    {/* Actions */}
                    <div className="pt-3 border-t border-white/5 flex items-center justify-between text-xs">
                      <button
                        onClick={() => {
                          updateAdminUser(admin.id, { isActive: !admin.isActive });
                        }}
                        className={`text-[11px] font-bold hover:underline ${
                          admin.isActive ? 'text-amber-400' : 'text-emerald-400'
                        }`}
                      >
                        {admin.isActive ? 'غیرفعال‌سازی' : 'فعال‌سازی حساب'}
                      </button>

                      {admin.roleLevel !== 'super_admin' && (
                        <button
                          onClick={() => deleteAdminUser(admin.id)}
                          className="text-[11px] text-red-400 hover:text-red-300 hover:underline flex items-center gap-0.5"
                        >
                          <span className="material-symbols-outlined text-xs">delete</span>
                          <span>حذف مدیر</span>
                        </button>
                      )}
                    </div>

                  </div>
                ))}
              </div>
            </div>
          )}

          {/* TAB 2: CREATE ADMIN / MANAGER */}
          {activeAdminTab === 'create_admin' && (
            <div className={`p-6 rounded-2xl border space-y-6 max-w-3xl mx-auto ${
              isLuxury ? 'bg-[#141414] border-white/10' : 'bg-white border-gray-200'
            }`}>
              <div>
                <h3 className="text-sm font-bold text-white flex items-center gap-2">
                  <span className="material-symbols-outlined text-amber-500">person_add</span>
                  <span>فرم تعریف کاربر ادمین و تعیین سطوح دسترسی (RBAC)</span>
                </h3>
                <p className="text-xs text-gray-400 mt-1">
                  مشخصات مدیر جدید را وارد کرده و مجوزهای دسترسی مجاز را تیک بزنید.
                </p>
              </div>

              <form onSubmit={handleCreateAdmin} className="space-y-5">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-gray-300 mb-1">
                      نام و نام خانوادگی مدیر <span className="text-red-400">*</span>
                    </label>
                    <input
                      type="text"
                      required
                      value={formData.fullName}
                      onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
                      placeholder="مثال: مهندس رامین نوری"
                      className={`w-full px-3.5 py-2.5 rounded-xl border text-xs outline-none ${
                        isLuxury ? 'bg-[#1e1e1e] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900'
                      }`}
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-gray-300 mb-1">
                      شماره تماس همراه <span className="text-red-400">*</span>
                    </label>
                    <input
                      type="tel"
                      required
                      dir="ltr"
                      value={formData.phone}
                      onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                      placeholder="09123456789"
                      className={`w-full px-3.5 py-2.5 rounded-xl border text-xs font-mono outline-none ${
                        isLuxury ? 'bg-[#1e1e1e] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900'
                      }`}
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-gray-300 mb-1">
                      آدرس ایمیل سازمانی مدیر <span className="text-red-400">*</span>
                    </label>
                    <input
                      type="email"
                      required
                      dir="ltr"
                      value={formData.email}
                      onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                      placeholder="admin.name@ndn-post.ir"
                      className={`w-full px-3.5 py-2.5 rounded-xl border text-xs font-mono outline-none ${
                        isLuxury ? 'bg-[#1e1e1e] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900'
                      }`}
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-gray-300 mb-1">
                      سطح نقش و سمت مدیریتی <span className="text-red-400">*</span>
                    </label>
                    <select
                      value={formData.roleLevel}
                      onChange={(e) => handleRoleLevelChange(e.target.value as AdminRoleLevel)}
                      className={`w-full px-3.5 py-2.5 rounded-xl border text-xs outline-none ${
                        isLuxury ? 'bg-[#1e1e1e] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900'
                      }`}
                    >
                      {roleLevelOptions.map((opt) => (
                        <option key={opt.id} value={opt.id}>
                          {opt.title}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                {/* Permissions Matrix */}
                <div className="pt-3 border-t border-white/10 space-y-3">
                  <div className="flex justify-between items-center">
                    <label className="text-xs font-bold text-amber-400">
                      ماتریس تفکیک سطوح دسترسی (Permissions Granular):
                    </label>
                    <span className="text-[10px] text-gray-400">
                      {formData.permissions.length} مجوز انتخاب شده
                    </span>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {availablePermissions.map((perm) => {
                      const isChecked = formData.permissions.includes(perm.id);
                      return (
                        <div
                          key={perm.id}
                          onClick={() => togglePermission(perm.id)}
                          className={`p-3 rounded-xl border cursor-pointer flex items-start gap-3 transition-all ${
                            isChecked
                              ? (isLuxury ? 'bg-amber-400/10 border-amber-400/40 text-white' : 'bg-blue-50 border-blue-400 text-blue-900')
                              : (isLuxury ? 'bg-[#1c1c1c] border-white/5 text-gray-400 hover:border-white/20' : 'bg-gray-50 border-gray-200 text-gray-500')
                          }`}
                        >
                          <div className={`w-5 h-5 rounded-md flex items-center justify-center border mt-0.5 ${
                            isChecked ? 'bg-amber-400 border-amber-400 text-black' : 'border-gray-600 bg-black/40'
                          }`}>
                            {isChecked && <span className="material-symbols-outlined text-sm font-bold">check</span>}
                          </div>
                          <div>
                            <div className="text-xs font-bold flex items-center gap-1.5">
                              <span className="material-symbols-outlined text-sm text-amber-400">{perm.icon}</span>
                              <span>{perm.title}</span>
                            </div>
                            <p className="text-[10px] opacity-75 mt-0.5 leading-relaxed">{perm.desc}</p>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>

                <div className="pt-4 flex gap-3">
                  <button
                    type="submit"
                    className={`flex-1 py-3 px-4 rounded-xl font-bold text-xs shadow-lg transition-all active:scale-95 flex items-center justify-center gap-2 ${
                      isLuxury ? 'bg-[#d4af37] text-black hover:bg-[#e9c349]' : 'bg-[#1e3a8a] text-white hover:bg-[#00236f]'
                    }`}
                  >
                    <span className="material-symbols-outlined text-base">how_to_reg</span>
                    <span>ثبت و صدور دسترسی کاربر مدیر</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setActiveAdminTab('admins_list')}
                    className="px-4 py-3 rounded-xl border border-white/10 text-xs text-gray-400 hover:text-white"
                  >
                    انصراف
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* TAB 3: SYSTEM ADMIN RECOVERY CONTACTS */}
          {activeAdminTab === 'recovery_settings' && (
            <div className={`p-6 rounded-2xl border space-y-6 max-w-3xl mx-auto ${
              isLuxury ? 'bg-[#141414] border-white/10' : 'bg-white border-gray-200'
            }`}>
              <div>
                <h3 className="text-sm font-bold text-white flex items-center gap-2">
                  <span className="material-symbols-outlined text-amber-500">support_agent</span>
                  <span>تنظیمات شماره تماس و ایمیل مدیر سیستم جهت بازیابی رمز عبور</span>
                </h3>
                <p className="text-xs text-gray-400 mt-1">
                  در صورت فراموشی رمز عبور توسط کاربران، لینک‌ها و کدهای تایید به این اطلاعات ارسال و رونوشت می‌گردد.
                </p>
              </div>

              <form onSubmit={handleSaveContactSettings} className="space-y-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-gray-300 mb-1">
                      شماره تماس مستقیم مدیر سیستم (پیامک بازیابی) <span className="text-red-400">*</span>
                    </label>
                    <div className="relative">
                      <input
                        type="text"
                        required
                        dir="ltr"
                        value={contactData.primaryPhone}
                        onChange={(e) => setContactData({ ...contactData, primaryPhone: e.target.value })}
                        className={`w-full px-3.5 py-2.5 pl-10 rounded-xl border text-xs font-mono outline-none ${
                          isLuxury ? 'bg-[#1e1e1e] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900'
                        }`}
                      />
                      <span className="material-symbols-outlined absolute left-3 top-2.5 text-amber-400 text-base">
                        phone
                      </span>
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-gray-300 mb-1">
                      تلفن دفتر مرکزی / پشتیبانی اضطراری
                    </label>
                    <div className="relative">
                      <input
                        type="text"
                        dir="ltr"
                        value={contactData.emergencyPhone}
                        onChange={(e) => setContactData({ ...contactData, emergencyPhone: e.target.value })}
                        className={`w-full px-3.5 py-2.5 pl-10 rounded-xl border text-xs font-mono outline-none ${
                          isLuxury ? 'bg-[#1e1e1e] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900'
                        }`}
                      />
                      <span className="material-symbols-outlined absolute left-3 top-2.5 text-gray-400 text-base">
                        call
                      </span>
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-gray-300 mb-1">
                      ایمیل اصلی مدیر ارشد (دریافت رونوشت بازیابی) <span className="text-red-400">*</span>
                    </label>
                    <div className="relative">
                      <input
                        type="email"
                        required
                        dir="ltr"
                        value={contactData.primaryEmail}
                        onChange={(e) => setContactData({ ...contactData, primaryEmail: e.target.value })}
                        className={`w-full px-3.5 py-2.5 pl-10 rounded-xl border text-xs font-mono outline-none ${
                          isLuxury ? 'bg-[#1e1e1e] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900'
                        }`}
                      />
                      <span className="material-symbols-outlined absolute left-3 top-2.5 text-amber-400 text-base">
                        mark_email_read
                      </span>
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-gray-300 mb-1">
                      ایمیل سامانه بازیابی رمز
                    </label>
                    <div className="relative">
                      <input
                        type="email"
                        dir="ltr"
                        value={contactData.supportEmail}
                        onChange={(e) => setContactData({ ...contactData, supportEmail: e.target.value })}
                        className={`w-full px-3.5 py-2.5 pl-10 rounded-xl border text-xs font-mono outline-none ${
                          isLuxury ? 'bg-[#1e1e1e] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900'
                        }`}
                      />
                      <span className="material-symbols-outlined absolute left-3 top-2.5 text-gray-400 text-base">
                        mail
                      </span>
                    </div>
                  </div>
                </div>

                {/* Dispatch Method Settings */}
                <div className="p-4 rounded-xl bg-white/5 border border-white/10 space-y-3">
                  <div className="text-xs font-bold text-gray-200">
                    نحوه ارسال درخواست‌های بازیابی رمز عبور:
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 text-xs">
                    {[
                      { id: 'both', label: 'ارسال هم‌زمان (پیامک + ایمیل)', icon: 'forward_to_inbox' },
                      { id: 'sms', label: 'فقط پیامک به مدیر و کاربر', icon: 'sms' },
                      { id: 'email', label: 'فقط ایمیل امن به مدیر', icon: 'email' },
                    ].map((m) => (
                      <button
                        key={m.id}
                        type="button"
                        onClick={() => setContactData({ ...contactData, recoveryDispatchMethod: m.id as any })}
                        className={`p-2.5 rounded-lg border text-right flex items-center gap-2 transition-all ${
                          contactData.recoveryDispatchMethod === m.id
                            ? (isLuxury ? 'bg-amber-400 text-black font-bold border-amber-400' : 'bg-blue-800 text-white font-bold border-blue-800')
                            : 'bg-black/30 border-white/10 text-gray-400 hover:text-white'
                        }`}
                      >
                        <span className="material-symbols-outlined text-sm">{m.icon}</span>
                        <span className="text-[11px]">{m.label}</span>
                      </button>
                    ))}
                  </div>
                </div>

                <div className="pt-2 flex justify-end">
                  <button
                    type="submit"
                    className={`py-3 px-6 rounded-xl font-bold text-xs shadow-lg transition-all active:scale-95 flex items-center gap-2 ${
                      isLuxury ? 'bg-[#d4af37] text-black hover:bg-[#e9c349]' : 'bg-[#1e3a8a] text-white hover:bg-[#00236f]'
                    }`}
                  >
                    <span className="material-symbols-outlined text-base">save</span>
                    <span>ذخیره اطلاعات تماس مدیر و قوانین بازیابی</span>
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* TAB 4: AUDIT LOGS */}
          {activeAdminTab === 'audit_logs' && (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-bold text-white flex items-center gap-2">
                  <span className="material-symbols-outlined text-amber-500">security</span>
                  <span>لاگ‌های ممیزی، ورودها و بازیابی رمز عبور در شبکه</span>
                </h3>
                <span className="text-xs text-gray-400">
                  تعداد کل رویدادها: {activityLogs.length}
                </span>
              </div>

              <div className={`rounded-2xl border divide-y overflow-hidden ${
                isLuxury ? 'bg-[#141414] border-white/10 divide-white/5' : 'bg-white border-gray-200 divide-gray-100'
              }`}>
                {activityLogs.map((log) => (
                  <div key={log.id} className="p-3.5 text-xs flex items-start justify-between gap-3 text-right">
                    <div className="flex items-start gap-2.5">
                      <span className={`material-symbols-outlined text-base flex-shrink-0 mt-0.5 ${
                        log.source === 'admin' ? 'text-amber-400' : log.source === 'system' ? 'text-blue-400' : 'text-emerald-400'
                      }`}>
                        {log.source === 'admin' ? 'admin_panel_settings' : log.source === 'system' ? 'settings_suggest' : 'notifications'}
                      </span>
                      <div>
                        <p className="text-gray-200 leading-relaxed">{log.text}</p>
                        {log.trackingCode && (
                          <span className="font-mono text-[10px] text-amber-400 block mt-0.5">
                            کد مرجع: {log.trackingCode}
                          </span>
                        )}
                      </div>
                    </div>
                    
                    <div className="flex flex-col items-end flex-shrink-0">
                      <span className="text-[10px] text-gray-400 font-mono">{log.timestamp}</span>
                      <span className="px-1.5 py-0.5 rounded text-[9px] mt-1 bg-white/5 text-gray-300">
                        {log.source === 'admin' ? 'ادمین' : log.source === 'system' ? 'سیستم' : 'کاربر'}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

        </div>

      </div>
    </div>
  );
};
