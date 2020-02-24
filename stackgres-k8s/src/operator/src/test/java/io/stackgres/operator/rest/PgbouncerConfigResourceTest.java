/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.rest.dto.pgbouncerconfig.PgbouncerConfigDto;
import io.stackgres.operator.rest.transformer.PgbouncerConfigTransformer;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigList;
import io.stackgres.operator.utils.JsonUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PgbouncerConfigResourceTest {

  @Mock
  private CustomResourceFinder<StackGresPgbouncerConfig> finder;

  @Mock
  private CustomResourceScanner<StackGresPgbouncerConfig> scanner;

  @Mock
  private CustomResourceScheduler<StackGresPgbouncerConfig> scheduler;

  private StackGresPgbouncerConfigList pgbouncerConfigs;

  private PgbouncerConfigDto pgbouncerConfigDto;

  private ConnectionPoolingConfigResource resource;

  @BeforeEach
  void setUp() {
    pgbouncerConfigs = JsonUtil
        .readFromJson("pgbouncer_config/list.json", StackGresPgbouncerConfigList.class);
    pgbouncerConfigDto = JsonUtil
        .readFromJson("pgbouncer_config/dto.json", PgbouncerConfigDto.class);

    resource = new ConnectionPoolingConfigResource(scanner, finder, scheduler,
        new PgbouncerConfigTransformer());
  }

  @Test
  void listShouldReturnAllPgbouncerConfigs() {
    when(scanner.getResources()).thenReturn(pgbouncerConfigs.getItems());

    List<PgbouncerConfigDto> pgbouncerConfigs = resource.list();

    assertEquals(1, pgbouncerConfigs.size());

    assertNotNull(pgbouncerConfigs.get(0).getMetadata());

    assertEquals("default", pgbouncerConfigs.get(0).getMetadata().getNamespace());

    assertEquals("pgbouncerconf", pgbouncerConfigs.get(0).getMetadata().getName());
  }

  @Test
  void getOfAnExistingPgbouncerConfigShouldReturnTheExistingPgbouncerConfig() {
    when(finder.findByNameAndNamespace("pgbouncerconf", "default"))
        .thenReturn(Optional.of(pgbouncerConfigs.getItems().get(0)));

    PgbouncerConfigDto pgbouncerConfig = resource.get("default", "pgbouncerconf");

    assertNotNull(pgbouncerConfig.getMetadata());

    assertEquals("default", pgbouncerConfig.getMetadata().getNamespace());

    assertEquals("pgbouncerconf", pgbouncerConfig.getMetadata().getName());
  }

  @Test
  void createShouldNotFail() {
    resource.create(pgbouncerConfigDto);
  }

  @Test
  void updateShouldNotFail() {
    resource.update(pgbouncerConfigDto);
  }

  @Test
  void deleteShouldNotFail() {
    resource.delete(pgbouncerConfigDto);
  }

}