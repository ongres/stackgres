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

import java.util.Map;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StringUtil;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServiceType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresServices;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.testutil.JsonUtil;
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

  private final PatroniServices patroniServices = new PatroniServices();

  private StackGresCluster defaultCluster;

  private StackGresCluster loadBalancerIPCluster;

  @BeforeEach
  void setUp() {
    patroniServices.setLabelFactory(labelFactory);

    defaultCluster = JsonUtil
        .readFromJson("stackgres_cluster/default.json", StackGresCluster.class);

    loadBalancerIPCluster = JsonUtil
        .readFromJson("stackgres_cluster/using_load_balancer_ip.json", StackGresCluster.class);

    when(context.getSource())
        .thenReturn(defaultCluster);

    when(context.getCluster())
        .thenReturn(defaultCluster);

    lenient().when(labelFactory.genericLabels(any(StackGresCluster.class)))
        .thenReturn(ImmutableMap.of());
    lenient().when(labelFactory.patroniPrimaryLabels(any(StackGresCluster.class)))
        .thenReturn(ImmutableMap.of());
    lenient().when(labelFactory.patroniReplicaLabels(any(StackGresCluster.class)))
        .thenReturn(ImmutableMap.of());

    when(context.getSource())
        .thenReturn(defaultCluster);

    when(context.getCluster())
        .thenReturn(defaultCluster);
  }

  @Test
  void primaryService_shouldBeEnabledByDefault() {
    defaultCluster.getSpec().setPostgresServices(null);

    Stream<HasMetadata> services = patroniServices.generateResource(context);

    long primaryServicesCount = services
        .filter(s -> s.getKind().equals("Service"))
        .filter(s -> s.getMetadata().getName().endsWith(PatroniUtil.READ_WRITE_SERVICE))
        .count();

    assertEquals(1, primaryServicesCount);
  }

  @Test
  void replicaService_shouldBeEnabledByDefault() {
    defaultCluster.getSpec().setPostgresServices(null);

    Stream<HasMetadata> services = patroniServices.generateResource(context);

    long primaryServicesCount = services
        .filter(s -> s.getKind().equals("Service"))
        .filter(s -> s.getMetadata().getName().endsWith(PatroniUtil.READ_ONLY_SERVICE))
        .count();

    assertEquals(1, primaryServicesCount);
  }

  @Test
  void givenPrimaryServiceEnabled_shouldBeIncluded() {
    enablePrimaryService(true);

    Stream<HasMetadata> services = patroniServices.generateResource(context);

    long primaryServicesCount = services
        .filter(s -> s.getKind().equals("Service"))
        .filter(s -> s.getMetadata().getName().endsWith(PatroniUtil.READ_WRITE_SERVICE))
        .count();

    assertEquals(1, primaryServicesCount);
  }

  @Test
  void givenPrimaryServiceDisabled_shouldNotBeIncluded() {
    enablePrimaryService(false);

    Stream<HasMetadata> services = patroniServices.generateResource(context);

    long primaryServicesCount = services
        .filter(s -> s.getKind().equals("Service"))
        .filter(s -> s.getMetadata().getName().endsWith(PatroniUtil.READ_WRITE_SERVICE))
        .count();

    assertEquals(0, primaryServicesCount);
  }

  @Test
  void givenPrimaryServiceIsNotDefined_itShouldDefaultToExternalName() {
    enablePrimaryService(true);
    defaultCluster.getSpec().getPostgresServices().getPrimary().setType(null);

    Stream<HasMetadata> services = patroniServices.generateResource(context);

    Service primaryService = getPrimaryService(services);

    assertEquals(StackGresPostgresServiceType.EXTERNAL_NAME.toString(),
        primaryService.getSpec().getType());
  }

  @Test
  void givenPatroniyServiceIsNotDefined_itShouldDefaultToClusterIp() {
    enablePrimaryService(true);
    defaultCluster.getSpec().getPostgresServices().getPrimary().setType(null);

    Stream<HasMetadata> services = patroniServices.generateResource(context);

    Service primaryService = getPatroniService(services);

    assertEquals(StackGresPostgresServiceType.CLUSTER_IP.toString(),
        primaryService.getSpec().getType());
  }

  @Test
  void givenPatroniServiceTypeIsLoadBalancer_serviceTypeShouldBeLoadBalancer() {
    enablePrimaryService(StackGresPostgresServiceType.LOAD_BALANCER);

    Stream<HasMetadata> services = patroniServices.generateResource(context);

    Service primaryService = getPatroniService(services);

    assertEquals(StackGresPostgresServiceType.LOAD_BALANCER.toString(),
        primaryService.getSpec().getType());
  }

  @Test
  void givenPatroniServiceHasCustomAnnotations_shouldBeReflectedOnTheService() {

    String key = StringUtil.generateRandom();
    String annotation = StringUtil.generateRandom();
    enablePrimaryService(ImmutableMap.of(key, annotation));

    Stream<HasMetadata> services = patroniServices.generateResource(context);

    Service primaryService = getPatroniService(services);

    final Map<String, String> annotations = primaryService.getMetadata().getAnnotations();
    assertTrue(annotations.containsKey(key));
    assertEquals(annotation, annotations.get(key));

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
  void givenReplicaServiceIsNotDefined_itShouldDefaultToClusterIp() {
    resetReplicas(true);
    defaultCluster.getSpec().getPostgresServices().getReplicas().setType(null);

    Stream<HasMetadata> services = patroniServices.generateResource(context);

    Service replicaService = getReplicaService(services);

    assertEquals(StackGresPostgresServiceType.CLUSTER_IP.toString(),
        replicaService.getSpec().getType());
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
  void onceSGPrimaryServiceHasLoadBalancerIP_PrimaryServiceShouldUseProperIP() {
    when(context.getSource())
        .thenReturn(loadBalancerIPCluster);
    when(context.getCluster())
        .thenReturn(loadBalancerIPCluster);

    Stream<HasMetadata> services = patroniServices.generateResource(context);
    Service primaryService = getPrimaryService(services);
    assertEquals("13.33.108.129",
        primaryService.getSpec().getLoadBalancerIP());
  }

  @Test
  void onceReplicaServiceHasExternalIPs_serviceShouldHasExternalIPs() {
    Stream<HasMetadata> services = patroniServices.generateResource(context);

    Service replicaService = getReplicaService(services);

    assertEquals(replicasExternalIP(),
        replicaService.getSpec().getExternalIPs().stream().findFirst().get());
  }

  @Test
  void givenPrimaryServiceHasExternalIPs_patroniShouldHasExternalIPs() {
    Stream<HasMetadata> services = patroniServices.generateResource(context);

    Service patroniService = getPatroniService(services);

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
    StackGresPostgresService primaryService = new StackGresPostgresService();
    primaryService.setEnabled(enabled);
    postgresServices.setPrimary(primaryService);
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

  private void resetReplicas(boolean enabled) {
    StackGresClusterPostgresServices postgresServices = new StackGresClusterPostgresServices();
    StackGresPostgresService replicaService = new StackGresPostgresService();
    replicaService.setEnabled(enabled);
    postgresServices.setReplicas(replicaService);
    defaultCluster.getSpec().setPostgresServices(postgresServices);
  }

  private void enableReplicasAndSetServiceTypeAs(StackGresPostgresServiceType serviceType) {
    resetReplicas(true);
    final StackGresPostgresService primary = defaultCluster
        .getSpec()
        .getPostgresServices()
        .getReplicas();
    primary.setType(serviceType.toString());
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

  private Service getPatroniService(Stream<HasMetadata> services) {
    return services
        .filter(Service.class::isInstance)
        .filter(s -> s.getMetadata().getName().equals(defaultCluster.getMetadata().getName()))
        .map(Service.class::cast)
        .findFirst().orElseGet(() -> fail("No postgres primary service found"));
  }

  private Service getPrimaryService(Stream<HasMetadata> services) {
    return services
        .filter(Service.class::isInstance)
        .filter(s -> s.getMetadata().getName()
            .equals(PatroniUtil.readWriteName(defaultCluster.getMetadata().getName())))
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
