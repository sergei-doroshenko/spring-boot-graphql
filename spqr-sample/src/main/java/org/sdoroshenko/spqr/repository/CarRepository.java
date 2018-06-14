package org.sdoroshenko.spqr.repository;

import org.sdoroshenko.spqr.model.Car;
import org.springframework.data.repository.CrudRepository;

public interface CarRepository extends CrudRepository<Car, Long> {
}
