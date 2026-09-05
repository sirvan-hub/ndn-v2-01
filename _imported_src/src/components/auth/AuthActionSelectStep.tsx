import React from 'react';
import { useApp } from '../../context/AppContext';
import { SecurityBanner } from './SecurityBanner';

export const AuthActionSelectStep: React.FC = () => {
  const { selectedPanel, startRegisterFlow, startLoginFlow, switchPanelFlow, theme } = useApp();
  const isLuxury = theme === 'luxury';

  const roleTitles = {
    customer: { title: 'پنل مشتری', desc: 'گیرنده و فرستنده بسته‌های محله', icon: 'account_circle' },
    hub_manager: { title: 'پنل هاب محله', desc: 'فروشگاه‌ها و کسب‌وکارهای میزبان PUDO', icon: 'storefront' },
    courier: { title: 'پنل مامور توزیع (PuDo)', desc: 'سفیران پست و پخش مرسولات', icon: 'local_shipping' },
  };

  const activeRole = selectedPanel ? roleTitles[selectedPanel] : roleTitles.customer;

  return (
    <div className="w-full max-w-xl mx-auto space-y-6 py-4 animate-in fade-in duration-200">
      
      {/* Selected Panel Info Header */}
      <div className={`p-5 rounded-2xl border flex items-center justify-between shadow-lg ${
        isLuxury ? 'bg-[#0e0e0e] border-[#d4af37]/40' : 'bg-white border-blue-200'
      }`}>
        <div className="flex items-center gap-3">
          <div className={`p-3 rounded-xl flex items-center justify-center ${
            isLuxury ? 'bg-amber-400/20 text-[#f2ca50]' : 'bg-blue-100 text-blue-900'
          }`}>
            <span className="material-symbols-outlined text-3xl">{activeRole.icon}</span>
          </div>
          <div className="text-right">
            <span className="text-[10px] text-gray-400 block">پنل انتخاب شده:</span>
            <h3 className={`text-base md:text-lg font-bold ${isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'}`}>
              {activeRole.title}
            </h3>
            <span className="text-xs text-gray-400">{activeRole.desc}</span>
          </div>
        </div>

        <button
          onClick={switchPanelFlow}
          className="text-xs text-amber-400 hover:underline flex items-center gap-1"
        >
          <span className="material-symbols-outlined text-sm">swap_horiz</span>
          <span>تغییر پنل</span>
        </button>
      </div>

      <SecurityBanner />

      {/* Main 2 Options */}
      <div className={`p-6 md:p-8 rounded-2xl border shadow-2xl space-y-5 ${
        isLuxury ? 'bg-[#0e0e0e] border-[#353535]' : 'bg-white border-gray-200'
      }`}>
        <div className="text-center space-y-1">
          <h3 className="text-lg font-bold text-white">
            نحوه دسترسی به حساب کاربری
          </h3>
          <p className="text-xs text-gray-400">
            آیا قبلاً در سامانه ثبت نام کرده‌اید یا می‌خواهید حساب جدید بسازید؟
          </p>
        </div>

        <div className="grid grid-cols-1 gap-4 pt-2">
          {/* Option 1: Login */}
          <div
            onClick={startLoginFlow}
            className={`p-5 rounded-2xl border-2 cursor-pointer transition-all flex items-center justify-between group ${
              isLuxury
                ? 'border-[#353535] bg-[#141414] hover:border-[#d4af37] hover:bg-[#1c1911]'
                : 'border-gray-200 bg-gray-50 hover:border-blue-500 hover:bg-blue-50'
            }`}
          >
            <div className="flex items-center gap-4">
              <div className={`p-3 rounded-xl ${
                isLuxury ? 'bg-amber-400/10 text-amber-300' : 'bg-blue-100 text-blue-800'
              }`}>
                <span className="material-symbols-outlined text-3xl">login</span>
              </div>
              <div className="text-right">
                <h4 className="text-base font-bold text-white group-hover:text-amber-400">
                  ورود کاربر به سیستم
                </h4>
                <p className="text-xs text-gray-400">
                  ورود با شماره تماس، رمز عبور، کپچا و تایید دو مرحله‌ای ۲FA
                </p>
              </div>
            </div>
            <span className="material-symbols-outlined text-gray-400 group-hover:text-amber-400 group-hover:-translate-x-1 transition-transform">
              arrow_back
            </span>
          </div>

          {/* Option 2: Register New User */}
          <div
            onClick={startRegisterFlow}
            className={`p-5 rounded-2xl border-2 cursor-pointer transition-all flex items-center justify-between group ${
              isLuxury
                ? 'border-[#353535] bg-[#141414] hover:border-orange-500 hover:bg-[#1c1611]'
                : 'border-gray-200 bg-gray-50 hover:border-orange-500 hover:bg-orange-50'
            }`}
          >
            <div className="flex items-center gap-4">
              <div className={`p-3 rounded-xl ${
                isLuxury ? 'bg-orange-500/10 text-orange-400' : 'bg-orange-100 text-orange-800'
              }`}>
                <span className="material-symbols-outlined text-3xl">person_add</span>
              </div>
              <div className="text-right">
                <h4 className="text-base font-bold text-white group-hover:text-orange-400">
                  ثبت نام کاربر جدید
                </h4>
                <p className="text-xs text-gray-400">
                  تکمیل اطلاعات احراز هویت و مشخصات مربوط به {activeRole.title}
                </p>
              </div>
            </div>
            <span className="material-symbols-outlined text-gray-400 group-hover:text-orange-400 group-hover:-translate-x-1 transition-transform">
              arrow_back
            </span>
          </div>
        </div>

        <div className="pt-2 text-center">
          <button
            onClick={switchPanelFlow}
            className="text-xs text-gray-400 hover:text-white flex items-center justify-center gap-1 mx-auto"
          >
            <span className="material-symbols-outlined text-sm">arrow_forward</span>
            <span>بازگشت به انتخاب نوع پنل</span>
          </button>
        </div>
      </div>

    </div>
  );
};
