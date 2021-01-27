/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni.factory;

import static io.stackgres.testutil.StringUtils.getRandomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresService;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresServiceType;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresServices;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.common.ImmutableStackGresUserClusterContext;
import io.stackgres.operator.common.LabelFactoryDelegator;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class PatroniServicesTest {

  @Mock
  private LabelFactoryDelegator delegator;

  @Mock
  private LabelFactory<StackGresCluster> labelFactory;

  @Mock
  private StackGresGeneratorContext generatorContext;

  private final PatroniServices patroniServices = new PatroniServices();

  private StackGresCluster defaultCluster;

  @BeforeEach
  void setUp() {
    patroniServices.setFactoryDelegator(delegator);

    defaultCluster = JsonUtil
        .readFromJson("stackgres_cluster/default.json", StackGresCluster.class);

    lenient().when(delegator.pickFactory(any(StackGresClusterContext.class)))
        .thenAnswer((Answer<LabelFactory<StackGresCluster>>) invocationOnMock -> labelFactory);
    lenient().when(labelFactory.clusterLabels(any(StackGresCluster.class))).thenReturn(ImmutableMap.of());
    lenient().when(labelFactory.patroniPrimaryLabels(any(StackGresCluster.class)))
        .thenReturn(ImmutableMap.of());
    lenient().when(labelFactory.patroniReplicaLabels(any(StackGresCluster.class)))
        .thenReturn(ImmutableMap.of());

    when(generatorContext.getClusterContext())
        .thenReturn(buildStackgresContext(defaultCluster));

  }

  @Test
  void primaryService_shouldBeEnabledByDefault() {

    defaultCluster.getSpec().setPostgresServices(null);

    Stream<HasMetadata> services = patroniServices.streamResources(generatorContext);

    long primaryServicesCount = services
        .filter(s -> s.getKind().equals("Service"))
        .filter(s -> s.getMetadata().getName().endsWith(PatroniUtil.READ_WRITE_SERVICE))
        .count();

    assertEquals(1, primaryServicesCount);
  }

  @Test
  void replicaService_shouldBeEnabledByDefault() {

    defaultCluster.getSpec().setPostgresServices(null);

    Stream<HasMetadata> services = patroniServices.streamResources(generatorContext);

    long primaryServicesCount = services
        .filter(s -> s.getKind().equals("Service"))
        .filter(s -> s.getMetadata().getName().endsWith(PatroniUtil.READ_ONLY_SERVICE))
        .count();

    assertEquals(1, primaryServicesCount);
  }

  @Test
  void ifPrimaryServiceEnabled_shouldBeIncluded() {

    enablePrimaryService(true);

    Stream<HasMetadata> services = patroniServices.streamResources(generatorContext);

    long primaryServicesCount = services
        .filter(s -> s.getKind().equals("Service"))
        .filter(s -> s.getMetadata().getName().endsWith(PatroniUtil.READ_WRITE_SERVICE))
        .count();

    assertEquals(1, primaryServicesCount);
  }

  @Test
  void ifPrimaryServiceDisabled_shouldNotBeIncluded() {

    enablePrimaryService(false);

    Stream<HasMetadata> services = patroniServices.streamResources(generatorContext);

    long primaryServicesCount = services
        .filter(s -> s.getKind().equals("Service"))
        .filter(s -> s.getMetadata().getName().endsWith(PatroniUtil.READ_WRITE_SERVICE))
        .count();

    assertEquals(0, primaryServicesCount);
  }

  @Test
  void ifPrimaryServiceIsNotDefined_itShouldDefaultToClusterIp() {

    enablePrimaryService(true);
    defaultCluster.getSpec().getPostgresServices().getPrimary().setType(null);

    Stream<HasMetadata> services = patroniServices.streamResources(generatorContext);

    Service primaryService = getPrimaryService(services);

    assertEquals(StackGresClusterPostgresServiceType.CLUSTER_IP.type(),
        primaryService.getSpec().getType());

  }

  @Test
  void ifPrimaryServiceTypeIsLoadBalancer_serviceTypeShouldBeLoadBalancer() {

    enablePrimaryService(StackGresClusterPostgresServiceType.LOAD_BALANCER);

    Stream<HasMetadata> services = patroniServices.streamResources(generatorContext);

    Service primaryService = getPrimaryService(services);

    assertEquals(StackGresClusterPostgresServiceType.LOAD_BALANCER.type(),
        primaryService.getSpec().getType());

  }

  @Test
  void ifPrimaryServiceHasCustomAnnotations_ifShouldBeReflectedOnTheService() {

    String key = getRandomString();
    String annotation = getRandomString();
    enablePrimaryService(ImmutableMap.of(key, annotation));

    Stream<HasMetadata> services = patroniServices.streamResources(generatorContext);

    Service primaryService = getPrimaryService(services);

    final Map<String, String> annotations = primaryService.getMetadata().getAnnotations();
    assertTrue(annotations.containsKey(key));
    assertEquals(annotation, annotations.get(key));

  }

  @Test
  void ifReplicaServiceEnabled_shouldBeIncluded() {

    enableReplicaService(true);

    Stream<HasMetadata> services = patroniServices.streamResources(generatorContext);

    long replicaServicesCount = services
        .filter(s -> s.getKind().equals("Service"))
        .filter(s -> s.getMetadata().getName().endsWith(PatroniUtil.READ_ONLY_SERVICE))
        .count();

    assertEquals(1, replicaServicesCount);
  }

  @Test
  void ifReplicaServiceDisabled_shouldNotBeIncluded() {

    enableReplicaService(false);

    Stream<HasMetadata> services = patroniServices.streamResources(generatorContext);

    long ReplicaServicesCount = services
        .filter(s -> s.getKind().equals("Service"))
        .filter(s -> s.getMetadata().getName().endsWith(PatroniUtil.READ_ONLY_SERVICE))
        .count();

    assertEquals(0, ReplicaServicesCount);
  }

  @Test
  void ifReplicaServiceIsNotDefined_itShouldDefaultToClusterIp() {

    enableReplicaService(true);
    defaultCluster.getSpec().getPostgresServices().getReplicas().setType(null);

    Stream<HasMetadata> services = patroniServices.streamResources(generatorContext);

    Service ReplicaService = getReplicaService(services);

    assertEquals(StackGresClusterPostgresServiceType.CLUSTER_IP.type(),
        ReplicaService.getSpec().getType());

  }

  @Test
  void ifReplicaServiceTypeIsLoadBalancer_serviceTypeShouldBeLoadBalancer() {

    enableReplicaService(StackGresClusterPostgresServiceType.LOAD_BALANCER);

    Stream<HasMetadata> services = patroniServices.streamResources(generatorContext);

    Service ReplicaService = getReplicaService(services);

    assertEquals(StackGresClusterPostgresServiceType.LOAD_BALANCER.type(),
        ReplicaService.getSpec().getType());

  }

  @Test
  void ifReplicaServiceHasCustomAnnotations_ifShouldBeReflectedOnTheService() {

    String key = getRandomString();
    String annotation = getRandomString();
    enableReplicaService(ImmutableMap.of(key, annotation));

    Stream<HasMetadata> services = patroniServices.streamResources(generatorContext);

    Service ReplicaService = getReplicaService(services);

    final Map<String, String> annotations = ReplicaService.getMetadata().getAnnotations();
    assertTrue(annotations.containsKey(key));
    assertEquals(annotation, annotations.get(key));

  }

  private ImmutableStackGresUserClusterContext buildStackgresContext(StackGresCluster defaultCluster) {
    return ImmutableStackGresUserClusterContext.builder()
        .ownerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(defaultCluster)))
        .cluster(defaultCluster)
        .operatorContext(new OperatorPropertyContext())
        .clusterNamespace(defaultCluster.getMetadata().getNamespace())
        .clusterKey(StackGresContext.CLUSTER_KEY)
        .clusterName(defaultCluster.getMetadata().getName())
        .scheduledBackupKey(StackGresContext.SCHEDULED_BACKUP_KEY)
        .backupKey(StackGresContext.BACKUP_KEY)
        .build();
  }

  private void enablePrimaryService(boolean enabled){
    StackGresClusterPostgresServices postgresServices = new StackGresClusterPostgresServices();
    StackGresClusterPostgresService primaryService = new StackGresClusterPostgresService();
    primaryService.setEnabled(enabled);
    postgresServices.setPrimary(primaryService);
    defaultCluster.getSpec().setPostgresServices(postgresServices);

  }

  private void enableReplicaService(boolean enabled) {
    StackGresClusterPostgresServices postgresServices = new StackGresClusterPostgresServices();
    StackGresClusterPostgresService replicaService = new StackGresClusterPostgresService();
    replicaService.setEnabled(enabled);
    postgresServices.setReplicas(replicaService);
    defaultCluster.getSpec().setPostgresServices(postgresServices);
  }

  private void enablePrimaryService(StackGresClusterPostgresServiceType type){
    enablePrimaryService(true);
    final StackGresClusterPostgresService primary = defaultCluster
        .getSpec()
        .getPostgresServices()
        .getPrimary();
    primary.setType(type.type());
  }

  private void enableReplicaService(StackGresClusterPostgresServiceType type) {
    enableReplicaService(true);
    final StackGresClusterPostgresService primary = defaultCluster
        .getSpec()
        .getPostgresServices()
        .getReplicas();
    primary.setType(type.type());
  }

  private void enablePrimaryService(Map<String,String> annotations){
    enablePrimaryService(true);
    final StackGresClusterPostgresService primary = defaultCluster
        .getSpec()
        .getPostgresServices()
        .getPrimary();
    primary.setAnnotations(annotations);
  }

  private void enableReplicaService(ImmutableMap<String,String> annotations) {
    enableReplicaService(true);
    final StackGresClusterPostgresService replica = defaultCluster
        .getSpec()
        .getPostgresServices()
        .getReplicas();
    replica.setAnnotations(annotations);
  }

  private Service getPrimaryService(Stream<HasMetadata> services) {
    return services
        .filter(s -> s.getKind().equals("Service"))
        .filter(s -> s.getMetadata().getName().equals(PatroniUtil.name(defaultCluster.getMetadata().getName())))
        .map(s -> (Service) s)
        .findFirst().orElseGet(() -> fail("No postgres primary service found"));
  }

  private Service getReplicaService(Stream<HasMetadata> services) {
    return services
        .filter(s -> s.getKind().equals("Service"))
        .filter(s -> s.getMetadata().getName().endsWith(PatroniUtil.READ_ONLY_SERVICE))
        .map(s -> (Service) s)
        .findFirst().orElseGet(() -> fail("No postgres replica service found"));
  }


}