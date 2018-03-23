package org.sdoroshenko.spqr;

import com.google.common.collect.Lists;
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

}
