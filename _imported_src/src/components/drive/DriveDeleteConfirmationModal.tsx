import React from 'react';
import { DriveFileItem } from '../../types';

interface DriveDeleteConfirmationModalProps {
  isOpen: boolean;
  file: DriveFileItem | null;
  isDeleting: boolean;
  onConfirm: () => void;
  onCancel: () => void;
  isLuxury?: boolean;
}

export const DriveDeleteConfirmationModal: React.FC<DriveDeleteConfirmationModalProps> = ({
  isOpen,
  file,
  isDeleting,
  onConfirm,
  onCancel,
  isLuxury = true,
}) => {
  if (!isOpen || !file) return null;

  const isFolder = file.mimeType === 'application/vnd.google-apps.folder';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className={`w-full max-w-md rounded-2xl border p-6 shadow-2xl relative text-right ${
        isLuxury 
          ? 'bg-[#121212] border-red-500/40 text-gray-100' 
          : 'bg-white border-red-200 text-gray-900'
      }`}>
        {/* Warning Icon Header */}
        <div className="flex items-center gap-3 mb-4">
          <div className="w-12 h-12 rounded-2xl bg-red-500/20 border border-red-500/40 flex items-center justify-center text-red-400 flex-shrink-0">
            <span className="material-symbols-outlined text-3xl">delete_forever</span>
          </div>
          <div>
            <h3 className="text-base font-bold text-red-400">
              تایید حذف {isFolder ? 'پوشه' : 'فایل'} از گوگل درایو
            </h3>
            <p className="text-xs text-gray-400">عملیات تغییرناپذیر روی حساب کاربری Google Drive</p>
          </div>
        </div>

        {/* File info box */}
        <div className={`p-3.5 rounded-xl border mb-5 ${
          isLuxury ? 'bg-black/60 border-white/10' : 'bg-gray-50 border-gray-200'
        }`}>
          <div className="flex items-center gap-2.5">
            <span className="material-symbols-outlined text-xl text-amber-400">
              {isFolder ? 'folder' : 'description'}
            </span>
            <div className="flex-1 min-w-0">
              <p className="text-xs font-bold truncate text-white" title={file.name}>
                {file.name}
              </p>
              <p className="text-[10px] text-gray-400 mt-0.5">
                شناسه درایو: <span className="font-mono">{file.id.substring(0, 16)}...</span>
              </p>
            </div>
          </div>
        </div>

        <p className="text-xs leading-relaxed text-gray-300 mb-6">
          آیا مطمئن هستید که می‌خواهید این {isFolder ? 'پوشه و تمام محتویات آن' : 'فایل'} را از Google Drive حذف کنید؟ 
          این عملیات بلافاصله اعمال شده و با اجازه مستقیم شما صورت می‌پذیرد.
        </p>

        {/* Action Buttons */}
        <div className="flex items-center justify-end gap-3 pt-3 border-t border-white/10">
          <button
            type="button"
            onClick={onCancel}
            disabled={isDeleting}
            className={`px-4 py-2.5 rounded-xl text-xs font-semibold transition-all ${
              isLuxury ? 'bg-white/10 text-gray-300 hover:bg-white/15' : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            }`}
          >
            انصراف و لغو
          </button>
          
          <button
            type="button"
            onClick={onConfirm}
            disabled={isDeleting}
            className="px-5 py-2.5 rounded-xl text-xs font-bold bg-red-600 hover:bg-red-700 text-white shadow-lg transition-all flex items-center gap-2 disabled:opacity-50"
          >
            {isDeleting ? (
              <>
                <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                <span>در حال حذف از Drive...</span>
              </>
            ) : (
              <>
                <span className="material-symbols-outlined text-base">delete</span>
                <span>بله، با اطمینان حذف شود</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
