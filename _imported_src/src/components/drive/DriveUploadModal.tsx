import React, { useState, useRef } from 'react';
import { uploadFileToDrive } from '../../services/googleDriveService';
import { DriveFolderBreadcrumb } from '../../types';

interface DriveUploadModalProps {
  isOpen: boolean;
  onClose: () => void;
  onUploadSuccess: () => void;
  currentFolder: DriveFolderBreadcrumb;
  isLuxury?: boolean;
}

export const DriveUploadModal: React.FC<DriveUploadModalProps> = ({
  isOpen,
  onClose,
  onUploadSuccess,
  currentFolder,
  isLuxury = true,
}) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  if (!isOpen) return null;

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      setSelectedFile(e.dataTransfer.files[0]);
      setErrorMessage(null);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setSelectedFile(e.target.files[0]);
      setErrorMessage(null);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    try {
      setIsUploading(true);
      setErrorMessage(null);
      await uploadFileToDrive(selectedFile, currentFolder.id === 'root' ? undefined : currentFolder.id);
      setSelectedFile(null);
      onUploadSuccess();
      onClose();
    } catch (err: any) {
      console.error('Upload error:', err);
      setErrorMessage(err.message || 'خطا در بارگذاری فایل در گوگل درایو');
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className={`w-full max-w-lg rounded-2xl border p-6 shadow-2xl relative text-right ${
        isLuxury 
          ? 'bg-[#121212] border-[#d4af37]/40 text-gray-100' 
          : 'bg-white border-blue-200 text-gray-900'
      }`}>
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-white/10 mb-4">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-2xl text-amber-400">cloud_upload</span>
            <h3 className="text-base font-bold text-white">آپلود فایل در Google Drive</h3>
          </div>
          <button 
            onClick={onClose}
            disabled={isUploading}
            className="p-1 rounded-lg text-gray-400 hover:text-white hover:bg-white/10"
          >
            <span className="material-symbols-outlined text-lg">close</span>
          </button>
        </div>

        {/* Destination folder */}
        <div className="text-xs text-gray-400 mb-3 flex items-center gap-1.5">
          <span>مقصد در درایو:</span>
          <span className="font-bold text-amber-400 px-2 py-0.5 rounded bg-amber-400/10 border border-amber-400/30">
            {currentFolder.name}
          </span>
        </div>

        {/* Drop zone */}
        <div
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
          className={`border-2 border-dashed rounded-2xl p-6 text-center cursor-pointer transition-all ${
            isDragging
              ? 'border-amber-400 bg-amber-400/10 scale-[1.01]'
              : isLuxury
                ? 'border-white/20 bg-black/40 hover:border-amber-400/60 hover:bg-white/5'
                : 'border-blue-300 bg-blue-50/50 hover:border-blue-500'
          }`}
        >
          <input
            ref={fileInputRef}
            type="file"
            onChange={handleFileChange}
            className="hidden"
          />

          <div className="w-12 h-12 mx-auto rounded-full bg-amber-400/20 text-amber-400 flex items-center justify-center mb-3">
            <span className="material-symbols-outlined text-3xl">upload_file</span>
          </div>

          <p className="text-xs font-semibold text-white mb-1">
            فایل را به اینجا بکشید یا برای انتخاب کلیک کنید
          </p>
          <p className="text-[11px] text-gray-400">
            پشتیبانی از انواع اسناد، تصاویر، فاکتورهای پستی، بارکدها و بسته‌های JSON
          </p>
        </div>

        {/* Selected file preview */}
        {selectedFile && (
          <div className={`mt-4 p-3 rounded-xl border flex items-center justify-between gap-3 ${
            isLuxury ? 'bg-amber-400/10 border-amber-400/30' : 'bg-blue-50 border-blue-200'
          }`}>
            <div className="flex items-center gap-2.5 min-w-0">
              <span className="material-symbols-outlined text-amber-400 text-xl">description</span>
              <div className="min-w-0">
                <p className="text-xs font-bold text-white truncate">{selectedFile.name}</p>
                <p className="text-[10px] text-gray-400 font-mono">{(selectedFile.size / 1024).toFixed(1)} KB</p>
              </div>
            </div>
            <button
              onClick={(e) => {
                e.stopPropagation();
                setSelectedFile(null);
              }}
              className="text-gray-400 hover:text-red-400 p-1"
            >
              <span className="material-symbols-outlined text-base">delete</span>
            </button>
          </div>
        )}

        {errorMessage && (
          <div className="mt-3 p-3 rounded-xl bg-red-500/20 border border-red-500/40 text-red-300 text-xs flex items-center gap-2">
            <span className="material-symbols-outlined text-base">error</span>
            <span>{errorMessage}</span>
          </div>
        )}

        {/* Actions */}
        <div className="flex items-center justify-end gap-3 mt-6 pt-3 border-t border-white/10">
          <button
            type="button"
            onClick={onClose}
            disabled={isUploading}
            className={`px-4 py-2.5 rounded-xl text-xs font-semibold transition-all ${
              isLuxury ? 'bg-white/10 text-gray-300 hover:bg-white/15' : 'bg-gray-200 text-gray-700'
            }`}
          >
            انصراف
          </button>
          <button
            type="button"
            onClick={handleUpload}
            disabled={!selectedFile || isUploading}
            className="px-6 py-2.5 rounded-xl text-xs font-bold bg-gradient-to-r from-amber-500 to-yellow-400 text-black hover:brightness-110 shadow-lg transition-all flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isUploading ? (
              <>
                <div className="w-3.5 h-3.5 border-2 border-black border-t-transparent rounded-full animate-spin" />
                <span>در حال آپلود در Drive...</span>
              </>
            ) : (
              <>
                <span className="material-symbols-outlined text-base">cloud_upload</span>
                <span>بارگذاری در درایو</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
