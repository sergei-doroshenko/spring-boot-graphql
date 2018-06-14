package org.sdoroshenko.tools.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Car {

    @Id
    @Column(name = "car_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Getter
    @Setter
    private Long id;

    @Column(name = "car_vin", nullable = false)
    @Getter
    @Setter
    private String vin;

    @Column(name = "car_make", nullable = false)
    @Getter
    @Setter
    private String make;

    @Column(name = "car_model", nullable = false)
    @Getter
    @Setter
    private String model;

    @Column(name = "car_year", nullable = false)
    @Getter
    @Setter
    private String year;
}
