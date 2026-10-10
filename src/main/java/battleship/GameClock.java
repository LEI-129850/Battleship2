package battleship;

import com.google.common.base.Stopwatch;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameClock {

    private final Duration bank;
    private final Stopwatch watch = Stopwatch.createUnstarted();
    private final Runnable onTimeout;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "game-clock");
                t.setDaemon(true);
                return t;
            });

    private boolean over;

    public GameClock(Duration bank, Runnable onTimeout) {
        if (bank == null || bank.isZero() || bank.isNegative())
            throw new IllegalArgumentException("O banco de tempo tem de ser positivo");
        this.bank = bank;
        this.onTimeout = onTimeout;
    }

    public synchronized void start() {
        if (over || watch.isRunning()) return;
        watch.start();
        scheduler.schedule(this::expire, bank.toNanos(), TimeUnit.NANOSECONDS);
    }

    public synchronized void stop() {
        if (over) return;
        over = true;
        if (watch.isRunning()) watch.stop();
        scheduler.shutdownNow();
    }

    public synchronized Duration remaining() {
        Duration left = bank.minus(watch.elapsed());
        return left.isNegative() ? Duration.ZERO : left;
    }

    public synchronized boolean isOver() {
        return over;
    }

    public static String format(Duration d) {
        long secs = (d.toMillis() + 999) / 1000;
        return String.format("%02d:%02d", secs / 60, secs % 60);
    }

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