import React from 'react';
import { useApp } from '../../context/AppContext';

export const SecurityBanner: React.FC = () => {
  const { theme, sessionRemainingSeconds, isAuthenticated, resetSessionTimer } = useApp();
  const isLuxury = theme === 'luxury';

  const formatTime = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div className="w-full space-y-2 text-xs">
      {/* HTTPS & Encryption Security Badge */}
      <div className={`p-3 rounded-xl border flex flex-wrap items-center justify-between gap-2 shadow-sm ${
        isLuxury ? 'bg-[#141414] border-white/10 text-gray-300' : 'bg-blue-50/70 border-blue-200 text-blue-950'
      }`}>
        <div className="flex items-center gap-2">
          <span className="material-symbols-outlined text-emerald-400 text-lg">lock</span>
          <div>
            <span className="font-bold text-white block">پروتکل امنیتی HTTPS و رمزنگاری اطلاعات</span>
            <span className="text-[10px] text-gray-400">اطلاعات شما با استاندارد TLS 1.3 رمزگذاری و محافظت می‌شود.</span>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <div className="flex items-center gap-1 text-[11px] font-mono text-emerald-400 bg-emerald-500/10 px-2.5 py-1 rounded-full border border-emerald-500/20">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping"></span>
            <span>SSL Active</span>
          </div>

          {isAuthenticated && (
            <div className="flex items-center gap-1.5 text-[11px] font-mono text-amber-400 bg-amber-400/10 px-2.5 py-1 rounded-full border border-amber-400/30">
              <span className="material-symbols-outlined text-xs">timer</span>
              <span>انقضای نشست: {formatTime(sessionRemainingSeconds)}</span>
              <button 
                onClick={resetSessionTimer} 
                className="text-[10px] underline hover:text-white mr-1"
                title="تمدید نشست کاری"
              >
                تمدید
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
