package com.solar.management.service.impl;

import com.solar.management.entity.Location;
import com.solar.management.repository.LocationRepository;
import com.solar.management.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Location> getAllLocations() {
        return locationRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Location> getLocationById(Long id) {
        return locationRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Location> getLocationByName(String name) {
        return locationRepository.findByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Location> getLocationsByType(Location.LocationType type) {
        return locationRepository.findByType(type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Location> getActiveLocations() {
        return locationRepository.findByActive(true);
    }

    @Override
    public Location createLocation(Location location) {
        return locationRepository.save(location);
    }

    @Override
    public Location updateLocation(Long id, Location location) {
        return locationRepository.findById(id)
                .map(existing -> {
                    existing.setName(location.getName());
                    existing.setType(location.getType());
                    existing.setAddress(location.getAddress());
                    existing.setCity(location.getCity());
                    existing.setState(location.getState());
                    existing.setPostalCode(location.getPostalCode());
                    existing.setCountry(location.getCountry());
                    existing.setLatitude(location.getLatitude());
                    existing.setLongitude(location.getLongitude());
                    existing.setContactPerson(location.getContactPerson());
                    existing.setContactPhone(location.getContactPhone());
                    existing.setNotes(location.getNotes());
                    existing.setActive(location.getActive());
                    return locationRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Location not found with id: " + id));
    }

    @Override
    public void deleteLocation(Long id) {
        locationRepository.deleteById(id);
    }
}
