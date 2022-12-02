package ru.nsu.connext;


import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static ru.nsu.LoggerApp.LOGGER;


public class Dns implements SocketHandler {
    private final DatagramChannel resolverChannel = DatagramChannel.open();
    private final InetSocketAddress DnsServerAddr;

    private final ByteBuffer readBuffer = ByteBuffer.allocateDirect(Message.MAXLENGTH);
    private final ByteBuffer writeBuffer = ByteBuffer.allocateDirect(Message.MAXLENGTH);

    private final SelectionKey key;

    private final Deque<Message> deque = new LinkedList<>();

    private final Map<Integer, SessionSocks> attachments = new HashMap<>();

    public Dns(int port, Selector selector) throws IOException
    {

        resolverChannel.configureBlocking(false);
        resolverChannel.register(selector, 0, this);
        key = resolverChannel.keyFor(selector);
        resolverChannel.bind(new InetSocketAddress(port));
        DnsServerAddr = ResolverConfig.getCurrentConfig().server();
        resolverChannel.connect(DnsServerAddr);
        readBuffer.clear();
        writeBuffer.clear();
        LOGGER.info("CREATE DNS connect with dns servers: "+DnsServerAddr.toString());

    }

    public void sendToResolve(String domainName, SessionSocks handler)
    {
        try
        {
            Message dnsRequest = Message.newQuery(Record.newRecord(new Name(domainName + '.'), Type.A, DClass.IN));
            deque.addLast(dnsRequest);
            attachments.put(dnsRequest.getHeader().getID(), handler);
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        } catch (TextParseException ex)
        {
            ex.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException
    {
        resolverChannel.close();
    }

    @Override
    public void accept(SelectionKey key)
    {
        try
        {
            if (!key.isValid())
            {
                this.close();
                key.cancel();
                return;
            }
            if (key.isReadable())
                read(key);
            else if (key.isWritable())
                write(key);
        } catch (IOException ex)
        {
            ex.printStackTrace();
            try
            {
                this.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void read(SelectionKey key) throws IOException
    {
        if (resolverChannel.receive(readBuffer) != null)
        {
            readBuffer.flip();
            byte[] data = new byte[readBuffer.limit()];
            readBuffer.get(data);
            readBuffer.clear();
            Message response = new Message(data);
            SessionSocks session = attachments.remove(response.getHeader().getID());
            for (Record record : response.getSection(Section.ANSWER))
                if (record instanceof ARecord)
                {
                    ARecord it = (ARecord) record;
                    if (session.connectToServer(it.getAddress()))
                        break;
                }
        }
        if (attachments.isEmpty())
            key.interestOps(key.interestOps() ^ SelectionKey.OP_READ);
    }

    @Override
    public void write(SelectionKey key) throws IOException
    {
        Message dnsRequest = deque.pollFirst();
        while (dnsRequest != null)
        {
            writeBuffer.clear();
            writeBuffer.put(dnsRequest.toWire());
            writeBuffer.flip();
            if (resolverChannel.send(writeBuffer, DnsServerAddr) == 0)
            {
                deque.addFirst(dnsRequest);
                break;
            }
            key.interestOps(key.interestOps() | SelectionKey.OP_READ);
            dnsRequest = deque.pollFirst();
        }
        key.interestOps(key.interestOps() ^ SelectionKey.OP_WRITE);
    }
}