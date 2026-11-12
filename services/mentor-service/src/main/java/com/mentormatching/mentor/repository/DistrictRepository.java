package com.mentormatching.mentor.repository;

import com.mentormatching.mentor.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DistrictRepository extends JpaRepository<District, Long> {
    List<District> findByCityId(Long cityId);
    Optional<District> findByCityIdAndCode(Long cityId, String code);
}

