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
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class PostgresUnixSocket extends Socket {

  private final String socketPath;
  private final SocketAddress address;
  private final SocketChannel channel;

  public PostgresUnixSocket(String socketFileName) throws IOException {
    this.socketPath = socketFileName;
    this.address = UnixDomainSocketAddress.of(socketPath);
    this.channel = SocketChannel.open(address);
  }

  @Override
  public InputStream getInputStream() throws IOException {
    if (isClosed()) {
      throw new SocketException("Socket is closed");
    }
    if (!isConnected()) {
      throw new SocketException("Socket is not connected");
    }
    if (isInputShutdown()) {
      throw new SocketException("Socket input is shutdown");
    }

    return Channels.newInputStream(channel);
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    if (isClosed()) {
      throw new SocketException("Socket is closed");
    }
    if (!isConnected()) {
      throw new SocketException("Socket is not connected");
    }
    if (isOutputShutdown()) {
      throw new SocketException("Socket output is shutdown");
    }

    return Channels.newOutputStream(new WrappedWritableByteChannel());
  }

  @Override
  public SocketChannel getChannel() {
    return channel;
  }

  @Override
  public SocketAddress getLocalSocketAddress() {
    return address;
  }

  @Override
  public SocketAddress getRemoteSocketAddress() {
    return address;
  }

  @Override
  public void close() throws IOException {
    super.close();
    this.channel.close();
  }

  @Override
  public boolean isConnected() {
    return channel.isConnected();
  }

  @Override
  public boolean isBound() {
    return channel.isConnected();
  }

  @Override
  public boolean isInputShutdown() {
    return !channel.isConnected();
  }

  @Override
  public boolean isOutputShutdown() {
    return !channel.isConnected();
  }

  @Override
  public void shutdownInput() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void shutdownOutput() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void connect(SocketAddress endpoint) throws IOException {
  }

  @Override
  public void connect(SocketAddress endpoint, int timeout) throws IOException {
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

  private class WrappedWritableByteChannel implements WritableByteChannel {
    @Override
    public int write(ByteBuffer src) throws IOException {
      return PostgresUnixSocket.this.channel.write(src);
    }

    @Override
    public boolean isOpen() {
      return PostgresUnixSocket.this.channel.isOpen();
    }

    @Override
    public void close() throws IOException {
      PostgresUnixSocket.this.channel.close();
    }
  }

  @Override
  public String toString() {
    return "PostgresUnixSocket[" + socketPath + "]";
  }

}
