import React from 'react';
import { useApp } from '../../context/AppContext';

export const HubManagerView: React.FC = () => {
  const {
    selectedHub,
    toggleHubStatus,
    openScanner,
    activityLogs,
    theme
  } = useApp();

  const isLuxury = theme === 'luxury';

  const inventoryItems = [
    {
      id: 'inv-1',
      code: 'IR-1049-X72',
      receiver: 'علی احمدی',
      dimensions: 'متوسط - ۲ کیلوگرم',
      daysInHub: 2,
      isOverdue: false,
      statusLabel: '۲ روز در انبار',
    },
    {
      id: 'inv-2',
      code: 'IR-8821-M14',
      receiver: 'سارا محمدی',
      dimensions: 'کوچک - ۰.۵ کیلوگرم',
      daysInHub: 5,
      isOverdue: true,
      statusLabel: '۵ روز در انبار (نیاز به پیگیری)',
    },
    {
      id: 'inv-3',
      code: 'TRK-987654321',
      receiver: 'علی رضایی',
      dimensions: 'متوسط - ۱.۲ کیلوگرم',
      daysInHub: 1,
      isOverdue: false,
      statusLabel: 'امروز تحویل گرفته شد',
    }
  ];

  return (
    <div className="w-full max-w-7xl mx-auto space-y-6 animate-in fade-in duration-200">
      
      {/* Activity Log Banner (Matching Screen 8) */}
      <section className={`p-4 rounded-xl border-r-4 border-[#d4af37] border flex flex-col gap-2 shadow-sm ${
        isLuxury ? 'bg-[#181818] border-[#353535] text-gray-200' : 'bg-white border-blue-200 text-gray-800'
      }`}>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-amber-500 text-xl">history</span>
            <h3 className="text-xs font-bold">لاگ فعالیت‌ها</h3>
          </div>
          <div className="flex items-center gap-1.5 text-[11px] text-gray-400">
            <span className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse"></span>
            <span>همگام‌سازی شده</span>
          </div>
        </div>

        <div className="flex items-start gap-2.5 text-xs text-right pr-2">
          <div className="w-1.5 h-1.5 rounded-full bg-amber-400 mt-1.5 flex-shrink-0"></div>
          <div>
            <p className="font-medium text-white">
              {activityLogs[0]?.text || 'بسته جدید توسط مامور پست ثبت شد - کد: TRK-987654321'}
            </p>
            <p className="text-[10px] text-gray-400 mt-0.5">
              {activityLogs[0]?.timestamp || 'هم‌اکنون'} • از طریق اپلیکیشن مامور
            </p>
          </div>
        </div>
      </section>

      {/* Header & Status Toggle (Matching Screen 8) */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h2 className={`text-2xl font-bold ${isLuxury ? 'text-white' : 'text-[#00236f]'}`}>
            {selectedHub.name} (هاب توزیع محله)
          </h2>
          <p className="text-xs text-gray-400 mt-1">
            مدیریت موجودی، دریافت از سفیران و تحویل به همسایگان محله
          </p>
        </div>

        {/* Hub Open/Close Switch */}
        <div className={`flex items-center gap-3 px-4 py-2 rounded-xl border shadow-sm ${
          isLuxury ? 'bg-[#1f1f1f] border-[#353535]' : 'bg-white border-gray-200'
        }`}>
          <span className="text-xs font-medium text-gray-300">وضعیت هاب:</span>
          <button
            onClick={() => toggleHubStatus(selectedHub.id)}
            className="flex items-center gap-2"
          >
            <div className={`w-12 h-6 rounded-full p-0.5 transition-colors relative flex items-center ${
              selectedHub.isOpen ? 'bg-[#d4af37]' : 'bg-gray-700'
            }`}>
              <div className={`w-5 h-5 rounded-full bg-white transition-transform ${
                selectedHub.isOpen ? 'translate-x-0' : '-translate-x-6'
              }`} />
            </div>
            <span className={`text-xs font-bold font-mono ${
              selectedHub.isOpen ? 'text-[#f2ca50]' : 'text-gray-400'
            }`}>
              {selectedHub.isOpen ? 'باز است' : 'بسته است'}
            </span>
          </button>
        </div>
      </div>

      {/* Weekly Earnings & Turnover (Matching Screen 8) */}
      <section className={`p-5 rounded-2xl border shadow-sm ${
        isLuxury ? 'bg-[#0e0e0e] border-[#353535]' : 'bg-white border-blue-200'
      }`}>
        <div className="flex items-center gap-2 mb-4">
          <span className="material-symbols-outlined text-amber-500">payments</span>
          <h3 className="text-sm font-bold text-white">عملکرد و درآمد هفتگی هاب</h3>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div className={`p-4 rounded-xl border-r-4 border-[#d4af37] ${
            isLuxury ? 'bg-[#1b1b1b]' : 'bg-blue-50/60'
          }`}>
            <p className="text-[11px] text-gray-400 mb-1">کل جابجایی‌ها</p>
            <p className={`text-3xl font-bold font-mono ${isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'}`}>
              ۱۵۶
            </p>
          </div>

          <div className={`p-4 rounded-xl border-r-4 border-[#fd761a] ${
            isLuxury ? 'bg-[#1b1b1b]' : 'bg-orange-50/60'
          }`}>
            <p className="text-[11px] text-gray-400 mb-1">سهم درآمد هاب (تومان)</p>
            <p className="text-2xl md:text-3xl font-bold text-[#fd761a] font-mono">
              ۳,۱۲۰,۰۰۰
            </p>
          </div>
        </div>
      </section>

      {/* Fast Scan Viewfinder Section (Matching Screen 8) */}
      <section className={`p-6 rounded-2xl border-2 border-dashed border-[#d4af37]/40 flex flex-col items-center gap-5 ${
        isLuxury ? 'bg-[#131313]' : 'bg-blue-50/40'
      }`}>
        <div className="text-center">
          <h3 className={`text-base font-bold ${isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'}`}>
            اسکن سریع هاب
          </h3>
          <p className="text-xs text-gray-400 mt-1">
            برای دریافت بسته از مامور یا تحویل به مشتری، کد QR را اسکن کنید
          </p>
        </div>

        {/* Viewfinder Target */}
        <div className="relative w-full max-w-sm aspect-square bg-black rounded-xl overflow-hidden flex items-center justify-center border border-white/10 shadow-2xl">
          <div className="absolute top-4 left-4 w-8 h-8 border-t-4 border-l-4 border-amber-400"></div>
          <div className="absolute top-4 right-4 w-8 h-8 border-t-4 border-r-4 border-amber-400"></div>
          <div className="absolute bottom-4 left-4 w-8 h-8 border-b-4 border-l-4 border-amber-400"></div>
          <div className="absolute bottom-4 right-4 w-8 h-8 border-b-4 border-r-4 border-amber-400"></div>

          <span className="material-symbols-outlined text-6xl text-white/30">
            qr_code_scanner
          </span>

          <div className="absolute left-0 right-0 h-0.5 bg-amber-400 shadow-[0_0_12px_#d4af37] animate-scan-line"></div>

          <div className="absolute bottom-4 inset-x-4 text-center">
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-black/70 border border-white/20 text-[10px] text-gray-300">
              <span className="material-symbols-outlined text-xs">photo_camera</span>
              <span>ثبت خودکار تصویر در لحظه اسکن</span>
            </div>
          </div>
        </div>

        {/* Receive & Deliver Scan Action Buttons */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 w-full max-w-sm">
          <button
            onClick={() => openScanner('hub_receive')}
            className={`py-3 px-4 rounded-xl font-bold text-xs flex flex-col items-center gap-1 shadow-md transition-all active:scale-95 ${
              isLuxury ? 'bg-[#d4af37] text-black hover:bg-[#e9c349]' : 'bg-[#1e3a8a] text-white'
            }`}
          >
            <div className="flex items-center gap-1.5">
              <span className="material-symbols-outlined text-base">download</span>
              <span>اسکن برای دریافت</span>
            </div>
            <span className="text-[10px] opacity-80">+ اطلاع‌رسانی آنی به مشتری</span>
          </button>

          <button
            onClick={() => openScanner('hub_deliver')}
            className={`py-3 px-4 rounded-xl font-bold text-xs flex flex-col items-center gap-1 shadow-md transition-all active:scale-95 ${
              isLuxury ? 'bg-[#464747] text-white hover:bg-gray-600' : 'bg-orange-500 text-white'
            }`}
          >
            <div className="flex items-center gap-1.5">
              <span className="material-symbols-outlined text-base">upload</span>
              <span>اسکن برای تحویل</span>
            </div>
            <span className="text-[10px] opacity-80">+ ثبت نهایی در پنل کاربر</span>
          </button>
        </div>
      </section>

      {/* Two Large Quick Action Cards (Matching Screen 8) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <button
          onClick={() => openScanner('hub_receive')}
          className={`p-6 rounded-2xl flex flex-col items-center justify-center gap-2 shadow-lg transition-all active:scale-95 ${
            isLuxury ? 'bg-[#d4af37] text-black hover:bg-[#e9c349]' : 'bg-[#1e3a8a] text-white'
          }`}
        >
          <span className="material-symbols-outlined text-4xl">local_shipping</span>
          <span className="text-base font-bold">دریافت از مامور پخش</span>
        </button>

        <button
          onClick={() => openScanner('hub_deliver')}
          className={`p-6 rounded-2xl flex flex-col items-center justify-center gap-2 shadow-lg transition-all active:scale-95 ${
            isLuxury ? 'bg-[#353535] text-white hover:bg-[#464747]' : 'bg-[#fd761a] text-white'
          }`}
        >
          <span className="material-symbols-outlined text-4xl">hail</span>
          <span className="text-base font-bold">تحویل به مشتری</span>
        </button>
      </div>

      {/* Current Hub Inventory (Matching Screen 8) */}
      <section className="space-y-4 pt-2">
        <div className="flex justify-between items-center">
          <h3 className="text-lg font-bold text-white">موجودی فعلی هاب</h3>
          <span className="px-3 py-1 rounded-full text-xs font-mono bg-white/10 text-gray-300">
            {inventoryItems.length} بسته
          </span>
        </div>

        <div className="space-y-3">
          {inventoryItems.map((inv) => (
            <div
              key={inv.id}
              className={`p-4 rounded-xl border flex flex-col gap-2 transition-all ${
                isLuxury ? 'bg-[#0e0e0e] border-[#353535] hover:border-[#d4af37]/40' : 'bg-white border-gray-200'
              }`}
            >
              <div className="flex justify-between items-center">
                <span className={`text-xs font-mono font-bold tracking-wider ${
                  isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'
                }`}>
                  {inv.code}
                </span>

                <span className={`px-2.5 py-1 rounded-full text-[11px] font-semibold flex items-center gap-1 ${
                  inv.isOverdue
                    ? 'bg-red-500/20 text-red-400 border border-red-500/40'
                    : 'bg-white/10 text-gray-300'
                }`}>
                  <span className="material-symbols-outlined text-xs">
                    {inv.isOverdue ? 'warning' : 'timer'}
                  </span>
                  <span>{inv.statusLabel}</span>
                </span>
              </div>

              <div className="grid grid-cols-2 gap-2 text-xs pt-1 border-t border-gray-800">
                <div>
                  <span className="text-gray-400 block text-[11px]">گیرنده</span>
                  <span className="font-semibold text-white">{inv.receiver}</span>
                </div>
                <div className="text-left">
                  <span className="text-gray-400 block text-[11px]">ابعاد</span>
                  <span className="text-gray-300">{inv.dimensions}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>

    </div>
  );
};
