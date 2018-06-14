package org.sdoroshenko.spqr.graph;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.sdoroshenko.spqr.model.Car;
import org.sdoroshenko.spqr.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class CarGraph {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DataLoaderRegistry registry;

    @Autowired
    public CarGraph(NamedParameterJdbcTemplate jdbcTemplate, DataLoaderRegistry registry) {
        this.jdbcTemplate = jdbcTemplate;
        this.registry = registry;
    }

    /*@GraphQLQuery(name = "car")
    public Car car(@GraphQLContext Message message) {
        log.debug("{}. {} {} in: {}", 1, "start", "car", Thread.currentThread().getName());
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Car result = jdbcTemplate.queryForObject(
            "select * from car where id = :carId",
            new MapSqlParameterSource("carId", message.getCarId()),
            (rs, rowNum) -> {
                Car car = new Car();
                car.setId(rs.getLong(1));
                car.setVin(rs.getString(2));
                return car;
            }
        );
        log.debug("{}. {} {} in: {}", 2, "completed", "car", Thread.currentThread().getName());
        return result;
    }*/

    @GraphQLQuery(name = "car")
    public CompletableFuture<Car> car(@GraphQLContext Message message) {
        log.debug("{}. {} {} in: {}", 1, "start", "cars", Thread.currentThread().getName());

        DataLoader<Long, Car> dataLoader = registry.getDataLoader("car");

        CompletableFuture<Car> result = dataLoader.load(message.getCarId());

        log.debug("{}. {} {} in: {}", 2, "completed", "cars", Thread.currentThread().getName());
        return result;
    }

    /*@GraphQLQuery(name = "car")
    @Batched
    public List<Car> cars(@GraphQLContext List<Message> messages) {
        log.debug("{}. {} {} in: {}", 1, "start", "cars", Thread.currentThread().getName());
        List<Long> carIds = messages.stream().map(Message::getCarId).collect(Collectors.toList());
        SqlParameterSource parameters = new MapSqlParameterSource("carIds", carIds);

        List<Car> result = jdbcTemplate.query("select * from car where id in(:carIds)", parameters, (rs, rowNum) -> {
            Car car = new Car();
            car.setId(rs.getLong(1));
            car.setVin(rs.getString(2));
            return car;
        });
        log.debug("{}. {} {} in: {}", 2, "completed", "cars", Thread.currentThread().getName());
        return result;
    }*/

    @GraphQLQuery(name = "images")
    public List<String> images(
        @GraphQLContext Car car,
        @GraphQLArgument(name = "limit", defaultValue = "0") int limit
    ) {
        return car.getImages().subList(
            0, limit > 0 ? limit : car.getImages().size());
    }
}
