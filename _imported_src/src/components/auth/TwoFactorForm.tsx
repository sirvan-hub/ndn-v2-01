import React, { useState, useEffect } from 'react';
import { useApp } from '../../context/AppContext';
import { SecurityBanner } from './SecurityBanner';

export const TwoFactorForm: React.FC = () => {
  const { pendingUserFor2FA, verify2FA, setAuthStep, theme } = useApp();
  const isLuxury = theme === 'luxury';

  const [otpCode, setOtpCode] = useState('');
  const [timerSeconds, setTimerSeconds] = useState(60);
  const [canResend, setCanResend] = useState(false);
  const [isVerifyingBiometric, setIsVerifyingBiometric] = useState(false);

  useEffect(() => {
    if (timerSeconds <= 0) {
      setCanResend(true);
      return;
    }
    const interval = setInterval(() => {
      setTimerSeconds((prev) => prev - 1);
    }, 1000);
    return () => clearInterval(interval);
  }, [timerSeconds]);

  const handleResend = () => {
    setTimerSeconds(60);
    setCanResend(false);
    setOtpCode('');
  };

  const handleOtpSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    verify2FA(otpCode);
  };

  const handleBiometricAuth = () => {
    setIsVerifyingBiometric(true);
    setTimeout(() => {
      setIsVerifyingBiometric(false);
      verify2FA('1234', true);
    }, 1200);
  };

  return (
    <div className="w-full max-w-md mx-auto space-y-5 py-4 animate-in fade-in duration-200">
      
      {/* Header */}
      <div className="flex items-center justify-between border-b pb-3 border-gray-700/30">
        <div className="text-right">
          <h2 className={`text-xl font-bold ${isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'}`}>
            احراز هویت دو مرحله‌ای (2FA)
          </h2>
          <p className="text-xs text-gray-400 mt-0.5">
            تایید هویت امن دومرحله‌ای جهت افزایش ضریب امنیت حساب
          </p>
        </div>
        <button
          onClick={() => setAuthStep('login_form')}
          className="p-2 rounded-xl text-gray-400 hover:text-white hover:bg-white/5 transition-colors"
          title="بازگشت"
        >
          <span className="material-symbols-outlined text-xl">arrow_forward</span>
        </button>
      </div>

      <SecurityBanner />

      <div className={`p-6 md:p-8 rounded-2xl border shadow-2xl space-y-5 text-right ${
        isLuxury ? 'bg-[#0e0e0e] border-[#353535]' : 'bg-white border-gray-200'
      }`}>

        {/* User Identity Preview */}
        <div className="p-3 rounded-xl bg-white/5 border border-white/10 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-full bg-amber-400/20 text-amber-400 flex items-center justify-center font-bold">
              <span className="material-symbols-outlined text-xl">verified_user</span>
            </div>
            <div>
              <span className="text-xs font-bold text-white block">
                {pendingUserFor2FA?.fullName || 'کاربر گرامی'}
              </span>
              <span className="text-[10px] text-gray-400 font-mono">
                {pendingUserFor2FA?.phone}
              </span>
            </div>
          </div>
          <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
            مرحله نهایی
          </span>
        </div>

        {/* OTP Input Form */}
        <form onSubmit={handleOtpSubmit} className="space-y-4">
          <div>
            <div className="flex justify-between items-center mb-1.5">
              <label className="text-xs font-semibold text-gray-300">
                کد تایید ۴ رقمی پیامک شده <span className="text-red-400">*</span>
              </label>
              <span className="text-[10px] text-gray-400">
                کد تستی پیش‌فرض: <code className="text-amber-400 font-mono">1234</code>
              </span>
            </div>
            <input
              type="text"
              required
              maxLength={6}
              dir="ltr"
              autoFocus
              value={otpCode}
              onChange={(e) => setOtpCode(e.target.value)}
              placeholder="1 2 3 4"
              className={`w-full py-3 px-4 rounded-xl border text-center text-lg font-mono tracking-[0.5em] font-bold outline-none ${
                isLuxury ? 'bg-[#181818] border-amber-400/50 text-amber-300 focus:border-[#d4af37]' : 'bg-gray-50 border-blue-400 text-blue-900 focus:border-blue-600'
              }`}
            />
          </div>

          <div className="flex items-center justify-between text-xs">
            {canResend ? (
              <button
                type="button"
                onClick={handleResend}
                className="text-amber-400 hover:underline flex items-center gap-1 font-semibold"
              >
                <span className="material-symbols-outlined text-sm">replay</span>
                <span>ارسال مجدد کد تایید</span>
              </button>
            ) : (
              <span className="text-gray-400 flex items-center gap-1 text-[11px]">
                <span className="material-symbols-outlined text-xs">timer</span>
                <span>ارسال مجدد تا {timerSeconds} ثانیه دیگر</span>
              </span>
            )}

            <button
              type="button"
              onClick={() => setOtpCode('1234')}
              className="text-[11px] text-gray-400 hover:text-white underline"
            >
              درج خودکار کد (1234)
            </button>
          </div>

          <button
            type="submit"
            className={`w-full py-3.5 px-4 rounded-xl font-bold text-xs shadow-lg transition-all active:scale-95 flex items-center justify-center gap-2 ${
              isLuxury 
                ? 'bg-[#d4af37] text-black hover:bg-[#e9c349]' 
                : 'bg-[#1e3a8a] text-white hover:bg-[#00236f]'
            }`}
          >
            <span className="material-symbols-outlined text-base">lock_open</span>
            <span>تایید کد و ورود نهایی به پنل</span>
          </button>
        </form>

        {/* Biometric Option */}
        <div className="pt-3 border-t border-gray-800 text-center space-y-2">
          <span className="text-[11px] text-gray-400 block">یا استفاده از احراز هویت بیومتریک:</span>
          <button
            type="button"
            onClick={handleBiometricAuth}
            disabled={isVerifyingBiometric}
            className={`w-full py-2.5 px-3 rounded-xl border flex items-center justify-center gap-2 text-xs transition-all ${
              isVerifyingBiometric
                ? 'bg-amber-400/20 border-amber-400 text-amber-300 animate-pulse'
                : 'bg-white/5 border-white/10 hover:bg-white/10 text-gray-200'
            }`}
          >
            <span className="material-symbols-outlined text-lg text-emerald-400">fingerprint</span>
            <span>
              {isVerifyingBiometric ? 'در حال اسکن اثر انگشت / چهره...' : 'ورود سریع با اثر انگشت / فیس‌آیدی'}
            </span>
          </button>
        </div>

      </div>

    </div>
  );
};
