/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceList;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.AnyNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.stackgres.common.CrdLoader;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class CrdInstallerTest {

  @Mock
  private ResourceFinder<CustomResourceDefinition> customResourceDefinitionFinder;

  @Mock
  private ResourceWriter<CustomResourceDefinition> customResourceDefinitionWriter;

  @Mock
  private KubernetesClient client;

  @SuppressWarnings("rawtypes")
  @Mock
  private MixedOperation operation;

  @SuppressWarnings("rawtypes")
  @Mock
  private AnyNamespaceOperation anyNamespaceOperation;

  @Mock
  private GenericKubernetesResourceList operationList;

  @SuppressWarnings("rawtypes")
  @Mock
  private MixedOperation emptyOperation;

  @SuppressWarnings("rawtypes")
  @Mock
  private AnyNamespaceOperation emptyAnyNamespaceOperation;

  @Mock
  private GenericKubernetesResourceList emptyOperationList;

  private final CrdLoader crdLoader = new CrdLoader(new YamlMapperProvider().get());

  private CrdInstaller crdInstaller;

  @BeforeEach
  void setUp() {
    crdInstaller = new CrdInstaller(
        customResourceDefinitionFinder,
        customResourceDefinitionWriter,
        new YamlMapperProvider(),
        client);
  }

  @Test
  void checkUpgrade_shouldPassIfAllClusterNotTooOldVersionAreFound() {
    when(customResourceDefinitionFinder.findByName(any()))
        .thenAnswer(invocation -> Optional
            .of(crdLoader.scanCrds().stream().filter(crd -> crd.getMetadata().getName()
                .equals((String) invocation.getArguments()[0]))
                .findFirst().orElseThrow()));
    when(client.genericKubernetesResources(any())).thenAnswer(invocation -> {
      CustomResourceDefinitionContext context = invocation.getArgument(0);
      if (context.getKind().equals(HasMetadata.getKind(StackGresCluster.class))) {
        return operation;
      }
      return emptyOperation;
    });
    when(emptyOperation.inAnyNamespace()).thenReturn(emptyAnyNamespaceOperation);
    when(emptyAnyNamespaceOperation.list()).thenReturn(emptyOperationList);
    when(operation.inAnyNamespace()).thenReturn(anyNamespaceOperation);
    when(anyNamespaceOperation.list()).thenReturn(operationList);
    when(operation.inAnyNamespace()).thenReturn(anyNamespaceOperation);
    when(anyNamespaceOperation.list()).thenReturn(operationList);
    when(operationList.getItems()).thenReturn(List.of(
        new GenericKubernetesResourceBuilder()
        .withKind("SGCluster")
        .withNewMetadata()
        .withNamespace("test")
        .withName("test")
        .withAnnotations(Map.of(StackGresContext.VERSION_KEY, StackGresVersion.LATEST.getVersion()))
        .withAnnotations(Map.of(StackGresContext.VERSION_KEY, StackGresVersion.OLDEST.getVersion()))
        .endMetadata()
        .build()));

    crdInstaller.checkUpgrade();
  }

  @Test
  void checkUpgrade_shouldFailIfAClusterWithTooOldVersionIsFound() {
    when(customResourceDefinitionFinder.findByName(any()))
        .thenAnswer(invocation -> Optional
            .of(crdLoader.scanCrds().stream().filter(crd -> crd.getMetadata().getName()
                .equals((String) invocation.getArguments()[0]))
                .findFirst().orElseThrow()));
    when(client.genericKubernetesResources(any())).thenAnswer(invocation -> {
      CustomResourceDefinitionContext context = invocation.getArgument(0);
      if (context.getKind().equals(HasMetadata.getKind(StackGresCluster.class))) {
        return operation;
      }
      return emptyOperation;
    });
    when(emptyOperation.inAnyNamespace()).thenReturn(emptyAnyNamespaceOperation);
    when(emptyAnyNamespaceOperation.list()).thenReturn(emptyOperationList);
    when(operation.inAnyNamespace()).thenReturn(anyNamespaceOperation);
    when(anyNamespaceOperation.list()).thenReturn(operationList);
    when(operationList.getItems()).thenReturn(List.of(
        new GenericKubernetesResourceBuilder()
        .withKind("SGCluster")
        .withNewMetadata()
        .withNamespace("test")
        .withName("test")
        .withAnnotations(Map.of(StackGresContext.VERSION_KEY, "0.9"))
        .endMetadata()
        .build()));

    var exception = Assertions.assertThrows(
        RuntimeException.class, () -> crdInstaller.checkUpgrade());
    Assertions.assertEquals(
        "Can not upgrade due to some resources still at version"
          + " older than \"" + StackGresVersion.OLDEST.getVersion() + "\"."
          + " Please, downgrade to a previous version of the operator and run a SGDbOps of"
          + " type securityUpgrade on all the SGClusters (that are not part of an SGShardedCluster),"
          + " a SGShardedDbOps of type securityUpgrade on all the SGShardedClusters and perform the"
          + " upgrade procedure as explained in https://stackgres.io/doc/latest/administration/distributed-logs/upgrade/"
          + " of the following list:\n"
          + "SGCluster test.test: version at 0.9",
        exception.getMessage());
  }

  @Test
  void installCrd_shouldInstallTheResourceIfDoesNotExists() {
    CustomResourceDefinition definition = crdLoader.scanCrds().get(0);
    when(customResourceDefinitionFinder.findByName(definition.getMetadata().getName()))
        .thenReturn(Optional.empty());

    when(customResourceDefinitionWriter.create(any(CustomResourceDefinition.class)))
        .thenReturn(definition);

    crdInstaller.installCrd(definition);

    verify(customResourceDefinitionFinder).findByName(definition.getMetadata().getName());
    verify(customResourceDefinitionWriter).create(any(CustomResourceDefinition.class));
  }

  @Test
  void installCrd_shouldPatchTheResourceIfExists() {
    CustomResourceDefinition definition = crdLoader.scanCrds().get(0);
    when(customResourceDefinitionFinder.findByName(definition.getMetadata().getName()))
        .thenAnswer((Answer<Optional<CustomResourceDefinition>>) invocation -> Optional
            .of(crdLoader.getCrd(definition.getSpec().getNames().getKind())));

    when(customResourceDefinitionWriter.update(any(CustomResourceDefinition.class),
        Mockito.<Consumer<CustomResourceDefinition>>any()))
        .thenReturn(definition);

    crdInstaller.installCrd(definition);

    verify(customResourceDefinitionFinder).findByName(definition.getMetadata().getName());
    verify(customResourceDefinitionWriter).update(any(CustomResourceDefinition.class),
        Mockito.<Consumer<CustomResourceDefinition>>any());
  }

}
