package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários de caixa-branca para a classe {@link Frigate}.
 * Valida o construtor em todas as orientações de {@link Compass} (NORTH, SOUTH, EAST, WEST),
 * casos inválidos com argumentos nulos, tamanho, categoria e estado inicial do navio.
 */
class FrigateTest {

	private Frigate frigate;

	@BeforeEach
	void setUp() {
		frigate = new Frigate(Compass.NORTH, new Position(5, 5));
	}

	@AfterEach
	void tearDown() {
		frigate = null;
	}

	@Test
	@DisplayName("Deve criar uma fragata orientada a NORTH com as 4 posições verticais corretas")
	void testConstructorNorth() {
		assertNotNull(frigate, "A instância de Frigate não deve ser nula");
		assertEquals("Fragata", frigate.getCategory(), "A categoria deve ser 'Fragata'");
		assertEquals(Compass.NORTH, frigate.getBearing(), "A orientação deve ser NORTH");
		assertEquals(new Position(5, 5), frigate.getPosition(), "A posição inicial deve ser (5, 5)");
		assertEquals(4, frigate.getSize(), "O tamanho da fragata deve ser 4");

		List<IPosition> positions = frigate.getPositions();
		assertNotNull(positions, "A lista de posições não deve ser nula");
		assertEquals(4, positions.size(), "A fragata deve ocupar exatamente 4 posições");
		assertEquals(new Position(5, 5), positions.get(0), "A 1.ª posição para NORTH está incorreta");
		assertEquals(new Position(6, 5), positions.get(1), "A 2.ª posição para NORTH está incorreta");
		assertEquals(new Position(7, 5), positions.get(2), "A 3.ª posição para NORTH está incorreta");
		assertEquals(new Position(8, 5), positions.get(3), "A 4.ª posição para NORTH está incorreta");
	}

	@Test
	@DisplayName("Deve criar uma fragata orientada a SOUTH com as 4 posições verticais corretas")
	void testConstructorSouth() {
		Frigate southFrigate = new Frigate(Compass.SOUTH, new Position(2, 3));
		List<IPosition> positions = southFrigate.getPositions();

		assertEquals("Fragata", southFrigate.getCategory(), "A categoria deve ser 'Fragata'");
		assertEquals(Compass.SOUTH, southFrigate.getBearing(), "A orientação deve ser SOUTH");
		assertEquals(new Position(2, 3), southFrigate.getPosition(), "A posição inicial deve ser (2, 3)");
		assertEquals(4, southFrigate.getSize(), "O tamanho da fragata deve ser 4");
		assertNotNull(positions, "A lista de posições não deve ser nula");
		assertEquals(4, positions.size(), "A fragata deve ocupar exatamente 4 posições");
		assertEquals(new Position(2, 3), positions.get(0), "A 1.ª posição para SOUTH está incorreta");
		assertEquals(new Position(3, 3), positions.get(1), "A 2.ª posição para SOUTH está incorreta");
		assertEquals(new Position(4, 3), positions.get(2), "A 3.ª posição para SOUTH está incorreta");
		assertEquals(new Position(5, 3), positions.get(3), "A 4.ª posição para SOUTH está incorreta");
	}

	@Test
	@DisplayName("Deve criar uma fragata orientada a EAST com as 4 posições horizontais corretas")
	void testConstructorEast() {
		Frigate eastFrigate = new Frigate(Compass.EAST, new Position(4, 1));
		List<IPosition> positions = eastFrigate.getPositions();

		assertEquals("Fragata", eastFrigate.getCategory(), "A categoria deve ser 'Fragata'");
		assertEquals(Compass.EAST, eastFrigate.getBearing(), "A orientação deve ser EAST");
		assertEquals(new Position(4, 1), eastFrigate.getPosition(), "A posição inicial deve ser (4, 1)");
		assertEquals(4, eastFrigate.getSize(), "O tamanho da fragata deve ser 4");
		assertNotNull(positions, "A lista de posições não deve ser nula");
		assertEquals(4, positions.size(), "A fragata deve ocupar exatamente 4 posições");
		assertEquals(new Position(4, 1), positions.get(0), "A 1.ª posição para EAST está incorreta");
		assertEquals(new Position(4, 2), positions.get(1), "A 2.ª posição para EAST está incorreta");
		assertEquals(new Position(4, 3), positions.get(2), "A 3.ª posição para EAST está incorreta");
		assertEquals(new Position(4, 4), positions.get(3), "A 4.ª posição para EAST está incorreta");
	}

