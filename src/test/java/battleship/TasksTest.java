package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários da classe {@link Tasks}.
 * <p>
 * O método {@link Tasks#menu()} lê comandos de {@code System.in} e escreve em
 * {@code System.out}. Por isso, cada teste substitui os dois streams por versões
 * controladas e depois repõe os originais.
 * <p>
 * Não são cobertos (não é possível testá-los de forma automática):
 * <ul>
 *   <li>os ramos que chamam {@code System.exit(0)} (frota afundada em {@code rajada}
 *       e fim do comando {@code simula}), porque terminariam a JVM dos testes;</li>
 *   <li>o comando {@code aisimula}, que contacta a API do Gemini pela rede;</li>
 *   <li>o ramo {@code else if} de {@link Tasks#readClassicPosition(Scanner)}, que é
 *       inalcançável: se o primeiro padrão falha, o segundo também falha.</li>
 * </ul>
 *
 * @author Gonçalo Sobral
 * Date: 18/17/2026
 */
@DisplayName("Tasks")
class TasksTest {

	/**
	 * Frota válida (11 navios, sem se tocarem), no formato lido por {@code lefrota}:
	 * tipo, linha, coluna (a partir de 0) e orientação (n, s, e, o).
	 * Células ocupadas: galeão A1-A3, B2, C2; fragata A6-A9; naus C6-E6 e C9-E9;
	 * caravelas G1-H1, G4-G5 e G8-H8; barcas J1, J4, J7 e J10.
	 */
	private static final String[] FROTA_VALIDA = {
			"galeao 0 0 n",
			"fragata 0 5 e",
			"nau 2 5 s",
			"nau 2 8 s",
			"caravela 6 0 s",
			"caravela 6 3 e",
			"caravela 6 7 s",
			"barca 9 0 n",
			"barca 9 3 n",
			"barca 9 6 n",
			"barca 9 9 n"
	};

	private PrintStream saidaOriginal;
	private InputStream entradaOriginal;
	private ByteArrayOutputStream saidaCapturada;

	// ------------------------------------------------------------------
	// Preparação
	// ------------------------------------------------------------------

	/**
	 * Evita que o comando {@code gerafrota} abra a janela do relógio durante os testes.
	 * Se o ambiente gráfico já tiver sido inicializado por outro teste, esta propriedade
	 * não tem efeito (nesse caso, a janela abre e fecha-se sozinha no fim de cada teste).
	 */
	@BeforeAll
	static void semJanelas() {
		System.setProperty("java.awt.headless", "true");
	}

	@BeforeEach
	void setUp() {
		saidaOriginal = System.out;
		entradaOriginal = System.in;
		saidaCapturada = new ByteArrayOutputStream();
		System.setOut(new PrintStream(saidaCapturada, true, StandardCharsets.UTF_8));
	}

	@AfterEach
	void tearDown() {
		System.setOut(saidaOriginal);
		System.setIn(entradaOriginal);
	}

	// ------------------------------------------------------------------
	// Auxiliares
	// ------------------------------------------------------------------

	private String saida() {
		return saidaCapturada.toString(StandardCharsets.UTF_8);
	}

	/** Corre o menu com os comandos indicados (um por linha) e devolve o que foi escrito. */
	private String correrMenu(String... linhas) {
		saidaCapturada.reset();
		String entrada = String.join("\n", linhas) + "\n";
		System.setIn(new ByteArrayInputStream(entrada.getBytes(StandardCharsets.UTF_8)));
		Tasks.menu();
		return saida();
	}

	/**
	 * Corre o menu com a entrada entregue por fases: entre uma fase e a seguinte há uma
	 * pausa. Serve para deixar passar o tempo (por exemplo, até o relógio esgotar)
	 * enquanto o menu está à espera do próximo comando.
	 */
	private String correrMenuEmFases(long pausaMs, String... fases) {
		saidaCapturada.reset();
		System.setIn(new EntradaFaseada(pausaMs, fases));
		Tasks.menu();
		return saida();
	}

	/** Sessão com a frota válida carregada por {@code lefrota}, seguida dos comandos dados. */
	private static String[] sessaoComFrota(String... comandos) {
		List<String> linhas = new ArrayList<>();
		linhas.add("lefrota");
		linhas.addAll(Arrays.asList(FROTA_VALIDA));
		linhas.addAll(Arrays.asList(comandos));
		linhas.add("desisto");
		return linhas.toArray(new String[0]);
	}

	private static int contar(String texto, String padrao) {
		int total = 0;
		int indice = 0;
		while ((indice = texto.indexOf(padrao, indice)) != -1) {
			total++;
			indice += padrao.length();
		}
		return total;
	}

	private static void assertContem(String texto, String fragmento) {
		assertTrue(texto.contains(fragmento),
				"Esperava encontrar \"" + fragmento + "\" na saída:\n" + texto);
	}

	private static void assertCorresponde(String texto, String regex) {
		assertTrue(Pattern.compile(regex).matcher(texto).find(),
				"Esperava corresponder a /" + regex + "/ na saída:\n" + texto);
	}

	private static void dormir(long milissegundos) {
		try {
			Thread.sleep(milissegundos);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/** Entrada que entrega um bloco de texto de cada vez, com uma pausa entre blocos. */
	private static final class EntradaFaseada extends InputStream {
		private final long pausaMs;
		private final List<byte[]> fases = new ArrayList<>();
		private int proxima = 0;
		private byte[] atual = new byte[0];
		private int posicao = 0;

		EntradaFaseada(long pausaMs, String... fases) {
			this.pausaMs = pausaMs;
			for (String fase : fases) {
				this.fases.add(fase.getBytes(StandardCharsets.UTF_8));
			}
		}

		@Override
		public int read() {
			byte[] um = new byte[1];
			int lidos = read(um, 0, 1);
			return lidos == -1 ? -1 : (um[0] & 0xFF);
		}

		@Override
		public int read(byte[] destino, int inicio, int tamanho) {
			if (tamanho == 0) {
				return 0;
			}
			if (posicao >= atual.length) {
				if (proxima >= fases.size()) {
					return -1;
				}
				if (proxima > 0) {
					dormir(pausaMs);
				}
				atual = fases.get(proxima++);
				posicao = 0;
			}
			int lidos = Math.min(tamanho, atual.length - posicao);
			System.arraycopy(atual, posicao, destino, inicio, lidos);
			posicao += lidos;
			return lidos;
		}
	}

	// ------------------------------------------------------------------
	// menuHelp
	// ------------------------------------------------------------------

	@Test
	@DisplayName("menuHelp lista todos os comandos do jogo")
	void menuHelp() {
		Tasks.menuHelp();
		String texto = saida();

		assertContem(texto, "AJUDA DO MENU");
		for (String comando : new String[]{"tempo", "gerafrota", "lefrota", "estado", "mapa",
				"relogio", "rajada", "simula", "aisimula", "radar", "tiros", "historico", "desisto"}) {
			assertContem(texto, "- " + comando);
		}
	}

	// ------------------------------------------------------------------
	// readClassicPosition
	// ------------------------------------------------------------------

	@Test
	@DisplayName("readClassicPosition lê o formato compacto (A3)")
	void readClassicPositionCompacto() {
		IPosition pos = Tasks.readClassicPosition(new Scanner("A3"));

		assertEquals(0, pos.getRow());
		assertEquals(2, pos.getColumn());
	}

	@Test
	@DisplayName("readClassicPosition lê letra e número separados, em minúsculas (c 5)")
	void readClassicPositionComEspacoEMinusculas() {
		IPosition pos = Tasks.readClassicPosition(new Scanner("c 5"));

		assertEquals(2, pos.getRow());
		assertEquals(4, pos.getColumn());
	}

	@Test
	@DisplayName("readClassicPosition aceita a última casa do tabuleiro (J10)")
	void readClassicPositionUltimaCasa() {
		IPosition pos = Tasks.readClassicPosition(new Scanner("J10"));

		assertEquals(9, pos.getRow());
		assertEquals(9, pos.getColumn());
		assertTrue(pos.isInside());
	}

	@Test
	@DisplayName("readClassicPosition devolve uma posição fora do tabuleiro (K11)")
	void readClassicPositionForaDoTabuleiro() {
		IPosition pos = Tasks.readClassicPosition(new Scanner("K11"));

		assertFalse(pos.isInside());
	}

	@Test
	@DisplayName("readClassicPosition falha quando não há nada para ler")
	void readClassicPositionSemTokens() {
		IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
				() -> Tasks.readClassicPosition(new Scanner("")));

		assertContem(erro.getMessage(), "Nenhuma posi");
	}

	@Test
	@DisplayName("readClassicPosition rejeita formatos inválidos")
	void readClassicPositionFormatoInvalido() {
		for (String entrada : new String[]{"3A", "A", "AB 3", "A-3", "33"}) {
			IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
					() -> Tasks.readClassicPosition(new Scanner(entrada)),
					"Devia rejeitar: " + entrada);

			assertContem(erro.getMessage(), "Use 'A3'");
		}
	}

	// ------------------------------------------------------------------
	// readPosition e readShip
	// ------------------------------------------------------------------

	@Test
	@DisplayName("readPosition lê a linha e a coluna (a partir de 0)")
	void readPosition() {
		Position pos = Tasks.readPosition(new Scanner("3 4"));

		assertEquals(3, pos.getRow());
		assertEquals(4, pos.getColumn());
	}

	@Test
	@DisplayName("readPosition falha com entrada não numérica")
	void readPositionNaoNumerica() {
		assertThrows(InputMismatchException.class,
				() -> Tasks.readPosition(new Scanner("a b")));
	}

	@Test
	@DisplayName("readShip constrói um galeão virado a norte")
	void readShip() {
		Ship navio = Tasks.readShip(new Scanner("galeao 0 0 n"));

		assertNotNull(navio);
		assertEquals("Galeao", navio.getCategory());
		assertEquals(Compass.NORTH, navio.getBearing());
		assertEquals(5, navio.getSize().intValue());
	}

	@Test
	@DisplayName("readShip constrói uma barca virada a oeste na posição lida")
	void readShipBarcaOeste() {
		Ship navio = Tasks.readShip(new Scanner("barca 3 4 o"));

		assertNotNull(navio);
		assertEquals("Barca", navio.getCategory());
		assertEquals(Compass.WEST, navio.getBearing());
		assertEquals(3, navio.getPosition().getRow());
		assertEquals(4, navio.getPosition().getColumn());
	}

	@Test
	@DisplayName("readShip devolve null para um tipo de navio desconhecido")
	void readShipTipoDesconhecido() {
		assertNull(Tasks.readShip(new Scanner("xpto 0 0 n")));
	}

	// ------------------------------------------------------------------
	// buildFleet
	// ------------------------------------------------------------------

	@Test
	@DisplayName("buildFleet constrói a frota completa de 11 navios")
	void buildFleet() {
		Fleet frota = Tasks.buildFleet(new Scanner(String.join("\n", FROTA_VALIDA)));

		assertEquals(11, frota.getShips().size());
		assertEquals(11, frota.getFloatingShips().size());
		assertEquals(1, frota.getShipsLike("Galeao").size());
		assertEquals(1, frota.getShipsLike("Fragata").size());
		assertEquals(2, frota.getShipsLike("Nau").size());
		assertEquals(3, frota.getShipsLike("Caravela").size());
		assertEquals(4, frota.getShipsLike("Barca").size());
	}

	@Test
	@DisplayName("buildFleet ignora navios desconhecidos, em colisão ou fora do tabuleiro")
	void buildFleetIgnoraNaviosInvalidos() {
		List<String> linhas = new ArrayList<>();
		linhas.add("xpto 0 0 n");        // tipo desconhecido
		linhas.add(FROTA_VALIDA[0]);     // galeão válido
		linhas.add("barca 1 1 n");       // colide com o galeão
		linhas.add("fragata 0 8 e");     // sai do tabuleiro pela direita
		linhas.add("barca 10 10 n");     // fora do tabuleiro
		linhas.addAll(Arrays.asList(FROTA_VALIDA).subList(1, FROTA_VALIDA.length));

		Fleet frota = Tasks.buildFleet(new Scanner(String.join("\n", linhas)));

		assertEquals(11, frota.getShips().size());
	}

	// ------------------------------------------------------------------
	// menu: comandos básicos
	// ------------------------------------------------------------------

	@Test
	@DisplayName("menu mostra a ajuda e despede-se com 'desisto'")
	void menuDesistirImediatamente() {
		String texto = correrMenu("desisto");

		assertContem(texto, "AJUDA DO MENU");
		assertContem(texto, "Bons ventos!");
	}

	@Test
	@DisplayName("menu volta a mostrar a ajuda com o comando 'ajuda'")
	void menuAjuda() {
		String texto = correrMenu("ajuda", "desisto");

		assertEquals(2, contar(texto, "AJUDA DO MENU"));
	}

	@Test
	@DisplayName("menu avisa quando o comando é desconhecido")
	void menuComandoDesconhecido() {
		String texto = correrMenu("xpto", "desisto");

		assertContem(texto, "Que comando");
	}

	@Test
	@DisplayName("menu: estado, mapa, tiros e simula sem partida não fazem nada")
	void menuComandosSemPartida() {
		String texto = correrMenu("estado", "mapa", "tiros", "simula", "desisto");

		assertContem(texto, "Bons ventos!");
		assertFalse(texto.contains("Estado da Frota"));
		assertFalse(texto.contains("LEGENDA"));
	}

	@Test
	@DisplayName("menu: relogio sem partida pede para criar uma frota")
	void menuRelogioSemPartida() {
		String texto = correrMenu("relogio", "desisto");

		assertContem(texto, "Usa gerafrota ou lefrota");
	}

	@Test
	@DisplayName("menu: historico mostra o histórico de partidas")
	void menuHistorico() {
		String texto = correrMenu("historico", "desisto");

		assertContem(texto, "de Partidas:");
	}

	// ------------------------------------------------------------------
	// menu: gerafrota e lefrota
	// ------------------------------------------------------------------

	@Test
	@DisplayName("menu: gerafrota inicia a partida com o tempo por omissão (15:00)")
	void menuGerafrota() {
		String texto = correrMenu("gerafrota", "desisto");

		assertContem(texto, "Partida iniciada!");
		assertContem(texto, "Tempo de jogo: 15:00");
		assertContem(texto, "LEGENDA");
	}

	@Test
	@DisplayName("menu: gerafrota duas vezes pára o relógio da partida anterior")
	void menuGerafrotaDuasVezes() {
		String texto = correrMenu("gerafrota", "gerafrota", "desisto");

		assertEquals(2, contar(texto, "Partida iniciada!"));
	}

	@Test
	@DisplayName("menu: lefrota depois de gerafrota substitui a partida")
	void menuLefrotaDepoisDeGerafrota() {
		String[] comandos = new String[2 + FROTA_VALIDA.length + 1];
		comandos[0] = "gerafrota";
		comandos[1] = "lefrota";
		System.arraycopy(FROTA_VALIDA, 0, comandos, 2, FROTA_VALIDA.length);
		comandos[comandos.length - 1] = "desisto";

		String texto = correrMenu(comandos);

		assertEquals(2, contar(texto, "Partida iniciada!"));
	}

	@Test
	@DisplayName("menu: com a frota lida, estado, mapa e tiros mostram o tabuleiro")
	void menuEstadoMapaETiros() {
		String texto = correrMenu(sessaoComFrota("estado", "mapa", "tiros"));

		assertContem(texto, "Estado da Frota: 11 a flutuar, 0 afundados!");
		// lefrota, mapa e tiros imprimem o tabuleiro com legenda
		assertEquals(3, contar(texto, "LEGENDA"));
	}

	// ------------------------------------------------------------------
	// menu: tempo
	// ------------------------------------------------------------------

	@Test
	@DisplayName("menu: tempo em minutos aplica-se à partida seguinte")
	void menuTempoEmMinutos() {
		String texto = correrMenu("tempo 10", "gerafrota", "desisto");

		assertContem(texto, "Tempo de jogo: 10:00 (aplica-se");
		assertContem(texto, "Partida iniciada! Tempo de jogo: 10:00");
	}

	@Test
	@DisplayName("menu: tempo em segundos, com 's' minúsculo ou maiúsculo")
	void menuTempoEmSegundos() {
		assertContem(correrMenu("tempo 30s", "desisto"), "Tempo de jogo: 00:30");
		assertContem(correrMenu("tempo 45S", "desisto"), "Tempo de jogo: 00:45");
	}

	@Test
	@DisplayName("menu: tempo aceita o limite máximo de 180 minutos")
	void menuTempoLimiteMaximo() {
		String texto = correrMenu("tempo 180", "desisto");

		assertContem(texto, "Tempo de jogo: 180:00");
	}

	@Test
	@DisplayName("menu: tempo fora do intervalo é recusado e mantém o tempo anterior")
	void menuTempoForaDoIntervalo() {
		for (String valor : new String[]{"0", "-5", "181", "0s", "-3s"}) {
			String texto = correrMenu("tempo " + valor, "gerafrota", "desisto");

			assertContem(texto, "Usa entre 1 segundo e 180 minutos.");
			assertContem(texto, "Partida iniciada! Tempo de jogo: 15:00");
		}
	}

	@Test
	@DisplayName("menu: tempo com formato inválido é recusado")
	void menuTempoFormatoInvalido() {
		for (String valor : new String[]{"abc", "1.5", "s"}) {
			String texto = correrMenu("tempo " + valor, "desisto");

			assertContem(texto, "Exemplos: 'tempo 15'");
		}
	}

	// ------------------------------------------------------------------
	// menu: radar
	// ------------------------------------------------------------------

	@Test
	@DisplayName("menu: radar sem partida pede para criar uma frota")
	void menuRadarSemPartida() {
		String texto = correrMenu("radar A1", "desisto");

		assertContem(texto, "Primeiro cria uma frota");
	}

	@Test
	@DisplayName("menu: radar sem indicar a posição mostra um exemplo")
	void menuRadarSemPosicao() {
		String texto = correrMenu("gerafrota", "radar", "desisto");

		assertContem(texto, "por exemplo: radar C5");
	}

	@Test
	@DisplayName("menu: radar fora do tabuleiro é recusado")
	void menuRadarForaDoTabuleiro() {
		String texto = correrMenu(sessaoComFrota("radar K11"));

		assertContem(texto, "fora do tabuleiro");
	}

	@Test
	@DisplayName("menu: radar com formato inválido mostra a mensagem de erro")
	void menuRadarFormatoInvalido() {
		String texto = correrMenu(sessaoComFrota("radar 33"));

		assertContem(texto, "Use 'A3', 'A 3' ou similar.");
	}

	@Test
	@DisplayName("menu: radar numa zona sem navios diz que a zona está limpa")
	void menuRadarZonaLimpa() {
		String texto = correrMenu(sessaoComFrota("radar E2"));

		assertContem(texto, "Zona limpa em E2");
	}

	@Test
	@DisplayName("menu: radar junto de um navio indica quantos foram detetados")
	void menuRadarDetetaNavio() {
		// J2 está ao lado da barca em J1 (e não há mais nada na área 3x3)
		String texto = correrMenu(sessaoComFrota("radar J2"));

		assertContem(texto, "Radar utilizado: 1 navio(s)");
	}

	@Test
	@DisplayName("menu: o radar só pode ser usado uma vez por jogo")
	void menuRadarSegundoUso() {
		String texto = correrMenu(sessaoComFrota("radar J2", "radar A1"));

		assertContem(texto, "Radar utilizado: 1 navio(s)");
		assertContem(texto, "usaste o radar neste jogo.");
	}

	// ------------------------------------------------------------------
	// menu: rajada
	// ------------------------------------------------------------------

	@Test
	@DisplayName("menu: rajada dispara três tiros e mostra o resultado e o tempo restante")
	void firingRound() {
		// A1 e B2 acertam no galeão, C3 cai na água
		String texto = correrMenu(sessaoComFrota("rajada A1 B2 C3"));

		assertContem(texto, "Jogada n");
		assertContem(texto, "Galeao");
		assertContem(texto, "Estado da Frota: 11 a flutuar, 0 afundados!");
		assertContem(texto, "Tempo restante:");
		assertCorresponde(texto, "\"validShots\"\\s*:\\s*3");
		assertCorresponde(texto, "\"missedShots\"\\s*:\\s*1");
	}

	@Test
	@DisplayName("menu: rajada aceita letra e número separados por espaços")
	void menuRajadaComEspacos() {
		String texto = correrMenu(sessaoComFrota("rajada A 1 B 2 C 3"));

		assertCorresponde(texto, "\"validShots\"\\s*:\\s*3");
	}

	@Test
	@DisplayName("menu: rajada com tiros repetidos e fora do tabuleiro")
	void menuRajadaRepetidoEForaDoTabuleiro() {
		String texto = correrMenu(sessaoComFrota("rajada A1 A1 K11"));

		assertContem(texto, "repetido");
		assertContem(texto, "exterior");
		assertCorresponde(texto, "\"repeatedShots\"\\s*:\\s*1");
		assertCorresponde(texto, "\"outsideShots\"\\s*:\\s*1");
	}

	@Test
	@DisplayName("menu: rajada com menos de três posições lança IllegalArgumentException")
	void menuRajadaIncompleta() {
		assertThrows(IllegalArgumentException.class,
				() -> correrMenu("gerafrota", "rajada A1 B2", "desisto"));
	}

	// ------------------------------------------------------------------
	// menu: tempo esgotado (usa pausas reais, por isso são os testes mais lentos)
	// ------------------------------------------------------------------

	@Test
	@Timeout(15)
	@DisplayName("menu: com o tempo esgotado, rajada, simula e radar são recusados")
	void menuTempoEsgotadoBloqueiaComandos() {
		// O relógio de 1 s acaba durante a pausa de 1,8 s entre as duas fases
		String texto = correrMenuEmFases(1800,
				"tempo 1s\ngerafrota\n",
				"rajada A1 B2 C3\nsimula\nradar A1\ndesisto\n");

		assertContem(texto, "TEMPO ESGOTADO");
		assertEquals(3, contar(texto, "Tempo esgotado:"));
		assertContem(texto, "Bons ventos!");
	}

	@Test
	@Timeout(15)
	@DisplayName("menu: relogio mostra o tempo restante a decrescer")
	void menuRelogioEmDireto() {
		String texto = correrMenuEmFases(1300,
				"gerafrota\nrelogio\n",
				"\ndesisto\n");

		assertContem(texto, "em direto");
		assertContem(texto, "Tempo restante:");
	}

	@Test
	@Timeout(15)
	@DisplayName("menu: relogio pára sozinho quando o tempo esgota")
	void menuRelogioAteAoFimDoTempo() {
		String texto = correrMenuEmFases(2500,
				"tempo 1s\ngerafrota\nrelogio\n",
				"\ndesisto\n");

		assertContem(texto, "em direto");
		assertContem(texto, "TEMPO ESGOTADO");
	}
}