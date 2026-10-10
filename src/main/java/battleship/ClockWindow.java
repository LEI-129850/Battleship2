package battleship;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

/**
 * Janelinha sempre visível com o tempo restante da partida.
 * <p>
 * Atualiza-se várias vezes por segundo, fica sempre por cima das outras janelas
 * e não rouba o foco à consola. O texto fica vermelho no último minuto e a
 * janela indica "TEMPO ESGOTADO" quando o tempo acaba.
 * <p>
 * Toda a interface é criada e atualizada na thread de eventos do Swing.
 * Se não houver ambiente gráfico (modo headless), a janela não é criada.
 */
public class ClockWindow implements AutoCloseable {

    /** A janela; só é criada depois de a thread do Swing correr o código de arranque. */
    private volatile JFrame frame;

    /** Temporizador do Swing que atualiza o texto da janela. */
    private volatile Timer timer;

    /** False se o ambiente é headless (sem ecrã) e por isso a janela não existe. */
    private final boolean supported;

    /**
     * Cria e mostra a janela do relógio para o jogo indicado.
     * Não bloqueia: a criação da janela é agendada na thread do Swing.
     *
     * @param game o jogo cujo tempo restante é mostrado
     */
    public ClockWindow(IGame game) {
        this.supported = !GraphicsEnvironment.isHeadless();
        if (!supported)
            return;

        SwingUtilities.invokeLater(() -> {
            JLabel label = new JLabel("--:--", SwingConstants.CENTER);
            label.setFont(new Font(Font.MONOSPACED, Font.BOLD, 40));
            label.setOpaque(true);
            label.setBackground(new Color(20, 30, 50));
            label.setForeground(new Color(80, 220, 120));

            JFrame f = new JFrame("Tempo de jogo");
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            f.add(label);
            f.setSize(260, 110);
            f.setAlwaysOnTop(true);
            f.setFocusableWindowState(false);   // não rouba o foco à consola
            f.setLocationByPlatform(true);
            f.setVisible(true);
            frame = f;

            Timer t = new Timer(200, e -> {
                if (game.isFinished()) {
                    label.setText("00:00");
                    label.setForeground(Color.RED);
                    f.setTitle("TEMPO ESGOTADO");
                    ((Timer) e.getSource()).stop();
                    return;
                }
                long secs = (game.getRemainingTime().toMillis() + 999) / 1000;
                label.setText(GameClock.format(game.getRemainingTime()));
                label.setForeground(secs <= 60 ? Color.RED : new Color(80, 220, 120));
            });
            t.start();
            timer = t;
        });
    }

    /**
     * Indica se a janela pode ser mostrada.
     *
     * @return false se não há ambiente gráfico (ex: servidor sem ecrã)
     */
    public boolean isSupported() {
        return supported;
    }

    /**
     * Para a atualização e fecha a janela. Pode ser chamado a partir de qualquer thread.
     */
    @Override
    public void close() {
        SwingUtilities.invokeLater(() -> {
            if (timer != null) timer.stop();
            if (frame != null) frame.dispose();
        });
    }
}