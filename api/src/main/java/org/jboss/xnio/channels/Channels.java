/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.xnio.channels;

import org.jboss.xnio.OptionMap;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

/**
 * A utility class containing static methods to convert from one channel type to another.
 *
 * @apiviz.exclude
 */
public final class Channels {

    private Channels() {
    }

    /**
     * Create an allocated message channel which wraps a stream channel using a simple length-body protocol.  The
     * maximum inbound and outbound message sizes may be specified in the option map.
     *
     * @param streamChannel the stream channel
     * @param optionMap the initial options
     * @return the allocated message channel
     * @see org.jboss.xnio.channels.CommonOptions#MAX_INBOUND_MESSAGE_SIZE
     * @see org.jboss.xnio.channels.CommonOptions#MAX_OUTBOUND_MESSAGE_SIZE
     *
     * @since 2.0
     */
    public static AllocatedMessageChannel createAllocatedMessageChannel(final StreamChannel streamChannel, final OptionMap optionMap) {
        return new WrappingAllocatedMessageChannel(streamChannel, optionMap);
    }

    /**
     * Create a SSL/TLS-enabled channel over a TCP channel.  Uses the given {@code SSLContext}, and uses the option map to configure
     * the parameters of the connection (including whether this side is the client or the server).
     *
     * @param sslContext the SSL context to use
     * @param tcpChannel the TCP channel over which the connection is encapsulated
     * @param optionMap the configuration options for the channel
     * @return the new SSL TCP channel
     *
     * @since 2.0
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    public static SslTcpChannel createSslTcpChannel(final SSLContext sslContext, final TcpChannel tcpChannel, final OptionMap optionMap) throws IOException {
        throw new UnsupportedOperationException("SSL channel");
    }

    /**
     * Simple utility method to execute a blocking write on a byte channel.  The method blocks until the channel
     * is writable, and then the message is written.
     *
     * @param channel the channel to write on
     * @param buffer the data to write
     * @param <C> the channel type
     * @return the number of bytes written
     * @throws IOException if an I/O exception occurs
     * @since 1.2
     */
    public static <C extends WritableByteChannel & SuspendableWriteChannel> int writeBlocking(C channel, ByteBuffer buffer) throws IOException {
        int res;
        while ((res = channel.write(buffer)) == 0) {
            channel.awaitWritable();
        }
        return res;
    }

    /**
     * Simple utility method to execute a blocking write on a byte channel with a timeout.  The method blocks until the channel
     * is writable, and then the message is written.
     *
     * @param channel the channel to write on
     * @param buffer the data to write
     * @param time the amount of time to wait
     * @param unit the unit of time to wait
     * @param <C> the channel type
     * @return the number of bytes written
     * @throws IOException if an I/O exception occurs
     * @since 1.2
     */
    public static <C extends WritableByteChannel & SuspendableWriteChannel> int writeBlocking(C channel, ByteBuffer buffer, long time, TimeUnit unit) throws IOException {
        int res = channel.write(buffer);
        if (res == 0) {
            channel.awaitWritable(time, unit);
            return channel.write(buffer);
        } else {
            return res;
        }
    }

    /**
     * Simple utility method to execute a blocking write on a gathering byte channel.  The method blocks until the channel
     * is writable, and then the message is written.
     *
     * @param channel the channel to write on
     * @param buffers the data to write
     * @param offs the index of the first buffer to write
     * @param len the number of buffers to write
     * @param <C> the channel type
     * @return the number of bytes written
     * @throws IOException if an I/O exception occurs
     * @since 1.2
     */
    public static <C extends GatheringByteChannel & SuspendableWriteChannel> long writeBlocking(C channel, ByteBuffer[] buffers, int offs, int len) throws IOException {
        long res;
        while ((res = channel.write(buffers, offs, len)) == 0) {
            channel.awaitWritable();
        }
        return res;
    }

    /**
     * Simple utility method to execute a blocking write on a gathering byte channel with a timeout.  The method blocks until the channel
     * is writable, and then the message is written.
     *
     * @param channel the channel to write on
     * @param buffers the data to write
     * @param offs the index of the first buffer to write
     * @param len the number of buffers to write
     * @param time the amount of time to wait
     * @param unit the unit of time to wait
     * @param <C> the channel type
     * @return the number of bytes written
     * @throws IOException if an I/O exception occurs
     * @since 1.2
     */
    public static <C extends GatheringByteChannel & SuspendableWriteChannel> long writeBlocking(C channel, ByteBuffer[] buffers, int offs, int len, long time, TimeUnit unit) throws IOException {
        long res = channel.write(buffers, offs, len);
        if (res == 0L) {
            channel.awaitWritable(time, unit);
            return channel.write(buffers, offs, len);
        } else {
            return res;
        }
    }

