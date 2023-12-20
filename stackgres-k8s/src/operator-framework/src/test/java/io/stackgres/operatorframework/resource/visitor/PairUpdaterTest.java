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

public class PairUpdaterTest {

  @Test
  public void testUpdateWithValue() {
    Pod leftMeta = new Pod();
    leftMeta.setSpec(new PodSpec());
    leftMeta.getSpec().setDnsPolicy("test");
    Pod rightMeta = new Pod();
    rightMeta.setSpec(new PodSpec());
    rightMeta.getSpec().setDnsPolicy("test2");
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertEquals("test2", leftMeta.getSpec().getDnsPolicy());
  }

  @Test
  public void testUpdateWithDefault() {
    Pod leftMeta = new Pod();
    leftMeta.setSpec(new PodSpec());
    leftMeta.getSpec().setDnsPolicy("test");
    Pod rightMeta = new Pod();
    rightMeta.setSpec(new PodSpec());
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertEquals("ClusterFirst", leftMeta.getSpec().getDnsPolicy());
  }

  @Test
  public void testUpdateWithNull() {
    Pod leftMeta = new Pod();
    leftMeta.setSpec(new PodSpec());
    leftMeta.getSpec().setHostname("test");
    Pod rightMeta = new Pod();
    rightMeta.setSpec(new PodSpec());
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNull(leftMeta.getSpec().getHostname());
  }

