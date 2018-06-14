package org.sdoroshenko.dataloader.graph;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.sdoroshenko.dataloader.model.Car;
import org.sdoroshenko.dataloader.model.Message;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class CarGraph {

    private final DataLoaderRegistry registry;

    @Autowired
    public CarGraph(DataLoaderRegistry registry) {
        this.registry = registry;
    }

    @GraphQLQuery(name = "car")
    public CompletableFuture<Car> car(@GraphQLContext Message message) {
        log.debug("{}. {} {} in: {}", 1, "start", "cars", Thread.currentThread().getName());

        DataLoader<Long, Car> dataLoader = registry.getDataLoader("car");
        CompletableFuture<Car> result = dataLoader.load(message.getCarId());

        log.debug("{}. {} {} in: {}", 2, "completed", "cars", Thread.currentThread().getName());
        return result;
    }

    @GraphQLQuery(name = "images")
    public List<String> images(
        @GraphQLContext Car car,
        @GraphQLArgument(name = "limit", defaultValue = "0") int limit
    ) {
        return car.getImages().subList(
            0, limit > 0 ? limit : car.getImages().size());
    }
}
