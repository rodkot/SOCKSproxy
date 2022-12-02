package ru.nsu.message;

import ru.nsu.AuthMode;
import ru.nsu.connext.SessionSocks;

import java.io.IOException;

public class MessageReader extends Message
{
    MessageReader(byte[] buff)
    {
        super(buff);
    }

    static public HelloMessage readHelloMessage(SessionSocks session) throws IOException {
        int read_bytes = session.getClientChannel().read(session.getReadBuffer());
        if (read_bytes == -1)
        {
            session.close();
            return null;
        }
        if (HelloMessage.isCorrectSizeOfMessage(session.getReadBuffer()))
        {
            session.setReadBuffer(session.getReadBuffer().flip());
            return new HelloMessage(session.getReadBuffer());
        }
        return null;
    }


    static public RequestMessage readRequestMessage(SessionSocks session) throws IOException
    {
        int read_bytes = session.getClientChannel().read(session.getReadBuffer());
        if (read_bytes == -1)
        {
            session.close();
            return null;
        }
        if (RequestMessage.isCorrectSizeOfMessage(session.getReadBuffer()))
        {
            session.setReadBuffer(session.getReadBuffer().flip());
            return new RequestMessage(session.getReadBuffer());
        }
        return null;
    }

    static public byte[] getResponse(HelloMessage hello, AuthMode authMode)
    {
        byte[] data = new byte[2];
        data[0] = SOCKS_5;
        if (!hello.hasMethod(authMode))
            data[1] = NO_ACCEPTABLE_METHODS;
        else
            data[1] = getCurrentMethod();

        return data;
    }
}