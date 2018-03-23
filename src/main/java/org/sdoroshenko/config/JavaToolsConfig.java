package org.sdoroshenko.config;

import org.sdoroshenko.repository.CarRepository;
import org.sdoroshenko.resolver.CarResolver;
import org.sdoroshenko.resolver.Mutation;
import org.sdoroshenko.resolver.Query;
import org.sdoroshenko.resolver.Subscription;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "graphql.type", havingValue = "java-tools")
public class JavaToolsConfig {

    @Bean
    public CarResolver carResolver(CarRepository carRepository) {
        return new CarResolver(carRepository);
    }


    @Bean
    public Query query(CarRepository carRepository) {
        return new Query(carRepository);
    }

    @Bean
    public Mutation mutation(CarRepository carRepository) {
        return new Mutation(carRepository);
    }

    @Bean
    public Subscription subscription() {
        return new Subscription();
    }
}
