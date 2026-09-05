import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import { getAccessToken, googleSignIn, getOrCreateNDNBackupFolder, uploadJsonToDrive } from '../../services/googleDriveService';
import { PackageItem } from '../../types';

interface DriveQuickSyncButtonProps {
  pkg: PackageItem;
  className?: string;
  isLuxury?: boolean;
}

export const DriveQuickSyncButton: React.FC<DriveQuickSyncButtonProps> = ({
  pkg,
  className = '',
  isLuxury = true
}) => {
  const { showToast } = useApp();
  const [isSyncing, setIsSyncing] = useState(false);

  const handleSync = async (e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      setIsSyncing(true);

      let token = await getAccessToken();
      if (!token) {
        // Trigger quick sign in if not already logged in
        const res = await googleSignIn();
        token = res?.accessToken || null;
      }

      if (!token) {
        showToast('برای ذخیره در درایو، ابتدا وارد حساب گوگل شوید.');
        return;
      }

      const folder = await getOrCreateNDNBackupFolder();
      const fileName = `NDN_Receipt_${pkg.trackingCode}_${pkg.status}.json`;
      
      const receiptData = {
        app: 'سامانه پستی محله (NDN)',
        receiptTitle: `رسید دیجیتال مرسوله ${pkg.trackingCode}`,
        trackingCode: pkg.trackingCode,
        packageTitle: pkg.title,
        status: pkg.status,
        statusText: pkg.statusText,
        sender: pkg.sender,
        receiver: pkg.receiver,
        receiverPhone: pkg.receiverPhone,
        hubName: pkg.hubName,
        hubAddress: pkg.hubAddress,
        courierName: pkg.courierName,
        totalFee: pkg.totalFee,
        isPaid: pkg.isPaid,
        dimensions: pkg.dimensions,
        weight: pkg.weight,
        history: pkg.history,
        exportedAt: new Date().toISOString()
      };

      await uploadJsonToDrive(fileName, receiptData, folder.id);
      showToast(`رسید دیجیتال مرسوله ${pkg.trackingCode} در Google Drive ذخیره گردید.`);
    } catch (err: any) {
      console.error('Quick sync error:', err);
      showToast(err.message || 'خطا در پشتیبان‌گیری در گوگل درایو');
    } finally {
      setIsSyncing(false);
    }
  };

  return (
    <button
      type="button"
      onClick={handleSync}
      disabled={isSyncing}
      title="ذخیره رسید دیجیتال در Google Drive"
      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-[11px] font-semibold transition-all border ${
        isLuxury 
          ? 'bg-amber-400/10 border-amber-400/30 text-amber-300 hover:bg-amber-400/20' 
          : 'bg-blue-50 border-blue-200 text-blue-800 hover:bg-blue-100'
      } ${className}`}
    >
      {isSyncing ? (
        <div className="w-3 h-3 border-2 border-amber-400 border-t-transparent rounded-full animate-spin" />
      ) : (
        <span className="material-symbols-outlined text-sm text-amber-400">cloud_upload</span>
      )}
      <span>ذخیره رسید در Drive</span>
    </button>
  );
};
