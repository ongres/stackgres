/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.stackgres.stream.jobs.target.migration.dialect.postgres;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

import io.debezium.connector.jdbc.dialect.DatabaseDialect;
import io.debezium.connector.jdbc.type.AbstractType;
import io.debezium.connector.jdbc.type.JdbcType;
import io.debezium.connector.jdbc.util.DateTimeUtils;
import io.debezium.data.VariableScaleDecimal;
import io.debezium.sink.SinkConnectorConfig;
import io.debezium.sink.valuebinding.ValueBindDescriptor;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link JdbcType} for {@code ARRAY} column types.
 *
 * @author Bertrand Paquet
 */

public class ArrayType extends AbstractType {

  private static final Logger LOGGER = LoggerFactory.getLogger(ArrayType.class);

  public static final ArrayType INSTANCE = new ArrayType();

  private static final DateTimeFormatter TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .append(DateTimeFormatter.ISO_LOCAL_DATE)
      .appendLiteral(' ')
      .appendValue(HOUR_OF_DAY, 2)
      .appendLiteral(':')
      .appendValue(MINUTE_OF_HOUR, 2)
      .optionalStart()
      .appendLiteral(':')
      .appendValue(SECOND_OF_MINUTE, 2)
      .optionalStart()
      .appendFraction(NANO_OF_SECOND, 6, 6, true)
      .optionalStart()
      .appendOffsetId()
      .toFormatter();

  private static final DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .appendValue(HOUR_OF_DAY, 2)
      .appendLiteral(':')
      .appendValue(MINUTE_OF_HOUR, 2)
      .optionalStart()
      .appendLiteral(':')
      .appendValue(SECOND_OF_MINUTE, 2)
      .optionalStart()
      .appendFraction(NANO_OF_SECOND, 6, 6, true)
      .optionalStart()
      .appendOffsetId()
      .toFormatter();

  private TimeZone databaseTimeZone;

  @Override
  public void configure(SinkConnectorConfig config, DatabaseDialect dialect) {
      super.configure(config, dialect);

      final String databaseTimeZone = config.useTimeZone();
      try {
        this.databaseTimeZone = TimeZone.getTimeZone(ZoneId.of(databaseTimeZone));
      }
      catch (Exception e) {
        LOGGER.error("Failed to resolve time zone '{}', please specify a correct time zone value", databaseTimeZone, e);
        throw e;
      }
  }

  @Override
  public String[] getRegistrationKeys() {
    return new String[] { "ARRAY" };
  }

  @Override
  public String getTypeName(Schema schema, boolean isKey) {
    Optional<String> sourceColumnType = getSourceColumnType(schema)
        .map(this::removeUnderscore);

    String typeName = getElementTypeName(getDialect(), schema, false);
    if (typeName.indexOf('(') > 0 && typeName.indexOf(')') > 0) {
      typeName = typeName.substring(0, typeName.indexOf('('))
          + typeName.substring(typeName.indexOf(')') + 1);
    }
    if ((typeName.equals("bytea") || typeName.equals("boolean"))
        && sourceColumnType
        .map(type -> true)
        .orElse(false)) {
      typeName = sourceColumnType.get().toLowerCase(Locale.US);
    }
    if (!typeName.endsWith("[]")) {
      typeName = typeName + "[]";
    }
    return typeName;
  }

  private String removeUnderscore(String typeName) {
    if (typeName.indexOf('_') == 0) {
      return typeName.substring(1);
    }
    return typeName;
  }

  private String getElementTypeName(DatabaseDialect dialect, Schema schema, boolean isKey) {
    JdbcType elementType = dialect.getSchemaType(schema.valueSchema());
    return elementType.getTypeName(schema.valueSchema(), isKey);
  }

  @Override
  public List<ValueBindDescriptor> bind(int index, Schema schema, Object value) {
    if (value == null) {
      return List.of(new ValueBindDescriptor(index, null));
    }
    final String typeName = getTypeName(schema, false)
        .transform(type -> type.substring(0, type.length() - 2));
    if (value instanceof List valueList
        && valueList.size() > 0) {
      if (valueList.get(0) instanceof Number) {
        return bindListOfNumbers(index, typeName, valueList);
      }
      if (valueList.get(0) instanceof ByteBuffer) {
        return bindListOfByteBuffers(index, typeName, valueList);
      }
      if (valueList.get(0) instanceof Struct) {
        return bindListOfStructs(index, typeName, valueList);
      }
    }
    return List.of(new ValueBindDescriptor(index, value, java.sql.Types.ARRAY, typeName));
  }

  @SuppressWarnings("unchecked")
  private List<ValueBindDescriptor> bindListOfNumbers(
      int index,
      String typeName,
      List<?> valueList) {
    return List.of(new ValueBindDescriptor(
        index,
        ((List<Number>) valueList)
        .stream()
        .map(number -> number != null ? parseNumber(typeName, number) : null)
        .toList(),
        java.sql.Types.ARRAY,
        typeName));
  }

  private Object parseNumber(String typeName, Number number) {
     if (typeName.equals("date")) {
      return DateTimeUtils.toLocalDateOfEpochDays(number.longValue());
    }
    if (typeName.equals("timestamp")
        || typeName.equals("timestamptz")) {
      final LocalDateTime localDateTime = DateTimeUtils.toLocalDateTimeFromInstantEpochMicros(number.longValue());
      if (getDialect().isTimeZoneSet()) {
        return localDateTime.atZone(databaseTimeZone.toZoneId()).toLocalDateTime()
            .format(TIMESTAMP_FORMATTER);
      }
      return localDateTime
          .format(TIMESTAMP_FORMATTER);
    }
    if (typeName.equals("time")
        || typeName.equals("timetz")) {
      final LocalTime localTime = DateTimeUtils.toLocalTimeFromDurationMicroseconds(number.longValue());
      final LocalDateTime localDateTime = localTime.atDate(LocalDate.now());
      if (getDialect().isTimeZoneSet()) {
          return localDateTime.atZone(databaseTimeZone.toZoneId()).toLocalDateTime()
              .format(TIME_FORMATTER);
      }
      return localDateTime
          .format(TIME_FORMATTER);
    }
    return number.toString();
  }

  @SuppressWarnings("unchecked")
  private List<ValueBindDescriptor> bindListOfStructs(
      int index,
      String typeName,
      List<?> valueList) {
    return List.of(new ValueBindDescriptor(
        index,
        ((List<Struct>) valueList)
        .stream()
        .map(struct -> struct != null ? VariableScaleDecimal.toLogical(struct).getDecimalValue().orElseThrow() : null)
        .toList(),
        java.sql.Types.ARRAY,
        typeName));
  }

  @SuppressWarnings("unchecked")
  private List<ValueBindDescriptor> bindListOfByteBuffers(
      int index,
      String typeName,
      List<?> valueList) {
    return List.of(new ValueBindDescriptor(
        index,
        ((List<ByteBuffer>) valueList)
        .stream()
        .map(byteBuffer -> byteBuffer != null ? new String(byteBuffer.array(), StandardCharsets.UTF_8) : null)
        .toList(),
        java.sql.Types.ARRAY,
        typeName));
  }
}
