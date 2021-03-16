package io.stackgres.operatorframework.resource.visitor;

import java.util.HashMap;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
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
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNull(leftMeta.getMetadata().getAnnotations());
  }

  @Test
  public void testUpdateWithNonNullAnnoations() {
    ConfigMap leftMeta = new ConfigMap();
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    rightMeta.getMetadata().setAnnotations(new HashMap<>());
    ResourcePairVisitor.update(leftMeta, rightMeta);
    Assertions.assertNotNull(leftMeta.getMetadata());
    Assertions.assertNotNull(leftMeta.getMetadata().getAnnotations());
    Assertions.assertEquals(0, leftMeta.getMetadata().getAnnotations().size());
  }

  @Test
  public void testUpdateWithSomeAnnoations() {
    ConfigMap leftMeta = new ConfigMap();
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
  public void testUpdateEmptyAnnoationsWithNullNotOverwritten() {
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
  public void testUpdateSomeAnnoationsWithNullNotOverwritten() {
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
  public void testUpdateSomeAnnoationsWithEmptyNotOverwritten() {
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
  public void testUpdateSomeAnnoationsNotOverwritten() {
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
  public void testUpdateSomeAnnoationsPartiallyOverwritten() {
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

}
