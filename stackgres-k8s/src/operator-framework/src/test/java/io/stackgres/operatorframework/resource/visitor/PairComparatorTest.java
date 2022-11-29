/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource.visitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.fabric8.kubernetes.api.model.Affinity;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.NodeAffinity;
import io.fabric8.kubernetes.api.model.NodeSelectorRequirement;
import io.fabric8.kubernetes.api.model.NodeSelectorTerm;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PreferredSchedulingTerm;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PairComparatorTest {

  @Test
  public void testEqualsWithSameValue() {
    Pod leftMeta = new Pod();
    leftMeta.setSpec(new PodSpec());
    leftMeta.getSpec().setDnsPolicy("test");
    Pod rightMeta = new Pod();
    rightMeta.setSpec(new PodSpec());
    rightMeta.getSpec().setDnsPolicy("test");
    Assertions.assertTrue(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsWithValue() {
    Pod leftMeta = new Pod();
    leftMeta.setSpec(new PodSpec());
    leftMeta.getSpec().setDnsPolicy("test");
    Pod rightMeta = new Pod();
    rightMeta.setSpec(new PodSpec());
    rightMeta.getSpec().setDnsPolicy("test2");
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsWithDefault() {
    Pod leftMeta = new Pod();
    leftMeta.setSpec(new PodSpec());
    leftMeta.getSpec().setDnsPolicy("test");
    Pod rightMeta = new Pod();
    rightMeta.setSpec(new PodSpec());
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsWithNull() {
    Pod leftMeta = new Pod();
    leftMeta.setSpec(new PodSpec());
    leftMeta.getSpec().setHostname("test");
    Pod rightMeta = new Pod();
    rightMeta.setSpec(new PodSpec());
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsWithNonNull() {
    Pod leftMeta = new Pod();
    leftMeta.setSpec(new PodSpec());
    Pod rightMeta = new Pod();
    rightMeta.setSpec(new PodSpec());
    rightMeta.getSpec().setDnsPolicy("test");
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsWithNullObject() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    ConfigMap rightMeta = new ConfigMap();
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsWithNonNullObject() {
    ConfigMap leftMeta = new ConfigMap();
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsWithNonNullNodeSelectorRequirement() {
    Pod leftMeta = podWithNodeSelectorRequirement(null);
    Pod rightMeta = podWithNodeSelectorRequirement(Lists.newArrayList());
    Assertions.assertTrue(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsWithSomeNodeSelectorRequirement() {
    Pod leftMeta = podWithNodeSelectorRequirement(null);
    Pod rightMeta = podWithNodeSelectorRequirement(Lists.newArrayList("1", "2"));
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsEmptyNodeSelectorRequirementWithNullOverwritten() {
    Pod leftMeta = podWithNodeSelectorRequirement(Lists.newArrayList());
    Pod rightMeta = podWithNodeSelectorRequirement(null);
    Assertions.assertTrue(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsSomeNodeSelectorRequirementWithNullOverwritten() {
    Pod leftMeta = podWithNodeSelectorRequirement(Lists.newArrayList("1", "2"));
    Pod rightMeta = podWithNodeSelectorRequirement(Lists.newArrayList());
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsSomeNodeSelectorRequirementWithEmptyOverwritten() {
    Pod leftMeta = podWithNodeSelectorRequirement(Lists.newArrayList("1", "2"));
    Pod rightMeta = podWithNodeSelectorRequirement(Lists.newArrayList());
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsSomeNodeSelectorRequirementOverwritten() {
    Pod leftMeta = podWithNodeSelectorRequirement(Lists.newArrayList("1", "2"));
    Pod rightMeta = podWithNodeSelectorRequirement(Lists.newArrayList("3", "4"));
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsSomeNodeSelectorRequirementFullyOverwritten() {
    Pod leftMeta = podWithNodeSelectorRequirement(Lists.newArrayList("1", "2"));
    Pod rightMeta = podWithNodeSelectorRequirement(Lists.newArrayList("1", "2", "3"));
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  private Pod podWithNodeSelectorRequirement(List<String> values) {
    Pod pod = new Pod();
    pod.setSpec(new PodSpec());
    pod.getSpec().setAffinity(new Affinity());
    pod.getSpec().getAffinity().setNodeAffinity(new NodeAffinity());
    pod.getSpec().getAffinity().getNodeAffinity()
        .setPreferredDuringSchedulingIgnoredDuringExecution(
            Lists.newArrayList(new PreferredSchedulingTerm()));
    pod.getSpec().getAffinity().getNodeAffinity()
        .getPreferredDuringSchedulingIgnoredDuringExecution()
        .get(0).setPreference(new NodeSelectorTerm());
    pod.getSpec().getAffinity().getNodeAffinity()
        .getPreferredDuringSchedulingIgnoredDuringExecution()
        .get(0).getPreference().setMatchFields(Lists.newArrayList(new NodeSelectorRequirement()));
    pod.getSpec().getAffinity().getNodeAffinity()
        .getPreferredDuringSchedulingIgnoredDuringExecution()
        .get(0).getPreference().getMatchFields().get(0).setValues(values);
    return pod;
  }

  @Test
  public void testEqualsWithNonNullOwnerReferences() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setOwnerReferences(new ArrayList<>());
    Assertions.assertTrue(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsWithSomeOwnerReferences() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata()
        .setOwnerReferences(Lists.newArrayList(new OwnerReference(), new OwnerReference()));
    rightMeta.getMetadata().getOwnerReferences().get(0).setName("1");
    rightMeta.getMetadata().getOwnerReferences().get(1).setName("2");
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsEmptyOwnerReferencesWithNullOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setOwnerReferences(Lists.newArrayList());
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    Assertions.assertTrue(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsSomeOwnerReferencesWithNullOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata()
        .setOwnerReferences(Lists.newArrayList(new OwnerReference(), new OwnerReference()));
    leftMeta.getMetadata().getOwnerReferences().get(0).setName("1");
    leftMeta.getMetadata().getOwnerReferences().get(1).setName("2");
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsSomeOwnerReferencesWithEmptyOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata()
        .setOwnerReferences(Lists.newArrayList(new OwnerReference(), new OwnerReference()));
    leftMeta.getMetadata().getOwnerReferences().get(0).setName("1");
    leftMeta.getMetadata().getOwnerReferences().get(1).setName("2");
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setOwnerReferences(Lists.newArrayList());
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsSomeOwnerReferencesOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata()
        .setOwnerReferences(Lists.newArrayList(new OwnerReference(), new OwnerReference()));
    leftMeta.getMetadata().getOwnerReferences().get(0).setName("1");
    leftMeta.getMetadata().getOwnerReferences().get(1).setName("2");
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata()
        .setOwnerReferences(Lists.newArrayList(new OwnerReference(), new OwnerReference()));
    rightMeta.getMetadata().getOwnerReferences().get(0).setName("3");
    rightMeta.getMetadata().getOwnerReferences().get(1).setName("4");
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsSomeOwnerReferencesFullyOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata()
        .setOwnerReferences(Lists.newArrayList(new OwnerReference(), new OwnerReference()));
    leftMeta.getMetadata().getOwnerReferences().get(0).setName("1");
    leftMeta.getMetadata().getOwnerReferences().get(1).setName("2");
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setOwnerReferences(
        Lists.newArrayList(new OwnerReference(), new OwnerReference(), new OwnerReference()));
    rightMeta.getMetadata().getOwnerReferences().get(0).setName("1");
    rightMeta.getMetadata().getOwnerReferences().get(1).setName("2");
    rightMeta.getMetadata().getOwnerReferences().get(2).setName("3");
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsWithNonNullLabels() {
    ConfigMap leftMeta = new ConfigMap();
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setLabels(new HashMap<>());
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsWithSomeLabels() {
    ConfigMap leftMeta = new ConfigMap();
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setLabels(Maps.newHashMap(ImmutableMap.of("1", "a", "2", "b")));
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsEmptyLabelsWithNullOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setLabels(Maps.newHashMap(ImmutableMap.of()));
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setLabels(null);
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsSomeLabelsWithNullOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setLabels(Maps.newHashMap(ImmutableMap.of("1", "a", "2", "b")));
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsSomeLabelsWithEmptyOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setLabels(Maps.newHashMap(ImmutableMap.of("1", "a", "2", "b")));
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setLabels(Maps.newHashMap(ImmutableMap.of()));
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsSomeLabelsOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setLabels(Maps.newHashMap(ImmutableMap.of("1", "a", "2", "b")));
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setLabels(Maps.newHashMap(ImmutableMap.of("3", "c")));
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsSomeLabelsFullyOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setLabels(Maps.newHashMap(ImmutableMap.of("1", "a", "2", "b")));
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setLabels(Maps.newHashMap(ImmutableMap.of("2", "c")));
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsWithNonNullAnnotations() {
    ConfigMap leftMeta = new ConfigMap();
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setAnnotations(new HashMap<>());
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsWithSomeAnnotations() {
    ConfigMap leftMeta = new ConfigMap();
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setAnnotations(Maps.newHashMap(ImmutableMap.of("1", "a", "2", "b")));
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsEmptyAnnotationsWithNullNotOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setAnnotations(Maps.newHashMap(ImmutableMap.of()));
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    Assertions.assertTrue(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsSomeAnnotationsWithNullNotOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setAnnotations(Maps.newHashMap(ImmutableMap.of("1", "a", "2", "b")));
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    Assertions.assertTrue(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsSomeAnnotationsWithEmptyNotOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setAnnotations(Maps.newHashMap(ImmutableMap.of("1", "a", "2", "b")));
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setAnnotations(Maps.newHashMap(ImmutableMap.of()));
    Assertions.assertTrue(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsSomeAnnotationsNotOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setAnnotations(Maps.newHashMap(ImmutableMap.of("1", "a", "2", "b")));
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setAnnotations(Maps.newHashMap(ImmutableMap.of("3", "c")));
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsSomeAnnotationsPartiallyOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setAnnotations(Maps.newHashMap(ImmutableMap.of("1", "a", "2", "b")));
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setAnnotations(Maps.newHashMap(ImmutableMap.of("2", "c")));
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsWithSomeAdditionalProperties() {
    final ConfigMap leftMeta = new ConfigMap();
    final ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setAdditionalProperty("1", "a");
    rightMeta.getMetadata().setAdditionalProperty("2", "b");
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsSomeAdditionalPropertiesWithEmptyOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setAdditionalProperty("1", "a");
    leftMeta.getMetadata().setAdditionalProperty("2", "b");
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsSomeAdditionalPropertiesOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setAdditionalProperty("1", "a");
    leftMeta.getMetadata().setAdditionalProperty("2", "b");
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setAdditionalProperty("3", "c");
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testEqualsSomeAdditionalPropertiesFullyOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setAdditionalProperty("1", "a");
    leftMeta.getMetadata().setAdditionalProperty("2", "b");
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setAdditionalProperty("2", "c");
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

}
