package org.example;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class Board {

    private Square posPlayer0, posPlayer1;
    private int wallsLeftPlayer0;
    private int wallsLeftPlayer1;

    private boolean [][] horizontalWalls;
    private boolean [][] verticalWalls;

    private boolean player0Active;

    private int [][] currentDistancePlayer0;
    private int [][] currentDistancePlayer1;
    private Map<Board, Integer> database;
    private Move [][] currentContinuation;


    LinkedList<Move> moves = new LinkedList<>();


    public Board(){
        horizontalWalls = new boolean[Base.WALLS][Base.WALLS];
        verticalWalls = new boolean[Base.WALLS][Base.WALLS];
        currentDistancePlayer0 = new int[Base.BOARD_SIZE][Base.BOARD_SIZE];
        currentDistancePlayer1 = new int[Base.BOARD_SIZE][Base.BOARD_SIZE];
        currentContinuation = new Move[Base.DEPTH][Base.DEPTH];
        database = new HashMap<>();
        for(int i = 0; i < Base.WALLS; i++){
            for(int o = 0; o < Base.WALLS; o++) {
                horizontalWalls[i][o] = false;
                verticalWalls[i][o] = false;
            }
        }
    }

    public Tuple<Move [], Integer> bestMove(){
        database.clear();
        int value =  bestMoveRecursive(0, player0Active, Integer.MIN_VALUE, Integer.MAX_VALUE);
        return new Tuple<>(currentContinuation[0], value);
    }
    private int bestMoveRecursive(int depth, boolean isMaximizingPlayer, int alpha, int beta) {
        if(gameOver()){
            int value = ratePosition();
            database.put(this, value);
            return value;
        }
        if(Base.USE_DATABASE && database.containsKey(this)){
            int value = database.get(this);
            return value;
        }
        if(depth == Base.DEPTH) {
            int value = ratePosition();
            database.put(this, value);
            return value;
        }
        int bestVal;
        int value;
        List<Move> possibleMoves  = possibleMoves();

        if(isMaximizingPlayer) {
            bestVal = Integer.MIN_VALUE;
            for (Move move : possibleMoves) {
                makeMove(move);
                value = bestMoveRecursive(depth + 1, false, alpha, beta);
                undoMove();
                value = calculateMovePenalty(value);
                bestVal = max(bestVal, value);
                alpha = max(alpha, bestVal);
                if(value == bestVal){
                    currentContinuation[depth][depth] = move;
                    for(int i = depth + 1; i < Base.DEPTH; i++) {
                        currentContinuation[depth][i] = currentContinuation[depth + 1][i];
                    }
                }
                if (beta <= alpha) {
                    break;
                }
            }
        }
        else {
            bestVal = Integer.MAX_VALUE;
            for (Move move : possibleMoves) {
                makeMove(move);
                value = bestMoveRecursive(depth + 1, true, alpha, beta);
                undoMove();
                value = calculateMovePenalty(value);
                bestVal = min(bestVal, value);
                beta = min(beta, bestVal);
                if(value == bestVal){
                    currentContinuation[depth][depth] = move;
                    for(int i = depth + 1; i < Base.DEPTH; i++) {
                        currentContinuation[depth][i] = currentContinuation[depth + 1][i];
                    }
                }
                if(beta <= alpha){
                    break;
                }
            }
        }
        database.put(this, bestVal);
        return bestVal;
    }

    private int calculateMovePenalty(int value) {
        if(value < Integer.MIN_VALUE / 2){
            return value + Base.MOVE_PENALTY;
        }
        else if (value > Integer.MAX_VALUE / 2){
            return value - Base.MOVE_PENALTY;
        }
        return value;
    }

    public void makeMoveSecurely(Move move){
        try {
            isLegal(move);
        } catch (IllegalMoveException e) {
            System.out.println(e.toString());
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        makeMove(move);
    }

    private void makeMove(Move move) {
        if(move.wall){
            if(move.horizontal) {
                setHorizontalWalls(move.wallBottomLeft.getxPos(), move.wallBottomLeft.getyPos(), true);
            }
            else{
                setVerticalWalls(move.wallBottomLeft.getxPos(), move.wallBottomLeft.getyPos(), true);
            }
            if(player0Active){
                wallsLeftPlayer0--;
            }
            else{
                wallsLeftPlayer1--;
            }
        }
        else{
            if(player0Active){
                posPlayer0 = move.end;
            }
            else{
                posPlayer1 = move.end;
            }
        }
        player0Active = !player0Active;
        moves.add(move);
        updateDistance();
    }

    private boolean isLegal(@NotNull Move move) throws IllegalMoveException {
        Tuple<Boolean, String> result;
        if(move.wall){
            result = canPutWallAt(move.wallBottomLeft.getxPos(), move.wallBottomLeft.getyPos(), move.horizontal);
        }
        else{
            Square start = player0Active ? posPlayer0 : posPlayer1;
            result = canMoveInDirection(start, move.dir, move);

        }
        if(!result.value){
            throw new IllegalMoveException(result.code);
        }
        return true;
    }

    public void undoMove() {
        Move move = moves.removeLast();

        if (move.wall) {
            if (player0Active) {
                wallsLeftPlayer1++;
            } else {
                wallsLeftPlayer0++;
            }
            if(move.horizontal){
                setHorizontalWalls(move.wallBottomLeft.getxPos(), move.wallBottomLeft.getyPos(), false);
            }
            else{
                setVerticalWalls(move.wallBottomLeft.getxPos(), move.wallBottomLeft.getyPos(), false);
            }
        }
        else {
            if (player0Active) {
                posPlayer1 = move.start;
            } else {
                posPlayer0 = move.start;
            }
        }
        player0Active = !player0Active;
        updateDistance();
    }

    private int ratePosition(){
        if(player0Won()){
            return Integer.MAX_VALUE;
        }
        else if(player1Won()){
            return Integer.MIN_VALUE;
        }
        return wallsLeftPlayer0 - wallsLeftPlayer1 - distance(true) + distance(false);
    }

    public boolean gameOver(){
        return player0Won() || player1Won();
    }

    private boolean player1Won() {
        return posPlayer1.getyPos() == 0;
    }

    private boolean player0Won() {
        return posPlayer0.getyPos() == Base.BOARD_SIZE - 1;
    }

    public int distance(boolean player0) {
        return player0 ? currentDistancePlayer0[posPlayer0.getxPos()][posPlayer0.getyPos()] : currentDistancePlayer1[posPlayer1.getxPos()][posPlayer1.getyPos()];
    }

    private void updateDistance() {
        for(int i = 0; i < Base.BOARD_SIZE; i++){
            for(int o = 0; o < Base.BOARD_SIZE; o++) {
                currentDistancePlayer0[i][o] = Base.ERROR_NOT_VISITED;
                currentDistancePlayer1[i][o] = Base.ERROR_NOT_VISITED;
                if(o == 0){
                    currentDistancePlayer1[i][o] = 0;
                } else if(o == Base.BOARD_SIZE - 1) {
                    currentDistancePlayer0[i][o] = 0;
                }
            }
        }
        updateDistanceRecursive(posPlayer0, Base.ERROR_DISTANCE, currentDistancePlayer0);
        updateDistanceRecursive(posPlayer1, Base.ERROR_DISTANCE, currentDistancePlayer1);
    }
    private int updateDistanceRecursive(Square square, int lowestCurrentDistance, int [][] currentDistance) {
        if(currentDistance[square.getxPos()][square.getyPos()] != Base.ERROR_NOT_VISITED && currentDistance[square.getxPos()][square.getyPos()] <= lowestCurrentDistance){
            return currentDistance[square.getxPos()][square.getyPos()];
        }
        currentDistance[square.getxPos()][square.getyPos()] = lowestCurrentDistance;
        List<Tuple<Square, Move.direction>> squares = possibleSquaresWithPiece(square);
        int lowest = lowestCurrentDistance;
        for(Tuple<Square, Move.direction> s : squares){
            lowest = min(updateDistanceRecursive(s.value, lowest + 1, currentDistance) + 1, lowest);
        }
        currentDistance[square.getxPos()][square.getyPos()] = lowest;
        return lowest;
    }

    private List<Move> possibleMoves() {
        List<Move> result = possibleMovesWithPiece();
        if((player0Active ? wallsLeftPlayer0 : wallsLeftPlayer1) > 0){
            result = possibleMovesWithWall(result);
        }
        return result;
    }

    private List<Move> possibleMovesWithWall(List<Move> result) {
        boolean top, right, bottom, left, hereHorizontal, hereVertical;


        for(int o = 0; o < Base.WALLS; o++) {
            right = isHorizontalWall(0, o);
            hereHorizontal = isHorizontalWall(-1 ,o);
            for(int i = 0; i < Base.WALLS; i++){
                hereVertical = isVerticalWall(i, o);
                left = hereHorizontal;
                hereHorizontal = right;
                right = isHorizontalWall(i + 1, o);
                top = isVerticalWall(i, o + 1);
                bottom = isVerticalWall(i, o - 1);
                if(!hereVertical && !hereHorizontal && !left && !right){
                    Move move = Move.Builder.newInstance()
                            .setDir(Base.DEFAULT_DIRECTION)
                            .setHorizontal(true)
                            .setJump(false)
                            .setStart(Base.DEFAULT_SQUARE)
                            .setEnd(Base.DEFAULT_SQUARE)
                            .setJumpDir(Base.DEFAULT_DIRECTION)
                            .setWall(true)
                            .setWallBottomLeft(new Square(i, o))
                            .build();
                    makeMove(move);
                    if(distance(true) != Base.ERROR_DISTANCE && distance(false) != Base.ERROR_DISTANCE) {
                        result.add(
                                move
                        );
                    }
                    undoMove();
                }
                else if(!hereVertical && !hereHorizontal && !top && !bottom){
                    Move move = Move.Builder.newInstance()
                            .setDir(Base.DEFAULT_DIRECTION)
                            .setHorizontal(false)
                            .setJump(false)
                            .setStart(Base.DEFAULT_SQUARE)
                            .setEnd(Base.DEFAULT_SQUARE)
                            .setJumpDir(Base.DEFAULT_DIRECTION)
                            .setWall(true)
                            .setWallBottomLeft(new Square(i, o))
                            .build();
                    makeMove(move);
                    if(distance(true) != Base.ERROR_DISTANCE && distance(false) != Base.ERROR_DISTANCE) {
                        result.add(
                                move
                        );
                    }
                    undoMove();
                }
            }
        }

        return result;
    }

    private List<Tuple<Square, Move.direction>> possibleSquaresWithPiece(Square start){
        List<Tuple<Square, Move.direction>> result = new ArrayList<>();
        for(Move.direction direction : Move.direction.values()){
            if(canMoveInDirection(start, direction, null).value){
                switch (direction){
                    case N:
                        result.add(new Tuple<>(new Square(start.getxPos(), start.getyPos() + 1), direction));
                        break;
                    case E:
                        result.add(new Tuple<>(new Square(start.getxPos() + 1, start.getyPos()), direction));
                        break;
                    case S:
                        result.add(new Tuple<>(new Square(start.getxPos(), start.getyPos() - 1), direction));
                        break;
                    case W:
                        result.add(new Tuple<>(new Square(start.getxPos() - 1, start.getyPos()), direction));
                        break;
                    default:
                        result.add(new Tuple<>(Base.DEFAULT_SQUARE, Base.DEFAULT_DIRECTION));
                        System.out.println("ERROR");

                }
            }
        }
        return result;
    }

    private List<Move> possibleMovesWithPiece(){
        Square start = player0Active ? posPlayer0 : posPlayer1;
        List<Tuple<Square, Move.direction>> squares = possibleSquaresWithPiece(start);
        List<Move> result = new ArrayList<>();
        squares.forEach((Tuple<Square, Move.direction> s) -> {
            result.add(
                    Move.Builder.newInstance()
                            .setDir(s.code)
                            .setHorizontal(false)
                            .setJump(false)
                            .setStart(start)
                            .setEnd(s.value)
                            .setJumpDir(Base.DEFAULT_DIRECTION)
                            .setWall(false)
                            .setWallBottomLeft(Base.DEFAULT_SQUARE)
                            .build()
            );
        });
        return result;
    }

    public void buildPosition(String pos){
        System.out.println(pos);
        Pattern pattern = Pattern.compile(Base.POSITION_PATTERN);
        Matcher matcher = pattern.matcher(pos);
        boolean matchFound = matcher.find();
        if(matchFound) {
            String horizontal = matcher.group(1);
            String vertical = matcher.group(3);
            String player0Pos = matcher.group(5);
            String player1Pos = matcher.group(6);

            wallsLeftPlayer0 = Integer.parseInt(matcher.group(7));
            wallsLeftPlayer1 = Integer.parseInt(matcher.group(8));

            player0Active = matcher.group(9).compareTo("0") == 0;

            for(int i = 0; i + 1 < horizontal.length(); i += 2){
                // a equals 0

                setHorizontalWalls(horizontal.charAt(i) - 'a', (Character.digit(horizontal.charAt(i + 1), 10) - 1), true);
            }

            for(int i = 0; i + 1 < vertical.length(); i += 2){
                // a equals 0
                setVerticalWalls(horizontal.charAt(i) - 'a', (Character.digit(horizontal.charAt(i + 1), 10) - 1), true);
            }

            posPlayer0 = new Square(player0Pos.charAt(0) - 'a', Character.digit(player0Pos.charAt(1), 10) - 1);


            posPlayer1 = new Square(player1Pos.charAt(0) - 'a', Character.digit(player1Pos.charAt(1), 10) - 1);

        }
        else {
              System.out.println("Error reading position");
        }
        updateDistance();
    }

    @Override
    public String toString(){
        StringBuilder result = new StringBuilder();

        for(int i = 0; i < horizontalWalls.length; i++){
            for(int o = 0; o < horizontalWalls[i].length; o++) {
                if(horizontalWalls[i][o]){
                    result.append((char)('a' + i));
                    result.append(o + 1);
                }
            }
        }

        result.append(' ');
        result.append('/');
        result.append(' ');

        for(int i = 0; i < verticalWalls.length; i++){
            for(int o = 0; o < verticalWalls[i].length; o++) {
                if(verticalWalls[i][o]){
                    result.append((char)('a' + i));
                    result.append(o + 1);
                }
            }
        }

        result.append(' ');
        result.append('/');
        result.append(' ');

        result.append(posPlayer0.toString());

        result.append(' ');

        result.append(posPlayer1.toString());

        result.append(' ');
        result.append('/');
        result.append(' ');

        result.append(wallsLeftPlayer0);

        result.append(' ');

        result.append(wallsLeftPlayer1);

        result.append(' ');
        result.append('/');
        result.append(' ');

        result.append(player0Active ? '0' : '1');

        return result.toString();
    }

    public void cliOutput(){
        StringBuilder result = new StringBuilder();
        result
                .append("\t\t\t\t\t\t\t[N]\n")
                .append("\n")
                .append("\t\t\t  A\t  B\t  C\t  D\t  E\t  F\t  G\t  H\t  I\n")
                .append("\t\t\t  |\t  |\t  |\t  |\t  |\t  |\t  |\t  |\t  |\n")
                .append("\t\t\t+-----------------------------------+\n");
        for(int o = Base.WALLS; o > -1; o--){
            if(o != Base.WALLS){
                result.append("\t\t\t|");
                for(int i = 0; i < Base.BOARD_SIZE; i++){
                    if(i != 0){
                        if (horizontalWalls[i - 1][o]) {
                            result.append("--");
                        }
                    }
                    if(i != Base.WALLS) {
                        if (horizontalWalls[i][o]) {
                            result.append("--");
                        }
                        result.append("\t+");
                    }
                }
                result.append("\t|\n");
            }

            result.append("\t\t" + (o + 1) + "--\t|");

            for(int i = 0; i < Base.WALLS; i++){
                if(posPlayer1.isHere(i, o)){
                    result.append(" X");
                } else if (posPlayer0.isHere(i, o)) {
                    result.append(" O");
                }
                result.append("\t");
                if(o != 0){
                    if(verticalWalls[i][o - 1]){
                        result.append("|");
                    }
                }
                if(o != Base.WALLS){
                    if(verticalWalls[i][o]){
                        result.append("|");
                    }
                }
            }
            if(posPlayer1.isHere(Base.WALLS, o)){
                result.append("  X");
            } else if (posPlayer0.isHere(Base.WALLS, o)) {
                result.append("  O");
            }

            result.append("\t|--" + (o + 1) + "\n");
        }
        result
                .append("\t\t\t+-----------------------------------+\n")
                .append("\t\t\t  |\t  |\t  |\t  |\t  |\t  |\t  |\t  |\t  |\n")
                .append("\t\t\t  A\t  B\t  C\t  D\t  E\t  F\t  G\t  H\t  I\n")
                .append("\t\t\t\t(O) Player 0: ").append(wallsLeftPlayer0).append(" wall(s) left").append(player0Active ? " [active]" : "").append("\n")
                .append("\t\t\t\t(X) Player 1: ").append(wallsLeftPlayer1).append(" wall(s) left").append(player0Active ? "" : " [active]").append("\n")
                .append("\t\t\t\t\t\t\t[S]");

        System.out.println(result.toString());
    }

    private Tuple<Boolean, String> canPutWallAt(int x, int y, boolean horizontal){
        if(player0Active) {
            if (wallsLeftPlayer0 == 0) {
                return new Tuple<>(false, "Player 0 has no more walls left");
            }
        }
        else{
            if (wallsLeftPlayer1 == 0) {
                return new Tuple<>(false, "Player 1 has no more walls left");
            }
        }

        if(isVerticalWall(x, y)) {
            return new Tuple<>(false, "There is already a vertical wall at this position");
        } else if(isHorizontalWall(x, y)){
            return new Tuple<>(false, "There is already a horizontal wall at this position");
        }

        if(horizontal){
            if(isHorizontalWall(x + 1, y)) {
                return new Tuple<>(false, "There is already a wall to the right of this position");
            } else if(isHorizontalWall(x - 1, y)) {
                return new Tuple<>(false, "There is already a wall to the left of this position");
            } else {
                return new Tuple<>(true, "");
            }
        }
        else{
            if(isVerticalWall(x, y + 1)) {
                return new Tuple<>(false, "There is already a wall above this position");
            } else if(isVerticalWall(x, y - 1)) {
                return new Tuple<>(false, "There is already a wall below this position");
            } else {
                return new Tuple<>(true, "");
            }
        }
    }

    private Tuple<Boolean, String> canMoveInDirection(Square start, Move.direction direction, Move move){


        if(move != null && !start.equals(move.start)){
            return new Tuple<>(false, "Player is not at that position");
        }

        switch (direction){
            case N:
                if(start.getyPos() == Base.WALLS){
                    return new Tuple<>(false, "already at the top most position");
                }
                else if(
                        isHorizontalWall(start.getxPos(), start.getyPos()) ||
                                isHorizontalWall(start.getxPos() - 1, start.getyPos())
                ){
                    return new Tuple<>(false, "Wall obstructing move");
                }
                if(move != null && (start.getxPos() != move.end.getxPos() || start.getyPos() + 1 != move.end.getyPos())){
                    return new Tuple<>(false, "TargetSquare incorrect");
                }
                else{
                    return new Tuple<>(true, "");
                }
            case E:
                if(start.getxPos() == Base.WALLS){
                    return new Tuple<>(false, "already at the right most position");
                }
                else if(
                        isVerticalWall(start.getxPos(), start.getyPos()) ||
                                isVerticalWall(start.getxPos(), start.getyPos() - 1)
                ){
                    return new Tuple<>(false, "Wall obstructing move");
                }
                if(move != null && (start.getxPos() + 1 != move.end.getxPos() || start.getyPos() != move.end.getyPos())){
                    return new Tuple<>(false, "TargetSquare incorrect");
                }
                else{
                    return new Tuple<>(true, "");
                }
            case S:
                if(start.getyPos() == 0){
                    return new Tuple<>(false, "already at the bottom most position");
                }
                else if(
                        isHorizontalWall(start.getxPos(), start.getyPos() - 1) ||
                                isHorizontalWall(start.getxPos() - 1, start.getyPos() - 1)
                ){
                    return new Tuple<>(false, "Wall obstructing move");
                }
                if(move != null && (start.getxPos() != move.end.getxPos() || start.getyPos() - 1 != move.end.getyPos())){
                    return new Tuple<>(false, "TargetSquare incorrect");
                }
                else{
                    return new Tuple<>(true, "");
                }
            case W:
                if(start.getxPos() == 0){
                    return new Tuple<>(false, "already at the left most position");
                }
                else if(
                        isVerticalWall(start.getxPos() - 1, start.getyPos()) ||
                                isVerticalWall(start.getxPos() - 1, start.getyPos() - 1)
                ){
                    return new Tuple<>(false, "Wall obstructing move");
                }
                if(move != null && (start.getxPos() - 1 != move.end.getxPos() || start.getyPos() != move.end.getyPos())){
                    return new Tuple<>(false, "TargetSquare incorrect");
                }
                else{
                    return new Tuple<>(true, "");
                }
            default:
                return new Tuple<>(false, "invalid direction");
        }
    }
    private boolean isHorizontalWall(int x, int y){
        if(x < 0 || x > Base.WALLS - 1 || y < 0 || y > Base.WALLS - 1){
            return false;
        }
        return horizontalWalls[x][y];
    }

    private boolean isVerticalWall(int x, int y){
        if(x < 0 || x > Base.WALLS - 1 || y < 0 || y > Base.WALLS - 1){
            return false;
        }
        return verticalWalls[x][y];
    }

    private void setHorizontalWalls(int x, int y, boolean value){
        if(x < 0 || x > Base.WALLS - 1 || y < 0 || y > Base.WALLS - 1){
            return;
        }
        horizontalWalls[x][y] = value;
    }

    private void setVerticalWalls(int x, int y, boolean value){
        if(x < 0 || x > Base.WALLS - 1 || y < 0 || y > Base.WALLS - 1){
            return;
        }
        verticalWalls[x][y] = value;
    }

    @Override
    public int hashCode() {
        // return toString().hashCode();

        return 1 * Arrays.deepHashCode(verticalWalls)
                + 4 * Arrays.deepHashCode(horizontalWalls)
                + 8 * wallsLeftPlayer0
                + 32 * wallsLeftPlayer1
                + 3 * posPlayer1.hashCode()
                + 12 * posPlayer0.hashCode()
                + (player0Active ? 1000 : 2000);

    }

    public boolean isPlayer0Active() {
        return player0Active;
    }
}
