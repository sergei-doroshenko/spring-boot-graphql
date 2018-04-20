package org.sdoroshenko;

import graphql.schema.Coercing;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.AbstractTypeAdapter;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import org.joda.time.DateTime;

import java.lang.reflect.AnnotatedType;

public class JodaTypeAdapter extends AbstractTypeAdapter<DateTime, Long> {

    private static final GraphQLScalarType GraphQLJodaDateTime = new GraphQLScalarType("JodaDateTime", "Custom JodaDateTime Timestamp", new Coercing<DateTime, Long>() {

        @Override
        public Long serialize(Object dataFetcherResult) { // we have DateTime instance here
            return ((DateTime) dataFetcherResult).getMillis();
        }

        @Override
        public DateTime parseValue(Object input) {
            return new DateTime(input);
        }

        @Override
        public DateTime parseLiteral(Object input) {
            return new DateTime(Long.valueOf((String) input));
        }
    });

    @Override
    public DateTime convertInput(Long substitute, AnnotatedType type, GlobalEnvironment environment, ValueMapper valueMapper) {
        return new DateTime(substitute);
    }

    @Override
    public Long convertOutput(DateTime original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return original.getMillis();
    }

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        return GraphQLJodaDateTime;
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return super.supports(type) || GenericTypeReflector.isSuperType(DateTime.class, type.getType());
    }
}
