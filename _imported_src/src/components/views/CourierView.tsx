import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import { PackageSize } from '../../types';

export const CourierView: React.FC = () => {
  const {
    openScanner,
    registerNewPackage,
    selectedHub,
    packages,
    theme
  } = useApp();

  const [selectedSize, setSelectedSize] = useState<PackageSize>('medium');
  const [hasCustomerRequest, setHasCustomerRequest] = useState(true);

  const isLuxury = theme === 'luxury';

  const courierParcels = [
    {
      id: 'cp-1',
      code: 'IR-987654321',
      sender: 'علی رضایی',
      hubDestination: 'هاب مرکزی غرب (سعادت‌آباد)',
      status: 'در حال انتقال',
      step: 2,
    },
    {
      id: 'cp-2',
      code: 'IR-123456789',
      sender: 'فروشگاه آنلاین',
      hubDestination: 'هاب شمال (سوپرمارکت یاران)',
      status: 'در حال انتقال',
      step: 2,
    },
    {
      id: 'cp-3',
      code: 'IR-554433221',
      sender: 'دیجی‌کالا انبار مرکزی',
      hubDestination: 'کافی‌نت دیجیتال پلاس',
      status: 'در دست سفیر',
      step: 1,
    }
  ];

  const handleQuickScanCourier = () => {
    openScanner('courier_register');
  };

  return (
    <div className="w-full max-w-7xl mx-auto space-y-6 animate-in fade-in duration-200">
      
      {/* Top Bento Metrics & Quick Register Banner (Matching Screen 4 & 5) */}
      <section className="grid grid-cols-2 md:grid-cols-4 gap-4">
        
        {/* Metric 1: Deliveries today */}
        <div className={`p-5 rounded-2xl border flex flex-col items-center justify-center text-center shadow-sm ${
          isLuxury ? 'bg-[#0e0e0e] border-[#353535]' : 'bg-white border-blue-200'
        }`}>
          <span className={`text-3xl md:text-4xl font-bold font-mono ${
            isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'
          }`}>
            ۲۴
          </span>
          <span className="text-xs text-gray-400 mt-1">تحویل امروز</span>
        </div>

        {/* Metric 2: Pending in hand */}
        <div className={`p-5 rounded-2xl border flex flex-col items-center justify-center text-center shadow-sm ${
          isLuxury ? 'bg-[#0e0e0e] border-[#353535]' : 'bg-white border-blue-200'
        }`}>
          <span className={`text-3xl md:text-4xl font-bold font-mono ${
            isLuxury ? 'text-[#fd761a]' : 'text-[#fd761a]'
          }`}>
            ۱۲
          </span>
          <span className="text-xs text-gray-400 mt-1">در دست اقدام</span>
        </div>

        {/* Metric 3: Weekly Earnings & Stats */}
        <div className={`col-span-2 p-4 md:p-5 rounded-2xl border flex flex-col justify-between shadow-sm ${
          isLuxury ? 'bg-[#1a1a1a] border-[#353535]' : 'bg-blue-50/70 border-blue-200'
        }`}>
          <div className="flex justify-between items-center mb-2">
            <h3 className="text-xs font-bold text-gray-400">عملکرد و درآمد هفتگی</h3>
            <div className="flex items-center gap-1 text-emerald-400 text-xs font-bold font-mono">
              <span className="material-symbols-outlined text-sm">trending_up</span>
              <span>۱۲٪+</span>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col text-right">
              <span className="text-[11px] text-gray-400">درآمد خالص (تومان)</span>
              <span className={`text-lg md:text-xl font-bold font-mono ${
                isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'
              }`}>
                ۱,۲۶۰,۰۰۰
              </span>
            </div>
            <div className="flex flex-col text-right border-r border-gray-700/40 pr-4">
              <span className="text-[11px] text-gray-400">بسته‌های ثبت شده</span>
              <span className="text-lg md:text-xl font-bold text-white font-mono">
                ۸۴
              </span>
            </div>
          </div>
        </div>

        {/* Big Action Button: Register Package for Hub */}
        <div
          onClick={handleQuickScanCourier}
          className={`col-span-2 md:col-span-4 p-4 rounded-2xl shadow-lg flex items-center justify-center gap-3 cursor-pointer transition-all active:scale-95 ${
            isLuxury
              ? 'bg-[#d4af37] text-black hover:bg-[#e9c349]'
              : 'bg-[#1e3a8a] text-white hover:bg-[#00236f]'
          }`}
        >
          <span className="material-symbols-outlined text-3xl">add_box</span>
          <span className="text-base md:text-lg font-bold">ثبت بسته برای هاب</span>
        </div>

      </section>

      {/* Main Grid: Parcels In Hand (8 cols) + Quick Scan Panel (4 cols) */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-start">
        
        {/* Parcels in hand (8 cols) */}
        <div className="md:col-span-8 space-y-4">
          <section className={`p-5 rounded-2xl border shadow-sm ${
            isLuxury ? 'bg-[#0e0e0e] border-[#353535]' : 'bg-white border-blue-200'
          }`}>
            <div className="flex items-center justify-between border-b pb-3 mb-4 border-gray-700/30">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-amber-500">local_shipping</span>
                <h2 className="text-base md:text-lg font-bold text-white">
                  بسته‌های در دست (برای هاب)
                </h2>
              </div>
              <span className="text-xs font-mono text-gray-400">
                {courierParcels.length} مرسوله در مسیر
              </span>
            </div>

            <div className="space-y-4">
              {courierParcels.map((parcel) => (
                <div
                  key={parcel.id}
                  className={`p-4 rounded-xl border flex flex-col gap-3 transition-all ${
                    isLuxury ? 'bg-[#131313] border-[#353535] hover:border-[#d4af37]/40' : 'bg-gray-50 border-gray-200'
                  }`}
                >
                  <div className="flex justify-between items-center">
                    <span className="text-xs font-mono text-gray-400 tracking-wider">
                      {parcel.code}
                    </span>
                    <span className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-amber-500/15 text-amber-400 border border-amber-500/30">
                      {parcel.status}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-2 text-xs">
                    <div>
                      <span className="text-gray-400 block text-[11px]">فرستنده:</span>
                      <span className="font-semibold text-white">{parcel.sender}</span>
                    </div>
                    <div>
                      <span className="text-gray-400 block text-[11px]">هاب مقصد:</span>
                      <span className="font-semibold text-amber-400/90">{parcel.hubDestination}</span>
                    </div>
                  </div>

                  {/* Delivery Progress Tracker Line (Matching Screen 4 & 5) */}
                  <div className="pt-2 border-t border-gray-800 flex items-center justify-between px-2">
                    <div className="flex flex-col items-center">
                      <div className="w-3.5 h-3.5 rounded-full bg-[#d4af37] ring-2 ring-amber-400/30" />
                      <span className="text-[9px] text-gray-400 mt-1">مبدا</span>
                    </div>
                    <div className="h-0.5 flex-1 bg-[#d4af37] mx-1" />
                    <div className="flex flex-col items-center">
                      <div className="w-3.5 h-3.5 rounded-full bg-orange-500 ring-4 ring-orange-500/30 animate-pulse" />
                      <span className="text-[9px] text-orange-400 mt-1">سفیر</span>
                    </div>
                    <div className="h-0.5 flex-1 bg-gray-700 mx-1" />
                    <div className="flex flex-col items-center">
                      <div className="w-3.5 h-3.5 rounded-full bg-gray-700" />
                      <span className="text-[9px] text-gray-500 mt-1">هاب محله</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>

        {/* Quick Scan Area (4 cols on desktop) */}
        <div className="md:col-span-4 space-y-4">
          <section className={`p-5 rounded-2xl border shadow-sm ${
            isLuxury ? 'bg-[#0e0e0e] border-[#353535]' : 'bg-white border-blue-200'
          }`}>
            <h3 className={`text-base font-bold mb-3 ${isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'}`}>
              ثبت سریع بسته (اسکن بارکد)
            </h3>

            {/* Simulated Live Viewfinder */}
            <div 
              onClick={handleQuickScanCourier}
              className="relative w-full aspect-square rounded-xl bg-black border-2 border-dashed border-amber-400/70 overflow-hidden flex items-center justify-center cursor-pointer group shadow-inner"
            >
              <span className="material-symbols-outlined text-6xl text-gray-600 group-hover:text-amber-400 transition-colors">
                qr_code_scanner
              </span>
              
              {/* Laser Scan line */}
              <div className="absolute w-full h-1 bg-orange-400/80 shadow-[0_0_12px_#fd761a] animate-scan-line"></div>

              {/* Photo pill */}
              <div className="absolute top-2 right-2 bg-amber-500 text-black px-2 py-0.5 rounded-full text-[10px] font-bold flex items-center gap-1">
                <span className="material-symbols-outlined text-xs">photo_camera</span>
                <span>ثبت خودکار تصویر فعال است</span>
              </div>
            </div>

            {/* Real-time Sync Indicator */}
            <div className="mt-3 flex items-center justify-center gap-1.5 py-1.5 px-3 rounded-full bg-white/5 border border-white/10 text-xs">
              <span className="material-symbols-outlined text-amber-400 text-sm">verified</span>
              <span className="text-amber-400/90 text-[11px]">همگام‌سازی لحظه‌ای با پنل هاب و مشتری فعال است</span>
            </div>

            <p className="text-xs text-gray-400 text-center my-3">
              بارکد روی بسته را در کادر بالا قرار دهید تا به صورت خودکار شناسایی شود.
            </p>

            {/* Size Selector Buttons (Matching Screen 4 & 5) */}
            <div className="mb-4">
              <label className="block text-xs text-gray-400 mb-1.5">سایز بسته (اجباری)</label>
              <div className="grid grid-cols-3 gap-2">
                {[
                  { id: 'small', label: 'کوچک', icon: 'mail' },
                  { id: 'medium', label: 'متوسط', icon: 'inventory_2' },
                  { id: 'large', label: 'بزرگ', icon: 'package_2' }
                ].map((sz) => (
                  <button
                    key={sz.id}
                    onClick={() => setSelectedSize(sz.id as PackageSize)}
                    className={`flex flex-col items-center justify-center p-2 rounded-xl border text-xs transition-all ${
                      selectedSize === sz.id
                        ? (isLuxury ? 'border-[#d4af37] bg-amber-400/15 text-[#f2ca50] font-bold' : 'border-blue-700 bg-blue-50 text-blue-900 font-bold')
                        : (isLuxury ? 'border-gray-800 bg-[#131313] text-gray-400 hover:border-gray-700' : 'border-gray-200 bg-white text-gray-700')
                    }`}
                  >
                    <span className="material-symbols-outlined text-base mb-0.5">{sz.icon}</span>
                    <span>{sz.label}</span>
                  </button>
                ))}
              </div>
              <p className="text-[10px] text-gray-500 mt-1.5 leading-relaxed">
                قیمت نهایی بر اساس سایز انتخابی و مدت زمان نگهداری در هاب محاسبه خواهد شد.
              </p>
            </div>

            {/* Customer match pill */}
            <div className={`p-3 rounded-xl border text-xs flex flex-col gap-1 mb-4 ${
              isLuxury ? 'bg-[#161616] border-amber-400/30' : 'bg-blue-50 border-blue-200'
            }`}>
              <div className="flex items-center gap-1.5 text-amber-400 font-bold">
                <span className="material-symbols-outlined text-sm">check_circle</span>
                <span>درخواست مشتری یافت شد (آماده ثبت)</span>
              </div>
              <p className="text-[10px] text-gray-400">
                پس از اسکن، درخواست تایید برای مشتری ارسال خواهد شد.
              </p>
            </div>

            {/* Action Button */}
            <button
              onClick={handleQuickScanCourier}
              className={`w-full py-3 px-4 rounded-xl font-bold text-xs flex items-center justify-center gap-2 shadow-md transition-all active:scale-95 ${
                isLuxury
                  ? 'bg-[#fd761a] text-white hover:bg-orange-600'
                  : 'bg-[#fd761a] text-white hover:bg-orange-600'
              }`}
            >
              <span className="material-symbols-outlined text-base">qr_code_scanner</span>
              <span className="material-symbols-outlined text-base">send</span>
              <span>اسکن و ارسال درخواست تایید / ثبت نهایی</span>
            </button>
          </section>
        </div>

      </div>

    </div>
  );
};
