/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.apiweb.dto.Metadata;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.ModelTestUtil;
import io.stackgres.testutil.StringUtils;

public class TransformerTestUtil {

  public static final Random RANDOM = new Random(7);

  public static <T, S> void assertTransformation(
      Transformer<T, S> transformer,
      TransformerTuple<T, S> transformerTuple) {

    var target = transformerTuple.target();
    var source = transformerTuple.source();

    var actualTarget = transformer.toTarget(source);

    JsonUtil.assertJsonEquals(JsonUtil.toJson(target), JsonUtil.toJson(actualTarget),
        "Transformation from CRD to DTO doesn't "
            + "return the expected output. Which means that the transformer is not accurately "
            + "transforming CRDs to DTOs.");

    var actualSource = transformer.toSource(target);

    assertNotSame(source, actualSource,
        "The original CRD is being returned which can lead to side effects errors");

    JsonUtil.assertJsonEquals(JsonUtil.toJson(source), JsonUtil.toJson(actualSource),
        "Transformation from DTO to CRD doesn't return the "
            + "expected output. Which means that the "
            + "transformer is not accurately transforming DTOs to CRDs.");
  }

  public static <T extends ResourceDto, S extends CustomResource<?, ?>> void assertTransformation(
      AbstractResourceTransformer<T, S> transformer,
      TransformerTuple<T, S> transformerTuple) {

    var target = transformerTuple.target();
    var source = transformerTuple.source();

    var actualTarget = transformer.toDto(source);

    JsonUtil.assertJsonEquals(JsonUtil.toJson(target), JsonUtil.toJson(actualTarget),
        "Transformation from CRD to DTO doesn't "
            + "return the expected output. Which means that the transformer is not accurately "
            + "transforming CRDs to DTOs.");

    var actualSource = transformer.toCustomResource(target, source);

    assertNotSame(source, actualSource,
        "The original CRD is being returned which can lead to side effects errors");

    JsonUtil.assertJsonEquals(JsonUtil.toJson(source), JsonUtil.toJson(actualSource),
        "Transformation from DTO to CRD doesn't return the "
            + "expected output. Which means that the "
            + "transformer is not accurately transforming DTOs to CRDs.");
  }

  public static <T extends ResourceDto, S extends CustomResource<?, ?>> void assertTransformation(
      DependencyResourceTransformer<T, S> transformer,
      TransformerTuple<T, S> transformerTuple,
      List<String> clusters) {

    var target = transformerTuple.target();
    var source = transformerTuple.source();

    var actualTarget = transformer.toResource(source, clusters);

    JsonUtil.assertJsonEquals(JsonUtil.toJson(target), JsonUtil.toJson(actualTarget),
        "Transformation from CRD to DTO doesn't "
            + "return the expected output. Which means that the transformer is not accurately "
            + "transforming CRDs to DTOs.");

    var actualSource = transformer.toCustomResource(target, source);

    assertNotSame(source, actualSource,
        "The original CRD is being returned which can lead to side effects errors");

    JsonUtil.assertJsonEquals(JsonUtil.toJson(target), JsonUtil.toJson(actualTarget),
        "Transformation from DTO to CRD doesn't return the "
            + "expected output. Which means that the "
            + "transformer is not accurately transforming DTOs to CRDs.");
  }

  public static TransformerTuple<Metadata, ObjectMeta> createMetadataTuple() {
    final String clusterName = StringUtils.getRandomResourceName();
    final String namespace = StringUtils.getRandomNamespace();
    var crdMetadata = new ObjectMetaBuilder()
        .withName(clusterName)
        .withNamespace(namespace)
        .build();
    var dtoMetadata = new Metadata();
    dtoMetadata.setName(clusterName);
    dtoMetadata.setNamespace(namespace);
    return new TransformerTuple<>(dtoMetadata, crdMetadata);
  }

  public static TransformerTuple<List<String>, List<String>> generateRandomListTuple() {
    int desiredListSize = RANDOM.nextInt(10) + 1; //More than this could be counter-productive

    List<String> targetList = new ArrayList<>(desiredListSize);
    List<String> sourceList = new ArrayList<>(desiredListSize);

    for (int i = 0; i < desiredListSize; i++) {
      String randomValue = StringUtils.getRandomString(10);
      targetList.add(randomValue);
      sourceList.add(randomValue);
    }

    return new TransformerTuple<>(targetList, sourceList);
  }

