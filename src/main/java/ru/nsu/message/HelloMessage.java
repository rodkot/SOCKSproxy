package ru.nsu.message;

import ru.nsu.AuthMode;

import java.nio.ByteBuffer;

public class HelloMessage extends Message {
    public HelloMessage(ByteBuffer buffer)
    {
        super(new byte[buffer.limit()]);
        buffer.get(data);
        if (data[1] + 2 != data.length)
            throw new IllegalArgumentException();
    }

    public boolean hasMethod(AuthMode authMode)
    {
        byte curMethod = getCurrentMethod();
        for (int i = 0; i < data[1]; ++i)
            if (curMethod == data[i + 2])
                return true;

        return false;

    }

    public static boolean isCorrectSizeOfMessage(ByteBuffer data) {
        return data.position() > 1 && data.position() >= 2 + data.get(1);
    }
}