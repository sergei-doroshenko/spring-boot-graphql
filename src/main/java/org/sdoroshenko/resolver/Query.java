package org.sdoroshenko.resolver;

import com.coxautodev.graphql.tools.GraphQLQueryResolver;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.sdoroshenko.model.Car;
import org.sdoroshenko.repository.CarRepository;

@AllArgsConstructor
public class Query implements GraphQLQueryResolver {

    @Getter @Setter private CarRepository carRepository;

    public Iterable<Car> findAllCars() {
        return carRepository.findAll();
    }
}
