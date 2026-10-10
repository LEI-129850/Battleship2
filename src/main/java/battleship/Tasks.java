package battleship;

import java.time.Duration;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * The type Tasks.
 */
public class Tasks {
	/**
	 * The constant LOGGER.
	 */
	private static final Logger LOGGER = LogManager.getLogger();

	/**
	 * The constant GOODBYE_MESSAGE.
	 */
	private static final String GOODBYE_MESSAGE = "Bons ventos!";

	/**
	 * Strings to be used by the user
	 */
	private static final String AJUDA = "ajuda";
	private static final String GERAFROTA = "gerafrota";
	private static final String LEFROTA = "lefrota";
	private static final String DESISTIR = "desisto";
	private static final String RAJADA = "rajada";
	private static final String TIROS = "tiros";
	private static final String MAPA = "mapa";
	private static final String STATUS = "estado";
	private static final String SIMULA = "simula";
	private static final String TEMPO = "tempo";
	private static final String RELOGIO = "relogio";

	/**
	 * Tempo máximo (em minutos) que o jogador pode escolher.
	 */
	private static final int MAX_TIME_BANK_MINUTES = 180;

	private static final String TIME_UP_MESSAGE = "Tempo esgotado: já não é possível fazer jogadas.";

	/**
	 * This task also tests the fighting element of a round of three shots
	 */
	public static void menu() {

		IFleet myFleet = null;
		IGame game = null;
		Duration timeBank = Game.DEFAULT_TIME_BANK;
		menuHelp();

		System.out.print("> ");
		Scanner in = new Scanner(System.in);
		String command = in.next();
		while (!command.equals(DESISTIR)) {

			switch (command) {
				case TEMPO:
					timeBank = readTimeBank(in, timeBank);
					break;
				case GERAFROTA:
					if (game != null)
						game.stopClock();   // evita relógios antigos ativos
					myFleet = Fleet.createRandom();
					game = new Game(myFleet, timeBank);
					game.printMyBoard(false, true);
					System.out.println("Partida iniciada! Tempo de jogo: " + GameClock.format(timeBank));
					break;
				case LEFROTA:
					if (game != null)
						game.stopClock();
					myFleet = buildFleet(in);
					game = new Game(myFleet, timeBank);
					game.printMyBoard(false, true);
					System.out.println("Partida iniciada! Tempo de jogo: " + GameClock.format(timeBank));
					break;
				case STATUS:
					if (myFleet != null)
						myFleet.printStatus();
					break;
				case MAPA:
					if (myFleet != null)
						game.printMyBoard(false, true);
					break;
				case RELOGIO:
					if (game != null)
						showLiveClock(game, in);
					else
						System.out.println("Ainda não há partida. Usa gerafrota ou lefrota.");
					break;
				case RAJADA:
					if (game != null) {
						if (game.isFinished()) {
							in.nextLine();   // descarta os tiros escritos nesta linha
							System.out.println(TIME_UP_MESSAGE);
							break;
						}
						try {
							game.readEnemyFire(in);
						} catch (IllegalStateException e) {   // o tempo acabou enquanto escrevia
							System.out.println(e.getMessage());
							break;
						}
						myFleet.printStatus();
						game.printMyBoard(true, false);
						System.out.println(game.clockStatus());

						if (game.getRemainingShips() == 0) {
							game.stopClock();
							game.over();
							System.exit(0);
						}
					}
					break;
				case SIMULA:
					if (game != null) {
						if (game.isFinished()) {
							System.out.println(TIME_UP_MESSAGE);
							break;
						}
						game.stopClock();   // a simulação não usa o limite de tempo
						while (game.getRemainingShips() > 0){
							game.randomEnemyFire();
							myFleet.printStatus();
							game.printMyBoard(true, false);
							try {
								Thread.sleep(3000);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt(); // Best practice: restore interrupt status
							}
						}

						if (game.getRemainingShips() == 0) {
							game.over();
							System.exit(0);
						}
					}
					break;
				case TIROS:
					if (game != null)
						game.printMyBoard(true, true);
					break;
				case AJUDA:
					menuHelp();
					break;
				default:
					System.out.println("Que comando é esse??? Repete ...");
			}
			System.out.print("> ");
			command = in.next();
		}
		if (game != null)
			game.stopClock();
		System.out.println(GOODBYE_MESSAGE);
	}

	/**
	 * This function provides help information about the menu commands.
	 */
	public static void menuHelp() {
		System.out.println("======================= AJUDA DO MENU =========================");
		System.out.println("Digite um dos comandos abaixo para interagir com o jogo:");
		System.out.println("- " + TEMPO + " <min>: Define o tempo de jogo antes da partida (ex: tempo 10, ou tempo 30s).");
		System.out.println("- " + GERAFROTA + ": Gera uma frota aleatória de navios.");
		System.out.println("- " + LEFROTA + ": Permite criar e carregar uma frota personalizada.");
		System.out.println("- " + STATUS + ": Mostra o status atual da frota.)");
		System.out.println("- " + MAPA + ": Exibe o mapa da frota.");
		System.out.println("- " + RELOGIO + ": Mostra o tempo restante a decrescer em direto (Enter para sair).");
		System.out.println("- " + RAJADA + ": Realiza uma rajada de disparos.");
		System.out.println("- " + SIMULA + ": Simula um jogo completo (sem limite de tempo).");
		System.out.println("- " + TIROS + ": Lista os tiros válidos realizados (* = tiro em navio, o = tiro na água)");
		System.out.println("- " + DESISTIR + ": Encerra o jogo.");
		System.out.println("===============================================================");
	}

