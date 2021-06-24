package io.stackgres.jobs.dbops;

import static io.stackgres.jobs.dbops.clusterrestart.PodTestUtil.assertPodEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.smallrye.mutiny.Multi;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.ClusterDbOpsRestartStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.DbOpsRestartStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.jobs.dbops.clusterrestart.ClusterRestartImpl;
import io.stackgres.jobs.dbops.clusterrestart.ClusterRestartState;
import io.stackgres.jobs.dbops.clusterrestart.ImmutableClusterRestartState;
import io.stackgres.jobs.dbops.clusterrestart.ImmutableRestartEvent;
import io.stackgres.jobs.dbops.clusterrestart.InvalidCluster;
import io.stackgres.jobs.dbops.clusterrestart.PodTestUtil;
import io.stackgres.jobs.dbops.clusterrestart.RestartEventType;
import io.stackgres.jobs.dbops.lock.MockKubeDb;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WithKubernetesTestServer
public abstract class ClusterStateHandlerTest {

  @InjectMock
  public ClusterRestartImpl clusterRestart;

  @Inject
  public PodTestUtil podTestUtil;

  @Inject
  public MockKubeDb kubeDb;

  public String namespace = StringUtils.getRandomNamespace();

  public String clusterName = StringUtils.getRandomClusterName();

  public StackGresDbOps securityUpgradeOp;

  public StackGresCluster cluster;

  protected static void assertEqualsRestartState(ClusterRestartState expected,
                                               ClusterRestartState actual) {
    assertEquals(expected.getClusterName(), actual.getClusterName());
    assertEquals(expected.getNamespace(), actual.getNamespace());

    assertPodEquals(expected.getPrimaryInstance(), actual.getPrimaryInstance());

    var expectedInitialInstances = expected.getInitialInstances()
        .stream().sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .collect(Collectors.toUnmodifiableList());
    var actualInitialInstances = actual.getInitialInstances().stream()
        .sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .collect(Collectors.toUnmodifiableList());

    Seq.zip(expectedInitialInstances, actualInitialInstances)
        .forEach(tuple -> assertPodEquals(tuple.v1, tuple.v2));

    final List<Pod> restartedInstances = expected.getRestartedInstances()
        .stream().sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .collect(Collectors.toUnmodifiableList());

    final List<Pod> actualRestartedInstances = actual.getRestartedInstances()
        .stream().sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .collect(Collectors.toUnmodifiableList());

    Seq.zip(restartedInstances, actualRestartedInstances)
        .forEach(tuple -> assertPodEquals(tuple.v1, tuple.v2));

    final List<Pod> expectedTotalInstances = expected.getTotalInstances()
        .stream().sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .collect(Collectors.toUnmodifiableList());
    final List<Pod> actualTotalInstances = actual.getTotalInstances()
        .stream().sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .collect(Collectors.toUnmodifiableList());

    Seq.zip(expectedTotalInstances, actualTotalInstances)
        .forEach(tuple -> assertPodEquals(tuple.v1, tuple.v2));
  }

  @BeforeEach
  void setUp() {
    namespace = StringUtils.getRandomNamespace();
    clusterName = StringUtils.getRandomClusterName();

    securityUpgradeOp = JsonUtil.readFromJson("stackgres_dbops/dbops_securityupgrade.json",
        StackGresDbOps.class);

    cluster = JsonUtil.readFromJson("stackgres_cluster/default.json",
        StackGresCluster.class);

    securityUpgradeOp.getMetadata().setNamespace(namespace);
    securityUpgradeOp.getSpec().setSgCluster(clusterName);
    securityUpgradeOp.setStatus(new StackGresDbOpsStatus());
    securityUpgradeOp.getStatus().setOpRetries(0);
    securityUpgradeOp.getStatus().setOpStarted(Instant.now().toString());
    securityUpgradeOp.getSpec().setOp("securityUpgrade");

    cluster.getMetadata().setName(clusterName);
    cluster.getMetadata().setNamespace(namespace);

    cluster = kubeDb.addOrReplaceCluster(cluster);
    securityUpgradeOp = kubeDb.addOrReplaceDbOps(securityUpgradeOp);
  }

  public abstract AbstractRestartStateHandler getRestartStateHandler();

