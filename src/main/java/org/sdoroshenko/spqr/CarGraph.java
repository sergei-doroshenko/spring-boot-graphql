package org.sdoroshenko.spqr;

import com.google.common.collect.Lists;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.AllArgsConstructor;
import org.sdoroshenko.model.Car;
import org.sdoroshenko.repository.CarRepository;

import java.util.List;

@AllArgsConstructor
public class CarGraph {

    private CarRepository carRepository;

    @GraphQLQuery(name = "cars")
    public List<Car> findAllCars() {
        return Lists.newArrayList(carRepository.findAll());
    }

    @GraphQLMutation(name = "addCar")
    public Car addCar(
        @GraphQLArgument(name = "vin") String vin,
        @GraphQLArgument(name = "make") @GraphQLNonNull String make,
        @GraphQLArgument(name = "model") String model,
        @GraphQLArgument(name = "year") String year
    ) {

        return carRepository.save(new Car(null, vin, make, model, year));
    }

}
