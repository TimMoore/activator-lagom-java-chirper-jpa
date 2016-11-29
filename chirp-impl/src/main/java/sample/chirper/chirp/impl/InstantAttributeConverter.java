package sample.chirper.chirp.impl;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.Timestamp;
import java.time.Instant;

@Converter(autoApply = true)
public class InstantAttributeConverter implements AttributeConverter<Instant, Timestamp> {
    @Override
    public Timestamp convertToDatabaseColumn(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }

    @Override
    public Instant convertToEntityAttribute(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }
}
