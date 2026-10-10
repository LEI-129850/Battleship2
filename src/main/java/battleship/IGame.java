/**
 *
 */
package battleship;

import java.time.Duration;
import java.util.List;
import java.util.Scanner;

/**
 * The interface Game.
 */
public interface IGame
{
	/**
	 * Simulates a random enemy firing action on the game board.
	 *
	 * @return a {@code String} indicating the result of the firing action, such as details about a hit, a miss, or other outcomes
	 */
	String randomEnemyFire();

	/**
	 * Reads the result of an enemy firing action from input and interprets it.
	 *
	 * @param in the {@code Scanner} to read the input from, which provides the details of the enemy fire
	 * @return a {@code String} describing the outcome of the enemy fire, such as a hit, a miss, or other results
	 */
	String readEnemyFire(Scanner in);

	/**
	 * Fires a set of shots in a given move.
	 *
	 * @param shots the positions where the shots are fired
	 * @throws IllegalStateException se o tempo de jogo esgotou ({@link #isFinished()})
	 */
	void fireShots(List<IPosition> shots);

	record ShotResult(boolean valid, boolean repeated, IShip ship, boolean sunk) {}

	/**
	 * Fires a single shot at the specified position and indicates whether the shot is valid, repeated,
	 * and if it hit or sank a ship.
	 *
	 * @param pos the position to fire the shot at, represented as an instance of {@code IPosition}
	 * @param isRepeated a boolean indicating if the shot is a repeated attempt at the given position
	 * @return a {@code ShotResult} containing details such as the validity of the shot, whether it was repeated,
	 *         the ship (if any) affected, and whether the ship was sunk
	 */
	ShotResult fireSingleShot(IPosition pos, boolean isRepeated);

	/**
	 * Gets my fleet.
	 *
	 * @return my fleet
	 */
	IFleet getMyFleet();

	/**
	 * Gets alien moves
	 *
	 * @return the alien moves
	 */
	List<IMove> getAlienMoves();

	/**
	 * Gets the alien fleet
	 *
	 * @return the alien fleet
	 */
	IFleet getAlienFleet();

	/**
	 * Gets my moves
	 *
	 * @return my moves
	 */
	List<IMove> getMyMoves();

	/**
	 * Gets repeated shots.
	 *
	 * @return the repeated shots
	 */
	int getRepeatedShots();

	/**
	 * Gets invalid shots.
	 *
	 * @return the invalid shots
	 */
	int getInvalidShots();

	/**
	 * Gets hits.
	 *
	 * @return the hits
	 */
	int getHits();

	/**
	 * Gets sunk ships.
	 *
	 * @return the sunk ships
	 */
	int getSunkShips();

	/**
	 * Gets remaining ships.
	 *
	 * @return the remaining ships
	 */
	int getRemainingShips();

	/**
	 * Print my board (my fleet + alien shots).
	 */
	void printMyBoard(boolean show_shots, boolean show_legend);

	/**
	 * Print the alien board (alien fleet + my shots).
	 */
	void printAlienBoard(boolean show_shots, boolean show_legend);

	void over();

	/**
	 * Devolve o tempo de jogo que ainda resta.
	 * O relógio começa a contar quando o jogo é criado.
	 *
	 * @return o tempo restante, nunca negativo ({@link Duration#ZERO} quando o tempo acabou)
	 */
	Duration getRemainingTime();

	/**
	 * Devolve um texto pronto a mostrar com o tempo restante, no formato
	 * {@code "Tempo restante: mm:ss"}, por exemplo {@code "Tempo restante: 09:41"}.
	 *
	 * @return o texto com o estado do relógio
	 */
	String clockStatus();

	/**
	 * Pára o relógio de jogo sem o dar como esgotado.
	 * Deve ser chamado quando a partida termina por outro motivo, quando é criada
	 * uma nova partida ou quando se corre a simulação, para não ficar nenhuma
	 * thread agendada nem aparecer um falso "tempo esgotado".
	 */
	void stopClock();

	/**
	 * Indica se o jogo terminou porque o tempo esgotou. Quando é true, já não é
	 * possível fazer jogadas e o jogador perdeu a partida.
	 *
	 * @return true se o tempo chegou a zero
	 */
	boolean isFinished();
}