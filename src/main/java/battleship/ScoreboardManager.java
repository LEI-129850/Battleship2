package battleship;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Classe responsável por gerir a gravação e leitura do histórico de jogos
 * numa base de dados local SQLite.
 */
public class ScoreboardManager {

    private static final String DB_URL = "jdbc:sqlite:battleship.db";

    /**
     * Construtor da classe ScoreboardManager.
     * Inicializa a base de dados automaticamente no momento da instanciação.
     */
    public ScoreboardManager() {
        initializeDB();
    }

    /**
     * Cria a tabela de histórico de jogos na base de dados, caso ainda não exista.
     * Contem o comando sql para a criação da tabela e dos devidos campos
     */
    private void initializeDB() {
        String sql = "CREATE TABLE IF NOT EXISTS game_history ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "game_date TEXT NOT NULL,"
                + "shots_fired INTEGER NOT NULL,"
                + "result TEXT NOT NULL"
                + ");";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println("Erro SQLite: " + e.getMessage());
        }
    }

    /**
     * Guarda o resultado de uma simulação terminada na base de dados local.
     *
     * @param tirosTotais O número de rajadas disparadas pelo algoritmo/Alien durante o jogo.
     * @param resultado   Uma string descritiva do desfecho da partida (ex: "Frota Afundada!").
     */
    public void saveGame(int tirosTotais, String resultado) {
        String sql = "INSERT INTO game_history(game_date, shots_fired, result) VALUES(?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String dataAtual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            pstmt.setString(1, dataAtual);
            pstmt.setInt(2, tirosTotais);
            pstmt.setString(3, resultado);
            pstmt.executeUpdate();

        } catch (Exception e) {
            System.out.println("Erro ao guardar o jogo: " + e.getMessage());
        }
    }

    /**
     * Consulta a base de dados e imprime na consola o relatório das últimas 10 partidas.
     * Contém o comando sql para selecionar os devidos parametros da tabela e ordenar pelas 10 ultimas
     * Este método é necessário para termos uma noção da progressão da Ai bem como do código
     */
    public void printScoreboard() {
        String sql = "SELECT game_date, shots_fired, result FROM game_history ORDER BY id DESC LIMIT 10";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println();
            System.out.println("Histórico de Partidas:");

            while (rs.next()) {
                String data = rs.getString("game_date");
                int tiros = rs.getInt("shots_fired");
                String res = rs.getString("result");

                System.out.println("[" + data + "] Rajadas: " + tiros + " -> " + res);
            }
            System.out.println();

        } catch (Exception e) {
            System.out.println("Erro ao ler os dados: " + e.getMessage());
        }
    }
}