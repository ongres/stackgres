/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.stackgres.apiweb.NamespaceResource;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.testutil.JsonUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NamespaceResourceTest {

  @Mock
  private ResourceScanner<Namespace> scanner;

  private NamespaceList namespaces;

  private NamespaceResource resource;

  @BeforeEach
  void setUp() {
    namespaces = JsonUtil
        .readFromJson("namespace/list.json", NamespaceList.class);

    resource = new NamespaceResource();
    resource.setNamespaceScanner(scanner);
  }

  @Test
  void getShouldReturnAllNamespacesNames() {
    when(scanner.findResources()).thenReturn(namespaces.getItems());

    List<String> namespaces = resource.get();

    assertEquals(1, namespaces.size());

    assertEquals("default", namespaces.get(0));

  }
}