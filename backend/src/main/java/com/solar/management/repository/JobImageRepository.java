package com.solar.management.repository;

import com.solar.management.entity.Job;
import com.solar.management.entity.JobImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobImageRepository extends JpaRepository<JobImage, Long> {
    List<JobImage> findByJob(Job job);
    List<JobImage> findByJobAndImageType(Job job, JobImage.ImageType imageType);
}
