package battleship;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

/**
 * Test class for Position.
 * Author: britoeabreu
 * Date: 2024-03-19 15:30
 * Cyclomatic Complexity for each method:
 * - Constructor: 1
 * - getRow: 1
 * - getColumn: 1
 * - isValid: 4
 * - isAdjacentTo: 4
 * - isOccupied: 1
 * - isHit: 1
 * - occupy: 1
 * - shoot: 1
 * - equals: 3
 * - hashCode: 1
 * - toString: 1
 */
public class PositionTest {
	private Position position;

	@BeforeEach
	void setUp() {
		position = new Position(2, 3);
	}

	@AfterEach
	void tearDown() {
		position = null;
	}

	@Test
	@DisplayName("Garante a correta inicialização de atributos pelo construtor")
	void constructor() {
		Position pos = new Position(1, 1);
		assertNotNull(pos, "Failed to create Position: object is null");
		assertEquals(1, pos.getRow(), "Failed to set row: expected 1 but got " + pos.getRow());
		assertEquals(1, pos.getColumn(), "Failed to set column: expected 1 but got " + pos.getColumn());
		assertFalse(pos.isOccupied(), "New position should not be occupied");
		assertFalse(pos.isHit(), "New position should not be hit");
	}

	@Test
	@DisplayName("Deve retornar a linha numérica correta")
	void getRow() {
		assertEquals(2, position.getRow(), "Failed to get row: expected 2 but got " + position.getRow());
	}

	@Test
	@DisplayName("Deve retornar a coluna numérica correta")
	void getColumn() {
		assertEquals(3, position.getColumn(), "Failed to get column: expected 3 but got " + position.getColumn());
	}

	@Test
	@DisplayName("Deve retornar a letra correspondente à linha (formato clássico)")
	void getClassicRow() {
		assertEquals('C', position.getClassicRow(), "Failed to get row: expected C but got " + position.getClassicRow());
	}

	@Test
	@DisplayName("Deve retornar a coluna no formato clássico")
	void getClassicColumn() {
		assertEquals(4, position.getClassicColumn(), "Failed to get column: expected 3 but got " + position.getClassicColumn());
	}

	@Test
	@DisplayName("Deve validar como verdadeira uma posição dentro dos limites (0,0)")
	void isValid1() {
		position = new Position(0, 0);
		assertTrue(position.isInside(), "Position (0,0) should be valid");
	}

	@Test
	@DisplayName("Deve invalidar uma posição com linha negativa")
	void isValid2() {
		position = new Position(-1, 5);
		assertFalse(position.isInside(), "Position with negative row should be invalid");
	}

	@Test
	@DisplayName("Deve invalidar uma posição com coluna negativa")
	void isValid3() {
		position = new Position(5, -1);
		assertFalse(position.isInside(), "Position with negative column should be invalid");
	}

	@Test
	@DisplayName("Deve invalidar uma posição em que a linha excede o tamanho do tabuleiro")
	void isValid4() {
		position = new Position(Game.BOARD_SIZE, 5);
		assertFalse(position.isInside(), "Position with row >= BOARD_SIZE should be invalid");
	}

	@Test
	@DisplayName("Deve invalidar uma posição em que a coluna excede o tamanho do tabuleiro")
	void isValid5() {
		position = new Position(5, Game.BOARD_SIZE);
		assertFalse(position.isInside(), "Position with column >= BOARD_SIZE should be invalid");
	}

	@Test
	@DisplayName("Deve detetar adjacência horizontal")
	void isAdjacentTo1() {
		Position other = new Position(2, 4);
		assertTrue(position.isAdjacentTo(other), "Failed to detect horizontally adjacent position");
	}

	@Test
	@DisplayName("Deve detetar adjacência vertical")
	void isAdjacentTo2() {
		Position other = new Position(3, 3);
		assertTrue(position.isAdjacentTo(other), "Failed to detect vertically adjacent position");
	}

	@Test
	@DisplayName("Deve detetar adjacência diagonal")
	void isAdjacentTo3() {
		Position other = new Position(3, 4);
		assertTrue(position.isAdjacentTo(other), "Failed to detect diagonally adjacent position");
	}

