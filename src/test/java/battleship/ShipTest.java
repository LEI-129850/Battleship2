package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de caixa-branca para a classe abstrata {@link Ship} e as suas subclasses.
 * Valida a integridade dos atributos, extremos espaciais, colisões, tiros, adjacências e construtores.
 */
class ShipTest {

    private Ship ship;
    private Ship shipBorder;

    /**
     * Subclasse concreta auxiliar para testar diretamente ramos específicos da classe abstrata Ship.
     */
    static class CustomShip extends Ship {
        CustomShip(String category, Compass bearing, IPosition pos, int size) {
            super(new ShipSpecs(category, size), new ShipPlacement(bearing, pos));
        }

        void addPos(int row, int col) {
            this.positions.add(new Position(row, col));
        }
    }

    @BeforeEach
    void setUp() {
        ship = new Barge(Compass.NORTH, new Position(5, 5));
        shipBorder = new Barge(Compass.NORTH, new Position(0, 0));
    }

    @AfterEach
    void tearDown() {
        ship = null;
        shipBorder = null;
    }

    // --- Construtor e Validação de Nulos ---

    @Test
    @DisplayName("Garante a correta inicialização dos atributos pelo construtor")
    void testConstructor() {
        assertNotNull(ship, "A instância do navio não deve ser nula");
        assertEquals("Barca", ship.getCategory(), "A categoria do navio está incorreta");
        assertEquals(Compass.NORTH, ship.getBearing(), "O bearing do navio está incorreto");
        assertEquals(1, ship.getSize(), "O tamanho do navio está incorreto");
        assertFalse(ship.getPositions().isEmpty(), "As posições do navio não devem estar vazias");
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando a categoria for nula")
    void testConstructorNullCategory() {
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> new CustomShip(null, Compass.NORTH, new Position(1, 1), 1)
        );
        assertEquals("Ship's category must not be null", ex.getMessage());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando o bearing for nulo")
    void testConstructorNullBearing() {
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> new CustomShip("Barca", null, new Position(1, 1), 1)
        );
        assertEquals("Ship's bearing must not be null", ex.getMessage());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando a posição inicial for nula")
    void testConstructorNullPosition() {
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> new CustomShip("Barca", Compass.NORTH, null, 1)
        );
        assertEquals("Ship's position must not be null", ex.getMessage());
    }

    // --- Getters Básicos ---

    @Test
    @DisplayName("Deve retornar a categoria correta do navio")
    void testGetCategory() {
        assertEquals("Barca", ship.getCategory(), "A categoria deve ser 'Barca'");
    }

    @Test
    @DisplayName("Deve retornar o tamanho correto do navio")
    void testGetSize() {
        assertEquals(1, ship.getSize(), "O tamanho da barca deve ser 1");
    }

    @Test
    @DisplayName("Deve retornar o bearing correto do navio")
    void testGetBearing() {
        assertEquals(Compass.NORTH, ship.getBearing(), "O bearing deve ser NORTH");
    }

    @Test
    @DisplayName("Deve retornar a posição inicial de referência do navio")
    void testGetPosition() {
        IPosition pos = ship.getPosition();
        assertNotNull(pos, "A posição do navio não deve ser nula");
        assertEquals(5, pos.getRow(), "A linha da posição de referência deve ser 5");
        assertEquals(5, pos.getColumn(), "A coluna da posição de referência deve ser 5");
    }

    @Test
    @DisplayName("Deve retornar a lista de posições do navio com as coordenadas esperadas")
    void testGetPositions() {
        List<IPosition> positions = ship.getPositions();
        assertNotNull(positions, "A lista de posições não deve ser nula");
        assertEquals(1, positions.size(), "A barca deve ter exatamente uma posição");
        assertEquals(5, positions.get(0).getRow(), "A linha deve ser 5");
        assertEquals(5, positions.get(0).getColumn(), "A coluna deve ser 5");
    }

    // --- stillFloating ---

    @Test
    @DisplayName("Deve retornar true em stillFloating quando o navio não sofreu tiros")
    void testStillFloatingIntact() {
        assertTrue(ship.stillFloating(), "O navio intacto deve reportar stillFloating como true");
    }

