import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import { UserRole } from '../../types';

export const TopHeader: React.FC = () => {
  const { 
    role, 
    setRole, 
    theme, 
    deviceViewMode,
    activityLogs, 
    notificationsEnabled, 
    setNotificationsEnabled, 
    setActiveTab,
    isAuthenticated,
    currentUser,
    switchPanelFlow
  } = useApp();

  const [showRoleMenu, setShowRoleMenu] = useState(false);
  const [showNotifications, setShowNotifications] = useState(false);

  const isLuxury = theme === 'luxury';
  const isFramed = deviceViewMode !== 'responsive';

  const roleLabels: Record<UserRole, { title: string; subtitle: string; icon: string }> = {
    customer: { title: 'پنل مشتری', subtitle: 'گیرنده / فرستنده محله', icon: 'account_circle' },
    courier: { title: 'پنل مامور توزیع', subtitle: 'سفیر PuDo شبکه', icon: 'local_shipping' },
    hub_manager: { title: 'مدیریت هاب', subtitle: 'مدیر فروشگاه و هاب', icon: 'storefront' }
  };

  return (
    <header className={`${isFramed ? 'sticky top-0' : 'fixed top-0 left-0 right-0'} w-full z-40 transition-colors duration-200 ${
      isLuxury ? 'bg-black/95 text-gray-100 border-b border-[#353535] backdrop-blur-md' : 'bg-[#f8f9ff]/95 text-[#0b1c30] border-b border-[#c5c5d3] backdrop-blur-md'
    }`}>
      <div className="max-w-7xl mx-auto px-4 md:px-8 h-16 flex items-center justify-between">
        
        {/* Right side in RTL (User profile / Panel Switcher) */}
        <div className="relative flex items-center gap-2">
          {isAuthenticated && currentUser ? (
            <button
              onClick={() => setActiveTab('auth')}
              className={`flex items-center gap-2.5 px-3 py-1.5 rounded-full transition-all border ${
                isLuxury 
                  ? 'hover:bg-[#1f1f1f] border-[#d4af37]/40 bg-[#131313]' 
                  : 'hover:bg-[#e5eeff] border-[#1e3a8a]/30 bg-white'
              }`}
              title="مشاهده پروفایل کاربری و امنیت"
            >
              <div className="w-7 h-7 rounded-full overflow-hidden border border-amber-400 bg-amber-400/20 flex items-center justify-center">
                {currentUser.profileAvatar ? (
                  <img 
                    src={currentUser.profileAvatar}
                    alt={currentUser.fullName}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <span className="material-symbols-outlined text-amber-400 text-lg">
                    {roleLabels[currentUser.role]?.icon || 'person'}
                  </span>
                )}
              </div>
              
              <div className="hidden sm:flex flex-col text-right">
                <span className={`text-xs font-bold ${isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'}`}>
                  {currentUser.fullName}
                </span>
                <span className="text-[9px] text-gray-400">
                  {roleLabels[currentUser.role]?.title}
                </span>
              </div>
            </button>
          ) : (
            <button
              onClick={switchPanelFlow}
              className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-bold transition-all border shadow-sm ${
                isLuxury 
                  ? 'bg-gradient-to-r from-[#d4af37] to-[#f2ca50] text-black border-amber-400 hover:brightness-110' 
                  : 'bg-[#1e3a8a] text-white border-blue-900 hover:bg-[#00236f]'
              }`}
            >
              <span className="material-symbols-outlined text-base">login</span>
              <span>انتخاب پنل / ورود و ثبت نام</span>
            </button>
          )}

          {/* Role quick switcher button */}
          <button
            onClick={() => setShowRoleMenu(!showRoleMenu)}
            className="p-2 rounded-xl text-gray-400 hover:text-white hover:bg-white/5 transition-colors text-xs flex items-center gap-1 border border-white/5"
            title="تغییر سریع پنل"
          >
            <span className="material-symbols-outlined text-sm">swap_vert</span>
            <span className="hidden md:inline text-[10px]">تغییر نقش</span>
          </button>

          {/* Role selector dropdown */}
          {showRoleMenu && (
            <div className={`absolute top-full right-0 mt-2 w-60 rounded-xl shadow-2xl border p-2 z-50 animate-in fade-in zoom-in-95 duration-150 ${
              isLuxury ? 'bg-[#1a1a1a] border-[#d4af37]/40 text-white' : 'bg-white border-blue-200 text-gray-800'
            }`}>
              <div className="text-[11px] font-semibold text-gray-400 px-3 py-1 border-b border-gray-700/30 mb-1 flex justify-between items-center">
                <span>تغییر سریع پنل کاربری:</span>
                <button onClick={switchPanelFlow} className="text-amber-400 text-[10px] hover:underline">
                  احراز هویت مجدد
                </button>
              </div>
              {(['customer', 'courier', 'hub_manager'] as UserRole[]).map((r) => (
                <button
                  key={r}
                  onClick={() => {
                    setRole(r);
                    setShowRoleMenu(false);
                  }}
                  className={`w-full flex items-center justify-between px-3 py-2.5 rounded-lg text-right text-xs transition-colors ${
                    role === r 
                      ? (isLuxury ? 'bg-[#d4af37] text-black font-bold' : 'bg-[#1e3a8a] text-white font-bold') 
                      : (isLuxury ? 'hover:bg-white/10 text-gray-300' : 'hover:bg-blue-50 text-gray-700')
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <span className="material-symbols-outlined text-base">
                      {roleLabels[r].icon}
                    </span>
                    <div>
                      <div>{roleLabels[r].title}</div>
                      <div className="text-[10px] opacity-75">{roleLabels[r].subtitle}</div>
                    </div>
                  </div>
                  {role === r && <span className="material-symbols-outlined text-sm">check</span>}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Center Brand Title */}
        <div className="flex flex-col items-center cursor-pointer" onClick={() => setActiveTab('dashboard')}>
          <h1 className={`text-sm sm:text-base md:text-xl font-bold tracking-tight text-center ${
            isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'
          }`}>
            سامانه تحویل مطمئن محله (NDN)
          </h1>
          <span className="text-[10px] text-gray-400 hidden sm:block">
            شبکه توزیع محلی، هاب‌های PUDO و احراز هویت امن
          </span>
        </div>

        {/* Left side in RTL (Security SSL badge + Google Drive + Notifications) */}
        <div className="flex items-center gap-2 sm:gap-3">
          {/* Google Drive Link Button */}
          <button
            onClick={() => setActiveTab('drive')}
            className={`hidden sm:flex items-center gap-1.5 px-3 py-1 rounded-full text-xs border transition-all ${
              isLuxury 
                ? 'bg-[#1f1f1f] text-amber-300 border-amber-500/30 hover:bg-amber-400/10' 
                : 'bg-blue-50 text-blue-900 border-blue-200 hover:bg-blue-100'
            }`}
            title="پشتیبان‌گیری و مدیریت Google Drive"
          >
            <span className="material-symbols-outlined text-xs text-amber-400">cloud_sync</span>
            <span className="text-[10px] font-semibold">Google Drive</span>
          </button>

          {/* SSL / HTTPS Active badge */}
          <div className={`hidden lg:flex items-center gap-1.5 px-3 py-1 rounded-full text-xs border ${
            isLuxury 
              ? 'bg-[#1f1f1f] text-emerald-400 border-emerald-500/30' 
              : 'bg-[#eff4ff] text-blue-900 border-blue-200'
          }`}>
            <span className="material-symbols-outlined text-xs text-emerald-400">https</span>
            <span className="font-mono text-[10px]">TLS 1.3 Secure</span>
          </div>

          {/* Notifications Button */}
          <div className="relative">
            <button
              onClick={() => setShowNotifications(!showNotifications)}
              className={`relative p-2 rounded-full transition-colors ${
                isLuxury 
                  ? 'hover:bg-[#2a2a2a] text-[#d4af37]' 
                  : 'hover:bg-[#e5eeff] text-[#1e3a8a]'
              }`}
              title="اعلان‌ها"
            >
              <span className="material-symbols-outlined text-2xl">notifications</span>
              {activityLogs.length > 0 && notificationsEnabled && (
                <span className="absolute top-1.5 right-1.5 w-2.5 h-2.5 bg-red-500 rounded-full border-2 border-black animate-ping"></span>
              )}
            </button>

            {/* Notification popover */}
            {showNotifications && (
              <div className={`absolute left-0 top-full mt-2 w-80 max-w-[90vw] rounded-xl shadow-2xl border p-3 z-50 ${
                isLuxury ? 'bg-[#181818] border-[#353535] text-gray-100' : 'bg-white border-gray-200 text-gray-900'
              }`}>
                <div className="flex items-center justify-between pb-2 mb-2 border-b border-gray-700/30">
                  <div className="flex items-center gap-1.5 font-bold text-xs">
                    <span className="material-symbols-outlined text-sm text-amber-500">history</span>
                    <span>آخرین رویدادهای زنده</span>
                  </div>
                  <button
                    onClick={() => setNotificationsEnabled(!notificationsEnabled)}
                    className="text-[10px] text-gray-400 hover:text-amber-400"
                  >
                    {notificationsEnabled ? 'بی‌صدا کردن' : 'فعال‌سازی'}
                  </button>
                </div>
                
                <div className="max-h-60 overflow-y-auto space-y-2 text-xs">
                  {activityLogs.map((log) => (
                    <div 
                      key={log.id} 
                      className={`p-2.5 rounded-lg border text-right transition-all ${
                        isLuxury ? 'bg-[#222222] border-white/5' : 'bg-blue-50/50 border-blue-100'
                      }`}
                    >
                      <div className="flex items-center justify-between text-[10px] text-gray-400 mb-1">
                        <span className="font-mono">{log.timestamp}</span>
                        <span className="px-1.5 py-0.5 rounded text-[9px] bg-amber-500/20 text-amber-400">
                          {log.source === 'courier' ? 'سفیر' : log.source === 'hub' ? 'هاب' : 'مشتری'}
                        </span>
                      </div>
                      <p className="leading-relaxed">{log.text}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>

      </div>
    </header>
  );
};
