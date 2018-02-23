package org.sdoroshenko.resolver;

import com.coxautodev.graphql.tools.GraphQLMutationResolver;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.sdoroshenko.repository.CarRepository;
import org.sdoroshenko.model.Car;

@AllArgsConstructor
public class Mutation implements GraphQLMutationResolver {
    @Getter @Setter
    private CarRepository carRepository;

    public Car newCar(String vin, String make, String model, String year) {
        Car car = new Car(null, vin, make, model, year);
        carRepository.save(car);
        return car;
    }

    public Boolean deleteCar(Long id) {
        carRepository.delete(id);
        return Boolean.TRUE;
    }
}
