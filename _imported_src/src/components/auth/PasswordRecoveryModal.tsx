import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import { SecurityBanner } from './SecurityBanner';

export const PasswordRecoveryModal: React.FC = () => {
  const { 
    setAuthStep, 
    systemAdminContact, 
    requestPasswordRecovery, 
    resetPasswordWithCode,
    latestRecoveryCode,
    activeRecoveryUser,
    theme 
  } = useApp();

  const isLuxury = theme === 'luxury';

  const [identifier, setIdentifier] = useState('');
  const [stage, setStage] = useState<'request' | 'verify'>('request');
  const [enteredCode, setEnteredCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleRequestCode = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setStatusMessage(null);

    const res = requestPasswordRecovery(identifier);
    if (res.success) {
      setStatusMessage(res.message);
      setStage('verify');
    } else {
      setErrorMessage(res.message);
    }
  };

  const handleResetPassword = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (newPassword.length < 4) {
      setErrorMessage('رمز عبور جدید باید حداقل ۴ کاراکتر باشد.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setErrorMessage('رمز عبور جدید و تکرار آن یکسان نیستند.');
      return;
    }

    const success = resetPasswordWithCode(identifier, enteredCode, newPassword);
    if (!success) {
      setErrorMessage('کد تایید نامعتبر است یا منقضی شده است.');
    }
  };

  return (
    <div className="w-full max-w-md mx-auto space-y-5 py-4 animate-in fade-in duration-200">
      
      {/* Header */}
      <div className="flex items-center justify-between border-b pb-3 border-gray-700/30">
        <div className="text-right">
          <h2 className={`text-xl font-bold ${isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'}`}>
            بازیابی و تغییر رمز عبور
          </h2>
          <p className="text-xs text-gray-400 mt-0.5">
            ارسال کد تایید به شماره کاربر و مدیر ارشد سیستم
          </p>
        </div>
        <button
          onClick={() => setAuthStep('login_form')}
          className="p-2 rounded-xl text-gray-400 hover:text-white hover:bg-white/5 transition-colors"
          title="بازگشت به فرم ورود"
        >
          <span className="material-symbols-outlined text-xl">arrow_forward</span>
        </button>
      </div>

      <SecurityBanner />

      {/* System Admin Emergency Contact Box */}
      <div className={`p-4 rounded-xl border text-xs space-y-2 ${
        isLuxury ? 'bg-[#141414] border-amber-400/30' : 'bg-blue-50 border-blue-200'
      }`}>
        <div className="flex items-center gap-2 font-bold text-amber-400">
          <span className="material-symbols-outlined text-base">support_agent</span>
          <span>اطلاعات تماس مدیر ارشد سامانه جهت بازیابی اضطراری:</span>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-[11px] pt-1">
          <div className="flex items-center gap-1.5 text-gray-300">
            <span className="text-gray-400">تلفن مستقیم:</span>
            <span className="font-mono text-white font-bold">{systemAdminContact.primaryPhone}</span>
          </div>
          <div className="flex items-center gap-1.5 text-gray-300">
            <span className="text-gray-400">ایمیل مدیر:</span>
            <span className="font-mono text-amber-300">{systemAdminContact.primaryEmail}</span>
          </div>
        </div>
      </div>

      {/* Error / Status alerts */}
      {errorMessage && (
        <div className="p-3 rounded-xl bg-red-950/60 border border-red-500/50 text-red-200 text-xs flex items-center gap-2 shadow-lg animate-shake">
          <span className="material-symbols-outlined text-red-400 text-base">error</span>
          <span>{errorMessage}</span>
        </div>
      )}

      {statusMessage && (
        <div className="p-3 rounded-xl bg-emerald-950/60 border border-emerald-500/50 text-emerald-200 text-xs flex items-center gap-2 shadow-lg">
          <span className="material-symbols-outlined text-emerald-400 text-base">check_circle</span>
          <span className="leading-relaxed">{statusMessage}</span>
        </div>
      )}

      {/* Stage 1: Request Code */}
      {stage === 'request' && (
        <form onSubmit={handleRequestCode} className={`p-6 rounded-2xl border shadow-2xl space-y-4 text-right ${
          isLuxury ? 'bg-[#0e0e0e] border-[#353535]' : 'bg-white border-gray-200'
        }`}>
          <div>
            <label className="block text-xs font-semibold text-gray-300 mb-1.5">
              شماره موبایل یا ایمیل حساب کاربری <span className="text-red-400">*</span>
            </label>
            <div className="relative">
              <input
                type="text"
                required
                dir="ltr"
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value)}
                placeholder="09123456789 یا user@example.com"
                className={`w-full px-3.5 py-2.5 pl-10 rounded-xl border text-xs font-mono outline-none ${
                  isLuxury ? 'bg-[#181818] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900 focus:border-blue-500'
                }`}
              />
              <span className="material-symbols-outlined absolute left-3 top-2.5 text-gray-400 text-base">
                contact_mail
              </span>
            </div>
            <span className="text-[10px] text-gray-500 block mt-1">
              کد اعتبارسنجی هم‌زمان برای شما و ایمیل مدیر سیستم ارسال خواهد شد.
            </span>
          </div>

          <div className="pt-2">
            <button
              type="submit"
              className={`w-full py-3.5 px-4 rounded-xl font-bold text-xs shadow-lg transition-all active:scale-95 flex items-center justify-center gap-2 ${
                isLuxury ? 'bg-[#d4af37] text-black hover:bg-[#e9c349]' : 'bg-[#1e3a8a] text-white hover:bg-[#00236f]'
              }`}
            >
              <span>ارسال کد تایید بازیابی رمز</span>
              <span className="material-symbols-outlined text-base">send</span>
            </button>
          </div>
        </form>
      )}

      {/* Stage 2: Verify Code & Set New Password */}
      {stage === 'verify' && (
        <form onSubmit={handleResetPassword} className={`p-6 rounded-2xl border shadow-2xl space-y-4 text-right ${
          isLuxury ? 'bg-[#0e0e0e] border-[#353535]' : 'bg-white border-gray-200'
        }`}>
          <div>
            <div className="flex justify-between items-center mb-1">
              <label className="text-xs font-semibold text-gray-300">
                کد تایید ۴ رقمی ارسال شده <span className="text-red-400">*</span>
              </label>
              {latestRecoveryCode && (
                <button
                  type="button"
                  onClick={() => setEnteredCode(latestRecoveryCode)}
                  className="text-[10px] text-amber-400 underline font-mono"
                >
                  درج خودکار کد ({latestRecoveryCode})
                </button>
              )}
            </div>
            <input
              type="text"
              required
              maxLength={6}
              dir="ltr"
              value={enteredCode}
              onChange={(e) => setEnteredCode(e.target.value)}
              placeholder="1234"
              className={`w-full py-2.5 px-3.5 rounded-xl border text-center text-base font-mono tracking-widest font-bold outline-none ${
                isLuxury ? 'bg-[#181818] border-amber-400/50 text-amber-300' : 'bg-gray-50 border-blue-400 text-blue-900'
              }`}
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-300 mb-1.5">
              رمز عبور جدید <span className="text-red-400">*</span>
            </label>
            <input
              type="password"
              required
              dir="ltr"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="رمز عبور جدید"
              className={`w-full px-3.5 py-2.5 rounded-xl border text-xs font-mono outline-none ${
                isLuxury ? 'bg-[#181818] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900'
              }`}
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-300 mb-1.5">
              تکرار رمز عبور جدید <span className="text-red-400">*</span>
            </label>
            <input
              type="password"
              required
              dir="ltr"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="تکرار مجدد رمز عبور"
              className={`w-full px-3.5 py-2.5 rounded-xl border text-xs font-mono outline-none ${
                isLuxury ? 'bg-[#181818] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900'
              }`}
            />
          </div>

          <div className="pt-2">
            <button
              type="submit"
              className={`w-full py-3.5 px-4 rounded-xl font-bold text-xs shadow-lg transition-all active:scale-95 flex items-center justify-center gap-2 ${
                isLuxury ? 'bg-[#d4af37] text-black hover:bg-[#e9c349]' : 'bg-[#1e3a8a] text-white hover:bg-[#00236f]'
              }`}
            >
              <span>ذخیره رمز عبور جدید و ورود</span>
              <span className="material-symbols-outlined text-base">lock_reset</span>
            </button>
          </div>

          <div className="text-center pt-2">
            <button
              type="button"
              onClick={() => setStage('request')}
              className="text-[11px] text-gray-400 hover:text-white"
            >
              تغییر شماره / ارسال مجدد
            </button>
          </div>
        </form>
      )}

    </div>
  );
};
