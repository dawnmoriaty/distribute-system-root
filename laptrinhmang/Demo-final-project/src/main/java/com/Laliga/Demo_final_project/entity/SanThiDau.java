package com.Laliga.Demo_final_project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "san_thi_dau")
public class SanThiDau {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //@Column(name = "MaSan")
    private Integer maSan;

    //@Column(name = "TenSon")
    private String tenSon;

    //@Column(name = "DiaChi")
    private String diaChi;

    //@Column(name = "SucChua")
    private Integer sucChua;

}
