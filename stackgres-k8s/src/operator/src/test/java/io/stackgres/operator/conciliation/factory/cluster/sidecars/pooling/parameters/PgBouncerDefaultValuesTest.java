/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Properties;

import io.stackgres.common.StackGresVersion;
import org.junit.jupiter.api.Test;

class PgBouncerDefaultValuesTest {

  @Test
  void getDefaultValues_shouldReturnNonEmptyMap() {
    Map<String, String> defaultValues = PgBouncerDefaultValues.getDefaultValues();

    assertFalse(defaultValues.isEmpty());
  }

  @Test
  void getDefaultValues_shouldContainPoolMode() {
    Map<String, String> defaultValues = PgBouncerDefaultValues.getDefaultValues();

    assertTrue(defaultValues.containsKey("pool_mode"),
        "Expected default values to contain 'pool_mode'");
    assertEquals("session", defaultValues.get("pool_mode"));
  }

  @Test
  void getDefaultValues_shouldContainMaxClientConn() {
    Map<String, String> defaultValues = PgBouncerDefaultValues.getDefaultValues();

    assertTrue(defaultValues.containsKey("max_client_conn"),
        "Expected default values to contain 'max_client_conn'");
    assertEquals("1000", defaultValues.get("max_client_conn"));
  }

  @Test
  void getDefaultValues_shouldContainDefaultPoolSize() {
    Map<String, String> defaultValues = PgBouncerDefaultValues.getDefaultValues();

    assertTrue(defaultValues.containsKey("default_pool_size"),
        "Expected default values to contain 'default_pool_size'");
    assertEquals("1000", defaultValues.get("default_pool_size"));
  }

  @Test
  void getDefaultValues_shouldContainAuthType() {
    Map<String, String> defaultValues = PgBouncerDefaultValues.getDefaultValues();

    assertTrue(defaultValues.containsKey("auth_type"),
        "Expected default values to contain 'auth_type'");
    assertEquals("md5", defaultValues.get("auth_type"));
  }

  @Test
  void getDefaultValues_shouldContainServerCheckQuery() {
    Map<String, String> defaultValues = PgBouncerDefaultValues.getDefaultValues();

    assertTrue(defaultValues.containsKey("server_check_query"),
        "Expected default values to contain 'server_check_query'");
    assertEquals(";", defaultValues.get("server_check_query"));
  }

  @Test
  void getProperties_shouldReturnNonNullProperties() {
    Properties properties = PgBouncerDefaultValues.getProperties();

    assertNotNull(properties);
    assertFalse(properties.isEmpty());
  }

  @Test
  void getProperties_withVersion_shouldReturnNonNullProperties() {
    Properties properties = PgBouncerDefaultValues.getProperties(StackGresVersion.LATEST);

    assertNotNull(properties);
    assertFalse(properties.isEmpty());
  }

  @Test
  void getProperties_withNullVersion_shouldThrowNpe() {
    assertThrows(NullPointerException.class,
        () -> PgBouncerDefaultValues.getProperties(null));
  }

  @Test
  void getDefaultValues_withVersion_shouldReturnSameAsNoArgs() {
    Map<String, String> defaultValues = PgBouncerDefaultValues.getDefaultValues();
    Map<String, String> defaultValuesWithVersion =
        PgBouncerDefaultValues.getDefaultValues(StackGresVersion.LATEST);

    assertEquals(defaultValues, defaultValuesWithVersion);
  }
}
