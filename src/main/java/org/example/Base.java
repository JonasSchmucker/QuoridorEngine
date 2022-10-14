package org.example;

public class Base {

    protected static final String ESCAPED_CLEAR_COMMAND = "\033[H\033[2J";
    protected static final int MOVE_PENALTY = 1;
    protected static final int DEPTH = 2;
    protected static final String POSITION_PATTERN = "(([a-i][1-9]){0,20}) / (([a-i][1-9]){0,20}) / ([a-i][1-9]) ([a-i][1-9]) / (1?[0-9]) (1?[0-9]) / ([01])";
    protected static final int BOARD_SIZE = 9;
    protected static final int WALLS = BOARD_SIZE - 1;

    protected static final int ERROR_DISTANCE = BOARD_SIZE * BOARD_SIZE + 1;

    protected static final int ERROR_NOT_VISITED = Integer.MAX_VALUE;


    protected static final Square DEFAULT_SQUARE= new Square(0, 0);

    protected static final Move.direction DEFAULT_DIRECTION = Move.direction.N;

    protected static final boolean USE_DATABASE = false;



}
