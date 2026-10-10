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

	public GameSession(String gameId, String playerName, String callbackUrl) {
		this(gameId, playerName, callbackUrl, Game.DEFAULT_TIME_BANK);
	}

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
	public Duration getTimeBank()   { return timeBank; }

	/** True se a frota afundou ou se o tempo acabou. */
	public synchronized boolean isGameOver() {
		checkTimeout();
		return gameOver;
	}

	public synchronized String getWinner() {
		checkTimeout();
		return winner;
	}

	/** Tempo restante em segundos (0 quando acabou). Útil para as respostas REST. */
	public long getRemainingSeconds() {
		return (game.getRemainingTime().toMillis() + 999) / 1000;
	}

	/** True se o jogo terminou especificamente por tempo esgotado. */
	public boolean isTimeUp() {
		return game.isFinished();
	}

	// ── State transitions ────────────────────────────────────────────────────

	public synchronized void markAiWins() {
		if (gameOver) return;
		this.gameOver = true;
		this.winner   = "AI_WINS";
		game.stopClock();
	}

	public synchronized void markStudentWins() {
		if (gameOver) return;
		this.gameOver = true;
		this.winner   = "STUDENT_WINS";
		game.stopClock();
	}

	// ── Internals ────────────────────────────────────────────────────────────

	/** Se o relógio chegou a zero, o estudante perde automaticamente. */
	private void checkTimeout() {
		if (!gameOver && game.isFinished()) {
			this.gameOver = true;
			this.winner   = "AI_WINS";
		}
	}
}