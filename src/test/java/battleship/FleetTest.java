package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for battleship.Fleet.
 * Author: LEI-129830
 * Date: 2026-11-21 10:15
 * Cyclomatic Complexity:
 * - Fleet(): 1
 * - createRandom(): 4
 * - getShips(): 1
 * - addShip(IShip): 4
 * - getShipsLike(String): 3
 * - getFloatingShips(): 3
 * - getSunkShips(): 3
 * - shipAt(IPosition): 3
 * - collisionRisk(IShip): 3 (covered through addShip)
 * - printShips(List<IShip>): 2
 * - printStatus(): 1
 * - printShipsByCategory(String): 1
 * - printFloatingShips(): 1
 * - printAllShips(): 1
 */
class FleetTest {

	private Fleet fleet;
	private PrintStream originalOut;
	private ByteArrayOutputStream output;

	@BeforeEach
	void setUp() {
		fleet = new Fleet();
		originalOut = System.out;
		output = new ByteArrayOutputStream();
		System.setOut(new PrintStream(output));
	}

	@AfterEach
	void tearDown() {
		System.setOut(originalOut);
		fleet = null;
		output = null;
		originalOut = null;
	}

	private Barge bargeAt(int row, int column) {
		return new Barge(Compass.NORTH, new Position(row, column));
	}

	private String printedOutput() {
		return output.toString(StandardCharsets.UTF_8);
	}

	private void addSeparatedBarges(int count) {
		for (int i = 0; i < count; i++) {
			int row = (i / 5) * 2;
			int column = (i % 5) * 2;
			assertTrue(fleet.addShip(bargeAt(row, column)),
					"Error: expected a separated barge at (" + row + ", " + column + ") to be added");
		}
	}

	@Test
	@DisplayName("Constructor creates an empty fleet")
	void constructor() {
		assertTrue(fleet.getShips().isEmpty(),
				"Error: expected a newly constructed fleet to contain no ships");
	}

	@Test
	@DisplayName("createRandom creates a full fleet")
	void createRandom1() {
		IFleet randomFleet = Fleet.createRandom();

		assertEquals(IFleet.FLEET_SIZE, randomFleet.getShips().size(),
				"Error: expected the randomly generated fleet to contain " + IFleet.FLEET_SIZE + " ships");
	}

	@Test
	@DisplayName("createRandom returns only ships placed inside the board")
	void createRandom2() {
		IFleet randomFleet = Fleet.createRandom();

		assertTrue(randomFleet.getShips().stream().allMatch(IShip::isInsideBoard),
				"Error: expected every randomly generated ship to be inside the board");
	}

	@Test
	@DisplayName("createRandom returns ships that do not collide or touch")
	void createRandom3() {
		IFleet randomFleet = Fleet.createRandom();
		List<IShip> ships = randomFleet.getShips();

		for (int i = 0; i < ships.size(); i++) {
			for (int j = i + 1; j < ships.size(); j++) {
				assertFalse(ships.get(i).tooCloseTo(ships.get(j)),
						"Error: expected generated ships not to collide or touch");
			}
		}
	}

	@Test
	@DisplayName("createRandom retries placements until the fleet is full")
	void createRandom4() {
		IFleet randomFleet = Fleet.createRandom();

		assertEquals(IFleet.FLEET_SIZE, randomFleet.getShips().size(),
				"Error: expected retries to eventually produce a full fleet");
	}

	@Test
	@DisplayName("getShips returns the fleet's ship list")
	void getShips() {
		Barge ship = bargeAt(0, 0);
		fleet.addShip(ship);

		assertEquals(List.of(ship), fleet.getShips(),
				"Error: expected getShips() to return the ship added to the fleet");
	}

	@Test
	@DisplayName("addShip accepts a valid, non-adjacent ship")
	void addShip1() {
		Barge ship = bargeAt(0, 0);

		assertTrue(fleet.addShip(ship),
				"Error: expected a valid ship to be accepted by an empty fleet");
	}

