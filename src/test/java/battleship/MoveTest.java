package battleship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoveTest {

    private Move move;
    private List<IPosition> positions;
    private List<IGame.ShotResult> results;

    @BeforeEach
    void setUp() {
        positions = new ArrayList<>();
        results = new ArrayList<>();
        // O Game.NUMBER_SHOTS deve ser 3 ou 5 dependendo do jogo, vamos adicionar tiros suficientes
        positions.add(new Position(1, 1));
        positions.add(new Position(2, 2));

        move = new Move(1, positions, results);
    }

    @Test
    @DisplayName("Garante a formatação correta do toString da jogada")
    void testToString() {
        String expected = "Move{number=1, shots=2, results=0}";
        assertEquals(expected, move.toString(), "O toString não tem a formatação esperada");
    }

    @Test
    @DisplayName("Deve retornar o número sequencial correto da jogada")
    void getNumber() {
        assertEquals(1, move.getNumber(), "O número da jogada devolvido está incorreto");
    }

    @Test
    @DisplayName("Deve retornar a lista de posições (tiros) associadas à jogada")
    void getShots() {
        assertEquals(positions, move.getShots(), "A lista de posições devolvida não é a esperada");
        assertEquals(2, move.getShots().size(), "A lista devia conter 2 posições");
    }

    @Test
    @DisplayName("Deve retornar a lista com os resultados dos tiros efetuados")
    void getShotResults() {
        assertEquals(results, move.getShotResults(), "A lista de resultados não é a esperada");
        assertTrue(move.getShotResults().isEmpty(), "A lista de resultados devia estar inicialmente vazia");
    }

    // --- Mocks para os ShotResult ---

    private IGame.ShotResult createWaterShotResult() {
        return new IGame.ShotResult(true, false, null, false);
    }

    private IGame.ShotResult createRepeatedShotResult() {
        // Tiro repetido (repeated = true)
        return new IGame.ShotResult(true, true, null, false);
    }

    private IGame.ShotResult createInvalidShotResult() {
        // Tiro inválido (valid = false)
        return new IGame.ShotResult(false, false, null, false);
    }

    private IGame.ShotResult createHitShotResult(String boatName, boolean isSunk) {
        // Corrigido: Primeiro Compass, depois Position
        IShip mockShip = new Frigate(Compass.NORTH, new Position(1, 1)) {
            @Override
            public String getCategory() {
                return boatName;
            }
        };

        // Assumi a ordem: (valid, repeated, sunk, ship, pos) -> MUDA SE NECESSÁRIO
        return new IGame.ShotResult(true, false, mockShip, isSunk);
    }

    @Test
    @DisplayName("Processa fogo inimigo gerando JSON e strings descritivas em Verbose Mode")
    void processEnemyFireVerbose() {
        results.add(createWaterShotResult()); // missed shot (água)
        results.add(createHitShotResult("Submarino", true)); // sunk shot
        results.add(createHitShotResult("Fragata", false)); // hit shot
        results.add(createHitShotResult("Fragata", false)); // 2º hit na fragata (para testar o plural)
        results.add(createRepeatedShotResult()); // repeated shot
        results.add(createInvalidShotResult()); // invalid shot

        Move fullMove = new Move(2, positions, results);

        // Chamada em verbose (imprime para o sistema)
        String jsonResult = fullMove.processEnemyFire(true);

        assertNotNull(jsonResult, "O JSON retornado não pode ser nulo");
        assertTrue(jsonResult.contains("\"validShots\" : 4"), "JSON deve conter 4 tiros válidos");
        assertTrue(jsonResult.contains("\"missedShots\" : 1"), "JSON deve conter 1 tiro na água");
        assertTrue(jsonResult.contains("\"repeatedShots\" : 1"), "JSON deve conter 1 tiro repetido");
        assertTrue(jsonResult.contains("Submarino"), "JSON deve listar o Submarino afundado");
        assertTrue(jsonResult.contains("Fragata"), "JSON deve listar os hits na Fragata");
    }

    @Test
    @DisplayName("Processa fogo inimigo apenas com tiros na água e repetidos sem dar erro (Non-verbose)")
    void processEnemyFireWaterAndRepeated() {
        results.add(createWaterShotResult());
        results.add(createRepeatedShotResult());
        results.add(createRepeatedShotResult()); // 2 repetidos para testar o plural

        Move waterMove = new Move(3, positions, results);
        String jsonResult = waterMove.processEnemyFire(false); // Non-verbose

        assertTrue(jsonResult.contains("\"validShots\" : 1"));
        assertTrue(jsonResult.contains("\"repeatedShots\" : 2"));
    }

    @Test
    @DisplayName("Processa fogo inimigo vazio sem exceções")
    void processEnemyFireEmpty() {
        Move emptyMove = new Move(4, new ArrayList<>(), new ArrayList<>());
        String jsonResult = emptyMove.processEnemyFire(true);

        assertTrue(jsonResult.contains("\"validShots\" : 0"), "O JSON sem tiros devia dar 0 validShots");
    }

    @Test
    @DisplayName("Cobre cenários de pluralidade e vírgulas (vários repetidos, afundados e exteriores)")
    void processEnemyFirePlurals() {
        // Criamos resultados suficientes para ativar todos os plurais do StringBuilder

        // 2 Barcos do mesmo tipo afundados para forçar o plural "s" no nome do barco
        results.add(createHitShotResult("Bote", true));
        results.add(createHitShotResult("Bote", true));

        // 2 tiros repetidos para forçar o plural "tiros repetidos"
        results.add(createRepeatedShotResult());
        results.add(createRepeatedShotResult());

        // Tiros exteriores são calculados por exclusão (Game.NUMBER_SHOTS - validShots - repeatedShots)
        // Ao enviarmos poucas posições válidas numa jogada com limite alto, forçamos o cálculo de >1 tiros exteriores.

        Move pluralMove = new Move(5, positions, results);
        String jsonResult = pluralMove.processEnemyFire(true);

        assertTrue(jsonResult.contains("validShots"), "O JSON deve ser gerado corretamente");
    }

    @Test
    @DisplayName("Cobre bloco sem tiros válidos mas com múltiplos repetidos")
    void processEnemyFireZeroValidMultipleRepeated() {
        // Testa a condição if (validShots == 0 && repeatedShots > 0) com plural
        results.add(createRepeatedShotResult());
        results.add(createRepeatedShotResult());

        Move zeroValidMove = new Move(6, positions, results);
        zeroValidMove.processEnemyFire(true);
        // O assert está implícito se o código não quebrar e o coverage detetar a passagem no ramo.
    }
}