package sslengine.utils;

import org.apache.log4j.Logger;

import java.io.*;

public class EncodeUtils {

    private static final Logger LOG = Logger.getLogger(EncodeUtils.class);

    public static Object toObject(byte[] bytes) {
        try (ByteArrayInputStream   bis = new ByteArrayInputStream(bytes);
             ObjectInput            in  = new ObjectInputStream(bis))
        {
            return in.readObject();
        } catch (ClassNotFoundException|IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] toBytes(Object object) {
        LOG.debug("toBytes: " + object);
        try {
            try (ByteArrayOutputStream  bos = new ByteArrayOutputStream();
                 ObjectOutput           out = new ObjectOutputStream(bos))
            {
                out.writeObject(object);
                return bos.toByteArray();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
