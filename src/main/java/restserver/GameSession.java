package restserver;

import battleship.Fleet;
import battleship.Game;
import battleship.IGame;

import java.time.Duration;

/**
 * Holds all state for one active game between the AI opponent and a student player.
 *
 * One GameSession is created per registration (m0) and stored in the GameRegistry
 * keyed by its gameId. It wraps the existing Game class without modifying it.
 *
 * O jogo tem um limite de tempo global: se o tempo acabar, o estudante perde.
 */
public class GameSession {

	/** Unique identifier for this game, returned to the student on registration. */
	private final String gameId;

	/** The student's name, for logging / display purposes. */
	private final String playerName;

	/**
	 * The URL of the student's server where the AI will POST its shots (m2a).
	 * Example: "http://student-host:9090"
	 * The AI will call POST {callbackUrl}/game/{gameId}/shots
	 */
	private final String callbackUrl;

	/**
	 * The core game object from the existing codebase.
	 * myFleet = AI's fleet  (receives student's shots)
	 * alienFleet = tracked knowledge of student's fleet
	 */
	private final IGame game;

	/** Number of shots fired per turn (matches Game.NUMBER_SHOTS = 3). */
	private final int shotsPerTurn;

	/** Tempo total atribuído à partida. */
	private final Duration timeBank;

	/** True once one fleet is completely sunk or the time ran out. */
	private boolean gameOver;

	/** "AI_WINS" or "STUDENT_WINS" — set when gameOver becomes true. */
	private String winner;

	// -------------------------------------------------------------------------

	/**
	 * Cria uma sessão com o tempo de jogo por omissão ({@link Game#DEFAULT_TIME_BANK}).
	 *
	 * @param gameId      o identificador único da partida
	 * @param playerName  o nome do estudante
	 * @param callbackUrl o URL do servidor do estudante
	 */
	public GameSession(String gameId, String playerName, String callbackUrl) {
		this(gameId, playerName, callbackUrl, Game.DEFAULT_TIME_BANK);
	}

	/**
	 * Cria uma sessão com um tempo de jogo à escolha. O relógio da partida
	 * começa a contar assim que a sessão é criada.
	 *
	 * @param gameId      o identificador único da partida
	 * @param playerName  o nome do estudante
	 * @param callbackUrl o URL do servidor do estudante
	 * @param timeBank    o tempo total da partida; tem de ser positivo
	 */
	public GameSession(String gameId, String playerName, String callbackUrl, Duration timeBank) {
		this.gameId       = gameId;
		this.playerName   = playerName;
		this.callbackUrl  = callbackUrl;
		this.timeBank     = timeBank;
		this.game         = new Game(Fleet.createRandom(), timeBank); // o relógio começa aqui
		this.shotsPerTurn = Game.NUMBER_SHOTS;
		this.gameOver     = false;
		this.winner       = null;
	}

	// ── Getters ──────────────────────────────────────────────────────────────

	public String getGameId()       { return gameId; }
	public String getPlayerName()   { return playerName; }
	public String getCallbackUrl()  { return callbackUrl; }
	public IGame  getGame()         { return game; }
	public int    getShotsPerTurn() { return shotsPerTurn; }

	/**
	 * Devolve o tempo total atribuído à partida.
	 *
	 * @return o banco de tempo da partida
	 */
	public Duration getTimeBank()   { return timeBank; }

	/**
	 * Indica se a partida terminou, porque uma frota afundou ou porque o tempo
	 * esgotou. Antes de responder, verifica se o relógio chegou a zero.
	 *
	 * @return true se a partida terminou
	 */
	public synchronized boolean isGameOver() {
		checkTimeout();
		return gameOver;
	}

	/**
	 * Devolve o vencedor, {@code "AI_WINS"} ou {@code "STUDENT_WINS"}.
	 * Se o tempo esgotou, o vencedor é sempre a IA.
	 *
	 * @return o vencedor, ou null se a partida ainda decorre
	 */
	public synchronized String getWinner() {
		checkTimeout();
		return winner;
	}

	/**
	 * Devolve o tempo restante em segundos, arredondado para cima
	 * (0 quando o tempo acabou). Útil para as respostas REST.
	 *
	 * @return os segundos que faltam
	 */
	public long getRemainingSeconds() {
		return (game.getRemainingTime().toMillis() + 999) / 1000;
	}

	/**
	 * Indica se a partida terminou especificamente por tempo esgotado.
	 *
	 * @return true se o tempo chegou a zero
	 */
	public boolean isTimeUp() {
		return game.isFinished();
	}

	// ── State transitions ────────────────────────────────────────────────────

	/**
	 * Marca a IA como vencedora e pára o relógio. Não faz nada se a partida
	 * já tinha terminado, para não substituir o resultado.
	 */
	public synchronized void markAiWins() {
		if (gameOver) return;
		this.gameOver = true;
		this.winner   = "AI_WINS";
		game.stopClock();
	}

	/**
	 * Marca o estudante como vencedor e pára o relógio. Não faz nada se a
	 * partida já tinha terminado, para não substituir o resultado.
	 */
	public synchronized void markStudentWins() {
		if (gameOver) return;
		this.gameOver = true;
		this.winner   = "STUDENT_WINS";
		game.stopClock();
	}

	// ── Internals ────────────────────────────────────────────────────────────

	/**
	 * Se o relógio chegou a zero, o estudante perde automaticamente:
	 * a partida passa a terminada com vitória da IA.
	 */
	private void checkTimeout() {
		if (!gameOver && game.isFinished()) {
			this.gameOver = true;
			this.winner   = "AI_WINS";
		}
	}
}