package org.sdoroshenko.dataloader.repository;

import org.sdoroshenko.dataloader.model.Car;
import org.springframework.data.repository.CrudRepository;

public interface CarRepository extends CrudRepository<Car, Long> {
}
