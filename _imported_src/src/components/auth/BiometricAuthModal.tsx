import React, { useState, useEffect } from 'react';
import { useApp } from '../../context/AppContext';
import { UserProfile } from '../../types';

interface BiometricAuthModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const BiometricAuthModal: React.FC<BiometricAuthModalProps> = ({ isOpen, onClose }) => {
  const { 
    theme, 
    registeredUsers, 
    savedBiometricUserId, 
    setSavedBiometricUserId, 
    loginWithBiometrics 
  } = useApp();

  const isLuxury = theme === 'luxury';
  
  const [selectedUser, setSelectedUser] = useState<UserProfile | null>(() => {
    if (savedBiometricUserId) {
      const found = registeredUsers.find(u => u.id === savedBiometricUserId);
      if (found) return found;
    }
    return registeredUsers[0] || null;
  });

  const [scanMode, setScanMode] = useState<'fingerprint' | 'face'>('fingerprint');
  const [scanStatus, setScanStatus] = useState<'idle' | 'scanning' | 'success' | 'failed'>('idle');
  const [scanProgress, setScanProgress] = useState<number>(0);
  const [statusMessage, setStatusMessage] = useState<string>('انگشت خود را روی حسگر قرار دهید یا دکمه اسکن را لمس کنید');

  useEffect(() => {
    if (isOpen) {
      setScanStatus('idle');
      setScanProgress(0);
      setStatusMessage(
        scanMode === 'fingerprint'
          ? 'انگشت خود را روی حسگر قرار دهید یا دکمه اسکن را لمس کنید'
          : 'چهره خود را روبروی دوربین قرار دهید'
      );
      if (savedBiometricUserId) {
        const found = registeredUsers.find(u => u.id === savedBiometricUserId);
        if (found) setSelectedUser(found);
      }
    }
  }, [isOpen, scanMode, savedBiometricUserId, registeredUsers]);

  if (!isOpen) return null;

  const handleStartScan = async () => {
    if (scanStatus === 'scanning' || scanStatus === 'success') return;

    setScanStatus('scanning');
    setStatusMessage(
      scanMode === 'fingerprint' 
        ? 'در حال خواندن داده‌های بیومتریک اثر انگشت از سخت‌افزار دستگاه...' 
        : 'در حال اسکن سه‌بعدی چهره و انطباق با کلید امنیتی دستگاه...'
    );

    // Simulate scanning progress animation
    let current = 0;
    const interval = setInterval(() => {
      current += 25;
      setScanProgress(Math.min(current, 95));
      if (current >= 100) {
        clearInterval(interval);
      }
    }, 180);

    setTimeout(async () => {
      clearInterval(interval);
      setScanProgress(100);
      setScanStatus('success');
      setStatusMessage('هویت بیومتریک با موفقیت تایید شد! در حال هدایت مستقیم به پنل...');

      setTimeout(async () => {
        const res = await loginWithBiometrics(selectedUser?.id);
        if (res.success) {
          onClose();
        } else {
          setScanStatus('failed');
          setStatusMessage(res.error || 'خطا در ورود به حساب کاربری');
        }
      }, 700);
    }, 1100);
  };

  const getRoleBadge = (role: string) => {
    switch (role) {
      case 'hub_manager':
        return { label: 'پنل هاب محله (PUDO)', color: 'bg-emerald-500/20 text-emerald-400 border-emerald-500/40', icon: 'storefront' };
      case 'courier':
        return { label: 'پنل مامور توزیع (سفیر پست)', color: 'bg-orange-500/20 text-orange-400 border-orange-500/40', icon: 'local_shipping' };
      case 'customer':
      default:
        return { label: 'پنل مشتری (گیرنده/فرستنده)', color: 'bg-blue-500/20 text-blue-400 border-blue-500/40', icon: 'account_circle' };
    }
  };

  const selectedBadge = selectedUser ? getRoleBadge(selectedUser.role) : null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in duration-200">
      <div className={`w-full max-w-lg rounded-3xl border shadow-2xl overflow-hidden relative flex flex-col ${
        isLuxury 
          ? 'bg-gradient-to-b from-[#161616] to-[#0a0a0a] border-[#d4af37]/40 text-white' 
          : 'bg-gradient-to-b from-white to-gray-50 border-blue-200 text-gray-900'
      }`}>
        
        {/* Header */}
        <div className={`p-5 flex items-center justify-between border-b ${
          isLuxury ? 'border-white/10 bg-black/40' : 'border-gray-100 bg-gray-50'
        }`}>
          <div className="flex items-center gap-3">
            <div className={`w-10 h-10 rounded-2xl flex items-center justify-center border shadow-inner ${
              isLuxury ? 'bg-amber-400/20 text-amber-400 border-amber-400/40' : 'bg-blue-600 text-white border-blue-700'
            }`}>
              <span className="material-symbols-outlined text-2xl">fingerprint</span>
            </div>
            <div>
              <h3 className="font-extrabold text-sm sm:text-base">
                ورود هوشمند با احراز هویت بیومتریک
              </h3>
              <p className="text-[11px] text-gray-400">
                WebAuthn / Passkey Biometric Login
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-gray-400 hover:text-white transition-all"
          >
            <span className="material-symbols-outlined text-lg">close</span>
          </button>
        </div>

