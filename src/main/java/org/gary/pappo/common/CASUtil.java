package org.gary.pappo.common;

import org.gary.pappo.client.ProxyHandler;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class CASUtil {

    private static long CONNECTFLAG;
    private static Unsafe unsafe;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
            Class<?> k = ProxyHandler.class;
            CONNECTFLAG = unsafe.objectFieldOffset
                    (k.getDeclaredField("connectFlag"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean cas(Object object, int oldValue, int newValue) {
        return unsafe.compareAndSwapInt(object, CONNECTFLAG, oldValue, newValue);
    }

}
