/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common.fixture;

import java.util.HashMap;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.testutil.fixture.Fixture;

public class VersionedFixture<T extends AdmissionReview<? extends HasMetadata>>
    extends Fixture<T> {

  @Override
  protected void transform(T resource) {
    if (resource.getRequest().getObject() != null) {
      if (resource.getRequest().getObject().getMetadata().getAnnotations() == null) {
        resource.getRequest().getObject().getMetadata().setAnnotations(new HashMap<>());
      }
      resource.getRequest().getObject().getMetadata().getAnnotations().put(
          StackGresContext.VERSION_KEY, StackGresVersion.LATEST.getVersion());
    }
    if (resource.getRequest().getOldObject() != null) {
      if (resource.getRequest().getOldObject().getMetadata().getAnnotations() == null) {
        resource.getRequest().getOldObject().getMetadata().setAnnotations(new HashMap<>());
      }
      resource.getRequest().getOldObject().getMetadata().getAnnotations().put(
          StackGresContext.VERSION_KEY, StackGresVersion.LATEST.getVersion());
    }
  }

}
