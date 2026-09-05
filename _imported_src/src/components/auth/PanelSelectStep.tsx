import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import { UserRole } from '../../types';
import { SecurityBanner } from './SecurityBanner';
import { BiometricAuthModal } from './BiometricAuthModal';

export const PanelSelectStep: React.FC = () => {
  const { choosePanel, theme, registeredUsers, savedBiometricUserId, loginWithBiometrics } = useApp();
  const isLuxury = theme === 'luxury';
  const [isBiometricModalOpen, setIsBiometricModalOpen] = useState(false);

  // Find saved biometric user on this device if any
  const savedUser = savedBiometricUserId 
    ? registeredUsers.find(u => u.id === savedBiometricUserId) 
    : registeredUsers[0];

  const getRoleLabel = (role?: UserRole) => {
    switch (role) {
      case 'hub_manager':
        return 'پنل هاب محله';
      case 'courier':
        return 'پنل مامور توزیع';
      case 'customer':
      default:
        return 'پنل مشتری';
    }
  };

  const panels: {
    id: UserRole;
    title: string;
    subtitle: string;
    icon: string;
    badge: string;
    features: string[];
    bgAccent: string;
  }[] = [
    {
      id: 'customer',
      title: 'پنل مشتری',
      subtitle: 'گیرنده و فرستنده محله',
      icon: 'account_circle',
      badge: 'کاربران عمومی',
      features: [
        'دریافت مطمئن بسته‌ها در هاب نزدیک منزل',
        'رهگیری زنده و تایید تحویل با اسکن QR',
        'پرداخت آنلاین و عدم نیاز به حضور فیزیکی در خانه'
      ],
      bgAccent: isLuxury ? 'hover:border-amber-400/80 hover:bg-[#1a1811]' : 'hover:border-blue-500 hover:bg-blue-50/50'
    },
    {
      id: 'hub_manager',
      title: 'پنل هاب محله',
      subtitle: 'کسب‌وکارها، سوپرمارکت‌ها و دفاتر',
      icon: 'storefront',
      badge: 'کسب درآمد و PUDO',
      features: [
        'کسب درآمد پایدار از دریافت و تحویل بسته‌ها',
        'افزایش پاخور مشتریان محله به مغازه',
        'داشبورد اختصاصی مدیریت موجودی و انبارداری'
      ],
      bgAccent: isLuxury ? 'hover:border-[#f2ca50] hover:bg-[#1c1911]' : 'hover:border-emerald-500 hover:bg-emerald-50/50'
    },
    {
      id: 'courier',
      title: 'پنل مامور توزیع',
      subtitle: 'سفیران پست و ناوگان توزیع (PuDo)',
      icon: 'local_shipping',
      badge: 'سفیران پستی',
      features: [
        'تخلیه چندبسته در یک هاب به جای مراجعه تک‌تک',
        'کاهش زمان تردد و ترافیک شهری',
        'ثبت بارکد سریع با دوربین و تسویه منظم'
      ],
      bgAccent: isLuxury ? 'hover:border-orange-500 hover:bg-[#1c1611]' : 'hover:border-orange-500 hover:bg-orange-50/50'
    }
  ];

  return (
    <div className="w-full max-w-4xl mx-auto space-y-6 py-4 animate-in fade-in duration-200">
      
      {/* Brand Header */}
      <div className="text-center space-y-3">
        <div className="inline-flex p-3 rounded-2xl bg-amber-400/10 border border-amber-400/30 text-[#f2ca50] shadow-xl">
          <span className="material-symbols-outlined text-4xl">domain_verification</span>
        </div>
        <h2 className={`text-2xl md:text-3xl font-extrabold ${isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'}`}>
          به سامانه تحویل مطمئن محله (NDN) خوش آمدید
        </h2>
        <p className="text-xs md:text-sm text-gray-400 max-w-xl mx-auto leading-relaxed">
          لطفاً برای شروع کار با سامانه و دسترسی به امکانات مربوطه، نوع پنل کاربری خود را انتخاب فرمایید:
        </p>
      </div>

      {/* Security Status */}
      <SecurityBanner />

      {/* BIOMETRIC ONE-TOUCH LOGIN SECTION (User Requested) */}
      <section className={`p-5 md:p-6 rounded-2xl border-2 shadow-2xl relative overflow-hidden transition-all ${
        isLuxury 
          ? 'bg-gradient-to-r from-[#18150d] via-[#101010] to-[#14120c] border-[#d4af37]' 
          : 'bg-gradient-to-r from-blue-900 via-indigo-900 to-blue-950 border-blue-400 text-white'
      }`}>
        {/* Background glow circle */}
        <div className="absolute -left-10 -bottom-10 w-40 h-40 rounded-full bg-amber-400/10 blur-2xl pointer-events-none" />

        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 relative z-10">
          
          <div className="flex items-start gap-4">
            <div className={`w-14 h-14 rounded-2xl flex items-center justify-center border shadow-lg flex-shrink-0 cursor-pointer transition-transform hover:scale-105 active:scale-95 ${
              isLuxury 
                ? 'bg-gradient-to-br from-amber-400/30 to-amber-950/50 border-[#d4af37] text-[#f2ca50]' 
                : 'bg-white/20 border-white/40 text-white'
            }`}
            onClick={() => setIsBiometricModalOpen(true)}
            >
              <span className="material-symbols-outlined text-4xl animate-pulse">fingerprint</span>
            </div>

            <div className="space-y-1 text-right">
              <div className="flex flex-wrap items-center gap-2">
                <h3 className="text-base md:text-lg font-extrabold text-white flex items-center gap-1.5">
                  <span>ورود با احراز هویت</span>
                  <span className="text-xs text-amber-400 font-mono">(اثر انگشت / چهره)</span>
                </h3>
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-400/40 flex items-center gap-1">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping" />
                  <span>ورود مستقیم و آنی</span>
                </span>
              </div>

              <p className="text-xs text-gray-300 max-w-xl leading-relaxed">
                بدون نیاز به انتخاب نوع پنل کاربری و وارد کردن رمز عبور؛ در صورت ذخیره اطلاعات در این دستگاه، با اسکن اثر انگشت مستقیماً به پنل اختصاصی خود وارد شوید.
              </p>

              {savedUser && (
                <div className="pt-1 flex items-center gap-2 text-xs text-amber-300/90">
                  <span className="material-symbols-outlined text-sm text-amber-400">verified_user</span>
                  <span>حساب پیش‌فرض این دستگاه:</span>
                  <strong className="text-white underline decoration-amber-400/50">{savedUser.fullName}</strong>
                  <span className="text-[10px] px-1.5 py-0.5 rounded bg-white/10 text-gray-200">
                    {getRoleLabel(savedUser.role)}
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* Action Trigger Buttons */}
          <div className="w-full md:w-auto flex flex-col sm:flex-row md:flex-col gap-2 flex-shrink-0">
            <button
              type="button"
              onClick={() => setIsBiometricModalOpen(true)}
              className={`w-full py-3 px-5 rounded-xl font-extrabold text-xs shadow-xl transition-all active:scale-95 flex items-center justify-center gap-2 whitespace-nowrap ${
                isLuxury
                  ? 'bg-gradient-to-r from-[#d4af37] via-[#f2ca50] to-[#d4af37] text-black hover:brightness-110 shadow-amber-400/20'
                  : 'bg-white text-blue-900 hover:bg-gray-100 shadow-md'
              }`}
            >
              <span className="material-symbols-outlined text-lg">fingerprint</span>
              <span>ورود با اثر انگشت / چهره</span>
            </button>

            <button
              type="button"
              onClick={async () => {
                // Instant direct login without modal
                await loginWithBiometrics();
              }}
              className="w-full py-1.5 px-3 rounded-lg text-[11px] font-semibold text-gray-300 hover:text-white bg-white/5 hover:bg-white/10 transition-all flex items-center justify-center gap-1"
            >
              <span className="material-symbols-outlined text-xs text-emerald-400">bolt</span>
              <span>ورود فوری با یک کلیک</span>
            </button>
          </div>

        </div>
      </section>

      {/* 3 Panel Option Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        {panels.map((p) => (
          <div
            key={p.id}
            onClick={() => choosePanel(p.id)}
            className={`p-6 rounded-2xl border-2 cursor-pointer transition-all duration-300 flex flex-col justify-between group transform hover:-translate-y-1 shadow-xl ${
              isLuxury
                ? `bg-[#0e0e0e] border-[#353535] ${p.bgAccent}`
                : `bg-white border-gray-200 ${p.bgAccent}`
            }`}
          >
            <div>
              <div className="flex justify-between items-start mb-4">
                <div className={`p-3 rounded-2xl flex items-center justify-center transition-transform group-hover:scale-110 ${
                  isLuxury ? 'bg-amber-400/15 text-[#f2ca50]' : 'bg-blue-100 text-blue-900'
                }`}>
                  <span className="material-symbols-outlined text-3xl">{p.icon}</span>
                </div>
                <span className={`text-[10px] font-bold px-2.5 py-1 rounded-full border ${
                  isLuxury ? 'bg-white/5 text-amber-300 border-amber-400/30' : 'bg-blue-50 text-blue-800 border-blue-200'
                }`}>
                  {p.badge}
                </span>
              </div>

              <h3 className={`text-lg font-bold mb-1 ${isLuxury ? 'text-white group-hover:text-[#f2ca50]' : 'text-gray-900 group-hover:text-blue-700'}`}>
                {p.title}
              </h3>
              <p className="text-xs text-gray-400 mb-4">{p.subtitle}</p>

              <div className="space-y-2 mb-6">
                {p.features.map((feat, i) => (
                  <div key={i} className="flex items-start gap-2 text-xs text-right">
                    <span className="material-symbols-outlined text-sm text-emerald-400 mt-0.5 flex-shrink-0">check_circle</span>
                    <span className="text-gray-300 leading-relaxed">{feat}</span>
                  </div>
                ))}
              </div>
            </div>

            <button
              type="button"
              className={`w-full py-3 px-4 rounded-xl font-bold text-xs flex items-center justify-center gap-2 transition-all shadow-md active:scale-95 ${
                isLuxury
                  ? 'bg-[#d4af37] text-black group-hover:bg-[#f2ca50]'
                  : 'bg-[#1e3a8a] text-white group-hover:bg-[#00236f]'
              }`}
            >
              <span>انتخاب و ادامه</span>
              <span className="material-symbols-outlined text-base">arrow_back</span>
            </button>
          </div>
        ))}
      </div>

      {/* Biometric Scanner Modal */}
      <BiometricAuthModal 
        isOpen={isBiometricModalOpen}
        onClose={() => setIsBiometricModalOpen(false)}
      />

    </div>
  );
};