	@Test
	@DisplayName("addShip rejects a ship outside the board")
	void addShip2() {
		Barge ship = bargeAt(-1, 0);

		assertFalse(fleet.addShip(ship),
				"Error: expected a ship outside the board to be rejected");
	}

	@Test
	@DisplayName("addShip rejects a ship adjacent to an existing ship")
	void addShip3() {
		fleet.addShip(bargeAt(0, 0));

		assertFalse(fleet.addShip(bargeAt(0, 1)),
				"Error: expected a ship adjacent to an existing ship to be rejected");
	}

	@Test
	@DisplayName("addShip rejects a ship when the fleet already exceeds its allowed size")
	void addShip4() {
		addSeparatedBarges(IFleet.FLEET_SIZE + 1);

		assertFalse(fleet.addShip(bargeAt(8, 8)),
				"Error: expected a ship to be rejected once the fleet contains more than FLEET_SIZE ships");
	}

	@Test
	@DisplayName("addShip rejects null with AssertionError")
	void addShip5() {
		assertThrows(AssertionError.class,
				() -> fleet.addShip(null),
				"Error: expected addShip(null) to throw AssertionError");
	}

	@Test
	@DisplayName("getShipsLike returns an empty list for an empty fleet")
	void getShipsLike1() {
		assertTrue(fleet.getShipsLike("Barca").isEmpty(),
				"Error: expected no matching ships in an empty fleet");
	}

	@Test
	@DisplayName("getShipsLike excludes ships with a different category")
	void getShipsLike2() {
		fleet.addShip(bargeAt(0, 0));

		assertTrue(fleet.getShipsLike("Galeao").isEmpty(),
				"Error: expected ships of a different category not to be returned");
	}

	@Test
	@DisplayName("getShipsLike returns ships with the requested category")
	void getShipsLike3() {
		Barge ship = bargeAt(0, 0);
		fleet.addShip(ship);

		assertEquals(List.of(ship), fleet.getShipsLike("Barca"),
				"Error: expected getShipsLike() to return the matching barge");
	}

	@Test
	@DisplayName("getShipsLike rejects null category")
	void getShipsLike4() {
		assertThrows(AssertionError.class,
				() -> fleet.getShipsLike(null),
				"Error: expected getShipsLike(null) to throw AssertionError");
	}


	@Test
	@DisplayName("getFloatingShips returns an empty list for an empty fleet")
	void getFloatingShips1() {
		assertTrue(fleet.getFloatingShips().isEmpty(),
				"Error: expected an empty fleet to have no floating ships");
	}

	@Test
	@DisplayName("getFloatingShips includes an unhit ship")
	void getFloatingShips2() {
		Barge ship = bargeAt(0, 0);
		fleet.addShip(ship);

		assertEquals(List.of(ship), fleet.getFloatingShips(),
				"Error: expected an unhit ship to be included among floating ships");
	}

	@Test
	@DisplayName("getFloatingShips excludes a sunk ship")
	void getFloatingShips3() {
		Barge ship = bargeAt(0, 0);
		fleet.addShip(ship);
		ship.sink();

		assertTrue(fleet.getFloatingShips().isEmpty(),
				"Error: expected a sunk ship not to be included among floating ships");
	}

	@Test
	@DisplayName("getSunkShips returns an empty list for an empty fleet")
	void getSunkShips1() {
		assertTrue(fleet.getSunkShips().isEmpty(),
				"Error: expected an empty fleet to have no sunk ships");
	}

	@Test
	@DisplayName("getSunkShips excludes a floating ship")
	void getSunkShips2() {
		fleet.addShip(bargeAt(0, 0));

		assertTrue(fleet.getSunkShips().isEmpty(),
				"Error: expected a floating ship not to be included among sunk ships");
	}

	@Test
	@DisplayName("getSunkShips includes a sunk ship")
	void getSunkShips3() {
		Barge ship = bargeAt(0, 0);
		fleet.addShip(ship);
		ship.sink();

		assertEquals(List.of(ship), fleet.getSunkShips(),
				"Error: expected a sunk ship to be included in the sunk ships list");
	}

