package battleship;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ScoreboardManager{
    private static final String DataBase_URL = "jdbc:sqlite:battleship.db";

    public ScoreboardManager(){
        initializeDB();
    }

    public static void initializeDB(){
        String sql = "CREATE TABLE IF NOT EXISTS game_history ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "game_date TEXT NOT NULL,"
                + "shots_fired INTEGER NOT NULL,"
                + "result TEXT NOT NULL"
                + ");";
        try (Connection connection = DriverManager.getConnection(DataBase_URL); Statement statement = connection.createStatement()){
            statement.execute(sql);
        }catch (SQLException e){
            System.err.println("Erro ao iniciar a Base de Dados: " + e.getMessage());
        }
    }

    public void saveGame(int tirosTotais, String resultado) {
        String sql = "INSERT INTO game_history(game_date, shots_fired, result) VALUES(?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(DataBase_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            preparedStatement.setString(1, currentDate);
            preparedStatement.setInt(2, tirosTotais);
            preparedStatement.setString(3, resultado);
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            System.err.println("Erro ao carregar resultado na Base de Dados: " + e.getMessage());
        }
    }

    public void printScoreboard() {
        String sql = "SELECT game_date, shots_fired, result FROM game_history ORDER BY id DESC LIMIT 10";
        try (Connection conn = DriverManager.getConnection(DataBase_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- Últimos 10 Jogos ---");
            while (rs.next()) {
                System.out.printf("Data: %s | Tiros: %d | Resultado: %s%n",
                        rs.getString("game_date"),
                        rs.getInt("shots_fired"),
                        rs.getString("result"));
            }
            System.out.println("------------------------\n");
        } catch (Exception e) {
            System.err.println("Erro ao ler o histórico: " + e.getMessage());
        }
    }



}
