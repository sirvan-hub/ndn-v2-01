import { initializeApp, getApps, getApp } from 'firebase/app';
import { 
  getAuth, 
  signInWithPopup, 
  GoogleAuthProvider, 
  onAuthStateChanged, 
  User,
  signOut
} from 'firebase/auth';
import firebaseConfig from '../../firebase-applet-config.json';
import { DriveAboutInfo, DriveFileItem } from '../types';

// Initialize Firebase App singleton
const app = !getApps().length ? initializeApp(firebaseConfig) : getApp();
export const auth = getAuth(app);

// Configure Google Provider with all required Google Drive scopes
export const DRIVE_SCOPES = [
  'https://www.googleapis.com/auth/drive',
  'https://www.googleapis.com/auth/drive.activity',
  'https://www.googleapis.com/auth/drive.activity.readonly',
  'https://www.googleapis.com/auth/drive.appdata',
  'https://www.googleapis.com/auth/drive.apps.readonly',
  'https://www.googleapis.com/auth/drive.file',
  'https://www.googleapis.com/auth/drive.install',
  'https://www.googleapis.com/auth/drive.meet.readonly',
  'https://www.googleapis.com/auth/drive.metadata',
  'https://www.googleapis.com/auth/drive.metadata.readonly',
  'https://www.googleapis.com/auth/drive.photos.readonly',
  'https://www.googleapis.com/auth/drive.readonly',
  'https://www.googleapis.com/auth/drive.scripts'
];

const provider = new GoogleAuthProvider();
DRIVE_SCOPES.forEach((scope) => provider.addScope(scope));
provider.setCustomParameters({
  prompt: 'select_account'
});

// Flag to track sign-in in progress
let isSigningIn = false;
// Cached access token in memory ONLY (never in localStorage or sessionStorage)
let cachedAccessToken: string | null = null;

/**
 * Initialize auth state listener.
 */
export const initAuth = (
  onAuthSuccess?: (user: User, token: string) => void,
  onAuthFailure?: () => void
) => {
  return onAuthStateChanged(auth, async (user: User | null) => {
    if (user) {
      if (cachedAccessToken) {
        if (onAuthSuccess) onAuthSuccess(user, cachedAccessToken);
      } else if (!isSigningIn) {
        cachedAccessToken = null;
        if (onAuthFailure) onAuthFailure();
      }
    } else {
      cachedAccessToken = null;
      if (onAuthFailure) onAuthFailure();
    }
  });
};

/**
 * Trigger Google Sign In popup with Google Drive scopes
 */
export const googleSignIn = async (): Promise<{ user: User; accessToken: string } | null> => {
  try {
    isSigningIn = true;
    const result = await signInWithPopup(auth, provider);
    const credential = GoogleAuthProvider.credentialFromResult(result);
    if (!credential?.accessToken) {
      throw new Error('توکن دسترسی معتبر از گوگل دریافت نشد.');
    }

    cachedAccessToken = credential.accessToken;
    return { user: result.user, accessToken: cachedAccessToken };
  } catch (error: any) {
    console.error('Google Sign in error:', error);
    throw error;
  } finally {
    isSigningIn = false;
  }
};

/**
 * Retrieve the current in-memory access token
 */
export const getAccessToken = async (): Promise<string | null> => {
  return cachedAccessToken;
};

/**
 * Set an access token manually into memory
 */
export const setCachedAccessToken = (token: string | null) => {
  cachedAccessToken = token;
};

/**
 * Sign out of Google and clear in-memory token
 */
export const logoutGoogle = async () => {
  try {
    await signOut(auth);
  } catch (e) {
    console.error('Sign out error:', e);
  } finally {
    cachedAccessToken = null;
  }
};

/**
 * Helper to make authenticated fetch requests to Google Drive v3 APIs
 */
async function driveFetch(endpoint: string, options: RequestInit = {}): Promise<any> {
  const token = await getAccessToken();
  if (!token) {
    throw new Error('برای دسترسی به گوگل درایو باید ابتدا وارد حساب کاربری گوگل خود شوید.');
  }

  const headers = new Headers(options.headers || {});
  headers.set('Authorization', `Bearer ${token}`);

  const response = await fetch(endpoint, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    cachedAccessToken = null;
    throw new Error('نشست کاری گوگل شما منقضی شده است. لطفا مجددا وارد شوید.');
  }

  if (response.status === 204) {
    return true;
  }

  const responseData = await response.json().catch(() => ({}));
  if (!response.ok) {
    const message = responseData?.error?.message || `خطای ارتباط با سرور گوگل درایو (${response.status})`;
    throw new Error(message);
  }

  return responseData;
}

