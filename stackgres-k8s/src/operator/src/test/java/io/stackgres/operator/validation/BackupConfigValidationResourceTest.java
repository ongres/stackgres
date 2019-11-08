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
class BackupConfigValidationResourceTest extends ValidationResourceTest<BackupConfigReview> {

    @BeforeEach
    public void setUp() {
        resource = new BackupConfigValidationResource(pipeline);

        review = JsonUtil
                .readFromJson("backupconfig_allow_request/create.json", BackupConfigReview.class);

        deleteReview = JsonUtil.readFromJson("backupconfig_allow_request/delete.json",
                BackupConfigReview.class);
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
