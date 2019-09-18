package io.stackgres.operator.validation;

import io.stackgres.operator.app.KubernetesClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/validation")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ValidationResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(ValidationResource.class);

  @POST
  public AdmissionResponse validate(AdmissionReview cluster) {

    AdmissionResponse response = new AdmissionResponse();
    response.setUid(cluster.getRequest().getUid());
    response.setAllowed(true);
    return response;

  }



}