  public static <T, S> TransformerTuple<List<T>, List<S>> generateRandomListTuple(
      Class<T> targetParameterizedType, Class<S> sourceParameterizedType) {

    int desiredListSize = RANDOM.nextInt(10) + 1; //More than this could be counter-productive

    List<T> targetList = new ArrayList<>(desiredListSize);
    List<S> sourceList = new ArrayList<>(desiredListSize);

    for (int i = 0; i < desiredListSize; i++) {
      TransformerTuple<T, S> item = fillTupleWithRandomData(
          targetParameterizedType, sourceParameterizedType);
      targetList.add(item.target());
      sourceList.add(item.source());
    }

    return new TransformerTuple<>(targetList, sourceList);
  }

  public static <K1, V1, K2, V2> TransformerTuple<Map<K1, V1>, Map<K2, V2>> generateRandomMapTuple(
      Class<K1> targetKeyType,
      Class<V1> targetValueType,
      Class<K2> sourceKeyType,
      Class<V2> sourceValueType,
      Type targetGenericValueType,
      Type sourceGenericValueType) {

    int desiredMapSize = RANDOM.nextInt(10) + 1; //More than this could be counter-productive

    Map<K1, V1> targetMap = new HashMap<>(desiredMapSize);
    Map<K2, V2> sourceMap = new HashMap<>(desiredMapSize);

    for (int i = 0; i < desiredMapSize; i++) {
      TransformerTuple<K1, K2> key = fillTupleWithRandomData(
          targetKeyType, sourceKeyType);
      TransformerTuple<V1, V2> value = fillTupleWithRandomData(
          targetValueType, sourceValueType);
      targetMap.put(key.target(), value.target());
      sourceMap.put(key.source(), value.source());
    }

    return new TransformerTuple<>(targetMap, sourceMap);
  }

  public static <T, S> TransformerTuple<T, S> fillTupleWithRandomData(
      Class<T> targetClazz, Class<S> sourceClazz) {
    return visit(new RandomDataVisitor<>(), targetClazz, sourceClazz);
  }

  public static class RandomDataVisitor<T, S> implements ResourceVisitor<T, S> {
    @Override
    @SuppressWarnings("unchecked")
    public     TransformerTuple<T, S> onObject(
        Class<?> targetClazz,
        Class<?> sourceClazz,
        List<TransformerTuple<Field, Field>> commonFields) {
      T targetInstance;
      try {
        targetInstance = (T) targetClazz.getDeclaredConstructor().newInstance();
      } catch (ReflectiveOperationException ex) {
        throw new RuntimeException(ex);
      }
      T sourceInstance;
      try {
        sourceInstance = (T) sourceClazz.getDeclaredConstructor().newInstance();
      } catch (ReflectiveOperationException ex) {
        throw new RuntimeException(ex);
      }
      for (TransformerTuple<Field, Field> field : commonFields) {
        setFieldWithRandomData(field, targetInstance, sourceInstance);
      }
      return new TransformerTuple<T, S>((T) targetInstance, (S) sourceInstance);
    }

    private void setFieldWithRandomData(
        TransformerTuple<Field, Field> field, Object target, Object source) {
      try {
        TransformerTuple<Object, Object> value = visit(new RandomDataVisitor<>(),
            field.target().getType(), field.target().getGenericType(),
            field.source().getType(), field.source().getGenericType());
        field.target().setAccessible(true);
        field.target().set(target, value.target());
        field.target().setAccessible(false);
        field.source().setAccessible(true);
        field.source().set(source, value.source());
        field.source().setAccessible(false);
      } catch (ReflectiveOperationException ex) {
        throw new RuntimeException(ex);
      }
    }

