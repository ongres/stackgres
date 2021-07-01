package io.stackgres.operator.fixture;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.NodeAffinityBuilder;
import io.fabric8.kubernetes.api.model.NodeSelector;
import io.fabric8.kubernetes.api.model.NodeSelectorBuilder;
import io.fabric8.kubernetes.api.model.PreferredSchedulingTerm;
import io.stackgres.common.crd.NodeAffinity;

public class NodeAffinityFixture {

	private NodeSelector requireds = new NodeSelectorBuilder().build();
	private List<PreferredSchedulingTerm> preference = new ArrayList<PreferredSchedulingTerm>();

	public NodeAffinityFixture withRequireds(NodeSelector requireds) {
		this.requireds = requireds;
		return this;
	}

	public NodeAffinityFixture withPreferredbuild(List<PreferredSchedulingTerm> preference) {
		this.preference = preference;
		return this;
	}

	public NodeAffinity build() {
		NodeAffinity nodeAffinity = new NodeAffinity();
		io.fabric8.kubernetes.api.model.NodeAffinity k8sNodeAffinity = new NodeAffinityBuilder()
		  .withRequiredDuringSchedulingIgnoredDuringExecution(requireds)
		  .withPreferredDuringSchedulingIgnoredDuringExecution(preference)
		  .build();
		nodeAffinity.setPreferredDuringSchedulingIgnoredDuringExecution(k8sNodeAffinity.getPreferredDuringSchedulingIgnoredDuringExecution());
		nodeAffinity.setRequiredDuringSchedulingIgnoredDuringExecution(k8sNodeAffinity.getRequiredDuringSchedulingIgnoredDuringExecution());
		return nodeAffinity;
	}
}

