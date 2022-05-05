/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.quarkus.arc.DefaultBean;

@ApplicationScoped
public class ResourceWriterProducers {

  @Produces
  @DefaultBean
  ResourceWriter<ConfigMap> configMapWriter(ConfigMapWriter cmWriter) {
    return cmWriter;
  }

  @Produces
  @DefaultBean
  ResourceWriter<Job> jobWriter(JobWriter jobWriter) {
    return jobWriter;
  }

  @Produces
  @DefaultBean
  ResourceWriter<Secret> secretWriter(SecretWriter secretWriter) {
    return secretWriter;
  }

}
