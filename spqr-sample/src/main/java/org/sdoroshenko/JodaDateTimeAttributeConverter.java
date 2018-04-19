package org.sdoroshenko;

import org.joda.time.DateTime;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.Timestamp;

@Converter(autoApply = true)
public class JodaDateTimeAttributeConverter implements AttributeConverter<DateTime, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(DateTime dateTime) {
        return (dateTime == null ? null : new Timestamp(dateTime.getMillis()));
    }

    @Override
    public DateTime convertToEntityAttribute(Timestamp timestamp) {
        return new DateTime(timestamp.getTime());
    }
}
