/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public abstract class ConversionResourceTest<T extends CustomResource<?, ?>> {

  protected static final ObjectMapper MAPPER = JsonUtil.JSON_MAPPER;

  protected ConversionResource conversionResource;

  @Mock
  protected ConversionPipeline pipeline;

  protected abstract T getCustomResource();

  protected abstract ConversionResource getConversionResource();

  @BeforeEach
  void setUp() {
    conversionResource = getConversionResource();
  }

  @Test
  void resourceConversion_shouldNotFail() {

    T customResource = getCustomResource();
    final ConversionReview conversionReview = buildConversionReview(customResource);

    final List<ObjectNode> objects = conversionReview.getRequest().getObjects();

    when(pipeline.convert("stackgres.io/v1", objects))
        .thenReturn(objects);

    ConversionReviewResponse response = conversionResource.convert(conversionReview);

    verify(pipeline).convert("stackgres.io/v1", objects);

    assertEquals(objects, response.getResponse().getConvertedObjects());
    assertEquals("Success", response.getResponse().getResult().getStatus());
    assertEquals(conversionReview.getRequest().getUid(), response.getResponse().getUid());
  }

  @Test
  void ifPipelineFails_resourceConversionShouldNotFail() {

    T customResource = getCustomResource();
    final ConversionReview conversionReview = buildConversionReview(customResource);

    final List<ObjectNode> objects = conversionReview.getRequest().getObjects();

    when(pipeline.convert("stackgres.io/v1", objects))
        .thenThrow(new RuntimeException("Any exception"));

    ConversionReviewResponse response = conversionResource.convert(conversionReview);
    assertNull(response.getResponse().getConvertedObjects());
    assertEquals("Failed", response.getResponse().getResult().getStatus());
    assertEquals(conversionReview.getRequest().getUid(), response.getResponse().getUid());

    verify(pipeline).convert("stackgres.io/v1", objects);
  }

  public static <T> ConversionReview buildConversionReview(T object) {
    ConversionReview conversionReview = new ConversionReview();
    conversionReview.setRequest(new ConversionRequest());
    conversionReview.getRequest().setUid(UUID.randomUUID());
    conversionReview.getRequest().setDesiredApiVersion("stackgres.io/v1");
    conversionReview.getRequest().setObjects(Collections.singletonList(MAPPER.valueToTree(object)));
    return conversionReview;
  }
}
