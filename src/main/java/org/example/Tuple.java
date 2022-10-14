package org.example;

public class Tuple<A, B> {
    A value;
    B code;

    public Tuple(A value, B code) {
        this.value = value;
        this.code = code;
    }
}
