import React from 'react';
import { useApp } from '../../context/AppContext';
import { PanelSelectStep } from '../auth/PanelSelectStep';
import { AuthActionSelectStep } from '../auth/AuthActionSelectStep';
import { RegisterCustomerForm } from '../auth/RegisterCustomerForm';
import { RegisterHubForm } from '../auth/RegisterHubForm';
import { RegisterCourierForm } from '../auth/RegisterCourierForm';
import { LoginForm } from '../auth/LoginForm';
import { TwoFactorForm } from '../auth/TwoFactorForm';
import { LockedOutStep } from '../auth/LockedOutStep';
import { PasswordRecoveryModal } from '../auth/PasswordRecoveryModal';
import { SecurityBanner } from '../auth/SecurityBanner';

export const AuthView: React.FC = () => {
  const { 
    authStep, 
    selectedPanel, 
    isAuthenticated, 
    currentUser, 
    logout, 
    switchPanelFlow,
    setActiveTab,
    theme 
  } = useApp();

  const isLuxury = theme === 'luxury';

  // If already authenticated and visiting profile/auth tab:
  if (isAuthenticated && currentUser && authStep === 'authenticated') {
    return (
      <div className="w-full max-w-xl mx-auto space-y-6 py-4 animate-in fade-in duration-200">
        
        {/* Profile Card */}
        <div className={`p-6 md:p-8 rounded-2xl border shadow-2xl space-y-6 text-right ${
          isLuxury ? 'bg-[#0e0e0e] border-[#d4af37]/40' : 'bg-white border-blue-200'
        }`}>
          
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-14 h-14 rounded-2xl overflow-hidden bg-amber-400/20 flex items-center justify-center border-2 border-amber-400/40">
                {currentUser.profileAvatar ? (
                  <img src={currentUser.profileAvatar} alt={currentUser.fullName} className="w-full h-full object-cover" />
                ) : (
                  <span className="material-symbols-outlined text-3xl text-amber-400">account_circle</span>
                )}
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-lg font-bold text-white">{currentUser.fullName}</h2>
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                    احراز هویت شده (2FA)
                  </span>
                </div>
                <span className="text-xs text-gray-400 font-mono">{currentUser.phone}</span>
              </div>
            </div>

            <div className="text-left">
              <span className="text-[11px] font-bold text-amber-400 bg-amber-400/10 px-2.5 py-1 rounded-full border border-amber-400/30">
                {currentUser.role === 'hub_manager' ? 'مدیر هاب محله' : currentUser.role === 'courier' ? 'سفیر توزیع PuDo' : 'مشتری محله'}
              </span>
            </div>
          </div>

          <SecurityBanner />

          {/* Role specific detailed attributes */}
          <div className="p-4 rounded-xl bg-white/5 border border-white/10 space-y-3 text-xs">
            <div className="font-bold text-gray-200 flex items-center gap-1.5 border-b border-white/5 pb-2">
              <span className="material-symbols-outlined text-amber-400 text-sm">badge</span>
              <span>اطلاعات تکمیلی حساب کاربری:</span>
            </div>

            {currentUser.role === 'customer' && (
              <>
                <div className="flex justify-between">
                  <span className="text-gray-400">کد پستی ۱۰ رقمی:</span>
                  <span className="font-mono text-white">{currentUser.postalCode || '۱۹۹۸۷۶۵۴۳۲'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">آدرس ثبت‌شده:</span>
                  <span className="text-white text-right max-w-xs">{currentUser.address || 'تهران، سعادت‌آباد، میدان کاج، خیابان سرو شرقی، پلاک ۲۴'}</span>
                </div>
              </>
            )}

            {currentUser.role === 'hub_manager' && (
              <>
                <div className="flex justify-between">
                  <span className="text-gray-400">نام فروشگاه هاب:</span>
                  <span className="text-white font-bold">{currentUser.storeName || 'سوپرمارکت یاران'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">صنف کسب‌وکار:</span>
                  <span className="text-white">{currentUser.guildType || 'سوپرمارکت و هایپرمارکت'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">شماره کارت تسویه:</span>
                  <span className="font-mono text-amber-400">{currentUser.bankCardNumber || '۶۰۳۷-۹۹۷۵-۱۲۳۴-۵۶۷۸'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">ساعات کاری هفته:</span>
                  <span className="text-white">{currentUser.workingHoursWeekday || '۰۸:۰۰ الی ۲۲:۰۰'}</span>
                </div>
              </>
            )}

            {currentUser.role === 'courier' && (
              <>
                <div className="flex justify-between">
                  <span className="text-gray-400">منطقه پستی فعالیت:</span>
                  <span className="text-white font-bold">{currentUser.postalDistrict || 'منطقه ۲ پستی تهران'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">شعاع تحت پوشش:</span>
                  <span className="text-amber-400 font-mono">{currentUser.coverageRadiusKm || 5} کیلومتر</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">شماره کارت تسویه سفیر:</span>
                  <span className="font-mono text-white">{currentUser.bankCardNumber || '۵۸۹۲-۱۰۱۲-۸۸۸۸-۴۴۴۴'}</span>
                </div>
              </>
            )}
          </div>

          {/* Action buttons */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
            <button
              onClick={() => setActiveTab('dashboard')}
              className={`py-3 px-4 rounded-xl font-bold text-xs flex items-center justify-center gap-2 shadow-lg ${
                isLuxury ? 'bg-[#d4af37] text-black hover:bg-[#e9c349]' : 'bg-[#1e3a8a] text-white hover:bg-[#00236f]'
              }`}
            >
              <span className="material-symbols-outlined text-base">dashboard</span>
              <span>ورود به داشبورد کاری</span>
            </button>

            <button
              onClick={switchPanelFlow}
              className="py-3 px-4 rounded-xl font-bold text-xs flex items-center justify-center gap-2 border border-white/10 hover:bg-white/5 text-gray-200"
            >
              <span className="material-symbols-outlined text-base">swap_horiz</span>
              <span>تغییر پنل یا حساب</span>
            </button>
          </div>

          <div className="text-center pt-2">
            <button
              onClick={logout}
              className="text-xs text-red-400 hover:text-red-300 flex items-center justify-center gap-1 mx-auto"
            >
              <span className="material-symbols-outlined text-sm">logout</span>
              <span>خروج امن از حساب کاربری</span>
            </button>
          </div>

        </div>

      </div>
    );
  }

  // Render Step-by-Step Flow according to user's exact specification
  switch (authStep) {
    case 'panel_select':
      return <PanelSelectStep />;

    case 'auth_action_select':
      return <AuthActionSelectStep />;

    case 'register_form':
      if (selectedPanel === 'hub_manager') return <RegisterHubForm />;
      if (selectedPanel === 'courier') return <RegisterCourierForm />;
      return <RegisterCustomerForm />;

    case 'login_form':
      return <LoginForm />;

    case 'two_factor_form':
      return <TwoFactorForm />;

    case 'password_recovery':
      return <PasswordRecoveryModal />;

    case 'locked_out':
      return <LockedOutStep />;

    default:
      return <PanelSelectStep />;
  }
};
