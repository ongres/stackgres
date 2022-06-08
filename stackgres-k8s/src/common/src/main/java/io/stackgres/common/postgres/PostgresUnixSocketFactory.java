/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.postgres;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnixDomainSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;

import javax.net.SocketFactory;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class PostgresUnixSocketFactory extends SocketFactory {

  @Override
  public Socket createSocket(String host, int port) throws IOException {
    var sc = SocketChannel.open(UnixDomainSocketAddress.of(host + "/.s.PGSQL." + port));
    return sc.socket();
  }

  @Override
  public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
      throws IOException, UnknownHostException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Socket createSocket(InetAddress host, int port) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
      throws IOException {
    throw new UnsupportedOperationException();
  }

}
