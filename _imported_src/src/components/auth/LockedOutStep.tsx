import React from 'react';
import { useApp } from '../../context/AppContext';
import { SecurityBanner } from './SecurityBanner';

export const LockedOutStep: React.FC = () => {
  const { lockoutRemainingSeconds, setAuthStep, theme } = useApp();
  const isLuxury = theme === 'luxury';

  const formatTime = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div className="w-full max-w-md mx-auto space-y-5 py-6 animate-in zoom-in-95 duration-200">
      
      <SecurityBanner />

      <div className={`p-6 md:p-8 rounded-2xl border-2 border-red-500/60 shadow-2xl space-y-5 text-center ${
        isLuxury ? 'bg-[#120808]' : 'bg-red-50/70 border-red-300'
      }`}>

        <div className="w-16 h-16 rounded-2xl bg-red-500/20 border border-red-500/40 text-red-400 flex items-center justify-center mx-auto animate-pulse">
          <span className="material-symbols-outlined text-4xl">lock_clock</span>
        </div>

        <div className="space-y-1.5">
          <h2 className="text-xl font-extrabold text-red-400">
            حساب کاربری موقتاً مسدود شد
          </h2>
          <p className="text-xs text-gray-300 leading-relaxed">
            به دلیل ثبت ۴ مرتبه تلاش ناموفق در ورود، جهت جلوگیری از نفوذ غیرمجاز، حساب شما به مدت ۵ دقیقه قفل گردید.
          </p>
        </div>

        {/* Big Timer */}
        <div className="p-4 rounded-xl bg-black/60 border border-red-500/30 font-mono">
          <span className="text-[11px] text-gray-400 block mb-1">مدت زمان باقی‌مانده تا رفع قفل:</span>
          <span className="text-3xl font-bold text-red-400 tracking-widest">
            {formatTime(lockoutRemainingSeconds)}
          </span>
        </div>

        <div className="text-right space-y-2 text-xs text-gray-400 bg-white/5 p-3.5 rounded-xl border border-white/5">
          <div className="flex items-center gap-2 text-white font-semibold">
            <span className="material-symbols-outlined text-amber-400 text-sm">info</span>
            <span>راهنمای امنیتی NDN:</span>
          </div>
          <p className="text-[11px] leading-relaxed">
            پس از پایان زمان معکوس، فرم ورود به صورت خودکار باز خواهد شد. در صورت فراموشی اطلاعات، می‌توانید با پشتیبانی هاب تماس بگیرید.
          </p>
        </div>

        <div className="pt-2 flex flex-col gap-2">
          <button
            type="button"
            onClick={() => setAuthStep('password_recovery')}
            className={`w-full py-3 px-4 rounded-xl text-xs font-bold shadow transition-all flex items-center justify-center gap-2 ${
              isLuxury ? 'bg-[#d4af37] text-black hover:bg-[#e9c349]' : 'bg-[#1e3a8a] text-white hover:bg-[#00236f]'
            }`}
          >
            <span className="material-symbols-outlined text-base">support_agent</span>
            <span>بازیابی رمز عبور از طریق مدیر سیستم</span>
          </button>

          <button
            type="button"
            onClick={() => setAuthStep('panel_select')}
            className="w-full py-2 px-4 rounded-xl text-xs font-semibold bg-white/10 hover:bg-white/15 text-gray-300 transition-all"
          >
            بازگشت به انتخاب نوع پنل
          </button>
        </div>

      </div>

    </div>
  );
};