    @Override
    @SuppressWarnings("unchecked")
    public TransformerTuple<T, S> onList(
        Class<?> targetClazz, Class<?> targetElementClazz,
        Class<?> sourceClazz, Class<?> sourceElementClazz) {
      int desiredListSize = RANDOM.nextInt(3) + 1; //More than this could be counter-productive

      List<Object> targetList = new ArrayList<>(desiredListSize);
      if (targetClazz != List.class) {
        try {
          var constructor = Arrays.stream(targetClazz.getConstructors())
              .filter(c -> c.getParameterTypes().length == 1
                  && c.getParameterTypes()[0] == List.class)
              .findAny()
              .orElseThrow();
          targetList = (List<Object>) constructor.newInstance(targetList);
        } catch (SecurityException | InvocationTargetException | IllegalAccessException
            | InstantiationException ex) {
          throw new RuntimeException(ex);
        }
      }

      List<Object> sourceList = new ArrayList<>(desiredListSize);
      if (sourceClazz != List.class) {
        try {
          var constructor = Arrays.stream(sourceClazz.getConstructors())
              .filter(c -> c.getParameterTypes().length == 1
                  && c.getParameterTypes()[0] == List.class)
              .findAny()
              .orElseThrow();
          sourceList = (List<Object>) constructor.newInstance(sourceList);
        } catch (SecurityException | InvocationTargetException | IllegalAccessException
            | InstantiationException ex) {
          throw new RuntimeException(ex);
        }
      }

      for (int i = 0; i < desiredListSize; i++) {
        TransformerTuple<Object, Object> item = visit(new RandomDataVisitor<>(),
            targetElementClazz, sourceElementClazz);
        targetList.add(item.target());
        sourceList.add(item.source());
      }

      return new TransformerTuple<T, S>((T) targetList, (S) sourceList);
    }

    @Override
    @SuppressWarnings("unchecked")
    public TransformerTuple<T, S> onMap(
        Class<?> targetClazz, Class<?> targetKeyClazz, Class<?> targetValueClazz, Type targetValueType,
        Class<?> sourceClazz, Class<?> sourceKeyClazz, Class<?> sourceValueClazz, Type sourceValueType) {
      int desiredMapSize = RANDOM.nextInt(3) + 1; //More than this could be counter-productive

      Map<Object, Object> targetMap = new HashMap<>(desiredMapSize);
      if (targetClazz != Map.class) {
        try {
          var constructor = Arrays.stream(targetClazz.getConstructors())
              .filter(c -> c.getParameterTypes().length == 1
                  && c.getParameterTypes()[0] == Map.class)
              .findAny()
              .orElseThrow();
          targetMap = (Map<Object, Object>) constructor.newInstance(targetMap);
        } catch (SecurityException | InvocationTargetException | IllegalAccessException
            | InstantiationException ex) {
          throw new RuntimeException(ex);
        }
      }

      Map<Object, Object> sourceMap = new HashMap<>(desiredMapSize);
      if (sourceClazz != Map.class) {
        try {
          var constructor = Arrays.stream(sourceClazz.getConstructors())
              .filter(c -> c.getParameterTypes().length == 1
                  && c.getParameterTypes()[0] == Map.class)
              .findAny()
              .orElseThrow();
          sourceMap = (Map<Object, Object>) constructor.newInstance(sourceMap);
        } catch (SecurityException | InvocationTargetException | IllegalAccessException
            | InstantiationException ex) {
          throw new RuntimeException(ex);
        }
      }

      for (int i = 0; i < desiredMapSize; i++) {
        TransformerTuple<Object, Object> key = visit(new RandomDataVisitor<>(),
            targetKeyClazz, sourceKeyClazz);
        TransformerTuple<Object, Object> value = visit(new RandomDataVisitor<>(),
            targetValueClazz, targetValueType, sourceValueClazz, sourceValueType);
        targetMap.put(key.target(), value.target());
        sourceMap.put(key.source(), value.source());
      }

      return new TransformerTuple<T, S>((T) targetMap, (S) sourceMap);
    }

    @Override
    @SuppressWarnings("unchecked")
    public TransformerTuple<T, S> onValue(
        Class<?> targetClazz,
        Class<?> sourceClazz) {
      Object value = ModelTestUtil.generateRandomValue(targetClazz);
      return new TransformerTuple<>((T) value, (S) value);
    }
  }

  public interface ResourceVisitor<T, S> {
    TransformerTuple<T, S> onObject(
        Class<?> targetClazz,
        Class<?> sourceClazz,
        List<TransformerTuple<Field, Field>> commonFields);

    TransformerTuple<T, S> onList(
        Class<?> targetClazz, Class<?> targetElementClazz,
        Class<?> sourceClazz, Class<?> sourceElementClazz);

    TransformerTuple<T, S> onMap(
        Class<?> targetClazz, Class<?> targetKeyClazz, Class<?> targetValueClazz, Type targetGenericType,
        Class<?> sourceClazz, Class<?> sourceKeyClazz, Class<?> sourceValueClazz, Type sourceGenericType);

