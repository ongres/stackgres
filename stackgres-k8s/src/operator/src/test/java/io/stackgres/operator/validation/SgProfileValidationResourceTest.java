/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operatorframework.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class SgProfileValidationResourceTest extends ValidationResourceTest<SgProfileReview> {

    @BeforeEach
    public void setUp() {
        resource = new SgProfileValidationResource(pipeline);

        review = JsonUtil
                .readFromJson("sgprofile_allow_request/create.json", SgProfileReview.class);

        deleteReview = JsonUtil
                .readFromJson("sgprofile_allow_request/delete.json", SgProfileReview.class);
    }

    @Test
    void givenAnValidAdmissionReview_itShouldReturnASuccessfulResponse() throws ValidationFailed {

        super.givenAnValidAdmissionReview_itShouldReturnASuccessfulResponse();

    }

    @Test
    void givenAnInvalidAdmissionReview_itShouldReturnAFailedResponse() throws ValidationFailed {

        super.givenAnInvalidAdmissionReview_itShouldReturnAFailedResponse();

    }

    @Test
    void givenAnDeletionReview_itShouldNotFail() throws ValidationFailed {
        super.givenAnDeletionReview_itShouldNotFail();

    }
}
