/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operator.common.PgConfigReview;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BlacklistValidatorTest {

  private static String[] BLACKLISTED_PROPERTIES = PgConfigValidator.BLACKLIST_PROPERTIES;

  private BlacklistValidator validator = new BlacklistValidator();


  @Test
  void givenValidConfigurationCreation_shouldNotFail() throws ValidationFailed {

    PgConfigReview review = JsonUtil.readFromJson("pgconfig_allow_request/valid_pgconfig.json",
        PgConfigReview.class);

    validator.validate(review);

  }

  @Test
  void givenValidConfigurationUpdate_shouldNotFail() throws ValidationFailed {

    PgConfigReview review = JsonUtil.readFromJson("pgconfig_allow_request/valid_pgconfig_update.json",
        PgConfigReview.class);

    validator.validate(review);

  }

  @Test
  void givenConfigurationDeletion_shouldNotFail() throws ValidationFailed {

    PgConfigReview review = JsonUtil.readFromJson("pgconfig_allow_request/pgconfig_delete.json",
            PgConfigReview.class);

    validator.validate(review);

  }

  @Test
  void givenCreationWithBlacklistedProperties_shouldFail(){

    PgConfigReview review = JsonUtil.readFromJson("pgconfig_allow_request/valid_pgconfig.json",
        PgConfigReview.class);

    StackGresPostgresConfig pgConfig = review.getRequest().getObject();
    String[] blacklistedProperties = addBlacklistProperties(pgConfig);

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String errorMessage = ex.getResult().getMessage();
    assertEquals("Invalid postgres configuration, properties: "
        + String.join(", ", blacklistedProperties)
        + " cannot be settled", errorMessage);
  }

  @Test
  void givenUpdateWithBlacklistedProperties_shouldFail(){

    PgConfigReview review = JsonUtil.readFromJson("pgconfig_allow_request/valid_pgconfig_update.json",
        PgConfigReview.class);

    StackGresPostgresConfig pgConfig = review.getRequest().getObject();
    String[] blacklistedProperties = addBlacklistProperties(pgConfig);

    assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String errorMessage = ex.getResult().getMessage();
    assertEquals("Invalid postgres configuration, properties: "
        + String.join(", ", blacklistedProperties)
        + " cannot be settled", errorMessage);

  }

  private String[] addBlacklistProperties(StackGresPostgresConfig pgConfig) {
    Random r = new Random();
    int howManyBlacklistPropertiesToAdd = r.nextInt(BLACKLISTED_PROPERTIES.length) + 1;

    Set<String> blacklistProperties = new HashSet<>();

    do {
      String randomProperty = BLACKLISTED_PROPERTIES[r.nextInt(BLACKLISTED_PROPERTIES.length)];
      blacklistProperties.add(randomProperty);
    } while (blacklistProperties.size() == howManyBlacklistPropertiesToAdd);

    blacklistProperties.forEach((b) -> pgConfig
        .getSpec().getPostgresqlConf().put(b, "I'm being naughty"));

    return blacklistProperties.toArray(new String[0]);
  }

}
