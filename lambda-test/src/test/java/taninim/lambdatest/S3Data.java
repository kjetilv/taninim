package taninim.lambdatest;

import module java.base;

record S3Data(byte[] data, String str, Instant time) {

    @Override
    public String toString() {
        return getClass().getSimpleName() +
               "[data:" + (data == null ? "null" : data.length) +
               " str:" + (str == null ? "null": str.length()) +
               "]";
    }
}

