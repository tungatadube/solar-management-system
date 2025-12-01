import React, { useState } from 'react';
import { Autocomplete, useLoadScript } from '@react-google-maps/api';
import { TextField, Box, CircularProgress } from '@mui/material';

const libraries: ("places")[] = ["places"];

interface PlaceResult {
  address: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  latitude: number;
  longitude: number;
}

interface GoogleMapsAutocompleteProps {
  onPlaceSelected: (place: PlaceResult) => void;
  label?: string;
  error?: boolean;
  helperText?: string;
  required?: boolean;
  defaultValue?: string;
}

const GoogleMapsAutocomplete: React.FC<GoogleMapsAutocompleteProps> = ({
  onPlaceSelected,
  label = "Search Location",
  error = false,
  helperText,
  required = false,
  defaultValue = ''
}) => {
  const [autocomplete, setAutocomplete] = useState<google.maps.places.Autocomplete | null>(null);
  const [inputValue, setInputValue] = useState(defaultValue);

  const { isLoaded, loadError } = useLoadScript({
    googleMapsApiKey: process.env.REACT_APP_GOOGLE_MAPS_API_KEY || '',
    libraries,
  });

  const onLoad = (autocompleteInstance: google.maps.places.Autocomplete) => {
    setAutocomplete(autocompleteInstance);
  };

  const onPlaceChanged = () => {
    if (autocomplete) {
      const place = autocomplete.getPlace();

      if (place.geometry && place.geometry.location) {
        const addressComponents = place.address_components || [];

        // Extract address components
        let street = '';
        let city = '';
        let state = '';
        let postalCode = '';
        let country = '';

        addressComponents.forEach((component) => {
          const types = component.types;

          if (types.includes('street_number')) {
            street = component.long_name + ' ';
          }
          if (types.includes('route')) {
            street += component.long_name;
          }
          if (types.includes('locality')) {
            city = component.long_name;
          }
          if (types.includes('administrative_area_level_1')) {
            state = component.short_name;
          }
          if (types.includes('postal_code')) {
            postalCode = component.long_name;
          }
          if (types.includes('country')) {
            country = component.long_name;
          }
        });

        const result: PlaceResult = {
          address: street || place.formatted_address || '',
          city,
          state,
          postalCode,
          country,
          latitude: place.geometry.location.lat(),
          longitude: place.geometry.location.lng(),
        };

        setInputValue(place.formatted_address || '');
        onPlaceSelected(result);
      }
    }
  };

  if (loadError) {
    return <Box color="error.main">Error loading Google Maps</Box>;
  }

  if (!isLoaded) {
    return (
      <Box display="flex" alignItems="center" gap={1}>
        <CircularProgress size={20} />
        <span>Loading Google Maps...</span>
      </Box>
    );
  }

  return (
    <Autocomplete onLoad={onLoad} onPlaceChanged={onPlaceChanged}>
      <TextField
        fullWidth
        label={label}
        value={inputValue}
        onChange={(e) => setInputValue(e.target.value)}
        error={error}
        helperText={helperText}
        required={required}
        placeholder="Start typing an address..."
      />
    </Autocomplete>
  );
};

export default GoogleMapsAutocomplete;
