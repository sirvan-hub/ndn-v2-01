import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import { getOrCreateNDNBackupFolder, uploadJsonToDrive } from '../../services/googleDriveService';

interface DriveBackupModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (fileName: string) => void;
  isLuxury?: boolean;
}

export const DriveBackupModal: React.FC<DriveBackupModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  isLuxury = true,
}) => {
  const { packages, activityLogs, hubs, registeredUsers, currentUser } = useApp();
  const [isBackingUp, setIsBackingUp] = useState(false);
  const [backupType, setBackupType] = useState<'full' | 'packages_only' | 'logs_only'>('full');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleStartBackup = async () => {
    try {
      setIsBackingUp(true);
      setErrorMessage(null);

      // Ensure dedicated NDN folder exists in Drive
      const targetFolder = await getOrCreateNDNBackupFolder();

      const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
      const fileName = `NDN_Backup_${backupType}_${timestamp}.json`;

      let backupPayload: any = {
        app: 'سامانه تحویل مطمئن محله (NDN)',
        version: '2.4.0',
        exportDate: new Date().toISOString(),
        exportedBy: currentUser ? {
          name: currentUser.fullName,
          role: currentUser.role,
          phone: currentUser.phone
        } : 'Anonymous',
        stats: {
          packagesCount: packages.length,
          logsCount: activityLogs.length,
          hubsCount: hubs.length,
        }
      };

      if (backupType === 'full' || backupType === 'packages_only') {
        backupPayload.packages = packages;
      }
      if (backupType === 'full' || backupType === 'logs_only') {
        backupPayload.activityLogs = activityLogs;
      }
      if (backupType === 'full') {
        backupPayload.hubs = hubs;
        backupPayload.users = registeredUsers.map(u => ({
          id: u.id,
          role: u.role,
          fullName: u.fullName,
          phone: u.phone,
          postalCode: u.postalCode
        }));
      }

      await uploadJsonToDrive(fileName, backupPayload, targetFolder.id);

      onSuccess(fileName);
      onClose();
    } catch (err: any) {
      console.error('Backup error:', err);
      setErrorMessage(err.message || 'خطا در پشتیبان‌گیری در درایو');
    } finally {
      setIsBackingUp(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className={`w-full max-w-lg rounded-2xl border p-6 shadow-2xl relative text-right ${
        isLuxury 
          ? 'bg-[#121212] border-[#d4af37]/40 text-gray-100' 
          : 'bg-white border-blue-200 text-gray-900'
      }`}>
        <div className="flex items-center justify-between pb-3 border-b border-white/10 mb-4">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-2xl text-amber-400">cloud_sync</span>
            <h3 className="text-base font-bold text-white">پشتیبان‌گیری امن در Google Drive</h3>
          </div>
          <button 
            onClick={onClose}
            disabled={isBackingUp}
            className="p-1 rounded-lg text-gray-400 hover:text-white"
          >
            <span className="material-symbols-outlined text-lg">close</span>
          </button>
        </div>

        <p className="text-xs text-gray-300 mb-4 leading-relaxed">
          داده‌های پستی، سوابق تحویل بسته‌ها، بارکدها و لاگ‌های امنیتی شما به طور مستقیم در پوشه اختصاصی <strong className="text-amber-400 font-mono">NDN_Postal_Backups</strong> در حساب گوگل درایو ذخیره می‌گردند.
        </p>

        {/* Options */}
        <div className="space-y-2.5 mb-5">
          <label className="block text-xs font-semibold text-gray-400 mb-1">
            نوع نسخه پشتیبان:
          </label>

          {[
            { id: 'full', title: 'پشتیبان کامل سامانه', desc: `${packages.length} بسته + ${activityLogs.length} لاگ + مشخصات هاب‌ها` },
            { id: 'packages_only', title: 'فقط مرسولات و وضعیت بسته‌ها', desc: `شامل اطلاعات کامل ${packages.length} بسته پستی` },
            { id: 'logs_only', title: 'فقط تاریخچه لاگ‌ها و رویدادها', desc: `شامل ${activityLogs.length} رکورد فعالیت سیستم` },
          ].map((opt) => (
            <div
              key={opt.id}
              onClick={() => setBackupType(opt.id as any)}
              className={`p-3.5 rounded-xl border cursor-pointer transition-all flex items-center justify-between ${
                backupType === opt.id
                  ? isLuxury 
                    ? 'bg-amber-400/10 border-amber-400 text-white' 
                    : 'bg-blue-50 border-blue-500 text-blue-900'
                  : 'bg-white/5 border-white/10 text-gray-300 hover:border-white/20'
              }`}
            >
              <div>
                <div className="text-xs font-bold">{opt.title}</div>
                <div className="text-[11px] text-gray-400 mt-0.5">{opt.desc}</div>
              </div>
              <div className={`w-5 h-5 rounded-full border flex items-center justify-center ${
                backupType === opt.id ? 'border-amber-400 bg-amber-400 text-black' : 'border-gray-500'
              }`}>
                {backupType === opt.id && <span className="material-symbols-outlined text-xs font-bold">check</span>}
              </div>
            </div>
          ))}
        </div>

        {errorMessage && (
          <div className="p-3 rounded-xl bg-red-500/20 border border-red-500/40 text-red-300 text-xs flex items-center gap-2 mb-4">
            <span className="material-symbols-outlined text-base">error</span>
            <span>{errorMessage}</span>
          </div>
        )}

        <div className="flex items-center justify-end gap-3 pt-3 border-t border-white/10">
          <button
            type="button"
            onClick={onClose}
            disabled={isBackingUp}
            className={`px-4 py-2.5 rounded-xl text-xs font-semibold ${
              isLuxury ? 'bg-white/10 text-gray-300 hover:bg-white/15' : 'bg-gray-200 text-gray-700'
            }`}
          >
            انصراف
          </button>
          <button
            type="button"
            onClick={handleStartBackup}
            disabled={isBackingUp}
            className="px-6 py-2.5 rounded-xl text-xs font-bold bg-gradient-to-r from-amber-500 to-yellow-400 text-black hover:brightness-110 shadow-lg transition-all flex items-center gap-2 disabled:opacity-50"
          >
            {isBackingUp ? (
              <>
                <div className="w-3.5 h-3.5 border-2 border-black border-t-transparent rounded-full animate-spin" />
                <span>در حال ایجاد پشتیبان در Google Drive...</span>
              </>
            ) : (
              <>
                <span className="material-symbols-outlined text-base">backup</span>
                <span>شروع پشتیبان‌گیری در درایو</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
