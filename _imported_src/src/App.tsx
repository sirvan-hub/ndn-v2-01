/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { AppProvider, useApp } from './context/AppContext';
import { TopHeader } from './components/layout/TopHeader';
import { BottomNav } from './components/layout/BottomNav';
import { SideNav } from './components/layout/SideNav';
import { DeviceSwitcherBar } from './components/layout/DeviceSwitcherBar';
import { DeviceFrameWrapper } from './components/layout/DeviceFrameWrapper';
import { CustomerView } from './components/views/CustomerView';
import { CourierView } from './components/views/CourierView';
import { HubManagerView } from './components/views/HubManagerView';
import { HubsMapView } from './components/views/HubsMapView';
import { SettingsView } from './components/views/SettingsView';
import { AuthView } from './components/views/AuthView';
import { GoogleDriveView } from './components/drive/GoogleDriveView';
import { BarcodeScannerModal } from './components/common/BarcodeScannerModal';
import { PackagePhotoModal } from './components/common/PackagePhotoModal';

const AppContent: React.FC = () => {
  const { role, activeTab, theme, toastMessage, isAuthenticated, deviceViewMode } = useApp();
  const isLuxury = theme === 'luxury';
  const isMobileSim = deviceViewMode === 'mobile';
  const isFramed = deviceViewMode !== 'responsive';

  const renderActiveView = () => {
    // If not authenticated, always show the auth/onboarding step flow unless in settings or drive
    if (!isAuthenticated && activeTab !== 'settings' && activeTab !== 'drive') {
      return <AuthView />;
    }

    switch (activeTab) {
      case 'map':
        return <HubsMapView />;
      case 'settings':
        return <SettingsView />;
      case 'drive':
        return <GoogleDriveView />;
      case 'auth':
      case 'profile':
        return <AuthView />;
      case 'packages':
      case 'dashboard':
      default:
        if (role === 'courier') {
          return <CourierView />;
        } else if (role === 'hub_manager') {
          return <HubManagerView />;
        } else {
          return <CustomerView />;
        }
    }
  };

  return (
    <DeviceFrameWrapper>
      <div className={`min-h-full w-full transition-colors duration-200 flex flex-col font-sans relative ${
        isLuxury ? 'bg-black text-gray-100 selection:bg-amber-500/30 selection:text-amber-200' : 'bg-[#f8f9ff] text-[#0b1c30] selection:bg-blue-200'
      }`}>
        {/* Top Fixed / Sticky Header */}
        <TopHeader />

        {/* Main Container Layout */}
        <div className={`flex-1 max-w-7xl w-full mx-auto ${isFramed ? 'pt-4' : 'pt-20'} pb-24 md:pb-12 px-3 sm:px-4 md:px-8 flex`}>
          
          {/* Desktop/Tablet Sidebar */}
          <SideNav />

          {/* Content Area */}
          <main className={`flex-1 ${isMobileSim ? 'pr-0' : 'md:pr-72'} transition-all duration-200 w-full`}>
            {renderActiveView()}
          </main>
        </div>

        {/* Mobile Bottom Navigation */}
        <BottomNav />

        {/* Global Interactive Modals */}
        <BarcodeScannerModal />
        <PackagePhotoModal />

        {/* Toast Notification Alert */}
        {toastMessage && (
          <div className="fixed bottom-20 md:bottom-6 left-1/2 -translate-x-1/2 z-50 animate-in fade-in slide-in-from-bottom-5 duration-200 pointer-events-none">
            <div className="flex items-center gap-2 px-4 py-3 rounded-xl bg-black/90 text-amber-300 border border-amber-500/50 shadow-2xl backdrop-blur-md text-xs font-semibold">
              <span className="material-symbols-outlined text-base text-amber-400">info</span>
              <span>{toastMessage}</span>
            </div>
          </div>
        )}
      </div>
    </DeviceFrameWrapper>
  );
};

export default function App() {
  return (
    <AppProvider>
      {/* Device Viewport Preview & Switcher Toolbar */}
      <DeviceSwitcherBar />
      <AppContent />
    </AppProvider>
  );
}

