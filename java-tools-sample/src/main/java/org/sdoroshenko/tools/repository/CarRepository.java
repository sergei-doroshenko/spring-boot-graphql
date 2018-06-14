package org.sdoroshenko.tools.repository;

import org.sdoroshenko.tools.model.Car;
import org.springframework.data.repository.CrudRepository;

public interface CarRepository extends CrudRepository<Car, Long> {
}
