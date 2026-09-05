import React, { useState, useEffect } from 'react';
import { useApp } from '../../context/AppContext';

interface DeviceFrameWrapperProps {
  children: React.ReactNode;
}

export const DeviceFrameWrapper: React.FC<DeviceFrameWrapperProps> = ({ children }) => {
  const { deviceViewMode, deviceOrientation, theme } = useApp();
  const isLuxury = theme === 'luxury';

  const [currentTime, setCurrentTime] = useState('۱۲:۴۵');

  useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      const hours = now.getHours().toString().padStart(2, '0');
      const minutes = now.getMinutes().toString().padStart(2, '0');
      setCurrentTime(`${hours}:${minutes}`);
    };
    updateTime();
    const interval = setInterval(updateTime, 30000);
    return () => clearInterval(interval);
  }, []);

  // Standard responsive fluid layout without artificial frame
  if (deviceViewMode === 'responsive') {
    return <>{children}</>;
  }

  // Mobile Simulator Frame
  if (deviceViewMode === 'mobile') {
    const isPortrait = deviceOrientation === 'portrait';
    return (
      <div className="w-full min-h-screen py-16 px-2 flex flex-col items-center justify-center bg-[#07090e] transition-all duration-300">
        
        {/* Device metadata tag */}
        <div className="mb-3 text-xs text-amber-400/80 font-mono flex items-center gap-2">
          <span className="material-symbols-outlined text-sm">smartphone</span>
          <span>شبیه‌ساز گوشی همراه ({isPortrait ? 'عمودی ۳۹۰×۸۴۴' : 'افقی ۸۴۴×۳۹۰'})</span>
          <span className="px-2 py-0.5 rounded-full bg-amber-400/20 text-[10px] text-amber-300 font-bold">iOS / Android</span>
        </div>

        {/* Smartphone Shell */}
        <div 
          className={`relative rounded-[50px] p-3 border-[9px] border-[#2c2d30] shadow-[0_25px_70px_rgba(0,0,0,0.85),0_0_20px_rgba(212,175,55,0.15)] transition-all duration-300 ${
            isPortrait ? 'w-[390px] h-[844px]' : 'w-[844px] h-[480px]'
          } bg-black flex flex-col overflow-hidden`}
        >
          {/* Outer Volume & Power Button aesthetic notches */}
          <div className="absolute -left-3 top-24 w-1 h-12 bg-gray-600 rounded-l-md pointer-events-none" />
          <div className="absolute -left-3 top-40 w-1 h-12 bg-gray-600 rounded-l-md pointer-events-none" />
          <div className="absolute -right-3 top-28 w-1 h-16 bg-gray-600 rounded-r-md pointer-events-none" />

          {/* Device Screen Canvas */}
          <div className={`relative w-full h-full rounded-[42px] overflow-hidden flex flex-col ${
            isLuxury ? 'bg-black text-gray-100' : 'bg-[#f8f9ff] text-[#0b1c30]'
          }`}>
            
            {/* Mobile Top Status Bar */}
            <div className="h-11 w-full flex items-center justify-between px-6 z-50 flex-shrink-0 bg-transparent select-none">
              <span className="font-mono text-xs font-bold tracking-tight text-gray-200">{currentTime}</span>

              {/* Dynamic Island / Camera Notch */}
              <div className="w-28 h-6 bg-black rounded-full border border-gray-800 flex items-center justify-center gap-2 shadow-inner px-2">
                <div className="w-2 h-2 rounded-full bg-[#111] border border-blue-900/60" />
                <div className="w-1.5 h-1.5 rounded-full bg-emerald-500/80 animate-pulse" />
              </div>

              {/* Status Icons */}
              <div className="flex items-center gap-1.5 text-gray-200">
                <span className="material-symbols-outlined text-xs">signal_cellular_4_bar</span>
                <span className="material-symbols-outlined text-xs">wifi</span>
                <div className="flex items-center gap-0.5">
                  <span className="text-[10px] font-mono">98%</span>
                  <span className="material-symbols-outlined text-sm text-emerald-400">battery_charging_full</span>
                </div>
              </div>
            </div>

            {/* Inner Content Area (Scrollable mobile viewport) */}
            <div className="flex-1 w-full overflow-y-auto overflow-x-hidden relative custom-scrollbar flex flex-col">
              {children}
            </div>

            {/* Bottom Home Indicator Bar */}
            <div className="h-6 w-full flex items-center justify-center flex-shrink-0 z-50 select-none pointer-events-none bg-gradient-to-t from-black/40 to-transparent">
              <div className="w-32 h-1 bg-gray-400/60 rounded-full" />
            </div>

          </div>
        </div>
      </div>
    );
  }

  // Tablet Simulator Frame
  if (deviceViewMode === 'tablet') {
    const isPortrait = deviceOrientation === 'portrait';
    return (
      <div className="w-full min-h-screen py-16 px-2 flex flex-col items-center justify-center bg-[#07090e] transition-all duration-300">
        
        {/* Device metadata tag */}
        <div className="mb-3 text-xs text-amber-400/80 font-mono flex items-center gap-2">
          <span className="material-symbols-outlined text-sm">tablet_mac</span>
          <span>شبیه‌ساز تبلت ({isPortrait ? 'عمودی ۷۶۸×۱۰۲۴' : 'افقی ۱۰۲۴×۷۶۸'})</span>
          <span className="px-2 py-0.5 rounded-full bg-blue-500/20 text-[10px] text-blue-300 font-bold">iPad / Android Tablet</span>
        </div>

        {/* Tablet Shell */}
        <div 
          className={`relative rounded-[36px] p-4 border-[12px] border-[#222326] shadow-[0_30px_90px_rgba(0,0,0,0.9),0_0_25px_rgba(212,175,55,0.15)] transition-all duration-300 ${
            isPortrait ? 'w-[768px] h-[980px] max-w-[95vw]' : 'w-[1024px] h-[720px] max-w-[98vw]'
          } bg-black flex flex-col overflow-hidden`}
        >
          {/* Tablet Screen Canvas */}
          <div className={`relative w-full h-full rounded-[24px] overflow-hidden flex flex-col ${
            isLuxury ? 'bg-black text-gray-100' : 'bg-[#f8f9ff] text-[#0b1c30]'
          }`}>
            
            {/* Tablet Status Bar */}
            <div className="h-8 w-full flex items-center justify-between px-6 z-50 flex-shrink-0 border-b border-white/5 bg-black/20 text-xs">
              <span className="font-mono text-xs font-bold text-gray-300">{currentTime}</span>

              {/* Tablet Front Camera Pin */}
              <div className="w-2.5 h-2.5 rounded-full bg-black border border-gray-700 flex items-center justify-center">
                <div className="w-1 h-1 rounded-full bg-blue-500/40" />
              </div>

              {/* Status Icons */}
              <div className="flex items-center gap-2 text-gray-300">
                <span className="font-mono text-[10px]">Wi-Fi 6</span>
                <span className="material-symbols-outlined text-xs">wifi</span>
                <span className="material-symbols-outlined text-sm text-emerald-400">battery_full</span>
              </div>
            </div>

            {/* Inner Content Area */}
            <div className="flex-1 w-full overflow-y-auto overflow-x-hidden relative custom-scrollbar flex flex-col">
              {children}
            </div>

            {/* Bottom Home Indicator Bar */}
            <div className="h-4 w-full flex items-center justify-center flex-shrink-0 z-50 select-none pointer-events-none">
              <div className="w-40 h-1 bg-gray-400/50 rounded-full" />
            </div>

          </div>
        </div>
      </div>
    );
  }

  // Desktop Simulator Frame (1280px with browser mockup chrome)
  if (deviceViewMode === 'desktop') {
    return (
      <div className="w-full min-h-screen py-16 px-4 flex flex-col items-center justify-center bg-[#07090e] transition-all duration-300">
        
        {/* Device metadata tag */}
        <div className="mb-3 text-xs text-amber-400/80 font-mono flex items-center gap-2">
          <span className="material-symbols-outlined text-sm">desktop_windows</span>
          <span>شبیه‌ساز مانیتور و دسکتاپ (۱۲۸۰×۸۶۰ پیکسل)</span>
          <span className="px-2 py-0.5 rounded-full bg-emerald-500/20 text-[10px] text-emerald-300 font-bold">Desktop Browser</span>
        </div>

        {/* Desktop Browser Window Frame */}
        <div className="w-full max-w-[1280px] h-[860px] max-h-[85vh] rounded-2xl border-2 border-[#353535] shadow-[0_30px_90px_rgba(0,0,0,0.9)] bg-[#121212] flex flex-col overflow-hidden">
          
          {/* Browser Titlebar & Window Controls */}
          <div className="h-10 w-full bg-[#1e1e1e] border-b border-[#333] flex items-center justify-between px-4 flex-shrink-0 select-none">
            {/* Traffic light buttons */}
            <div className="flex items-center gap-2">
              <div className="w-3 h-3 rounded-full bg-[#ff5f56] border border-[#e0443e]" />
              <div className="w-3 h-3 rounded-full bg-[#ffbd2e] border border-[#dea123]" />
              <div className="w-3 h-3 rounded-full bg-[#27c93f] border border-[#1aab29]" />
            </div>

            {/* Address Bar */}
            <div className="w-96 max-w-[60vw] h-6 rounded-lg bg-[#0e0e0e] border border-[#3a3a3a] px-3 flex items-center justify-between text-[11px] text-gray-300">
              <div className="flex items-center gap-1.5 text-emerald-400">
                <span className="material-symbols-outlined text-xs">lock</span>
                <span className="font-mono text-gray-200">https://ndn.post.ir/app</span>
              </div>
              <span className="text-[10px] text-gray-500 font-mono">TLS 1.3</span>
            </div>

            {/* Tabs / Tools */}
            <div className="flex items-center gap-2 text-gray-400">
              <span className="material-symbols-outlined text-sm">tune</span>
              <span className="material-symbols-outlined text-sm">refresh</span>
            </div>
          </div>

          {/* Desktop Content Canvas */}
          <div className={`flex-1 w-full overflow-y-auto overflow-x-hidden relative custom-scrollbar flex flex-col ${
            isLuxury ? 'bg-black text-gray-100' : 'bg-[#f8f9ff] text-[#0b1c30]'
          }`}>
            {children}
          </div>

        </div>
      </div>
    );
  }

  return <>{children}</>;
};
