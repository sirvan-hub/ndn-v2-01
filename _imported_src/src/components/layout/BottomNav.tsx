import React from 'react';
import { useApp } from '../../context/AppContext';
import { NavigationTab } from '../../types';

export const BottomNav: React.FC = () => {
  const { activeTab, setActiveTab, theme, openScanner, role, deviceViewMode } = useApp();
  const isLuxury = theme === 'luxury';
  const isFramed = deviceViewMode !== 'responsive';

  // In desktop mode simulator, hide bottom nav
  if (deviceViewMode === 'desktop') {
    return null;
  }

  const navItems: { id: NavigationTab; label: string; icon: string }[] = [
    { id: 'dashboard', label: 'پیش‌خوان', icon: 'dashboard' },
    { id: 'packages', label: 'بسته‌ها', icon: 'package_2' },
    { id: 'drive', label: 'درایو', icon: 'cloud_sync' },
    { id: 'scan', label: 'اسکن', icon: 'qr_code_scanner' },
    { id: 'settings', label: 'تنظیمات', icon: 'settings' },
  ];

  const handleTabClick = (tabId: NavigationTab) => {
    if (tabId === 'scan') {
      const mode = role === 'courier' ? 'courier_register' : role === 'hub_manager' ? 'hub_receive' : 'customer_confirm';
      openScanner(mode);
    } else {
      setActiveTab(tabId);
    }
  };

  const showClass = deviceViewMode === 'mobile' ? 'flex' : 'flex md:hidden';

  return (
    <nav className={`${isFramed ? 'sticky bottom-0' : 'fixed bottom-0 left-0 right-0'} w-full z-40 ${showClass} justify-around items-center px-2 py-2 border-t rounded-t-2xl shadow-[0_-4px_16px_rgba(0,0,0,0.4)] backdrop-blur-lg mt-auto ${
      isLuxury 
        ? 'bg-[#0e0e0e]/95 border-[#353535] text-[#d0c5af]' 
        : 'bg-white/95 border-[#c5c5d3] text-[#444651]'
    }`}>
      {navItems.map((item) => {
        const isActive = activeTab === item.id;
        return (
          <button
            key={item.id}
            onClick={() => handleTabClick(item.id)}
            className={`relative flex flex-col items-center justify-center py-1.5 px-3 rounded-xl transition-all duration-200 ${
              isActive 
                ? (isLuxury ? 'text-[#f2ca50]' : 'text-[#fd761a]') 
                : 'hover:text-amber-400 active:scale-90 text-gray-400'
            }`}
          >
            {isActive && (
              <span className={`absolute -top-2 w-8 h-1 rounded-b-full ${
                isLuxury ? 'bg-[#d4af37]' : 'bg-[#fd761a]'
              }`} />
            )}
            <span 
              className={`material-symbols-outlined text-2xl transition-transform ${isActive ? 'scale-110' : ''}`}
              style={isActive ? { fontVariationSettings: "'FILL' 1, 'wght' 600" } : undefined}
            >
              {item.icon}
            </span>
            <span className={`text-[11px] mt-0.5 ${isActive ? 'font-bold' : 'font-medium'}`}>
              {item.label}
            </span>
          </button>
        );
      })}
    </nav>
  );
};
