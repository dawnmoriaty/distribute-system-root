package com.Laliga.Demo_final_project.repository;

import com.Laliga.Demo_final_project.entity.SanThiDau;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SanThiDauRepository extends JpaRepository<SanThiDau, Integer> {
    Optional<SanThiDau> findByTenSon(String tenSon);
}
