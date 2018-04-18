package org.sdoroshenko;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.ExecutorServiceExecutionStrategy;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.annotations.GraphQLQuery;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ParallelTest {

    Vehicle car = new Vehicle(103L, "Honda", "Civic", "2017");

    GraphQLSchema schema = new GraphQLSchemaGenerator()
        .withOperationsFromSingleton(car)
        .generate();

    BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>() {
        @Override
        public boolean offer(Runnable e) {
            /* queue that always rejects tasks */
            return false;
        }
    };

    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
        4, /* core pool size 4 thread */
        4, /* max pool size 4 thread */
        30, TimeUnit.SECONDS,
        /*
         * Do not use the queue to prevent threads waiting on enqueued tasks.
         */
        queue,
        /*
         *  If all the threads are working, then the caller thread
         *  should execute the code in its own thread. (serially)
         */
        new ThreadPoolExecutor.CallerRunsPolicy());

    GraphQL exe = GraphQL.newGraphQL(schema)
        .queryExecutionStrategy(new ExecutorServiceExecutionStrategy(threadPoolExecutor))
        .build();

    String operation = "{id make model year}";

    @Test
    public void parallelQueryExecution() {
        ExecutionResult result = exe.execute(operation);
        Assert.assertEquals("{id=103, make=Honda, model=Civic, year=2017}", result.getData().toString());
        Assert.assertEquals(4, car.getThreads().size());
    }

    public static class Vehicle {
        final Set<Long> threads;
        final Long id;
        final String make;
        final String model;
        final String year;

        public Vehicle(Long id, String make, String model, String year) {
            this.threads = Collections.synchronizedSet(new HashSet());
            this.id = id;
            this.make = make;
            this.model = model;
            this.year = year;
        }

        @GraphQLQuery(name = "id")
        public Long getId() {
            threads.add(Thread.currentThread().getId());
            return id;
        }

        @GraphQLQuery(name = "make")
        public String getMake() {
            threads.add(Thread.currentThread().getId());
            return make;
        }

        @GraphQLQuery(name = "model")
        public String getModel() {
            threads.add(Thread.currentThread().getId());
            return model;
        }

        @GraphQLQuery(name = "year")
        public String getYear() {
            threads.add(Thread.currentThread().getId());
            return year;
        }

        public Set<Long> getThreads() {
            return threads;
        }
    }
}
