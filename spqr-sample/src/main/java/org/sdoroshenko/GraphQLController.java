package org.sdoroshenko;

import graphql.ErrorType;
import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class GraphQLController {

    private final GraphQL graphQL;

    @Autowired
    public GraphQLController(GraphQL graphQL) {
        this.graphQL = graphQL;
    }

    @PostMapping(value = "/graphql", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Map<String, Object> execute(@RequestBody GraphQLRequest request) {
        log.info("Received request: {}", request);
        ExecutionResult executionResult = graphQL.execute(
            ExecutionInput.newExecutionInput()
                .query(request.getQuery())
                .operationName(request.getOperationName())
                .variables(request.getVariables())
                .build()
        );
//        Map<String, Object> toSpecificationResult = executionResult.toSpecification();
        return createResultFromDataAndErrors(executionResult.getData(), executionResult.getErrors());
    }

    private Map<String, Object> createResultFromDataAndErrors(Object data, List<GraphQLError> errors) {

        final Map<String, Object> result = new HashMap<>();
        result.put("data", data);

        if (errors != null) {
            result.put("errors", processErrors(errors));
        }

        return result;
    }

    private List<GraphQLError> processErrors(List<GraphQLError> errors) {
        List<GraphQLError> clientErrors = errors.stream()
            .filter(this::isClientError)
            .map(GraphQLErrorWrapper::new)
            .collect(Collectors.toList());

        List<GraphQLError> serverErrors = errors.stream()
            .filter(e -> !isClientError(e))
            .map(GraphQLErrorWrapper::new)
            .collect(Collectors.toList());

        List<GraphQLError> e = new ArrayList<>();
        e.addAll(clientErrors);
        e.addAll(serverErrors);
        return e;
    }

    private boolean isClientError(GraphQLError error) {
        return !(error instanceof ExceptionWhileDataFetching || error instanceof Throwable);
    }

    static class GraphQLErrorWrapper implements GraphQLError {

        private GraphQLError error;

        public GraphQLErrorWrapper(GraphQLError error) {
            this.error = error;
        }

        @Override
        public String getMessage() {
            return "Server error: " + ((error instanceof ExceptionWhileDataFetching) ? ((ExceptionWhileDataFetching) error).getException().getMessage() : error.getMessage());
        }

        @Override
        public List<SourceLocation> getLocations() {
            return error.getLocations();
        }

        @Override
        public ErrorType getErrorType() {
            return error.getErrorType();
        }

        @Override
        public List<Object> getPath() {
            return error.getPath();
        }

        @Override
        public Map<String, Object> toSpecification() {
            return error.toSpecification();
        }

        @Override
        public Map<String, Object> getExtensions() {
            return error.getExtensions();
        }
    }
}
