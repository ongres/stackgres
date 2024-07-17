/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusCause;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.StackGresPostgresConfigReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PgConfigParametersValidatorTest extends AbstractPgConfigReview {

  private @NotNull PgConfigValidator validator = new PgConfigParametersValidator();

  @Test
  void givenValidConfigurationCreation_shouldNotFail() {
    assertDoesNotThrow(() -> validator.validate(validConfigReview()));
  }

  @Test
  void givenValidConfigurationUpdate_shouldNotFail() {
    assertDoesNotThrow(() -> validator.validate(validConfigUpdate()));
  }

  @Test
  void givenConfigurationDeletion_shouldNotFail() {
    assertDoesNotThrow(() -> validator.validate(validConfigDelete()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"11", "12", "13", "14"})
  void givenCreationWithInvalidProperties_shouldFail(String pgVersion) {
    StackGresPostgresConfigReview review = validConfigReview();
    StackGresPostgresConfig pgConfig = review.getRequest().getObject();
    pgConfig.getSpec().setPostgresVersion(pgVersion);
    Map<String, String> postgresqlConf = pgConfig.getSpec().getPostgresqlConf();
    postgresqlConf.put("default_toast_compression", "zstd");
    postgresqlConf.put("archive_mode", "test");

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });
    Status result = ex.getResult();
    checkValidationFailed(result);
  }

  @ParameterizedTest
  @ValueSource(strings = {"11", "12", "13", "14"})
  void givenUpdateWithInvalidProperties_shouldFail(String pgVersion) {
    StackGresPostgresConfigReview review = validConfigUpdate();
    StackGresPostgresConfig pgConfig = review.getRequest().getObject();
    pgConfig.getSpec().setPostgresVersion(pgVersion);
    Map<String, String> postgresqlConf = pgConfig.getSpec().getPostgresqlConf();
    postgresqlConf.put("default_toast_compression", "zstd");
    postgresqlConf.put("archive_mode", "test");

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });
    Status result = ex.getResult();
    checkValidationFailed(result);
  }

  @ParameterizedTest
  @ValueSource(strings = {"11", "12", "13", "14"})
  void givenCreateWithValidProperties_shouldPass(String pgVersion) {
    StackGresPostgresConfigReview review = validConfigReview();
    StackGresPostgresConfig pgConfig = review.getRequest().getObject();
    pgConfig.getSpec().setPostgresVersion(pgVersion);
    Map<String, String> postgresqlConf = pgConfig.getSpec().getPostgresqlConf();
    postgresqlConf.clear();
    postgresqlConf.put("application_name", "Demo App");
    postgresqlConf.put("checkpoint_completion_target", "0.99");
    postgresqlConf.put("cpu_operator_cost", "1.5987");
    postgresqlConf.put("jit", "0");
    postgresqlConf.put("log_statement", "all");
    postgresqlConf.put("max_locks_per_transaction", "256");
    postgresqlConf.put("pg_stat_statements.track", "top");
    postgresqlConf.put("pg_stat_statements.max", "10000");
    postgresqlConf.put("pg_stat_statements.track_planning", "true");
    postgresqlConf.put("auto_explain.log_wal", "true");
    postgresqlConf.put("auto_explain.log_format", "yaml");
    postgresqlConf.put("auto_explain.log_min_duration", "3s");
    postgresqlConf.put("session_preload_libraries", "auto_explain,pg_stat_statements");

    assertDoesNotThrow(() -> validator.validate(review));
  }

  @ParameterizedTest
  @CsvSource(value = {
      "default_toast_compression,zstd,"
          + "'invalid value for parameter \"default_toast_compression\": \"zstd\"',"
          + "'Available values: pglz, lz4'",
      "deadlock_timeout,0h,"
          + "'0 ms is outside the valid range for parameter "
          + "\"deadlock_timeout\" (1 .. 2147483647)',",
      "max_worker_processes,-1,"
          + "'-1 is outside the valid range for parameter "
          + "\"max_worker_processes\" (0 .. 262143)',",
      "hash_mem_multiplier,1000.99,"
          + "'1000.99 is outside the valid range for parameter "
          + "\"hash_mem_multiplier\" (1 .. 1000)',",
      "enable_memoize,99,"
          + "'parameter \"enable_memoize\" requires a Boolean value but was: \"99\"',"
          + "'Allowed values: on, off'"})
  void givenParametersWithInvalidProperties_shouldFail(String param, String value, String message,
      String reason) {
    StackGresPostgresConfigReview review = validConfigUpdate();
    StackGresPostgresConfig pgConfig = review.getRequest().getObject();
    pgConfig.getSpec().setPostgresVersion("14");
    Map<String, String> postgresqlConf = pgConfig.getSpec().getPostgresqlConf();
    postgresqlConf.put(param, value);

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    Status result = ex.getResult();
    checkValidationFailed(result);

    StatusCause statusCause = result.getDetails().getCauses().get(0);
    assertThat(statusCause.getField()).isEqualTo("spec.postgresql\\.conf." + param);
    assertThat(statusCause.getMessage()).isEqualTo(message);
    assertThat(statusCause.getReason()).isEqualTo(reason);
  }

  private void checkValidationFailed(Status result) {
    assertThat(result).isNotNull();
    assertThat(result.getCode()).isEqualTo(400);
    assertThat(result.getKind()).isEqualTo(StackGresPostgresConfig.KIND);
    assertThat(result.getMessage())
        .isEqualTo("Postgres configuration \"postgresconf\" has invalid parameters.");
    assertThat(result.getDetails()).isNotNull();
    assertThat(result.getDetails().getCauses()).isNotEmpty();
    result.getDetails().getCauses().forEach(cause -> {
      assertThat(cause.getField()).isNotEmpty();
      assertThat(cause.getMessage()).isNotEmpty();
    });
  }
}
