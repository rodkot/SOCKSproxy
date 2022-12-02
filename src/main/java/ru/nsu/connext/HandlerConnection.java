package ru.nsu.connext;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface HandlerConnection {
    void close() throws IOException;
    void accept(SelectionKey key);
}