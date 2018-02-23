package org.sdoroshenko.repository;

import org.springframework.data.repository.CrudRepository;
import org.sdoroshenko.model.Car;

public interface CarRepository extends CrudRepository<Car, Long> {
}