    TransformerTuple<T, S> onValue(
        Class<?> targetClazz,
        Class<?> sourceClazz);
  }

  public static <T, S> TransformerTuple<T, S> visit(ResourceVisitor<T, S> visitor,
      Class<?> targetClazz, Class<?> sourceClazz) {
    return visit(visitor, targetClazz, null, sourceClazz, null);
  }

  @SuppressFBWarnings(value = "SA_LOCAL_SELF_COMPARISON",
      justification = "False positive")
  public static <T, S> TransformerTuple<T, S> visit(
      ResourceVisitor<T, S> visitor,
      Class<?> targetClazz, Type targetGenericType,
      Class<?> sourceClazz, Type sourceGenericType) {
    if (ModelTestUtil.isValueType(targetClazz)) {
      if (!ModelTestUtil.isValueType(sourceClazz)) {
        throw new RuntimeException(targetClazz.getSimpleName() + " and "
            + sourceClazz.getSimpleName() + " are incompatible");
      }
      return visitor.onValue(targetClazz, sourceClazz);
    } else if (List.class.isAssignableFrom(targetClazz)) {
      if (!List.class.isAssignableFrom(sourceClazz)) {
        throw new RuntimeException(targetClazz.getSimpleName() + " and "
            + sourceClazz.getSimpleName() + " are incompatible");
      }
      var targetElementClazz = ModelTestUtil.getParameterFromGenericType(targetClazz, targetGenericType, 0);
      var sourceElementClazz = ModelTestUtil.getParameterFromGenericType(sourceClazz, sourceGenericType, 0);
      return visitor.onList(targetClazz, targetElementClazz, sourceClazz, sourceElementClazz);
    } else if (Map.class.isAssignableFrom(targetClazz)) {
      if (!Map.class.isAssignableFrom(sourceClazz)) {
        throw new RuntimeException(targetClazz.getSimpleName() + " and "
            + sourceClazz.getSimpleName() + " are incompatible");
      }
      var targetKeyClazz = ModelTestUtil.getParameterFromGenericType(targetClazz, targetGenericType, 0);
      var targetValueClazz = ModelTestUtil.getParameterFromGenericType(targetClazz, targetGenericType, 1);
      var sourceKeyClazz = ModelTestUtil.getParameterFromGenericType(sourceClazz, sourceGenericType, 0);
      var sourceValueClazz = ModelTestUtil.getParameterFromGenericType(sourceClazz, sourceGenericType, 1);
      Type targetGenericOrParameterizedType = targetGenericType;
      if (targetGenericType instanceof ParameterizedType targetParameterizedValueType) {
        targetGenericOrParameterizedType = targetParameterizedValueType
            .getActualTypeArguments()[1];
      }
      Type sourceGenericOrParameterizedType = sourceGenericType;
      if (sourceGenericType instanceof ParameterizedType sourceParameterizedValueType) {
        sourceGenericOrParameterizedType = sourceParameterizedValueType
            .getActualTypeArguments()[1];
      }
      return visitor.onMap(
          targetClazz, targetKeyClazz, targetValueClazz,
          targetGenericOrParameterizedType,
          sourceClazz, sourceKeyClazz, sourceValueClazz,
          sourceGenericOrParameterizedType);
    }

    List<TransformerTuple<Field, Field>> commonFields = getCommonFields(
        ModelTestUtil.getRepresentativeFields(targetClazz),
        ModelTestUtil.getRepresentativeFields(sourceClazz));

    return visitor.onObject(targetClazz, sourceClazz, commonFields);
  }

  private static List<TransformerTuple<Field, Field>> getCommonFields(
      List<Field> targetFields,
      List<Field> sourceFields) {

    List<TransformerTuple<Field, Field>> commonFields = new ArrayList<>();

    // If the performance becomes a problem this could be improved by using quick select, but is
    // unlikely
    for (Field targetField : targetFields) {
      String targetFieldName = ModelTestUtil.getJsonFieldName(targetField);
      for (Field sourceField : sourceFields) {
        String sourceFieldName = ModelTestUtil.getJsonFieldName(sourceField);
        if (Objects.equals(targetFieldName, sourceFieldName)) {
          commonFields.add(
              new TransformerTuple<>(targetField, sourceField)
          );
        }
      }
    }

    return commonFields;
  }

}
