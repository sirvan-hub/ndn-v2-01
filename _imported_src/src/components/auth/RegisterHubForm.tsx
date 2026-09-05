import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import { SecurityBanner } from './SecurityBanner';

export const RegisterHubForm: React.FC = () => {
  const { registerUser, setAuthStep, theme } = useApp();
  const isLuxury = theme === 'luxury';

  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [nationalCardImage, setNationalCardImage] = useState<string | null>(null);
  const [bankCardNumber, setBankCardNumber] = useState('');
  const [storeImage, setStoreImage] = useState<string | null>(null);
  const [storeName, setStoreName] = useState('');
  const [guildType, setGuildType] = useState('سوپرمارکت');
  const [exactAddress, setExactAddress] = useState('');
  const [storeLocation, setStoreLocation] = useState<{ lat: number; lng: number }>({ lat: 35.7924, lng: 51.3789 });
  const [workingHoursWeekday, setWorkingHoursWeekday] = useState('۰۸:۰۰ الی ۲۲:۰۰');
  const [workingHoursThursday, setWorkingHoursThursday] = useState('۰۸:۰۰ الی ۲۳:۰۰');
  const [workingHoursFriday, setWorkingHoursFriday] = useState('۰۹:۰۰ الی ۲۱:۰۰');
  const [landlinePhone, setLandlinePhone] = useState('');
  const [servicesDescription, setServicesDescription] = useState('انبارداری موقت بسته‌ها، تحویل با اسکن QR، بسته‌بندی');

  const guildOptions = [
    'سوپرمارکت',
    'لوازم‌التحریر و کتابفروشی',
    'کافی‌نت و خدمات دیجیتال',
    'خشکشویی و اتوشویی',
    'داروخانه و بهداشتی',
    'فروشگاه پروتئینی و هایپرمارکت',
    'قنادی و نان فانتزی',
    'فروشگاه کالای دیجیتال و موبایل',
    'سایر اصناف محله'
  ];

  const handleBankCardChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    let val = e.target.value.replace(/\D/g, '').slice(0, 16);
    // Format into groups of 4
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

  const handleStoreImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = () => setStoreImage(reader.result as string);
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
      storeImage: storeImage || 'https://images.unsplash.com/photo-1578916171728-46686eac8d58?w=500&auto=format&fit=crop&q=80',
      storeName,
      guildType,
      exactAddress,
      storeLocation,
      workingHoursWeekday,
      workingHoursThursday,
      workingHoursFriday,
      landlinePhone: landlinePhone || undefined,
      servicesDescription
    });
  };

  return (
    <div className="w-full max-w-2xl mx-auto space-y-5 py-4 animate-in fade-in duration-200">
      
      {/* Header */}
      <div className="flex items-center justify-between border-b pb-3 border-gray-700/30">
        <div className="text-right">
          <h2 className={`text-xl font-bold ${isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'}`}>
            ثبت نام هاب محله (کسب‌وکارها و فروشگاه‌ها)
          </h2>
          <p className="text-xs text-gray-400 mt-0.5">
            تکمیل اطلاعات فروشگاه، شماره شبا/کارت تسویه و ساعات کاری جهت میزبانی مرسولات محله
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

        {/* Manager Full Name & Mobile */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-semibold text-gray-300 mb-1.5">
              نام و نام خانوادگی مدیر هاب <span className="text-red-400">*</span>
            </label>
            <input
              type="text"
              required
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="مثال: امید اکبری"
              className={`w-full px-3.5 py-2.5 rounded-xl border text-xs outline-none ${
                isLuxury ? 'bg-[#181818] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900 focus:border-blue-500'
              }`}
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-300 mb-1.5">
              شماره موبایل مدیر <span className="text-red-400">*</span>
            </label>
            <input
              type="tel"
              required
              dir="ltr"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="09129876543"
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
              شماره کارت مبادلات مالی (به نام صاحب حساب) <span className="text-red-400">*</span>
            </label>
            <input
              type="text"
              required
              dir="ltr"
              value={bankCardNumber}
              onChange={handleBankCardChange}
              placeholder="۶۰۳۷-۹۹۷۵-xxxx-xxxx"
              className={`w-full px-3.5 py-2.5 rounded-xl border text-xs font-mono outline-none ${
                isLuxury ? 'bg-[#181818] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900 focus:border-blue-500'
              }`}
            />
          </div>
        </div>

        {/* Store Name, Guild Type & Landline */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div>
            <label className="block text-xs font-semibold text-gray-300 mb-1.5">
              نام مغازه / فروشگاه <span className="text-red-400">*</span>
            </label>
            <input
              type="text"
              required
              value={storeName}
              onChange={(e) => setStoreName(e.target.value)}
              placeholder="مثال: سوپرمارکت یاران"
              className={`w-full px-3.5 py-2.5 rounded-xl border text-xs outline-none ${
                isLuxury ? 'bg-[#181818] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900 focus:border-blue-500'
              }`}
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-300 mb-1.5">
              نوع صنف کسب‌وکار <span className="text-red-400">*</span>
            </label>
            <select
              value={guildType}
              onChange={(e) => setGuildType(e.target.value)}
              className={`w-full px-3.5 py-2.5 rounded-xl border text-xs outline-none ${
                isLuxury ? 'bg-[#181818] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900 focus:border-blue-500'
              }`}
            >
              {guildOptions.map((opt) => (
                <option key={opt} value={opt} className="bg-gray-900 text-white">{opt}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-300 mb-1.5">
              تلفن ثابت (اختیاری)
            </label>
            <input
              type="tel"
              dir="ltr"
              value={landlinePhone}
              onChange={(e) => setLandlinePhone(e.target.value)}
              placeholder="۰۲۱-۲۲۱۴۵۶۷۸"
              className={`w-full px-3.5 py-2.5 rounded-xl border text-xs font-mono outline-none ${
                isLuxury ? 'bg-[#181818] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900 focus:border-blue-500'
              }`}
            />
          </div>
        </div>

        {/* Uploads: National Card Image + Storefront Image */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {/* National card */}
          <div>
            <label className="block text-xs font-semibold text-gray-300 mb-1.5">
              تصویر کارت ملی صاحب هاب <span className="text-red-400">*</span>
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

          {/* Storefront Image */}
          <div>
            <label className="block text-xs font-semibold text-gray-300 mb-1.5">
              یک تصویر از مغازه / تابلو <span className="text-red-400">*</span>
            </label>
            {storeImage ? (
              <div className="relative w-full h-28 rounded-xl overflow-hidden border border-amber-400">
                <img src={storeImage} alt="تصویر مغازه" className="w-full h-full object-cover" />
                <button type="button" onClick={() => setStoreImage(null)} className="absolute top-1 right-1 bg-black/80 text-white p-1 rounded-full">
                  <span className="material-symbols-outlined text-xs">close</span>
                </button>
              </div>
            ) : (
              <label className={`border-2 border-dashed rounded-xl p-3 flex flex-col items-center justify-center gap-1 cursor-pointer ${
                isLuxury ? 'border-amber-400/30 bg-[#161616]' : 'border-blue-300 bg-blue-50/50'
              }`}>
                <span className="material-symbols-outlined text-2xl text-[#fd761a]">storefront</span>
                <span className="text-[11px]">آپلود تصویر مغازه</span>
                <input type="file" accept="image/*" className="hidden" onChange={handleStoreImageUpload} />
              </label>
            )}
          </div>
        </div>

        {/* Exact Address */}
        <div>
          <label className="block text-xs font-semibold text-gray-300 mb-1.5">
            آدرس دقیق هاب <span className="text-red-400">*</span>
          </label>
          <textarea
            required
            rows={2}
            value={exactAddress}
            onChange={(e) => setExactAddress(e.target.value)}
            placeholder="تهران، منطقه، خیابان اصلی، پلاک، نام فروشگاه..."
            className={`w-full px-3.5 py-2.5 rounded-xl border text-xs outline-none ${
              isLuxury ? 'bg-[#181818] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900 focus:border-blue-500'
            }`}
          />
        </div>

        {/* Interactive Map Pin Selector for Hub */}
        <div>
          <div className="flex justify-between items-center mb-1.5">
            <label className="text-xs font-semibold text-gray-300">
              انتخاب لوکیشن دقیق روی نقشه <span className="text-red-400">*</span>
            </label>
            <span className="text-[10px] text-amber-400 font-mono">
              مختصات: {storeLocation.lat.toFixed(4)}, {storeLocation.lng.toFixed(4)}
            </span>
          </div>

          <div className="relative w-full h-44 rounded-xl overflow-hidden border border-white/10 group cursor-crosshair">
            <img
              src="https://lh3.googleusercontent.com/aida-public/AB6AXuAqiO6yEkvWRyaYQiyjSz91lp3e60pXZYQ87va8pyebpCB2f1oBjXL1nJLAY-WU5JMK47816jbaRI3zS6lTT3Xy0cn6dtR8ZHaUboKG2j8PRBAYP757HgNetGJWwZAf9j52KA0sUeRZFidKJ39Gg3vVuL01DEOuS7iqNbirmnv6070O-BoJLp1i0Pw9VIrvibE6LDDJpPGzf38H1raAwDH3i5ebdv_i3CH0vUpSQDcjNr-trOlQYm2W9g"
              alt="نقشه انتخاب لوکیشن"
              className="w-full h-full object-cover grayscale opacity-70"
            />
            <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
              <div className="flex flex-col items-center animate-bounce">
                <span className="material-symbols-outlined text-4xl text-[#d4af37] drop-shadow-[0_4px_8px_rgba(0,0,0,0.8)]">
                  location_on
                </span>
                <span className="bg-black/90 text-[#f2ca50] text-[10px] px-2 py-0.5 rounded-full border border-amber-400 font-bold shadow-lg">
                  محل هاب شما
                </span>
              </div>
            </div>
            <div className="absolute bottom-2 right-2 bg-black/80 px-2.5 py-1 rounded-lg text-[10px] text-gray-300 border border-white/10">
              کلیک برای جابجایی دقیق پین
            </div>
          </div>
        </div>

        {/* Working Hours for Weekdays */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div>
            <label className="block text-[11px] font-semibold text-gray-300 mb-1">
              شنبه تا چهارشنبه <span className="text-red-400">*</span>
            </label>
            <input
              type="text"
              required
              value={workingHoursWeekday}
              onChange={(e) => setWorkingHoursWeekday(e.target.value)}
              className={`w-full px-3 py-2 rounded-xl border text-xs ${
                isLuxury ? 'bg-[#181818] border-white/10 text-white' : 'bg-gray-50 border-gray-300 text-gray-800'
              }`}
            />
          </div>

          <div>
            <label className="block text-[11px] font-semibold text-gray-300 mb-1">
              پنج‌شنبه‌ها <span className="text-red-400">*</span>
            </label>
            <input
              type="text"
              required
              value={workingHoursThursday}
              onChange={(e) => setWorkingHoursThursday(e.target.value)}
              className={`w-full px-3 py-2 rounded-xl border text-xs ${
                isLuxury ? 'bg-[#181818] border-white/10 text-white' : 'bg-gray-50 border-gray-300 text-gray-800'
              }`}
            />
          </div>

          <div>
            <label className="block text-[11px] font-semibold text-gray-300 mb-1">
              جمعه و ایام تعطیل <span className="text-red-400">*</span>
            </label>
            <input
              type="text"
              required
              value={workingHoursFriday}
              onChange={(e) => setWorkingHoursFriday(e.target.value)}
              className={`w-full px-3 py-2 rounded-xl border text-xs ${
                isLuxury ? 'bg-[#181818] border-white/10 text-white' : 'bg-gray-50 border-gray-300 text-gray-800'
              }`}
            />
          </div>
        </div>

        {/* Services Description */}
        <div>
          <label className="block text-xs font-semibold text-gray-300 mb-1.5">
            شرح خدمات قابل ارائه در هاب محله <span className="text-red-400">*</span>
          </label>
          <textarea
            required
            rows={2}
            value={servicesDescription}
            onChange={(e) => setServicesDescription(e.target.value)}
            placeholder="مثال: انبارداری موقت، پرینت لیبل، تایید با بارکدخوان، نگهداری مرسولات حجیم و سردخانه‌ای..."
            className={`w-full px-3.5 py-2.5 rounded-xl border text-xs outline-none ${
              isLuxury ? 'bg-[#181818] border-white/10 text-white focus:border-[#d4af37]' : 'bg-gray-50 border-gray-300 text-gray-900 focus:border-blue-500'
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
            <span>تکمیل ثبت نام هاب و ارسال کد تایید (۲FA)</span>
            <span className="material-symbols-outlined text-base">arrow_back</span>
          </button>
        </div>

      </form>

    </div>
  );
};
