package sslengine.utils;

import java.io.*;

public class EncodeUtils {

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
