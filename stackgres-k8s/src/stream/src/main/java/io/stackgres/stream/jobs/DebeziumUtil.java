/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.stackgres.common.crd.sgstream.DebeziumDefault;
import io.stackgres.common.crd.sgstream.DebeziumListSeparator;
import io.stackgres.common.crd.sgstream.DebeziumMapOptions;
import io.stackgres.common.crd.sgstream.StackGresStreamSourcePostgres;
import io.stackgres.common.crd.sgstream.StackGresStreamSourceSgCluster;
import org.jooq.lambda.Seq;

public interface DebeziumUtil {

  static void configureDebeziumSectionProperties(
      Properties props,
      Object debeziumSectionProperties,
      Class<?> debeziumSectionPropertiesClass) {
    for (Field field : debeziumSectionPropertiesClass.getDeclaredFields()) {
      if (!List.of(String.class, Boolean.class, Integer.class, List.class, Map.class).contains(field.getType())) {
        continue;
      }
      String property = field.getName().replaceAll("([A-Z])", ".$1").toLowerCase();
      String getterMethodName = "get" + field.getName().substring(0, 1).toUpperCase()
          + field.getName().substring(1);
      Method getterMethod;
      try {
        getterMethod = debeziumSectionPropertiesClass.getMethod(getterMethodName);
        Object value = debeziumSectionProperties != null ? getterMethod.invoke(debeziumSectionProperties) : null;
        value = Optional.ofNullable(value)
            .or(() -> Optional.ofNullable(field.getAnnotation(DebeziumDefault.class))
                .<Object>map(DebeziumDefault::value))
            .orElse(null);
        if (value != null) {
          if (value instanceof Map map) {
            setMapProperties(props, field, property, map);
          } else if (value instanceof List list) {
            props.setProperty(property, joinList(field, list));
          } else {
            props.setProperty(property, value.toString());
          }
        }
      } catch (RuntimeException ex) {
        throw ex;
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  @SuppressWarnings("unchecked")
  static void setMapProperties(Properties props, Field field, String property, Map<?, ?> map) {
    Map<String, ?> mapWithKeys = (Map<String, ?>) map;
    var mapOptions = Optional.ofNullable(field.getAnnotation(DebeziumMapOptions.class));
    if (getMapGenerateSummary(mapOptions)) {
      props.put(property, joinList(field, mapWithKeys.keySet()));
    }
    if (getMapValueFromLevel(mapOptions) <= 0) {
      props.setProperty(property,
          joinList(
              field,
              mapWithKeys.entrySet().stream()
              .map(entry -> entry.getKey()
                  + getMapSeparatorAtLevel0(mapOptions)
                  + entry.getValue().toString())
              .toList()));
      return;
    }
    for (var entry : mapWithKeys.entrySet()) {
      String entryProperty = (isMapPrefixFromLevel0(mapOptions)
          ? property + getMapSeparatorAtLevel0(mapOptions) : "") + entry.getKey();
      if (entry.getValue() instanceof Map entryValueMap) {
        Map<String, ?> entryValueMapWithKeys = (Map<String, ?>) entryValueMap;
        for (var entryValueEntry : entryValueMapWithKeys.entrySet()) {
          String entryValueEntryProperty =
              entryProperty + getMapSeparatorAtLevel1(mapOptions) + entryValueEntry.getKey();
          if (entryValueEntry.getValue() instanceof List entryValueEntryValueList) {
            props.setProperty(entryValueEntryProperty, joinList(field, entryValueEntryValueList));
          } else {
            props.setProperty(entryValueEntryProperty, entryValueEntry.getValue().toString());
          }
        }
      } else {
        props.setProperty(entryProperty, entry.getValue().toString());
      }
    }
  }

  static boolean getMapGenerateSummary(Optional<DebeziumMapOptions> mapOptions) {
    return mapOptions.map(DebeziumMapOptions::generateSummary)
        .orElse(DebeziumMapOptions.DEFAULT_GENERATE_SUMMARY);
  }

  static boolean isMapPrefixFromLevel0(Optional<DebeziumMapOptions> mapOptions) {
    return mapOptions.map(DebeziumMapOptions::prefixFromLevel)
        .orElse(DebeziumMapOptions.DEFAULT_PREFIX_FROM_LEVEL) == 0;
  }

  static int getMapValueFromLevel(Optional<DebeziumMapOptions> mapOptions) {
    return mapOptions.map(DebeziumMapOptions::valueFromLevel)
        .orElse(DebeziumMapOptions.DEFAULT_VALUE_FROM_LEVEL);
  }

  static String getMapSeparatorAtLevel0(Optional<DebeziumMapOptions> mapOptions) {
    return mapOptions.map(DebeziumMapOptions::separatorLevel0)
        .orElse(DebeziumMapOptions.DEFAULT_MAP_SEPARATOR);
  }

  static String getMapSeparatorAtLevel1(Optional<DebeziumMapOptions> mapOptions) {
    return mapOptions.map(DebeziumMapOptions::separatorLevel1)
        .orElse(DebeziumMapOptions.DEFAULT_MAP_SEPARATOR);
  }

  static String joinList(Field field, Iterable<?> list) {
    return Seq.seq(list)
        .map(Object::toString)
        .collect(Collectors.joining(
            Optional.ofNullable(field.getAnnotation(DebeziumListSeparator.class))
            .map(DebeziumListSeparator::value)
            .orElse(",")));
  }

  static void configureDebeziumIncludesAndExcludes(
      Properties props,
      StackGresStreamSourceSgCluster sgCluster) {
    configureDebeziumFilter(props, sgCluster, StackGresStreamSourceSgCluster::getIncludes, ".include.list");
    configureDebeziumFilter(props, sgCluster, StackGresStreamSourceSgCluster::getExcludes, ".exclude.list");
  }

  static void configureDebeziumIncludesAndExcludes(
      Properties props,
      StackGresStreamSourcePostgres postgres) {
    configureDebeziumFilter(props, postgres, StackGresStreamSourcePostgres::getIncludes, ".include.list");
    configureDebeziumFilter(props, postgres, StackGresStreamSourcePostgres::getExcludes, ".exclude.list");
  }

  static <T> void configureDebeziumFilter(
      Properties props,
      T sgCluster,
      Function<T, List<String>> getter,
      String suffix) {
    Optional.of(sgCluster)
        .map(getter)
        .ifPresent(patterns -> patterns.stream()
            .collect(Collectors.groupingBy(include -> Math.min(3, include.split("\\\\\\.").length)))
            .entrySet()
            .forEach(entry -> {
              if (entry.getKey().intValue() < 2) {
                props.put("schema" + suffix, entry.getValue().stream().collect(Collectors.joining(",")));
              } else if (entry.getKey().intValue() < 3) {
                props.put("table" + suffix, entry.getValue().stream().collect(Collectors.joining(",")));
              } else {
                props.put("column" + suffix, entry.getValue().stream().collect(Collectors.joining(",")));
              }
            }));
  }

}
