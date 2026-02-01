package com.Laliga.Demo_final_project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "doi_bong")
public class DoiBong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //@Column(name = "MaDoi")
    private Integer maDoi;

    //@Column(name = "TenDoi")
    private String tenDoi;

    //@Column(name = "HLV")
    private String hlv;

    //@Column(name = "NamThanhLap")
    private Integer namThanhLap;

    @ManyToOne
    @JoinColumn(name = "Ma_San_Nha")
    private SanThiDau sanNha;
}
