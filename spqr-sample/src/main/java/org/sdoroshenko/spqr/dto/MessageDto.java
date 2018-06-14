package org.sdoroshenko.spqr.dto;

import io.leangen.graphql.annotations.GraphQLNonNull;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;
import org.sdoroshenko.spqr.model.Customer;

@AllArgsConstructor
@NoArgsConstructor
public class MessageDto {

    @GraphQLNonNull
    public String body;

    @GraphQLNonNull
    public Long conversationId;

    public Long carId;

    public Customer customer;

    @GraphQLNonNull
    public DateTime createdAt;
}
