package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de caixa-branca para a classe {@link GameClock}.
 * Valida a gestão de tempo, estados, ciclo de vida do agendador, formatação e concorrência.
 */
class GameClockTest {

    private List<GameClock> activeClocks;

    @BeforeEach
    void setUp() {
        activeClocks = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        if (activeClocks != null) {
            for (GameClock clock : activeClocks) {
                if (clock != null) {
                    try {
                        clock.stop();
                    } catch (Exception ignored) {
                        // Limpeza silenciosa para garantir que threads daemon ou agendamentos são terminados
                    }
                }
            }
            activeClocks.clear();
        }
    }

    private GameClock createClock(Duration bank, Runnable onTimeout) {
        GameClock clock = new GameClock(bank, onTimeout);
        activeClocks.add(clock);
        return clock;
    }

    // --- Testes do Construtor ---

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o banco de tempo for nulo")
    void deveLancarExcecaoQuandoBancoForNulo() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new GameClock(null, () -> {}),
                "Deveria ter lançado IllegalArgumentException quando o banco é nulo"
        );
        assertEquals("O banco de tempo tem de ser positivo", ex.getMessage(),
                "A mensagem da exceção deve corresponder à especificação");
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o banco de tempo for zero")
    void deveLancarExcecaoQuandoBancoForZero() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new GameClock(Duration.ZERO, () -> {}),
                "Deveria ter lançado IllegalArgumentException quando o banco é zero"
        );
        assertEquals("O banco de tempo tem de ser positivo", ex.getMessage(),
                "A mensagem da exceção deve corresponder à especificação");
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o banco de tempo for negativo")
    void deveLancarExcecaoQuandoBancoForNegativo() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new GameClock(Duration.ofSeconds(-1), () -> {}),
                "Deveria ter lançado IllegalArgumentException quando o banco é negativo"
        );
        assertEquals("O banco de tempo tem de ser positivo", ex.getMessage(),
                "A mensagem da exceção deve corresponder à especificação");
    }

    @Test
    @DisplayName("Deve inicializar o relógio parado, não terminado e com o tempo total no banco")
    void deveInicializarRelogioCorretamente() {
        Duration bank = Duration.ofMinutes(5);
        GameClock clock = createClock(bank, () -> {});

        assertNotNull(clock, "O relógio instanciado não deve ser nulo");
        assertFalse(clock.isOver(), "O relógio recém-criado não deve estar terminado");
        assertEquals(bank, clock.remaining(), "O tempo restante inicial deve ser igual ao banco configurado");
    }

    @Test
    @DisplayName("Deve permitir criar relógio com ação de timeout nula")
    void devePermitirAcaoDeTimeoutNula() {
        Duration bank = Duration.ofMinutes(2);
        GameClock clock = createClock(bank, null);

        assertNotNull(clock, "O relógio com callback nulo deve ser instanciado com sucesso");
        assertFalse(clock.isOver(), "O relógio não deve estar terminado");
        assertEquals(bank, clock.remaining(), "O tempo restante inicial deve corresponder ao banco");
    }

    // --- Testes de start() ---

    @Test
    @DisplayName("Deve iniciar o relógio e decrementar o tempo restante sem terminar de imediato")
    void deveIniciarRelogioEReduzirTempoRestante() {
        Duration bank = Duration.ofSeconds(10);
        GameClock clock = createClock(bank, () -> {});

        clock.start();

        assertFalse(clock.isOver(), "O relógio em contagem não deve estar terminado");
        Duration remaining = clock.remaining();
        assertTrue(remaining.compareTo(Duration.ZERO) > 0, "O tempo restante deve ser positivo");
        assertTrue(remaining.compareTo(bank) <= 0, "O tempo restante deve ser menor ou igual ao banco inicial");
    }

    @Test
    @DisplayName("Deve ignorar chamadas subsequentes ao método start() quando o relógio já está a correr")
    void deveIgnorarChamadasSubsequentesDeStart() {
        Duration bank = Duration.ofSeconds(5);
        GameClock clock = createClock(bank, () -> {});

        clock.start();
        clock.start(); // Segunda chamada redundante

        assertFalse(clock.isOver(), "O relógio deve continuar em execução após start redundante");
        assertTrue(clock.remaining().compareTo(Duration.ZERO) > 0, "O relógio deve continuar a contar");
    }

    @Test
    @DisplayName("Deve ignorar chamada ao start() se o relógio já tiver sido parado")
    void deveIgnorarStartQuandoRelogioJaTiverSidoParado() {
        GameClock clock = createClock(Duration.ofSeconds(5), () -> {});
        clock.stop();
        assertTrue(clock.isOver(), "O relógio deve estar terminado após stop()");

        clock.start(); // Invocação após stop
        assertTrue(clock.isOver(), "O relógio deve permanecer terminado após tentativa de start()");
    }

    @Test
    @DisplayName("Deve ignorar chamada ao start() quando o relógio já terminou por expiração")
    void deveIgnorarStartQuandoRelogioJaTiverExpirado() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        GameClock clock = createClock(Duration.ofMillis(30), latch::countDown);

        clock.start();
        boolean completed = latch.await(2, TimeUnit.SECONDS);

        assertTrue(completed, "O callback de timeout deve ter sido invocado dentro do limite");
        assertTrue(clock.isOver(), "O relógio deve estar marcado como terminado após timeout");

        clock.start(); // Invocação após expiração
        assertTrue(clock.isOver(), "O relógio deve continuar marcado como terminado");
    }

    // --- Testes de stop() ---

    @Test
    @DisplayName("Deve parar o relógio em execução, marcar como terminado e congelar o tempo restante")
    void devePararRelogioEmExecucaoECongelarTempo() throws InterruptedException {
        GameClock clock = createClock(Duration.ofSeconds(10), () -> {});
        clock.start();

        // Breve pausa para permitir contagem
        Thread.sleep(30);
        clock.stop();

        assertTrue(clock.isOver(), "O relógio deve estar terminado após stop()");
        Duration rem1 = clock.remaining();
        Thread.sleep(30);
        Duration rem2 = clock.remaining();

        assertEquals(rem1, rem2, "O tempo restante deve ficar congelado após a paragem do relógio");
        assertTrue(rem1.compareTo(Duration.ZERO) > 0, "O tempo congelado deve ser positivo");
        assertTrue(rem1.compareTo(Duration.ofSeconds(10)) < 0, "O tempo congelado deve ter decrescido em relação ao inicial");
    }

    @Test
    @DisplayName("Deve permitir parar um relógio que nunca foi iniciado sem lançar exceções")
    void devePararRelogioQueNuncaFoiIniciado() {
        Duration bank = Duration.ofSeconds(15);
        GameClock clock = createClock(bank, () -> {});

        clock.stop();

        assertTrue(clock.isOver(), "O relógio deve ficar marcado como terminado após stop()");
        assertEquals(bank, clock.remaining(), "O tempo restante deve continuar a ser o banco inicial");
    }

    @Test
    @DisplayName("Deve ser idempotente em chamadas repetidas ao método stop()")
    void deveSerIdempotenteEmChamadasRepetidasDeStop() {
        GameClock clock = createClock(Duration.ofSeconds(10), () -> {});
        clock.start();

        clock.stop();
        assertTrue(clock.isOver(), "O relógio deve estar terminado após o primeiro stop()");

        clock.stop(); // Segundo stop
        assertTrue(clock.isOver(), "O relógio deve permanecer terminado após o segundo stop()");
    }

    @Test
    @DisplayName("Deve ignorar chamada ao método stop() se o relógio já tiver expirado")
    void deveIgnorarStopAposExpiracaoDoTempo() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        GameClock clock = createClock(Duration.ofMillis(30), latch::countDown);

        clock.start();
        assertTrue(latch.await(2, TimeUnit.SECONDS), "O timeout deve disparar");
        assertTrue(clock.isOver(), "O relógio deve estar terminado após expiração");

        clock.stop(); // stop após expirar
        assertTrue(clock.isOver(), "O relógio deve permanecer terminado");
    }

    // --- Testes de expiração, Agendador e Threads ---

    @Test
    @DisplayName("Deve disparar timeout ao expirar o tempo e executar ação na thread daemon 'game-clock'")
    void deveDispararTimeoutEExecutarAcaoNaThreadDaemon() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Thread> executionThread = new AtomicReference<>();

        GameClock clock = createClock(Duration.ofMillis(40), () -> {
            executionThread.set(Thread.currentThread());
            latch.countDown();
        });

        clock.start();
        boolean finished = latch.await(2, TimeUnit.SECONDS);

        assertTrue(finished, "O callback de expiração deve ter sido executado");
        assertNotNull(executionThread.get(), "A thread de execução do timeout não deve ser nula");
        assertEquals("game-clock", executionThread.get().getName(), "A thread agendadora deve ter o nome 'game-clock'");
        assertTrue(executionThread.get().isDaemon(), "A thread agendadora deve ser daemon");
        assertTrue(clock.isOver(), "O relógio deve reportar isOver() como true");
        assertEquals(Duration.ZERO, clock.remaining(), "O tempo restante após expiração deve ser Duration.ZERO");
    }

    @Test
    @DisplayName("Deve cancelar a execução do timeout caso stop() seja invocado antes de expirar")
    void deveCancelarTimeoutQuandoStopForChamadoPreviamente() throws InterruptedException {
        AtomicBoolean timeoutFired = new AtomicBoolean(false);
        GameClock clock = createClock(Duration.ofMillis(80), () -> timeoutFired.set(true));

        clock.start();
        clock.stop();

        // Aguarda margem superior ao banco para garantir que o agendador não executou a ação
        Thread.sleep(120);

        assertFalse(timeoutFired.get(), "O callback de timeout não deve ter sido executado após stop()");
        assertTrue(clock.isOver(), "O relógio deve estar marcado como terminado");
    }

    @Test
    @DisplayName("Deve ignorar o método privado expire() caso o relógio já se encontre terminado")
    void deveIgnorarExpireSeRelogioJaEstiverTerminado() throws Exception {
        AtomicBoolean timeoutFired = new AtomicBoolean(false);
        GameClock clock = createClock(Duration.ofSeconds(10), () -> timeoutFired.set(true));

        clock.stop();
        assertTrue(clock.isOver(), "O relógio deve estar parado");

        Method expireMethod = GameClock.class.getDeclaredMethod("expire");
        expireMethod.setAccessible(true);
        expireMethod.invoke(clock);

        assertFalse(timeoutFired.get(), "A ação onTimeout não deve ser executada se over for true");
    }

    // --- Testes de remaining() e isOver() ---

    @Test
    @DisplayName("Deve retornar Duration.ZERO quando o tempo decorrido exceder o banco de tempo")
    void deveRetornarZeroQuandoTempoRestanteForExcedido() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        GameClock clock = createClock(Duration.ofMillis(20), latch::countDown);

        clock.start();
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Timeout deve ocorrer");
        // Pequena espera adicional para garantir que watch.elapsed() >= bank
        Thread.sleep(20);

        assertEquals(Duration.ZERO, clock.remaining(), "O tempo restante não pode ser negativo e deve retornar Duration.ZERO");
    }

    // --- Testes de format() ---

    @Test
    @DisplayName("Deve formatar Duration.ZERO exatamente como '00:00'")
    void deveFormatarZeroCorretamente() {
        assertEquals("00:00", GameClock.format(Duration.ZERO), "Zero deve formatar como 00:00");
    }

    @Test
    @DisplayName("Deve formatar frações de segundo arredondando para cima para o próximo segundo")
    void deveFormatarFracoesComArredondamentoParaCima() {
        assertEquals("00:01", GameClock.format(Duration.ofMillis(1)), "1ms deve arredondar para 00:01");
        assertEquals("00:01", GameClock.format(Duration.ofMillis(500)), "500ms deve arredondar para 00:01");
        assertEquals("00:01", GameClock.format(Duration.ofMillis(999)), "999ms deve arredondar para 00:01");
        assertEquals("00:01", GameClock.format(Duration.ofMillis(1000)), "1000ms deve ser 00:01");
        assertEquals("00:02", GameClock.format(Duration.ofMillis(1001)), "1001ms deve arredondar para 00:02");
    }

    @Test
    @DisplayName("Deve formatar durações em segundos corretamente")
    void deveFormatarSegundosCorretamente() {
        assertEquals("00:05", GameClock.format(Duration.ofSeconds(5)), "5s deve formatar como 00:05");
        assertEquals("00:10", GameClock.format(Duration.ofSeconds(10)), "10s deve formatar como 00:10");
        assertEquals("00:59", GameClock.format(Duration.ofSeconds(59)), "59s deve formatar como 00:59");
    }

    @Test
    @DisplayName("Deve formatar durações em minutos e segundos com padding de zeros")
    void deveFormatarMinutosESegundosComPadding() {
        assertEquals("01:00", GameClock.format(Duration.ofSeconds(60)), "60s deve formatar como 01:00");
        assertEquals("01:05", GameClock.format(Duration.ofSeconds(65)), "65s deve formatar como 01:05");
        assertEquals("05:30", GameClock.format(Duration.ofMinutes(5).plusSeconds(30)), "5m30s deve formatar como 05:30");
        assertEquals("10:00", GameClock.format(Duration.ofMinutes(10)), "10m deve formatar como 10:00");
        assertEquals("59:59", GameClock.format(Duration.ofMinutes(59).plusSeconds(59)), "59m59s deve formatar como 59:59");
    }

    @Test
    @DisplayName("Deve formatar durações superiores a uma hora mantendo os minutos totais")
    void deveFormatarDuracoesSuperioresAUmaHora() {
        assertEquals("60:00", GameClock.format(Duration.ofHours(1)), "1 hora deve formatar como 60:00");
        assertEquals("75:03", GameClock.format(Duration.ofMinutes(75).plusSeconds(3)), "75m3s deve formatar como 75:03");
    }

    // --- Testes de Concorrência e Thread Safety ---

    @Test
    @DisplayName("Deve garantir thread-safety durante chamadas simultâneas aos métodos do relógio")
    void deveGarantirThreadSafetyEmOperacoesConcorrentes() throws InterruptedException {
        GameClock clock = createClock(Duration.ofMillis(200), () -> {});
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(8);

        for (int i = 0; i < 8; i++) {
            Runnable action = (i == 7) ? clock::stop : clock::start;
            pool.submit(() -> runConcurrentClockOps(clock, action, startSignal, doneSignal));
        }

        startSignal.countDown();
        boolean completed = doneSignal.await(2, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertTrue(completed, "Todas as threads concorrentes devem concluir sem bloqueio mútuo");
        assertTrue(clock.isOver(), "O relógio deve estar finalizado após o stop() concorrente");
    }

    private void runConcurrentClockOps(
            GameClock clock, Runnable action, CountDownLatch startSignal, CountDownLatch doneSignal) {
        try {
            startSignal.await();
            action.run();
            clock.remaining();
            clock.isOver();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            doneSignal.countDown();
        }
    }
}
