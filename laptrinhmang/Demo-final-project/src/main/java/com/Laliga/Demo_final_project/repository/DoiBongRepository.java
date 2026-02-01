package com.Laliga.Demo_final_project.repository;

import com.Laliga.Demo_final_project.entity.DoiBong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoiBongRepository extends JpaRepository<DoiBong, Integer> {
    Optional<DoiBong> findByTenDoi(String tenDoi);
}