  public abstract DbOpsRestartStatus getRestartStatus(StackGresDbOps dbOps);

  public abstract Optional<ClusterDbOpsRestartStatus> getRestartStatus(StackGresCluster dbOps);

  protected abstract void initializeDbOpsStatus(StackGresDbOps securityUpgradeOp, List<Pod> pods);

  protected Pod getPrimaryInstance(List<Pod> pods) {
    return pods.stream()
        .filter(pod -> StackGresContext.PRIMARY_ROLE.equals(
            pod.getMetadata().getLabels().get(StackGresContext.ROLE_KEY)))
        .findFirst().orElseThrow(() -> new InvalidCluster("Cluster has no primary pod"));
  }

  @Test
  void givenAnUninitializedJobState_itShouldInitializeIt() {

    podTestUtil.preparePods(cluster, 0, 1, 2);

    var pods = podTestUtil.getCLusterPods(cluster);

    final String dbOpsName = securityUpgradeOp.getMetadata().getName();
    var initializedOp = getRestartStateHandler()
        .initDbOpsStatus(dbOpsName, namespace, pods)
        .await()
        .atMost(Duration.ofMillis(50))
        .getItem1();

    List<String> expectedInitialInstances = pods.stream().map(Pod::getMetadata)
        .map(ObjectMeta::getName)
        .sorted(String::compareTo)
        .collect(Collectors.toUnmodifiableList());

    final DbOpsRestartStatus initializedSecurityUpgradeStatus = getRestartStatus(initializedOp);

    Pod primaryPod = podTestUtil.buildPrimaryPod(cluster, 0);

    assertEquals(primaryPod.getMetadata().getName(), initializedSecurityUpgradeStatus
        .getPrimaryInstance());

    List<String> actualInitialInstances = initializedSecurityUpgradeStatus
        .getInitialInstances();

    assertEquals(expectedInitialInstances, actualInitialInstances);

    List<String> actualPendingRestartedInstances = initializedSecurityUpgradeStatus
        .getPendingToRestartInstances();

    assertEquals(expectedInitialInstances, actualPendingRestartedInstances);

    assertTrue(() -> initializedSecurityUpgradeStatus.getRestartedInstances() == null
        || initializedSecurityUpgradeStatus.getRestartedInstances().isEmpty());

    assertNull(initializedSecurityUpgradeStatus.getFailure());

    assertFalse(Boolean.parseBoolean(initializedSecurityUpgradeStatus.getSwitchoverInitiated()));

    var storedOp = kubeDb.getDbOps(dbOpsName,
        namespace);

    assertEquals(initializedOp, storedOp, "It should store the DBOps status changes");

  }

  @Test
  void givenAnUninitializedClusterStatus_itShouldInitializeIt() {
    podTestUtil.preparePods(cluster, 0, 1, 2);

    var pods = podTestUtil.getCLusterPods(cluster);

    var initializedCluster = getRestartStateHandler()
        .initClusterDbOpsStatus(clusterName, namespace)
        .await()
        .atMost(Duration.ofMillis(50))
        .getItem1();


    List<String> expectedInitialInstances = pods.stream().map(Pod::getMetadata)
        .map(ObjectMeta::getName)
        .sorted(String::compareTo)
        .collect(Collectors.toUnmodifiableList());

    final ClusterDbOpsRestartStatus initializedSecurityUpgradeStatus = getRestartStatus(initializedCluster)
        .orElseThrow();

    Pod primaryPod = podTestUtil.buildPrimaryPod(cluster, 0);

    assertEquals(primaryPod.getMetadata().getName(), initializedSecurityUpgradeStatus
        .getPrimaryInstance());

    List<String> actualInitialInstances = initializedSecurityUpgradeStatus
        .getInitialInstances();

    assertEquals(expectedInitialInstances, actualInitialInstances);

    var storedCluster = kubeDb.getCluster(clusterName,
        namespace);

    assertEquals(initializedCluster, storedCluster, "It should store the DBOps status changes");

  }

