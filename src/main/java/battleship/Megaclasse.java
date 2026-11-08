package battleship;

import java.util.ArrayList;
import java.util.List;

/** Responsabilidade unica: persistencia do estado e registo de eventos. */
public class Megaclasse {

    private final List<String> log = new ArrayList<>();

    public String serialize(Board board) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                sb.append(board.cellAt(i, j));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public boolean load(Board board, String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        String[] rows = data.split("\n");
        if (rows.length < Board.SIZE) {
            return false;
        }
        for (int i = 0; i < Board.SIZE; i++) {
            if (rows[i].length() < Board.SIZE) {
                return false;
            }
            for (int j = 0; j < Board.SIZE; j++) {
                board.setCell(i, j, rows[i].charAt(j));
            }
        }
        return true;
    }

    public void append(String entry) {
        log.add(entry);
    }

    public List<String> entries() {
        return new ArrayList<>(log);
    }
}
