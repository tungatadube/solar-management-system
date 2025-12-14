package com.solar.management.repository;

import com.solar.management.entity.Job;
import com.solar.management.entity.SolarAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolarAnalysisRepository extends JpaRepository<SolarAnalysis, Long> {

    Optional<SolarAnalysis> findByJob(Job job);

    List<SolarAnalysis> findByJobId(Long jobId);

    @Query("SELECT sa FROM SolarAnalysis sa WHERE sa.latitude BETWEEN :minLat AND :maxLat " +
           "AND sa.longitude BETWEEN :minLon AND :maxLon")
    List<SolarAnalysis> findByLocationRange(
        @Param("minLat") Double minLat,
        @Param("maxLat") Double maxLat,
        @Param("minLon") Double minLon,
        @Param("maxLon") Double maxLon
    );

    @Query("SELECT sa FROM SolarAnalysis sa WHERE sa.address LIKE %:address%")
    List<SolarAnalysis> findByAddressContaining(@Param("address") String address);

    @Query("SELECT AVG(sa.annualProduction) FROM SolarAnalysis sa WHERE sa.systemCapacity = :capacity")
    Double getAverageProductionByCapacity(@Param("capacity") Double capacity);
}
