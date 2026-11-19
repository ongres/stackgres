/*
 * Copyright (C) 2026 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.matriarch.clustername;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the pure, best-effort parsers of the Phase 1 cluster-name detection chain. The
 * network/IMDS and Kubernetes-API paths are not covered here (they need a cluster or metadata
 * service); this pins the string-parsing that produces wrong keys if it drifts.
 */
class ClusterNameParsersTest {

  @Test
  void kindProviderIdYieldsClusterName() {
    assertEquals(Optional.of("foo"),
        ClusterNameDetector.parseKindProviderId("kind://docker/foo/foo-control-plane"));
    assertEquals(Optional.of("node-prov"),
        ClusterNameDetector.parseKindProviderId("kind://podman/node-prov/node-prov-control-plane"));
  }

  @Test
  void nonKindProviderIdIsIgnored() {
    assertTrue(ClusterNameDetector.parseKindProviderId(
        "aws:///us-east-1a/i-0abc123").isEmpty());
    assertTrue(ClusterNameDetector.parseKindProviderId(
        "gce://my-project/us-central1-a/gke-node").isEmpty());
    assertTrue(ClusterNameDetector.parseKindProviderId(null).isEmpty());
    assertTrue(ClusterNameDetector.parseKindProviderId("kind://").isEmpty());
  }

  @Test
  void stripsControlPlaneSuffix() {
    assertEquals("foo", ClusterNameDetector.stripControlPlaneSuffix("foo-control-plane"));
    assertEquals("foo", ClusterNameDetector.stripControlPlaneSuffix("foo-master"));
    assertEquals("minikube", ClusterNameDetector.stripControlPlaneSuffix("minikube"));
  }

  @Test
  void aksNodeResourceGroupYieldsClusterName() {
    assertEquals(Optional.of("mycluster"),
        AzureClusterNameProbe.parseClusterNameFromNodeResourceGroup(
            "MC_myrg_mycluster_westeurope"));
  }

  @Test
  void nonAksResourceGroupIsIgnored() {
    assertTrue(AzureClusterNameProbe.parseClusterNameFromNodeResourceGroup(
        "my-plain-rg").isEmpty());
    assertTrue(AzureClusterNameProbe.parseClusterNameFromNodeResourceGroup(null).isEmpty());
    assertTrue(AzureClusterNameProbe.parseClusterNameFromNodeResourceGroup("MC_rg").isEmpty());
  }

  @Test
  void eksBootstrapUserDataYieldsClusterName() {
    String userData = """
        #!/bin/bash
        set -o xtrace
        /etc/eks/bootstrap.sh my-eks-cluster --kubelet-extra-args '--node-labels=x=y'
        """;
    assertEquals(Optional.of("my-eks-cluster"),
        AwsClusterNameProbe.parseClusterNameFromUserData(userData));
  }

  @Test
  void eksNodeadmUserDataYieldsClusterName() {
    String userData = """
        apiVersion: node.eks.aws/v1alpha1
        kind: NodeConfig
        spec:
          cluster:
            name: al2023-cluster
            apiServerEndpoint: https://example.eks.amazonaws.com
        """;
    assertEquals(Optional.of("al2023-cluster"),
        AwsClusterNameProbe.parseClusterNameFromUserData(userData));
  }

  @Test
  void emptyUserDataIsIgnored() {
    assertTrue(AwsClusterNameProbe.parseClusterNameFromUserData(null).isEmpty());
    assertTrue(AwsClusterNameProbe.parseClusterNameFromUserData("   ").isEmpty());
    assertTrue(AwsClusterNameProbe.parseClusterNameFromUserData(
        "#!/bin/bash\necho hello\n").isEmpty());
  }

}
