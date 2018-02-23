package org.sdoroshenko.resolver;

import com.coxautodev.graphql.tools.GraphQLResolver;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import org.sdoroshenko.repository.CarRepository;
import org.sdoroshenko.model.Car;

@AllArgsConstructor
public class CarResolver implements GraphQLResolver<Car> {
    @Getter @Setter
    private CarRepository carRepository;

    public String getVin(Car car) {
        return car.getVin();
    }

}
