import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import { HubItem } from '../../types';

export const HubsMapView: React.FC = () => {
  const { hubs, selectedHub, setSelectedHub, setActiveTab, showToast, theme } = useApp();
  const [selectedFilter, setSelectedFilter] = useState<string>('all');
  const [searchQuery, setSearchQuery] = useState('');

  const isLuxury = theme === 'luxury';

  const filterOptions = [
    { id: 'all', label: 'همه هاب‌ها' },
    { id: 'supermarket', label: 'سوپرمارکت' },
    { id: 'stationery', label: 'لوازم‌التحریر' },
    { id: 'netcafe', label: 'کافی‌نت' },
  ];

  const filteredHubs = hubs.filter((hub) => {
    const matchesFilter = selectedFilter === 'all' || hub.type === selectedFilter;
    const matchesSearch = hub.name.includes(searchQuery) || hub.address.includes(searchQuery);
    return matchesFilter && matchesSearch;
  });

  const handleSelectOriginHub = (hub: HubItem) => {
    setSelectedHub(hub);
    showToast(`هاب "${hub.name}" به عنوان هاب پستی انتخاب شد.`);
    setActiveTab('dashboard');
  };

  return (
    <div className="relative w-full min-h-[calc(100vh-140px)] flex flex-col justify-between overflow-hidden rounded-2xl border border-gray-800 shadow-2xl animate-in fade-in duration-200">
      
      {/* Background Interactive Styled Map with Markers (Matching Screen 9) */}
      <div className="absolute inset-0 z-0">
        <img
          src="https://lh3.googleusercontent.com/aida-public/AB6AXuDAa4ypKcmRsUBxkyqoCHinxdQKl0Zv6t5QcRqbNwzsoTm3KdA2NIivTzac46gZPnzohvPuiK_M3H1uaJSHckduDUxvho-liOVTdvwc7DahHW6vFUaxEQwileZwzeW-0Ks6nutIavRiegtIqg806-R2PsZZk2bEVwThXZlxYQCPT4RpeLBe7nO2J841q4ucCvwgAGzr4qP8d2HBymK7xc_93uruDhZ16zcrJ4FEcqx-RLKKWuAXic0o6A"
          alt="نقشه شهر و هاب‌ها"
          className="w-full h-full object-cover opacity-60 grayscale"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black via-black/40 to-black/80 pointer-events-none" />

        {/* Interactive Hub Pins on Map */}
        <div className="absolute inset-0 pointer-events-auto p-8">
          {filteredHubs.map((hub, index) => {
            const isCurrent = selectedHub.id === hub.id;
            // Simulated positions
            const topPos = index === 0 ? '45%' : index === 1 ? '30%' : index === 2 ? '60%' : '25%';
            const leftPos = index === 0 ? '50%' : index === 1 ? '35%' : index === 2 ? '65%' : '75%';

            return (
              <button
                key={hub.id}
                onClick={() => setSelectedHub(hub)}
                style={{ top: topPos, left: leftPos }}
                className={`absolute -translate-x-1/2 -translate-y-1/2 flex flex-col items-center gap-1 transition-transform duration-200 hover:scale-110 ${
                  isCurrent ? 'z-30 scale-110' : 'z-20'
                }`}
              >
                <div className={`p-2 rounded-full shadow-2xl flex items-center justify-center border-2 ${
                  isCurrent 
                    ? 'bg-[#d4af37] text-black border-white ring-4 ring-amber-400/40' 
                    : 'bg-black/90 text-[#f2ca50] border-[#d4af37]/60'
                }`}>
                  <span className="material-symbols-outlined text-lg">
                    {hub.type === 'supermarket' ? 'storefront' : hub.type === 'stationery' ? 'draw' : 'desktop_windows'}
                  </span>
                </div>
                <div className={`px-2 py-0.5 rounded-full text-[10px] font-bold whitespace-nowrap shadow-md border ${
                  isCurrent 
                    ? 'bg-[#d4af37] text-black border-amber-300' 
                    : 'bg-black/80 text-gray-200 border-white/10'
                }`}>
                  {hub.name}
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* Floating Search Bar & Filters (Top layer) */}
      <div className="relative z-10 w-full p-4 flex flex-col gap-3 max-w-xl mx-auto">
        
        {/* Search input (Matching Screen 9) */}
        <div className={`flex items-center gap-2 p-1.5 rounded-2xl border shadow-xl backdrop-blur-md ${
          isLuxury ? 'bg-[#0e0e0e]/90 border-[#353535]' : 'bg-white/95 border-gray-300'
        }`}>
          <span className="material-symbols-outlined text-gray-400 pr-3 text-xl">
            search
          </span>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="جستجوی محله یا کد پستی..."
            className="w-full bg-transparent border-none outline-none text-xs md:text-sm text-right px-2 py-1.5 text-white placeholder:text-gray-500"
          />
        </div>

        {/* Filter Chips (Matching Screen 9) */}
        <div className="flex gap-2 overflow-x-auto pb-1 no-scrollbar">
          {filterOptions.map((f) => {
            const isSelected = selectedFilter === f.id;
            return (
              <button
                key={f.id}
                onClick={() => setSelectedFilter(f.id)}
                className={`whitespace-nowrap px-4 py-1.5 rounded-full text-xs font-semibold shadow-md transition-all ${
                  isSelected
                    ? (isLuxury ? 'bg-[#d4af37] text-black' : 'bg-[#1e3a8a] text-white')
                    : (isLuxury ? 'bg-black/80 text-gray-300 border border-white/15 hover:bg-white/10' : 'bg-white text-gray-700 border border-gray-200')
                }`}
              >
                {f.label}
              </button>
            );
          })}
        </div>

      </div>

      {/* Selected Hub Bottom Sheet Card (Matching Screen 9) */}
      <div className="relative z-10 w-full p-4 max-w-xl mx-auto">
        <div className={`p-5 rounded-2xl border shadow-2xl backdrop-blur-md flex flex-col gap-3 text-right ${
          isLuxury ? 'bg-[#0e0e0e]/95 border-[#d4af37]/40 text-gray-100' : 'bg-white/95 border-blue-200 text-gray-900'
        }`}>
          
          <div className="flex justify-between items-start">
            <div className={`p-2.5 rounded-xl flex items-center justify-center ${
              isLuxury ? 'bg-[#1f1f1f] text-[#f2ca50]' : 'bg-blue-100 text-[#00236f]'
            }`}>
              <span className="material-symbols-outlined text-2xl">storefront</span>
            </div>

            <div className="flex flex-col items-end flex-1 pr-3">
              <div className="flex items-center gap-2">
                <span className="text-base md:text-lg font-bold text-white">
                  {selectedHub.name}
                </span>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                  {selectedHub.isOpen ? 'باز است' : 'بسته'}
                </span>
              </div>
              <p className="text-xs text-gray-400 mt-0.5 leading-relaxed">
                {selectedHub.address}
              </p>
            </div>
          </div>

          {/* Rating & Hours */}
          <div className="flex justify-between items-center py-2 border-y border-gray-800 text-xs">
            <div className="flex items-center gap-1 text-gray-400">
              <span className="material-symbols-outlined text-base">schedule</span>
              <span>{selectedHub.workingHours}</span>
            </div>
            <div className="flex items-center gap-1">
              <span className="material-symbols-outlined text-amber-400 text-base" style={{ fontVariationSettings: "'FILL' 1" }}>star</span>
              <span className="font-bold text-white font-mono">{selectedHub.rating}</span>
              <span className="text-gray-400 text-[11px]">({selectedHub.reviewCount} نظر)</span>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex gap-2 pt-1">
            <button
              onClick={() => handleSelectOriginHub(selectedHub)}
              className={`flex-1 py-2.5 px-4 rounded-xl font-bold text-xs shadow-lg transition-all active:scale-95 ${
                isLuxury
                  ? 'bg-[#d4af37] text-black hover:bg-[#e9c349]'
                  : 'bg-[#1e3a8a] text-white hover:bg-[#00236f]'
              }`}
            >
              انتخاب به عنوان هاب مبدا / مقصد
            </button>
            <button
              onClick={() => showToast('مسیر با موفقیت در نقشه محله رسم شد.')}
              className={`p-2.5 rounded-xl border flex items-center justify-center transition-colors ${
                isLuxury ? 'border-[#d4af37] text-[#f2ca50] hover:bg-[#d4af37]/10' : 'border-blue-700 text-blue-800'
              }`}
              title="مسیریابی"
            >
              <span className="material-symbols-outlined text-xl">directions</span>
            </button>
          </div>

        </div>
      </div>

    </div>
  );
};
