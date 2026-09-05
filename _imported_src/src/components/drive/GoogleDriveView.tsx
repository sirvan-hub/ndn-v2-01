import React, { useState, useEffect, useCallback } from 'react';
import { useApp } from '../../context/AppContext';
import { 
  googleSignIn, 
  logoutGoogle, 
  getAccessToken, 
  getDriveAbout, 
  listDriveFiles, 
  deleteDriveFile,
  formatBytes 
} from '../../services/googleDriveService';
import { 
  DriveAboutInfo, 
  DriveFileItem, 
  DriveFolderBreadcrumb 
} from '../../types';
import { GoogleSignInButton } from './GoogleSignInButton';
import { DriveDeleteConfirmationModal } from './DriveDeleteConfirmationModal';
import { DriveUploadModal } from './DriveUploadModal';
import { DriveNewFolderModal } from './DriveNewFolderModal';
import { DriveBackupModal } from './DriveBackupModal';

export const GoogleDriveView: React.FC = () => {
  const { theme, showToast } = useApp();
  const isLuxury = theme === 'luxury';

  // Auth State
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [isAuthChecking, setIsAuthChecking] = useState<boolean>(true);
  const [isLoggingIn, setIsLoggingIn] = useState<boolean>(false);
  const [driveAbout, setDriveAbout] = useState<DriveAboutInfo | null>(null);

  // Files & Browsing State
  const [files, setFiles] = useState<DriveFileItem[]>([]);
  const [isLoadingFiles, setIsLoadingFiles] = useState<boolean>(false);
  const [breadcrumbs, setBreadcrumbs] = useState<DriveFolderBreadcrumb[]>([
    { id: 'root', name: 'Google Drive من' }
  ]);
  const currentFolder = breadcrumbs[breadcrumbs.length - 1];

  // Filters & Search
  const [searchQuery, setSearchQuery] = useState('');
  const [activeMimeFilter, setActiveMimeFilter] = useState<'all' | 'folders' | 'images' | 'documents' | 'spreadsheets' | 'backups'>('all');
  const [viewLayout, setViewLayout] = useState<'grid' | 'list'>('grid');

  // Modals State
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [isNewFolderOpen, setIsNewFolderOpen] = useState(false);
  const [isBackupOpen, setIsBackupOpen] = useState(false);
  const [fileToDelete, setFileToDelete] = useState<DriveFileItem | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Check initial token on mount
  useEffect(() => {
    const checkToken = async () => {
      try {
        const token = await getAccessToken();
        if (token) {
          setIsAuthenticated(true);
          loadDriveDetails();
        } else {
          setIsAuthenticated(false);
        }
      } catch (e) {
        setIsAuthenticated(false);
      } finally {
        setIsAuthChecking(false);
      }
    };
    checkToken();
  }, []);

  const loadDriveDetails = useCallback(async () => {
    try {
      const about = await getDriveAbout();
      setDriveAbout(about);
    } catch (e) {
      console.warn('Could not load drive about info', e);
    }
  }, []);

  const loadFiles = useCallback(async () => {
    setIsLoadingFiles(true);
    try {
      const result = await listDriveFiles({
        folderId: currentFolder.id,
        query: searchQuery.trim() || undefined,
        mimeFilter: activeMimeFilter,
        pageSize: 50
      });
      setFiles(result.files);
    } catch (err: any) {
      console.error('Error listing files:', err);
      showToast(err.message || 'خطا در دریافت لیست فایل‌ها از گوگل درایو');
    } finally {
      setIsLoadingFiles(false);
    }
  }, [currentFolder.id, searchQuery, activeMimeFilter, showToast]);

  useEffect(() => {
    if (isAuthenticated) {
      loadFiles();
    }
  }, [isAuthenticated, loadFiles]);

  const handleLogin = async () => {
    try {
      setIsLoggingIn(true);
      const res = await googleSignIn();
      if (res?.accessToken) {
        setIsAuthenticated(true);
        showToast('اتصال به Google Drive با موفقیت برقرار شد.');
        loadDriveDetails();
        loadFiles();
      }
    } catch (err: any) {
      console.error('Google Sign in failed:', err);
      showToast(err.message || 'خطا در ورود به حساب گوگل.');
    } finally {
      setIsLoggingIn(false);
    }
  };

  const handleLogout = async () => {
    await logoutGoogle();
    setIsAuthenticated(false);
    setDriveAbout(null);
    setFiles([]);
    showToast('اتصال Google Drive قطع گردید.');
  };

  const handleNavigateToFolder = (folder: DriveFileItem) => {
    setBreadcrumbs((prev) => [...prev, { id: folder.id, name: folder.name }]);
    setSearchQuery('');
  };

  const handleBreadcrumbClick = (index: number) => {
    setBreadcrumbs((prev) => prev.slice(0, index + 1));
    setSearchQuery('');
  };

  const handleDeleteConfirmed = async () => {
    if (!fileToDelete) return;
    try {
      setIsDeleting(true);
      await deleteDriveFile(fileToDelete.id);
      showToast(`فایل "${fileToDelete.name}" با موفقیت از Drive حذف شد.`);
      setFileToDelete(null);
      loadFiles();
      loadDriveDetails();
    } catch (err: any) {
      console.error('Delete error:', err);
      showToast(err.message || 'خطا در حذف فایل از گوگل درایو');
    } finally {
      setIsDeleting(false);
    }
  };

  const getFileIcon = (mimeType: string) => {
    if (mimeType === 'application/vnd.google-apps.folder') return 'folder';
    if (mimeType.includes('image/')) return 'image';
    if (mimeType.includes('pdf')) return 'picture_as_pdf';
    if (mimeType.includes('spreadsheet') || mimeType.includes('excel') || mimeType.includes('csv')) return 'table_chart';
    if (mimeType.includes('document') || mimeType.includes('word') || mimeType.includes('text/')) return 'article';
    if (mimeType.includes('presentation')) return 'slideshow';
    if (mimeType.includes('zip') || mimeType.includes('tar') || mimeType.includes('compressed')) return 'folder_zip';
    if (mimeType.includes('json')) return 'data_object';
    return 'insert_drive_file';
  };

  const getQuotaPercent = () => {
    if (!driveAbout?.storageQuota?.limit || !driveAbout?.storageQuota?.usage) return 0;
    const limit = parseInt(driveAbout.storageQuota.limit, 10);
    const usage = parseInt(driveAbout.storageQuota.usage, 10);
    if (isNaN(limit) || limit === 0) return 0;
    return Math.min(100, Math.round((usage / limit) * 100));
  };

  // If initial auth check is loading
  if (isAuthChecking) {
    return (
      <div className="w-full flex items-center justify-center min-h-[400px]">
        <div className="flex flex-col items-center gap-3">
          <div className="w-8 h-8 border-2 border-amber-400 border-t-transparent rounded-full animate-spin" />
          <span className="text-xs text-gray-400">در حال بررسی وضعیت اتصال به Google Drive...</span>
        </div>
      </div>
    );
  }

  // If user is not authenticated with Google Drive
  if (!isAuthenticated) {
    return (
      <div className="w-full max-w-4xl mx-auto space-y-6 animate-in fade-in duration-200">
        <div className="border-b pb-4 border-gray-700/30 text-right">
          <h2 className={`text-2xl font-bold ${isLuxury ? 'text-[#f2ca50]' : 'text-[#00236f]'}`}>
            مدیریت و پشتیبان‌گیری ابری با Google Drive
          </h2>
          <p className="text-xs text-gray-400 mt-1">
            یکپارچگی مستقیم با حساب گوگل جهت ذخیره رسیدها، اسناد پستی و تهیه نسخه‌های پشتیبان امن
          </p>
        </div>

        {/* Hero Card for Google Connection */}
        <div className={`p-8 rounded-3xl border-2 shadow-2xl relative overflow-hidden text-center flex flex-col items-center gap-6 ${
          isLuxury 
            ? 'bg-gradient-to-b from-[#161616] to-[#0d0d0d] border-[#d4af37]/40' 
            : 'bg-white border-blue-200'
        }`}>
          {/* Decorative Google Drive SVG Icon */}
          <div className="w-20 h-20 rounded-3xl bg-amber-400/10 border border-amber-400/30 flex items-center justify-center p-4 shadow-xl">
            <svg viewBox="0 0 87.3 78" className="w-full h-full">
              <path d="m6.6 66.85 3.85 6.65c.8 1.4 1.95 2.5 3.3 3.3l13.75-23.8h-27.5c0 1.55.4 3.1 1.2 4.5z" fill="#0066da"/>
              <path d="m43.65 25-13.75-23.8c-1.35.8-2.5 1.9-3.3 3.3l-25.4 44.05c-.8 1.4-1.2 2.95-1.2 4.5h27.5z" fill="#00ac47"/>
              <path d="m73.55 76.8c1.35-.8 2.5-1.9 3.3-3.3l1.6-2.75 7.65-13.25c.8-1.4 1.2-2.95 1.2-4.5h-27.502l5.852 11.5z" fill="#ea4335"/>
              <path d="m43.65 25 13.75-23.8c-1.35-.8-2.9-1.2-4.5-1.2h-18.5c-1.6 0-3.15.45-4.5 1.25z" fill="#00832d"/>
              <path d="m59.8 53h-32.3l-13.75 23.8c1.35.8 2.9 1.2 4.5 1.2h50.8c1.6 0 3.15-.45 4.5-1.25z" fill="#2684fc"/>
              <path d="m73.4 26.5-12.7-22c-.8-1.4-1.95-2.5-3.3-3.3l-13.75 23.8 16.15 28h27.45c0-1.55-.4-3.1-1.2-4.5z" fill="#ffba00"/>
            </svg>
          </div>

          <div className="max-w-xl space-y-2">
            <h3 className="text-xl font-bold text-white">
              اتصال ایمن سامانه NDN به حساب Google Drive
            </h3>
            <p className="text-xs text-gray-400 leading-relaxed">
              با اتصال حساب گوگل خود، سامانه تحویل محله (NDN) با اجازه و دسترسی شما امکان مشاهده فایل‌ها، ساخت پوشه‌های پستی، بارگذاری تصاویر و رسیدهای تحویل، و تهیه نسخه پشتیبان دائمی از وضعیت مرسولات را خواهد داشت.
            </p>
          </div>

          {/* Key Capabilities Pills */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 w-full max-w-2xl text-right">
            <div className="p-3.5 rounded-xl bg-white/5 border border-white/10 flex items-start gap-2.5">
              <span className="material-symbols-outlined text-amber-400 text-xl">cloud_sync</span>
              <div>
                <div className="text-xs font-bold text-white">پشتیبان‌گیری ۱-کلیکی</div>
                <div className="text-[10px] text-gray-400">انتقال آنی مرسولات و لاگ‌ها به درایو</div>
              </div>
            </div>
            <div className="p-3.5 rounded-xl bg-white/5 border border-white/10 flex items-start gap-2.5">
              <span className="material-symbols-outlined text-amber-400 text-xl">receipt_long</span>
              <div>
                <div className="text-xs font-bold text-white">آرشیو رسیدها و عکس‌ها</div>
                <div className="text-[10px] text-gray-400">ذخیره خودکار عکس بسته‌ها و بارکدها</div>
              </div>
            </div>
            <div className="p-3.5 rounded-xl bg-white/5 border border-white/10 flex items-start gap-2.5">
              <span className="material-symbols-outlined text-amber-400 text-xl">lock</span>
              <div>
                <div className="text-xs font-bold text-white">امنیت و رمزنگاری Google</div>
                <div className="text-[10px] text-gray-400">توکن در حافظه موقت بدون ذخیره محلی</div>
              </div>
            </div>
          </div>

          {/* Sign In Button */}
          <div className="pt-2">
            <GoogleSignInButton
              onClick={handleLogin}
              isLoading={isLoggingIn}
              text="ورود با حساب گوگل و اتصال Google Drive"
              className="px-8 py-3.5 text-sm"
            />
          </div>

          <span className="text-[10px] text-gray-500 font-mono">
            مجوزها: drive, drive.file, drive.metadata, drive.appdata
          </span>
        </div>
      </div>
    );
  }

  // Connected Google Drive Explorer
  return (
    <div className="w-full max-w-7xl mx-auto space-y-6 animate-in fade-in duration-200">
      
      {/* Top Header & Account Quota Strip */}
      <div className={`p-5 rounded-2xl border shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-4 ${
        isLuxury ? 'bg-[#0e0e0e] border-[#353535]' : 'bg-white border-[#c5c5d3]'
      }`}>
        {/* User Info & Connection Badge */}
        <div className="flex items-center gap-3.5">
          <div className="w-12 h-12 rounded-2xl bg-amber-400/20 border border-amber-400/40 overflow-hidden flex items-center justify-center flex-shrink-0">
            {driveAbout?.user?.photoLink ? (
              <img 
                src={driveAbout.user.photoLink} 
                alt={driveAbout.user.displayName || 'Google User'} 
                className="w-full h-full object-cover"
                referrerPolicy="no-referrer"
              />
            ) : (
              <span className="material-symbols-outlined text-2xl text-amber-400">account_circle</span>
            )}
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-sm md:text-base font-bold text-white">
                {driveAbout?.user?.displayName || 'حساب گوگل متصل'}
              </h3>
              <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 flex items-center gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                <span>Drive متصل است</span>
              </span>
            </div>
            <p className="text-xs text-gray-400 font-mono mt-0.5">
              {driveAbout?.user?.emailAddress || 'حساب احراز هویت شده'}
            </p>
          </div>
        </div>

        {/* Quota Progress & Disconnect */}
        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4 w-full md:w-auto">
          {driveAbout?.storageQuota && (
            <div className="min-w-[180px] space-y-1">
              <div className="flex justify-between text-[11px] text-gray-400">
                <span>فضای درایو:</span>
                <span className="font-mono text-amber-400">
                  {formatBytes(driveAbout.storageQuota.usage)} از {formatBytes(driveAbout.storageQuota.limit)}
                </span>
              </div>
              <div className="w-full h-2 rounded-full bg-white/10 overflow-hidden">
                <div 
                  className="h-full bg-gradient-to-r from-amber-500 to-yellow-400 rounded-full transition-all duration-500"
                  style={{ width: `${getQuotaPercent()}%` }}
                />
              </div>
            </div>
          )}

          <div className="flex items-center gap-2">
            <button
              onClick={() => setIsBackupOpen(true)}
              className="px-3.5 py-2 rounded-xl text-xs font-bold bg-gradient-to-r from-amber-500 to-yellow-400 text-black hover:brightness-110 shadow-md transition-all flex items-center gap-1.5 active:scale-95"
            >
              <span className="material-symbols-outlined text-base">cloud_sync</span>
              <span>پشتیبان‌گیری سامانه</span>
            </button>

            <button
              onClick={handleLogout}
              className="p-2 rounded-xl text-gray-400 hover:text-red-400 hover:bg-white/10 text-xs border border-white/5 transition-all"
              title="خروج از حساب گوگل"
            >
              <span className="material-symbols-outlined text-base">logout</span>
            </button>
          </div>
        </div>
      </div>

      {/* Toolbar: Breadcrumbs, Search, Filters, Create & Upload */}
      <div className={`p-4 rounded-2xl border shadow-sm space-y-3.5 ${
        isLuxury ? 'bg-[#121212] border-[#353535]' : 'bg-white border-gray-200'
      }`}>
        {/* Top actions row */}
        <div className="flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-3">
          
          {/* Breadcrumb path */}
          <div className="flex items-center gap-1.5 overflow-x-auto py-1 text-xs text-gray-300">
            {breadcrumbs.map((bc, idx) => {
              const isLast = idx === breadcrumbs.length - 1;
              return (
                <React.Fragment key={bc.id}>
                  {idx > 0 && <span className="text-gray-600">/</span>}
                  <button
                    onClick={() => handleBreadcrumbClick(idx)}
                    className={`px-2 py-1 rounded-lg transition-all truncate max-w-[150px] ${
                      isLast 
                        ? 'font-bold text-amber-400 bg-amber-400/10 border border-amber-400/30' 
                        : 'text-gray-400 hover:text-white hover:bg-white/5'
                    }`}
                  >
                    {idx === 0 ? '📁 ' + bc.name : bc.name}
                  </button>
                </React.Fragment>
              );
            })}
          </div>

          {/* Buttons: Upload, New Folder, Refresh */}
          <div className="flex items-center gap-2 flex-wrap justify-end">
            <button
              onClick={() => setIsUploadOpen(true)}
              className="px-3.5 py-2 rounded-xl text-xs font-bold bg-white text-gray-900 hover:bg-gray-100 shadow-sm transition-all flex items-center gap-1.5"
            >
              <span className="material-symbols-outlined text-base text-blue-600">upload_file</span>
              <span>آپلود فایل</span>
            </button>

            <button
              onClick={() => setIsNewFolderOpen(true)}
              className={`px-3.5 py-2 rounded-xl text-xs font-bold border transition-all flex items-center gap-1.5 ${
                isLuxury ? 'bg-white/5 border-white/15 text-white hover:bg-white/10' : 'bg-gray-100 border-gray-300 text-gray-800'
              }`}
            >
              <span className="material-symbols-outlined text-base text-amber-400">create_new_folder</span>
              <span>پوشه جدید</span>
            </button>

            <button
              onClick={loadFiles}
              disabled={isLoadingFiles}
              className={`p-2 rounded-xl border transition-all ${
                isLuxury ? 'bg-white/5 border-white/15 text-gray-300 hover:text-white' : 'bg-gray-100 text-gray-700'
              }`}
              title="بارگذاری مجدد"
            >
              <span className={`material-symbols-outlined text-base ${isLoadingFiles ? 'animate-spin' : ''}`}>
                refresh
              </span>
            </button>

            {/* View layout toggle */}
            <div className={`flex items-center rounded-xl p-0.5 border ${
              isLuxury ? 'bg-black/50 border-white/10' : 'bg-gray-100 border-gray-300'
            }`}>
              <button
                onClick={() => setViewLayout('grid')}
                className={`p-1.5 rounded-lg transition-all ${
                  viewLayout === 'grid' 
                    ? isLuxury ? 'bg-amber-400 text-black' : 'bg-white text-blue-900 shadow-sm'
                    : 'text-gray-400 hover:text-white'
                }`}
                title="نمایش شبکه‌ای"
              >
                <span className="material-symbols-outlined text-base">grid_view</span>
              </button>
              <button
                onClick={() => setViewLayout('list')}
                className={`p-1.5 rounded-lg transition-all ${
                  viewLayout === 'list' 
                    ? isLuxury ? 'bg-amber-400 text-black' : 'bg-white text-blue-900 shadow-sm'
                    : 'text-gray-400 hover:text-white'
                }`}
                title="نمایش لیستی"
              >
                <span className="material-symbols-outlined text-base">view_list</span>
              </button>
            </div>
          </div>
        </div>

        {/* Search & Filter row */}
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 pt-2 border-t border-white/5">
          {/* Search bar */}
          <div className="relative flex-1 max-w-md">
            <span className="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 text-lg">
              search
            </span>
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="جستجو در Google Drive..."
              className={`w-full pr-10 pl-4 py-2 rounded-xl border text-xs text-right focus:outline-none transition-all ${
                isLuxury 
                  ? 'bg-black/40 border-white/10 text-white focus:border-amber-400' 
                  : 'bg-gray-50 border-gray-300 text-gray-900 focus:border-blue-500'
              }`}
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white text-xs"
              >
                پاک کردن
              </button>
            )}
          </div>

          {/* Filter Pills */}
          <div className="flex items-center gap-1.5 overflow-x-auto py-1">
            {[
              { id: 'all', label: 'همه فایل‌ها' },
              { id: 'folders', label: 'پوشه‌ها' },
              { id: 'backups', label: 'پشتیبان‌های NDN' },
              { id: 'images', label: 'تصاویر و رسیدها' },
              { id: 'documents', label: 'اسناد' },
              { id: 'spreadsheets', label: 'اکسل و جداول' },
            ].map((f) => (
              <button
                key={f.id}
                onClick={() => setActiveMimeFilter(f.id as any)}
                className={`px-3 py-1.5 rounded-lg text-[11px] font-medium whitespace-nowrap transition-all ${
                  activeMimeFilter === f.id
                    ? isLuxury
                      ? 'bg-amber-400 text-black font-bold'
                      : 'bg-blue-600 text-white font-bold'
                    : isLuxury
                      ? 'bg-white/5 text-gray-400 hover:text-white hover:bg-white/10'
                      : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                {f.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Files Display Area */}
      {isLoadingFiles ? (
        <div className="w-full flex flex-col items-center justify-center min-h-[300px] gap-3">
          <div className="w-8 h-8 border-2 border-amber-400 border-t-transparent rounded-full animate-spin" />
          <span className="text-xs text-gray-400">در حال دریافت فایل‌ها از Google Drive...</span>
        </div>
      ) : files.length === 0 ? (
        <div className={`w-full p-12 rounded-2xl border text-center flex flex-col items-center justify-center gap-3 ${
          isLuxury ? 'bg-[#0e0e0e] border-[#353535]' : 'bg-white border-gray-200'
        }`}>
          <div className="w-16 h-16 rounded-full bg-white/5 border border-white/10 flex items-center justify-center text-gray-400">
            <span className="material-symbols-outlined text-3xl">folder_off</span>
          </div>
          <h4 className="text-sm font-bold text-white">هیچ فایلی در این بخش یافت نشد</h4>
          <p className="text-xs text-gray-400 max-w-sm">
            می‌توانید با استفاده از دکمه‌های بالا فایل جدید آپلود کرده یا یک نسخه پشتیبان از سامانه پستی ایجاد کنید.
          </p>
          <div className="flex gap-2 mt-2">
            <button
              onClick={() => setIsUploadOpen(true)}
              className="px-4 py-2 rounded-xl text-xs font-bold bg-amber-400 text-black hover:bg-amber-500"
            >
              آپلود اولین فایل
            </button>
            <button
              onClick={() => setIsBackupOpen(true)}
              className="px-4 py-2 rounded-xl text-xs font-bold bg-white/10 text-white hover:bg-white/20"
            >
              تهیه پشتیبان پستی
            </button>
          </div>
        </div>
      ) : viewLayout === 'grid' ? (
        /* GRID VIEW */
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          {files.map((file) => {
            const isFolder = file.mimeType === 'application/vnd.google-apps.folder';
            const icon = getFileIcon(file.mimeType);

            return (
              <div
                key={file.id}
                onClick={() => {
                  if (isFolder) handleNavigateToFolder(file);
                }}
                className={`p-4 rounded-2xl border transition-all flex flex-col justify-between group ${
                  isFolder ? 'cursor-pointer' : ''
                } ${
                  isLuxury 
                    ? 'bg-[#131313] border-[#353535] hover:border-amber-400/60 hover:bg-[#181818]' 
                    : 'bg-white border-gray-200 hover:border-blue-400 hover:shadow-md'
                }`}
              >
                <div>
                  {/* Top row: Icon + menu */}
                  <div className="flex items-start justify-between gap-2 mb-3">
                    <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                      isFolder 
                        ? 'bg-amber-400/20 text-amber-400 border border-amber-400/30' 
                        : 'bg-blue-500/20 text-blue-400 border border-blue-500/30'
                    }`}>
                      <span className="material-symbols-outlined text-2xl">{icon}</span>
                    </div>

                    {/* Quick Action buttons */}
                    <div className="flex items-center gap-1 opacity-80 group-hover:opacity-100">
                      {file.webViewLink && (
                        <a
                          href={file.webViewLink}
                          target="_blank"
                          rel="noopener noreferrer"
                          onClick={(e) => e.stopPropagation()}
                          className="p-1.5 rounded-lg text-gray-400 hover:text-amber-400 hover:bg-white/10"
                          title="مشاهده در Google Drive"
                        >
                          <span className="material-symbols-outlined text-base">open_in_new</span>
                        </a>
                      )}
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setFileToDelete(file);
                        }}
                        className="p-1.5 rounded-lg text-gray-400 hover:text-red-400 hover:bg-white/10"
                        title="حذف از Google Drive"
                      >
                        <span className="material-symbols-outlined text-base">delete</span>
                      </button>
                    </div>
                  </div>

                  {/* Thumbnail if present */}
                  {file.thumbnailLink && (
                    <div className="w-full h-24 mb-3 rounded-xl overflow-hidden bg-black/40 border border-white/5 flex items-center justify-center">
                      <img 
                        src={file.thumbnailLink} 
                        alt={file.name} 
                        className="w-full h-full object-cover"
                        referrerPolicy="no-referrer"
                      />
                    </div>
                  )}

                  {/* File name */}
                  <h4 className="text-xs font-bold text-white line-clamp-2 leading-snug mb-1" title={file.name}>
                    {file.name}
                  </h4>
                </div>

                {/* Meta footer */}
                <div className="flex items-center justify-between text-[10px] text-gray-400 pt-3 border-t border-white/5 mt-2 font-mono">
                  <span>{isFolder ? 'پوشه' : formatBytes(file.size)}</span>
                  <span>{file.modifiedTime ? new Date(file.modifiedTime).toLocaleDateString('fa-IR') : '—'}</span>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        /* LIST VIEW */
        <div className={`rounded-2xl border overflow-hidden shadow-sm ${
          isLuxury ? 'bg-[#121212] border-[#353535]' : 'bg-white border-gray-200'
        }`}>
          <div className="overflow-x-auto">
            <table className="w-full text-right text-xs">
              <thead className={`border-b text-gray-400 text-[11px] ${
                isLuxury ? 'bg-black/60 border-white/10' : 'bg-gray-50 border-gray-200'
              }`}>
                <tr>
                  <th className="py-3 px-4">نام فایل / پوشه</th>
                  <th className="py-3 px-4">نوع</th>
                  <th className="py-3 px-4">حجم</th>
                  <th className="py-3 px-4">آخرین ویرایش</th>
                  <th className="py-3 px-4 text-left">عملیات</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {files.map((file) => {
                  const isFolder = file.mimeType === 'application/vnd.google-apps.folder';
                  const icon = getFileIcon(file.mimeType);

                  return (
                    <tr 
                      key={file.id} 
                      onClick={() => {
                        if (isFolder) handleNavigateToFolder(file);
                      }}
                      className={`transition-colors ${isFolder ? 'cursor-pointer hover:bg-amber-400/5' : 'hover:bg-white/5'}`}
                    >
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-2.5">
                          <span className={`material-symbols-outlined text-lg ${isFolder ? 'text-amber-400' : 'text-blue-400'}`}>
                            {icon}
                          </span>
                          <span className="font-semibold text-white truncate max-w-xs">{file.name}</span>
                        </div>
                      </td>
                      <td className="py-3 px-4 text-gray-400 text-[11px]">
                        {isFolder ? 'پوشه درایو' : file.mimeType.split('/')[1] || 'فایل'}
                      </td>
                      <td className="py-3 px-4 font-mono text-gray-300">
                        {isFolder ? '—' : formatBytes(file.size)}
                      </td>
                      <td className="py-3 px-4 text-gray-400 font-mono text-[11px]">
                        {file.modifiedTime ? new Date(file.modifiedTime).toLocaleDateString('fa-IR') : '—'}
                      </td>
                      <td className="py-3 px-4 text-left">
                        <div className="flex items-center justify-end gap-1.5">
                          {file.webViewLink && (
                            <a
                              href={file.webViewLink}
                              target="_blank"
                              rel="noopener noreferrer"
                              onClick={(e) => e.stopPropagation()}
                              className="p-1.5 rounded-lg text-gray-400 hover:text-amber-400 hover:bg-white/10"
                              title="مشاهده در Google Drive"
                            >
                              <span className="material-symbols-outlined text-base">open_in_new</span>
                            </a>
                          )}
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setFileToDelete(file);
                            }}
                            className="p-1.5 rounded-lg text-gray-400 hover:text-red-400 hover:bg-white/10"
                            title="حذف فایل با تایید"
                          >
                            <span className="material-symbols-outlined text-base">delete</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Global Modals */}
      <DriveUploadModal
        isOpen={isUploadOpen}
        onClose={() => setIsUploadOpen(false)}
        onUploadSuccess={() => {
          showToast('فایل با موفقیت در Google Drive بارگذاری گردید.');
          loadFiles();
          loadDriveDetails();
        }}
        currentFolder={currentFolder}
        isLuxury={isLuxury}
      />

      <DriveNewFolderModal
        isOpen={isNewFolderOpen}
        onClose={() => setIsNewFolderOpen(false)}
        onSuccess={() => {
          showToast('پوشه جدید در Google Drive ساخته شد.');
          loadFiles();
        }}
        currentFolder={currentFolder}
        isLuxury={isLuxury}
      />

      <DriveBackupModal
        isOpen={isBackupOpen}
        onClose={() => setIsBackupOpen(false)}
        onSuccess={(name) => {
          showToast(`نسخه پشتیبان با نام "${name}" در Google Drive ذخیره شد.`);
          loadFiles();
          loadDriveDetails();
        }}
        isLuxury={isLuxury}
      />

      {/* MANDATORY Confirmation Dialog for Destructive Operations */}
      <DriveDeleteConfirmationModal
        isOpen={!!fileToDelete}
        file={fileToDelete}
        isDeleting={isDeleting}
        onConfirm={handleDeleteConfirmed}
        onCancel={() => setFileToDelete(null)}
        isLuxury={isLuxury}
      />

    </div>
  );
};
