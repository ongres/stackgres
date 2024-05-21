/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.stream.ImmutableStackGresStreamContext;
import io.stackgres.operator.conciliation.stream.StackGresStreamContext;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

abstract class StreamJobTestCase {

  @Inject
  @OperatorVersionBinder
  StreamDeploymentOrJob streamJob;

  StackGresConfig config;

  StackGresStream stream;

  @BeforeEach
  void setUp() {
    config = Fixtures.config().loadDefault().get();
    stream = getStream();
  }

  abstract StackGresStream getStream();

  void setSgStreamScheduling() {
    var streamScheduling = Fixtures.stream().scheduling().loadDefault().get();
    stream.getSpec().getPods().setScheduling(streamScheduling);
  }

  @Test
  void givenAContextWithAStream_itShouldGenerateADeployment() {
    StackGresStreamContext context = ImmutableStackGresStreamContext.builder()
        .config(config)
        .source(stream)
        .build();

    var generatedResources = streamJob.generateResource(context)
        .collect(Collectors.toUnmodifiableList());

    assertEquals(1, generatedResources.stream().filter(r -> r.getKind().equals("Deployment"))
        .count());
  }

  @Test
  void givenAContextWithAStream_itShouldGenerateAJob() {
    stream.getSpec().setMaxRetries(1);
    StackGresStreamContext context = ImmutableStackGresStreamContext.builder()
        .config(config)
        .source(stream)
        .build();

    var generatedResources = streamJob.generateResource(context)
        .collect(Collectors.toUnmodifiableList());

    assertEquals(1, generatedResources.stream().filter(r -> r.getKind().equals("Job"))
        .count());
  }

  @Test
  void shouldGenerateDeploymentWithNodeSelector_onceSgStreamSchedulingHasNodeSelector() {
    setSgStreamScheduling();

    StackGresStreamContext context = ImmutableStackGresStreamContext.builder()
        .config(config)
        .source(stream)
        .build();

    var generatedResources = streamJob.generateResource(context)
        .collect(Collectors.toUnmodifiableList());
    var deployment = (Deployment) generatedResources.getFirst();
    assertEquals(2, deployment.getSpec().getTemplate().getSpec().getNodeSelector().size());
  }

  @Test
  void shouldGenerateJobWithNodeSelector_onceSgStreamSchedulingHasNodeSelector() {
    setSgStreamScheduling();
    stream.getSpec().setMaxRetries(1);

    StackGresStreamContext context = ImmutableStackGresStreamContext.builder()
        .config(config)
        .source(stream)
        .build();

    var generatedResources = streamJob.generateResource(context)
        .collect(Collectors.toUnmodifiableList());
    var job = (Job) generatedResources.getFirst();
    assertEquals(2, job.getSpec().getTemplate().getSpec().getNodeSelector().size());
  }

  @Test
  void shouldGenerateDeploymentWithNodeAffinity_onceSgStreamSchedulingHasAffinity() {
    setSgStreamScheduling();

    StackGresStreamContext context = ImmutableStackGresStreamContext.builder()
        .config(config)
        .source(stream)
        .build();

    var generatedResources = streamJob.generateResource(context)
        .collect(Collectors.toUnmodifiableList());

    var deployment = (Deployment) generatedResources.getFirst();
    var nodeAffinity = deployment.getSpec().getTemplate().getSpec().getAffinity().getNodeAffinity();
    assertEquals(1, nodeAffinity.getPreferredDuringSchedulingIgnoredDuringExecution().size());
    assertEquals(1, nodeAffinity.getRequiredDuringSchedulingIgnoredDuringExecution()
        .getNodeSelectorTerms().size());
  }

  @Test
  void shouldGenerateJobWithNodeAffinity_onceSgStreamSchedulingHasAffinity() {
    setSgStreamScheduling();
    stream.getSpec().setMaxRetries(1);

    StackGresStreamContext context = ImmutableStackGresStreamContext.builder()
        .config(config)
        .source(stream)
        .build();

    var generatedResources = streamJob.generateResource(context)
        .collect(Collectors.toUnmodifiableList());

    var job = (Job) generatedResources.getFirst();
    var nodeAffinity = job.getSpec().getTemplate().getSpec().getAffinity().getNodeAffinity();
    assertEquals(1, nodeAffinity.getPreferredDuringSchedulingIgnoredDuringExecution().size());
    assertEquals(1, nodeAffinity.getRequiredDuringSchedulingIgnoredDuringExecution()
        .getNodeSelectorTerms().size());
  }

}
