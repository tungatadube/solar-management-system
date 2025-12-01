package com.solar.management.repository;

import com.solar.management.entity.Location;
import com.solar.management.entity.StockItem;
import com.solar.management.entity.StockMovement;
import com.solar.management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByStockItem(StockItem stockItem);
    List<StockMovement> findByFromLocation(Location fromLocation);
    List<StockMovement> findByToLocation(Location toLocation);
    List<StockMovement> findByPerformedBy(User user);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.movementDate BETWEEN :start AND :end")
    List<StockMovement> findMovementsBetweenDates(@Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);
}
