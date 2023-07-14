/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.application.SgApplication;
import io.stackgres.apiweb.application.bbfcompass.BabelfishCompass;
import io.stackgres.apiweb.application.bbfcompass.FileUpload;
import io.stackgres.apiweb.dto.ApplicationDto;
import io.stackgres.apiweb.rest.utils.CommonApiResponses;
import io.stackgres.common.StringUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jooq.lambda.Unchecked;

@Path("applications")
@RequestScoped
@Authenticated
public class ApplicationsResource {

  @Inject
  Instance<SgApplication> applications;

  @Operation(responses = {
      @ApiResponse(responseCode = "200", description = "OK",
          content = {@Content(
              mediaType = MediaType.APPLICATION_JSON,
              schema = @Schema(type = "object"))})
  })
  @CommonApiResponses
  @GET
  public Map<String, List<ApplicationDto>> getAllApplications() {
    var appsDto = new ArrayList<ApplicationDto>();
    for (SgApplication app : applications) {
      if (app.isEnabled()) {
        appsDto.add(ApplicationDto.builder()
            .publisher(app.publisher())
            .name(app.appName())
            .build());
      }
    }
    return Map.of("applications", List.copyOf(appsDto));
  }

  @Operation(responses = {
      @ApiResponse(responseCode = "200", description = "OK",
          content = {@Content(
              mediaType = MediaType.APPLICATION_JSON,
              schema = @Schema(type = "object"))})
  })
  @CommonApiResponses
  @GET
  @Path("{publisher}/{name}")
  public ApplicationDto getApplication(@PathParam String publisher, @PathParam String name) {
    for (SgApplication app : applications) {
      if (app.isEnabled() && app.appName().equals(name) && app.publisher().equals(publisher)) {
        return ApplicationDto.builder()
            .publisher(app.publisher())
            .name(app.appName())
            .build();
      }
    }
    throw new NotFoundException("Application %s/%s not found".formatted(publisher, name));
  }

  @Operation(responses = {
      @ApiResponse(responseCode = "200", description = "OK",
          content = {@Content(
              mediaType = MediaType.APPLICATION_JSON,
              schema = @Schema(type = "object"))})
  })
  @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA))
  @CommonApiResponses
  @POST
  @Path("com.ongres/babelfish-compass")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Map<String, String> getApplication(@MultipartForm MultipartFormDataInput form)
      throws IOException {
    BabelfishCompass bbfCompass = getBabelfishCompassApp();

    Map<String, List<InputPart>> formDataMap = form.getFormDataMap();

    String reportName = Optional.ofNullable(formDataMap.get("reportName"))
        .filter(m -> !m.isEmpty())
        .map(m -> m.get(0))
        .map(Unchecked.function(InputPart::getBodyAsString))
        .orElseThrow(() -> new WebApplicationException("reportName is required",
            Response.Status.BAD_REQUEST));

    List<InputPart> sqlFiles = Optional.ofNullable(formDataMap.get("sqlFiles"))
        .orElseThrow(() -> new WebApplicationException("sqlFiles is required",
            Response.Status.BAD_REQUEST));

    List<FileUpload> cmFiles = new ArrayList<>();
    for (InputPart inputPart : sqlFiles) {
      String line = inputPart.getHeaders().getFirst("Content-Disposition");
      String filename = getFileNameOfUploadedFile(line);
      InputStream is = inputPart.getBody(InputStream.class, null);
      cmFiles.add(new FileUpload(filename, is));
    }

    if (cmFiles.isEmpty()) {
      throw new WebApplicationException("Could not extract sqlFiles",
          Response.Status.BAD_REQUEST);
    }

    return bbfCompass.run(reportName, List.copyOf(cmFiles));
  }

  @SuppressFBWarnings(value = "SA_LOCAL_SELF_COMPARISON",
      justification = "False positive")
  private BabelfishCompass getBabelfishCompassApp() {
    BabelfishCompass bbfCompass = null;
    for (SgApplication app : applications) {
      if (app instanceof BabelfishCompass compass && compass.isEnabled()) {
        bbfCompass = compass;
        break;
      }
    }
    if (bbfCompass == null) {
      throw new WebApplicationException("babelfish-compass app is not enabled",
          Response.Status.BAD_REQUEST);
    }
    return bbfCompass;
  }

  private String getFileNameOfUploadedFile(String header) {
    if (header != null && !header.isEmpty()) {
      String[] contentDispositionTokens = header.split(";");
      for (String contentDispositionHeaderToken : contentDispositionTokens) {
        if (contentDispositionHeaderToken.trim().startsWith("filename")) {
          return contentDispositionHeaderToken.split("=")[1].strip()
              .replace("\"", "");
        }
      }
    }
    return StringUtil.generateRandom(8);
  }

}