    @Test
    @DisplayName("Deve retornar true em stillFloating quando o navio é atingido apenas parcialmente")
    void testStillFloatingPartiallyHit() {
        Caravel caravel = new Caravel(Compass.NORTH, new Position(3, 3));
        assertEquals(2, caravel.getSize(), "A caravela deve ter tamanho 2");

        // Atinge apenas a primeira posição
        caravel.getPositions().get(0).shoot();
        assertTrue(caravel.getPositions().get(0).isHit(), "A primeira posição deve estar atingida");
        assertFalse(caravel.getPositions().get(1).isHit(), "A segunda posição deve estar intacta");

        assertTrue(caravel.stillFloating(), "A caravela deve continuar a flutuar com pelo menos uma célula intacta");
    }

    @Test
    @DisplayName("Deve retornar false em stillFloating quando todas as posições foram atingidas")
    void testStillFloatingCompletelyHit() {
        ship.getPositions().get(0).shoot();
        assertFalse(ship.stillFloating(), "A barca com a sua única posição atingida não deve estar a flutuar");
    }

    // --- shoot ---

    @Test
    @DisplayName("Deve marcar como atingida a posição correta quando o tiro acerta no navio")
    void testShootValidPosition() {
        Position target = new Position(5, 5);
        ship.shoot(target);
        assertTrue(ship.getPositions().get(0).isHit(), "A posição atingida deve ficar com isHit como true");
    }

    @Test
    @DisplayName("Não deve marcar a posição do navio quando o tiro é disparado numa coordenada diferente")
    void testShootNonMatchingPosition() {
        Position target = new Position(0, 0);
        ship.shoot(target);
        assertFalse(ship.getPositions().get(0).isHit(), "O tiro fora não deve alterar o estado do navio");
    }

    @Test
    @DisplayName("Deve atingir apenas a célula correspondente num navio com múltiplas posições")
    void testShootMultiCellShip() {
        Caravel caravel = new Caravel(Compass.NORTH, new Position(4, 4));
        Position target = new Position(5, 4);

        caravel.shoot(target);

        assertFalse(caravel.getPositions().get(0).isHit(), "A primeira posição (4,4) não deve ter sido atingida");
        assertTrue(caravel.getPositions().get(1).isHit(), "A segunda posição (5,4) deve ter sido atingida");
    }

    // --- occupies ---

    @Test
    @DisplayName("Deve retornar true em occupies quando a posição coincide com a primeira célula")
    void testOccupiesFirstPosition() {
        Position pos = new Position(5, 5);
        assertTrue(ship.occupies(pos), "O navio deve ocupar a posição (5, 5)");
    }

    @Test
    @DisplayName("Deve retornar true em occupies quando a posição coincide com uma célula posterior")
    void testOccupiesSubsequentPosition() {
        Caravel caravel = new Caravel(Compass.NORTH, new Position(4, 4));
        Position secondPos = new Position(5, 4);
        assertTrue(caravel.occupies(secondPos), "A caravela deve ocupar a sua segunda posição (5, 4)");
    }

    @Test
    @DisplayName("Deve retornar false em occupies quando a posição não pertence ao navio")
    void testOccupiesFalse() {
        Position pos = new Position(1, 1);
        assertFalse(ship.occupies(pos), "O navio não deve ocupar uma posição diferente");
    }

    // --- tooCloseTo(IPosition) ---

    @Test
    @DisplayName("Deve retornar true em tooCloseTo(IPosition) quando a posição é adjacente à primeira célula")
    void testTooCloseToPositionFirstCell() {
        Position adjacentPos = new Position(5, 6);
        assertTrue(ship.tooCloseTo(adjacentPos), "A posição (5, 6) deve ser considerada tooCloseTo da barca em (5, 5)");
    }

    @Test
    @DisplayName("Deve retornar true em tooCloseTo(IPosition) quando a posição é adjacente a uma célula posterior")
    void testTooCloseToPositionSubsequentCell() {
        CustomShip multiShip = new CustomShip("Test", Compass.NORTH, new Position(2, 2), 2);
        multiShip.addPos(2, 2);
        multiShip.addPos(2, 3);

        // Posição (2, 4) é adjacente a (2, 3), mas não a (2, 2)
        Position target = new Position(2, 4);
        assertTrue(multiShip.tooCloseTo(target), "Deve retornar true quando a posição é adjacente à segunda célula");
    }

    @Test
    @DisplayName("Deve retornar false em tooCloseTo(IPosition) quando a posição não é adjacente a nenhuma célula")
    void testTooCloseToPositionNotAdjacent() {
        Position farPos = new Position(8, 8);
        assertFalse(ship.tooCloseTo(farPos), "Uma posição distante não deve ser considerada tooCloseTo");
    }

    // --- tooCloseTo(IShip) ---

