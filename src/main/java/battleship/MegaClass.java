package battleship;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MegaClass {
    private static final int SIZE = 10;

    private final char[][] board = new char[SIZE][SIZE];
    private final char[][] enemyBoard = new char[SIZE][SIZE];
    private final List<String> ships = new ArrayList<>();
    private final List<String> sunkShips = new ArrayList<>();
    private final Map<String, Integer> scores = new HashMap<>();
    private final List<String> log = new ArrayList<>();
    private String currentPlayer = "P1";
    private int turn;
    private int shotsFired;
    private int hits;
    private int misses;
    private boolean gameOver;

    public void initBoard() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = '~';
                enemyBoard[i][j] = '~';
            }
        }
    }

    public boolean isInside(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    public boolean isFree(int row, int col) {
        if (!isInside(row, col)) {
            return false;
        }
        return board[row][col] == '~';
    }

    public void clearBoard() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] != '~') {
                    board[i][j] = '~';
                }
            }
        }
    }

    // --- 5..9: navios ------------------------------------------------------
    public boolean placeShip(String name, int row, int col, int size, boolean horizontal) {
        if (name == null || name.isEmpty() || size <= 0) {
            return false;
        }
        if (horizontal) {
            if (col + size > SIZE) {
                return false;
            }
            for (int j = col; j < col + size; j++) {
                if (!isFree(row, j)) {
                    return false;
                }
            }
            for (int j = col; j < col + size; j++) {
                board[row][j] = 'S';
            }
        } else {
            if (row + size > SIZE) {
                return false;
            }
            for (int i = row; i < row + size; i++) {
                if (!isFree(i, col)) {
                    return false;
                }
            }
            for (int i = row; i < row + size; i++) {
                board[i][col] = 'S';
            }
        }
        ships.add(name + ":" + row + ":" + col + ":" + size + ":" + horizontal);
        return true;
    }

    public boolean removeShip(String name) {
        for (int i = 0; i < ships.size(); i++) {
            if (ships.get(i).startsWith(name + ":")) {
                ships.remove(i);
                return true;
            }
        }
        return false;
    }

    public int countShips() {
        int n = 0;
        for (String s : ships) {
            if (s != null && !s.isEmpty()) {
                n++;
            }
        }
        return n;
    }

    public boolean isShipSunk(String name) {
        for (String s : sunkShips) {
            if (s.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public boolean allShipsSunk() {
        if (ships.isEmpty()) {
            return false;
        }
        for (String s : ships) {
            String name = s.split(":")[0];
            if (!isShipSunk(name)) {
                return false;
            }
        }
        return true;
    }

    // --- 10..14: tiros e turnos -------------------------------------------
    public String fire(int row, int col) {
        if (gameOver) {
            return "GAME_OVER";
        }
        if (!isInside(row, col)) {
            return "INVALID";
        }
        shotsFired++;
        if (board[row][col] == 'S') {
            board[row][col] = 'X';
            hits++;
            addScore(currentPlayer, 10);
            log.add(currentPlayer + " hit " + row + "," + col);
            if (allShipsSunk()) {
                gameOver = true;
                return "WIN";
            }
            return "HIT";
        } else if (board[row][col] == '~') {
            board[row][col] = 'O';
            misses++;
            log.add(currentPlayer + " miss " + row + "," + col);
            switchPlayer();
            return "MISS";
        } else {
            return "REPEATED";
        }
    }

    public void switchPlayer() {
        if ("P1".equals(currentPlayer)) {
            currentPlayer = "P2";
        } else {
            currentPlayer = "P1";
        }
        turn++;
    }

    public String getCurrentPlayer() {
        return currentPlayer;
    }

    public int getTurn() {
        return turn;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    // --- 15..18: pontuacao e estatisticas ---------------------------------
    public void addScore(String player, int points) {
        if (player == null) {
            return;
        }
        Integer current = scores.get(player);
        if (current == null) {
            scores.put(player, points);
        } else {
            scores.put(player, current + points);
        }
    }

    public int getScore(String player) {
        Integer v = scores.get(player);
        if (v == null) {
            return 0;
        }
        return v;
    }

    public double accuracy() {
        if (shotsFired == 0) {
            return 0.0;
        }
        return (double) hits / (double) shotsFired;
    }

    public String ranking() {
        String best = "-";
        int max = -1;
        for (Map.Entry<String, Integer> e : scores.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                best = e.getKey();
            } else if (e.getValue() == max && best.compareTo(e.getKey()) > 0) {
                best = e.getKey();
            }
        }
        return best;
    }

    // --- 19..21: persistencia ---------------------------------------------
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                sb.append(board[i][j]);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public boolean load(String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        String[] rows = data.split("\n");
        if (rows.length < SIZE) {
            return false;
        }
        for (int i = 0; i < SIZE; i++) {
            if (rows[i].length() < SIZE) {
                return false;
            }
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = rows[i].charAt(j);
            }
        }
        return true;
    }

    public List<String> getLog() {
        return new ArrayList<>(log);
    }

    // --- 22..24: apresentacao ---------------------------------------------
    public String render() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                char c = board[i][j];
                if (c == 'S') {
                    sb.append('.');
                } else if (c == 'X') {
                    sb.append('X');
                } else if (c == 'O') {
                    sb.append('o');
                } else {
                    sb.append('~');
                }
                sb.append(' ');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public String statusLine() {
        if (gameOver) {
            return "Fim de jogo. Vencedor: " + ranking();
        }
        if (shotsFired == 0) {
            return "Turno " + turn + " - " + currentPlayer + " - sem tiros";
        }
        return "Turno " + turn + " - " + currentPlayer
                + " - precisao " + String.format("%.2f", accuracy());
    }

    public String helpText() {
        return "Comandos: place, fire, save, load, status, quit";
    }
}
