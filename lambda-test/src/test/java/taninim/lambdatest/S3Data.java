package taninim.lambdatest;

import module java.base;

record S3Data(byte[] data, Object object, Instant time) {

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + (data == null ? "0" : data.length) + "->" + object + "]";
    }
}

