/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.common.resource.ResourceWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NamespaceResourceTest {

  @Mock
  private ResourceScanner<Namespace> scanner;

  @Mock
  private ResourceWriter<Namespace> writer;

  private NamespaceList namespaces;

  private NamespaceResource resource;

  @BeforeEach
  void setUp() {
    namespaces = Fixtures.namespaceList().loadDefault().get();

    resource = new NamespaceResource(scanner, writer);
  }

  @Test
  void getShouldReturnAllNamespacesNames() {
    final List<Namespace> listNamespaces = namespaces.getItems();
    when(scanner.findResources()).thenReturn(listNamespaces);

    final List<String> namespaces = resource.get();

    assertThat(listNamespaces, hasSize(7));
    assertThat(namespaces, hasSize(7));

    assertThat(namespaces, contains(
        "default", "kube-node-lease", "kube-public", "kube-system",
        "odoo", "pgconf-staging", "stackgres"));
  }

  @Test
  void postShouldCreateANamespace() {
    resource.create("test");

    verify(writer).create(any());
  }

}
