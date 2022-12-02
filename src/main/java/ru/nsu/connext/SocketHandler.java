package ru.nsu.connext;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface SocketHandler extends HandlerConnection {
    void read(SelectionKey key) throws IOException;
    void write(SelectionKey key) throws IOException;
}
