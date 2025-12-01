package com.solar.management.service;

import com.solar.management.entity.Location;

import java.util.List;
import java.util.Optional;

public interface LocationService {
    List<Location> getAllLocations();
    Optional<Location> getLocationById(Long id);
    Optional<Location> getLocationByName(String name);
    List<Location> getLocationsByType(Location.LocationType type);
    List<Location> getActiveLocations();
    Location createLocation(Location location);
    Location updateLocation(Long id, Location location);
    void deleteLocation(Long id);
}
