/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.stackgres.apiweb.config.WebApiContext;
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

    resource = new NamespaceResource(scanner, new WebApiContext());
  }

  @Test
  void getShouldReturnAllNamespacesNames() {
    final List<Namespace> listNamespaces = namespaces.getItems();
    when(scanner.findResources()).thenReturn(listNamespaces);

    final List<String> namespaces = resource.get();

    assertThat(listNamespaces, hasSize(7));
    assertThat(namespaces, hasSize(3));

    assertThat(namespaces, contains("default", "odoo", "pgconf-staging"));
  }

  @Test
  void getShouldNotReturnStackGresNamespace() {
    when(scanner.findResources()).thenReturn(namespaces.getItems());

    final List<String> namespaces = resource.get();

    assertThat(namespaces, not(hasItem("stackgres")));
  }

  @Test
  void getShouldNotReturnKubeNamespacesNames() {
    when(scanner.findResources()).thenReturn(namespaces.getItems());

    final List<String> namespaces = resource.get();

    assertThat(namespaces, not(hasItems("kube-public", "kube-node-lease", "kube-system")));
  }

}
