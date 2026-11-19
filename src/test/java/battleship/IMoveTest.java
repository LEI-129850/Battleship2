package battleship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de caixa-branca para a interface {@link IMove}.
 * Valida o método estático {@link IMove#readMove(int, Scanner)}, os ramos do ciclo de leitura,
 * casos de fronteira, tratamento de exceções e a conformidade da interface.
 */
class IMoveTest {

    @Test
    @DisplayName("Deve ler jogada com múltiplos tiros válidos a partir do Scanner")
    void deveLerJogadaComMultiplosTirosValidos() {
        String input = "3 0 1 2 3 4 5";
        Scanner sc = new Scanner(input);

        Move move = IMove.readMove(1, sc);

        assertNotNull(move, "O objeto Move retornado não deve ser nulo");
        assertEquals(1, move.getNumber(), "O número do movimento deve corresponder ao fornecido");
        assertEquals(3, move.getShots().size(), "O movimento deve conter exatamente 3 tiros");

        assertEquals(0, move.getShots().get(0).getRow(), "Linha do primeiro tiro incorreta");
        assertEquals(1, move.getShots().get(0).getColumn(), "Coluna do primeiro tiro incorreta");

        assertEquals(2, move.getShots().get(1).getRow(), "Linha do segundo tiro incorreta");
        assertEquals(3, move.getShots().get(1).getColumn(), "Coluna do segundo tiro incorreta");

        assertEquals(4, move.getShots().get(2).getRow(), "Linha do terceiro tiro incorreta");
        assertEquals(5, move.getShots().get(2).getColumn(), "Coluna do terceiro tiro incorreta");

        assertNotNull(move.getShotResults(), "A lista de resultados não deve ser nula");
        assertTrue(move.getShotResults().isEmpty(), "A lista de resultados deve ser inicialmente vazia");
    }

    @Test
    @DisplayName("Deve ler jogada com zero tiros e retornar lista vazia de posições")
    void deveLerJogadaComZeroTiros() {
        String input = "0";
        Scanner sc = new Scanner(input);

        Move move = IMove.readMove(10, sc);

        assertNotNull(move, "O objeto Move retornado não deve ser nulo");
        assertEquals(10, move.getNumber(), "O número do movimento deve ser 10");
        assertTrue(move.getShots().isEmpty(), "A lista de tiros deve estar vazia quando numShots é 0");
        assertTrue(move.getShotResults().isEmpty(), "A lista de resultados deve estar vazia");
    }

    @Test
    @DisplayName("Deve ler jogada com exatamente um tiro")
    void deveLerJogadaComUmTiro() {
        String input = "1 7 8";
        Scanner sc = new Scanner(input);

        Move move = IMove.readMove(2, sc);

        assertNotNull(move, "O objeto Move retornado não deve ser nulo");
        assertEquals(2, move.getNumber(), "O número do movimento deve ser 2");
        assertEquals(1, move.getShots().size(), "A lista deve conter 1 tiro");
        assertEquals(7, move.getShots().get(0).getRow(), "A linha do tiro deve ser 7");
        assertEquals(8, move.getShots().get(0).getColumn(), "A coluna do tiro deve ser 8");
    }

    @Test
    @DisplayName("Deve lidar com número negativo de tiros retornando lista vazia")
    void deveLidarComNumeroNegativoDeTiros() {
        String input = "-1";
        Scanner sc = new Scanner(input);

        Move move = IMove.readMove(3, sc);

        assertNotNull(move, "O objeto Move retornado não deve ser nulo");
        assertEquals(3, move.getNumber(), "O número do movimento deve ser 3");
        assertTrue(move.getShots().isEmpty(), "A lista de tiros deve estar vazia quando numShots é negativo");
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando o Scanner for nulo")
    void deveLancarExcecaoQuandoScannerForNulo() {
        assertThrows(NullPointerException.class, () -> IMove.readMove(1, null),
                "Deve lançar NullPointerException se o scanner for nulo");
    }

    @Test
    @DisplayName("Deve lançar NoSuchElementException quando não existirem coordenadas suficientes")
    void deveLancarExcecaoQuandoNaoExistiremCoordenadasSuficientes() {
        // Indica 2 tiros mas apenas fornece coordenadas completas para 1 tiro e apenas a linha do segundo
        String input = "2 1 1 2";
        Scanner sc = new Scanner(input);

        assertThrows(NoSuchElementException.class, () -> IMove.readMove(1, sc),
                "Deve lançar NoSuchElementException quando os tokens no scanner terminam antes do esperado");
    }

    @Test
    @DisplayName("Deve lançar InputMismatchException quando os dados no Scanner não forem numéricos")
    void deveLancarExcecaoQuandoDadosForemInvalidos() {
        String input = "invalido";
        Scanner sc = new Scanner(input);

        assertThrows(InputMismatchException.class, () -> IMove.readMove(1, sc),
                "Deve lançar InputMismatchException quando o próximo token não for um número inteiro");
    }

    @Test
    @DisplayName("Garante a conformidade e execução dos métodos declarados na interface IMove")
    void deveGarantirConformidadeDaInterfaceIMove() {
        List<IPosition> shots = new ArrayList<>();
        shots.add(new Position(1, 2));
        List<IGame.ShotResult> results = new ArrayList<>();

        IMove customMove = new IMove() {
            @Override
            public int getNumber() {
                return 42;
            }

            @Override
            public List<IPosition> getShots() {
                return shots;
            }

            @Override
            public List<IGame.ShotResult> getShotResults() {
                return results;
            }

            @Override
            public String processEnemyFire(boolean verbose) {
                return "resultado-custom";
            }

            @Override
            public String toString() {
                return "CustomMove{42}";
            }
        };

        assertEquals(42, customMove.getNumber(), "getNumber() deve retornar o valor implementado");
        assertEquals(shots, customMove.getShots(), "getShots() deve retornar a lista de posições");
        assertEquals(results, customMove.getShotResults(), "getShotResults() deve retornar a lista de resultados");
        assertEquals("resultado-custom", customMove.processEnemyFire(false), "processEnemyFire deve retornar a mensagem correta");
        assertEquals("CustomMove{42}", customMove.toString(), "toString deve retornar a representação esperada");
    }
}
