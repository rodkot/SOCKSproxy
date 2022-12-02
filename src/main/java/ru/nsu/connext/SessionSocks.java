package ru.nsu.connext;

import lombok.Getter;
import lombok.Setter;
import ru.nsu.AuthMode;
import ru.nsu.message.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import static ru.nsu.LoggerApp.*;

public class SessionSocks implements SocketHandler
{
    private final AuthMode authMode;

    private SocketChannel serverChannel = null;

    @Getter
    private final SocketChannel clientChannel;

    private final Dns dns;
    private static final int BUFFER_SIZE = 4096;

    @Getter
    @Setter
    private ByteBuffer readBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private ByteBuffer writeBuffer = null;

    private HelloMessage hello = null;
    private RequestMessage request = null;

    private State state = State.HELLO;
    private enum State
    {
        HELLO,
        REQUEST,
        MESSAGE
    }



    public SessionSocks(SocketChannel socketChannel, Dns dns, Selector selector, AuthMode authMode) throws IOException
    {
        this.dns = dns;
        this.clientChannel = socketChannel;
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ, this);
        this.authMode = authMode;
    }

    @Override
    public void accept(SelectionKey key)
    {
        try
        {
            if (!key.isValid())
            {
                close();
                key.cancel();
                return;
            }
            if (key.isReadable())
                read(key);
            else if (key.isWritable())
                write(key);
            else if (key.isConnectable() && key.channel() == serverChannel)
                serverConnect(key);
        } catch (IOException ex)
        {
            try
            {
                close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() throws IOException
    {
        if (clientChannel != null)
            clientChannel.close();
        if (serverChannel != null)
            serverChannel.close();
    }

    @Override
    public void read(SelectionKey key) throws IOException
    {
        if (key.channel() == clientChannel)
            clientRead(key);
        else if (key.channel() == serverChannel)
            serverRead(key);
    }

    private void clientRead(SelectionKey key) throws IOException
    {
        switch (state)
        {
            case HELLO:
            {
                LOGGER.info("clientRead HELLO " + clientChannel.socket().toString());
                hello = MessageReader.readHelloMessage(this);
                if (hello == null) return;
                key.interestOps(SelectionKey.OP_WRITE);

                readBuffer.clear();
                break;
            }

            case REQUEST:
            {
                LOGGER.info("clientRead REQUEST " + clientChannel.socket().toString());
                request = MessageReader.readRequestMessage(this);
                if (request == null) return;
                if (!connect())
                {
                    serverChannel = null;
                    key.interestOps(SelectionKey.OP_WRITE);
                } else
                {
                    serverChannel.register(key.selector(), SelectionKey.OP_CONNECT, this);
                    key.interestOps(0);
                }
                readBuffer.clear();
                break;
            }
            case MESSAGE:
            {
                LOGGER.info("clientRead MESSAGE "+ clientChannel.socket().toString());
                if (this.readFrom(clientChannel, readBuffer))
                {
                    serverChannel.register(key.selector(), SelectionKey.OP_WRITE, this);
                    key.interestOps(0);
                }
                break;
            }
        }
    }

    private void serverRead(SelectionKey key) throws IOException
    {
        if (readFrom(serverChannel, readBuffer))
        {
            clientChannel.register(key.selector(), SelectionKey.OP_WRITE, this);
            key.interestOps(0);
        }
    }

    private void serverConnect(SelectionKey key) throws IOException
    {
        if (!serverChannel.isConnectionPending())
            return;
        if (!serverChannel.finishConnect())
            return;

        key.interestOps(0);
        clientChannel.register(key.selector(), SelectionKey.OP_WRITE, this);
    }

    @Override
    public void write(SelectionKey key) throws IOException
    {
        if (key.channel() == clientChannel)
            clientWrite(key);
        else if (key.channel() == serverChannel)
            serverWrite(key);
    }

    private void clientWrite(SelectionKey key) throws IOException {
        switch (state)
        {
            case HELLO:
            {
                if (writeBuffer == null)
                {
                    writeBuffer = ByteBuffer.wrap(MessageReader.getResponse(hello, authMode));
                }
                if (writeTo(clientChannel, writeBuffer))
                {
                    writeBuffer = null;
                    if (hello.hasMethod(authMode))
                    {
                        key.interestOps(SelectionKey.OP_READ);
                        state =   State.REQUEST;

                    } else
                    {
                        LOGGER.error("clientWrite NOT SUPPORT METHOD! " + clientChannel.socket().toString() + " closed");
                        this.close();
                    }
                    hello = null;
                }
                break;
            }
            case REQUEST:
            {
                if (writeBuffer == null)
                {
                    ResponseMessage response = new ResponseMessage(request);
                    writeBuffer = ByteBuffer.wrap(response.create(serverChannel != null)); //
                }
                if (writeTo(clientChannel, writeBuffer))
                {
                    writeBuffer = null;
                    if (request.isCommand(RequestMessage.CONNECT_TCP) || serverChannel == null)
                    {
                        this.close();
                        LOGGER.warn("clientWrite NOT TCP REQUEST " + clientChannel.socket().toString());
                    } else
                    {
                        key.interestOps(SelectionKey.OP_READ);
                        serverChannel.register(key.selector(), SelectionKey.OP_READ, this);
                        state = State.MESSAGE;
                    }
                    request = null;
                }
                break;
            }
            case MESSAGE:
            {
                if (writeTo(clientChannel, readBuffer))
                {
                    key.interestOps(SelectionKey.OP_READ);
                    serverChannel.register(key.selector(), SelectionKey.OP_READ, this);
                }
                break;
            }
        }
    }

    private void serverWrite(SelectionKey key) throws IOException
    {
        if (writeTo(serverChannel, readBuffer))
        {
            key.interestOps(SelectionKey.OP_READ);
            clientChannel.register(key.selector(), SelectionKey.OP_READ, this);
        }
    }

    public boolean connectToServer(InetAddress address)
    {
        try
        {
            serverChannel.connect(new InetSocketAddress(address, request.getDestPort()));
            LOGGER.info(" connectToServer CONNECTED to " + serverChannel.getRemoteAddress().toString());
        } catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean connect() throws IOException
    {
        serverChannel = SocketChannel.open();
        serverChannel.configureBlocking(false);
        switch (request.getAddressType())
        {
            case RequestMessage.IPv4:
                return connectToServer(InetAddress.getByAddress(request.getDestAddress()));
            case RequestMessage.IPv6:
            {
                LOGGER.error("connect NOT SUPPORT IPV6 client "+ clientChannel.socket().toString());
                return false;
            }
            case RequestMessage.DOMAIN_NAME:
            {
                dns.sendToResolve(new String(request.getDestAddress()), this);
                break;
            }
        }
        return true;
    }

    private boolean readFrom(SocketChannel channel, ByteBuffer buffer) throws IOException
    {
        buffer.compact();
        int read_bytes = channel.read(buffer);
        if (read_bytes == -1)
        {
            this.close();
            return false;
        }
        if (read_bytes != 0)
            buffer.flip();

        return read_bytes != 0;
    }

    private boolean writeTo(SocketChannel channel, ByteBuffer buffer) throws IOException
    {
        channel.write(buffer);
        return !buffer.hasRemaining();
    }
}