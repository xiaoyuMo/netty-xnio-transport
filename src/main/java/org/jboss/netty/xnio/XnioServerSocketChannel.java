/*
 * Copyright 2013 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */
package org.jboss.netty.xnio;

import io.netty.channel.EventLoop;
import org.xnio.Option;
import org.xnio.OptionMap;
import org.xnio.channels.AcceptingChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * {@link io.netty.channel.socket.ServerSocketChannel} which uses XNIO.
 *
 * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
 */
public final class XnioServerSocketChannel extends AbstractXnioServerSocketChannel {
    private volatile AcceptingChannel channel;
    private final OptionMap.Builder options = OptionMap.builder();
    private volatile EventLoop eventLoop;

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop instanceof XnioEventLoop;
    }

    @Override
    public EventLoop eventLoop() {
        if (eventLoop == null) {
            return super.eventLoop();
        }
        return eventLoop;
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        channel = ((XnioEventLoop) eventLoop()).executor.getWorker()
                .createStreamConnectionServer(localAddress, new AcceptListener(), options.getMap());
        eventLoop = new XnioEventLoop(channel.getIoThread());
        // start accepting
        channel.resumeAccepts();
    }

    @Override
    protected void doClose() throws Exception {
        if (channel != null) {
            channel.close();
        }
    }

    @Override
    protected void doBeginRead() throws Exception {
        if (channel != null) {
            channel.resumeAccepts();
        }
    }

    @Override
    public boolean isOpen() {
        return channel == null || channel.isOpen();
    }

    @Override
    protected InetSocketAddress localAddress0() {
        if (channel != null) {
            return channel.getLocalAddress(InetSocketAddress.class);
        }
        return null;
    }

    @Override
    protected <T> T getOption0(Option<T> option) throws IOException {
        if (channel != null) {
            return channel.getOption(option);
        }
        return options.getMap().get(option);
    }

    @Override
    protected <T> void setOption0(Option<T> option, T value) throws IOException {
        if (channel != null) {
            channel.setOption(option, value);
        } else {
            options.set(option, value);
        }
    }
}