	/**
	 * Lê o tempo de jogo escolhido pelo utilizador.
	 * "tempo 15" define 15 minutos; "tempo 30s" define 30 segundos (útil para testar).
	 *
	 * @param in      The scanner to read from
	 * @param current O tempo atual, mantido se o valor lido for inválido
	 * @return O novo tempo de jogo
	 */
	private static Duration readTimeBank(Scanner in, Duration current) {
		String arg = in.next().trim().toLowerCase();
		try {
			Duration d = arg.endsWith("s")
					? Duration.ofSeconds(Long.parseLong(arg.substring(0, arg.length() - 1)))
					: Duration.ofMinutes(Long.parseLong(arg));

			if (d.isZero() || d.isNegative() || d.compareTo(Duration.ofMinutes(MAX_TIME_BANK_MINUTES)) > 0) {
				System.out.println("Tempo inválido. Usa entre 1 segundo e " + MAX_TIME_BANK_MINUTES + " minutos.");
				return current;
			}
			System.out.println("Tempo de jogo: " + GameClock.format(d)
					+ " (aplica-se à próxima partida: gerafrota ou lefrota)");
			return d;
		} catch (NumberFormatException e) {
			System.out.println("Formato inválido. Exemplos: 'tempo 15' (minutos) ou 'tempo 30s' (segundos).");
			return current;
		}
	}

	/**
	 * Mostra o tempo restante a decrescer em direto, até o utilizador carregar em Enter.
	 *
	 * @param game The current game
	 * @param in   The scanner to read from
	 */
	private static void showLiveClock(IGame game, Scanner in) {
		in.nextLine();   // consome o resto da linha do comando
		System.out.println("Relógio em direto (prime Enter para sair)");

		ScheduledExecutorService ticker = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "clock-display");
			t.setDaemon(true);
			return t;
		});
		ticker.scheduleAtFixedRate(() -> {
			System.out.print("\r" + game.clockStatus() + "   ");
			if (game.isFinished())
				ticker.shutdown();
		}, 0, 1, TimeUnit.SECONDS);

		in.nextLine();   // espera pelo Enter
		ticker.shutdownNow();
		System.out.println();
	}

	/**
	 * This operation allows the build up of a fleet, given user data
	 *
	 * @param in The scanner to read from
	 * @return The fleet that has been built
	 */
	public static Fleet buildFleet(Scanner in) {

		assert in != null;

		Fleet fleet = new Fleet();
		int i = 0; // i represents the total of successfully created ships
		while (i < Fleet.FLEET_SIZE) {
			IShip s = readShip(in);
			if (s != null) {
				boolean success = fleet.addShip(s);
				if (success)
					i++;
				else
					LOGGER.info("Falha na criacao de {} {} {}", s.getCategory(), s.getBearing(), s.getPosition());
			} else {
				LOGGER.info("Navio desconhecido!");
			}
		}
		LOGGER.info("{} navios adicionados com sucesso!", i);
		return fleet;
	}

	/**
	 * This operation reads data about a ship, build it and returns it
	 *
	 * @param in The scanner to read from
	 * @return The created ship based on the data that has been read
	 */
	public static Ship readShip(Scanner in) {

		assert in != null;

		String shipKind = in.next();
		Position pos = readPosition(in);
		char c = in.next().charAt(0);
		Compass bearing = Compass.charToCompass(c);
		return Ship.buildShip(shipKind, bearing, pos);
	}

	/**
	 * This operation allows reading a position in the map
	 *
	 * @param in The scanner to read from
	 * @return The position that has been read
	 */
	public static Position readPosition(Scanner in) {

		assert in != null;

		int row = in.nextInt();
		int column = in.nextInt();
		return new Position(row, column);
	}

	/**
	 * This operation allows reading a position in the map
	 *
	 * @param in The scanner to read from
	 * @return The classic position that has been read
	 */
	public static IPosition readClassicPosition(@NotNull Scanner in) {
		// Verifica se ainda há tokens disponíveis
		if (!in.hasNext()) {
			throw new IllegalArgumentException("Nenhuma posição válida encontrada!");
		}

		String part1 = in.next(); // Primeiro token
		String part2 = null;

		if (in.hasNextInt()) {
			part2 = in.next(); // Segundo token, se disponível
		}

		String input = (part2 != null) ? part1 + part2 : part1;

		// Normalizar o input para tratar letras maiúsculas e minúsculas
		input = input.toUpperCase();

		// Verificar os dois formatos possíveis: compactos e com espaço
		if (input.matches("[A-Z]\\d+")) {
			char column = input.charAt(0); // Extrair a coluna
			int row = Integer.parseInt(input.substring(1)); // Extrair a linha
			return new Position(column, row);
		} else if (part2 != null && part1.matches("[A-Z]") && part2.matches("\\d+")) {
			char column = part1.charAt(0); // Extrair a coluna
			int row = Integer.parseInt(part2); // Extrair a linha
			return new Position(column, row);
		} else {
			throw new IllegalArgumentException("Formato inválido. Use 'A3', 'A 3' ou similar.");
		}
	}

}