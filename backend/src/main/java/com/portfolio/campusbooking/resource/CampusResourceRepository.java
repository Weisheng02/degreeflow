package com.portfolio.campusbooking.resource;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CampusResourceRepository extends JpaRepository<CampusResource, Long> {

    List<CampusResource> findByActiveTrueOrderByNameAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select resource from CampusResource resource where resource.id = :id")
    Optional<CampusResource> findByIdForUpdate(@Param("id") Long id);
}
