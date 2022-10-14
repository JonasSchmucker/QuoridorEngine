package org.example;

public class Square {
    public int getxPos() {
        return xPos;
    }

    public int getyPos() {
        return yPos;
    }
    private final int xPos, yPos;

    public Square(int xPos, int yPos){
        this.xPos = xPos;
        this.yPos = yPos;
    }

    public boolean isHere(int xPos, int yPos){
        return this.xPos == xPos && this.yPos == yPos;
    }

    @Override
    public boolean equals(Object s){
        if (s == this) {
            return true;
        }

        if (!(s instanceof Square)) {
            return false;
        }

        Square square = (Square) s;

        return this.xPos == square.xPos && this.yPos == square.yPos;
    }

    @Override
    public String toString(){
        StringBuilder result = new StringBuilder();
        result.append((char) (xPos + 'a'));
        result.append(yPos + 1);
        return result.toString();
    }

    @Override
    public int hashCode() {
        return xPos * 1024 + yPos;
    }
}
