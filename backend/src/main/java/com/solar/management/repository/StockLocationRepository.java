package com.solar.management.repository;

import com.solar.management.entity.Location;
import com.solar.management.entity.StockItem;
import com.solar.management.entity.StockLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockLocationRepository extends JpaRepository<StockLocation, Long> {
    List<StockLocation> findByStockItem(StockItem stockItem);
    List<StockLocation> findByLocation(Location location);
    Optional<StockLocation> findByStockItemAndLocation(StockItem stockItem, Location location);

    @Query("SELECT sl FROM StockLocation sl WHERE sl.location.type = :locationType")
    List<StockLocation> findByLocationType(@Param("locationType") Location.LocationType locationType);
}
