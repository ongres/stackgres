/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import static io.stackgres.operator.validation.CrdMatchTestHelper.getMaxLengthResourceNameFrom;
import static io.stackgres.testutil.StringUtils.getRandomClusterNameWithExactlySize;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.conciliation.cluster.ClusterRequiredResourcesGenerator;
import org.jooq.lambda.Unchecked;
import org.junit.jupiter.api.Test;

public abstract class AbstractRequiredResourceDecoratorTest<T> {

  @Test
  void shouldCreateResourceSuccessfully_OnceUsingTheCurrentCrdMaxLength() throws IOException {

    String validClusterName =
        getRandomClusterNameWithExactlySize(getMaxLengthResourceNameFrom(usingCrdFilename()));
    getResource().getMetadata().setName(validClusterName);

    List<HasMetadata> decorateResources =
        getResourceDecorator().decorateResources(getResourceContext());
    decorateResources.stream().forEach(
        resource -> {
          try {
            assertNameAndLabels(resource);
          } catch (Exception e) {
            throw new RuntimeException(
                String.format("Validation for resource %s of kind %s failed",
                    resource.getMetadata().getName(), resource.getKind()),
                e);
          }
        });
  }

  private void assertNameAndLabels(HasMetadata resource) {
    assertThatResourceNameIsComplaint(resource);

    resource.getMetadata().getLabels().entrySet().stream().forEach(label -> {
      asserThatLabelIsComplaint(label);
    });

    assertThatStatefulSetResourceLabelsAreComplaints(resource);
    assertThatCronJobResourceLabelsAreComplaints(resource);
    assertThatJobResourceLabelsAreComplaints(resource);
  }

  @Test
  void shouldGetAnExceededNameMessage_OnceUsingAnExceededMaxLengthName() throws IOException {

    String validClusterName =
        getRandomClusterNameWithExactlySize(getMaxLengthResourceNameFrom(usingCrdFilename()) + 1);
    getResource().getMetadata().setName(validClusterName);

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      List<HasMetadata> decorateResources =
          getResourceDecorator().decorateResources(getResourceContext());
      decorateResources.stream().forEach(
          resource -> {
            assertNameAndLabels(resource);
          });
    });
    String errorMessage = format("Validation for resource %s of kind %s failed",
        getResource().getMetadata().getName(), getResource().getKind());
    assertEquals(errorMessage, "Valid name must be 63 characters or less", exception.getMessage());
  }

  protected abstract String usingCrdFilename();

  protected abstract HasMetadata getResource();

  protected abstract RequiredResourceDecorator<T> getResourceDecorator();

  protected abstract T getResourceContext() throws IOException;

  public void assertThatResourceNameIsComplaint(HasMetadata resource) {
    if (resource instanceof Service) {
      ResourceUtil.nameIsValidService(resource.getMetadata().getName());
    } else {
      ResourceUtil.nameIsValidDnsSubdomain(resource.getMetadata().getName());
    }
  }

  public void asserThatLabelIsComplaint(Entry<String, String> label) {
    ResourceUtil.labelKey(label.getKey());
    ResourceUtil.labelValue(label.getValue());
  }

  public void assertThatStatefulSetResourceLabelsAreComplaints(HasMetadata resource) {
    if (resource instanceof StatefulSet) {
      ((StatefulSet) resource).getSpec().getTemplate().getMetadata().getLabels().entrySet().stream()
          .forEach(label -> {
            asserThatLabelIsComplaint(label);
          });

      assertThatVolumeClaimLabelsAreComplaints(resource);

      ((StatefulSet) resource).getSpec().getTemplate().getMetadata().getLabels().entrySet().stream()
          .forEach(label -> {
            asserThatLabelIsComplaint(label);
          });
    }
  }

  private void assertThatVolumeClaimLabelsAreComplaints(HasMetadata resource) {
    List<PersistentVolumeClaim> volumeClaims =
        ((StatefulSet) resource).getSpec().getVolumeClaimTemplates();

    volumeClaims.stream().forEach(volume -> {
      volume.getMetadata().getLabels().entrySet().stream().forEach(label -> {
        asserThatLabelIsComplaint(label);
      });
    });
  }

  public void assertThatCronJobResourceLabelsAreComplaints(HasMetadata resource) {
    if (resource instanceof CronJob) {
      ((CronJob) resource).getSpec().getJobTemplate().getMetadata().getLabels().entrySet()
          .stream().forEach(label -> {
            asserThatLabelIsComplaint(label);
          });
    }
  }

  public void assertThatJobResourceLabelsAreComplaints(HasMetadata resource) {
    if (resource instanceof Job) {
      ((Job) resource).getSpec().getTemplate().getMetadata().getLabels().entrySet().stream()
          .forEach(label -> {
            asserThatLabelIsComplaint(label);
          });
    }
  }

  public StackGresClusterScriptEntry getTestInitScripts() {
    final StackGresClusterScriptEntry script = new StackGresClusterScriptEntry();
    script.setName("test-script");
    script.setDatabase("db");
    script.setScript(Unchecked.supplier(() -> Resources
        .asCharSource(ClusterRequiredResourcesGenerator.class.getResource(
            "/prometheus-postgres-exporter/init.sql"),
            StandardCharsets.UTF_8)
        .read()).get());
    return script;
  }

}