/**
 * Fetch Google Drive storage quota and user info
 */
export async function getDriveAbout(): Promise<DriveAboutInfo> {
  const data = await driveFetch('https://www.googleapis.com/drive/v3/about?fields=user,storageQuota');
  return {
    user: data.user,
    storageQuota: data.storageQuota
  };
}

/**
 * List files and folders from Google Drive
 */
export async function listDriveFiles(params: {
  folderId?: string;
  query?: string;
  mimeFilter?: 'all' | 'folders' | 'images' | 'documents' | 'spreadsheets' | 'backups';
  pageSize?: number;
  pageToken?: string;
  trashed?: boolean;
}): Promise<{ files: DriveFileItem[]; nextPageToken?: string }> {
  const { folderId, query, mimeFilter = 'all', pageSize = 40, pageToken, trashed = false } = params;

  const queryParts: string[] = [`trashed = ${trashed}`];

  if (folderId && folderId !== 'root') {
    queryParts.push(`'${folderId}' in parents`);
  } else if (!query && (!folderId || folderId === 'root')) {
    // If no search query and at root, show items in 'root'
    queryParts.push(`'root' in parents`);
  }

  if (query && query.trim().length > 0) {
    const escapedQuery = query.replace(/'/g, "\\'");
    queryParts.push(`name contains '${escapedQuery}'`);
  }

  if (mimeFilter === 'folders') {
    queryParts.push(`mimeType = 'application/vnd.google-apps.folder'`);
  } else if (mimeFilter === 'images') {
    queryParts.push(`(mimeType contains 'image/' or mimeType = 'application/pdf')`);
  } else if (mimeFilter === 'documents') {
    queryParts.push(`(mimeType contains 'document' or mimeType contains 'text/' or mimeType = 'application/pdf')`);
  } else if (mimeFilter === 'spreadsheets') {
    queryParts.push(`(mimeType contains 'spreadsheet' or mimeType contains 'excel' or mimeType = 'text/csv')`);
  } else if (mimeFilter === 'backups') {
    queryParts.push(`(name contains 'NDN_Backup' or mimeType = 'application/json')`);
  }

  const q = queryParts.join(' and ');
  const fields = 'nextPageToken,files(id,name,mimeType,size,modifiedTime,createdTime,webViewLink,webContentLink,thumbnailLink,iconLink,shared,starred,trashed,parents,description)';
  const url = new URL('https://www.googleapis.com/drive/v3/files');
  url.searchParams.set('q', q);
  url.searchParams.set('fields', fields);
  url.searchParams.set('pageSize', pageSize.toString());
  url.searchParams.set('orderBy', 'folder,modifiedTime desc');

  if (pageToken) {
    url.searchParams.set('pageToken', pageToken);
  }

  const data = await driveFetch(url.toString());
  return {
    files: data.files || [],
    nextPageToken: data.nextPageToken
  };
}

/**
 * Create a new folder in Google Drive
 */
export async function createDriveFolder(name: string, parentFolderId?: string): Promise<DriveFileItem> {
  const metadata: any = {
    name,
    mimeType: 'application/vnd.google-apps.folder'
  };

  if (parentFolderId && parentFolderId !== 'root') {
    metadata.parents = [parentFolderId];
  }

  const data = await driveFetch('https://www.googleapis.com/drive/v3/files?fields=id,name,mimeType,webViewLink,createdTime', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(metadata)
  });

  return data;
}

/**
 * Find or create the dedicated NDN system backups folder
 */
export async function getOrCreateNDNBackupFolder(): Promise<DriveFileItem> {
  const folderName = 'NDN_Postal_Backups';
  const search = await listDriveFiles({
    query: folderName,
    mimeFilter: 'folders'
  });

  const existing = search.files.find(f => f.name === folderName && f.mimeType === 'application/vnd.google-apps.folder');
  if (existing) {
    return existing;
  }

  return await createDriveFolder(folderName);
}

/**
 * Upload a binary or image file to Google Drive using multipart upload
 */
