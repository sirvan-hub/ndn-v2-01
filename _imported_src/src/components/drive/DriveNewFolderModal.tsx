import React, { useState } from 'react';
import { createDriveFolder } from '../../services/googleDriveService';
import { DriveFolderBreadcrumb } from '../../types';

interface DriveNewFolderModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  currentFolder: DriveFolderBreadcrumb;
  isLuxury?: boolean;
}

export const DriveNewFolderModal: React.FC<DriveNewFolderModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  currentFolder,
  isLuxury = true,
}) => {
  const [folderName, setFolderName] = useState('');
  const [isCreating, setIsCreating] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!folderName.trim()) return;

    try {
      setIsCreating(true);
      setErrorMessage(null);
      await createDriveFolder(
        folderName.trim(), 
        currentFolder.id === 'root' ? undefined : currentFolder.id
      );
      setFolderName('');
      onSuccess();
      onClose();
    } catch (err: any) {
      console.error('Create folder error:', err);
      setErrorMessage(err.message || 'خطا در ایجاد پوشه جدید');
    } finally {
      setIsCreating(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className={`w-full max-w-md rounded-2xl border p-6 shadow-2xl relative text-right ${
        isLuxury 
          ? 'bg-[#121212] border-[#d4af37]/40 text-gray-100' 
          : 'bg-white border-blue-200 text-gray-900'
      }`}>
        <div className="flex items-center justify-between pb-3 border-b border-white/10 mb-4">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-2xl text-amber-400">create_new_folder</span>
            <h3 className="text-base font-bold text-white">ایجاد پوشه جدید در Drive</h3>
          </div>
          <button 
            onClick={onClose}
            disabled={isCreating}
            className="p-1 rounded-lg text-gray-400 hover:text-white"
          >
            <span className="material-symbols-outlined text-lg">close</span>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-gray-300 mb-1.5">
              نام پوشه:
            </label>
            <input
              type="text"
              value={folderName}
              onChange={(e) => setFolderName(e.target.value)}
              placeholder="مثال: رسیدهای پستی مهر ۱۴۰۳"
              autoFocus
              className={`w-full px-4 py-2.5 rounded-xl border text-sm text-right focus:outline-none transition-all ${
                isLuxury
                  ? 'bg-black/60 border-white/15 text-white focus:border-amber-400'
                  : 'bg-gray-50 border-gray-300 text-gray-900 focus:border-blue-500'
              }`}
            />
          </div>

          <p className="text-[11px] text-gray-400">
            مکان ایجاد: <strong className="text-amber-400">{currentFolder.name}</strong>
          </p>

          {errorMessage && (
            <div className="p-3 rounded-xl bg-red-500/20 border border-red-500/40 text-red-300 text-xs flex items-center gap-2">
              <span className="material-symbols-outlined text-base">error</span>
              <span>{errorMessage}</span>
            </div>
          )}

          <div className="flex items-center justify-end gap-3 pt-3 border-t border-white/10">
            <button
              type="button"
              onClick={onClose}
              disabled={isCreating}
              className={`px-4 py-2.5 rounded-xl text-xs font-semibold ${
                isLuxury ? 'bg-white/10 text-gray-300 hover:bg-white/15' : 'bg-gray-200 text-gray-700'
              }`}
            >
              انصراف
            </button>
            <button
              type="submit"
              disabled={!folderName.trim() || isCreating}
              className="px-5 py-2.5 rounded-xl text-xs font-bold bg-amber-400 hover:bg-amber-500 text-black shadow-lg transition-all flex items-center gap-2 disabled:opacity-50"
            >
              {isCreating ? (
                <>
                  <div className="w-3.5 h-3.5 border-2 border-black border-t-transparent rounded-full animate-spin" />
                  <span>در حال ایجاد...</span>
                </>
              ) : (
                <>
                  <span className="material-symbols-outlined text-base">folder</span>
                  <span>ایجاد پوشه</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