    @Test
    @DisplayName("Deve retornar true em tooCloseTo(IShip) quando a primeira posição do outro navio é adjacente")
    void testTooCloseToShipFirstCell() {
        Ship nearbyShip = new Barge(Compass.NORTH, new Position(5, 6));
        assertTrue(ship.tooCloseTo(nearbyShip), "Navios em (5,5) e (5,6) devem ser considerados tooCloseTo");
    }

    @Test
    @DisplayName("Deve retornar true em tooCloseTo(IShip) quando apenas uma célula subsequente do outro navio é adjacente")
    void testTooCloseToShipSubsequentCell() {
        CustomShip myShip = new CustomShip("MyShip", Compass.NORTH, new Position(2, 2), 1);
        myShip.addPos(2, 2);

        // otherShip tem a primeira célula em (2, 5) [afastada] e a segunda em (2, 3) [adjacente]
        CustomShip otherShip = new CustomShip("OtherShip", Compass.NORTH, new Position(2, 5), 2);
        otherShip.addPos(2, 5);
        otherShip.addPos(2, 3);

        assertTrue(myShip.tooCloseTo(otherShip), "Deve retornar true na segunda iteração do outro navio");
    }

    @Test
    @DisplayName("Deve retornar false em tooCloseTo(IShip) quando os navios estão afastados")
    void testTooCloseToShipFar() {
        Ship farShip = new Barge(Compass.NORTH, new Position(9, 9));
        assertFalse(ship.tooCloseTo(farShip), "Navios distantes não devem ser considerados tooCloseTo");
    }

    // --- Extremos de Coordenadas (getTopMostPos, getBottomMostPos, getLeftMostPos, getRightMostPos) ---

    @Test
    @DisplayName("Deve calcular corretamente a linha mais acima (menor valor de linha)")
    void testGetTopMostPos() {
        assertEquals(5, ship.getTopMostPos(), "Barca em (5,5) deve ter topMostPos igual a 5");

        // CustomShip para cobrir os ramos true e false de: if (row < top)
        CustomShip multi = new CustomShip("Multi", Compass.NORTH, new Position(5, 5), 3);
        multi.addPos(5, 5);
        multi.addPos(2, 5); // 2 < 5 -> ramo TRUE
        multi.addPos(7, 5); // 7 < 2 -> ramo FALSE
        assertEquals(2, multi.getTopMostPos(), "O topMostPos deve ser 2");
    }

    @Test
    @DisplayName("Deve calcular corretamente a linha mais abaixo (maior valor de linha)")
    void testGetBottomMostPos() {
        assertEquals(5, ship.getBottomMostPos(), "Barca em (5,5) deve ter bottomMostPos igual a 5");

        // CustomShip para cobrir os ramos true e false de: if (row > bottom)
        CustomShip multi = new CustomShip("Multi", Compass.NORTH, new Position(5, 5), 3);
        multi.addPos(5, 5);
        multi.addPos(8, 5); // 8 > 5 -> ramo TRUE
        multi.addPos(3, 5); // 3 > 8 -> ramo FALSE
        assertEquals(8, multi.getBottomMostPos(), "O bottomMostPos deve ser 8");
    }

    @Test
    @DisplayName("Deve calcular corretamente a coluna mais à esquerda (menor valor de coluna)")
    void testGetLeftMostPos() {
        assertEquals(5, ship.getLeftMostPos(), "Barca em (5,5) deve ter leftMostPos igual a 5");

        // CustomShip para cobrir os ramos true e false de: if (col < left)
        CustomShip multi = new CustomShip("Multi", Compass.NORTH, new Position(5, 5), 3);
        multi.addPos(5, 5);
        multi.addPos(5, 2); // 2 < 5 -> ramo TRUE
        multi.addPos(5, 7); // 7 < 2 -> ramo FALSE
        assertEquals(2, multi.getLeftMostPos(), "O leftMostPos deve ser 2");
    }

    @Test
    @DisplayName("Deve calcular corretamente a coluna mais à direita (maior valor de coluna)")
    void testGetRightMostPos() {
        assertEquals(5, ship.getRightMostPos(), "Barca em (5,5) deve ter rightMostPos igual a 5");

        // CustomShip para cobrir os ramos true e false de: if (col > right)
        CustomShip multi = new CustomShip("Multi", Compass.NORTH, new Position(5, 5), 3);
        multi.addPos(5, 5);
        multi.addPos(5, 8); // 8 > 5 -> ramo TRUE
        multi.addPos(5, 3); // 3 > 8 -> ramo FALSE
        assertEquals(8, multi.getRightMostPos(), "O rightMostPos deve ser 8");
    }