	@Test
	@DisplayName("shipAt returns null when the fleet is empty")
	void shipAt1() {
		assertNull(fleet.shipAt(new Position(0, 0)),
				"Error: expected no ship at a position in an empty fleet");
	}

	@Test
	@DisplayName("shipAt returns null when no ship occupies the requested position")
	void shipAt2() {
		fleet.addShip(bargeAt(0, 0));

		assertNull(fleet.shipAt(new Position(5, 5)),
				"Error: expected null when no ship occupies the requested position");
	}

	@Test
	@DisplayName("shipAt returns the ship occupying the requested position")
	void shipAt3() {
		Barge ship = bargeAt(0, 0);
		fleet.addShip(ship);

		assertSame(ship, fleet.shipAt(new Position(0, 0)),
				"Error: expected shipAt() to return the ship occupying the position");
	}

	@Test
	@DisplayName("shipAt rejects null position")
	void shipAt4() {
		assertThrows(AssertionError.class,
				() -> fleet.shipAt(null),
				"Error: expected shipAt(null) to throw AssertionError");
	}

	@Test
	@DisplayName("printShips prints no ship details for an empty list")
	void printShips1() {
		fleet.printShips(List.of());

		assertEquals("", printedOutput(),
				"Error: expected printing an empty ship list to produce no output");
	}

	@Test
	@DisplayName("printShips prints each ship in the list")
	void printShips2() {
		fleet.printShips(List.of(bargeAt(0, 0)));

		assertTrue(printedOutput().contains("Barca"),
				"Error: expected printed ship details to include the barge category");
	}

	@Test
	@DisplayName("printShips rejects null ship list")
	void printShips3() {
		assertThrows(AssertionError.class,
				() -> fleet.printShips(null),
				"Error: expected printShips(null) to throw AssertionError");
	}

	@Test
	@DisplayName("printStatus reports floating and sunk ship counts")
	void printStatus() {
		Barge floating = bargeAt(0, 0);
		Barge sunk = bargeAt(0, 2);
		fleet.addShip(floating);
		fleet.addShip(sunk);
		sunk.sink();

		fleet.printStatus();

		assertTrue(printedOutput().contains("1 a flutuar, 1 afundados"),
				"Error: expected status output to report one floating ship and one sunk ship");
	}

	@Test
	@DisplayName("printShipsByCategory prints ships of the requested category")
	void printShipsByCategory() {
		fleet.addShip(bargeAt(0, 0));

		fleet.printShipsByCategory("Barca");

		assertTrue(printedOutput().contains("Barca"),
				"Error: expected category output to include the matching barge");
	}

	@Test
	@DisplayName("printShipsByCategory rejects null category")
	void printShipsByCategory1() {
		assertThrows(AssertionError.class,
				() -> fleet.printShipsByCategory(null),
				"Error: expected printShipsByCategory(null) to throw AssertionError");
	}

	@Test
	@DisplayName("printFloatingShips prints floating ships")
	void printFloatingShips() {
		fleet.addShip(bargeAt(0, 0));

		fleet.printFloatingShips();

		assertTrue(printedOutput().contains("Barca"),
				"Error: expected floating ship output to include the unhit barge");
	}

	@Test
	@DisplayName("printAllShips prints every ship in the fleet")
	void printAllShips() {
		fleet.addShip(bargeAt(0, 0));

		fleet.printShips(fleet.getShips());

		assertTrue(printedOutput().contains("Barca"),
				"Error: expected all-ship output to include the fleet's barge");
	}

	@Test
	@DisplayName("collisionRisk rejects null when called directly")
	void collisionRiskAssertion1() throws ReflectiveOperationException {
		Method collisionRisk = Fleet.class.getDeclaredMethod("collisionRisk", IShip.class);
		collisionRisk.setAccessible(true);

		InvocationTargetException exception = assertThrows(
				InvocationTargetException.class,
				() -> collisionRisk.invoke(fleet, new Object[] { null }),
				"Error: expected direct collisionRisk(null) invocation to throw through reflection");

		assertEquals(AssertionError.class, exception.getCause().getClass(),
				"Error: expected the reflected method's cause to be AssertionError");
	}


}