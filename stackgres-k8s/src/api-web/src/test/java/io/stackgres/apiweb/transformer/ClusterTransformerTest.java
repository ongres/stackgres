/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.apiweb.dto.cluster.ClusterCondition;
import io.stackgres.apiweb.dto.cluster.ClusterConfiguration;
import io.stackgres.apiweb.dto.cluster.ClusterDistributedLogs;
import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.apiweb.dto.cluster.ClusterExtension;
import io.stackgres.apiweb.dto.cluster.ClusterInitData;
import io.stackgres.apiweb.dto.cluster.ClusterNonProduction;
import io.stackgres.apiweb.dto.cluster.ClusterPod;
import io.stackgres.apiweb.dto.cluster.ClusterPostgres;
import io.stackgres.apiweb.dto.cluster.ClusterPostgresServices;
import io.stackgres.apiweb.dto.cluster.ClusterReplication;
import io.stackgres.apiweb.dto.cluster.ClusterReplicationGroup;
import io.stackgres.apiweb.dto.cluster.ClusterScriptEntry;
import io.stackgres.apiweb.dto.cluster.ClusterSpec;
import io.stackgres.apiweb.dto.cluster.ClusterSpecMetadata;
import io.stackgres.apiweb.dto.cluster.ClusterStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterCondition;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresServices;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplication;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicationGroup;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ClusterTransformerTest {

  @Inject
  ClusterTransformer transformer;

  public static TransformerTuple<ClusterDto, StackGresCluster> createCluster() {

    StackGresCluster source = new StackGresCluster();
    ClusterDto target = new ClusterDto();

    var metadata = TransformerTestUtil.createMetadataTuple();
    source.setMetadata(metadata.getSource());
    target.setMetadata(metadata.getTarget());

    var spec = createSpec();
    source.setSpec(spec.getSource());
    target.setSpec(spec.getTarget());

    var status = createStatus();
    source.setStatus(status.getSource());
    target.setStatus(status.getTarget());

    return new TransformerTuple<>(target, source);
  }

  private static TransformerTuple<ClusterStatus, StackGresClusterStatus> createStatus() {
    var statusTuple = TransformerTestUtil
        .fillTupleWithRandomData(ClusterStatus.class, StackGresClusterStatus.class);

    var conditions = TransformerTestUtil.generateRandomListTuple(
        ClusterCondition.class,
        StackGresClusterCondition.class
    );

    statusTuple.getSource().setConditions(conditions.getSource());
    statusTuple.getTarget().setConditions(conditions.getTarget());
    return statusTuple;
  }

  private static TransformerTuple<ClusterSpec, StackGresClusterSpec> createSpec() {
    StackGresClusterSpec source = new StackGresClusterSpec();
    ClusterSpec target = new ClusterSpec();

    var instances = TransformerTestUtil.RANDOM.nextInt(5) + 1;
    source.setInstances(instances);
    target.setInstances(instances);

    var instanceProfile = StringUtils.getRandomString(10);
    source.setResourceProfile(instanceProfile);
    target.setSgInstanceProfile(instanceProfile);

    var postgres = createPostgres();
    source.setPostgres(postgres.getSource());
    target.setPostgres(postgres.getTarget());

    var replication = createReplication();
    source.setReplication(replication.getSource());
    target.setReplication(replication.getTarget());

    var configuration = createConfiguration();
    source.setConfiguration(configuration.getSource());
    target.setConfigurations(configuration.getTarget());

    var pods = createPodsConfiguration();
    source.setPod(pods.getSource());
    target.setPods(pods.getTarget());

    var initialData = createInitialData();
    source.setInitData(initialData.getSource());
    target.setInitData(initialData.getTarget());

    var distributedLogs = createDistributedLogs();
    source.setDistributedLogs(distributedLogs.getSource());
    target.setDistributedLogs(distributedLogs.getTarget());

    var prometheusAutobind = TransformerTestUtil.RANDOM.nextBoolean();
    source.setPrometheusAutobind(prometheusAutobind);
    target.setPrometheusAutobind(prometheusAutobind);

    var nonProductions = createNonProductionOptions();
    source.setNonProductionOptions(nonProductions.getSource());
    target.setNonProductionOptions(nonProductions.getTarget());

    var services = createPostgresServices();
    source.setPostgresServices(services.getSource());
    target.setPostgresServices(services.getTarget());

    var metadata = createClusterMetadata();
    source.setMetadata(metadata.getSource());
    target.setMetadata(metadata.getTarget());

    return new TransformerTuple<>(target, source);
  }

  private static TransformerTuple<ClusterPostgres, StackGresClusterPostgres> createPostgres() {

    TransformerTuple<ClusterPostgres, StackGresClusterPostgres> tuple = TransformerTestUtil
        .fillTupleWithRandomData(
            ClusterPostgres.class,
            StackGresClusterPostgres.class
        );

    TransformerTuple<List<ClusterExtension>, List<StackGresClusterExtension>> extensionTuple =
        TransformerTestUtil.generateRandomListTuple(
            ClusterExtension.class,
            StackGresClusterExtension.class
        );

    tuple.getTarget().setExtensions(extensionTuple.getTarget());
    tuple.getSource().setExtensions(extensionTuple.getSource());

    return tuple;
  }

  private static TransformerTuple<ClusterReplication, StackGresClusterReplication>
      createReplication() {

    TransformerTuple<ClusterReplication, StackGresClusterReplication> tuple = TransformerTestUtil
        .fillTupleWithRandomData(
            ClusterReplication.class,
            StackGresClusterReplication.class
        );

    TransformerTuple<List<ClusterReplicationGroup>, List<StackGresClusterReplicationGroup>>
        replicationGroupsTuple =
        TransformerTestUtil.generateRandomListTuple(
            ClusterReplicationGroup.class,
            StackGresClusterReplicationGroup.class
        );

    tuple.getTarget().setGroups(replicationGroupsTuple.getTarget());
    tuple.getSource().setGroups(replicationGroupsTuple.getSource());

    return tuple;
  }

  private static TransformerTuple<
      ClusterConfiguration, StackGresClusterConfiguration> createConfiguration() {
    return TransformerTestUtil.fillTupleWithRandomData(ClusterConfiguration.class,
        StackGresClusterConfiguration.class);
  }

  private static TransformerTuple<ClusterPod, StackGresClusterPod> createPodsConfiguration() {
    return TransformerTestUtil
        .fillTupleWithRandomData(ClusterPod.class, StackGresClusterPod.class);
  }

  private static TransformerTuple<ClusterInitData, StackGresClusterInitData> createInitialData() {

    var initialData = TransformerTestUtil
        .fillTupleWithRandomData(ClusterInitData.class, StackGresClusterInitData.class);
    var scripts = TransformerTestUtil.generateRandomListTuple(
        ClusterScriptEntry.class, StackGresClusterScriptEntry.class
    );
    initialData.getTarget().setScripts(scripts.getTarget());
    initialData.getSource().setScripts(scripts.getSource());
    return initialData;

  }

  private static TransformerTuple<
      ClusterDistributedLogs, StackGresClusterDistributedLogs> createDistributedLogs() {
    return TransformerTestUtil.fillTupleWithRandomData(
        ClusterDistributedLogs.class, StackGresClusterDistributedLogs.class
    );
  }

  private static TransformerTuple<ClusterNonProduction,
      StackGresClusterNonProduction> createNonProductionOptions() {
    var nonProduction = TransformerTestUtil.fillTupleWithRandomData(
        ClusterNonProduction.class,
        StackGresClusterNonProduction.class
    );

    var featureGates = TransformerTestUtil.generateRandomListTuple();
    nonProduction.getSource().setEnabledFeatureGates(featureGates.getSource());
    nonProduction.getTarget().setEnabledFeatureGates(featureGates.getTarget());

    return nonProduction;
  }

  private static TransformerTuple<
      ClusterPostgresServices, StackGresClusterPostgresServices> createPostgresServices() {
    return TransformerTestUtil.fillTupleWithRandomData(ClusterPostgresServices.class,
        StackGresClusterPostgresServices.class);
  }

  private static TransformerTuple<
      ClusterSpecMetadata, StackGresClusterSpecMetadata> createClusterMetadata() {
    return TransformerTestUtil.fillTupleWithRandomData(
        ClusterSpecMetadata.class, StackGresClusterSpecMetadata.class);
  }

  @Test
  void testClusterTransformation() {

    var tuple = createCluster();
    TransformerTestUtil.assertTransformation(transformer, tuple);

  }
}
