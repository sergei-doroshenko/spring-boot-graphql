package org.sdoroshenko.spqr.model;

import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String body;

    @Column(nullable = false, name = "conversation_id")
    private Long conversationId;

    @Column
    private Long carId;

    @Transient
    private Customer customer;

    @Column(nullable = false)
    private DateTime createdAt;

    @Transient
    private String summary;

    @Deprecated
    @GraphQLQuery(deprecationReason = "replaced by Summary object")
    public String getSummary() {
        return summary;
    }
}