    // --- getAdjacentPositions ---

    @Test
    @DisplayName("Deve calcular corretamente as posições adjacentes no centro (8) e na borda (3)")
    void testGetAdjacentPositionsSingleCell() {
        assertEquals(8, ship.getAdjacentPositions().size(),
                "O número de posições adjacentes de uma barca no centro deve ser 8");
        assertEquals(3, shipBorder.getAdjacentPositions().size(),
                "O número de posições adjacentes de uma barca no canto (0,0) deve ser 3");
    }

    @Test
    @DisplayName("Deve filtrar células do próprio navio e evitar duplicados em navios multi-célula")
    void testGetAdjacentPositionsMultiCell() {
        // Caravela horizontal de tamanho 2 nas posições (5, 5) e (5, 6)
        Caravel caravel = new Caravel(Compass.EAST, new Position(5, 5));
        List<IPosition> adj = caravel.getAdjacentPositions();

        // Uma barca de 2 células no centro tem 10 posições adjacentes únicas
        assertEquals(10, adj.size(), "Uma caravela de 2 células no centro deve ter 10 posições adjacentes");

        for (IPosition shipCell : caravel.getPositions()) {
            assertFalse(adj.contains(shipCell), "A lista de adjacências não deve conter nenhuma posição do próprio navio");
        }
    }

    // --- isInsideBoard ---

    @Test
    @DisplayName("Deve retornar true em isInsideBoard quando o navio está totalmente dentro do tabuleiro")
    void testIsInsideBoardFullyInside() {
        assertTrue(ship.isInsideBoard(), "Barca em (5, 5) deve estar dentro do tabuleiro");

        Caravel caravel = new Caravel(Compass.NORTH, new Position(2, 2));
        assertTrue(caravel.isInsideBoard(), "Caravela em (2, 2) deve estar dentro do tabuleiro");
    }

    @Test
    @DisplayName("Deve retornar false em isInsideBoard quando ultrapassa o limite esquerdo")
    void testIsInsideBoardOutOfBoundsLeft() {
        CustomShip outLeft = new CustomShip("Out", Compass.NORTH, new Position(2, -1), 1);
        outLeft.addPos(2, -1);
        assertFalse(outLeft.isInsideBoard(), "Navio com coluna negativa deve retornar false");
    }

    @Test
    @DisplayName("Deve retornar false em isInsideBoard quando ultrapassa o limite direito")
    void testIsInsideBoardOutOfBoundsRight() {
        CustomShip outRight = new CustomShip("Out", Compass.NORTH, new Position(2, 10), 1);
        outRight.addPos(2, 10);
        assertFalse(outRight.isInsideBoard(), "Navio com coluna superior a 9 deve retornar false");
    }

    @Test
    @DisplayName("Deve retornar false em isInsideBoard quando ultrapassa o limite superior")
    void testIsInsideBoardOutOfBoundsTop() {
        CustomShip outTop = new CustomShip("Out", Compass.NORTH, new Position(-1, 2), 1);
        outTop.addPos(-1, 2);
        assertFalse(outTop.isInsideBoard(), "Navio com linha negativa deve retornar false");
    }

    @Test
    @DisplayName("Deve retornar false em isInsideBoard quando ultrapassa o limite inferior")
    void testIsInsideBoardOutOfBoundsBottom() {
        CustomShip outBottom = new CustomShip("Out", Compass.NORTH, new Position(10, 2), 1);
        outBottom.addPos(10, 2);
        assertFalse(outBottom.isInsideBoard(), "Navio com linha superior a 9 deve retornar false");
    }

    @Test
    @DisplayName("Deve retornar true em isInsideBoard nos limites exatos dos cantos do tabuleiro")
    void testIsInsideBoardCorners() {
        Ship topLeft = new Barge(Compass.NORTH, new Position(0, 0));
        assertTrue(topLeft.isInsideBoard(), "Canto superior esquerdo deve ser válido");

        Ship bottomRight = new Barge(Compass.NORTH, new Position(Game.BOARD_SIZE - 1, Game.BOARD_SIZE - 1));
        assertTrue(bottomRight.isInsideBoard(), "Canto inferior direito deve ser válido");
    }

    // --- sink e toString ---

