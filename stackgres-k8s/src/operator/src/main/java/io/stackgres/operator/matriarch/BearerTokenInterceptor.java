package io.stackgres.operator.matriarch;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

/**
 * Attaches {@code Authorization: Bearer <token>} to every outbound uplink call so the cloud can
 * authenticate this matriarch as its tenant. The token is the environment credential (north-star P10)
 * — for the PoC the JWT supplied via the {@code STACKGRES_TOKEN} environment variable.
 */
final class BearerTokenInterceptor implements ClientInterceptor {

  private static final Metadata.Key<String> AUTHORIZATION =
      Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

  private final String bearer;

  BearerTokenInterceptor(String token) {
    this.bearer = "Bearer " + token;
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> method, CallOptions options, Channel next) {
    return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, options)) {
      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        headers.put(AUTHORIZATION, bearer);
        super.start(responseListener, headers);
      }
    };
  }
}
