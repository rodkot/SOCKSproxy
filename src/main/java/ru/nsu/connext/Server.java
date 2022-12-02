package ru.nsu.connext;

import ru.nsu.AuthMode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

import static ru.nsu.LoggerApp.*;

public class Server implements HandlerConnection {

    private final AuthMode authMode = AuthMode.NO_AUTH;
    private final ServerSocketChannel serverChannel = ServerSocketChannel.open();
    private final Dns dns;

    public Server(int port, Selector selector) throws IOException {
        LOGGER.info("CREATE SERVER on port: " + port);
        dns = new Dns(port, selector);
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT, this);

    }

    public void closeDNS() throws IOException {
        dns.close();
    }

    @Override
    public void close() throws IOException {
        serverChannel.close();
    }

    @Override
    public void accept(SelectionKey key) {
        try {
            if (!key.isValid()) {
                close();
                return;
            }
            new SessionSocks(serverChannel.accept(), dns, key.selector(), authMode);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }
}