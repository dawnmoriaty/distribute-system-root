package com.Laliga.Demo_final_project.repository;

import com.Laliga.Demo_final_project.entity.CauThu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CauThuRepository extends JpaRepository<CauThu, String> {
}
