/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.external.knative.Service;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamTargetPgLambda;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForStream;
import io.stackgres.common.labels.StreamLabelFactory;
import io.stackgres.common.labels.StreamLabelMapper;
import io.stackgres.operator.conciliation.stream.StackGresStreamContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StreamPgLambdaServiceTest {

  private final LabelFactoryForStream labelFactory =
      new StreamLabelFactory(new StreamLabelMapper());

  @Mock
  private StackGresStreamContext context;

  private StreamPgLamdaService streamPgLambdaService;

  private StackGresStream stream;

  @BeforeEach
  void setUp() {
    streamPgLambdaService = new StreamPgLamdaService();
    streamPgLambdaService.setLabelFactory(labelFactory);
    stream = Fixtures.stream().loadSgClusterToCloudEvent().get();
    lenient().when(context.getSource()).thenReturn(stream);
  }

  @Test
  void generateResource_whenPgLambdaTargetPresent_shouldGenerateService() {
    StackGresStreamTargetPgLambda pgLambda = new StackGresStreamTargetPgLambda();
    pgLambda.setScript("function process(record) { return record; }");
    stream.getSpec().getTarget().setPgLambda(pgLambda);

    List<HasMetadata> resources = streamPgLambdaService.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertTrue(resources.get(0) instanceof Service);
  }

  @Test
  void generateResource_whenNoPgLambdaTarget_shouldReturnEmpty() {
    stream.getSpec().getTarget().setPgLambda(null);

    List<HasMetadata> resources = streamPgLambdaService.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenPgLambdaPresent_serviceShouldHaveCorrectNamespace() {
    StackGresStreamTargetPgLambda pgLambda = new StackGresStreamTargetPgLambda();
    pgLambda.setScript("function process(record) { return record; }");
    stream.getSpec().getTarget().setPgLambda(pgLambda);

    List<HasMetadata> resources = streamPgLambdaService.generateResource(context).toList();

    assertEquals(1, resources.size());
    Service service = (Service) resources.get(0);
    assertEquals(stream.getMetadata().getNamespace(), service.getMetadata().getNamespace());
  }

  @Test
  void generateResource_whenPgLambdaPresent_serviceShouldHaveCorrectName() {
    StackGresStreamTargetPgLambda pgLambda = new StackGresStreamTargetPgLambda();
    pgLambda.setScript("function process(record) { return record; }");
    stream.getSpec().getTarget().setPgLambda(pgLambda);

    List<HasMetadata> resources = streamPgLambdaService.generateResource(context).toList();

    assertEquals(1, resources.size());
    Service service = (Service) resources.get(0);
    assertEquals(StreamPgLamdaService.name(context), service.getMetadata().getName());
  }

  @Test
  void generateResource_whenPgLambdaPresent_serviceShouldHaveLabels() {
    StackGresStreamTargetPgLambda pgLambda = new StackGresStreamTargetPgLambda();
    pgLambda.setScript("function process(record) { return record; }");
    stream.getSpec().getTarget().setPgLambda(pgLambda);

    List<HasMetadata> resources = streamPgLambdaService.generateResource(context).toList();

    assertEquals(1, resources.size());
    Service service = (Service) resources.get(0);
    assertTrue(service.getMetadata().getLabels() != null
        && !service.getMetadata().getLabels().isEmpty());
  }

}
