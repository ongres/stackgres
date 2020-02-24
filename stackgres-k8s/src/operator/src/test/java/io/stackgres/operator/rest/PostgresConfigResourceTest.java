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

import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigList;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.rest.dto.pgconfig.PostgresConfigDto;
import io.stackgres.operator.rest.transformer.PostgresConfigTransformer;
import io.stackgres.operator.utils.JsonUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostgresConfigResourceTest {

  @Mock
  private CustomResourceFinder<StackGresPostgresConfig> finder;

  @Mock
  private CustomResourceScanner<StackGresPostgresConfig> scanner;

  @Mock
  private CustomResourceScheduler<StackGresPostgresConfig> scheduler;

  private StackGresPostgresConfigList postgresConfigs;

  private PostgresConfigDto postgresConfigDto;

  private PostgresConfigResource resource;

  @BeforeEach
  void setUp() {
    postgresConfigs = JsonUtil
        .readFromJson("postgres_config/list.json", StackGresPostgresConfigList.class);
    postgresConfigDto = JsonUtil
        .readFromJson("postgres_config/dto.json", PostgresConfigDto.class);

    resource = new PostgresConfigResource(scanner, finder, scheduler,
        new PostgresConfigTransformer());
  }

  @Test
  void listShouldReturnAllPostgresConfigs() {
    when(scanner.getResources()).thenReturn(postgresConfigs.getItems());

    List<PostgresConfigDto> postgresConfigs = resource.list();

    assertEquals(1, postgresConfigs.size());

    assertNotNull(postgresConfigs.get(0).getMetadata());

    assertEquals("default", postgresConfigs.get(0).getMetadata().getNamespace());

    assertEquals("postgresconf", postgresConfigs.get(0).getMetadata().getName());
  }

  @Test
  void getOfAnExistingPostgresConfigShouldReturnTheExistingPostgresConfig() {
    when(finder.findByNameAndNamespace("postgresconf", "default"))
        .thenReturn(Optional.of(postgresConfigs.getItems().get(0)));

    PostgresConfigDto postgresConfig = resource.get("default", "postgresconf");

    assertNotNull(postgresConfig.getMetadata());

    assertEquals("default", postgresConfig.getMetadata().getNamespace());

    assertEquals("postgresconf", postgresConfig.getMetadata().getName());
  }

  @Test
  void createShouldNotFail() {
    resource.create(postgresConfigDto);
  }

  @Test
  void updateShouldNotFail() {
    resource.update(postgresConfigDto);
  }

  @Test
  void deleteShouldNotFail() {
    resource.delete(postgresConfigDto);
  }

}