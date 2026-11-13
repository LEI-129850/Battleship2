package battleship;

import java.util.List;
import java.time.Duration;
import java.util.ArrayList;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Game.
 * Author: britoeabreu
 * Date: 2024-03-19
 * Time: 15:30
 * Cyclomatic Complexity for each method:
 * - Game (constructor): 1
 * - fire: 7
 * - getShots: 1
 * - getRepeatedShots: 1
 * - getInvalidShots: 1
 * - getHits: 1
 * - getSunkShips: 1
 * - getRemainingShips: 1
 * - validShot: 3
 * - repeatedShot: 2
 * - printBoard: 1
 * - printValidShots: 1
 * - printFleet: 1
 */
public class GameTest {

	private Game game;

	@BeforeEach
	void setUp() {
		game = new Game(new Fleet()); // Assuming Fleet is a concrete implementation of IFleet
	}

	@AfterEach
	void tearDown() {
		game = null;
	}

	@Test
	void constructor() {
		assertNotNull(game, "Game instance should not be null after construction.");
		assertNotNull(game.getAlienMoves(), "Shots list should not be null after initialization.");
		assertTrue(game.getAlienMoves().isEmpty(), "Shots list should be empty upon initialization.");
		assertEquals(0, game.getInvalidShots(), "Invalid shots count should be zero upon initialization.");
		assertEquals(0, game.getRepeatedShots(), "Repeated shots count should be zero upon initialization.");
		assertEquals(0, game.getHits(), "Hits count should be zero upon initialization.");
		assertEquals(0, game.getSunkShips(), "Sunk ships count should be zero upon initialization.");
	}

	@Test
	void fire2() {
		Position invalidPosition = new Position(-1, 5);
		game.fireSingleShot(invalidPosition, false);
		assertEquals(1, game.getInvalidShots(), "Invalid shots counter should increase for an invalid shot.");
	}

	@Test
	void fire3() {
		Position position = new Position(2, 3);
		game.fireSingleShot(position, false);
		game.fireSingleShot(position, true);
		assertEquals(1, game.getRepeatedShots(), "Repeated shots counter should increase for a repeated shot.");
	}

	@Test
	void repeatedShot1() {
		List<IPosition> positions = List.of(new Position(2, 3), new Position(2, 4), new Position(2, 5));
		game.fireShots(positions);
		Position position = new Position(2, 3);
		assertTrue(game.repeatedShot(position), "Position (2,3) should be marked as repeated after firing.");
	}

	@Test
	void repeatedShot2() {
		Position position = new Position(2, 3);
		assertFalse(game.repeatedShot(position), "Position (2,3) should not be marked as repeated before firing.");
	}

	@Test
	void getAlienMoves() {
		List<IPosition> positions = List.of(new Position(2, 3), new Position(2, 4), new Position(2, 5));
		game.fireShots(positions);
		assertEquals(1, game.getAlienMoves().size(), "Shots list should contain one shot after firing once.");
	}

	@Test
	void getRemainingShips() {
		IFleet fleet = game.getMyFleet();
		Ship ship1 = new Barge(Compass.NORTH, new Position(1, 1));
		Ship ship2 = new Frigate(Compass.EAST, new Position(5, 5));

		fleet.addShip(ship1);
		assertEquals(1, game.getRemainingShips(), "Just one ship was created!");
		fleet.addShip(ship2);
		assertEquals(2, game.getRemainingShips(), "Two ships were created!");
		ship2.sink();
		assertEquals(1, game.getRemainingShips(), "Remaining ships count should be 1 after sinking one of two ships.");
	}

	@Test
	@DisplayName("Deve imprimir o tabuleiro com navios afundados, tiros e legenda sem erros")
	void testPrintBoardAndMyBoard() {
		IFleet fleet = game.getMyFleet();
		Ship barge = new Barge(Compass.NORTH, new Position(2, 2)); // Navio de 1 casa
		Ship frigate = new Frigate(Compass.EAST, new Position(5, 5)); // Navio maior
		fleet.addShip(barge);
		fleet.addShip(frigate);

		// Jogada 1 (3 tiros): afunda barge, acerta frigate e dá um tiro na água
		game.fireShots(List.of(
				new Position(2, 2),  // Acerta e afunda barge
				new Position(5, 5),  // Acerta frigate sem afundar
				new Position(0, 0)   // Tiro na água
		));

		// Jogada 2 (3 tiros): inclui um tiro fora do tabuleiro
		game.fireShots(List.of(
				new Position(-1, -1), // Tiro fora do tabuleiro
				new Position(0, 1),   // Tiro na água
				new Position(0, 2)    // Tiro na água
		));

		assertDoesNotThrow(() -> game.printMyBoard(true, true));
		assertDoesNotThrow(() -> game.printMyBoard(false, false));
	}

	@Test
	@DisplayName("Deve lançar exceção ao tentar disparar um número de tiros diferente de 3")
	void testFireShotsInvalidCount() {
		assertThrows(IllegalArgumentException.class, () ->
				game.fireShots(List.of(new Position(0, 0)))
		);
	}

	@Test
	@DisplayName("Deve devolver a frota inimiga e a lista de jogadas do jogador")
	void testGetAlienFleetAndMyMoves() {
		assertNotNull(game.getAlienFleet());
		assertNotNull(game.getMyMoves());
	}

