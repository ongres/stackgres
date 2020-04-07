/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationResource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public abstract class ValidationResourceTest<T extends AdmissionReview<?>> {

    @Mock
    protected ValidationPipeline<T> pipeline;

    protected ValidationResource<T> resource;

    protected T review;

    protected T deleteReview;

    @Test
    void givenAnValidAdmissionReview_itShouldReturnASuccessfulResponse() throws ValidationFailed {

        doNothing().when(pipeline).validate(review);

        AdmissionReviewResponse response = resource.validate(review);

        assertTrue(response.getResponse().isAllowed());

        verify(pipeline).validate(review);

    }

    @Test
    void givenAnInvalidAdmissionReview_itShouldReturnAFailedResponse() throws ValidationFailed {

        doThrow(new ValidationFailed("validation failed")).when(pipeline).validate(review);

        AdmissionReviewResponse response = resource.validate(review);

        assertFalse(response.getResponse().isAllowed());

        assertEquals("validation failed", response.getResponse().getStatus().getMessage());

        verify(pipeline).validate(review);

    }

    @Test
    void givenAnDeletionReview_itShouldNotFail() throws ValidationFailed {

        doNothing().when(pipeline).validate(deleteReview);

        AdmissionReviewResponse response = resource.validate(deleteReview);

        assertTrue(response.getResponse().isAllowed());

        verify(pipeline).validate(deleteReview);

    }
}
