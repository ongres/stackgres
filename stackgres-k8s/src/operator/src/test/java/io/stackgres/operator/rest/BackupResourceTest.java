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

import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupList;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.rest.dto.backup.BackupDto;
import io.stackgres.operator.rest.transformer.BackupConfigTransformer;
import io.stackgres.operator.rest.transformer.BackupTransformer;
import io.stackgres.operator.utils.JsonUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupResourceTest {

  @Mock
  private CustomResourceFinder<StackGresBackup> finder;

  @Mock
  private CustomResourceScanner<StackGresBackup> scanner;

  @Mock
  private CustomResourceScheduler<StackGresBackup> scheduler;

  private StackGresBackupList backups;

  private BackupDto backupDto;

  private BackupResource resource;

  @BeforeEach
  void setUp() {
    backups = JsonUtil
        .readFromJson("stackgres_backup/list.json", StackGresBackupList.class);
    backupDto = JsonUtil
        .readFromJson("stackgres_backup/dto.json", BackupDto.class);

    resource = new BackupResource(scanner, finder, scheduler,
        new BackupTransformer(new BackupConfigTransformer()));
  }

  @Test
  void listShouldReturnAllBackups() {
    when(scanner.getResources()).thenReturn(backups.getItems());

    List<BackupDto> backups = resource.list();

    assertEquals(1, backups.size());

    assertNotNull(backups.get(0).getMetadata());

    assertEquals("postgresql", backups.get(0).getMetadata().getNamespace());

    assertEquals("test", backups.get(0).getMetadata().getName());
  }

  @Test
  void getOfAnExistingBackupShouldReturnTheExistingBackup() {
    when(finder.findByNameAndNamespace("test", "postgresql"))
        .thenReturn(Optional.of(backups.getItems().get(0)));

    BackupDto backup = resource.get("postgresql", "test");

    assertNotNull(backup.getMetadata());

    assertEquals("postgresql", backup.getMetadata().getNamespace());

    assertEquals("test", backup.getMetadata().getName());
  }

  @Test
  void createShouldNotFail() {
    resource.create(backupDto);
  }

  @Test
  void updateShouldNotFail() {
    resource.update(backupDto);
  }

  @Test
  void deleteShouldNotFail() {
    resource.delete(backupDto);
  }

}