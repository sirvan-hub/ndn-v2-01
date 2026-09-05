import React, { useState, useEffect } from 'react';
import { useApp } from '../../context/AppContext';
import { DeviceViewMode, DeviceOrientation, UserRole } from '../../types';

export const DeviceSwitcherBar: React.FC = () => {
  const { 
    deviceViewMode, 
    setDeviceViewMode, 
    deviceOrientation, 
    setDeviceOrientation,
    theme,
    role,
    setRole,
    isAuthenticated,
    showToast
  } = useApp();

  const isLuxury = theme === 'luxury';
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [windowWidth, setWindowWidth] = useState(window.innerWidth);

  useEffect(() => {
    const handleResize = () => setWindowWidth(window.innerWidth);
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const deviceModes: {
    id: DeviceViewMode;
    label: string;
    icon: string;
    desc: string;
    resolution: string;
  }[] = [
    { id: 'responsive', label: 'واکنش‌گرا (کامل)', icon: 'devices', desc: 'حالت تمام‌صفحه و متناسب با ابعاد مرورگر', resolution: `${windowWidth}px` },
    { id: 'mobile', label: 'موبایل', icon: 'smartphone', desc: 'شبیه‌ساز گوشی هوشمند (iPhone / Galaxy)', resolution: deviceOrientation === 'portrait' ? '390 × 844' : '844 × 390' },
    { id: 'tablet', label: 'تبلت', icon: 'tablet_mac', desc: 'شبیه‌ساز آیپد و تبلت‌های هوشمند', resolution: deviceOrientation === 'portrait' ? '768 × 1024' : '1024 × 768' },
    { id: 'desktop', label: 'دسکتاپ', icon: 'desktop_windows', desc: 'نمای عریض دسکتاپ و مانیتور', resolution: '1280 × 860' },
  ];

  return (
    <div className="fixed top-2 left-1/2 -translate-x-1/2 z-50 transition-all duration-300 font-sans">
      <div className={`p-1.5 sm:p-2 rounded-2xl border shadow-2xl backdrop-blur-xl flex items-center gap-2 transition-all ${
        isLuxury 
          ? 'bg-black/90 border-[#d4af37]/60 text-white shadow-[0_8px_32px_rgba(212,175,55,0.2)]' 
          : 'bg-white/95 border-blue-300 text-gray-900 shadow-blue-500/20'
      }`}>
        
        {/* Toggle Collapse button */}
        <button
          onClick={() => setIsCollapsed(!isCollapsed)}
          className={`p-1.5 rounded-xl transition-colors flex items-center justify-center ${
            isLuxury ? 'bg-white/10 hover:bg-white/20 text-amber-400' : 'bg-gray-100 hover:bg-gray-200 text-blue-900'
          }`}
          title={isCollapsed ? 'باز کردن ابزار پیش‌نمایش دستگاه‌ها' : 'بستن نوار پیش‌نمایش'}
        >
          <span className="material-symbols-outlined text-base">
            {isCollapsed ? 'visibility' : 'close_fullscreen'}
          </span>
        </button>

        {/* Collapsed view banner */}
        {isCollapsed ? (
          <div 
            onClick={() => setIsCollapsed(false)}
            className="flex items-center gap-2 px-2 py-0.5 cursor-pointer text-xs font-bold"
          >
            <span className="material-symbols-outlined text-sm text-amber-400">
              {deviceModes.find(m => m.id === deviceViewMode)?.icon || 'devices'}
            </span>
            <span>نمای فعلی: {deviceModes.find(m => m.id === deviceViewMode)?.label}</span>
            <span className="text-[10px] opacity-75 font-mono">({deviceModes.find(m => m.id === deviceViewMode)?.resolution})</span>
          </div>
        ) : (
          <>
            {/* Title / Badge */}
            <div className="hidden lg:flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-amber-400/15 border border-amber-400/30 text-amber-400 text-xs font-extrabold flex-shrink-0">
              <span className="material-symbols-outlined text-sm">devices_other</span>
              <span>پیش‌نمایش دستگاه‌ها:</span>
            </div>

            {/* Mode selection buttons */}
            <div className="flex items-center gap-1">
              {deviceModes.map((mode) => {
                const isActive = deviceViewMode === mode.id;
                return (
                  <button
                    key={mode.id}
                    onClick={() => {
                      setDeviceViewMode(mode.id);
                      showToast(`نمای نرم‌افزار به حالت "${mode.label}" تغییر کرد.`);
                    }}
                    className={`flex items-center gap-1.5 px-2.5 sm:px-3 py-1.5 rounded-xl text-xs font-bold transition-all ${
                      isActive
                        ? isLuxury
                          ? 'bg-gradient-to-r from-[#d4af37] to-[#f2ca50] text-black shadow-md shadow-amber-500/30 scale-105'
                          : 'bg-[#1e3a8a] text-white shadow-md shadow-blue-900/30 scale-105'
                        : isLuxury
                        ? 'hover:bg-white/10 text-gray-300'
                        : 'hover:bg-gray-100 text-gray-700'
                    }`}
                    title={`${mode.label} - ${mode.desc} (${mode.resolution})`}
                  >
                    <span className="material-symbols-outlined text-base">{mode.icon}</span>
                    <span className="hidden sm:inline">{mode.label}</span>
                  </button>
                );
              })}
            </div>

            {/* Orientation Switcher (Only in mobile/tablet mode) */}
            {(deviceViewMode === 'mobile' || deviceViewMode === 'tablet') && (
              <div className="flex items-center gap-1 border-r border-white/15 pr-1.5 mr-1">
                <button
                  onClick={() => {
                    const next = deviceOrientation === 'portrait' ? 'landscape' : 'portrait';
                    setDeviceOrientation(next);
                    showToast(`جهت دستگاه به "${next === 'portrait' ? 'عمودی (Portrait)' : 'افقی (Landscape)'}" تغییر کرد.`);
                  }}
                  className={`p-1.5 rounded-xl flex items-center gap-1 text-xs font-bold transition-all ${
                    isLuxury ? 'bg-white/10 hover:bg-white/20 text-amber-300' : 'bg-gray-100 hover:bg-gray-200 text-blue-900'
                  }`}
                  title="تغییر جهت دستگاه (چرخش ۹۰ درجه)"
                >
                  <span className="material-symbols-outlined text-base">
                    {deviceOrientation === 'portrait' ? 'screen_rotation' : 'screen_lock_rotation'}
                  </span>
                  <span className="text-[10px] hidden md:inline">
                    {deviceOrientation === 'portrait' ? 'عمودی' : 'افقی'}
                  </span>
                </button>
              </div>
            )}

            {/* Quick Role Switcher shortcut */}
            {isAuthenticated && (
              <div className="hidden xl:flex items-center gap-1 border-r border-white/15 pr-1.5 mr-1">
                <span className="text-[10px] text-gray-400 pl-1">نقش:</span>
                {(['customer', 'hub_manager', 'courier'] as UserRole[]).map((r) => (
                  <button
                    key={r}
                    onClick={() => setRole(r)}
                    className={`px-2 py-0.5 rounded-lg text-[10px] font-bold transition-all ${
                      role === r
                        ? 'bg-amber-400 text-black'
                        : 'text-gray-400 hover:text-white bg-white/5'
                    }`}
                  >
                    {r === 'customer' ? 'مشتری' : r === 'hub_manager' ? 'هاب' : 'سفیر'}
                  </button>
                ))}
              </div>
            )}
          </>
        )}

      </div>
    </div>
  );
};
