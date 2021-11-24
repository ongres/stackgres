/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.kubernetesclient;

import java.util.Iterator;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagedFieldsReader {

  private static final Logger LOGGER = LoggerFactory.getLogger(ManagedFieldsReader.class);

  private static final ObjectMapper MANAGE_FIELDS_JSON_MAPPER = Serialization.jsonMapper();

  public ManagedFieldsReader() {
    throw new IllegalStateException("It should not be instantiated");
  }

  /**
   * Reads kubernetes object and remove all fields that are not managed.
   * Given a kubernetes object like the following:
   * <p>
   * <code>
   * apiVersion: v1<br>
   * kind: ConfigMap<br>
   * metadata:<br>
   * &nbsp;&nbsp;name: test-cm<br>
   * &nbsp;&nbsp;namespace: default<br>
   * &nbsp;&nbsp;labels:<br>
   * &nbsp;&nbsp;test-label: test<br>
   * &nbsp;&nbsp;managedFields:<br>
   * &nbsp;&nbsp;&nbsp;-&nbsp;manager: kubectl<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;operation: Apply<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;apiVersion: v1<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fields:<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;f:metadata:<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;f:labels:<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;f:test-label: {}<br>
   * &nbsp;&nbsp;&nbsp;-&nbsp;manager: kube-controller-manager<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;operation: Update<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;apiVersion: v1<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;time: '2019-03-30T16:00:00.000Z'<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fields:<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;f:data:<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;f:key: {}<br>
   * data:<br>
   * &nbsp;&nbsp;&nbsp;key: new value<br>
   * </code>
   * </p>
   * <p>
   * If the provided fieldManager is kubectl, this function will return:
   * <code>
   * apiVersion: v1<br>
   * kind: ConfigMap<br>
   * metadata:<br>
   * &nbsp;&nbsp;&nbsp;name: test-cm<br>
   * &nbsp;&nbsp;&nbsp;namespace: default<br>
   * &nbsp;&nbsp;&nbsp;labels:<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;test-label: test<br>
   * </code>
   * </p>
   *
   * @param source The object with the managed fields configuration
   * @return the source object without the managed fields configuration
   */
  public static ObjectNode readManagedFields(ObjectNode source, String fieldManager) {

    if (!source.has("metadata")
        || (source.has("metadata") && !source.get("metadata").has("name")
        || (source.has("metadata") && !source.get("metadata").has("namespace")))) {
      throw new IllegalArgumentException("Invalid kubernetes source object "
          + source);
    }
    ObjectNode managedFields = getManagedFieldConfiguration(source, fieldManager);

    ObjectNode rootManagedFieldConfiguration = (ObjectNode) managedFields.get("fieldsV1");

    ObjectNode cleanedUpObject = readOnlyManagedFields(source, rootManagedFieldConfiguration);

    if (source.has("apiVersion")) {
      cleanedUpObject.set("apiVersion", source.get("apiVersion"));
    }
    if (source.has("kind")) {
      cleanedUpObject.set("kind", source.get("kind"));
    }
    ObjectNode cleanedUpMetadata;
    if (cleanedUpObject.has("metadata")) {
      cleanedUpMetadata = (ObjectNode) cleanedUpObject.get("metadata");
    } else {
      cleanedUpMetadata = MANAGE_FIELDS_JSON_MAPPER.createObjectNode();
    }
    cleanedUpMetadata.set("name", source.get("metadata").get("name"));
    cleanedUpMetadata.set("namespace", source.get("metadata").get("namespace"));
    cleanedUpObject.set("metadata", cleanedUpMetadata);

    return cleanedUpObject;
  }

  /**
   * Returns the given json object with only the fields that appears in the given managed fields
   * configuration.
   *
   * @param source                    the json object to analyse
   * @param managedFieldConfiguration the managed fields configuration
   * @return a new json object containing only the managed fields
   */
  protected static ObjectNode readOnlyManagedFields(ObjectNode source,
                                                    ObjectNode managedFieldConfiguration) {

    if (managedFieldConfiguration.size() == 0) {
      return source.deepCopy();
    }

    ObjectNode cleanedUpObject = MANAGE_FIELDS_JSON_MAPPER.createObjectNode();
    Iterator<String> managedFields = managedFieldConfiguration.fieldNames();

    while (managedFields.hasNext()) {
      String managedField = managedFields.next();
      if (managedField.equals(".")) {
        continue;
      }
      if (managedField.startsWith("f:")) {
        String fieldName = managedField.substring(2);
        JsonNode jsonNode = source.get(fieldName);
        if (jsonNode == null) {
          /*
           * This is a nasty hack due to the stringData property in the Secret resources.
           * stringData field is used only for writes, but never on reads.
           * Hence, it appears in managed fields configuration but not in the object itself.
           * No other kubernetes resource exhibits this behavior, therefore it should
           * suffice by the time being
           */
          if (fieldName.equals("stringData") && source.has("data")) {
            jsonNode = source.get("data");
            fieldName = "data";
          } else {
            LOGGER.debug("Field {} appears as managed but can't be found in the json object {}",
                fieldName,
                source.toPrettyString());
            continue;
          }
        }
        if (jsonNode.isObject()) {
          cleanedUpObject.set(fieldName,
              readOnlyManagedFields(
                  (ObjectNode) jsonNode,
                  (ObjectNode) managedFieldConfiguration.get(managedField)
              )
          );
        } else if (jsonNode.isValueNode()) {
          cleanedUpObject.set(fieldName, jsonNode);
        } else if (jsonNode.isArray()) {
          cleanedUpObject.set(fieldName,
              readOnlyManagedFields(
                  (ArrayNode) jsonNode,
                  (ObjectNode) managedFieldConfiguration.get(managedField)
              )
          );
        } else {
          throw new IllegalArgumentException("Unexpected node type "
              + jsonNode.getNodeType());
        }
      } else {
        throw new IllegalArgumentException("Unexpected managed key " + managedField);
      }
    }
    return cleanedUpObject;
  }

  /**
   * Looks the managed items in a json array.
   *
   * <p>Given a json array like the following:
   * [<br>
   * &nbsp;&nbsp;&nbsp;{<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"name": "pgport",<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"port": 5432,<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"protocol": "TCP",<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"targetPort": "pgport"<br>
   * &nbsp;&nbsp;&nbsp;},<br>
   * &nbsp;&nbsp;&nbsp;{<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"name": "pgreplication",<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"port": 5433,<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"protocol": "TCP",<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"targetPort": "pgreplication",<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"someUnknownField": "unknownValue"<br>
   * &nbsp;&nbsp;&nbsp;},<br>
   * &nbsp;&nbsp;&nbsp;{<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"name": "other",<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"port": 9999,<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"protocol": "TCP",<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"targetPort": "other"<br>
   * &nbsp;&nbsp;&nbsp;}<br>
   * ]<br>
   * </p>
   * And managed field configuration:
   * {<br>
   * &nbsp;&nbsp;&nbsp;"k:{\"port\":5432,\"protocol\":\"TCP\"}": {<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;".": {},<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"f:name": {},<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"f:port": {},<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"f:protocol": {},<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"f:targetPort": {}<br>
   * &nbsp;&nbsp;&nbsp;},<br>
   * &nbsp;&nbsp;&nbsp;"k:{\"port\":5433,\"protocol\":\"TCP\"}": {<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;".": {},<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"f:name": {},<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"f:port": {},<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"f:protocol": {},<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"f:targetPort": {}<br>
   * &nbsp;&nbsp;&nbsp;}<br>
   * }<br>
   * <p>it will return:
   * [<br>
   * &nbsp;&nbsp;&nbsp;{<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"name": "pgport",<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"port": 5432,<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"protocol": "TCP",<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"targetPort": "pgport"<br>
   * &nbsp;&nbsp;&nbsp;},<br>
   * &nbsp;&nbsp;&nbsp;{<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"name": "pgreplication",<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"port": 5433,<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"protocol": "TCP",<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"targetPort": "pgreplication"<br>
   * &nbsp;&nbsp;&nbsp;}<br>
   * ]<br>
   * </p>
   * @param source                    the json array to look into
   * @param managedFieldConfiguration the array managed field configuration
   * @return the array items with the managed configuration
   */
  protected static ArrayNode readOnlyManagedFields(ArrayNode source,
                                                   ObjectNode managedFieldConfiguration) {
    if (managedFieldConfiguration.size() == 0) {
      return source.deepCopy();
    }

    ArrayNode cleanedUpArray = MANAGE_FIELDS_JSON_MAPPER.createArrayNode();

    for (JsonNode arrayItem : source) {
      ObjectNode item = (ObjectNode) arrayItem;

      Optional<ObjectNode> managedConfiguration = getManagingConfiguration(
          item,
          managedFieldConfiguration
      );

      managedConfiguration.ifPresent(mc -> {
        ObjectNode cleanedArrayItem = readOnlyManagedFields(item, mc);
        cleanedUpArray.add(cleanedArrayItem);
      });

    }
    return cleanedUpArray;
  }

  private static Optional<ObjectNode> getManagingConfiguration(
      ObjectNode item,
      ObjectNode managedFieldConfiguration) {

    Iterator<String> managedFields = managedFieldConfiguration.fieldNames();
    while (managedFields.hasNext()) {
      String managedArrayItem = managedFields.next();
      if (managedArrayItem.startsWith("k:")) {
        String key = managedArrayItem.substring(2);
        try {
          ObjectNode itemKey = (ObjectNode) MANAGE_FIELDS_JSON_MAPPER.readTree(key);
          if (isContainedIn(itemKey, item)) {
            return Optional.of(
                (ObjectNode) managedFieldConfiguration.get(managedArrayItem)
            );
          }
        } catch (JsonProcessingException e) {
          throw new IllegalArgumentException("Error parsing key " + key, e);
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Search in a json array for an item based on a key object.
   * <p>Given a json array like the following:</p>
   * <code>
   * [<br>
   * {<br>
   * &nbsp;&nbsp;"name": "pgport",<br>
   * &nbsp;&nbsp;"port": 5432,<br>
   * &nbsp;&nbsp;"protocol": "TCP",<br>
   * &nbsp;&nbsp;"targetPort": "pgport"<br>
   * },<br>
   * {<br>
   * &nbsp;&nbsp;"name": "pgreplication",<br>
   * &nbsp;&nbsp;"port": 5433,<br>
   * &nbsp;&nbsp;"protocol": "TCP",<br>
   * &nbsp;&nbsp;"targetPort": "pgreplication"<br>
   * }<br>
   * ]<br>
   * </code>
   * <p>And a key like this:</p>
   * <code>
   * {<br>
   * &nbsp;&nbsp;"port": 5432,<br>
   * &nbsp;&nbsp;"protocol": "TCP",<br>
   * }<br>
   * </code>
   * <p>It will return the item:</p>
   * <code>
   * {<br>
   * &nbsp;&nbsp;"name": "pgport",<br>
   * &nbsp;&nbsp;"port": 5432,<br>
   * &nbsp;&nbsp;"protocol": "TCP",<br>
   * &nbsp;&nbsp;"targetPort": "pgport"<br>
   * }<br>
   * </code>
   *
   * @param source the array to search
   * @param key    the json object key
   * @return the matching item
   * @throws IllegalArgumentException if no matching was found
   */
  protected static ObjectNode findMatchingItem(ArrayNode source, ObjectNode key) {

    for (JsonNode sourceItem : source) {
      ObjectNode sourceArrayItem = (ObjectNode) sourceItem;

      if (isContainedIn(key, sourceArrayItem)) {
        return sourceArrayItem;
      }
    }

    throw new IllegalArgumentException("Cannot find key "
        + key.toString()
        + " in array "
        + source.toPrettyString());
  }

  private static boolean isContainedIn(ObjectNode key, ObjectNode source) {
    Iterator<String> keyFields = key.fieldNames();
    while (keyFields.hasNext()) {
      String keyField = keyFields.next();
      if (source.has(keyField)) {
        JsonNode keyValue = key.get(keyField);
        JsonNode sourceValue = source.get(keyField);
        if (!keyValue.equals(sourceValue)) {
          return false;
        }
      } else {
        return false;
      }
    }
    return true;
  }

  /**
   * Fields the managed fields configuration of a kubernetes object.
   * <p>Given a kubernetes object like the following and the field manager 'kubectl':</p>
   * <code>
   * apiVersion: v1<br>
   * kind: ConfigMap<br>
   * metadata:<br>
   * &nbsp;&nbsp;&nbsp;name: test-cm<br>
   * &nbsp;&nbsp;&nbsp;namespace: default<br>
   * &nbsp;&nbsp;&nbsp;labels:<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;test-label: test<br>
   * &nbsp;&nbsp;&nbsp;managedFields:<br>
   * &nbsp;&nbsp;&nbsp;-&nbsp;manager: kubectl<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;operation: Apply<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;apiVersion: v1<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fields:<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;f:metadata:<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;f:labels:<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;f:test-label: {}<br>
   * &nbsp;&nbsp;&nbsp;-&nbsp;manager: kube-controller-manager<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;operation: Update<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;apiVersion: v1<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;time: '2019-03-30T16:00:00.000Z'<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fields:<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;f:data:<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;f:key: {}<br>
   * data:<br>
   * &nbsp;&nbsp;&nbsp;key: new value<br>
   * </code>
   * <p>it will return:</p>
   * <code>
   * manager: kube-controller-manager<br>
   * operation: Update<br>
   * apiVersion: v1<br>
   * time: '2019-03-30T16:00:00.000Z'<br>
   * fields:<br>
   * &nbsp;&nbsp;&nbsp;f:data:<br>
   * &nbsp;&nbsp;&nbsp;f:key: {}<br>
   * </code>
   *
   * @param source       the kubernetes object to search the managed fields from
   * @param fieldManager the field managed who owns the configuration
   * @return the field managed configuration
   */
  public static ObjectNode getManagedFieldConfiguration(ObjectNode source, String fieldManager) {
    if (source.has("metadata") && source.get("metadata").has("managedFields")) {
      ArrayNode managedFields = (ArrayNode) source.get("metadata").get("managedFields");

      for (JsonNode managedFieldConfiguration : managedFields) {
        String configuredManager = managedFieldConfiguration.get("manager").asText();
        if (configuredManager.equals(fieldManager)) {
          return (ObjectNode) managedFieldConfiguration;
        }
      }
    }
    throw new IllegalArgumentException("Managed fields configuration not found");
  }

  public static boolean hasManagedFieldsConfiguration(ObjectNode source, String fieldManager) {
    try {
      getManagedFieldConfiguration(source, fieldManager);
      return true;
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }
}
