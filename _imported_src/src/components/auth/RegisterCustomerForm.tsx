import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import { SecurityBanner } from './SecurityBanner';

export const RegisterCustomerForm: React.FC = () => {
  const { registerUser, setAuthStep, theme } = useApp();
  const isLuxury = theme === 'luxury';

  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [postalCode, setPostalCode] = useState('');
  const [address, setAddress] = useState('');
  const [nationalCardImage, setNationalCardImage] = useState<string | null>(null);

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = () => setNationalCardImage(reader.result as string);
      reader.readAsDataURL(file);
    }
  };

  const handleSimulateCameraCapture = () => {
    setNationalCardImage('https://images.unsplash.com/photo-1544717305-2782549b5136?w=500&auto=format&fit=crop&q=80');
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    registerUser({
      fullName,
      phone,
      password: password || '123456',
      postalCode,
      address,
      nationalCardImage: nationalCardImage || undefined
    });
  };

  return (
    <div className="w-full max-w-xl mx-auto space-y-5 py-4 animate-in fade-in duration-200">
      
      {/* Header */}
      <div className="flex items-center justify-between border-b pb-3 border-gray-700/30">
        <div className="text-right">
          <h2 className={`text-xl font-bold ${isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'}`}>
            ثبت نام مشتری (گیرنده و فرستنده محله)
          </h2>
          <p className="text-xs text-gray-400 mt-0.5">
            لطفاً اطلاعات هویتی و آدرس خود را برای دریافت مطمئن بسته‌ها وارد نمایید.
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

        {/* Full Name */}
        <div>
          <label className="block text-xs font-semibold text-gray-300 mb-1.5">
            نام و نام خانوادگی <span className="text-red-400">*</span>
          </label>
          <input
            type="text"
            required
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            placeholder="مثال: علی رضایی"
            className={`w-full px-3.5 py-2.5 rounded-xl border text-xs outline-none transition-all ${
              isLuxury 
                ? 'bg-[#181818] border-white/10 text-white focus:border-[#d4af37]' 
                : 'bg-gray-50 border-gray-300 text-gray-900 focus:border-blue-500'
            }`}
          />
        </div>

        {/* Mobile Number & Password */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-semibold text-gray-300 mb-1.5">
              شماره موبایل <span className="text-red-400">*</span>
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
                  isLuxury 
                    ? 'bg-[#181818] border-white/10 text-white focus:border-[#d4af37]' 
                    : 'bg-gray-50 border-gray-300 text-gray-900 focus:border-blue-500'
                }`}
              />
              <span className="material-symbols-outlined absolute left-3 top-2.5 text-gray-400 text-base">
                phone_iphone
              </span>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-300 mb-1.5">
              رمز عبور <span className="text-red-400">*</span>
            </label>
            <input
              type="password"
              required
              dir="ltr"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="حداقل ۶ کاراکتر"
              className={`w-full px-3.5 py-2.5 rounded-xl border text-xs font-mono outline-none ${
                isLuxury 
                  ? 'bg-[#181818] border-white/10 text-white focus:border-[#d4af37]' 
                  : 'bg-gray-50 border-gray-300 text-gray-900 focus:border-blue-500'
              }`}
            />
          </div>
        </div>

        {/* National ID Card Photo Upload */}
        <div>
          <label className="block text-xs font-semibold text-gray-300 mb-1.5">
            تصویر کارت ملی (احراز هویت) <span className="text-red-400">*</span>
          </label>
          
          {nationalCardImage ? (
            <div className="relative w-full h-36 rounded-xl overflow-hidden border-2 border-amber-400 shadow-md">
              <img src={nationalCardImage} alt="کارت ملی" className="w-full h-full object-cover" />
              <button
                type="button"
                onClick={() => setNationalCardImage(null)}
                className="absolute top-2 right-2 bg-black/80 text-white p-1 rounded-full hover:bg-red-600"
              >
                <span className="material-symbols-outlined text-sm">close</span>
              </button>
              <div className="absolute bottom-2 left-2 bg-emerald-500 text-white text-[10px] px-2 py-0.5 rounded-full flex items-center gap-1">
                <span className="material-symbols-outlined text-xs">check_circle</span>
                <span>تصویر بارگذاری شد</span>
              </div>
            </div>
          ) : (
            <div className="flex flex-col gap-2">
              <label className={`border-2 border-dashed rounded-xl p-4 flex flex-col items-center justify-center gap-1.5 cursor-pointer transition-all ${
                isLuxury ? 'border-amber-400/40 bg-[#161616] hover:bg-[#1f1f1f]' : 'border-blue-300 bg-blue-50/50 hover:bg-blue-50'
              }`}>
                <span className="material-symbols-outlined text-3xl text-amber-400">badge</span>
                <span className="text-xs font-medium text-gray-200">انتخاب تصویر کارت ملی یا کارت شناسایی</span>
                <span className="text-[10px] text-gray-400">JPG، PNG تا سقف ۵ مگابایت</span>
                <input type="file" accept="image/*" className="hidden" onChange={handleImageUpload} />
              </label>

              <button
                type="button"
                onClick={handleSimulateCameraCapture}
                className="text-xs text-amber-400 hover:underline flex items-center justify-center gap-1 py-1"
              >
                <span className="material-symbols-outlined text-sm">photo_camera</span>
                <span>یا عکسبرداری مستقیم با دوربین</span>
              </button>
            </div>
          )}
        </div>

        {/* Postal Code & Address */}
        <div>
          <label className="block text-xs font-semibold text-gray-300 mb-1.5">
            کد پستی ۱۰ رقمی <span className="text-red-400">*</span>
          </label>
          <input
            type="text"
            required
            maxLength={10}
            dir="ltr"
            value={postalCode}
            onChange={(e) => setPostalCode(e.target.value)}
            placeholder="1998765432"
            className={`w-full px-3.5 py-2.5 rounded-xl border text-xs font-mono outline-none ${
              isLuxury 
                ? 'bg-[#181818] border-white/10 text-white focus:border-[#d4af37]' 
                : 'bg-gray-50 border-gray-300 text-gray-900 focus:border-blue-500'
            }`}
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-gray-300 mb-1.5">
            آدرس دقیق پستی <span className="text-red-400">*</span>
          </label>
          <textarea
            required
            rows={3}
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            placeholder="استان، شهر، خیابان اصلی، کوچه، پلاک، واحد..."
            className={`w-full px-3.5 py-2.5 rounded-xl border text-xs outline-none leading-relaxed ${
              isLuxury 
                ? 'bg-[#181818] border-white/10 text-white focus:border-[#d4af37]' 
                : 'bg-gray-50 border-gray-300 text-gray-900 focus:border-blue-500'
            }`}
          />
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
            <span>ثبت اطلاعات و دریافت کد تایید (۲FA)</span>
            <span className="material-symbols-outlined text-base">arrow_back</span>
          </button>
        </div>

      </form>

    </div>
  );
};
