import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';

export const PackagePhotoModal: React.FC = () => {
  const { isPhotoModalOpen, closePhotoModal, activePackageForPhoto, confirmPackageDelivery, theme } = useApp();
  const [photoPreview, setPhotoPreview] = useState<string | null>(null);
  const [isUploading, setIsUploading] = useState(false);

  if (!isPhotoModalOpen || !activePackageForPhoto) return null;

  const isLuxury = theme === 'luxury';

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = () => {
        setPhotoPreview(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleSimulateCameraCapture = () => {
    // Generate a simulated delivery snapshot
    setPhotoPreview('https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=600&auto=format&fit=crop&q=80');
  };

  const handleSavePhoto = () => {
    setIsUploading(true);
    setTimeout(() => {
      confirmPackageDelivery(activePackageForPhoto.id, photoPreview || undefined);
      setIsUploading(false);
      setPhotoPreview(null);
      closePhotoModal();
    }, 800);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in duration-200">
      <div className={`relative w-full max-w-md rounded-2xl overflow-hidden shadow-2xl border p-6 flex flex-col gap-4 text-right ${
        isLuxury ? 'bg-[#131313] border-[#d4af37]/40 text-gray-100' : 'bg-white border-blue-200 text-gray-900'
      }`}>
        
        {/* Header */}
        <div className="flex items-center justify-between border-b pb-3 border-gray-700/30">
          <button
            onClick={() => {
              setPhotoPreview(null);
              closePhotoModal();
            }}
            className="p-1 rounded-full text-gray-400 hover:text-white hover:bg-white/10"
          >
            <span className="material-symbols-outlined text-2xl">close</span>
          </button>
          <div className="flex items-center gap-2">
            <h3 className={`text-base font-bold ${isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'}`}>
              آپلود عکس تحویل مرسوله
            </h3>
            <span className="material-symbols-outlined text-amber-500">add_a_photo</span>
          </div>
        </div>

        {/* Package info summary */}
        <div className={`p-3 rounded-xl border text-xs flex justify-between items-center ${
          isLuxury ? 'bg-[#1b1b1b] border-white/5' : 'bg-blue-50 border-blue-100'
        }`}>
          <div>
            <div className="font-bold">{activePackageForPhoto.title}</div>
            <div className="text-[10px] text-gray-400 font-mono">{activePackageForPhoto.trackingCode}</div>
          </div>
          <span className="px-2 py-0.5 rounded text-[10px] bg-amber-500/20 text-amber-400 font-medium">
            فرستنده: {activePackageForPhoto.sender}
          </span>
        </div>

        {/* Photo View / Upload Zone */}
        <div className="flex flex-col gap-3">
          {photoPreview ? (
            <div className="relative w-full aspect-video rounded-xl overflow-hidden border-2 border-amber-400 shadow-md">
              <img src={photoPreview} alt="عکس تحویل" className="w-full h-full object-cover" />
              <button
                type="button"
                onClick={() => setPhotoPreview(null)}
                className="absolute top-2 right-2 bg-black/70 text-white p-1.5 rounded-full hover:bg-red-600 transition-colors"
                title="حذف و عکس مجدد"
              >
                <span className="material-symbols-outlined text-base">delete</span>
              </button>
              <div className="absolute bottom-2 left-2 bg-emerald-500/90 text-white text-[10px] px-2 py-0.5 rounded-full flex items-center gap-1">
                <span className="material-symbols-outlined text-xs">verified</span>
                <span>تصویر آماده تایید</span>
              </div>
            </div>
          ) : (
            <div className="flex flex-col gap-2.5">
              {/* Drag and Drop / Select File */}
              <label className={`border-2 border-dashed rounded-xl p-6 flex flex-col items-center justify-center gap-2 cursor-pointer transition-all ${
                isLuxury 
                  ? 'border-[#d4af37]/40 bg-[#1a1a1a] hover:bg-[#222222]' 
                  : 'border-blue-300 bg-blue-50/50 hover:bg-blue-50'
              }`}>
                <span className={`material-symbols-outlined text-4xl ${isLuxury ? 'text-[#d4af37]' : 'text-blue-600'}`}>
                  cloud_upload
                </span>
                <span className="text-xs font-medium">انتخاب یا رها کردن فایل تصویر</span>
                <span className="text-[10px] text-gray-400">فرمت‌های JPG، PNG و WEBP تا حجم ۵ مگابایت</span>
                <input type="file" accept="image/*" className="hidden" onChange={handleFileUpload} />
              </label>

              {/* Instant Camera Capture Simulation Button */}
              <button
                type="button"
                onClick={handleSimulateCameraCapture}
                className={`py-2.5 px-3 rounded-xl border text-xs font-semibold flex items-center justify-center gap-2 transition-all ${
                  isLuxury 
                    ? 'border-[#d4af37] text-[#f2ca50] hover:bg-[#d4af37]/10' 
                    : 'border-blue-600 text-blue-700 hover:bg-blue-50'
                }`}
              >
                <span className="material-symbols-outlined text-base">photo_camera</span>
                <span>عکسبرداری آنی با دوربین گوشی</span>
              </button>
            </div>
          )}
        </div>

        {/* Modal Actions */}
        <div className="flex gap-2 mt-2">
          <button
            type="button"
            onClick={handleSavePhoto}
            disabled={!photoPreview || isUploading}
            className={`flex-1 py-3 px-4 rounded-xl font-bold text-xs flex items-center justify-center gap-2 transition-all shadow-md active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed ${
              isLuxury 
                ? 'bg-[#d4af37] text-black hover:bg-[#e9c349]' 
                : 'bg-[#1e3a8a] text-white hover:bg-[#00236f]'
            }`}
          >
            <span className="material-symbols-outlined text-base">
              {isUploading ? 'sync' : 'check'}
            </span>
            <span>{isUploading ? 'در حال ثبت در سرور...' : 'ثبت و تایید تصویر تحویل'}</span>
          </button>
          
          <button
            type="button"
            onClick={() => {
              setPhotoPreview(null);
              closePhotoModal();
            }}
            className={`px-4 py-3 rounded-xl text-xs font-medium border transition-colors ${
              isLuxury ? 'border-white/10 hover:bg-white/5 text-gray-400' : 'border-gray-300 hover:bg-gray-100 text-gray-700'
            }`}
          >
            بستن
          </button>
        </div>

      </div>
    </div>
  );
};
