package org.sdoroshenko.spqr.graph;

import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.extern.slf4j.Slf4j;
import org.sdoroshenko.spqr.model.Customer;
import org.sdoroshenko.spqr.model.Message;

@Slf4j
public class CustomerGraph {

    @GraphQLQuery(name = "customer")
    public Customer findCustomer(@GraphQLContext Message message) {
        log.debug("{}. {} {} in: {}", 3, "start", "customer", Thread.currentThread().getName());
        Customer customer = new Customer(222L, "Test User");
        log.debug("{}. {} {} in: {}", 4, "completed", "customer", Thread.currentThread().getName());
        return customer;
    }
}
