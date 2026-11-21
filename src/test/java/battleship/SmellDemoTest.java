package battleship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Testes unitários de caixa-branca para a classe {@link SmellDemo}.
 * Garante cobertura total do método estático {@link SmellDemo#metodoLongo()} e da classe.
 */
class SmellDemoTest {

    @Test
    @DisplayName("Deve retornar 3 ao invocar o método estático metodoLongo()")
    void testMetodoLongo() {
        int resultado = SmellDemo.metodoLongo();
        assertEquals(3, resultado, "metodoLongo() deve somar 0 + 1 + 2 e retornar 3");
    }

    @Test
    @DisplayName("Deve instanciar SmellDemo através do construtor por omissão sem lançar exceções")
    void testConstrutorPadrao() {
        SmellDemo demo = assertDoesNotThrow(SmellDemo::new, "A criação de SmellDemo não deve lançar exceções");
        assertNotNull(demo, "A instância criada de SmellDemo não deve ser nula");
    }
}