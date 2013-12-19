package com.jobscience.search;

import java.nio.ByteBuffer;
import java.util.UUID;

public  class Utils {
    private Utils() { }

    public static String shortUUID() {
        UUID uuid = UUID.randomUUID();
        long l = ByteBuffer.wrap(uuid.toString().getBytes()).getLong();
        return Long.toString(l, Character.MAX_RADIX);
    }

    public static String demoSfid(){
        return shortUUID();
    }
}
