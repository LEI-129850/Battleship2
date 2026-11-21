package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for battleship.Main.
 * Author: LEI-129830
 * Date: 2026-11-21 09:28
 * Cyclomatic Complexity:
 * - constructor Main(): 1
 * - main(String[]): 1
 */
class MainTest {

    private Main main;
    private ByteArrayInputStream input;
    private InputStream originalIn;
    private ByteArrayOutputStream output;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        main = new Main();
        originalIn = System.in;
        originalOut = System.out;

        input = new ByteArrayInputStream("desisto\n".getBytes(StandardCharsets.UTF_8));
        output = new ByteArrayOutputStream();

        System.setIn(input);
        System.setOut(new PrintStream(output));
    }

    @AfterEach
    void tearDown() {
        System.setIn(originalIn);
        System.setOut(originalOut);
        main = null;
        input = null;
        output = null;
    }

    @Test
    @DisplayName("Construtor cria a instancia do Main")
    void constructor() {
        assertEquals(Main.class, main.getClass(),
                "Error: expected a battleship.Main instance but got " + main.getClass());
    }


    @Test
    @DisplayName("Main da print ao titulo, mostra o menu, e sai com desisto")
    void main() {
        Main.main(new String[0]);

        String actual = output.toString(StandardCharsets.UTF_8);
        assertAll(
                () -> assertTrue(actual.contains("***  Battleship  ***"),
                        "Erro: Esperado que o output contenha o titulo Battleship"),
                () -> assertTrue(actual.contains("Digite um dos comandos"),
                        "Erro: Esperado que o output contenha as instrucoes do menu"),
                () -> assertTrue(actual.contains("Bons ventos!"),
                        "Erro: Esperado que o menu saia e de print a mensagem de despedidas")
        );
    }
}