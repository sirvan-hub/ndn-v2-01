import React from 'react';
import { useApp } from '../../context/AppContext';
import { LiveUpdateBanner } from '../common/LiveUpdateBanner';
import { DriveQuickSyncButton } from '../drive/DriveQuickSyncButton';

export const CustomerView: React.FC = () => {
  const {
    packages,
    selectedHub,
    openScanner,
    openPhotoModal,
    payPackageFee,
    setActiveTab,
    theme
  } = useApp();

  const isLuxury = theme === 'luxury';

  // Primary active package
  const activePackage = packages[0] || null;

  return (
    <div className="w-full max-w-7xl mx-auto space-y-6 animate-in fade-in duration-200">
      
      {/* Top Notification Banner */}
      <LiveUpdateBanner />

      {/* Main Grid: Packages List (8 cols) + Hub info & Payment (4 cols) */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-start">
        
        {/* Main Content Area (8 Columns on desktop) */}
        <div className="md:col-span-8 space-y-6">
          
          <section className={`p-5 md:p-6 rounded-2xl border transition-colors shadow-sm ${
            isLuxury ? 'bg-[#0e0e0e] border-[#353535]' : 'bg-white border-[#c5c5d3]'
          }`}>
            <div className="flex items-center justify-between mb-4 border-b pb-3 border-gray-700/20">
              <h2 className={`text-lg md:text-xl font-bold ${isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'}`}>
                بسته‌های در حال ارسال
              </h2>
              <span className={`text-xs font-mono px-2 py-0.5 rounded-full ${
                isLuxury ? 'bg-amber-400/10 text-[#f2ca50]' : 'bg-blue-100 text-blue-800'
              }`}>
                {packages.length} بسته فعال
              </span>
            </div>

            <div className="space-y-4">
              {packages.map((pkg, idx) => {
                const isFirst = idx === 0;
                const isAtHub = pkg.status === 'at_hub';

                return (
                  <div
                    key={pkg.id}
                    className={`relative rounded-xl border p-4 md:p-5 flex flex-col md:flex-row justify-between gap-4 overflow-hidden transition-all ${
                      isLuxury
                        ? 'bg-[#131313] border-[#353535] hover:border-[#d4af37]/50'
                        : 'bg-[#f8f9ff] border-gray-200 hover:border-blue-400'
                    } ${!isFirst && !isAtHub ? 'opacity-85' : ''}`}
                  >
                    {/* Status side bar stripe */}
                    <div className={`absolute top-0 right-0 h-full w-2 ${
                      isAtHub 
                        ? (isLuxury ? 'bg-[#d4af37]' : 'bg-[#fd761a]') 
                        : 'bg-gray-600'
                    }`} />

                    {/* Right side in RTL: Details */}
                    <div className="flex flex-col gap-2 pr-3 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className={`text-xs px-2.5 py-0.5 rounded-full font-semibold border ${
                          isAtHub
                            ? (isLuxury ? 'bg-[#1f1f1f] text-[#f2ca50] border-[#d4af37]/40' : 'bg-orange-100 text-orange-800 border-orange-200')
                            : (isLuxury ? 'bg-gray-800 text-gray-400 border-gray-700' : 'bg-gray-100 text-gray-600 border-gray-200')
                        }`}>
                          {pkg.statusText}
                        </span>
                        <span className="text-xs font-mono text-gray-400 tracking-wider">
                          {pkg.trackingCode}
                        </span>
                        <span className="text-[11px] text-amber-500 mr-auto flex items-center gap-1">
                          <span className="w-2 h-2 bg-amber-400 rounded-full animate-pulse"></span>
                          <span>آخرین به‌روزرسانی: {pkg.lastUpdated}</span>
                        </span>
                      </div>

                      <h3 className="text-base md:text-lg font-bold text-white mt-1">
                        {pkg.title}
                      </h3>
                      <p className="text-xs text-gray-400">
                        فرستنده: <span className="text-gray-300 font-medium">{pkg.sender}</span> | گیرنده: <span className="text-gray-300 font-medium">{pkg.receiver}</span>
                      </p>
                      <p className="text-xs text-gray-400">
                        هاب تحویل: <span className="text-amber-400/90 font-medium">{pkg.hubName}</span>
                      </p>

                      {/* Google Drive Quick Sync */}
                      <div className="mt-1">
                        <DriveQuickSyncButton pkg={pkg} isLuxury={isLuxury} />
                      </div>

                      {/* Photo verification badge if uploaded */}
                      {pkg.photoUrl && (
                        <div className="mt-2 flex items-center gap-2 text-xs text-emerald-400 bg-emerald-950/40 p-2 rounded-lg border border-emerald-500/30">
                          <span className="material-symbols-outlined text-base">verified</span>
                          <span>عکس تحویل توسط هاب ثبت و تایید شده است.</span>
                        </div>
                      )}
                    </div>

                    {/* Left side in RTL: Viewfinder preview & Action Buttons */}
                    <div className="flex flex-col items-end justify-between gap-3 md:w-72">
                      {isAtHub && (
                        <div className="relative w-full h-28 bg-black rounded-lg overflow-hidden border-2 border-amber-400/80 shadow-md flex items-center justify-center">
                          <div className="absolute inset-0 opacity-40 bg-[radial-gradient(circle,transparent_20%,black_20%,black_80%,transparent_80%,transparent)] bg-[length:20px_20px]"></div>
                          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-16 h-16 border-2 border-amber-400/50 rounded-lg"></div>
                          <div className="absolute top-0 left-0 w-full h-0.5 bg-amber-400 animate-pulse shadow-[0_0_10px_#d4af37]"></div>
                          <span className="material-symbols-outlined text-amber-400/30 text-4xl">
                            qr_code_scanner
                          </span>
                        </div>
                      )}

                      <p className="text-xs text-gray-400 text-right w-full">
                        ابعاد: {pkg.dimensions} | وزن: {pkg.weight}
                      </p>

                      <div className="flex flex-col gap-2 w-full">
                        {isAtHub ? (
                          <>
                            <button
                              onClick={() => openPhotoModal(pkg)}
                              className={`w-full py-2 px-3 rounded-lg border font-semibold text-xs flex items-center justify-center gap-1.5 transition-colors ${
                                isLuxury
                                  ? 'border-[#d4af37] text-[#f2ca50] hover:bg-[#d4af37]/10'
                                  : 'border-[#1e3a8a] text-[#1e3a8a] hover:bg-blue-50'
                              }`}
                            >
                              <span className="material-symbols-outlined text-base">add_a_photo</span>
                              <span>آپلود عکس تحویل</span>
                            </button>

                            <button
                              onClick={() => openScanner('customer_confirm', pkg)}
                              className={`w-full py-2.5 px-3 rounded-lg font-bold text-xs flex items-center justify-center gap-1.5 shadow-md transition-all active:scale-95 ${
                                isLuxury
                                  ? 'bg-[#d4af37] text-black hover:bg-[#e9c349]'
                                  : 'bg-[#1e3a8a] text-white hover:bg-[#00236f]'
                              }`}
                            >
                              <span className="material-symbols-outlined text-base">qr_code_scanner</span>
                              <span>تایید و اسکن (اتصال آنی به هاب)</span>
                            </button>
                          </>
                        ) : (
                          <button
                            disabled
                            className="w-full py-2.5 px-3 rounded-lg font-medium text-xs bg-gray-800/60 text-gray-500 border border-gray-700 cursor-not-allowed text-center"
                          >
                            تایید دریافت (بسته در مسیر است)
                          </button>
                        )}
                      </div>
                    </div>

                  </div>
                );
              })}
            </div>
          </section>

        </div>

        {/* Sidebar Section (4 Columns on desktop) */}
        <div className="md:col-span-4 space-y-6">
          
          {/* Map Preview Widget (Matching Screen 1 & 2) */}
          <section className={`p-4 md:p-5 rounded-2xl border transition-colors shadow-sm ${
            isLuxury ? 'bg-[#0e0e0e] border-[#353535]' : 'bg-white border-[#c5c5d3]'
          }`}>
            <h3 className={`text-base font-bold mb-3 flex items-center gap-1.5 ${
              isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'
            }`}>
              <span className="material-symbols-outlined text-xl">location_on</span>
              <span>هاب محلی شما</span>
            </h3>

            {/* Map Preview Image with Pin */}
            <div className="w-full h-44 rounded-xl overflow-hidden relative border border-white/10 shadow-inner group cursor-pointer" onClick={() => setActiveTab('map')}>
              <img
                src="https://lh3.googleusercontent.com/aida-public/AB6AXuAqiO6yEkvWRyaYQiyjSz91lp3e60pXZYQ87va8pyebpCB2f1oBjXL1nJLAY-WU5JMK47816jbaRI3zS6lTT3Xy0cn6dtR8ZHaUboKG2j8PRBAYP757HgNetGJWwZAf9j52KA0sUeRZFidKJ39Gg3vVuL01DEOuS7iqNbirmnv6070O-BoJLp1i0Pw9VIrvibE6LDDJpPGzf38H1raAwDH3i5ebdv_i3CH0vUpSQDcjNr-trOlQYm2W9g"
                alt="نقشه هاب محله"
                className="w-full h-full object-cover grayscale opacity-75 group-hover:scale-105 transition-transform duration-300"
              />
              <div className="absolute inset-0 bg-black/20 group-hover:bg-black/0 transition-colors flex items-center justify-center">
                <div className="p-2 bg-black/80 rounded-full border border-amber-400/60 shadow-lg text-amber-400 flex items-center gap-1">
                  <span className="material-symbols-outlined text-lg">storefront</span>
                  <span className="text-[11px] font-bold px-1">{selectedHub.name}</span>
                </div>
              </div>
            </div>

            <p className="text-xs text-gray-300 mt-3 text-right leading-relaxed">
              {selectedHub.address}
            </p>
            <div className="text-[11px] text-gray-400 mt-1 flex items-center justify-between">
              <span>ساعات کاری: {selectedHub.workingHours}</span>
              <span className="text-emerald-400 font-medium">● باز است</span>
            </div>

            <div className="flex flex-col gap-2 mt-4">
              <button
                onClick={() => setActiveTab('map')}
                className={`w-full py-2 px-3 rounded-lg border font-semibold text-xs transition-colors ${
                  isLuxury
                    ? 'border-[#d4af37] text-[#f2ca50] hover:bg-[#d4af37]/10'
                    : 'border-[#1e3a8a] text-[#1e3a8a] hover:bg-blue-50'
                }`}
              >
                مسیریابی
              </button>
              <button
                onClick={() => setActiveTab('map')}
                className={`w-full py-2 px-3 rounded-lg border font-semibold text-xs flex items-center justify-center gap-1.5 transition-colors ${
                  isLuxury
                    ? 'border-gray-700 text-gray-300 hover:bg-white/5'
                    : 'border-gray-300 text-gray-700 hover:bg-gray-100'
                }`}
              >
                <span className="material-symbols-outlined text-sm">map</span>
                <span>مشاهده هاب‌های نزدیک</span>
              </button>
            </div>
          </section>

          {/* Payment Fee Breakdown (Matching Screen 1 & 2) */}
          <section className={`p-4 md:p-5 rounded-2xl border transition-colors shadow-sm ${
            isLuxury ? 'bg-[#0e0e0e] border-[#353535]' : 'bg-white border-[#c5c5d3]'
          }`}>
            <h3 className={`text-base font-bold mb-3 flex items-center gap-1.5 ${
              isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'
            }`}>
              <span className="material-symbols-outlined text-xl">payments</span>
              <span>هزینه ارسال</span>
            </h3>

            <div className="space-y-2.5 text-xs text-gray-200">
              <div className="flex justify-between items-center py-1 border-b border-gray-800">
                <span className="text-gray-400">هزینه پایه</span>
                <span className="font-semibold font-mono">
                  {activePackage ? activePackage.baseFee.toLocaleString('fa-IR') : '۲۵,۰۰۰'} تومان
                </span>
              </div>
              <div className="flex justify-between items-center py-1 border-b border-gray-800">
                <span className="text-gray-400">مالیات (۹٪)</span>
                <span className="font-semibold font-mono">
                  {activePackage ? activePackage.tax.toLocaleString('fa-IR') : '۲,۲۵۰'} تومان
                </span>
              </div>
              <div className="flex justify-between items-center py-1">
                <span className="text-gray-400">هزینه نگهداری در هاب</span>
                <span className="font-semibold font-mono">
                  {activePackage ? activePackage.storageFee.toLocaleString('fa-IR') : '۰'} تومان
                </span>
              </div>

              <div className="flex justify-between items-center pt-3 border-t border-gray-700/60 text-sm font-bold">
                <div className="flex items-center gap-1 text-[#f2ca50]">
                  <span>مجموع</span>
                  <span 
                    className="material-symbols-outlined text-sm cursor-pointer text-gray-400 hover:text-amber-400"
                    title="قیمت بر اساس سایز بسته و مدت زمان توقف در شبکه توزیع محلی (PUDO) محاسبه شده است."
                  >
                    info
                  </span>
                </div>
                <span className={`text-base font-mono ${isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'}`}>
                  {activePackage ? activePackage.totalFee.toLocaleString('fa-IR') : '۲۷,۲۵۰'} تومان
                </span>
              </div>
            </div>

            <button
              onClick={() => activePackage && payPackageFee(activePackage.id)}
              disabled={activePackage?.isPaid}
              className={`w-full mt-4 py-2.5 px-4 rounded-xl font-bold text-xs shadow-md transition-all active:scale-95 ${
                activePackage?.isPaid
                  ? 'bg-emerald-600 text-white cursor-default'
                  : isLuxury
                    ? 'bg-[#d4af37] text-black hover:bg-[#e9c349]'
                    : 'bg-[#1e3a8a] text-white hover:bg-[#00236f]'
              }`}
            >
              {activePackage?.isPaid ? '✓ هزینه با موفقیت پرداخت شد' : 'پرداخت آنلاین'}
            </button>
          </section>

        </div>

      </div>

    </div>
  );
};
