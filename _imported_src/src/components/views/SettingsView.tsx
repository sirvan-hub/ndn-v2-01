import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import { UserRole, AppTheme } from '../../types';
import { AdminPortal } from '../admin/AdminPortal';

export const SettingsView: React.FC = () => {
  const {
    theme,
    setTheme,
    role,
    setRole,
    notificationsEnabled,
    setNotificationsEnabled,
    selectedHub,
    systemAdminContact,
    adminUsers,
    isAdminAuthenticated,
    registeredUsers,
    savedBiometricUserId,
    setSavedBiometricUserId,
    toggleBiometricsForUser,
    setActiveTab,
    showToast
  } = useApp();

  const isLuxury = theme === 'luxury';
  const [isAdminPortalOpen, setIsAdminPortalOpen] = useState(false);

  return (
    <div className="w-full max-w-4xl mx-auto space-y-6 animate-in fade-in duration-200">
      
      <div className="border-b pb-4 border-gray-700/30 text-right">
        <h2 className={`text-2xl font-bold ${isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'}`}>
          تنظیمات سامانه NDN
        </h2>
        <p className="text-xs text-gray-400 mt-1">
          شخصی‌سازی ظاهر، مدیریت اعلان‌های پستی، تعیین نقش و دسترسی اختصاصی مدیریت ارشد
        </p>
      </div>

      {/* DEDICATED ADMIN & ACCESS CONTROL SECTION */}
      <section className={`p-6 rounded-2xl border-2 shadow-xl relative overflow-hidden ${
        isLuxury 
          ? 'bg-gradient-to-br from-[#121212] via-[#0d0d0d] to-[#18150a] border-[#d4af37]' 
          : 'bg-gradient-to-br from-blue-900 to-indigo-950 text-white border-blue-600'
      }`}>
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="flex items-start gap-3.5">
            <div className="w-12 h-12 rounded-2xl bg-amber-400/20 border border-amber-400/50 flex items-center justify-center text-amber-400 flex-shrink-0">
              <span className="material-symbols-outlined text-3xl">admin_panel_settings</span>
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-bold text-white">
                  پنل مدیریت کل و کنترل سطوح دسترسی (Admin Portal)
                </h3>
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-400 text-black">
                  دسترسی ویژه
                </span>
              </div>
              <p className="text-xs text-gray-300 mt-1 leading-relaxed">
                تعریف کاربران ادمین، مدیران سیستم و ممیزان، پیکربندی سطوح دسترسی (RBAC) و تنظیم شماره تماس و ایمیل مدیر جهت بازیابی رمز عبور.
              </p>
            </div>
          </div>

          <button
            onClick={() => setIsAdminPortalOpen(true)}
            className={`w-full sm:w-auto px-5 py-3 rounded-xl font-bold text-xs shadow-lg transition-all active:scale-95 flex items-center justify-center gap-2 whitespace-nowrap ${
              isLuxury 
                ? 'bg-[#d4af37] text-black hover:bg-[#e9c349]' 
                : 'bg-white text-blue-900 hover:bg-gray-100'
            }`}
          >
            <span className="material-symbols-outlined text-base">key</span>
            <span>ورود به پنل ادمین و مدیران</span>
          </button>
        </div>

        {/* Quick info strip inside Admin banner */}
        <div className="mt-4 pt-4 border-t border-white/10 grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs text-gray-300">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-amber-400 text-sm">badge</span>
            <span>مدیران فعال: <strong className="text-white">{adminUsers.length} کاربر</strong></span>
          </div>
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-amber-400 text-sm">phone</span>
            <span>تلفن بازیابی رمز: <strong className="text-white font-mono">{systemAdminContact.primaryPhone}</strong></span>
          </div>
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-amber-400 text-sm">mail</span>
            <span>ایمیل مدیر: <strong className="text-amber-300 font-mono">{systemAdminContact.primaryEmail}</strong></span>
          </div>
        </div>
      </section>

      {/* Theme Selector (Matching Screen 10 & 11) */}
      <section className={`p-6 rounded-2xl border shadow-sm ${
        isLuxury ? 'bg-[#0e0e0e] border-[#353535]' : 'bg-white border-gray-200'
      }`}>
        <h3 className="text-base font-bold text-white mb-2 flex items-center gap-2">
          <span className="material-symbols-outlined text-amber-500">palette</span>
          <span>انتخاب تم و هویت بصری</span>
        </h3>
        <p className="text-xs text-gray-400 mb-4">
          می‌توانید بین دو طراحی اصیل انتخاب نمایید:
        </p>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {/* Luxury Theme Card */}
          <div
            onClick={() => {
              setTheme('luxury');
              showToast('تم "مشکی و طلایی لوکس" فعال شد.');
            }}
            className={`p-4 rounded-xl border-2 cursor-pointer transition-all flex flex-col gap-3 ${
              theme === 'luxury'
                ? 'border-[#d4af37] bg-black shadow-[0_0_15px_rgba(212,175,55,0.25)]'
                : 'border-gray-800 bg-[#141414] hover:border-gray-700 opacity-70'
            }`}
          >
            <div className="flex justify-between items-center">
              <span className="text-sm font-bold text-[#f2ca50]">مشکی و طلایی لوکس (Obsidian & Gold)</span>
              {theme === 'luxury' && (
                <span className="material-symbols-outlined text-[#f2ca50] text-lg">check_circle</span>
              )}
            </div>
            <div className="h-14 rounded-lg bg-black border border-[#d4af37]/40 flex items-center justify-around px-3">
              <div className="w-8 h-8 rounded-full bg-[#d4af37] flex items-center justify-center text-black font-bold text-xs">NDN</div>
              <div className="w-16 h-2 rounded bg-white/20"></div>
              <div className="w-8 h-4 rounded bg-[#fd761a]"></div>
            </div>
            <span className="text-[11px] text-gray-400">طراحی مدرن شبانه با کنتراست فوق‌العاده و جلوه طلایی</span>
          </div>

          {/* Classic Theme Card */}
          <div
            onClick={() => {
              setTheme('classic');
              showToast('تم "آبی سازمانی و نارنجی" فعال شد.');
            }}
            className={`p-4 rounded-xl border-2 cursor-pointer transition-all flex flex-col gap-3 ${
              theme === 'classic'
                ? 'border-[#1e3a8a] bg-blue-900/30 shadow-lg'
                : 'border-gray-800 bg-[#141414] hover:border-gray-700 opacity-70'
            }`}
          >
            <div className="flex justify-between items-center">
              <span className="text-sm font-bold text-blue-400">کلاسیک سازمانی (Corporate Blue & Orange)</span>
              {theme === 'classic' && (
                <span className="material-symbols-outlined text-blue-400 text-lg">check_circle</span>
              )}
            </div>
            <div className="h-14 rounded-lg bg-white border border-blue-200 flex items-center justify-around px-3">
              <div className="w-8 h-8 rounded-full bg-[#1e3a8a] flex items-center justify-center text-white font-bold text-xs">NDN</div>
              <div className="w-16 h-2 rounded bg-gray-200"></div>
              <div className="w-8 h-4 rounded bg-[#fd761a]"></div>
            </div>
            <span className="text-[11px] text-gray-400">طراحی رسمی پستی با رنگ‌های استاندارد خدمات شهری</span>
          </div>
        </div>
      </section>

      {/* Role Selection */}
      <section className={`p-6 rounded-2xl border shadow-sm ${
        isLuxury ? 'bg-[#0e0e0e] border-[#353535]' : 'bg-white border-gray-200'
      }`}>
        <h3 className="text-base font-bold text-white mb-2 flex items-center gap-2">
          <span className="material-symbols-outlined text-amber-500">switch_account</span>
          <span>پنل کاربری فعال</span>
        </h3>
        <p className="text-xs text-gray-400 mb-4">
          تغییر فوری دسترسی بین گیرنده/فرستنده، مامور پخش، یا صاحب کسب‌وکار (هاب):
        </p>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          {[
            { id: 'customer', title: 'پنل مشتری', desc: 'پیگیری و تایید تحویل', icon: 'account_circle' },
            { id: 'courier', title: 'پنل مامور پست', desc: 'اسکن و تحویل به هاب', icon: 'local_shipping' },
            { id: 'hub_manager', title: 'مدیریت هاب', desc: 'انبارداری و تحویل نهایی', icon: 'storefront' },
          ].map((r) => (
            <button
              key={r.id}
              onClick={() => {
                setRole(r.id as UserRole);
                showToast(`نقش به "${r.title}" تغییر یافت.`);
              }}
              className={`p-4 rounded-xl border flex flex-col items-center text-center gap-2 transition-all ${
                role === r.id
                  ? (isLuxury ? 'border-[#d4af37] bg-amber-400/15 text-[#f2ca50] font-bold' : 'border-[#1e3a8a] bg-blue-50 text-blue-900 font-bold')
                  : 'border-gray-800 bg-[#141414] text-gray-400 hover:border-gray-700'
              }`}
            >
              <span className="material-symbols-outlined text-3xl">{r.icon}</span>
              <span className="text-sm">{r.title}</span>
              <span className="text-[10px] opacity-75">{r.desc}</span>
            </button>
          ))}
        </div>
      </section>

      {/* Biometric & Passkey Management */}
      <section className={`p-6 rounded-2xl border shadow-sm ${
        isLuxury ? 'bg-[#0e0e0e] border-[#353535]' : 'bg-white border-gray-200'
      }`}>
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-base font-bold text-white flex items-center gap-2">
            <span className="material-symbols-outlined text-amber-400">fingerprint</span>
            <span>مدیریت احراز هویت بیومتریک و اثر انگشت</span>
          </h3>
          <span className="text-[11px] font-mono px-2.5 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
            FIDO2 / Passkey
          </span>
        </div>
        
        <p className="text-xs text-gray-400 mb-4 leading-relaxed">
          با فعال‌سازی احراز هویت بیومتریک روی این سیستم، می‌توانید در صفحه اول تنها با یک لمس اثر انگشت یا اسکن چهره بدون نیاز به وارد کردن کلمه عبور و انتخاب نقش وارد پنل خود شوید.
        </p>

        <div className="space-y-3">
          {registeredUsers.map((u) => {
            const isBound = u.hasBiometricsEnabled !== false;
            const isPrimary = savedBiometricUserId === u.id;
            return (
              <div 
                key={u.id}
                className={`p-3.5 rounded-xl border flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 ${
                  isPrimary
                    ? isLuxury ? 'bg-amber-400/10 border-amber-400/40' : 'bg-blue-50 border-blue-300'
                    : 'bg-white/5 border-white/10'
                }`}
              >
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-full bg-amber-400/20 text-amber-400 flex items-center justify-center font-bold text-xs">
                    {u.fullName.charAt(0)}
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-bold text-white">{u.fullName}</span>
                      <span className="text-[10px] px-2 py-0.5 rounded bg-white/10 text-gray-300">
                        {u.role === 'courier' ? 'مامور توزیع' : u.role === 'hub_manager' ? 'هاب محله' : 'مشتری'}
                      </span>
                      {isPrimary && (
                        <span className="text-[9px] px-1.5 py-0.5 rounded bg-amber-400 text-black font-bold">
                          حساب پیش‌فرض اثر انگشت
                        </span>
                      )}
                    </div>
                    <span className="text-[11px] text-gray-400 font-mono">{u.phone}</span>
                  </div>
                </div>

                <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
                  {!isPrimary && isBound && (
                    <button
                      type="button"
                      onClick={() => {
                        setSavedBiometricUserId(u.id);
                        localStorage.setItem('ndn_saved_biometric_user_id', u.id);
                        showToast(`حساب "${u.fullName}" به عنوان حساب پیش‌فرض ورود با اثر انگشت تعیین شد.`);
                      }}
                      className="px-2.5 py-1 rounded-lg text-[10px] bg-white/10 hover:bg-white/20 text-gray-200 transition-all"
                    >
                      تعیین به عنوان پیش‌فرض
                    </button>
                  )}

                  <button
                    type="button"
                    onClick={() => toggleBiometricsForUser(u.id, !isBound)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5 ${
                      isBound
                        ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40'
                        : 'bg-white/10 text-gray-400 hover:text-white'
                    }`}
                  >
                    <span className="material-symbols-outlined text-sm">
                      {isBound ? 'check_circle' : 'fingerprint'}
                    </span>
                    <span>{isBound ? 'فعال روی این سیستم' : 'غیرفعال'}</span>
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* Google Drive Integration */}
      <section className={`p-6 rounded-2xl border shadow-sm ${
        isLuxury ? 'bg-[#0e0e0e] border-[#353535]' : 'bg-white border-gray-200'
      }`}>
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <h3 className="text-base font-bold text-white flex items-center gap-2">
              <span className="material-symbols-outlined text-amber-400">cloud_sync</span>
              <span>یکپارچگی ابری با Google Drive</span>
            </h3>
            <p className="text-xs text-gray-400 mt-1 leading-relaxed">
              پشتیبان‌گیری خودکار از مرسولات پستی، بارگذاری تصاویر و بارکدها، و مدیریت امن پوشه‌ها در حساب شخصی Google Drive.
            </p>
          </div>

          <button
            onClick={() => setActiveTab('drive')}
            className={`w-full sm:w-auto px-5 py-2.5 rounded-xl text-xs font-bold transition-all shadow-md flex items-center justify-center gap-2 ${
              isLuxury 
                ? 'bg-gradient-to-r from-amber-500 to-yellow-400 text-black hover:brightness-110' 
                : 'bg-blue-600 text-white hover:bg-blue-700'
            }`}
          >
            <span className="material-symbols-outlined text-base">folder_open</span>
            <span>ورود به بخش Google Drive</span>
          </button>
        </div>
      </section>

      {/* Notifications & Connectivity */}
      <section className={`p-6 rounded-2xl border shadow-sm ${
        isLuxury ? 'bg-[#0e0e0e] border-[#353535]' : 'bg-white border-gray-200'
      }`}>
        <h3 className="text-base font-bold text-white mb-4 flex items-center gap-2">
          <span className="material-symbols-outlined text-amber-500">notifications_active</span>
          <span>اعلان‌ها و همگام‌سازی ابری</span>
        </h3>

        <div className="flex items-center justify-between py-3 border-b border-gray-800">
          <div>
            <span className="text-xs font-semibold text-white block">اعلان‌های پیامکی و درون‌برنامه‌ای</span>
            <span className="text-[11px] text-gray-400">اطلاع‌رسانی آنی هنگام تحویل بسته به هاب یا تغییر وضعیت</span>
          </div>
          <button
            onClick={() => setNotificationsEnabled(!notificationsEnabled)}
            className={`w-12 h-6 rounded-full p-0.5 transition-colors relative flex items-center ${
              notificationsEnabled ? 'bg-[#d4af37]' : 'bg-gray-700'
            }`}
          >
            <div className={`w-5 h-5 rounded-full bg-white transition-transform ${
              notificationsEnabled ? 'translate-x-0' : '-translate-x-6'
            }`} />
          </button>
        </div>

        <div className="flex items-center justify-between py-3">
          <div>
            <span className="text-xs font-semibold text-white block">پروتکل امنیتی PUDO محله</span>
            <span className="text-[11px] text-gray-400">رمزنگاری سرتاسری بارکدها و تاییدیه دوطرفه</span>
          </div>
          <span className="px-2.5 py-1 rounded-full bg-emerald-500/20 text-emerald-400 text-xs font-mono">
            فعال (TLS 1.3)
          </span>
        </div>
      </section>

      {/* Hub Details Info */}
      <section className={`p-6 rounded-2xl border shadow-sm ${
        isLuxury ? 'bg-[#0e0e0e] border-[#353535]' : 'bg-white border-gray-200'
      }`}>
        <h3 className="text-base font-bold text-white mb-4 flex items-center gap-2">
          <span className="material-symbols-outlined text-amber-500">verified</span>
          <span>اطلاعات هاب فعال محله</span>
        </h3>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
          <div className="p-3 rounded-xl bg-white/5 border border-white/10">
            <span className="text-gray-400 block text-[10px]">نام هاب و مدیر</span>
            <span className="font-bold text-white">{selectedHub.name} (مدیریت: {selectedHub.managerName})</span>
          </div>
          <div className="p-3 rounded-xl bg-white/5 border border-white/10">
            <span className="text-gray-400 block text-[10px]">شماره پروانه کسب</span>
            <span className="font-mono text-amber-400">{selectedHub.licenseNumber}</span>
          </div>
          <div className="p-3 rounded-xl bg-white/5 border border-white/10">
            <span className="text-gray-400 block text-[10px]">تلفن تماس هاب</span>
            <span className="font-mono text-gray-200">{selectedHub.phone}</span>
          </div>
          <div className="p-3 rounded-xl bg-white/5 border border-white/10">
            <span className="text-gray-400 block text-[10px]">ساعات کاری تایید شده</span>
            <span className="font-mono text-gray-200">{selectedHub.workingHours}</span>
          </div>
        </div>
      </section>

      {/* Admin Portal Modal */}
      {isAdminPortalOpen && (
        <AdminPortal onClose={() => setIsAdminPortalOpen(false)} />
      )}

    </div>
  );
};
