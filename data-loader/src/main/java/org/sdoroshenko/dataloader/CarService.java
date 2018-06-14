package org.sdoroshenko.dataloader;

import lombok.extern.slf4j.Slf4j;
import org.sdoroshenko.dataloader.model.Car;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.List;

@Slf4j
public class CarService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public CarService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Car> getCarDataViaBatchSQL(List<Long> ids) {
        log.debug("{}. {} {} in: {}", 1, "start", "getCarDataViaBatchSQL with ids[" + ids + "]", Thread.currentThread().getName());
        SqlParameterSource parameters = new MapSqlParameterSource("carIds", ids);

        List<Car> result = jdbcTemplate.query("select * from car where id in(:carIds)", parameters, (rs, rowNum) -> {
            Car car = new Car();
            car.setId(rs.getLong(1));
            car.setVin(rs.getString(2));
            return car;
        });
        log.debug("{}. {} {} in: {}", 2, "completed", "getCarDataViaBatchSQL with ids[" + ids + "]", Thread.currentThread().getName());
        return result;
    }
}
