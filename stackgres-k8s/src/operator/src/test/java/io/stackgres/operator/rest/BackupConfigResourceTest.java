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

import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigList;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.rest.dto.backupconfig.BackupConfigDto;
import io.stackgres.operator.rest.transformer.BackupConfigTransformer;
import io.stackgres.operator.utils.JsonUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupConfigResourceTest {

  @Mock
  private CustomResourceFinder<StackGresBackupConfig> finder;

  @Mock
  private CustomResourceScanner<StackGresBackupConfig> scanner;

  @Mock
  private CustomResourceScheduler<StackGresBackupConfig> scheduler;

  private StackGresBackupConfigList backupConfigs;

  private BackupConfigDto backupConfigDto;

  private BackupConfigResource resource;

  @BeforeEach
  void setUp() {
    backupConfigs = JsonUtil
        .readFromJson("backup_config/list.json", StackGresBackupConfigList.class);
    backupConfigDto = JsonUtil
        .readFromJson("backup_config/dto.json", BackupConfigDto.class);

    resource = new BackupConfigResource(scanner, finder, scheduler,
        new BackupConfigTransformer());
  }

  @Test
  void listShouldReturnAllBackupConfigs() {
    when(scanner.getResources()).thenReturn(backupConfigs.getItems());

    List<BackupConfigDto> backupConfigs = resource.list();

    assertEquals(1, backupConfigs.size());

    assertNotNull(backupConfigs.get(0).getMetadata());

    assertEquals("stackgres", backupConfigs.get(0).getMetadata().getNamespace());

    assertEquals("backupconf", backupConfigs.get(0).getMetadata().getName());
  }

  @Test
  void getOfAnExistingBackupConfigShouldReturnTheExistingBackupConfig() {
    when(finder.findByNameAndNamespace("backupconf", "stackgres"))
        .thenReturn(Optional.of(backupConfigs.getItems().get(0)));

    BackupConfigDto backupConfig = resource.get("stackgres", "backupconf");

    assertNotNull(backupConfig.getMetadata());

    assertEquals("stackgres", backupConfig.getMetadata().getNamespace());

    assertEquals("backupconf", backupConfig.getMetadata().getName());
  }

  @Test
  void createShouldNotFail() {
    resource.create(backupConfigDto);
  }

  @Test
  void updateShouldNotFail() {
    resource.update(backupConfigDto);
  }

  @Test
  void deleteShouldNotFail() {
    resource.delete(backupConfigDto);
  }

}