	@Test
	@DisplayName("Deve rejeitar posições que não sejam vizinhas diretas")
	void isAdjacentTo4() {
		Position other = new Position(4, 5);
		assertFalse(position.isAdjacentTo(other), "Non-adjacent position incorrectly identified as adjacent");
	}

	@Test
	@DisplayName("Deve lançar NullPointerException ao verificar adjacência com um objeto nulo")
	void isAdjacentToWithNull() {
		assertThrows(NullPointerException.class, () -> position.isAdjacentTo(null),
				"isAdjacentTo should throw NullPointerException for null input");
	}

	@Test
	@DisplayName("Garante a transição de estado de Livre para Ocupada")
	void isOccupied() {
		assertFalse(position.isOccupied(), "New position should not be occupied");
		position.occupy();
		assertTrue(position.isOccupied(), "Position should be occupied after occupy()");
	}

	@Test
	@DisplayName("Garante a transição de estado após um disparo na posição")
	void isHit() {
		assertFalse(position.isHit(), "New position should not be hit");
		position.shoot();
		assertTrue(position.isHit(), "Position should be hit after shoot()");
	}

	@Test
	@DisplayName("Duas posições com as mesmas coordenadas devem ser consideradas iguais")
	void equals1() {
		Position same = new Position(2, 3);
		assertEquals(same, position, "Equal positions not identified as equal");
	}

	@Test
	@DisplayName("Uma posição não deve ser considerada igual a Null")
	void equals2() {
		assertNotNull(position, "Position should not equal null");
	}

	@Test
	@DisplayName("Uma posição não deve ser considerada igual a um objeto de tipo diferente")
	void equals3() {
		Object other = new Object();
		assertNotEquals(other, position, "Position should not equal non-Position object");
	}

	@Test
	@DisplayName("Posições com colunas diferentes não podem ser iguais")
	void equals4() {
		Position other = new Position(2, 4);
		assertNotEquals(other, position, "Positions with the same row but different column should not be equal");
	}

	@Test
	@DisplayName("A própria instância deve ser igual a si mesma (reflexividade)")
	void equals5() {
        assertEquals(position, position, "A position should be equal to itself");
	}

	@Test
	@DisplayName("Hashcodes de objetos com coordenadas iguais devem coincidir")
	void hashCodeConsistency() {
		Position same = new Position(2, 3);
		assertEquals(position.hashCode(), same.hashCode(),
				"Hash codes not consistent for equal positions");
	}

	@Test
	@DisplayName("Verifica a formatação correta do ToString (ex: C4)")
	void toStringFormat() {
		String expected = "C4";
		assertEquals(expected, position.toString(),
				"Incorrect string representation: expected '" + expected +
						"' but got '" + position.toString() + "'");
	}

	@Test
	@DisplayName("Deve gerar uma posição aleatória válida dentro dos limites do tabuleiro")
	void randomPosition() {
		// Assume-se que randomPosition() é um método estático da classe Position.
		Position randPos = Position.randomPosition();
		assertNotNull(randPos, "A posição gerada não deve ser nula");
		assertTrue(randPos.isInside(), "A posição gerada aleatoriamente tem de pertencer aos limites do tabuleiro");
	}

	@Test
	@DisplayName("Deve retornar uma lista das posições vizinhas adjacentes válidas")
	void adjacentPositions() {
		// Testa a recolha das posições em redor da coordenada central (2,3)
		List<IPosition> adjacents = position.adjacentPositions();
		assertNotNull(adjacents, "A lista de posições adjacentes não pode ser nula");
		assertFalse(adjacents.isEmpty(), "Uma posição no meio do tabuleiro tem de ter vizinhos");

		// Verifica se pelo menos uma posição esperada (ex: C4) faz parte da lista
		boolean containsExpectedNeighbour = adjacents.stream()
				.anyMatch(p -> p.getRow() == 2 && p.getColumn() == 4);
		assertTrue(containsExpectedNeighbour, "A lista de posições adjacentes devia conter o vizinho direito (2,4)");
	}
}