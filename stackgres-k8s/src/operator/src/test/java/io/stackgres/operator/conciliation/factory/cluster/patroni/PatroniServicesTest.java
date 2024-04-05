/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StringUtil;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServiceNodePort;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServiceType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresService;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresServices;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForCluster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatroniServicesTest {

  @Mock
  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Mock
  private io.stackgres.operator.conciliation.cluster.StackGresClusterContext context;

  private PatroniServices patroniServices;

  private StackGresCluster defaultCluster;

  @BeforeEach
  void setUp() {
    patroniServices = new PatroniServices(labelFactory);

    defaultCluster = Fixtures.cluster().loadDefault().get();

    when(context.getSource())
        .thenReturn(defaultCluster);

    when(context.getCluster())
        .thenReturn(defaultCluster);

    lenient().when(labelFactory.genericLabels(any(StackGresCluster.class)))
        .thenReturn(ImmutableMap.of());
    lenient().when(labelFactory.clusterPrimaryLabels(any(StackGresCluster.class)))
        .thenReturn(ImmutableMap.of());
    lenient().when(labelFactory.clusterReplicaLabels(any(StackGresCluster.class)))
        .thenReturn(ImmutableMap.of());

    when(context.getSource())
        .thenReturn(defaultCluster);

    when(context.getCluster())
        .thenReturn(defaultCluster);
  }

  @Test
  void givenPrimaryServiceEnabled_shouldBeIncluded() {
    enablePrimaryService(true);

    Stream<HasMetadata> services = patroniServices.generateResource(context);

    long primaryServicesCount = services
        .filter(s -> s.getKind().equals("Service"))
        .filter(s -> s.getMetadata().getName().endsWith(PatroniUtil.DEPRECATED_READ_WRITE_SERVICE))
        .count();

    assertEquals(1, primaryServicesCount);
  }

  @Test
  void givenPrimaryServiceDisabled_shouldNotBeIncluded() {
    enablePrimaryService(false);

    Stream<HasMetadata> services = patroniServices.generateResource(context);

    long primaryServicesCount = services
        .filter(s -> s.getKind().equals("Service"))
        .filter(s -> s.getMetadata().getName().endsWith(PatroniUtil.DEPRECATED_READ_WRITE_SERVICE))
        .count();

    assertEquals(0, primaryServicesCount);
  }

  @Test
  void givenDeprecatedPrimaryServiceIsNotDefined_itShouldDefaultToExternalName() {
    enablePrimaryService(true);
    defaultCluster.getSpec().getPostgresServices().getPrimary().setType(null);

    Stream<HasMetadata> services = patroniServices.generateResource(context);

    Service primaryService = getDeprecatedPrimaryService(services);

    assertEquals("ExternalName",
        primaryService.getSpec().getType());
  }

  @Test
  void givenPrimaryServiceTypeIsLoadBalancer_serviceTypeShouldBeLoadBalancer() {
    enablePrimaryService(StackGresPostgresServiceType.LOAD_BALANCER);

    Stream<HasMetadata> services = patroniServices.generateResource(context);

    Service primaryService = getPrimaryService(services);

    assertEquals(StackGresPostgresServiceType.LOAD_BALANCER.toString(),
        primaryService.getSpec().getType());
  }

  @Test
  void givenPrimaryServiceHasCustomAnnotations_shouldBeReflectedOnTheService() {

    String key = StringUtil.generateRandom();
    String annotation = StringUtil.generateRandom();
    enablePrimaryService(ImmutableMap.of(key, annotation));

    Stream<HasMetadata> services = patroniServices.generateResource(context);

    Service primaryService = getPrimaryService(services);

    final Map<String, String> annotations = primaryService.getMetadata().getAnnotations();
    assertTrue(annotations.containsKey(key));
    assertEquals(annotation, annotations.get(key));

  }

  @Test
  void givenPrimaryServiceHasNodePorts_shouldBeIncluded() {
    enablePrimaryServiceNodePorts();
    final Stream<HasMetadata> services = patroniServices.generateResource(context);

    final Service primaryService = getPrimaryService(services);

    final List<Integer> availableNodePorts = primaryService.getSpec()
            .getPorts()
            .stream()
            .map(ServicePort::getNodePort)
            .toList();

    assertEquals(List.of(30432, 30433), availableNodePorts);
  }

  @Test
  void givenReplicaServiceHasNodePorts_shouldBeIncluded() {
    enableReplicasNodePorts();
    final Stream<HasMetadata> services = patroniServices.generateResource(context);

    final Service replicaService = getReplicaService(services);

    final List<Integer> availableNodePorts = replicaService.getSpec()
            .getPorts()
            .stream()
            .map(ServicePort::getNodePort)
            .toList();

    assertEquals(List.of(30432, 30433), availableNodePorts);
  }

  @Test
  void givenReplicaServiceEnabled_shouldBeIncluded() {
    resetReplicas(true);

    Stream<HasMetadata> services = patroniServices.generateResource(context);

    long replicaServicesCount = services
        .filter(s -> s.getKind().equals("Service"))
        .filter(s -> s.getMetadata().getName().endsWith(PatroniUtil.READ_ONLY_SERVICE))
        .count();

    assertEquals(1, replicaServicesCount);
  }

  @Test
  void givenReplicaServiceDisabled_shouldNotBeIncluded() {
    resetReplicas(false);

    Stream<HasMetadata> services = patroniServices.generateResource(context);

    long replicaServicesCount = services
        .filter(s -> s.getKind().equals("Service"))
        .filter(s -> s.getMetadata().getName().endsWith(PatroniUtil.READ_ONLY_SERVICE))
        .count();

    assertEquals(0, replicaServicesCount);
  }

  @Test
  void givenReplicaServiceTypeIsLoadBalancer_serviceTypeShouldBeLoadBalancer() {
    enableReplicasAndSetServiceTypeAs(StackGresPostgresServiceType.LOAD_BALANCER);
    Stream<HasMetadata> services = patroniServices.generateResource(context);

    Service replicaService = getReplicaService(services);

    assertEquals(StackGresPostgresServiceType.LOAD_BALANCER.toString(),
        replicaService.getSpec().getType());
  }

  @Test
  void onceReplicaServiceHasExternalIPs_serviceShouldHasExternalIPs() {
    Stream<HasMetadata> services = patroniServices.generateResource(context);

    Service replicaService = getReplicaService(services);

    assertEquals(replicasExternalIP(),
        replicaService.getSpec().getExternalIPs().stream().findFirst().get());
  }

  @Test
  void givenPrimaryServiceHasExternalIPs_primaryShouldHasExternalIPs() {
    Stream<HasMetadata> services = patroniServices.generateResource(context);

    Service patroniService = getPrimaryService(services);

    assertEquals(replicasExternalIP(),
        patroniService.getSpec().getExternalIPs().stream().findFirst().get());
  }

  private String replicasExternalIP() {
    return defaultCluster.getSpec().getPostgresServices().getReplicas().getExternalIPs().stream()
        .findFirst().get();
  }

  @Test
  void givenReplicaServiceHasCustomAnnotations_shouldBeReflectedOnTheService() {
    String key = StringUtil.generateRandom();
    String annotation = StringUtil.generateRandom();
    enableReplicasAndSetAnnotations(ImmutableMap.of(key, annotation));

    Stream<HasMetadata> services = patroniServices.generateResource(context);

    Service replicaService = getReplicaService(services);

    final Map<String, String> annotations = replicaService.getMetadata().getAnnotations();
    assertTrue(annotations.containsKey(key));
    assertEquals(annotation, annotations.get(key));
  }

  private void enablePrimaryService(boolean enabled) {
    StackGresClusterPostgresServices postgresServices = new StackGresClusterPostgresServices();
    StackGresClusterPostgresService primaryService = new StackGresClusterPostgresService();
    StackGresClusterPostgresService replicasService = new StackGresClusterPostgresService();
    primaryService.setEnabled(enabled);
    primaryService.setType("ClusterIP");
    postgresServices.setPrimary(primaryService);
    postgresServices.setReplicas(replicasService);
    defaultCluster.getSpec().setPostgresServices(postgresServices);
  }

  private void enablePrimaryService(StackGresPostgresServiceType type) {
    enablePrimaryService(true);
    final StackGresPostgresService primary = defaultCluster
        .getSpec()
        .getPostgresServices()
        .getPrimary();
    primary.setType(type.toString());
  }

  private void enablePrimaryService(Map<String, String> annotations) {
    enablePrimaryService(true);
    if (defaultCluster.getSpec().getMetadata() == null) {
      defaultCluster.getSpec().setMetadata(new StackGresClusterSpecMetadata());
    }
    if (defaultCluster.getSpec().getMetadata().getAnnotations() == null) {
      defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    }
    defaultCluster.getSpec().getMetadata().getAnnotations().setPrimaryService(annotations);
  }

  private void enablePrimaryServiceNodePorts() {
    enablePrimaryService(true);
    final StackGresPostgresService primary = defaultCluster
            .getSpec()
            .getPostgresServices()
            .getPrimary();

    final StackGresPostgresServiceNodePort nodePorts = new StackGresPostgresServiceNodePort();
    nodePorts.setPgport(30432);
    nodePorts.setReplicationport(30433);
    nodePorts.setBabelfish(30434);

    primary.setNodePorts(nodePorts);
  }

  private void resetReplicas(boolean enabled) {
    StackGresClusterPostgresServices postgresServices = new StackGresClusterPostgresServices();
    StackGresClusterPostgresService primaryService = new StackGresClusterPostgresService();
    StackGresClusterPostgresService replicaService = new StackGresClusterPostgresService();
    replicaService.setEnabled(enabled);
    replicaService.setType("ClusterIP");
    postgresServices.setPrimary(primaryService);
    postgresServices.setReplicas(replicaService);
    defaultCluster.getSpec().setPostgresServices(postgresServices);
  }

  private void enableReplicasAndSetServiceTypeAs(StackGresPostgresServiceType serviceType) {
    resetReplicas(true);
    final StackGresPostgresService replicas = defaultCluster
        .getSpec()
        .getPostgresServices()
        .getReplicas();
    replicas.setType(serviceType.toString());
  }

  private void enableReplicasAndSetAnnotations(Map<String, String> annotations) {
    resetReplicas(true);
    if (defaultCluster.getSpec().getMetadata() == null) {
      defaultCluster.getSpec().setMetadata(new StackGresClusterSpecMetadata());
    }
    if (defaultCluster.getSpec().getMetadata().getAnnotations() == null) {
      defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    }
    defaultCluster.getSpec().getMetadata().getAnnotations().setReplicasService(annotations);
  }

  private void enableReplicasNodePorts() {
    resetReplicas(true);
    final StackGresPostgresServiceNodePort nodePorts = new StackGresPostgresServiceNodePort();
    nodePorts.setPgport(30432);
    nodePorts.setReplicationport(30433);
    nodePorts.setBabelfish(30434);

    defaultCluster.getSpec().getPostgresServices().getReplicas().setNodePorts(nodePorts);
  }

  private Service getPrimaryService(Stream<HasMetadata> services) {
    return services
        .filter(Service.class::isInstance)
        .filter(s -> s.getMetadata().getName().equals(defaultCluster.getMetadata().getName()))
        .map(Service.class::cast)
        .findFirst().orElseGet(() -> fail("No postgres primary service found"));
  }

  private Service getDeprecatedPrimaryService(Stream<HasMetadata> services) {
    return services
        .filter(Service.class::isInstance)
        .filter(s -> s.getMetadata().getName()
            .equals(PatroniUtil.deprecatedReadWriteName(defaultCluster.getMetadata().getName())))
        .map(Service.class::cast)
        .findFirst().orElseGet(() -> fail("No postgres primary service found"));
  }

  private Service getReplicaService(Stream<HasMetadata> services) {
    return services
        .filter(Service.class::isInstance)
        .filter(s -> s.getMetadata().getName()
            .equals(PatroniUtil.readOnlyName(defaultCluster.getMetadata().getName())))
        .map(Service.class::cast)
        .findFirst().orElseGet(() -> fail("No postgres replica service found"));
  }

}
