package io.stackgres.operator.conciliation.factory.cluster;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import io.fabric8.kubernetes.api.model.NodeSelector;
import io.fabric8.kubernetes.api.model.NodeSelectorTerm;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.PreferredSchedulingTerm;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.NodeAffinity;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.fixture.NodeSelectorFixture;
import io.stackgres.fixture.NodeSelectorRequirementFixture;
import io.stackgres.fixture.NodeSelectorTermFixture;
import io.stackgres.fixture.PreferredSchedulingTermFixture;
import io.stackgres.operator.conciliation.ContainerFactoryDiscoverer;
import io.stackgres.operator.conciliation.InitContainerFactoryDiscover;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.fixture.NodeAffinityFixture;
import io.stackgres.operator.fixture.StackGresClusterFixture;

class PodTemplateSpecFactoryTest {

	@Mock
	private ResourceFactory<StackGresClusterContext, PodSecurityContext> podSecurityContext;

	@Mock
	private LabelFactory<StackGresCluster> labelFactory;

	@Mock
	private ContainerFactoryDiscoverer<StackGresClusterContainerContext> containerFactoryDiscoverer;

	@Mock
	private InitContainerFactoryDiscover<StackGresClusterContainerContext> initContainerFactoryDiscoverer;

	@InjectMocks
	private PodTemplateSpecFactory podTemplateSpecFactory;

	@BeforeEach
	public void setupClass() {
		this.podTemplateSpecFactory = new PodTemplateSpecFactory(
				podSecurityContext
				,labelFactory
				,containerFactoryDiscoverer
				,initContainerFactoryDiscoverer);
	}

	@Test
  void shouldValidateNodeAffinity_AsEmpty() {
	  io.fabric8.kubernetes.api.model.NodeAffinity k8sPodNodeAffinity = this.podTemplateSpecFactory
        .buildPodNodeAffinity(new StackGresClusterFixture().empty());
	  assertNull(k8sPodNodeAffinity.getRequiredDuringSchedulingIgnoredDuringExecution());
	  assertEquals(0,k8sPodNodeAffinity.getPreferredDuringSchedulingIgnoredDuringExecution().size());
	}
	
	@Test
	void shouldValidateNodeAffinity_WithRequiredDuringScheduling() {

		NodeSelectorTerm nodeSelectorTerm = NodeSelectorTermFixture.get()
				.withRequirement(new NodeSelectorRequirementFixture()
								.withKey("kubernetes.io/e2e-az-name")
								.withOperator("In")
								.withValue("e2e-az1", "e2e-az2")
								.build())
				.build();

		NodeSelectorTerm nodeSelectorTerm2 = NodeSelectorTermFixture.get()
				.withRequirement(new NodeSelectorRequirementFixture()
						.withKey("kubernetes.io/e2e-az-name")
						.withOperator("In")
						.withValue("e2e-az3", "e2e-az4")
						.build())
				.build();

		NodeSelector nodeSelectorRequirements = new NodeSelectorFixture()
				.withTerms(nodeSelectorTerm, nodeSelectorTerm2)
				.buildAsRequirements();

		NodeAffinity nodeAffinity = new NodeAffinityFixture()
			.withRequireds(nodeSelectorRequirements)
			.build();
		
		StackGresCluster cluster = new StackGresClusterFixture().withNodeAffinity(nodeAffinity).build();

		io.fabric8.kubernetes.api.model.NodeAffinity k8sPodNodeAffinity = this.podTemplateSpecFactory
				.buildPodNodeAffinity(cluster);

		assertEquals(2,
				k8sPodNodeAffinity
				.getRequiredDuringSchedulingIgnoredDuringExecution()
				.getNodeSelectorTerms().size());
		
		k8sPodNodeAffinity.getRequiredDuringSchedulingIgnoredDuringExecution().getNodeSelectorTerms().forEach(term -> {
			term.getMatchExpressions().forEach(match -> {
				Assert.assertEquals("kubernetes.io/e2e-az-name", match.getKey());
			});
		});
	}
	
	@Test
	void shouldValidateNodeAffinity_withPreferredExecution() {

		PreferredSchedulingTerm preference1 = new PreferredSchedulingTermFixture()
				.withWeight(1).withRequirement(new NodeSelectorRequirementFixture()
				  .withKey("kubernetes.io/e2e-az-preferred")
				  .withOperator("In")
				  .withValue("e2e-az1", "e2e-az2")
				  .build()
		   ).build();
		
		PreferredSchedulingTerm preference2 = new PreferredSchedulingTermFixture()
				.withWeight(1)
				.withRequirement(new NodeSelectorRequirementFixture()
				  .withKey("kubernetes.io/e2e-az-preferred")
				  .withOperator("In")
				  .withValue("e2e-az1", "e2e-az2")
				  .build())
				.build();

		NodeAffinity nodeAffinity = new NodeAffinityFixture()
				.withPreferredbuild(Arrays.asList(preference1, preference2))
				.build();
		
		StackGresCluster cluster = new StackGresClusterFixture().withNodeAffinity(nodeAffinity).build();

		io.fabric8.kubernetes.api.model.NodeAffinity k8sPodNodeAffinity = this.podTemplateSpecFactory
				.buildPodNodeAffinity(cluster);


		assertEquals(2, k8sPodNodeAffinity
				.getPreferredDuringSchedulingIgnoredDuringExecution()
				.size());
		
		k8sPodNodeAffinity.getPreferredDuringSchedulingIgnoredDuringExecution().forEach(preference -> {
			preference.getPreference().getMatchExpressions().forEach(math -> {
				assertEquals("kubernetes.io/e2e-az-preferred", math.getKey());
			});
		});
	}
}