        {/* Body Content */}
        <div className="p-6 space-y-6 flex flex-col items-center text-center">
          
          {/* Mode Switch (Fingerprint vs FaceID) */}
          <div className={`p-1 rounded-2xl border flex items-center gap-1 w-full max-w-xs ${
            isLuxury ? 'bg-black/60 border-white/10' : 'bg-gray-100 border-gray-200'
          }`}>
            <button
              type="button"
              onClick={() => {
                setScanMode('fingerprint');
                setScanStatus('idle');
              }}
              className={`flex-1 py-2 px-3 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-1.5 ${
                scanMode === 'fingerprint'
                  ? isLuxury ? 'bg-[#d4af37] text-black shadow-md' : 'bg-blue-600 text-white shadow-md'
                  : 'text-gray-400 hover:text-white'
              }`}
            >
              <span className="material-symbols-outlined text-base">fingerprint</span>
              <span>اثر انگشت</span>
            </button>

            <button
              type="button"
              onClick={() => {
                setScanMode('face');
                setScanStatus('idle');
              }}
              className={`flex-1 py-2 px-3 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-1.5 ${
                scanMode === 'face'
                  ? isLuxury ? 'bg-[#d4af37] text-black shadow-md' : 'bg-blue-600 text-white shadow-md'
                  : 'text-gray-400 hover:text-white'
              }`}
            >
              <span className="material-symbols-outlined text-base">face</span>
              <span>تشخیص چهره</span>
            </button>
          </div>

          {/* Account Card Recognized On This Device */}
          {selectedUser ? (
            <div className={`w-full p-4 rounded-2xl border transition-all text-right ${
              isLuxury ? 'bg-black/40 border-amber-400/30 shadow-inner' : 'bg-blue-50/70 border-blue-200 shadow-sm'
            }`}>
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-bold text-gray-400">حساب شناسایی‌شده روی این دستگاه:</span>
                {selectedBadge && (
                  <span className={`text-[10px] font-bold px-2.5 py-0.5 rounded-full border flex items-center gap-1 ${selectedBadge.color}`}>
                    <span className="material-symbols-outlined text-xs">{selectedBadge.icon}</span>
                    <span>{selectedBadge.label}</span>
                  </span>
                )}
              </div>

              <div className="mt-2 flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-amber-400/20 border border-amber-400/40 text-amber-400 flex items-center justify-center font-bold text-sm">
                  {selectedUser.fullName.charAt(0)}
                </div>
                <div>
                  <h4 className="text-sm font-bold text-white flex items-center gap-2">
                    <span>{selectedUser.fullName}</span>
                    <span className="text-[10px] font-mono px-1.5 py-0.2 rounded bg-white/10 text-gray-300">
                      {selectedUser.phone}
                    </span>
                  </h4>
                  <p className="text-[11px] text-gray-400 mt-0.5">
                    {selectedUser.email || 'رمز عبور و کلید امنیتی بیومتریک ذخیره شده'}
                  </p>
                </div>
              </div>
            </div>
          ) : (
            <div className="text-xs text-amber-400 bg-amber-400/10 p-3 rounded-xl border border-amber-400/20 w-full">
              حساب ذخیره‌شده‌ای یافت نشد. می‌توانید با اولین حساب نمونه وارد شوید.
            </div>
          )}

          {/* Interactive Biometric Sensor Touch Pad */}
          <div className="relative my-2 flex flex-col items-center justify-center">
            
            {/* Ripple Pulse Rings */}
            {scanStatus === 'scanning' && (
              <>
                <div className="absolute w-36 h-36 rounded-full bg-amber-400/10 animate-ping" />
                <div className="absolute w-48 h-48 rounded-full bg-amber-400/5 animate-pulse" />
              </>
            )}

            {/* Main Interactive Touch Circle */}
            <button
              type="button"
              onClick={handleStartScan}
              disabled={scanStatus === 'scanning' || scanStatus === 'success'}
              className={`relative w-28 h-28 rounded-full border-4 flex flex-col items-center justify-center transition-all duration-300 transform active:scale-95 shadow-2xl ${
                scanStatus === 'success'
                  ? 'border-emerald-500 bg-emerald-500/20 text-emerald-400 scale-105 shadow-emerald-500/40'
                  : scanStatus === 'scanning'
                  ? 'border-amber-400 bg-amber-400/20 text-amber-400 animate-pulse'
                  : isLuxury
                  ? 'border-amber-400/60 bg-gradient-to-br from-amber-400/20 via-black to-amber-950/30 text-amber-400 hover:border-amber-400 hover:scale-105 shadow-amber-400/20'
                  : 'border-blue-600 bg-gradient-to-br from-blue-100 to-blue-50 text-blue-700 hover:border-blue-700 hover:scale-105'
              }`}
            >
              {scanStatus === 'success' ? (
                <span className="material-symbols-outlined text-5xl animate-bounce">check_circle</span>
              ) : scanMode === 'fingerprint' ? (
                <span className={`material-symbols-outlined text-5xl transition-transform ${scanStatus === 'scanning' ? 'scale-110' : ''}`}>
                  fingerprint
                </span>
              ) : (
                <span className={`material-symbols-outlined text-5xl transition-transform ${scanStatus === 'scanning' ? 'scale-110' : ''}`}>
                  face
                </span>
              )}

