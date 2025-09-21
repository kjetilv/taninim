package taninim.fb;

import module java.base;

public interface FbListener {

    default void response(String userName, String id, Duration timeout) {
    }

    default void allowed(String userName, String id) {

    }

    default void disallowed(String userName, String id) {
    }
}
