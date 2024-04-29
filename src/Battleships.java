import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.InputMismatchException;
import java.util.Objects;
import java.util.Scanner;

public class Battleships {
    private static final int SIZE = 10;
    private static final int SHIPS = 5;

    private char[][] playerBoard;
    private char[][] opponentBoard;
    private Scanner scanner;

    private Boolean isServer;
    private ServerSocket serverSocket;
    private Socket connectionSocket;
    private Socket clientSocket;
    static BufferedReader in;
    static PrintWriter out;

    public Battleships() {
        playerBoard = new char[SIZE][SIZE];
        opponentBoard = new char[SIZE][SIZE];
        scanner = new Scanner(System.in);
        initializeBoards();
    }

    private void initializeBoards() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                playerBoard[i][j] = '-';
                opponentBoard[i][j] = '-';
            }
        }
        placeShips(playerBoard);
    }

    private void placeShips(char[][] board) {
        for (int i = 0; i < SHIPS; i++) {
            int x = (int) (Math.random() * SIZE);
            int y = (int) (Math.random() * SIZE);
            if (board[x][y] == '-') {
                board[x][y] = 'S';
            } else {
                i--; // Try again to place the ship
            }
        }
    }

    private void printBoard(char[][] board) {
        System.out.print("  ");
        for (int i = 0; i < SIZE; i++) {
            System.out.print(i + " ");
        }
        System.out.println();
        for (int i = 0; i < SIZE; i++) {
            System.out.print(i + " ");
            for (int j = 0; j < SIZE; j++) {
                System.out.print(board[i][j] + " ");
            }
            System.out.println();
        }
    }

    private boolean isGameOver(char[][] board) {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] == 'S') {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isValidMove(int x, int y) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE;
    }

    private void playGame(String[] args) throws IOException {
        if (args.length == 0 || !(Objects.equals(args[0], "true") || Objects.equals(args[0], "false")) ||
                (args.length != 2 && Boolean.parseBoolean(args[0])) ||
                (args.length != 3 && !Boolean.parseBoolean(args[0]))){
            System.out.println("Usage: java Battleships <isServer = true> <port> OR");
            System.out.println("Usage: java Battleships <isServer = false> <port> <ip>");
            return;
        }

        boolean isServer = Boolean.parseBoolean(args[0]);
        int port = Integer.parseInt(args[1]);
        if(isServer){
            serverSocket = new ServerSocket(port);
            connectionSocket = serverSocket.accept();
            System.out.println("Client connected: " + connectionSocket.getInetAddress().getHostAddress());
        } else {
            String serverIP = args[2];
            connectionSocket = new Socket(serverIP, port);
        }
        in = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
        out = new PrintWriter(connectionSocket.getOutputStream(), true);

        boolean player1Turn = true;
        while (true) {
            System.out.println("Player " + (player1Turn ? "1" : "2") + "'s turn:");
            if(!isServer ^ player1Turn) {
                // Triggers whenever it is Player1's Turn (!isServer = false and player1Turn = true)
                // or when it is Player2's Turn (isServer = true and player1Turn = false)
                System.out.println("Your board:");
                printBoard(playerBoard);
                System.out.println("Opponent's board:");
                printBoard(opponentBoard);
                System.out.print("Enter row (0-9): ");
                int x;
                int y;
                try {
                    x = scanner.nextInt();
                    System.out.print("Enter column (0-9): ");
                    y = scanner.nextInt();
                } catch (InputMismatchException e){
                    System.out.println("Invalid Character");
                    continue;
                }

                if (!isValidMove(x, y)) {
                    System.out.println("Invalid move, try again.");
                    continue;
                }

                String guess = x + ", " + y;
                out.println(guess);

                // Wait for opponent to tell if it was a hit
                String response = in.readLine();

                if (Objects.equals(response, "hit")) {
                    System.out.println("Hit!");
                    opponentBoard[x][y] = 'X';
                } else if (Objects.equals(response, "miss")) {
                    System.out.println("Miss!");
                    opponentBoard[x][y] = 'O';
                } else if (Objects.equals(response, "game over")){
                    System.out.println("Hit!");
                    opponentBoard[x][y] = 'X';
                    System.out.println("You won!");
                    break;
                }else {
                    System.out.println("You've already guessed this position, pick another one.");
                    continue;
                }

                out.println("next turn");
                player1Turn = !player1Turn;
            } else {
                // Triggers whenever it is Player1's Turn and Player2 is waiting
                // or when it is player2's turn and player 1 is waiting.
                // Wait for opponent to tell move
                String move = in.readLine();
                String[] values = move.split(",\\s*"); // Split by comma and optional whitespace
                int x = 0;
                int y = 0;
                String response = ".";

                if (values.length == 2) {
                    x = Integer.parseInt(values[0]);
                    y = Integer.parseInt(values[1]);
                    System.out.println("x = " + x + ", y = " + y);
                } else {
                    System.out.println("Invalid input format");
                    out.println(response);
                    continue;
                }

                if(playerBoard[x][y] == 'S'){
                    playerBoard[x][y] = 'X';
                    response = "hit";
                    if (isGameOver(playerBoard)) {
                        System.out.println("All your ships were destroyed! You Lost!");
                        response = "game over";
                        out.println(response);
                        break;
                    }
                } else if(playerBoard[x][y] == '-'){
                    playerBoard[x][y] = 'O';
                    response = "miss";
                }

                System.out.println(response);
                out.println(response);

                String ready = in.readLine();
                if(Objects.equals(ready, "next turn")){
                    player1Turn = !player1Turn;
                    continue;
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Battleships game = new Battleships();
        game.playGame(args);
    }
}
