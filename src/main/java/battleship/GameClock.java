package battleship;

import com.google.common.base.Stopwatch;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Contagem decrescente da partida, baseada no {@link Stopwatch} do Guava.
 * <p>
 * O {@code Stopwatch} mede o tempo que já passou e o relógio calcula o tempo
 * que falta (banco de tempo menos tempo decorrido). Um
 * {@link ScheduledExecutorService} agenda o fim do tempo, para que o timeout
 * seja detetado mesmo que a thread principal esteja bloqueada à espera de input.
 * <p>
 * A classe é thread-safe: o estado é protegido com {@code synchronized}, porque
 * é lido pela thread do menu, pela thread do agendador e pela janela do relógio.
 */
public class GameClock {

    /** Tempo total atribuído à partida. */
    private final Duration bank;

    /** Cronómetro do Guava que acumula o tempo decorrido enquanto está a correr. */
    private final Stopwatch watch = Stopwatch.createUnstarted();

    /** Ação executada uma única vez quando o tempo chega a zero. */
    private final Runnable onTimeout;

    /** Agendador que dispara {@link #expire()} quando o banco de tempo acaba (thread daemon). */
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "game-clock");
                t.setDaemon(true);
                return t;
            });

    /** True quando o relógio já terminou, por timeout ou por {@link #stop()}. */
    private boolean over;

    /**
     * Cria um relógio parado. A contagem só começa com {@link #start()}.
     *
     * @param bank      o tempo total da partida; tem de ser positivo
     * @param onTimeout a ação a executar quando o tempo chegar a zero
     * @throws IllegalArgumentException se o banco de tempo for nulo, zero ou negativo
     */
    public GameClock(Duration bank, Runnable onTimeout) {
        if (bank == null || bank.isZero() || bank.isNegative())
            throw new IllegalArgumentException("O banco de tempo tem de ser positivo");
        this.bank = bank;
        this.onTimeout = onTimeout;
    }

    /**
     * Inicia a contagem e agenda o fim do tempo.
     * Não faz nada se o relógio já estiver a correr ou já tiver terminado.
     */
    public synchronized void start() {
        if (over || watch.isRunning()) return;
        watch.start();
        scheduler.schedule(this::expire, bank.toNanos(), TimeUnit.NANOSECONDS);
    }

    /**
     * Pára o relógio sem disparar o timeout (nova partida, simulação, fim de jogo).
     * Depois de parado, o relógio não pode ser reiniciado.
     */
    public synchronized void stop() {
        if (over) return;
        over = true;
        if (watch.isRunning()) watch.stop();
        scheduler.shutdownNow();
    }

    /**
     * Calcula o tempo que ainda resta.
     *
     * @return o tempo restante, nunca negativo ({@link Duration#ZERO} quando acabou)
     */
    public synchronized Duration remaining() {
        Duration left = bank.minus(watch.elapsed());
        return left.isNegative() ? Duration.ZERO : left;
    }

    /**
     * Indica se o relógio já terminou.
     *
     * @return true se terminou por timeout ou por {@link #stop()}
     */
    public synchronized boolean isOver() {
        return over;
    }

    /**
     * Formata uma duração como {@code mm:ss}, a arredondar para cima, de modo que
     * só mostra {@code 00:00} quando o tempo acabou mesmo.
     *
     * @param d a duração a formatar
     * @return o texto no formato {@code mm:ss}
     */
    public static String format(Duration d) {
        long secs = (d.toMillis() + 999) / 1000;
        return String.format("%02d:%02d", secs / 60, secs % 60);
    }

    /**
     * Chamado pelo agendador quando o banco de tempo acaba. Pára o cronómetro,
     * marca o relógio como terminado e executa o {@code onTimeout}.
     * Não faz nada se o relógio já tiver sido parado.
     */
    private void expire() {
        synchronized (this) {
            if (over) return;
            watch.stop();
            over = true;
            scheduler.shutdown();
        }
        onTimeout.run();   // fora do lock, para evitar deadlocks
    }
}