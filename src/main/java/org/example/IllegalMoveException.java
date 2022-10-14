package org.example;

public class IllegalMoveException extends Exception {
    private final String info;

    public IllegalMoveException(String info){
        this.info = info;
    }

    @Override
    public String toString(){
        return info;
    }
}