  @Test
  public void testUpdateWithNonNull() {
    Pod leftMeta = new Pod();
    leftMeta.setSpec(new PodSpec());
    Pod rightMeta = new Pod();
    rightMeta.setSpec(new PodSpec());
    rightMeta.getSpec().setDnsPolicy("test");
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getSpec().getDnsPolicy());
  }

  @Test
  public void testUpdateWithNullObject() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    ConfigMap rightMeta = new ConfigMap();
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNull(leftMeta.getMetadata());
  }

  @Test
  public void testUpdateWithNonNullObject() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getAnnotations());
    Assertions.assertTrue(leftMeta.getMetadata().getAnnotations().isEmpty());
    Assertions.assertNotNull(leftMeta.getMetadata().getLabels());
    Assertions.assertTrue(leftMeta.getMetadata().getLabels().isEmpty());
    Assertions.assertNotNull(leftMeta.getMetadata().getAdditionalProperties());
    Assertions.assertEquals(0, leftMeta.getMetadata().getAdditionalProperties().size());
    Assertions.assertEquals(0, leftMeta.getMetadata().getOwnerReferences().size());
  }

  @Test
  public void testUpdateWithNonNullNodeSelectorRequirementNotOverwritten() {
    Pod leftMeta = podWithNodeSelectorRequirement(null);
    Pod rightMeta = podWithNodeSelectorRequirement(Lists.newArrayList());
    ResourcePairVisitor.update(leftMeta, rightMeta);
    List<String> values = getPodNodeSelectorRequirement(leftMeta).getValues();
    Assertions.assertNull(values);
  }

  @Test
  public void testUpdateWithSomeNodeSelectorRequirement() {
    Pod leftMeta = podWithNodeSelectorRequirement(null);
    Pod rightMeta = podWithNodeSelectorRequirement(Lists.newArrayList("1", "2"));
    ResourcePairVisitor.update(leftMeta, rightMeta);
    List<String> values = getPodNodeSelectorRequirement(leftMeta).getValues();
    Assertions.assertNotNull(values);
    Assertions.assertEquals(2, values.size());
    Assertions.assertEquals("1", values.getFirst());
    Assertions.assertEquals("2", values.get(1));
  }

  @Test
  public void testUpdateEmptyNodeSelectorRequirementWithNullNotOverwritten() {
    Pod leftMeta = podWithNodeSelectorRequirement(Lists.newArrayList());
    Pod rightMeta = podWithNodeSelectorRequirement(null);
    ResourcePairVisitor.update(leftMeta, rightMeta);
    List<String> values = getPodNodeSelectorRequirement(leftMeta).getValues();
    Assertions.assertNotNull(values);
    Assertions.assertEquals(0, values.size());
  }

  @Test
  public void testUpdateSomeNodeSelectorRequirementWithNullOverwritten() {
    Pod leftMeta = podWithNodeSelectorRequirement(Lists.newArrayList("1", "2"));
    Pod rightMeta = podWithNodeSelectorRequirement(null);
    ResourcePairVisitor.update(leftMeta, rightMeta);
    List<String> values = getPodNodeSelectorRequirement(leftMeta).getValues();
    Assertions.assertNull(values);
  }

  @Test
  public void testUpdateSomeNodeSelectorRequirementWithEmptyOverwritten() {
    Pod leftMeta = podWithNodeSelectorRequirement(Lists.newArrayList("1", "2"));
    Pod rightMeta = podWithNodeSelectorRequirement(Lists.newArrayList());
    ResourcePairVisitor.update(leftMeta, rightMeta);
    List<String> values = getPodNodeSelectorRequirement(leftMeta).getValues();
    Assertions.assertNotNull(values);
    Assertions.assertEquals(0, values.size());
  }

  @Test
  public void testUpdateSomeNodeSelectorRequirementOverwritten() {
    Pod leftMeta = podWithNodeSelectorRequirement(Lists.newArrayList("1", "2"));
    Pod rightMeta = podWithNodeSelectorRequirement(Lists.newArrayList("3", "4"));
    ResourcePairVisitor.update(leftMeta, rightMeta);
    List<String> values = getPodNodeSelectorRequirement(leftMeta).getValues();
    Assertions.assertNotNull(values);
    Assertions.assertEquals(2, values.size());
    Assertions.assertEquals("3", values.getFirst());
    Assertions.assertEquals("4", values.get(1));
  }

  @Test
  public void testUpdateSomeNodeSelectorRequirementFullyOverwritten() {
    Pod leftMeta = podWithNodeSelectorRequirement(Lists.newArrayList("1", "2"));
    Pod rightMeta = podWithNodeSelectorRequirement(Lists.newArrayList("1", "2", "3"));
    ResourcePairVisitor.update(leftMeta, rightMeta);
    List<String> values = getPodNodeSelectorRequirement(leftMeta).getValues();
    Assertions.assertNotNull(values);
    Assertions.assertEquals(3, values.size());
    Assertions.assertEquals("1", values.getFirst());
    Assertions.assertEquals("2", values.get(1));
    Assertions.assertEquals("3", values.get(2));
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
    getPodNodeSelectorRequirement(pod).setValues(values);
    return pod;
  }

  private NodeSelectorRequirement getPodNodeSelectorRequirement(Pod leftMeta) {
    return leftMeta.getSpec().getAffinity().getNodeAffinity()
        .getPreferredDuringSchedulingIgnoredDuringExecution().get(0)
        .getPreference().getMatchFields().get(0);
  }

  @Test
  public void testUpdateWithNonNullOwnerReferences() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setOwnerReferences(new ArrayList<>());
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getOwnerReferences());
    Assertions.assertEquals(0, leftMeta.getMetadata().getOwnerReferences().size());
  }

  @Test
  public void testUpdateWithSomeOwnerReferences() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata()
        .setOwnerReferences(Lists.newArrayList(new OwnerReference(), new OwnerReference()));
    rightMeta.getMetadata().getOwnerReferences().get(0).setName("1");
    rightMeta.getMetadata().getOwnerReferences().get(1).setName("2");
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getOwnerReferences());
    Assertions.assertEquals(2, leftMeta.getMetadata().getOwnerReferences().size());
    Assertions.assertEquals("1", leftMeta.getMetadata().getOwnerReferences().get(0).getName());
    Assertions.assertEquals("2", leftMeta.getMetadata().getOwnerReferences().get(1).getName());
  }

  @Test
  public void testUpdateEmptyOwnerReferencesWithNullOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setOwnerReferences(Lists.newArrayList());
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getOwnerReferences());
    Assertions.assertEquals(0, leftMeta.getMetadata().getOwnerReferences().size());
  }

  @Test
  public void testUpdateSomeOwnerReferencesWithNullOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata()
        .setOwnerReferences(Lists.newArrayList(new OwnerReference(), new OwnerReference()));
    leftMeta.getMetadata().getOwnerReferences().get(0).setName("1");
    leftMeta.getMetadata().getOwnerReferences().get(1).setName("2");
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getOwnerReferences());
    Assertions.assertEquals(0, leftMeta.getMetadata().getOwnerReferences().size());
  }

  @Test
  public void testUpdateSomeOwnerReferencesWithEmptyOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata()
        .setOwnerReferences(Lists.newArrayList(new OwnerReference(), new OwnerReference()));
    leftMeta.getMetadata().getOwnerReferences().get(0).setName("1");
    leftMeta.getMetadata().getOwnerReferences().get(1).setName("2");
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setOwnerReferences(Lists.newArrayList());
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getOwnerReferences());
    Assertions.assertEquals(0, leftMeta.getMetadata().getOwnerReferences().size());
  }

  @Test
  public void testUpdateSomeOwnerReferencesOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata()
        .setOwnerReferences(Lists.newArrayList(new OwnerReference(), new OwnerReference()));
    leftMeta.getMetadata().getOwnerReferences().get(0).setName("1");
    leftMeta.getMetadata().getOwnerReferences().get(1).setName("2");
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setOwnerReferences(Lists.newArrayList(new OwnerReference()));
    rightMeta.getMetadata().getOwnerReferences().get(0).setName("3");
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getOwnerReferences());
    Assertions.assertEquals(1, leftMeta.getMetadata().getOwnerReferences().size());
    Assertions.assertEquals("3", leftMeta.getMetadata().getOwnerReferences().get(0).getName());
  }

  @Test
  public void testUpdateSomeOwnerReferencesFullyOverwritten() {
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
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getOwnerReferences());
    Assertions.assertEquals(3, leftMeta.getMetadata().getOwnerReferences().size());
    Assertions.assertEquals("1", leftMeta.getMetadata().getOwnerReferences().get(0).getName());
    Assertions.assertEquals("2", leftMeta.getMetadata().getOwnerReferences().get(1).getName());
    Assertions.assertEquals("3", leftMeta.getMetadata().getOwnerReferences().get(2).getName());
  }

  @Test
  public void testUpdateWithNonNullLabels() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setLabels(new HashMap<>());
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getLabels());
    Assertions.assertEquals(0, leftMeta.getMetadata().getLabels().size());
  }

  @Test
  public void testUpdateWithSomeLabels() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setLabels(Maps.newHashMap(ImmutableMap.of("1", "a", "2", "b")));
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getLabels());
    Assertions.assertEquals(2, leftMeta.getMetadata().getLabels().size());
    Assertions.assertEquals("a", leftMeta.getMetadata().getLabels().get("1"));
    Assertions.assertEquals("b", leftMeta.getMetadata().getLabels().get("2"));
  }

  @Test
  public void testUpdateEmptyLabelsWithNullOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setLabels(Maps.newHashMap(ImmutableMap.of()));
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getLabels());
    Assertions.assertTrue(leftMeta.getMetadata().getLabels().isEmpty());
  }

  @Test
  public void testUpdateSomeLabelsWithNullOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setLabels(Maps.newHashMap(ImmutableMap.of("1", "a", "2", "b")));
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getLabels());
    Assertions.assertTrue(leftMeta.getMetadata().getLabels().isEmpty());
  }

  @Test
  public void testUpdateSomeLabelsWithEmptyOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setLabels(Maps.newHashMap(ImmutableMap.of("1", "a", "2", "b")));
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setLabels(Maps.newHashMap(ImmutableMap.of()));
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getLabels());
    Assertions.assertEquals(0, leftMeta.getMetadata().getLabels().size());
  }

  @Test
  public void testUpdateSomeLabelsOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setLabels(Maps.newHashMap(ImmutableMap.of("1", "a", "2", "b")));
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setLabels(Maps.newHashMap(ImmutableMap.of("3", "c")));
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getLabels());
    Assertions.assertEquals(1, leftMeta.getMetadata().getLabels().size());
    Assertions.assertEquals("c", leftMeta.getMetadata().getLabels().get("3"));
  }

  @Test
  public void testUpdateSomeLabelsFullyOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setLabels(Maps.newHashMap(ImmutableMap.of("1", "a", "2", "b")));
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setLabels(Maps.newHashMap(ImmutableMap.of("2", "c")));
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getLabels());
    Assertions.assertEquals(1, leftMeta.getMetadata().getLabels().size());
    Assertions.assertEquals("c", leftMeta.getMetadata().getLabels().get("2"));
  }

  @Test
  public void testUpdateWithNonNullAnnotations() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setAnnotations(new HashMap<>());
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getAnnotations());
    Assertions.assertEquals(0, leftMeta.getMetadata().getAnnotations().size());
  }

  @Test
  public void testUpdateWithSomeAnnotations() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setAnnotations(Maps.newHashMap(ImmutableMap.of("1", "a", "2", "b")));
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getAnnotations());
    Assertions.assertEquals(2, leftMeta.getMetadata().getAnnotations().size());
    Assertions.assertEquals("a", leftMeta.getMetadata().getAnnotations().get("1"));
    Assertions.assertEquals("b", leftMeta.getMetadata().getAnnotations().get("2"));
  }

  @Test
  public void testUpdateEmptyAnnotationsWithNullNotOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setAnnotations(Maps.newHashMap(ImmutableMap.of()));
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getAnnotations());
    Assertions.assertEquals(0, leftMeta.getMetadata().getAnnotations().size());
  }

  @Test
  public void testUpdateSomeAnnotationsWithNullNotOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setAnnotations(Maps.newHashMap(ImmutableMap.of("1", "a", "2", "b")));
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getAnnotations());
    Assertions.assertEquals(2, leftMeta.getMetadata().getAnnotations().size());
    Assertions.assertEquals("a", leftMeta.getMetadata().getAnnotations().get("1"));
    Assertions.assertEquals("b", leftMeta.getMetadata().getAnnotations().get("2"));
  }

  @Test
  public void testUpdateSomeAnnotationsWithEmptyNotOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setAnnotations(Maps.newHashMap(ImmutableMap.of("1", "a", "2", "b")));
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setAnnotations(Maps.newHashMap(ImmutableMap.of()));
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getAnnotations());
    Assertions.assertEquals(2, leftMeta.getMetadata().getAnnotations().size());
    Assertions.assertEquals("a", leftMeta.getMetadata().getAnnotations().get("1"));
    Assertions.assertEquals("b", leftMeta.getMetadata().getAnnotations().get("2"));
  }

  @Test
  public void testUpdateSomeAnnotationsNotOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setAnnotations(Maps.newHashMap(ImmutableMap.of("1", "a", "2", "b")));
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setAnnotations(Maps.newHashMap(ImmutableMap.of("3", "c")));
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getAnnotations());
    Assertions.assertEquals(3, leftMeta.getMetadata().getAnnotations().size());
    Assertions.assertEquals("a", leftMeta.getMetadata().getAnnotations().get("1"));
    Assertions.assertEquals("b", leftMeta.getMetadata().getAnnotations().get("2"));
    Assertions.assertEquals("c", leftMeta.getMetadata().getAnnotations().get("3"));
  }

  @Test
  public void testUpdateSomeAnnotationsPartiallyOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setAnnotations(Maps.newHashMap(ImmutableMap.of("1", "a", "2", "b")));
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setAnnotations(Maps.newHashMap(ImmutableMap.of("2", "c")));
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getAnnotations());
    Assertions.assertEquals(2, leftMeta.getMetadata().getAnnotations().size());
    Assertions.assertEquals("a", leftMeta.getMetadata().getAnnotations().get("1"));
    Assertions.assertEquals("c", leftMeta.getMetadata().getAnnotations().get("2"));
  }

  @Test
  public void testUpdateWithSomeAdditionalProperties() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setAdditionalProperty("1", "a");
    rightMeta.getMetadata().setAdditionalProperty("2", "b");
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getAdditionalProperties());
    Assertions.assertEquals(2, leftMeta.getMetadata().getAdditionalProperties().size());
    Assertions.assertEquals("a", leftMeta.getMetadata().getAdditionalProperties().get("1"));
    Assertions.assertEquals("b", leftMeta.getMetadata().getAdditionalProperties().get("2"));
  }

  @Test
  public void testUpdateSomeAdditionalPropertiesWithEmptyOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setAdditionalProperty("1", "a");
    leftMeta.getMetadata().setAdditionalProperty("2", "b");
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getAdditionalProperties());
    Assertions.assertEquals(0, leftMeta.getMetadata().getAdditionalProperties().size());
  }

  @Test
  public void testUpdateSomeAdditionalPropertiesOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setAdditionalProperty("1", "a");
    leftMeta.getMetadata().setAdditionalProperty("2", "b");
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setAdditionalProperty("3", "c");
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getAdditionalProperties());
    Assertions.assertEquals(1, leftMeta.getMetadata().getAdditionalProperties().size());
    Assertions.assertEquals("c", leftMeta.getMetadata().getAdditionalProperties().get("3"));
  }

  @Test
  public void testUpdateSomeAdditionalPropertiesFullyOverwritten() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    leftMeta.getMetadata().setAdditionalProperty("1", "a");
    leftMeta.getMetadata().setAdditionalProperty("2", "b");
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setAdditionalProperty("2", "c");
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getAdditionalProperties());
    Assertions.assertEquals(1, leftMeta.getMetadata().getAdditionalProperties().size());
    Assertions.assertEquals("c", leftMeta.getMetadata().getAdditionalProperties().get("2"));
  }

}