	@Test
	@DisplayName("Deve criar uma fragata orientada a WEST com as 4 posições horizontais corretas")
	void testConstructorWest() {
		Frigate westFrigate = new Frigate(Compass.WEST, new Position(0, 6));
		List<IPosition> positions = westFrigate.getPositions();

		assertEquals("Fragata", westFrigate.getCategory(), "A categoria deve ser 'Fragata'");
		assertEquals(Compass.WEST, westFrigate.getBearing(), "A orientação deve ser WEST");
		assertEquals(new Position(0, 6), westFrigate.getPosition(), "A posição inicial deve ser (0, 6)");
		assertEquals(4, westFrigate.getSize(), "O tamanho da fragata deve ser 4");
		assertNotNull(positions, "A lista de posições não deve ser nula");
		assertEquals(4, positions.size(), "A fragata deve ocupar exatamente 4 posições");
		assertEquals(new Position(0, 6), positions.get(0), "A 1.ª posição para WEST está incorreta");
		assertEquals(new Position(0, 7), positions.get(1), "A 2.ª posição para WEST está incorreta");
		assertEquals(new Position(0, 8), positions.get(2), "A 3.ª posição para WEST está incorreta");
		assertEquals(new Position(0, 9), positions.get(3), "A 4.ª posição para WEST está incorreta");
	}

	@Test
	@DisplayName("Deve lançar NullPointerException quando a orientação (Compass) for nula")
	void testConstructorNullBearing() {
		NullPointerException ex = assertThrows(
				NullPointerException.class,
				() -> new Frigate(null, new Position(5, 5)),
				"Deve lançar NullPointerException quando a orientação é nula"
		);
		assertEquals("Ship's bearing must not be null", ex.getMessage());
	}

	@Test
	@DisplayName("Deve lançar NullPointerException quando a posição inicial for nula")
	void testConstructorNullPosition() {
		NullPointerException ex = assertThrows(
				NullPointerException.class,
				() -> new Frigate(Compass.NORTH, null),
				"Deve lançar NullPointerException quando a posição é nula"
		);
		assertEquals("Ship's position must not be null", ex.getMessage());
	}

	@Test
	@DisplayName("Deve lançar NullPointerException quando orientação e posição forem ambas nulas")
	void testConstructorNullBearingAndPosition() {
		NullPointerException ex = assertThrows(
				NullPointerException.class,
				() -> new Frigate(null, null),
				"Deve lançar NullPointerException quando orientação e posição são nulas"
		);
		assertEquals("Ship's bearing must not be null", ex.getMessage());
	}

	@Test
	@DisplayName("Deve retornar tamanho 4 em getSize() e confirmar categoria e estado inicial intacto")
	void testGetSizeAndInitialState() {
		assertEquals(4, frigate.getSize(), "O tamanho da fragata deve ser 4");
		assertEquals("Fragata", frigate.getCategory(), "A categoria inicial deve ser 'Fragata'");
		assertTrue(frigate.stillFloating(), "Uma fragata recém-criada deve estar a flutuar");
		assertTrue(frigate.isInsideBoard(), "A fragata em (5, 5) orientada a NORTH deve estar dentro do tabuleiro");
		for (IPosition pos : frigate.getPositions()) {
			assertFalse(pos.isHit(), "Nenhuma posição da fragata deve estar atingida inicialmente");
		}
	}

	@Test
	@DisplayName("Deve continuar a flutuar quando apenas parte das posições é atingida e afundar quando todas são atingidas")
	void testStillFloatingAfterHits() {
		assertTrue(frigate.stillFloating(), "A fragata deve começar a flutuar");

		frigate.getPositions().get(0).shoot();
		assertTrue(frigate.stillFloating(), "A fragata deve continuar a flutuar com apenas 1 posição atingida");

		frigate.getPositions().forEach(IPosition::shoot);
		assertFalse(frigate.stillFloating(), "A fragata deve afundar quando todas as 4 posições são atingidas");
	}

	@Test
	@DisplayName("Deve calcular corretamente os limites espaciais de uma fragata vertical (NORTH)")
	void testSpatialBoundsVertical() {
		assertEquals(5, frigate.getTopMostPos(), "A linha mais superior deve ser 5");
		assertEquals(8, frigate.getBottomMostPos(), "A linha mais inferior deve ser 8");
		assertEquals(5, frigate.getLeftMostPos(), "A coluna mais à esquerda deve ser 5");
		assertEquals(5, frigate.getRightMostPos(), "A coluna mais à direita deve ser 5");
	}

	@Test
	@DisplayName("Deve calcular corretamente os limites espaciais de uma fragata horizontal (EAST)")
	void testSpatialBoundsHorizontal() {
		Frigate eastFrigate = new Frigate(Compass.EAST, new Position(3, 2));

		assertEquals(3, eastFrigate.getTopMostPos(), "A linha mais superior deve ser 3");
		assertEquals(3, eastFrigate.getBottomMostPos(), "A linha mais inferior deve ser 3");
		assertEquals(2, eastFrigate.getLeftMostPos(), "A coluna mais à esquerda deve ser 2");
		assertEquals(5, eastFrigate.getRightMostPos(), "A coluna mais à direita deve ser 5");
	}
}