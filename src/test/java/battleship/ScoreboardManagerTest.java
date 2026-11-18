package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class ScoreboardManagerTest {

    private ScoreboardManager scoreboardManager;

    // Ferramentas para intercetar o output da consola
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        // Redireciona o System.out para o nosso buffer antes de cada teste
        System.setOut(new PrintStream(outContent));

        // A instanciação já cobre o initializeDB()
        scoreboardManager = new ScoreboardManager();
    }

    @AfterEach
    void tearDown() {
        // Restaura a consola ao normal no fim de cada teste
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("Garante que um jogo é guardado na base de dados SQLite sem lançar exceções")
    void saveGame() {
        assertDoesNotThrow(() -> scoreboardManager.saveGame(42, "Teste JUnit Afundado"),
                "O saveGame não deve rebentar com a execução");

        // Se o try-catch da classe apanhar erro, ele imprime "Erro ao guardar o jogo"
        // Validamos que isso NÃO aconteceu.
        assertFalse(outContent.toString().contains("Erro ao guardar o jogo"),
                "Foi detetado um erro de SQL na consola ao tentar guardar");
    }

    @Test
    @DisplayName("Garante que o histórico é lido e impresso na consola com o formato correto")
    void printScoreboard() {
        // Garantimos que há pelo menos um registo conhecido na BD para testar a leitura
        scoreboardManager.saveGame(99, "Teste Print JUnit");

        // Limpamos o buffer antes da leitura para ignorar possíveis erros silenciosos do saveGame
        outContent.reset();

        // Lemos a BD
        scoreboardManager.printScoreboard();

        String output = outContent.toString();

        assertTrue(output.contains("Histórico de Partidas:"), "O cabeçalho do histórico não foi impresso");
        assertTrue(output.contains("Rajadas: 99 -> Teste Print JUnit"), "O registo inserido não foi encontrado na leitura");
        assertFalse(output.contains("Erro ao ler os dados"), "Foi detetado um erro de SQL na consola ao tentar ler");
    }
}