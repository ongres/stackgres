/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.postgres;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class PostgresUnixSocketFactory extends SocketFactory {

  private final String socketPath;

  public PostgresUnixSocketFactory(String socketPath) {
    this.socketPath = socketPath;
  }

  @Override
  public Socket createSocket() throws IOException {
    return new PostgresUnixSocket(socketPath);
  }

  @Override
  public Socket createSocket(String host, int port) throws IOException {
    return createSocket();
  }

  @Override
  public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
      throws IOException, UnknownHostException {
    return createSocket();
  }

  @Override
  public Socket createSocket(InetAddress host, int port) throws IOException {
    return createSocket();
  }

  @Override
  public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
      throws IOException {
    return createSocket();
  }

}
