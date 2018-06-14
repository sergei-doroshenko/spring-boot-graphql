package org.sdoroshenko.spqr.dto;

import io.leangen.graphql.annotations.types.GraphQLType;
import lombok.Data;

@Data
@GraphQLType(name = "filters", description = "Custom filters")
public class Filters {
    private String authorFilter;
    private String bodyFilter;
    private Order order;

    enum Order {
        ASC("asc"), DESC("desc");

        String orderName;

        Order(String orderName) {
            this.orderName = orderName;
        }

        public String getOrderName() {
            return orderName;
        }
    }
}
