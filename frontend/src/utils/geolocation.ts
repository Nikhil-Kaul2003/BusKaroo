export const SUPPORTED_COUNTRIES = ['India'];

export const detectCountryFromBrowser = (): Promise<string> => {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('UNSUPPORTED'));
      return;
    }

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        try {
          const { latitude, longitude } = position.coords;
          const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${latitude}&lon=${longitude}&zoom=3`;
          
          const response = await fetch(url, {
            headers: {
              'Accept-Language': 'en', // Ask for English country names
              'User-Agent': 'BusKarooTravelApp/1.0 (contact: support@buskaroo.example)'
            },
          });

          if (response.ok) {
            const data = await response.json();
            const country = data.address?.country || null;
            if (country) {
              resolve(country);
            } else {
              reject(new Error('GEOLOCATION_FAILED'));
            }
          } else {
            reject(new Error('NOMINATIM_FAILED'));
          }
        } catch (err) {
          console.error('Reverse geocoding failed', err);
          reject(new Error('GEOLOCATION_FAILED'));
        }
      },
      (error) => {
        console.warn('Geolocation query failed or denied by user', error);
        if (error.code === error.TIMEOUT) {
          reject(new Error('TIMEOUT'));
        } else if (error.code === error.PERMISSION_DENIED) {
          reject(new Error('PERMISSION_DENIED'));
        } else {
          reject(new Error('GEOLOCATION_FAILED'));
        }
      },
      { 
        timeout: 10000,
        maximumAge: 60000,       // Allow cache up to 60s
        enableHighAccuracy: false // Fast & robust on desktop/laptop
      }
    );
  });
};