    @Test
    @DisplayName("Deve afundar o navio marcando todas as suas posições como atingidas")
    void testSink() {
        Carrack carrack = new Carrack(Compass.NORTH, new Position(3, 3));
        assertTrue(carrack.stillFloating(), "A nau deve estar a flutuar antes de ser afundada");

        carrack.sink();

        assertFalse(carrack.stillFloating(), "A nau não deve estar a flutuar após sink()");
        for (IPosition pos : carrack.getPositions()) {
            assertTrue(pos.isHit(), "Todas as posições da nau devem estar marcadas como atingidas");
        }
    }

    @Test
    @DisplayName("Deve formatar corretamente a representação em String do navio")
    void testToString() {
        assertEquals("[Barca n F6]", ship.toString(),
                "A representação em String deve conter categoria, bearing e posição");
    }

    // --- buildShip Factory Method ---

    @Test
    @DisplayName("Deve construir corretamente todas as subclasses através de buildShip")
    void testBuildShipAllKinds() {
        Position pos = new Position(2, 2);
        assertBuiltShip("barca", pos, new Barge(Compass.NORTH, pos));
        assertBuiltShip("caravela", pos, new Caravel(Compass.SOUTH, pos));
        assertBuiltShip("nau", pos, new Carrack(Compass.EAST, pos));
        assertBuiltShip("fragata", pos, new Frigate(Compass.WEST, pos));
        assertBuiltShip("galeao", pos, new Galleon(Compass.NORTH, pos));
    }

    private void assertBuiltShip(String kind, Position pos, Ship expected) {
        Ship built = Ship.buildShip(kind, expected.getBearing(), pos);
        assertNotNull(built, "buildShip não deve retornar null para " + kind);
        assertInstanceOf(expected.getClass(), built, "Subclasse incorreta para " + kind);
        assertEquals(expected.getCategory(), built.getCategory(), "Categoria incorreta para " + kind);
        assertEquals(expected.getSize(), built.getSize(), "Tamanho incorreto para " + kind);
    }

    @Test
    @DisplayName("Deve retornar null em buildShip para tipos de navio desconhecidos ou inválidos")
    void testBuildShipUnknownKind() {
        Position pos = new Position(2, 2);
        assertNull(Ship.buildShip("submarino", Compass.NORTH, pos), "Tipo desconhecido deve retornar null");
        assertNull(Ship.buildShip("BARCA", Compass.NORTH, pos), "String em maiúsculas deve retornar null");
        assertNull(Ship.buildShip("", Compass.NORTH, pos), "String vazia deve retornar null");
    }

    // --- Cobertura das Subclasses Concretas em Todas as Orientações ---

    @Test
    @DisplayName("Deve instanciar Barge em todas as orientações do Compass")
    void testBargeAllOrientations() {
        Position pos = new Position(4, 4);
        for (Compass compass : Compass.values()) {
            Barge b = new Barge(compass, pos);
            assertEquals(1, b.getSize());
            assertEquals(compass, b.getBearing());
            assertEquals("Barca", b.getCategory());
        }
    }

    @Test
    @DisplayName("Deve instanciar Caravel em todas as orientações do Compass")
    void testCaravelAllOrientations() {
        Position pos = new Position(3, 3);
        for (Compass compass : Compass.values()) {
            Caravel c = new Caravel(compass, pos);
            assertEquals(2, c.getSize());
            assertEquals(compass, c.getBearing());
            assertEquals(2, c.getPositions().size());
        }
    }

    @Test
    @DisplayName("Deve instanciar Carrack em todas as orientações do Compass")
    void testCarrackAllOrientations() {
        Position pos = new Position(3, 3);
        for (Compass compass : Compass.values()) {
            Carrack k = new Carrack(compass, pos);
            assertEquals(3, k.getSize());
            assertEquals(compass, k.getBearing());
            assertEquals(3, k.getPositions().size());
        }
    }

    @Test
    @DisplayName("Deve instanciar Frigate em todas as orientações do Compass")
    void testFrigateAllOrientations() {
        Position pos = new Position(2, 2);
        for (Compass compass : Compass.values()) {
            Frigate f = new Frigate(compass, pos);
            assertEquals(4, f.getSize());
            assertEquals(compass, f.getBearing());
            assertEquals(4, f.getPositions().size());
        }
    }

    @Test
    @DisplayName("Deve instanciar Galleon em todas as orientações do Compass e validar posições")
    void testGalleonAllOrientations() {
        Position pos = new Position(4, 4);
        for (Compass compass : Compass.values()) {
            Galleon g = new Galleon(compass, pos);
            assertEquals(5, g.getSize());
            assertEquals(compass, g.getBearing());
            assertEquals(5, g.getPositions().size());
            assertEquals("Galeao", g.getCategory());
        }
    }
}
