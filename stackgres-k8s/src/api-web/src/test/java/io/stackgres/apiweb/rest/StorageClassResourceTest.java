/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.fabric8.kubernetes.api.model.storage.StorageClassList;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorageClassResourceTest {

  @Mock
  private ResourceScanner<StorageClass> scanner;

  private StorageClassList storageClasses;

  private StorageClassResource resource;

  @BeforeEach
  void setUp() {
    storageClasses = JsonUtil
        .readFromJson("storage_class/list.json", StorageClassList.class);

    resource = new StorageClassResource();
    resource.setStorageClassScanner(scanner);
  }

  @Test
  void getShouldReturnAllStorageClassesNames() {
    when(scanner.findResources()).thenReturn(storageClasses.getItems());

    List<String> storageClasses = resource.get();

    assertEquals(1, storageClasses.size());

    assertEquals("standard", storageClasses.get(0));

  }
}
