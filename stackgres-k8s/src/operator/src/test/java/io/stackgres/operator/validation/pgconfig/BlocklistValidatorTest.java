/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.Blocklist;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.Test;

class BlocklistValidatorTest {

  private static String[] BLOCKLISTED_PROPERTIES = Blocklist.getBlocklistParameters().toArray(new String[0]);

  private BlocklistValidator validator = new BlocklistValidator();


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
  void givenCreationWithBlocklistedProperties_shouldFail(){

    PgConfigReview review = JsonUtil.readFromJson("pgconfig_allow_request/valid_pgconfig.json",
        PgConfigReview.class);

    StackGresPostgresConfig pgConfig = review.getRequest().getObject();
    String[] blacklistedProperties = addBlocklistProperties(pgConfig);

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String errorMessage = ex.getResult().getMessage();
    assertEquals("Invalid postgres configuration, properties: "
        + String.join(", ", blacklistedProperties)
        + " cannot be settled", errorMessage);
  }

  @Test
  void givenUpdateWithBlocklistedProperties_shouldFail(){

    PgConfigReview review = JsonUtil.readFromJson("pgconfig_allow_request/valid_pgconfig_update.json",
        PgConfigReview.class);

    StackGresPostgresConfig pgConfig = review.getRequest().getObject();
    String[] blacklistedProperties = addBlocklistProperties(pgConfig);

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

  private String[] addBlocklistProperties(StackGresPostgresConfig pgConfig) {
    Random r = new Random();
    int howManyBlacklistPropertiesToAdd = r.nextInt(BLOCKLISTED_PROPERTIES.length) + 1;

    Set<String> blacklistProperties = new HashSet<>();

    do {
      String randomProperty = BLOCKLISTED_PROPERTIES[r.nextInt(BLOCKLISTED_PROPERTIES.length)];
      blacklistProperties.add(randomProperty);
    } while (blacklistProperties.size() == howManyBlacklistPropertiesToAdd);

    blacklistProperties.forEach((b) -> pgConfig
        .getSpec().getPostgresqlConf().put(b, "I'm being naughty"));

    return blacklistProperties.toArray(new String[0]);
  }

}
