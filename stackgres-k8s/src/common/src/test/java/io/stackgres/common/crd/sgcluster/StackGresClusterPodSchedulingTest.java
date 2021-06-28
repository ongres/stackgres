package io.stackgres.common.crd.sgcluster;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.NodeSelector;
import io.fabric8.kubernetes.api.model.NodeSelectorRequirement;
import io.stackgres.testutil.JsonUtil;

class StackGresClusterPodSchedulingTest {

	private static final int ONE_SCHEDULE = 1;
	private static final int TWO_NODE_AFFFINITY_EXPRESSION_VALUES = 2;
	private static final String MATCH_EXPRESION_OPERATOR = "In";
	private static final String MATCH_EXPRESION_KEY = "kubernetes.io/e2e-az-name";
	private NodeSelector nodeAffinityRequiredDuringScheduling;
	private NodeSelectorRequirement matchExpression;

	@BeforeEach
	public void setup() {
		StackGresClusterPodScheduling scheduling = getSGClusterPodScheduling();
		this.nodeAffinityRequiredDuringScheduling = scheduling
				.getNodeAffinity()
				.getRequiredDuringSchedulingIgnoredDuringExecution();
		this.matchExpression = nodeAffinityRequiredDuringScheduling
				.getNodeSelectorTerms()
				.get(0)
				.getMatchExpressions()
				.get(0);
	}
	
	@Test
	void shouldValidateOperatorInUsedForSGClusterPodSchedulingNodeAffinityDefinition() {
		assertTrue(matchExpression.getOperator().equals(MATCH_EXPRESION_OPERATOR));
	}
	
	@Test
	void shouldValidateMathExpressionKeyOnceLoadingSGClusterPodSchedulingDefinitionWithRequiredDuringSchedulingNodeAffinity() {
		assertTrue(matchExpression.getKey().equals(MATCH_EXPRESION_KEY));
	}
	
	@Test
	void shouldValidateMathExpressionValuesOnceLoadingSGClusterPodSchedulingDefinitionWithRequiredDuringSchedulingNodeAffinity() {
		assertEquals(TWO_NODE_AFFFINITY_EXPRESSION_VALUES,matchExpression.getValues().size());
	}

	private StackGresClusterPodScheduling getSGClusterPodScheduling() {
		StackGresClusterPodScheduling scheduling = JsonUtil
        .readFromJson("stackgres_cluster/scheduling.json",
        		StackGresClusterPodScheduling.class);
    return scheduling;
	}
	
}
