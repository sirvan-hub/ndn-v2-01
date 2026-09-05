import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import { PackageSize } from '../../types';

export const BarcodeScannerModal: React.FC = () => {
  const {
    isScannerOpen,
    closeScanner,
    scannerMode,
    packages,
    selectedPackage,
    selectedHub,
    registerNewPackage,
    confirmPackageDelivery,
    theme
  } = useApp();

  const [packageSize, setPackageSize] = useState<PackageSize>('medium');
  const [senderName, setSenderName] = useState('دیجی‌کالا');
  const [receiverName, setReceiverName] = useState('علی رضایی');
  const [packageTitle, setPackageTitle] = useState('بسته خرید پوشاک ورزشی');
  const [isScanning, setIsScanning] = useState(false);
  const [scanSuccess, setScanSuccess] = useState(false);

  if (!isScannerOpen) return null;

  const isLuxury = theme === 'luxury';

  const handleTriggerScan = () => {
    setIsScanning(true);
    setTimeout(() => {
      setIsScanning(false);
      setScanSuccess(true);
      setTimeout(() => {
        if (scannerMode === 'courier_register') {
          registerNewPackage({
            title: packageTitle,
            sender: senderName,
            receiver: receiverName,
            size: packageSize,
            dimensions: packageSize === 'small' ? '۱۰ × ۲۰ × ۵' : packageSize === 'large' ? '۳۰ × ۴۰ × ۲۰' : '۲۰ × ۳۰ × ۱۵',
            weight: packageSize === 'small' ? '۰.۵ کیلوگرم' : packageSize === 'large' ? '۲.۸ کیلوگرم' : '۱.۲ کیلوگرم'
          });
        } else if (scannerMode === 'hub_receive' || scannerMode === 'customer_confirm') {
          if (selectedPackage) {
            confirmPackageDelivery(selectedPackage.id);
          } else if (packages.length > 0) {
            confirmPackageDelivery(packages[0].id);
          }
        }
        setScanSuccess(false);
        closeScanner();
      }, 1000);
    }, 1500);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in duration-200">
      <div className={`relative w-full max-w-lg rounded-2xl overflow-hidden shadow-2xl border p-6 flex flex-col gap-4 text-right ${
        isLuxury ? 'bg-[#131313] border-[#d4af37]/40 text-gray-100' : 'bg-white border-blue-200 text-gray-900'
      }`}>
        
        {/* Modal Header */}
        <div className="flex items-center justify-between border-b pb-3 border-gray-700/30">
          <button
            onClick={closeScanner}
            className="p-1 rounded-full text-gray-400 hover:text-white hover:bg-white/10 transition-colors"
          >
            <span className="material-symbols-outlined text-2xl">close</span>
          </button>
          <div className="flex items-center gap-2">
            <h3 className={`text-lg font-bold ${isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'}`}>
              {scannerMode === 'courier_register' && 'ثبت سریع بسته (اسکن بارکد)'}
              {scannerMode === 'hub_receive' && 'اسکن برای دریافت در هاب'}
              {scannerMode === 'hub_deliver' && 'اسکن برای تحویل به مشتری'}
              {scannerMode === 'customer_confirm' && 'تایید و اسکن بسته'}
            </h3>
            <span className={`material-symbols-outlined ${isLuxury ? 'text-[#d4af37]' : 'text-blue-600'}`}>
              qr_code_scanner
            </span>
          </div>
        </div>

        {/* Viewfinder Frame */}
        <div className="relative w-full aspect-square max-h-72 bg-black rounded-xl overflow-hidden border-2 border-dashed border-amber-400/60 flex items-center justify-center">
          {/* Animated Background Grid */}
          <div className="absolute inset-0 opacity-20 bg-[radial-gradient(circle,transparent_20%,black_20%,black_80%,transparent_80%,transparent)] bg-[length:20px_20px]"></div>

          {/* Scanner Corner Highlights */}
          <div className="absolute top-4 left-4 w-8 h-8 border-t-4 border-l-4 border-amber-400"></div>
          <div className="absolute top-4 right-4 w-8 h-8 border-t-4 border-r-4 border-amber-400"></div>
          <div className="absolute bottom-4 left-4 w-8 h-8 border-b-4 border-l-4 border-amber-400"></div>
          <div className="absolute bottom-4 right-4 w-8 h-8 border-b-4 border-r-4 border-amber-400"></div>

          {/* Laser Scanning Line */}
          <div className="absolute left-0 right-0 h-1 bg-amber-400 shadow-[0_0_15px_#f2ca50] animate-scan-line"></div>

          {/* Target Central Reticle */}
          <div className="w-28 h-28 border-2 border-amber-400/40 rounded-xl flex items-center justify-center">
            {scanSuccess ? (
              <span className="material-symbols-outlined text-5xl text-emerald-400 animate-bounce">
                check_circle
              </span>
            ) : isScanning ? (
              <span className="material-symbols-outlined text-4xl text-amber-400 animate-spin">
                sync
              </span>
            ) : (
              <span className="material-symbols-outlined text-4xl text-amber-400/50">
                qr_code_scanner
              </span>
            )}
          </div>

          {/* Top Pill: Auto photo capture indicator */}
          <div className="absolute top-3 right-3 bg-amber-500/90 text-black px-2.5 py-1 rounded-full text-[11px] font-bold flex items-center gap-1">
            <span className="material-symbols-outlined text-sm">photo_camera</span>
            <span>ثبت خودکار تصویر فعال است</span>
          </div>

          {/* Bottom Pill: Live status */}
          <div className="absolute bottom-3 inset-x-4 text-center">
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-black/75 border border-white/20 text-[11px] text-gray-200 backdrop-blur-sm">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
              <span>همگام‌سازی لحظه‌ای با پنل هاب و مشتری</span>
            </div>
          </div>
        </div>

        {/* Courier Registration Inputs */}
        {scannerMode === 'courier_register' && (
          <div className="space-y-3">
            <div>
              <label className="block text-xs text-gray-400 mb-1">سایز بسته (اجباری)</label>
              <div className="grid grid-cols-3 gap-2">
                {[
                  { id: 'small', label: 'کوچک', icon: 'mail', sub: 'تا ۱ کیلو' },
                  { id: 'medium', label: 'متوسط', icon: 'inventory_2', sub: '۱ تا ۳ کیلو' },
                  { id: 'large', label: 'بزرگ', icon: 'package_2', sub: 'بالای ۳ کیلو' }
                ].map((s) => (
                  <button
                    key={s.id}
                    type="button"
                    onClick={() => setPackageSize(s.id as PackageSize)}
                    className={`flex flex-col items-center justify-center p-2.5 rounded-xl border text-xs transition-all ${
                      packageSize === s.id
                        ? (isLuxury ? 'border-[#d4af37] bg-[#d4af37]/15 text-[#f2ca50] font-bold' : 'border-[#1e3a8a] bg-blue-50 text-[#1e3a8a] font-bold')
                        : (isLuxury ? 'border-white/10 bg-[#1a1a1a] text-gray-400' : 'border-gray-200 bg-gray-50 text-gray-600')
                    }`}
                  >
                    <span className="material-symbols-outlined text-lg mb-1">{s.icon}</span>
                    <span>{s.label}</span>
                    <span className="text-[9px] opacity-75">{s.sub}</span>
                  </button>
                ))}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-2 text-xs">
              <div>
                <label className="block text-gray-400 mb-1">عنوان مرسوله</label>
                <input
                  type="text"
                  value={packageTitle}
                  onChange={(e) => setPackageTitle(e.target.value)}
                  className={`w-full px-2.5 py-1.5 rounded-lg border text-xs ${
                    isLuxury ? 'bg-[#1f1f1f] border-white/10 text-white' : 'bg-gray-50 border-gray-300 text-gray-800'
                  }`}
                />
              </div>
              <div>
                <label className="block text-gray-400 mb-1">فرستنده</label>
                <input
                  type="text"
                  value={senderName}
                  onChange={(e) => setSenderName(e.target.value)}
                  className={`w-full px-2.5 py-1.5 rounded-lg border text-xs ${
                    isLuxury ? 'bg-[#1f1f1f] border-white/10 text-white' : 'bg-gray-50 border-gray-300 text-gray-800'
                  }`}
                />
              </div>
            </div>

            <div className="text-[11px] text-gray-400 flex items-center gap-1">
              <span className="material-symbols-outlined text-sm text-amber-400">info</span>
              <span>هاب مقصد انتخابی: {selectedHub.name} ({selectedHub.typeName})</span>
            </div>
          </div>
        )}

        {/* Selected Package Details for Hub Check-in */}
        {scannerMode !== 'courier_register' && selectedPackage && (
          <div className={`p-3 rounded-xl border text-xs flex justify-between items-center ${
            isLuxury ? 'bg-[#1a1a1a] border-white/10' : 'bg-blue-50 border-blue-100'
          }`}>
            <div className="text-right">
              <div className="font-bold">{selectedPackage.title}</div>
              <div className="text-[10px] text-gray-400 font-mono">{selectedPackage.trackingCode}</div>
            </div>
            <span className="px-2 py-1 rounded-full text-[10px] bg-amber-500/20 text-amber-400 font-medium">
              {selectedPackage.statusText}
            </span>
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex gap-2 mt-2">
          <button
            type="button"
            onClick={handleTriggerScan}
            disabled={isScanning || scanSuccess}
            className={`flex-1 py-3 px-4 rounded-xl font-bold text-sm flex items-center justify-center gap-2 transition-all shadow-lg active:scale-95 ${
              isLuxury
                ? 'bg-[#d4af37] text-black hover:bg-[#e9c349]'
                : 'bg-[#1e3a8a] text-white hover:bg-[#00236f]'
            }`}
          >
            <span className="material-symbols-outlined text-lg">
              {isScanning ? 'sync' : 'qr_code_scanner'}
            </span>
            <span>
              {isScanning ? 'در حال تشخیص بارکد...' : scanSuccess ? 'بارکد شناسایی شد!' : 'اسکن و تایید نهایی'}
            </span>
          </button>

          <button
            type="button"
            onClick={closeScanner}
            className={`px-4 py-3 rounded-xl text-xs font-medium border transition-colors ${
              isLuxury ? 'border-white/10 hover:bg-white/5 text-gray-400' : 'border-gray-300 hover:bg-gray-100 text-gray-700'
            }`}
          >
            انصراف
          </button>
        </div>

      </div>
    </div>
  );
};
