package org.sdoroshenko.repository;

import org.sdoroshenko.model.Car;
import org.springframework.data.repository.CrudRepository;

public interface CarRepository extends CrudRepository<Car, Long> {
}
