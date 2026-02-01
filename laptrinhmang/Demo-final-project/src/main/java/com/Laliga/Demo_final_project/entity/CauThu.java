package com.Laliga.Demo_final_project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cau_thu")
public class CauThu {
    @Id
    //@Column(name = "Macauthu")
    private String maCauThu;

    //@Column(name = "TenCauTHu")
    private String tenCauThu;

    //@Column(name = "ViTri")
    private String viTri;

    //@Column(name = "SoAo")
    private Integer soAo;

    @Column(name = "Quoc_Tich")
    private String quocTich;

    @ManyToOne
    @JoinColumn(name = "Ma_Doi")
    private DoiBong doiBong;
}