export async function uploadFileToDrive(
  file: File, 
  parentFolderId?: string
): Promise<DriveFileItem> {
  const token = await getAccessToken();
  if (!token) throw new Error('کاربر در گوگل لاگین نیست.');

  const metadata: any = {
    name: file.name,
    mimeType: file.type || 'application/octet-stream'
  };

  if (parentFolderId && parentFolderId !== 'root') {
    metadata.parents = [parentFolderId];
  }

  const boundary = '-------314159265358979323846';
  const delimiter = `\r\n--${boundary}\r\n`;
  const closeDelimiter = `\r\n--${boundary}--`;

  const reader = new FileReader();
  const fileArrayBuffer = await new Promise<ArrayBuffer>((resolve, reject) => {
    reader.onload = () => resolve(reader.result as ArrayBuffer);
    reader.onerror = reject;
    reader.readAsArrayBuffer(file);
  });

  const metadataPart = `${delimiter}Content-Type: application/json; charset=UTF-8\r\n\r\n${JSON.stringify(metadata)}\r\n`;
  const fileHeaderPart = `${delimiter}Content-Type: ${file.type || 'application/octet-stream'}\r\nContent-Transfer-Encoding: base64\r\n\r\n`;

  // Convert ArrayBuffer to Base64
  let binary = '';
  const bytes = new Uint8Array(fileArrayBuffer);
  const len = bytes.byteLength;
  for (let i = 0; i < len; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  const base64Data = btoa(binary);

  const multipartBody = metadataPart + fileHeaderPart + base64Data + closeDelimiter;

  const response = await fetch('https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,mimeType,size,webViewLink,thumbnailLink,modifiedTime', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': `multipart/related; boundary=${boundary}`
    },
    body: multipartBody
  });

  if (!response.ok) {
    const err = await response.json().catch(() => ({}));
    throw new Error(err?.error?.message || 'خطا در بارگذاری فایل در گوگل درایو');
  }

  return await response.json();
}

/**
 * Upload a JSON structure (e.g. system backup, package report) directly to Google Drive
 */
export async function uploadJsonToDrive(
  fileName: string, 
  jsonData: any, 
  parentFolderId?: string
): Promise<DriveFileItem> {
  const token = await getAccessToken();
  if (!token) throw new Error('کاربر در گوگل لاگین نیست.');

  const metadata: any = {
    name: fileName,
    mimeType: 'application/json'
  };

  if (parentFolderId && parentFolderId !== 'root') {
    metadata.parents = [parentFolderId];
  }

  const boundary = '-------314159265358979323846';
  const delimiter = `\r\n--${boundary}\r\n`;
  const closeDelimiter = `\r\n--${boundary}--`;

  const jsonString = JSON.stringify(jsonData, null, 2);
  const metadataPart = `${delimiter}Content-Type: application/json; charset=UTF-8\r\n\r\n${JSON.stringify(metadata)}\r\n`;
  const dataPart = `${delimiter}Content-Type: application/json; charset=UTF-8\r\n\r\n${jsonString}`;
  const multipartBody = metadataPart + dataPart + closeDelimiter;

  const response = await fetch('https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,mimeType,size,webViewLink,modifiedTime', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': `multipart/related; boundary=${boundary}`
    },
    body: multipartBody
  });

  if (!response.ok) {
    const err = await response.json().catch(() => ({}));
    throw new Error(err?.error?.message || 'خطا در ذخیره داده‌ها در گوگل درایو');
  }

  return await response.json();
}

/**
 * Delete a file or folder permanently from Google Drive (Requires user confirmation)
 */
export async function deleteDriveFile(fileId: string): Promise<boolean> {
  await driveFetch(`https://www.googleapis.com/drive/v3/files/${fileId}`, {
    method: 'DELETE'
  });
  return true;
}

/**
 * Move a file or folder to/from trash in Google Drive
 */
export async function setFileTrashedState(fileId: string, trashed: boolean): Promise<DriveFileItem> {
  const data = await driveFetch(`https://www.googleapis.com/drive/v3/files/${fileId}`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ trashed })
  });
  return data;
}

/**
 * Format bytes to readable size
 */
export function formatBytes(bytesStr?: string | number, decimals = 1): string {
  if (!bytesStr) return '—';
  const bytes = typeof bytesStr === 'string' ? parseInt(bytesStr, 10) : bytesStr;
  if (isNaN(bytes) || bytes === 0) return '۰ بایت';

  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['بایت', 'کیلوبایت', 'مگابایت', 'گیگابایت', 'ترابایت'];

  const i = Math.floor(Math.log(bytes) / Math.log(k));
  const val = parseFloat((bytes / Math.pow(k, i)).toFixed(dm));
  return `${val} ${sizes[i]}`;
}
