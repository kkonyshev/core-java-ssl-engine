package sslengine;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import sslengine.utils.EncodeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

@RunWith(value = Parameterized.class)
public class EncodeUtilsTest {

    private Object object;

    public EncodeUtilsTest(Object object){
        this.object = object;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] {
                {
                        UUID.randomUUID().toString()
                },
                {
                        "short string here"
                },
                {
                        Long.MAX_VALUE
                },
                {
                        new ArrayList<>()
                }
            };
        return Arrays.asList(data);
    }

    @Test
    public void thereAndBackAgainTest() {
        Assert.assertEquals(object, EncodeUtils.toObject(EncodeUtils.toBytes(object)));
    }
}
