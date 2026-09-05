import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import { SecurityBanner } from './SecurityBanner';

export const RegisterCourierForm: React.FC = () => {
  const { registerUser, setAuthStep, theme } = useApp();
  const isLuxury = theme === 'luxury';

  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [nationalCardImage, setNationalCardImage] = useState<string | null>(null);
  const [bankCardNumber, setBankCardNumber] = useState('');
  const [profileAvatar, setProfileAvatar] = useState<string | null>(null);
  const [postalDistrict, setPostalDistrict] = useState('منطقه ۲ پستی (سعادت‌آباد و شهرک غرب)');
  const [workCoverageArea, setWorkCoverageArea] = useState('مناطق ۲ و ۵ تهران');
  const [coverageRadiusKm, setCoverageRadiusKm] = useState<number>(5);

  const postalDistrictOptions = [
    'منطقه ۲ پستی (سعادت‌آباد، شهرک غرب، فرحزاد)',
    'منطقه ۵ پستی (بلوار فردوس، پونک، صادقیه)',
    'منطقه ۱ پستی (تجریش، زعفرانیه، نیاوران، شریعتی)',
    'منطقه ۳ پستی (قلهک، دروس، میرداماد، ونک)',
    'منطقه ۶ پستی (یوسف‌آباد، امیرآباد، ولیعصر)',
    'منطقه ۲۲ پستی (چیتگر، دریاچه، دهکده المپیک)'
  ];

  const handleBankCardChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    let val = e.target.value.replace(/\D/g, '').slice(0, 16);
    val = val.replace(/(\d{4})(?=\d)/g, '$1-');
    setBankCardNumber(val);
  };

  const handleNationalCardUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = () => setNationalCardImage(reader.result as string);
      reader.readAsDataURL(file);
    }
  };

  const handleAvatarUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = () => setProfileAvatar(reader.result as string);
      reader.readAsDataURL(file);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    registerUser({
      fullName,
      phone,
      password: password || '123456',
      nationalCardImage: nationalCardImage || undefined,
      bankCardNumber,
      profileAvatar: profileAvatar || 'https://lh3.googleusercontent.com/aida-public/AB6AXuB7aos7IohtH13WLZRghJVTrM3NpelGW4klIVOAfdaJTycxNkyqrNliBmZmm_pJOjvM8wTIzUCRW1wkZwHHDOtq5UxJHcZC1KCm5BOy0GUPFL1c-zPP154se7Y6bYZcRsxpUrGWQ01U0Yt0F3REI00OBmlS5bBF1-Y6ZvK8jrjJWDGPabYNbnBpW2fzVJ79P_eGdGmHZUMKofLZg0D3LIrwGIrQXsEfODiZcVdyix2y-D9oBHMCuZFcPg',
      postalDistrict,
      workCoverageArea,
      coverageRadiusKm
    });
  };

  return (
    <div className="w-full max-w-2xl mx-auto space-y-5 py-4 animate-in fade-in duration-200">
      
      {/* Header */}
      <div className="flex items-center justify-between border-b pb-3 border-gray-700/30">
        <div className="text-right">
          <h2 className={`text-xl font-bold ${isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'}`}>
            ثبت نام مامور توزیع (PuDo سفیر)
          </h2>
          <p className="text-xs text-gray-400 mt-0.5">
            عضویت در ناوگان توزیع سریع محله، اتصال به هاب‌ها و تسویه حساب روزانه
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

      <form onSubmit={handleSubmit} className={`p-6 md:p-8 rounded-2xl border shadow-2xl space-y-4 text-right ${
        isLuxury ? 'bg-[#0e0e0e] border-[#353535]' : 'bg-white border-gray-200'
      }`}>

        {/* Full Name & Mobile */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-semibold text-gray-300 mb-1.5">
              نام و نام خانوادگی سفیر <span className="text-red-400">*</span>
            </label>
            <input
              type="text"
              required
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="مثال: محمد جوادی"
              className={`w-full px-3.5 py-2.5 rounded-xl border text-xs outline-none ${
                isLuxury ? 'bg-[#181818] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900 focus:border-blue-500'
              }`}
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-300 mb-1.5">
              شماره موبایل سفیر <span className="text-red-400">*</span>
            </label>
            <input
              type="tel"
              required
              dir="ltr"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="09121112233"
              className={`w-full px-3.5 py-2.5 rounded-xl border text-xs font-mono outline-none ${
                isLuxury ? 'bg-[#181818] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900 focus:border-blue-500'
              }`}
            />
          </div>
        </div>

        {/* Password & Bank Card */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-semibold text-gray-300 mb-1.5">
              رمز عبور ورود به سامانه <span className="text-red-400">*</span>
            </label>
            <input
              type="password"
              required
              dir="ltr"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="حداقل ۶ کاراکتر"
              className={`w-full px-3.5 py-2.5 rounded-xl border text-xs font-mono outline-none ${
                isLuxury ? 'bg-[#181818] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900 focus:border-blue-500'
              }`}
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-300 mb-1.5">
              شماره کارت بانکی برای تسویه مالی (به نام صاحب اکانت) <span className="text-red-400">*</span>
            </label>
            <input
              type="text"
              required
              dir="ltr"
              value={bankCardNumber}
              onChange={handleBankCardChange}
              placeholder="۵۸۹۲-۱۰۱۲-xxxx-xxxx"
              className={`w-full px-3.5 py-2.5 rounded-xl border text-xs font-mono outline-none ${
                isLuxury ? 'bg-[#181818] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900 focus:border-blue-500'
              }`}
            />
          </div>
        </div>

        {/* Uploads: Profile Avatar + National Card */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {/* Profile photo */}
          <div>
            <label className="block text-xs font-semibold text-gray-300 mb-1.5">
              یک تصویر برای پروفایل سفیر <span className="text-red-400">*</span>
            </label>
            {profileAvatar ? (
              <div className="relative w-full h-28 rounded-xl overflow-hidden border border-amber-400 flex items-center justify-center bg-black">
                <img src={profileAvatar} alt="تصویر پروفایل" className="w-24 h-24 rounded-full object-cover border-2 border-amber-400" />
                <button type="button" onClick={() => setProfileAvatar(null)} className="absolute top-1 right-1 bg-black/80 text-white p-1 rounded-full">
                  <span className="material-symbols-outlined text-xs">close</span>
                </button>
              </div>
            ) : (
              <label className={`border-2 border-dashed rounded-xl p-3 flex flex-col items-center justify-center gap-1 cursor-pointer ${
                isLuxury ? 'border-amber-400/30 bg-[#161616]' : 'border-blue-300 bg-blue-50/50'
              }`}>
                <span className="material-symbols-outlined text-2xl text-amber-400">account_circle</span>
                <span className="text-[11px]">آپلود تصویر چهره / پرسنلی</span>
                <input type="file" accept="image/*" className="hidden" onChange={handleAvatarUpload} />
              </label>
            )}
          </div>

          {/* National card */}
          <div>
            <label className="block text-xs font-semibold text-gray-300 mb-1.5">
              تصویر کارت ملی سفیر <span className="text-red-400">*</span>
            </label>
            {nationalCardImage ? (
              <div className="relative w-full h-28 rounded-xl overflow-hidden border border-amber-400">
                <img src={nationalCardImage} alt="کارت ملی" className="w-full h-full object-cover" />
                <button type="button" onClick={() => setNationalCardImage(null)} className="absolute top-1 right-1 bg-black/80 text-white p-1 rounded-full">
                  <span className="material-symbols-outlined text-xs">close</span>
                </button>
              </div>
            ) : (
              <label className={`border-2 border-dashed rounded-xl p-3 flex flex-col items-center justify-center gap-1 cursor-pointer ${
                isLuxury ? 'border-amber-400/30 bg-[#161616]' : 'border-blue-300 bg-blue-50/50'
              }`}>
                <span className="material-symbols-outlined text-2xl text-amber-400">badge</span>
                <span className="text-[11px]">آپلود تصویر کارت ملی</span>
                <input type="file" accept="image/*" className="hidden" onChange={handleNationalCardUpload} />
              </label>
            )}
          </div>
        </div>

        {/* Postal District Selection */}
        <div>
          <label className="block text-xs font-semibold text-gray-300 mb-1.5">
            منطقه پستی فعالیت <span className="text-red-400">*</span>
          </label>
          <select
            value={postalDistrict}
            onChange={(e) => setPostalDistrict(e.target.value)}
            className={`w-full px-3.5 py-2.5 rounded-xl border text-xs outline-none ${
              isLuxury ? 'bg-[#181818] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900 focus:border-blue-500'
            }`}
          >
            {postalDistrictOptions.map((opt) => (
              <option key={opt} value={opt} className="bg-gray-900 text-white">{opt}</option>
            ))}
          </select>
        </div>

        {/* Interactive Map Radius / Coverage Zone Selector */}
        <div>
          <div className="flex justify-between items-center mb-1.5">
            <label className="text-xs font-semibold text-gray-300">
              انتخاب محدوده کاری روی نقشه (شعاع تحت پوشش) <span className="text-red-400">*</span>
            </label>
            <span className="text-[11px] text-amber-400 font-bold font-mono">
              شعاع فعالیت: {coverageRadiusKm} کیلومتر
            </span>
          </div>

          <div className="relative w-full h-44 rounded-xl overflow-hidden border border-white/10 group cursor-crosshair">
            <img
              src="https://lh3.googleusercontent.com/aida-public/AB6AXuDAa4ypKcmRsUBxkyqoCHinxdQKl0Zv6t5QcRqbNwzsoTm3KdA2NIivTzac46gZPnzohvPuiK_M3H1uaJSHckduDUxvho-liOVTdvwc7DahHW6vFUaxEQwileZwzeW-0Ks6nutIavRiegtIqg806-R2PsZZk2bEVwThXZlxYQCPT4RpeLBe7nO2J841q4ucCvwgAGzr4qP8d2HBymK7xc_93uruDhZ16zcrJ4FEcqx-RLKKWuAXic0o6A"
              alt="محدوده کاری سفیر"
              className="w-full h-full object-cover grayscale opacity-75"
            />
            {/* Visual radar circle representing coverage radius */}
            <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
              <div 
                className="rounded-full border-2 border-dashed border-amber-400 bg-amber-500/20 flex items-center justify-center transition-all duration-300 animate-pulse"
                style={{ width: `${coverageRadiusKm * 32}px`, height: `${coverageRadiusKm * 32}px`, maxWidth: '160px', maxHeight: '160px' }}
              >
                <div className="w-3 h-3 rounded-full bg-amber-400 ring-4 ring-amber-400/40" />
              </div>
            </div>
            
            <div className="absolute bottom-2 right-2 bg-black/85 px-2.5 py-1 rounded-lg text-[10px] text-gray-200 border border-white/10 flex items-center gap-1">
              <span className="material-symbols-outlined text-xs text-amber-400">radar</span>
              <span>هاب‌های تحت پوشش این محدوده: ۴ هاب محلی</span>
            </div>
          </div>

          {/* Slider for radius */}
          <div className="flex items-center gap-3 mt-2 px-1">
            <span className="text-[10px] text-gray-400">۲ ک.م</span>
            <input
              type="range"
              min={2}
              max={15}
              value={coverageRadiusKm}
              onChange={(e) => setCoverageRadiusKm(Number(e.target.value))}
              className="flex-1 accent-[#d4af37] h-1.5 bg-gray-700 rounded-lg cursor-pointer"
            />
            <span className="text-[10px] text-gray-400">۱۵ ک.م</span>
          </div>
        </div>

        {/* Submit */}
        <div className="pt-3">
          <button
            type="submit"
            className={`w-full py-3.5 px-4 rounded-xl font-bold text-xs shadow-lg transition-all active:scale-95 flex items-center justify-center gap-2 ${
              isLuxury 
                ? 'bg-[#d4af37] text-black hover:bg-[#e9c349]' 
                : 'bg-[#1e3a8a] text-white hover:bg-[#00236f]'
            }`}
          >
            <span>تکمیل ثبت نام سفیر و دریافت کد تایید (۲FA)</span>
            <span className="material-symbols-outlined text-base">arrow_back</span>
          </button>
        </div>

      </form>

    </div>
  );
};