    /**
     * Simple utility method to execute a blocking send on a message channel.  The method blocks until the channel
     * is writable, and then the message is written.
     *
     * @param channel the channel to write on
     * @param buffer the data to write
     * @param <C> the channel type
     * @throws IOException if an I/O exception occurs
     * @since 1.2
     */
    public static <C extends WritableMessageChannel & SuspendableWriteChannel> void sendBlocking(C channel, ByteBuffer buffer) throws IOException {
        WritableMessageChannel.Result result;
        do {
            if ((result = channel.send(buffer)) == WritableMessageChannel.OK) return;
            channel.awaitWritable();
        } while (result == WritableMessageChannel.NOT_SENT);
        while (! channel.flush()) {
            channel.awaitWritable();
        }
    }

    /**
     * Simple utility method to execute a blocking send on a message channel with a timeout.  The method blocks until the channel
     * is writable, and then the message is written.
     *
     * @param channel the channel to write on
     * @param buffer the data to write
     * @param time the amount of time to wait
     * @param unit the unit of time to wait
     * @param <C> the channel type
     * @return the write result
     * @throws IOException if an I/O exception occurs
     * @since 1.2
     */
    public static <C extends WritableMessageChannel & SuspendableWriteChannel> WritableMessageChannel.Result sendBlocking(C channel, ByteBuffer buffer, long time, TimeUnit unit) throws IOException {
        // todo - change to use a deadline instead...
        switch (channel.send(buffer)) {
            case OK: return WritableMessageChannel.OK;
            case PARTIAL: channel.awaitWritable(time, unit); return channel.flush() ? WritableMessageChannel.OK : WritableMessageChannel.PARTIAL;
            case NOT_SENT: channel.awaitWritable(time, unit); return channel.send(buffer);
        }
        throw new IllegalStateException();
    }

    /**
     * Simple utility method to execute a blocking gathering send on a message channel.  The method blocks until the channel
     * is writable, and then the message is written.
     *
     * @param channel the channel to write on
     * @param buffers the data to write
     * @param offs the index of the first buffer to write
     * @param len the number of buffers to write
     * @param <C> the channel type
     * @throws IOException if an I/O exception occurs
     * @since 1.2
     */
    public static <C extends WritableMessageChannel & SuspendableWriteChannel> void sendBlocking(C channel, ByteBuffer[] buffers, int offs, int len) throws IOException {
        WritableMessageChannel.Result result;
        do {
            if ((result = channel.send(buffers, offs, len)) == WritableMessageChannel.OK) return;
            channel.awaitWritable();
        } while (result == WritableMessageChannel.NOT_SENT);
        while (! channel.flush()) {
            channel.awaitWritable();
        }
    }

    /**
     * Simple utility method to execute a blocking gathering send on a message channel with a timeout.  The method blocks until the channel
     * is writable, and then the message is written.
     *
     * @param channel the channel to write on
     * @param buffers the data to write
     * @param offs the index of the first buffer to write
     * @param len the number of buffers to write
     * @param time the amount of time to wait
     * @param unit the unit of time to wait
     * @param <C> the channel type
     * @return {@code true} if the message was written before the timeout
     * @throws IOException if an I/O exception occurs
     * @since 1.2
     */
    public static <C extends WritableMessageChannel & SuspendableWriteChannel> WritableMessageChannel.Result sendBlocking(C channel, ByteBuffer[] buffers, int offs, int len, long time, TimeUnit unit) throws IOException {
        // todo - change to use a deadline instead...
        switch (channel.send(buffers, offs, len)) {
            case OK: return WritableMessageChannel.OK;
            case PARTIAL: channel.awaitWritable(time, unit); return channel.flush() ? WritableMessageChannel.OK : WritableMessageChannel.PARTIAL;
            case NOT_SENT: channel.awaitWritable(time, unit); return channel.send(buffers, offs, len);
        }
        throw new IllegalStateException();
    }

    /**
     * Simple utility method to execute a blocking read on a readable byte channel.  This method blocks until the
     * channel is readable, and then the message is read.
     *
     * @param channel the channel to read from
     * @param buffer the buffer into which bytes are to be transferred
     * @param <C> the channel type
     * @return the number of bytes read
     * @throws IOException if an I/O exception occurs
     * @since 1.2
     */
    public static <C extends ReadableByteChannel & SuspendableReadChannel> int readBlocking(C channel, ByteBuffer buffer) throws IOException {
        int res;
        while ((res = channel.read(buffer)) == 0) {
            channel.awaitReadable();
        }
        return res;
    }

    /**
     * Simple utility method to execute a blocking read on a readable byte channel with a timeout.  This method blocks until the
     * channel is readable, and then the message is read.
     *
     * @param channel the channel to read from
     * @param buffer the buffer into which bytes are to be transferred
     * @param time the amount of time to wait
     * @param unit the unit of time to wait
     * @param <C> the channel type
     * @return the number of bytes read
     * @throws IOException if an I/O exception occurs
     * @since 1.2
     */
    public static <C extends ReadableByteChannel & SuspendableReadChannel> int readBlocking(C channel, ByteBuffer buffer, long time, TimeUnit unit) throws IOException {
        int res = channel.read(buffer);
        if (res == 0) {
            channel.awaitReadable(time, unit);
            return channel.read(buffer);
        } else {
            return res;
        }
    }

