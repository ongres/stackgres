package io.stackgres.slony.cloud;

import io.stackgres.cloud.CloudEnvironment;

import java.net.http.HttpClient;

interface CloudProbe {

    CloudEnvironment detect(HttpClient client);

}