	@Test
	@DisplayName("Deve gerar tiros aleatórios do inimigo em formato JSON")
	void testRandomEnemyFire() {
		// Adiciona e afunda um navio para testar a remoção de posições adjacentes
		Ship barge = new Barge(Compass.NORTH, new Position(1, 1));
		game.getMyFleet().addShip(barge);
		barge.sink();

		String resultJson = game.randomEnemyFire();
		assertNotNull(resultJson);
		assertFalse(game.getAlienMoves().isEmpty());
	}

	@Test
	@DisplayName("Deve ler coordenadas válidas do Scanner em ambos os formatos (junto e separado)")
	void testReadEnemyFireValid() {
		// Testa formatos como "A1 B 2 C3" (ajusta caso NUMBER_SHOTS seja diferente de 3)
		String input = "A1 B 2 C3\n";
		java.util.Scanner scanner = new java.util.Scanner(input);

		String json = game.readEnemyFire(scanner);
		assertNotNull(json);
		assertEquals(1, game.getAlienMoves().size());
	}

	@Test
	@DisplayName("Deve lançar exceção quando o input de tiros no Scanner é incompleto ou insuficiente")
	void testReadEnemyFireInvalid() {
		// Caso 1: Letra sem número a seguir
		java.util.Scanner incompleteScanner = new java.util.Scanner("A\n");
		assertThrows(IllegalArgumentException.class, () -> game.readEnemyFire(incompleteScanner));

		// Caso 2: Menos posições do que NUMBER_SHOTS
		java.util.Scanner fewShotsScanner = new java.util.Scanner("A1\n");
		assertThrows(IllegalArgumentException.class, () -> game.readEnemyFire(fewShotsScanner));
	}

	@Test
	@DisplayName("Deve imprimir mensagem de fim de jogo sem lançar exceções")
	void testOver() {
		assertDoesNotThrow(() -> game.over());
	}

	@Test
	@DisplayName("Deve gerir o relógio do jogo (tempo restante, estado e paragem)")
	void testClockMethods() {
		assertNotNull(game.getRemainingTime());
		String status = game.clockStatus();
		assertTrue(status.startsWith("Tempo restante:"));
		assertDoesNotThrow(() -> game.stopClock());
	}

	@Test
	@DisplayName("Deve detetar navios na área 3x3 na primeira utilização do radar e devolver -1 na segunda")
	void testRadar() {
		// Coloca um navio no canto (0,0) para testar também posições fora do tabuleiro (-1,-1) no ciclo do radar
		Ship barge = new Barge(Compass.NORTH, new Position(0, 0));
		game.getMyFleet().addShip(barge);

		int detected = game.radar(new Position(0, 0));
		assertEquals(1, detected, "Deve detetar 1 navio na área 3x3.");

		// Segunda chamada deve devolver -1 porque o radar já foi usado
		int secondUse = game.radar(new Position(0, 0));
		assertEquals(-1, secondUse, "Deve devolver -1 se o radar já tiver sido utilizado.");
	}
	@Test
	@DisplayName("Deve imprimir o tabuleiro do inimigo sem lançar exceções")
	void testPrintAlienBoard() {
		assertDoesNotThrow(() -> game.printAlienBoard(true, true));
		assertDoesNotThrow(() -> game.printAlienBoard(false, false));
	}

	@Test
	@DisplayName("Deve terminar o jogo por timeout e impedir novas jogadas quando o tempo esgota")
	void testGameTimeout() throws InterruptedException {
		Game fastGame = new Game(new Fleet(), Duration.ofMillis(50));
		Thread.sleep(120); // Aguarda o relógio expirar

		assertTrue(fastGame.isFinished(), "O jogo deve estar terminado após o tempo esgotar.");
		assertThrows(IllegalStateException.class, () ->
				fastGame.fireShots(List.of(new Position(0, 0), new Position(0, 1), new Position(0, 2)))
		);
	}

	@Test
	@DisplayName("Deve detetar tiros repetidos dentro da mesma rajada")
	void testRepeatedShotInSameMove() {
		Position p1 = new Position(4, 4);
		Position p2 = new Position(4, 5);
		game.fireShots(List.of(p1, p1, p2));

		assertEquals(1, game.getRepeatedShots(), "Deve contar 1 tiro repetido na mesma rajada.");
	}

	@Test
	@DisplayName("Deve executar randomEnemyFire mesmo quando restam menos de 3 posições disponíveis")
	void testRandomEnemyFireLowPositions() {
		// Dispara em 98 posições (32 rajadas de 3 + 2 posições) para deixar apenas 2 casas livres
		List<IPosition> allPositions = new ArrayList<>();
		for (int r = 0; r < Game.BOARD_SIZE; r++) {
			for (int c = 0; c < Game.BOARD_SIZE; c++) {
				allPositions.add(new Position(r, c));
			}
		}
		// Faz 33 rajadas = 99 tiros (sobra apenas 1 posição livre no tabuleiro 10x10)
		for (int i = 0; i < 99; i += 3) {
			game.fireShots(List.of(allPositions.get(i), allPositions.get(i + 1), allPositions.get(i + 2)));
		}

		String json = game.randomEnemyFire();
		assertNotNull(json);
	}
}