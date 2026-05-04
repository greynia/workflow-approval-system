package com.eva.workflow.approval.infrastructure.persistence.jpa.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.HolidayEntity;

public interface HolidayRepository extends JpaRepository<HolidayEntity, Long> {

    List<HolidayEntity> findByDateBetween(LocalDate startDate, LocalDate endDate);

    List<HolidayEntity> findByYearOrderByDateAsc(int year);

    Optional<HolidayEntity> findByDate(LocalDate date);

    boolean existsByDate(LocalDate date);

    @Modifying
    @Query(value = """
            INSERT INTO holidays (date, name, year)
            VALUES (:date, :name, :year)
            ON CONFLICT (date) DO NOTHING
            """, nativeQuery = true)
    int insertIfDateAbsent(@Param("date") LocalDate date, @Param("name") String name, @Param("year") int year);
}
