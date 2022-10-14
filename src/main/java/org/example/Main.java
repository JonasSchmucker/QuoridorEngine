package org.example;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class Main {


    public static void main(String[] args) {
        File inputFile = resolveInputFile(args);

        String positionString = readPositionString(inputFile);

        Board board = new Board();
        board.buildPosition(positionString);
        board.cliOutput();
        // System.out.println(board.distance(true));
        runGame(board);
        // position2Test(board);
    }

    private static void runGame(Board board) {
        int movesCount = 1;
        StringBuilder stringBuilder = new StringBuilder();
        while(!board.gameOver()) {
            Tuple<Move [], Integer> bestMove = board.bestMove();
            if(board.isPlayer0Active()){
                stringBuilder.append(movesCount + ":\t" + bestMove.value[0] + "\t" + ratingToString(bestMove.code) + "\t\t\t");
                System.out.println();
            }
            else{
                stringBuilder.append(bestMove.value[0] + "\t" + ratingToString(bestMove.code) + "\n");
            }
            movesCount++;
            // System.out.println("Best move is " + bestMove.value + " with a rating of " + bestMove.code);
            board.makeMoveSecurely(bestMove.value[0]);
            clearScreen();
            board.cliOutput();
            System.out.print(stringBuilder);
        }
        System.out.println("Game Over");
    }

    private static String ratingToString(int value){
        if(value < Integer.MIN_VALUE / 2){
            return "(Mate in " + (Integer.MIN_VALUE - value) + ")";
        }
        else if (value > Integer.MAX_VALUE / 2){
            return "(Mate in " + (Integer.MAX_VALUE - value) + ")";
        }
        return "(" + value + ")\t\t\t";
    }

    private static String readPositionString(File inputFile) {
        Scanner myReader = null;
        String data = "";
        try {
            myReader = new Scanner(inputFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        if (myReader.hasNextLine()) {
                data = myReader.nextLine();
        }
        myReader.close();
        return data;
    }

    public static File resolveInputFile(String args[]){
        ArgumentParser parser = ArgumentParsers.newFor("Quridor Engine").build()
                .description("Calculates best moves for the Quoridor game");
        parser.addArgument("-i", "--in")
                .dest("inputfile")
                .type(Arguments.fileType().acceptSystemIn().verifyCanRead())
                .setDefault("-")
                .help("Position Input File");
        try {
            Namespace res = parser.parseArgs(args);
            File inputFile = (File) res.get("inputfile");
            return inputFile;
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            return null;
        }
    }

    private static void clearScreen(){
        System.out.print(Base.ESCAPED_CLEAR_COMMAND);
        System.out.flush();
    }
}