    /**
     * Simple utility method to execute a blocking read on a scattering byte channel.  This method blocks until the
     * channel is readable, and then the message is read.
     *
     * @param channel the channel to read from
     * @param buffers the buffers into which bytes are to be transferred
     * @param offs the first buffer to use
     * @param len the number of buffers to use
     * @param <C> the channel type
     * @return the number of bytes read
     * @throws IOException if an I/O exception occurs
     * @since 1.2
     */
    public static <C extends ScatteringByteChannel & SuspendableReadChannel> long readBlocking(C channel, ByteBuffer[] buffers, int offs, int len) throws IOException {
        long res;
        while ((res = channel.read(buffers, offs, len)) == 0) {
            channel.awaitReadable();
        }
        return res;
    }

    /**
     * Simple utility method to execute a blocking read on a scattering byte channel with a timeout.  This method blocks until the
     * channel is readable, and then the message is read.
     *
     * @param channel the channel to read from
     * @param buffers the buffers into which bytes are to be transferred
     * @param offs the first buffer to use
     * @param len the number of buffers to use
     * @param time the amount of time to wait
     * @param unit the unit of time to wait
     * @param <C> the channel type
     * @return the number of bytes read
     * @throws IOException if an I/O exception occurs
     * @since 1.2
     */
    public static <C extends ScatteringByteChannel & SuspendableReadChannel> long readBlocking(C channel, ByteBuffer[] buffers, int offs, int len, long time, TimeUnit unit) throws IOException {
        long res = channel.read(buffers, offs, len);
        if (res == 0L) {
            channel.awaitReadable(time, unit);
            return channel.read(buffers, offs, len);
        } else {
            return res;
        }
    }

    /**
     * Simple utility method to execute a blocking receive on a readable message channel.  This method blocks until the
     * channel is readable, and then the message is received.
     *
     * @param channel the channel to read from
     * @param buffer the buffer into which bytes are to be transferred
     * @param <C> the channel type
     * @return the number of bytes read
     * @throws IOException if an I/O exception occurs
     * @since 1.2
     */
    public static <C extends ReadableMessageChannel & SuspendableReadChannel> int receiveBlocking(C channel, ByteBuffer buffer) throws IOException {
        int res;
        while ((res = channel.receive(buffer)) == 0) {
            channel.awaitReadable();
        }
        return res;
    }

    /**
     * Simple utility method to execute a blocking receive on a readable message channel with a timeout.  This method blocks until the
     * channel is readable, and then the message is received.
     *
     * @param channel the channel to read from
     * @param buffer the buffer into which bytes are to be transferred
     * @param time the amount of time to wait
     * @param unit the unit of time to wait
     * @param <C> the channel type
     * @return the number of bytes read
     * @throws IOException if an I/O exception occurs
     * @since 1.2
     */
    public static <C extends ReadableMessageChannel & SuspendableReadChannel> int receiveBlocking(C channel, ByteBuffer buffer, long time, TimeUnit unit) throws IOException {
        int res = channel.receive(buffer);
        if ((res) == 0) {
            channel.awaitReadable(time, unit);
            return channel.receive(buffer);
        } else {
            return res;
        }
    }

    /**
     * Simple utility method to execute a blocking receive on a readable message channel.  This method blocks until the
     * channel is readable, and then the message is received.
     *
     * @param channel the channel to read from
     * @param buffers the buffers into which bytes are to be transferred
     * @param offs the first buffer to use
     * @param len the number of buffers to use
     * @param <C> the channel type
     * @return the number of bytes read
     * @throws IOException if an I/O exception occurs
     * @since 1.2
     */
    public static <C extends ReadableMessageChannel & SuspendableReadChannel> long receiveBlocking(C channel, ByteBuffer[] buffers, int offs, int len) throws IOException {
        long res;
        while ((res = channel.receive(buffers, offs, len)) == 0) {
            channel.awaitReadable();
        }
        return res;
    }

    /**
     * Simple utility method to execute a blocking receive on a readable message channel with a timeout.  This method blocks until the
     * channel is readable, and then the message is received.
     *
     * @param channel the channel to read from
     * @param buffers the buffers into which bytes are to be transferred
     * @param offs the first buffer to use
     * @param len the number of buffers to use
     * @param time the amount of time to wait
     * @param unit the unit of time to wait
     * @param <C> the channel type
     * @return the number of bytes read
     * @throws IOException if an I/O exception occurs
     * @since 1.2
     */
    public static <C extends ReadableMessageChannel & SuspendableReadChannel> long receiveBlocking(C channel, ByteBuffer[] buffers, int offs, int len, long time, TimeUnit unit) throws IOException {
        long res = channel.receive(buffers, offs, len);
        if ((res) == 0) {
            channel.awaitReadable(time, unit);
            return channel.receive(buffers, offs, len);
        } else {
            return res;
        }
    }

}
