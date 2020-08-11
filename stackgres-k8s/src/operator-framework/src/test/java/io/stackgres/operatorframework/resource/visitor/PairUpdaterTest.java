package io.stackgres.operatorframework.resource.visitor;

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
  }

}
