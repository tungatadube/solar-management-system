package com.solar.management.repository;

import com.solar.management.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    List<Location> findByType(Location.LocationType type);
    List<Location> findByActive(Boolean active);
    Optional<Location> findByName(String name);
}
