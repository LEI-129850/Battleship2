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
 * Janelinha sempre visível com o tempo restante.
 * Atualiza-se várias vezes por segundo e não rouba o foco à consola.
 */
public class ClockWindow implements AutoCloseable {

    private volatile JFrame frame;
    private volatile Timer timer;
    private final boolean supported;

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

    /** False se não há ambiente gráfico (ex: servidor sem ecrã). */
    public boolean isSupported() {
        return supported;
    }

    @Override
    public void close() {
        SwingUtilities.invokeLater(() -> {
            if (timer != null) timer.stop();
            if (frame != null) frame.dispose();
        });
    }
}