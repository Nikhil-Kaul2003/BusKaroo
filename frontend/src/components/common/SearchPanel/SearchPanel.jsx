import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Button, Card } from '../index';
import { terminalsApi } from '../../../api/terminals';
import './SearchPanel.css';
// Suggestion Icons
const PinIcon = () => (<svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" strokeWidth="1.25" fill="none" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0, color: 'var(--secondary)' }}>
    <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
    <circle cx="12" cy="10" r="3"/>
  </svg>);
const BusIcon = () => (<svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" strokeWidth="1.1" fill="none" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0, color: 'var(--secondary)' }}>
    <rect x="4" y="3" width="16" height="15" rx="3"/>
    <rect x="6" y="6" width="12" height="5" rx="1"/>
    <circle cx="8" cy="15" r="1"/>
    <circle cx="16" cy="15" r="1"/>
    <path d="M6 18v3"/>
    <path d="M18 21v-3"/>
  </svg>);
// Diacritic & Accent Normalization
const normalizeText = (text) => {
    return text
        .toLowerCase()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '');
};
// Word-boundary Prefix Search
const matchesSearch = (text, query) => {
    const normalizedText = normalizeText(text);
    const normalizedQuery = normalizeText(query);
    if (!normalizedQuery)
        return true;
    const words = normalizedText.split(/[\s,.-]+/);
    return words.some(word => word.startsWith(normalizedQuery));
};
// Clean terminal name by stripping the city name prefix
const getCleanTerminalName = (cityName, terminalName) => {
    const cityLower = cityName.toLowerCase();
    const nameLower = terminalName.toLowerCase();
    if (nameLower.startsWith(cityLower)) {
        return terminalName.substring(cityName.length).replace(/^[\s,.-]+/, '');
    }
    return terminalName;
};
// Date formatting helper for local dates
const getMinDate = () => {
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const dd = String(today.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
};
const getMaxDate = () => {
    const today = new Date();
    // End of the month 3 months after current month (e.g. if June (5), get October (9) index 0th day which is Sept 30)
    const maxDate = new Date(today.getFullYear(), today.getMonth() + 4, 0);
    const yyyy = maxDate.getFullYear();
    const mm = String(maxDate.getMonth() + 1).padStart(2, '0');
    const dd = String(maxDate.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
};
// Bump the key when the location dataset changes so older browser caches do
// not hide newly seeded Indian destinations such as Leh.
const TERMINAL_CACHE_KEY = 'cached_terminals_v2';
export const SearchPanel = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    // Load initial params from URL if present (useful for SearchPage pre-filling)
    const urlDate = searchParams.get('date');
    const urlPassengers = searchParams.get('passengers');
    const urlTimeWindow = searchParams.get('timeWindow') || 'any';
    const [terminals, setTerminals] = useState([]);
    // Search states
    const [fromSearch, setFromSearch] = useState('');
    const [toSearch, setToSearch] = useState('');
    const [fromTerminal, setFromTerminal] = useState(null);
    const [toTerminal, setToTerminal] = useState(null);
    const [date, setDate] = useState(() => urlDate || getMinDate() || '');
    const [passengers, setPassengers] = useState(() => Number(urlPassengers) || 1);
    const [timeWindow, setTimeWindow] = useState(urlTimeWindow);
    // Suggestions visibility
    const [showFromSuggestions, setShowFromSuggestions] = useState(false);
    const [showToSuggestions, setShowToSuggestions] = useState(false);
    // Color & Rotation swapped state
    const [isSwapped, setIsSwapped] = useState(false);
    const [loading, setLoading] = useState(false);
    const fromContainerRef = useRef(null);
    const toContainerRef = useRef(null);
    // Pre-fill locations from URL search params
    const prefillFromUrl = React.useCallback((termList) => {
        const fromCity = searchParams.get('fromCity');
        const fromTerminalId = searchParams.get('fromTerminalId');
        const toCity = searchParams.get('toCity');
        const toTerminalId = searchParams.get('toTerminalId');
        if (termList.length === 0)
            return;
        if (fromCity) {
            let matchingTerm;
            if (fromTerminalId && Number(fromTerminalId) > 0) {
                matchingTerm = termList.find(t => t.id === Number(fromTerminalId));
            }
            if (!matchingTerm) {
                matchingTerm = termList.find(t => t.city.toLowerCase() === fromCity.toLowerCase());
                if (fromTerminalId && Number(fromTerminalId) < 0) {
                    const index = termList.findIndex(t => t.city.toLowerCase() === fromCity.toLowerCase());
                    if (index !== -1) {
                        matchingTerm = {
                            id: Number(fromTerminalId),
                            name: 'All stations',
                            city: termList[index].city,
                            country: termList[index].country
                        };
                    }
                }
            }
            if (matchingTerm) {
                setFromTerminal(matchingTerm);
                const cleanName = getCleanTerminalName(matchingTerm.city, matchingTerm.name);
                const displayText = matchingTerm.name === 'All stations'
                    ? `${matchingTerm.city}, ${matchingTerm.country}`
                    : `${matchingTerm.city} – ${cleanName}`;
                setFromSearch(displayText);
            }
        }
        if (toCity) {
            let matchingTerm;
            if (toTerminalId && Number(toTerminalId) > 0) {
                matchingTerm = termList.find(t => t.id === Number(toTerminalId));
            }
            if (!matchingTerm) {
                matchingTerm = termList.find(t => t.city.toLowerCase() === toCity.toLowerCase());
                if (toTerminalId && Number(toTerminalId) < 0) {
                    const index = termList.findIndex(t => t.city.toLowerCase() === toCity.toLowerCase());
                    if (index !== -1) {
                        matchingTerm = {
                            id: Number(toTerminalId),
                            name: 'All stations',
                            city: termList[index].city,
                            country: termList[index].country
                        };
                    }
                }
            }
            if (matchingTerm) {
                setToTerminal(matchingTerm);
                const cleanName = getCleanTerminalName(matchingTerm.city, matchingTerm.name);
                const displayText = matchingTerm.name === 'All stations'
                    ? `${matchingTerm.city}, ${matchingTerm.country}`
                    : `${matchingTerm.city} – ${cleanName}`;
                setToSearch(displayText);
            }
        }
    }, [searchParams]);
    // Show cached locations immediately, then always refresh from the backend.
    // Routes and terminals can be added while the app is already in use, so a
    // long-lived cache must never be the only source of autocomplete results.
    useEffect(() => {
        const loadTerminals = async () => {
            const cachedStr = localStorage.getItem(TERMINAL_CACHE_KEY);
            const now = new Date().getTime();
            const oneDay = 24 * 60 * 60 * 1000;
            let cachedTerminals = [];
            // 1. Use the versioned cache for an instant first render.
            if (cachedStr) {
                try {
                    const parsed = JSON.parse(cachedStr);
                    if (parsed && Array.isArray(parsed.data) && typeof parsed.expiry === 'number') {
                        if (now < parsed.expiry) {
                            setTerminals(parsed.data);
                            cachedTerminals = parsed.data;
                        }
                    }
                }
                catch (e) {
                    console.warn('Failed to parse cached terminals', e);
                }
            }
            // 2. Refresh on every mount so newly added cities appear without waiting
            // for the cache TTL to expire.
            try {
                const data = await terminalsApi.getAll();
                setTerminals(data);
                cachedTerminals = data;
                const cacheWrapper = {
                    data,
                    expiry: now + oneDay
                };
                localStorage.setItem(TERMINAL_CACHE_KEY, JSON.stringify(cacheWrapper));
            }
            catch (err) {
                console.error('Failed to refresh terminals from API', err);
                // Keep the last cache available when the backend is temporarily offline.
            }
            // 3. Pre-fill states once terminal data and URL params are available.
            if (cachedTerminals.length > 0) {
                prefillFromUrl(cachedTerminals);
            }
        };
        loadTerminals();
    }, [searchParams, prefillFromUrl]);
    // Click outside to close autocompletes
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (fromContainerRef.current && !fromContainerRef.current.contains(event.target)) {
                setShowFromSuggestions(false);
            }
            if (toContainerRef.current && !toContainerRef.current.contains(event.target)) {
                setShowToSuggestions(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);
    // Filter suggestions from cached list
    const getFilteredSuggestions = (search, excludedTerminal, currentSelectedTerminal = null) => {
        const cleanSearch = search.trim().toLowerCase();
        // If a terminal is currently selected and the input text matches its display representation,
        // show the city header and all terminals in that city.
        if (currentSelectedTerminal) {
            const cityName = currentSelectedTerminal.city;
            const cityLower = cityName.toLowerCase();
            const sampleTerm = terminals.find(t => t.city.toLowerCase() === cityLower);
            if (sampleTerm) {
                const isExcluded = (t) => {
                    if (!excludedTerminal)
                        return false;
                    return t.id === excludedTerminal.id || t.city.toLowerCase() === excludedTerminal.city.toLowerCase();
                };
                const suggested = [];
                // Add the city header (virtual terminal)
                suggested.push({
                    id: -1000 - terminals.findIndex(term => term.city.toLowerCase() === cityLower),
                    name: 'All stations',
                    city: cityName,
                    country: sampleTerm.country
                });
                // Add ALL terminals of this city in the database (except excluded ones)
                const cityTerminals = terminals.filter(t => t.city.toLowerCase() === cityLower && !isExcluded(t));
                suggested.push(...cityTerminals);
                return suggested;
            }
        }
        // Show every available city when the field is blank. The list is scrollable,
        // and typing filters it, so less common destinations such as Leh are still
        // discoverable without knowing that a search term is required.
        if (cleanSearch.length < 2) {
            const suggestedCities = Array.from(new Map(terminals
                .filter(t => t.country?.toLowerCase() === 'india')
                .map(t => [t.city.toLowerCase(), { city: t.city, country: t.country }])).values()).sort((a, b) => a.city.localeCompare(b.city));
            const suggested = [];
            suggestedCities.forEach((item, index) => {
                // Exclude the currently selected terminal on the other field
                if (excludedTerminal && excludedTerminal.city.toLowerCase() === item.city.toLowerCase()) {
                    return;
                }
                const termIndex = terminals.findIndex(term => term.city.toLowerCase() === item.city.toLowerCase());
                const id = termIndex !== -1 ? -1000 - termIndex : -2000 - index;
                suggested.push({
                    id,
                    name: 'All stations',
                    city: item.city,
                    country: item.country
                });
            });
            return suggested;
        }
        // Helper to determine if a terminal belongs to the excluded selection (same terminal OR same city)
        const isExcluded = (t) => {
            if (!excludedTerminal)
                return false;
            return t.id === excludedTerminal.id || t.city.toLowerCase() === excludedTerminal.city.toLowerCase();
        };
        // Filter terminals matching prefix word-boundaries
        const matches = terminals.filter(t => !isExcluded(t) && (matchesSearch(t.city, cleanSearch) ||
            matchesSearch(t.name, cleanSearch) ||
            matchesSearch(t.country, cleanSearch)));
        // Group matches into: City matches vs other matches (only terminal/country matched)
        const matchedCities = Array.from(new Set(matches.map(t => t.city)));
        // Split into priority cities (where city name itself matches query) and secondary cities
        const priorityCities = matchedCities.filter(cityName => matchesSearch(cityName, cleanSearch));
        const secondaryCities = matchedCities.filter(cityName => !matchesSearch(cityName, cleanSearch));
        const suggested = [];
        const addedCities = new Set();
        // 1. Process priority cities first: add city header and ALL of its terminals
        priorityCities.forEach(cityName => {
            const cityLower = cityName.toLowerCase();
            if (addedCities.has(cityLower))
                return;
            addedCities.add(cityLower);
            // Find first real terminal to get the country name
            const sampleTerm = terminals.find(t => t.city.toLowerCase() === cityLower);
            if (!sampleTerm)
                return;
            // Add the city header (virtual terminal)
            suggested.push({
                id: -1000 - terminals.findIndex(term => term.city.toLowerCase() === cityLower),
                name: 'All stations',
                city: cityName,
                country: sampleTerm.country
            });
            // Add ALL terminals of this city in the database (except excluded ones)
            const cityTerminals = terminals.filter(t => t.city.toLowerCase() === cityLower && !isExcluded(t));
            suggested.push(...cityTerminals);
        });
        // 2. Process secondary cities next: add city header and ONLY the terminals that matched
        secondaryCities.forEach(cityName => {
            const cityLower = cityName.toLowerCase();
            if (addedCities.has(cityLower))
                return;
            addedCities.add(cityLower);
            // Find first real terminal to get the country name
            const sampleTerm = terminals.find(t => t.city.toLowerCase() === cityLower);
            if (!sampleTerm)
                return;
            // Add the city header (virtual terminal)
            suggested.push({
                id: -1000 - terminals.findIndex(term => term.city.toLowerCase() === cityLower),
                name: 'All stations',
                city: cityName,
                country: sampleTerm.country
            });
            // Add ONLY the matching terminals of this city
            const cityMatchingTerminals = matches.filter(t => t.city.toLowerCase() === cityLower);
            suggested.push(...cityMatchingTerminals);
        });
        return suggested;
    };
    const handleSelectFrom = (terminal) => {
        setFromTerminal(terminal);
        const cleanName = getCleanTerminalName(terminal.city, terminal.name);
        const displayText = terminal.name === 'All stations'
            ? `${terminal.city}, ${terminal.country}`
            : `${terminal.city} – ${cleanName}`;
        setFromSearch(displayText);
        setShowFromSuggestions(false);
    };
    const handleSelectTo = (terminal) => {
        setToTerminal(terminal);
        const cleanName = getCleanTerminalName(terminal.city, terminal.name);
        const displayText = terminal.name === 'All stations'
            ? `${terminal.city}, ${terminal.country}`
            : `${terminal.city} – ${cleanName}`;
        setToSearch(displayText);
        setShowToSuggestions(false);
    };
    const handleSwap = () => {
        const tempSearch = fromSearch;
        const tempTerminal = fromTerminal;
        setFromSearch(toSearch);
        setFromTerminal(toTerminal);
        setToSearch(tempSearch);
        setToTerminal(tempTerminal);
        setIsSwapped(prev => !prev); // Toggle visual swapped state
    };
    const handleSearchSubmit = (e) => {
        e.preventDefault();
        if (!fromTerminal || !toTerminal || !date)
            return;
        setLoading(true);
        const fromId = fromTerminal.id;
        const toId = toTerminal.id;
        const params = new URLSearchParams({
            fromCity: fromTerminal.city,
            fromTerminalId: String(fromId),
            toCity: toTerminal.city,
            toTerminalId: String(toId),
            date,
            passengers: String(passengers),
            timeWindow,
        });
        navigate(`/search?${params.toString()}`);
        setLoading(false);
    };
    return (<Card className="search-card">
      <form onSubmit={handleSearchSubmit} className="search-form">
        <div className="search-fields-grid">

          {/* Field 1: From Autocomplete with Integrated Inline Swap */}
          <div className="search-field autocomplete-field" ref={fromContainerRef}>
            <label htmlFor="from-search-input">From</label>
            <div className="input-with-swap-container">
              <input id="from-search-input" name="fromSearch" type="text" placeholder="Departure city" autoComplete="off" value={fromSearch} onChange={(e) => {
            setFromSearch(e.target.value);
            if (fromTerminal)
                setFromTerminal(null);
        }} onFocus={() => setShowFromSuggestions(true)} required className="search-input"/>
              {fromSearch && (<button type="button" onClick={() => {
                setFromSearch('');
                setFromTerminal(null);
                setShowFromSuggestions(true);
            }} className="clear-button-inline from-clear" title="Clear input">
                  <svg viewBox="0 0 24 24" width="12" height="12" stroke="currentColor" strokeWidth="2.5" fill="none" strokeLinecap="round" strokeLinejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                  </svg>
                </button>)}
              <button type="button" onClick={handleSwap} className={`swap-button-inline ${isSwapped ? 'swapped' : ''}`} title="Swap locations">
                <svg viewBox="0 0 24 24" width="20" /* TO ADJUST SVG SIZE: Change width and height here to match the swap button container size (e.g., 20 is even to prevent subpixel layout jerks) */ height="20" stroke="currentColor" strokeWidth="2.2" fill="none" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M4 8h16M16 4l4 4-4 4"/>
                  <path d="M20 16H4M8 12l-4 4 4 4"/>
                </svg>
              </button>
            </div>
            {showFromSuggestions && (<div className="autocomplete-suggestions">
                <div className="suggestions-header">
                  {fromSearch.trim().length < 2 && !fromTerminal ? 'Popular Destinations' : 'Matching Locations'}
                </div>
                {getFilteredSuggestions(fromSearch, toTerminal, fromTerminal).length > 0 ? (getFilteredSuggestions(fromSearch, toTerminal, fromTerminal).map((term, index) => {
                const cleanName = getCleanTerminalName(term.city, term.name);
                return (<div key={`${term.id}-${index}`} className="suggestion-item" style={term.name !== 'All stations' ? { paddingLeft: '44px' } : undefined} onClick={() => handleSelectFrom(term)}>
                        <div className="suggestion-icon-wrapper">
                          {term.name === 'All stations' ? <PinIcon /> : <BusIcon />}
                        </div>
                        <div className="suggestion-details">
                          {term.name === 'All stations' ? (<span className="suggestion-city">
                              {term.city}<span className="suggestion-separator">, {term.country}</span>
                            </span>) : (<span className="suggestion-city">
                              {term.city}
                              <span className="suggestion-separator"> – </span>
                              <span className="suggestion-terminal">{cleanName}</span>
                            </span>)}
                        </div>
                      </div>);
            })) : (<div className="suggestions-empty">No locations found</div>)}
              </div>)}
          </div>

          {/* Field 2: To Autocomplete */}
          <div className="search-field autocomplete-field" ref={toContainerRef}>
            <label htmlFor="to-search-input">To</label>
            <div className="input-with-clear-container">
              <input id="to-search-input" name="toSearch" type="text" placeholder="Arrival city" autoComplete="off" value={toSearch} onChange={(e) => {
            setToSearch(e.target.value);
            if (toTerminal)
                setToTerminal(null);
        }} onFocus={() => setShowToSuggestions(true)} required className="search-input"/>
              {toSearch && (<button type="button" onClick={() => {
                setToSearch('');
                setToTerminal(null);
                setShowToSuggestions(true);
            }} className="clear-button-inline to-clear" title="Clear input">
                  <svg viewBox="0 0 24 24" width="12" height="12" stroke="currentColor" strokeWidth="2.5" fill="none" strokeLinecap="round" strokeLinejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                  </svg>
                </button>)}
            </div>
            {showToSuggestions && (<div className="autocomplete-suggestions">
                <div className="suggestions-header">
                  {toSearch.trim().length < 2 && !toTerminal ? 'Popular Destinations' : 'Matching Locations'}
                </div>
                {getFilteredSuggestions(toSearch, fromTerminal, toTerminal).length > 0 ? (getFilteredSuggestions(toSearch, fromTerminal, toTerminal).map((term, index) => {
                const cleanName = getCleanTerminalName(term.city, term.name);
                return (<div key={`${term.id}-${index}`} className="suggestion-item" style={term.name !== 'All stations' ? { paddingLeft: '44px' } : undefined} onClick={() => handleSelectTo(term)}>
                        <div className="suggestion-icon-wrapper">
                          {term.name === 'All stations' ? <PinIcon /> : <BusIcon />}
                        </div>
                        <div className="suggestion-details">
                          {term.name === 'All stations' ? (<span className="suggestion-city">
                              {term.city}<span className="suggestion-separator">, {term.country}</span>
                            </span>) : (<span className="suggestion-city">
                              {term.city}
                              <span className="suggestion-separator"> – </span>
                              <span className="suggestion-terminal">{cleanName}</span>
                            </span>)}
                        </div>
                      </div>);
            })) : (<div className="suggestions-empty">No locations found</div>)}
              </div>)}
          </div>

          {/* Field 3: Date Selector */}
          <div className="search-field date-field">
            <label htmlFor="date-input">Date</label>
            <input id="date-input" name="date" type="date" autoComplete="off" value={date} onChange={(e) => setDate(e.target.value)} onClick={(e) => {
            try {
                const target = e.currentTarget;
                if (typeof target.showPicker === 'function') {
                    target.showPicker();
                }
            }
            catch {
                // Empty catch to avoid unused variable warnings
            }
        }} min={getMinDate()} max={getMaxDate()} required className="search-input"/>
          </div>

          {/* Field 4: Passenger Counter (Inline UX) */}
          <div className="search-field passenger-field">
            <label>Passengers</label>
            <div className="passenger-counter">
              <button type="button" onClick={() => setPassengers(p => Math.max(1, p - 1))} disabled={passengers <= 1} className="passenger-btn" aria-label="Decrease passenger count">
                <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" strokeWidth="3" strokeLinecap="round">
                  <line x1="5" y1="12" x2="19" y2="12"/>
                </svg>
              </button>
              <span className="passenger-count-display">{passengers}</span>
              <button type="button" onClick={() => setPassengers(p => Math.min(9, p + 1))} disabled={passengers >= 9} className="passenger-btn" aria-label="Increase passenger count">
                <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" strokeWidth="3" strokeLinecap="round">
                  <line x1="5" y1="12" x2="19" y2="12"/>
                  <line x1="12" y1="5" x2="12" y2="19"/>
                </svg>
              </button>
            </div>
          </div>

          {/* Submit Action Button */}
          <div className="search-action-container">
            <Button type="submit" variant="primary" loading={loading} className="search-button-coral">
              Search Tickets
            </Button>
          </div>

        </div>
        <div className="search-options-row">
          <label htmlFor="departure-time-window">Departure time</label>
          <select id="departure-time-window" value={timeWindow} onChange={event => setTimeWindow(event.target.value)}>
            <option value="any">Any time</option>
            <option value="overnight">Overnight · 00:00–06:00</option>
            <option value="morning">Morning · 06:00–12:00</option>
            <option value="afternoon">Afternoon · 12:00–18:00</option>
            <option value="evening">Evening · 18:00–24:00</option>
          </select>
          <span>Times are shown in India Standard Time.</span>
        </div>
      </form>
    </Card>);
};