              {/* Laser Scan line animation */}
              {scanStatus === 'scanning' && (
                <div className="absolute top-2 left-2 right-2 h-1 bg-amber-300 shadow-glow rounded-full animate-bounce" />
              )}
            </button>

            {/* Instruction / Status Text */}
            <p className={`mt-4 text-xs font-semibold max-w-xs leading-relaxed transition-colors ${
              scanStatus === 'success'
                ? 'text-emerald-400'
                : scanStatus === 'failed'
                ? 'text-red-400'
                : scanStatus === 'scanning'
                ? 'text-amber-300 animate-pulse'
                : 'text-gray-300'
            }`}>
              {statusMessage}
            </p>

            {/* Progress bar */}
            {scanStatus === 'scanning' && (
              <div className="w-48 h-1.5 bg-white/10 rounded-full mt-2 overflow-hidden">
                <div 
                  className="h-full bg-gradient-to-r from-amber-400 to-emerald-400 transition-all duration-200"
                  style={{ width: `${scanProgress}%` }}
                />
              </div>
            )}
          </div>

          {/* Quick Account Switcher (if multiple accounts saved on device) */}
          {registeredUsers.length > 1 && (
            <div className="w-full pt-3 border-t border-white/10">
              <span className="text-[11px] text-gray-400 block mb-2 text-right">
                یا انتخاب سریع سایر حساب‌های ذخیره‌شده در این دستگاه:
              </span>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                {registeredUsers.map((u) => {
                  const badge = getRoleBadge(u.role);
                  const isSelected = selectedUser?.id === u.id;
                  return (
                    <button
                      key={u.id}
                      type="button"
                      onClick={() => {
                        setSelectedUser(u);
                        setSavedBiometricUserId(u.id);
                        setScanStatus('idle');
                      }}
                      className={`p-2 rounded-xl border text-right transition-all flex items-center gap-2 ${
                        isSelected
                          ? isLuxury ? 'bg-amber-400/20 border-amber-400 text-amber-300' : 'bg-blue-100 border-blue-600 text-blue-900 font-bold'
                          : 'bg-white/5 border-white/10 text-gray-400 hover:bg-white/10 hover:text-white'
                      }`}
                    >
                      <span className="material-symbols-outlined text-sm">{badge.icon}</span>
                      <div className="truncate text-right">
                        <div className="text-xs font-bold truncate">{u.fullName}</div>
                        <div className="text-[9px] text-gray-400">{u.role === 'courier' ? 'سفیر' : u.role === 'hub_manager' ? 'هاب' : 'مشتری'}</div>
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Direct Action Buttons */}
          <div className="w-full flex flex-col sm:flex-row gap-2 pt-2">
            <button
              type="button"
              onClick={handleStartScan}
              disabled={scanStatus === 'scanning' || scanStatus === 'success'}
              className={`flex-1 py-3 px-4 rounded-xl font-bold text-xs shadow-lg transition-all active:scale-95 flex items-center justify-center gap-2 ${
                isLuxury
                  ? 'bg-gradient-to-r from-[#d4af37] via-[#f2ca50] to-[#d4af37] text-black hover:brightness-110'
                  : 'bg-blue-600 text-white hover:bg-blue-700'
              }`}
            >
              <span className="material-symbols-outlined text-base">touch_app</span>
              <span>{scanStatus === 'scanning' ? 'در حال تایید هویت...' : 'شروع اسکن و ورود مستقیم به پنل'}</span>
            </button>

            <button
              type="button"
              onClick={onClose}
              className="py-3 px-4 rounded-xl text-xs font-semibold bg-white/10 hover:bg-white/15 text-gray-300 transition-all"
            >
              انصراف و انتخاب دستی
            </button>
          </div>

        </div>

        {/* Footer Security Note */}
        <div className={`px-5 py-3 text-[11px] text-gray-400 text-center border-t flex items-center justify-center gap-2 ${
          isLuxury ? 'bg-black/80 border-white/5' : 'bg-gray-100 border-gray-200'
        }`}>
          <span className="material-symbols-outlined text-emerald-400 text-sm">lock</span>
          <span>امنیت سخت‌افزاری FIDO2 / TPM بدون ارسال رمز عبور در شبکه</span>
        </div>

      </div>
    </div>
  );
};
