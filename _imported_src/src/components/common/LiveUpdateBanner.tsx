import React from 'react';
import { useApp } from '../../context/AppContext';

export const LiveUpdateBanner: React.FC = () => {
  const {
    isNewPackageRequestPending,
    dismissNewPackageRequest,
    acceptNewPackageRequest,
    theme
  } = useApp();

  const isLuxury = theme === 'luxury';

  return (
    <div className="flex flex-col gap-3 w-full">
      {/* Interactive New Package Registration Request Dialog (Matching Screenshot 2 & 3) */}
      {isNewPackageRequestPending && (
        <div className={`p-4 rounded-xl border-2 flex flex-col gap-3 transition-all animate-in fade-in slide-in-from-top-4 duration-300 ${
          isLuxury
            ? 'bg-[#0e0e0e] border-[#d4af37] shadow-[0_0_15px_rgba(212,175,55,0.25)] text-gray-100'
            : 'bg-white border-[#1e3a8a] shadow-lg text-gray-900'
        }`}>
          <div className="flex items-center gap-3">
            <div className={`p-2 rounded-xl flex items-center justify-center ${
              isLuxury ? 'bg-amber-400/10 text-[#d4af37]' : 'bg-blue-100 text-blue-800'
            }`}>
              <span className="material-symbols-outlined text-2xl">pending_actions</span>
            </div>
            <div className="flex flex-col text-right">
              <h3 className={`text-sm md:text-base font-bold ${isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'}`}>
                درخواست ثبت بسته جدید
              </h3>
              <p className="text-xs text-gray-400 mt-0.5">
                مامور پست (کد سفیر: ۱۲۳) در حال ثبت یک بسته جدید برای شماست.
              </p>
            </div>
          </div>

          <div className="flex gap-2 mt-1">
            <button
              onClick={acceptNewPackageRequest}
              className={`flex-1 py-2 px-3 rounded-lg font-bold text-xs shadow-md transition-all active:scale-95 ${
                isLuxury
                  ? 'bg-[#d4af37] text-black hover:bg-[#e9c349]'
                  : 'bg-[#1e3a8a] text-white hover:bg-[#00236f]'
              }`}
            >
              تایید و پرداخت اولیه
            </button>
            <button
              onClick={dismissNewPackageRequest}
              className={`flex-1 py-2 px-3 rounded-lg text-xs border transition-colors ${
                isLuxury
                  ? 'border-gray-700 hover:bg-white/5 text-gray-400'
                  : 'border-gray-300 hover:bg-gray-100 text-gray-600'
              }`}
            >
              رد درخواست
            </button>
          </div>
        </div>
      )}

      {/* Live Server Pulse Pill */}
      <div className={`p-3 rounded-xl border flex items-center justify-between shadow-sm transition-all ${
        isLuxury
          ? 'bg-[#1f1f1f] border-white/10 text-gray-200'
          : 'bg-[#e5eeff] border-blue-200 text-[#0b1c30]'
      }`}>
        <div className="flex items-center gap-2.5">
          <span className={`material-symbols-outlined text-xl animate-pulse ${
            isLuxury ? 'text-[#f2ca50]' : 'text-blue-600'
          }`}>
            sensors
          </span>
          <div className="flex flex-col text-right">
            <p className={`text-xs font-semibold ${isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'}`}>
              به‌روزرسانی زنده: بسته شما توسط مامور به هاب محله تحویل شد
            </p>
            <p className="text-[10px] text-gray-400">
              در حال دریافت اطلاعات از سرور و شبکه PUDO...
            </p>
          </div>
        </div>

        <div className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
        </div>
      </div>
    </div>
  );
};