  @Test
  void givenAnInitializedJobState_itShouldNotModifiedIt() {
    podTestUtil.preparePods(cluster, 0, 1, 2);


    var pods = podTestUtil.getCLusterPods(cluster);

    initializeDbOpsStatus(securityUpgradeOp, pods);

    final String dbOpsName = securityUpgradeOp.getMetadata().getName();
    var initializedOp = getRestartStateHandler()
        .initDbOpsStatus(dbOpsName, namespace, pods)
        .await()
        .atMost(Duration.ofMillis(50))
        .getItem1();

    assertEquals(securityUpgradeOp, initializedOp);
  }

  @Test
  void givenAnInitializedClusterStatus_itShouldNotModifiedIt() {
    podTestUtil.preparePods(cluster, 0, 1, 2);

    var pods = podTestUtil.getCLusterPods(cluster);

    initializeClusterStatus(cluster, pods);

    var initializedCluster = getRestartStateHandler()
        .initClusterDbOpsStatus(clusterName, namespace)
        .await()
        .atMost(Duration.ofMillis(50))
        .getItem1();

    assertEquals(cluster, initializedCluster);
  }

  protected abstract void initializeClusterStatus(StackGresCluster cluster,
                                                  List<Pod> pods);

  @Test
  void buildRestartState_shouldNotFail() {
    podTestUtil.preparePods(cluster, 0, 1, 2);

    var pods = podTestUtil.getCLusterPods(cluster);

    final Pod primaryPod = podTestUtil.buildPrimaryPod(cluster, 0);

    final Pod replica1Pod = podTestUtil.buildReplicaPod(cluster, 1);

    initializeDbOpsStatus(securityUpgradeOp, pods);

    securityUpgradeOp = kubeDb.addOrReplaceDbOps(securityUpgradeOp);

    var expectedClusterState = ImmutableClusterRestartState.builder()
        .namespace(namespace)
        .clusterName(clusterName)
        .restartMethod(securityUpgradeOp.getSpec().getSecurityUpgrade().getMethod())
        .isSwitchoverInitiated(Boolean.FALSE)
        .primaryInstance(primaryPod)
        .addInitialInstances(primaryPod, replica1Pod)
        .addRestartedInstances(replica1Pod)
        .addAllTotalInstances(pods)
        .build();

    var clusterState = getRestartStateHandler().buildClusterRestartState(securityUpgradeOp, pods);

    assertEqualsRestartState(expectedClusterState, clusterState);

  }


  @Test
  void givenACleanCluster_shouldUpdateTheOpStatus() {
    podTestUtil.preparePods(cluster, 0, 1, 2);

    var pods = podTestUtil.getCLusterPods(cluster)
        .stream().sorted(Comparator.comparing(p -> p.getMetadata().getName()))
        .collect(Collectors.toUnmodifiableList());

    when(clusterRestart.restartCluster(any()))
        .thenReturn(Multi.createFrom()
            .items(
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.POD_CREATED)
                    .pod(podTestUtil.buildReplicaPod(cluster, 3))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.POD_RESTART)
                    .pod(pods.get(1))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.POD_RESTART)
                    .pod(pods.get(2))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.SWITCHOVER)
                    .pod(pods.get(0))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.POD_RESTART)
                    .pod(pods.get(0))
                    .build()));

    var lastDbOp = getRestartStateHandler().restartCluster(securityUpgradeOp)
        .await().atMost(Duration.ofSeconds(2));

    StackGresDbOps updatedOp = kubeDb
        .getDbOps(securityUpgradeOp.getMetadata().getName(), namespace);

    assertEquals(updatedOp.toString(), lastDbOp.toString());

    final var securityUpgradeStatus = getRestartStatus(updatedOp);

    assertTrue(securityUpgradeStatus.getPendingToRestartInstances().isEmpty());
    assertEquals(Boolean.TRUE.toString(), securityUpgradeStatus.getSwitchoverInitiated());
    assertEquals(pods.size() + 1, securityUpgradeStatus.getRestartedInstances().size());
    assertEquals(pods.size(), securityUpgradeStatus.getInitialInstances().size());
    assertTrue(() -> securityUpgradeStatus.getFailure() == null
        || securityUpgradeStatus.getFailure().isEmpty());

    var lastClusterStatus = kubeDb.getCluster(clusterName, namespace);
    assertTrue(getRestartStatus(lastClusterStatus).isEmpty(),
        "It should erase the dbOps status after job is complete");
  }

}
