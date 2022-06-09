/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.postgres;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOption;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.util.Set;

public class PostgresUnixSocket extends Socket {

  private final String socketPath;

  private SocketChannel channel;
  private boolean bound = false;

  public PostgresUnixSocket(String socketFileName) {
    this.socketPath = socketFileName;
  }

  @Override
  public void close() throws IOException {
    if (channel != null) {
      channel.close();
    }
  }

  @Override
  public void connect(SocketAddress endpoint) throws IOException {
    connect(endpoint, 0);
  }

  @Override
  public void connect(SocketAddress endpoint, int timeout) throws IOException {
    channel = SocketChannel.open(UnixDomainSocketAddress.of(socketPath));
  }

  @Override
  public InputStream getInputStream() {
    return Channels.newInputStream(channel);
  }

  @Override
  public OutputStream getOutputStream() {
    return Channels.newOutputStream(channel);
  }

  @Override
  public void bind(SocketAddress bindpoint) throws IOException {
    bound = true;
  }

  @Override
  public InetAddress getInetAddress() {
    return null;
  }

  @Override
  public InetAddress getLocalAddress() {
    return null;
  }

  @Override
  public int getPort() {
    return -1;
  }

  @Override
  public int getLocalPort() {
    return -1;
  }

  @Override
  public SocketAddress getRemoteSocketAddress() {
    return null;
  }

  @Override
  public SocketAddress getLocalSocketAddress() {
    return null;
  }

  @Override
  public SocketChannel getChannel() {
    return channel;
  }

  @Override
  public void setTcpNoDelay(boolean on) throws SocketException {
  }

  @Override
  public boolean getTcpNoDelay() throws SocketException {
    return false;
  }

  @Override
  public void setSoLinger(boolean on, int linger) throws SocketException {
  }

  @Override
  public int getSoLinger() throws SocketException {
    return -1;
  }

  @Override
  public void sendUrgentData(int data) throws IOException {
  }

  @Override
  public void setOOBInline(boolean on) throws SocketException {
  }

  @Override
  public boolean getOOBInline() throws SocketException {
    return false;
  }

  @Override
  public synchronized void setSoTimeout(int timeout) throws SocketException {
  }

  @Override
  public synchronized int getSoTimeout() throws SocketException {
    return -1;
  }

  @Override
  public synchronized void setSendBufferSize(int size) throws SocketException {
  }

  @Override
  public synchronized int getSendBufferSize() throws SocketException {
    return -1;
  }

  @Override
  public synchronized void setReceiveBufferSize(int size) throws SocketException {
  }

  @Override
  public synchronized int getReceiveBufferSize() throws SocketException {
    return -1;
  }

  @Override
  public void setKeepAlive(boolean on) throws SocketException {
  }

  @Override
  public boolean getKeepAlive() throws SocketException {
    return false;
  }

  @Override
  public void setTrafficClass(int tc) throws SocketException {
  }

  @Override
  public int getTrafficClass() throws SocketException {
    return -1;
  }

  @Override
  public void setReuseAddress(boolean on) throws SocketException {
  }

  @Override
  public boolean getReuseAddress() throws SocketException {
    return false;
  }

  @Override
  public void shutdownInput() throws IOException {
  }

  @Override
  public void shutdownOutput() throws IOException {
  }

  @Override
  public String toString() {
    return "UnixSocket[" + socketPath + "]";
  }

  @Override
  public boolean isConnected() {
    return channel != null && channel.isConnected();
  }

  @Override
  public boolean isBound() {
    return channel != null && channel.isConnected() && bound;
  }

  @Override
  public boolean isClosed() {
    return channel == null || !channel.isOpen();
  }

  @Override
  public boolean isInputShutdown() {
    return channel == null || !channel.isConnected();
  }

  @Override
  public boolean isOutputShutdown() {
    return channel == null || !channel.isConnected();
  }

  @Override
  public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
  }

  @Override
  public <T> Socket setOption(SocketOption<T> name, T value) throws IOException {
    return this;
  }

  @Override
  public <T> T getOption(SocketOption<T> name) throws IOException {
    return null;
  }

  @Override
  public Set<SocketOption<?>> supportedOptions() {
    return Set.of();
  }

}
