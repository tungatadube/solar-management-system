package com.solar.management.repository;

import com.solar.management.entity.Job;
import com.solar.management.entity.JobStock;
import com.solar.management.entity.StockItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobStockRepository extends JpaRepository<JobStock, Long> {
    List<JobStock> findByJob(Job job);
    List<JobStock> findByStockItem(StockItem stockItem);

    @Query("SELECT SUM(js.totalCost) FROM JobStock js WHERE js.job = :job")
    Double calculateTotalCostForJob(@Param("job") Job job);
}
