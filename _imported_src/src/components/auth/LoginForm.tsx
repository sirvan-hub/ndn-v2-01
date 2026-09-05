import React, { useState, useEffect } from 'react';
import { useApp } from '../../context/AppContext';
import { SecurityBanner } from './SecurityBanner';

export const LoginForm: React.FC = () => {
  const { loginUser, selectedPanel, setAuthStep, failedAttempts, theme } = useApp();
  const isLuxury = theme === 'luxury';

  const [phone, setPhone] = useState(
    selectedPanel === 'hub_manager' ? '09129876543' : selectedPanel === 'courier' ? '09121112233' : '09123456789'
  );
  const [password, setPassword] = useState('123');
  const [captchaCode, setCaptchaCode] = useState('');
  const [enteredCaptcha, setEnteredCaptcha] = useState('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Generate random 4-character captcha
  const generateCaptcha = () => {
    const chars = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
    let code = '';
    for (let i = 0; i < 4; i++) {
      code += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    setCaptchaCode(code);
    setEnteredCaptcha('');
  };

  useEffect(() => {
    generateCaptcha();
  }, []);

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    const result = loginUser(phone, password, enteredCaptcha, captchaCode);
    if (!result.success && result.error) {
      setErrorMessage(result.error);
      generateCaptcha();
    }
  };

  return (
    <div className="w-full max-w-md mx-auto space-y-5 py-4 animate-in fade-in duration-200">
      
      {/* Header */}
      <div className="flex items-center justify-between border-b pb-3 border-gray-700/30">
        <div className="text-right">
          <h2 className={`text-xl font-bold ${isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'}`}>
            ورود به سامانه NDN
          </h2>
          <p className="text-xs text-gray-400 mt-0.5">
            {selectedPanel === 'hub_manager' ? 'ورود به پنل مدیریت هاب' : selectedPanel === 'courier' ? 'ورود به پنل مامور توزیع (PuDo)' : 'ورود به پنل مشتری محله'}
          </p>
        </div>
        <button
          onClick={() => setAuthStep('auth_action_select')}
          className="p-2 rounded-xl text-gray-400 hover:text-white hover:bg-white/5 transition-colors"
          title="بازگشت"
        >
          <span className="material-symbols-outlined text-xl">arrow_forward</span>
        </button>
      </div>

      <SecurityBanner />

      {/* Error Alert / Failed Attempts Notice */}
      {errorMessage && (
        <div className="p-3.5 rounded-xl bg-red-950/60 border border-red-500/50 text-red-200 text-xs flex items-start gap-2.5 shadow-lg animate-shake">
          <span className="material-symbols-outlined text-red-400 text-base flex-shrink-0 mt-0.5">error</span>
          <div className="text-right leading-relaxed">
            <p className="font-bold">{errorMessage}</p>
            {failedAttempts > 0 && failedAttempts < 4 && (
              <p className="text-[11px] text-red-300 mt-0.5">
                تعداد تلاش‌های ناموفق: {failedAttempts} از ۴
              </p>
            )}
          </div>
        </div>
      )}

      <form onSubmit={handleLogin} className={`p-6 md:p-8 rounded-2xl border shadow-2xl space-y-4 text-right ${
        isLuxury ? 'bg-[#0e0e0e] border-[#353535]' : 'bg-white border-gray-200'
      }`}>

        {/* Mobile Number */}
        <div>
          <label className="block text-xs font-semibold text-gray-300 mb-1.5">
            شماره تماس (نام کاربری) <span className="text-red-400">*</span>
          </label>
          <div className="relative">
            <input
              type="tel"
              required
              dir="ltr"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="09123456789"
              className={`w-full px-3.5 py-2.5 pl-10 rounded-xl border text-xs font-mono outline-none ${
                isLuxury ? 'bg-[#181818] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900 focus:border-blue-500'
              }`}
            />
            <span className="material-symbols-outlined absolute left-3 top-2.5 text-gray-400 text-base">
              phone_iphone
            </span>
          </div>
        </div>

        {/* Password */}
        <div>
          <div className="flex justify-between items-center mb-1.5">
            <label className="text-xs font-semibold text-gray-300">
              رمز ورود <span className="text-red-400">*</span>
            </label>
            <button
              type="button"
              onClick={() => setAuthStep('password_recovery')}
              className="text-[10px] text-amber-400 hover:underline flex items-center gap-0.5"
            >
              <span className="material-symbols-outlined text-xs">help</span>
              <span>فراموشی و بازیابی رمز عبور؟</span>
            </button>
          </div>
          <div className="relative">
            <input
              type="password"
              required
              dir="ltr"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="رمز عبور خود را وارد کنید"
              className={`w-full px-3.5 py-2.5 pl-10 rounded-xl border text-xs font-mono outline-none ${
                isLuxury ? 'bg-[#181818] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900 focus:border-blue-500'
              }`}
            />
            <span className="material-symbols-outlined absolute left-3 top-2.5 text-gray-400 text-base">
              lock
            </span>
          </div>
        </div>

        {/* Security Captcha Code */}
        <div>
          <label className="block text-xs font-semibold text-gray-300 mb-1.5">
            کد امنیتی (کپچا) <span className="text-red-400">*</span>
          </label>
          <div className="flex items-center gap-2">
            <input
              type="text"
              required
              maxLength={4}
              dir="ltr"
              value={enteredCaptcha}
              onChange={(e) => setEnteredCaptcha(e.target.value)}
              placeholder="کد تصویر"
              className={`w-28 px-3.5 py-2.5 rounded-xl border text-xs font-mono tracking-widest text-center uppercase outline-none ${
                isLuxury ? 'bg-[#181818] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900 focus:border-blue-500'
              }`}
            />

            {/* Visual Styled Captcha Box */}
            <div className="flex-1 h-10 rounded-xl bg-gradient-to-r from-amber-500/20 via-orange-500/20 to-amber-500/20 border border-amber-500/40 flex items-center justify-center select-none relative overflow-hidden">
              {/* Noise lines */}
              <div className="absolute inset-0 bg-[repeating-linear-gradient(45deg,transparent,transparent_4px,rgba(255,255,255,0.05)_4px,rgba(255,255,255,0.05)_8px)] pointer-events-none"></div>
              <span className="font-mono text-base font-bold text-amber-300 tracking-widest drop-shadow italic select-none">
                {captchaCode}
              </span>
            </div>

            {/* Refresh Captcha Button */}
            <button
              type="button"
              onClick={generateCaptcha}
              className="p-2.5 rounded-xl border border-white/10 bg-white/5 text-gray-400 hover:text-white hover:bg-white/10 transition-colors"
              title="تغییر کد امنیتی"
            >
              <span className="material-symbols-outlined text-base">refresh</span>
            </button>
          </div>
          <span className="text-[10px] text-gray-500 block mt-1">
            جهت جلوگیری از ورود ربات‌ها و حملات خودکار
          </span>
        </div>

        {/* Failed Attempt Warning Notice */}
        <div className="p-2.5 rounded-lg bg-amber-400/5 border border-amber-400/20 text-[11px] text-amber-300 flex items-center gap-1.5">
          <span className="material-symbols-outlined text-sm flex-shrink-0">gshield</span>
          <span>توجه: پس از ۴ بار اشتباه وارد کردن رمز، حساب به مدت ۵ دقیقه قفل می‌شود.</span>
        </div>

        {/* Submit */}
        <div className="pt-2">
          <button
            type="submit"
            className={`w-full py-3.5 px-4 rounded-xl font-bold text-xs shadow-lg transition-all active:scale-95 flex items-center justify-center gap-2 ${
              isLuxury 
                ? 'bg-[#d4af37] text-black hover:bg-[#e9c349]' 
                : 'bg-[#1e3a8a] text-white hover:bg-[#00236f]'
            }`}
          >
            <span>بررسی اطلاعات و دریافت تایید دو مرحله‌ای (۲FA)</span>
            <span className="material-symbols-outlined text-base">arrow_back</span>
          </button>
        </div>

        {/* Quick Demo Switcher helper */}
        <div className="pt-3 border-t border-gray-800 text-center">
          <span className="text-[10px] text-gray-400 block mb-1.5">حساب‌های نمونه آزمایشی:</span>
          <div className="flex flex-wrap justify-center gap-1.5">
            <button
              type="button"
              onClick={() => { setPhone('09123456789'); setPassword('123'); }}
              className="px-2 py-1 rounded bg-white/5 hover:bg-white/10 text-[10px] text-gray-300 border border-white/5"
            >
              مشتری (09123456789)
            </button>
            <button
              type="button"
              onClick={() => { setPhone('09129876543'); setPassword('123'); }}
              className="px-2 py-1 rounded bg-white/5 hover:bg-white/10 text-[10px] text-gray-300 border border-white/5"
            >
              هاب (09129876543)
            </button>
            <button
              type="button"
              onClick={() => { setPhone('09121112233'); setPassword('123'); }}
              className="px-2 py-1 rounded bg-white/5 hover:bg-white/10 text-[10px] text-gray-300 border border-white/5"
            >
              سفیر (09121112233)
            </button>
          </div>
        </div>

      </form>

    </div>
  );
};
