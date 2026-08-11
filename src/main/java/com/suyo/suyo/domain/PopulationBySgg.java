package com.suyo.suyo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "population_by_sgg")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PopulationBySgg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sgg_code", nullable = false, length = 10)
    private String sggCode;

    @Column(name = "sgg_name", nullable = false, length = 50)
    private String sggName;

    @Column(name = "quarter", nullable = false, length = 6)
    private String quarter;

    @Column(name = "flow_total")
    private Long flowTotal;

    @Column(name = "flow_male")
    private Long flowMale;

    @Column(name = "flow_female")
    private Long flowFemale;

    @Column(name = "flow_age10")
    private Long flowAge10;

    @Column(name = "flow_age20")
    private Long flowAge20;

    @Column(name = "flow_age30")
    private Long flowAge30;

    @Column(name = "flow_age40")
    private Long flowAge40;

    @Column(name = "flow_age50")
    private Long flowAge50;

    @Column(name = "flow_age60")
    private Long flowAge60;

    @Column(name = "resident_total")
    private Long residentTotal;

    @Column(name = "household_total")
    private Long householdTotal;

    @Column(name = "household_apt")
    private Long householdApt;

    @Column(name = "household_nonapt")
    private Long householdNonapt;

    @Column(name = "worker_total")
    private Long workerTotal;
}
