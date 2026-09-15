/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.docir.DocirExtensionMetadata;
import io.stackgres.common.docir.DocirMetadataManager;
import io.stackgres.common.docir.StackGresContextMock;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbOpsMajorVersionUpgradeExtensionsContextAppenderTest {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
      .streamOrderedVersions(StackGresContextMock.CONTEXT).findFirst().get();

  private static final String POSTGRES_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
      .streamOrderedMajorVersions(StackGresContextMock.CONTEXT).findFirst().get();

  private static final List<String> DEFAULT_EXTENSIONS =
      List.of("pg_stat_statements", "dblink", "auto_explain", "plpython3u");

  private DbOpsMajorVersionUpgradeExtensionsContextAppender contextAppender;

  private StackGresDbOps dbOps;

  private StackGresCluster cluster;

  private List<StackGresClusterInstalledExtension> installedExtensions;

  @Spy
  private StackGresDbOpsContext.Builder contextBuilder;

  @Mock
  private StackGresContext context;

  @Mock
  private DocirMetadataManager docirMetadataManager;

  @Mock
  private ExtensionMetadataManager extensionMetadataManager;

  @BeforeEach
  void setUp() {
    dbOps = Fixtures.dbOps().loadMajorVersionUpgrade().get();
    dbOps.getSpec().getMajorVersionUpgrade().setPostgresVersion(POSTGRES_VERSION);
    dbOps.getSpec().getMajorVersionUpgrade().setPostgresExtensions(
        List.of(getExtension("cube")));
    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getSpec().getPostgres().setExtensions(List.of());
    installedExtensions = Seq.seq(DEFAULT_EXTENSIONS)
        .append("cube")
        .map(this::getInstalledExtension)
        .toList();
    contextAppender = new DbOpsMajorVersionUpgradeExtensionsContextAppender(
        context,
        extensionMetadataManager);
    lenient().when(context.getMetadataManager())
        .thenReturn(docirMetadataManager);
    lenient().when(docirMetadataManager.getFlavors())
        .thenReturn(StackGresContextMock.CONTEXT.getMetadataManager().getFlavors());
    lenient().when(docirMetadataManager.getFlavors(nullable(URI.class)))
        .thenReturn(StackGresContextMock.CONTEXT.getMetadataManager().getFlavors());
  }

  @Test
  void givenExtensionsAvailable_shouldSetToInstallPostgresExtensionsWithoutPublisher() {
    mockFoundDefaultExtensions();
    when(docirMetadataManager.findExtensionCandidateSameMajorBuild(
        any(),
        any(StackGresCluster.class),
        any(StackGresClusterExtension.class),
        anyBoolean()))
        .then(this::findExtensionMetadata);

    contextAppender.appendContext(dbOps, cluster, contextBuilder);

    final var toInstallExtensions = dbOps.getSpec().getMajorVersionUpgrade()
        .getToInstallPostgresExtensions();
    assertEquals(
        Seq.seq(installedExtensions)
        .map(StackGresClusterInstalledExtension::getName)
        .sorted()
        .toList(),
        Seq.seq(toInstallExtensions)
        .map(StackGresClusterInstalledExtension::getName)
        .sorted()
        .toList());
    toInstallExtensions.forEach(
        installedExtension -> assertNull(installedExtension.getPublisher()));
  }

  @Test
  void givenAnExtensionNotAvailable_shouldFail() {
    mockFoundDefaultExtensions();
    when(docirMetadataManager.getExtensionsAnyVersion(
        any(),
        any(StackGresCluster.class),
        any(StackGresClusterExtension.class),
        anyBoolean()))
        .thenReturn(List.of());

    var ex = assertThrows(IllegalArgumentException.class,
        () -> contextAppender.appendContext(dbOps, cluster, contextBuilder));
    assertEquals("Extension was not found: cube 1.0.0", ex.getMessage());
  }

  private void mockFoundDefaultExtensions() {
    lenient().when(docirMetadataManager.findExtensionCandidateAnyVersion(
        any(),
        any(StackGresCluster.class),
        any(StackGresClusterExtension.class),
        anyBoolean()))
        .then(this::findExtensionMetadata);
  }

  private Optional<DocirExtensionMetadata> findExtensionMetadata(InvocationOnMock invocation) {
    final StackGresClusterExtension extension = invocation.getArgument(2);
    return installedExtensions.stream()
        .filter(installedExtension -> installedExtension.getName().equals(extension.getName()))
        .findFirst()
        .map(DocirExtensionMetadata::new);
  }

  private StackGresClusterInstalledExtension getInstalledExtension(String name) {
    final StackGresClusterInstalledExtension installedExtension =
        new StackGresClusterInstalledExtension();
    installedExtension.setName(name);
    installedExtension.setRepository("https://extensions.stackgres.io/postgres/repository");
    installedExtension.setVersion("1.0.0");
    installedExtension.setPostgresVersion(POSTGRES_MAJOR_VERSION);
    installedExtension.setBuild("1");
    return installedExtension;
  }

  private StackGresClusterExtension getExtension(String name) {
    final StackGresClusterExtension extension = new StackGresClusterExtension();
    extension.setName(name);
    extension.setVersion("1.0.0");
    return extension;
  }

}
