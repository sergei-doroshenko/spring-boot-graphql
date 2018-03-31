package org.sdoroshenko;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GraphQLController {

    private static final Logger logger = LoggerFactory.getLogger(GraphQLController.class);

    private final GraphQL graphQL;

    @Autowired
    public GraphQLController(GraphQL graphQL) {
        this.graphQL = graphQL;
    }

    @PostMapping(value = "/graphql", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public ExecutionResult execute(@RequestBody GraphQLRequest request) {
        logger.info("Received request: {}", request);
        return graphQL.execute(ExecutionInput.newExecutionInput()
                .query(request.getQuery())
                .operationName(request.getOperationName())
                .build());
    }

}
