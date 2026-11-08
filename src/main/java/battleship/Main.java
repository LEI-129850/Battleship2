/**
 * 
 */
package battleship;

public class Main
{
	/**
	 * Main.
	 *
	 * @param args the args
	 */
	public static void main(String[] args)
    {
		System.out.println("***  Battleship  ***");

		Tasks.menu();
    }

	/**
	 * Método criado deliberadamente para provocar o Code Smell "MethodCoupling" > 7.
	 * Acopla com 9 classes/interfaces do domínio:
	 * Compass, IPosition, Position, Barge, Caravel, Carrack, Frigate, Galleon, Move.
	 */
	public void provocarCheiroAcoplamento() {
		// 1. Compass
		Compass orientacao = Compass.NORTH;

		// 2 e 3. IPosition e Position
		IPosition pos = new Position(0, 0);

		// 4, 5, 6, 7 e 8. Instanciação de todos os tipos de barcos
		Barge barge = new Barge(orientacao, pos);
		Caravel caravel = new Caravel(orientacao, pos);
		Carrack carrack = new Carrack(orientacao, pos);
		Frigate frigate = new Frigate(orientacao, pos);
		Galleon galleon = new Galleon(orientacao, pos);

		// 9. Move
		Move move = new Move(1, new java.util.ArrayList<>(), new java.util.ArrayList<>());

		// Usa as instâncias para garantir que o compilador/analisador não as ignora
		System.out.println("Tamanho total: " +
				(barge.getSize() + caravel.getSize() + carrack.getSize() +
						frigate.getSize() + galleon.getSize() + move.getNumber()));
	}
}
