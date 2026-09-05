import React from 'react';
import { useApp } from '../../context/AppContext';
import { NavigationTab } from '../../types';

export const SideNav: React.FC = () => {
  const { activeTab, setActiveTab, theme, openScanner, role, deviceViewMode } = useApp();
  const isLuxury = theme === 'luxury';
  const isFramed = deviceViewMode !== 'responsive';

  if (deviceViewMode === 'mobile') {
    return null;
  }

  const navItems: { id: NavigationTab; label: string; icon: string; desc: string }[] = [
    { id: 'dashboard', label: 'پیش‌خوان', icon: 'dashboard', desc: 'نمای کلی و آمار وضعیت' },
    { id: 'packages', label: 'بسته‌ها', icon: 'package_2', desc: 'مدیریت و پیگیری مرسولات' },
    { id: 'drive', label: 'Google Drive', icon: 'cloud_sync', desc: 'پشتیبان‌گیری و فایل‌های ابری' },
    { id: 'map', label: 'نقشه و هاب‌ها', icon: 'map', desc: 'جستجو و مسیریابی هاب‌ها' },
    { id: 'scan', label: 'اسکنر سریع', icon: 'qr_code_scanner', desc: 'ثبت و تحویل با بارکد' },
    { id: 'auth', label: 'ورود / ثبت‌نام', icon: 'how_to_reg', desc: 'مدیریت حساب و احراز هویت' },
    { id: 'settings', label: 'تنظیمات NDN', icon: 'settings', desc: 'انتخاب قالب و اعلانات' },
  ];

  const handleTabClick = (tabId: NavigationTab) => {
    if (tabId === 'scan') {
      const mode = role === 'courier' ? 'courier_register' : role === 'hub_manager' ? 'hub_receive' : 'customer_confirm';
      openScanner(mode);
    } else {
      setActiveTab(tabId);
    }
  };

  return (
    <aside className={`hidden md:flex flex-col w-64 ${isFramed ? 'sticky top-16 h-[calc(100%-4rem)]' : 'fixed right-0 top-16 bottom-0'} z-30 p-4 border-l transition-colors duration-200 ${
      isLuxury 
        ? 'bg-[#131313] border-[#353535] text-gray-200' 
        : 'bg-[#eff4ff] border-[#c5c5d3] text-[#0b1c30]'
    }`}>
      <div className="flex flex-col gap-1.5 flex-1">
        <div className="text-[11px] font-bold text-gray-500 uppercase px-3 py-2">
          منوی دسترسی
        </div>
        {navItems.map((item) => {
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => handleTabClick(item.id)}
              className={`flex items-center gap-3 px-3 py-3 rounded-xl text-right transition-all group ${
                isActive
                  ? (isLuxury 
                      ? 'bg-[#d4af37] text-black font-bold shadow-lg shadow-amber-500/10' 
                      : 'bg-[#1e3a8a] text-white font-bold shadow-md')
                  : (isLuxury 
                      ? 'hover:bg-white/5 text-gray-300' 
                      : 'hover:bg-white text-gray-700')
              }`}
            >
              <span 
                className="material-symbols-outlined text-2xl transition-transform group-hover:scale-110"
                style={isActive ? { fontVariationSettings: "'FILL' 1" } : undefined}
              >
                {item.icon}
              </span>
              <div className="flex flex-col">
                <span className="text-sm">{item.label}</span>
                <span className={`text-[10px] ${isActive ? 'opacity-90' : 'text-gray-500'}`}>
                  {item.desc}
                </span>
              </div>
            </button>
          );
        })}
      </div>

      {/* Footer Info in Sidebar */}
      <div className={`p-3 rounded-xl border text-xs text-right mt-auto ${
        isLuxury ? 'bg-[#1f1f1f] border-white/5 text-gray-400' : 'bg-white border-blue-100 text-gray-600'
      }`}>
        <div className="flex items-center gap-1.5 font-bold mb-1 text-amber-500">
          <span className="material-symbols-outlined text-base">verified_user</span>
          <span>شبکه امن محله</span>
        </div>
        <p className="text-[11px] leading-relaxed">
          سامانه توزیع پستی نسل نو (PUDO) با اتصال مستقیم به هاب‌های محلی.
        </p>
      </div>
    </aside>
  );
};
