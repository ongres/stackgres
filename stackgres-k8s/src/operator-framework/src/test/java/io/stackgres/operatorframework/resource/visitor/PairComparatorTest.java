package io.stackgres.operatorframework.resource.visitor;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PairComparatorTest {

  @Test
  public void testUpdateWithSameValue() {
    Pod leftMeta = new Pod();
    leftMeta.setSpec(new PodSpec());
    leftMeta.getSpec().setDnsPolicy("test");
    Pod rightMeta = new Pod();
    rightMeta.setSpec(new PodSpec());
    rightMeta.getSpec().setDnsPolicy("test");
    Assertions.assertTrue(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testUpdateWithValue() {
    Pod leftMeta = new Pod();
    leftMeta.setSpec(new PodSpec());
    leftMeta.getSpec().setDnsPolicy("test");
    Pod rightMeta = new Pod();
    rightMeta.setSpec(new PodSpec());
    rightMeta.getSpec().setDnsPolicy("test2");
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testUpdateWithDefault() {
    Pod leftMeta = new Pod();
    leftMeta.setSpec(new PodSpec());
    leftMeta.getSpec().setDnsPolicy("test");
    Pod rightMeta = new Pod();
    rightMeta.setSpec(new PodSpec());
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testUpdateWithNull() {
    Pod leftMeta = new Pod();
    leftMeta.setSpec(new PodSpec());
    leftMeta.getSpec().setHostname("test");
    Pod rightMeta = new Pod();
    rightMeta.setSpec(new PodSpec());
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testUpdateWithNonNull() {
    Pod leftMeta = new Pod();
    leftMeta.setSpec(new PodSpec());
    Pod rightMeta = new Pod();
    rightMeta.setSpec(new PodSpec());
    rightMeta.getSpec().setDnsPolicy("test");
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testUpdateWithNullObject() {
    ConfigMap leftMeta = new ConfigMap();
    leftMeta.setMetadata(new ObjectMeta());
    ConfigMap rightMeta = new ConfigMap();
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

  @Test
  public void testUpdateWithNonNullObject() {
    ConfigMap leftMeta = new ConfigMap();
    ConfigMap rightMeta = new ConfigMap();
    rightMeta.setMetadata(new ObjectMeta());
    Assertions.assertFalse(ResourcePairVisitor.equals(leftMeta, rightMeta));
  }

}
