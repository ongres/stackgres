/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.conciliation.factory.cluster.postgres.PostgresBlocklist;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PgConfigBlocklistValidatorTest extends AbstractPgConfigReview {

  private static String[] BLOCKLISTED_PROPERTIES =
      PostgresBlocklist.getBlocklistParameters().toArray(new String[0]);

  private @NotNull PgConfigValidator validator = new PgConfigBlocklistValidator();

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

  @Test
  void givenCreationWithBlocklistedProperties_shouldFail() {
    PgConfigReview review = validConfigReview();

    StackGresPostgresConfig pgConfig = review.getRequest().getObject();
    String[] blocklistedProperties = addBlocklistProperties(pgConfig);

    validateThrows(review, blocklistedProperties);
  }

  @ParameterizedTest
  @MethodSource("provideBlockedParameter")
  void givenCreationWithBlocklistedProperties_shouldFail(String parameter) {
    PgConfigReview review = validConfigReview();

    StackGresPostgresConfig pgConfig = review.getRequest().getObject();
    pgConfig.getSpec().getPostgresqlConf().put(parameter, "I'm being naughty");

    validateThrows(review, parameter);
  }

  @Test
  void givenUpdateWithBlocklistedProperties_shouldFail() {
    PgConfigReview review = validConfigUpdate();

    StackGresPostgresConfig pgConfig = review.getRequest().getObject();
    String[] blocklistedProperties = addBlocklistProperties(pgConfig);

    validateThrows(review, blocklistedProperties);
  }

  @ParameterizedTest
  @MethodSource("provideBlockedParameter")
  void givenUpdateWithBlocklistedProperties_shouldFail(String parameter) {
    PgConfigReview review = validConfigUpdate();

    StackGresPostgresConfig pgConfig = review.getRequest().getObject();
    pgConfig.getSpec().getPostgresqlConf().put(parameter, "I'm being naughty");

    validateThrows(review, parameter);
  }

  private void validateThrows(PgConfigReview review, String... parameters) {
    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String errorMessage = ex.getResult().getMessage();
    assertEquals("Invalid postgres configuration, properties: "
        + String.join(", ", parameters)
        + " cannot be settled", errorMessage);
    assertEquals(400, ex.getResult().getCode());
  }

  private String[] addBlocklistProperties(StackGresPostgresConfig pgConfig) {
    Random r = new Random();
    int howManyblocklistPropertiesToAdd = r.nextInt(BLOCKLISTED_PROPERTIES.length) + 1;

    Set<String> blocklistProperties = new HashSet<>();

    do {
      String randomProperty = BLOCKLISTED_PROPERTIES[r.nextInt(BLOCKLISTED_PROPERTIES.length)];
      blocklistProperties.add(randomProperty);
    } while (blocklistProperties.size() == howManyblocklistPropertiesToAdd);

    blocklistProperties.forEach((b) -> pgConfig
        .getSpec().getPostgresqlConf().put(b, "I'm being naughty"));

    return blocklistProperties.toArray(new String[0]);
  }

  private static Stream<Arguments> provideBlockedParameter() {
    return Arrays.stream(BLOCKLISTED_PROPERTIES)
        .map(t -> Arguments.of(t));
  }

}
