/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.validation.CrdMatchTestHelper;

public class RequiredResourceDecoratorTestHelper extends CrdMatchTestHelper {

  private String selectedCrd;
  private int currentMaxLength;

  @Override
  protected List<String> getSelectedCrds() {
    return Arrays.asList(this.selectedCrd);
  }

  protected void setupMaxLengthFromCrd() throws IOException {
    withEveryYaml(crdTree -> {
      JsonNode metadataMaxLength = extractMetadataMaxLengthResourceName(crdTree);
      this.currentMaxLength = metadataMaxLength.intValue();
    });
  }

  private JsonNode extractMetadataMaxLengthResourceName(JsonNode crdTree) {
    return crdTree.get("spec").get("versions").get(0).get("schema").get("openAPIV3Schema")
        .get("properties").get("metadata")
        .get("properties").get("name").get("maxLength");
  }

  protected void withSelectedCrd(String crdFilename) throws IOException {
    this.selectedCrd = crdFilename;
    setupMaxLengthFromCrd();
  }

  public int withCurrentCrdMaxLength() {
    return currentMaxLength;
  }

  public void asserThatLabelIsComplaint(Entry<String, String> label) {
    ResourceUtil.labelKey(label.getKey());
    ResourceUtil.labelValue(label.getValue());
  }

  public void assertThatStatefulSetResourceLabelsAreComplaints(HasMetadata resource) {
    if (resource instanceof StatefulSet) {
      ((StatefulSet) resource).getSpec().getTemplate().getMetadata().getLabels().entrySet().stream().forEach(label -> {
        asserThatLabelIsComplaint(label);
      });
    }
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

}
