package ru.nsu;

import ru.nsu.connext.HandlerConnection;
import ru.nsu.connext.Server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;
import static ru.nsu.LoggerApp.*;

public class Proxy implements AutoCloseable, Runnable
{
    private final Selector selector = Selector.open();
    private final Server server;

    public Proxy(int port) throws IOException
    {
        server = new Server(port, selector);
    }

    @Override
    public void close() throws Exception
    {
        selector.close();
        server.close();
        server.closeDNS();
    }

    @Override
    public void run()
    {
        LOGGER.info("Proxy START");
        while (!Thread.currentThread().isInterrupted())
        {
            int count = 0;
            try
            {
                count = selector.select(10000);
            } catch (Exception ex)
            {
                ex.printStackTrace();
            }
            if (count == 0)
                continue;

            Set<SelectionKey> modified = selector.selectedKeys();
            for (SelectionKey selected : modified)
            {
                HandlerConnection key = (HandlerConnection) selected.attachment();
                key.accept(selected);
            }
            modified.clear();
        }

        try
        {
            close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
