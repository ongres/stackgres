/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import static io.stackgres.operator.validation.CrdMatchTestHelper.getMaxLengthResourceNameFrom;
import static io.stackgres.testutil.StringUtils.getRandomClusterNameWithExactlySize;
import static java.lang.String.format;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import junit.framework.AssertionFailedError;
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
    decorateResources.stream().forEach(this::assertNameAndLabels);
  }

  @Test
  void shouldGetAnExceededNameMessage_OnceUsingAnExceededMaxLengthName()
      throws JsonProcessingException, IOException {
    String invalidClusterName =
        getRandomClusterNameWithExactlySize(getMaxLengthResourceNameFrom(usingCrdFilename()) + 1);
    getResource().getMetadata().setName(invalidClusterName);

    assertThrows(AssertionFailedError.class, () -> {
      List<HasMetadata> decorateResources =
          getResourceDecorator().decorateResources(getResourceContext());
      decorateResources.stream().forEach(this::assertNameAndLabels);
    });
  }

  protected abstract String usingCrdFilename();

  protected abstract HasMetadata getResource();

  protected abstract RequiredResourceDecorator<T> getResourceDecorator();

  protected abstract T getResourceContext() throws IOException;

  private void assertNameAndLabels(HasMetadata resource) {
    try {
      assertThatResourceNameIsComplaint(resource);

      resource.getMetadata().getLabels().entrySet().stream().forEach(label -> {
        asserThatLabelIsComplaint(label);
      });

      assertThatStatefulSetResourceLabelsAreComplaints(resource);
      assertThatCronJobResourceLabelsAreComplaints(resource);
      assertThatJobResourceLabelsAreComplaints(resource);
    } catch (Exception ex) {
      throw new AssertionFailedError(format(
          "Validation for resource %s of kind %s failed",
          resource.getMetadata().getName(), resource.getKind()));
    }
  }

  public void assertThatResourceNameIsComplaint(HasMetadata resource) {
    if (resource instanceof Service) {
      ResourceUtil.nameIsValidService(resource.getMetadata().getName());
    } else if (resource instanceof StatefulSet) {
      ResourceUtil.nameIsValidDnsSubdomainForSts(resource.getMetadata().getName());
    } else if (resource instanceof Job) {
      ResourceUtil.nameIsValidDnsSubdomainForJob(resource.getMetadata().getName());
    } else if (resource instanceof CronJob) {
      ResourceUtil.nameIsValidDnsSubdomainForCronJob(resource.getMetadata().getName());
    } else {
      ResourceUtil.nameIsValidDnsSubdomain(resource.getMetadata().getName());
    }
  }

  public void asserThatLabelIsComplaint(Entry<String, String> label) {
    try {
      ResourceUtil.labelKey(label.getKey());
    } catch (Exception ex) {
      throw new AssertionFailedError(format(
          "Validation of key for label key %s with value %s failed",
          label.getKey(), label.getValue()));
    }
    try {
      ResourceUtil.labelValue(label.getValue());
    } catch (Exception ex) {
      throw new AssertionFailedError(format(
          "Validation of value for label key %s with value %s failed",
          label.getKey(), label.getValue()));
    }
  }

  public void assertThatStatefulSetResourceLabelsAreComplaints(HasMetadata resource) {
    if (resource instanceof StatefulSet) {
      final StatefulSet statefulSet = (StatefulSet) resource;
      statefulSet.getSpec().getTemplate().getMetadata().getLabels().entrySet().stream()
          .forEach(label -> {
            asserThatLabelIsComplaint(label);
          });

      assertThatVolumeClaimLabelsAreComplaints(statefulSet);

      statefulSet.getSpec().getTemplate().getMetadata().getLabels().entrySet().stream()
          .forEach(label -> {
            asserThatLabelIsComplaint(label);
          });
    }
  }

  private void assertThatVolumeClaimLabelsAreComplaints(StatefulSet statefulSet) {
    List<PersistentVolumeClaim> volumeClaims =
        statefulSet.getSpec().getVolumeClaimTemplates();

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
