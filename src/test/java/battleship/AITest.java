package battleship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Classe teste para AI.
 * Autor: Flavio Santos
 * Data: 2026-11-21
 */
@DisplayName("AITest")
class AITest {

    @Test
    @DisplayName("Deve executar uma simulação com sucesso sem usar uma chave da API")
    void simulaSucceedsWithoutApiKey() {
        FakeChat chat = new FakeChat(validVolley("A", 1));
        FakeChatProvider provider = new FakeChatProvider(chat);
        FakeGame game = new FakeGame(1);
        FakeScoreboard scoreboard = new FakeScoreboard();

        assertDoesNotThrow(() -> AI.simula(
                game,
                scoreboard,
                provider,
                milliseconds -> { }));

        assertEquals(1, chat.sentMessages.size());
        assertEquals(2, game.printCalls);
        assertEquals(1, game.fireShotsCalls);
        assertEquals(3, game.lastShots.size());
        assertTrue(game.overCalled);
        assertTrue(scoreboard.saved);
    }


    @Test
    @DisplayName("Deve criar uma nova conversa na décima rajada")
    void simulaCreatesNewChatAtTenthVolley() {
        Deque<String> responsesForInitialChat = new ArrayDeque<>();
        for (int volley = 2; volley <= 10; volley++) {
            responsesForInitialChat.add(validVolley(
                    String.valueOf((char) ('A' + volley - 1)),
                    1));
        }

        FakeChat initialChat = new FakeChat(
                validVolley("A", 1));
        initialChat.responses.addAll(responsesForInitialChat);

        FakeChat newChat = new FakeChat(validVolley("A", 1));
        FakeChatProvider provider =
                new FakeChatProvider(initialChat, newChat);

        // Keep the game alive past volley 10 so the reminder branch runs.
        FakeGame game = new FakeGame(11);
        FakeScoreboard scoreboard = new FakeScoreboard();

        assertDoesNotThrow(() -> AI.simula(
                game,
                scoreboard,
                provider,
                milliseconds -> { }));

        assertEquals(1, provider.createChatCalls);
        assertEquals(1, provider.createNewChatCalls);
        assertEquals(11, game.fireShotsCalls);

        assertTrue(newChat.sentMessages.size() >= 1);
        String reminder = newChat.sentMessages.getFirst();
        assertTrue(reminder.contains("\"regras\""));
        assertTrue(reminder.contains("\"historico\""));
        assertTrue(reminder.contains("\"posicoesJaJogadas\""));
    }

    @Test
    @DisplayName("Deve tentar novamente e obter sucesso após duas falhas")
    void sendWithRetrySucceedsAfterRetry() {
        int[] attempts = {0};

        AI.AIChat chat = message -> {
            attempts[0]++;

            if (attempts[0] < 3) {
                throw new RuntimeException("Falha temporária");
            }

            return "success";
        };

        List<Long> sleeps = new ArrayList<>();

        assertDoesNotThrow(() -> {
            String response = AI.sendWithRetry(
                    chat,
                    "test",
                    milliseconds -> sleeps.add(milliseconds));

            assertEquals("success", response);
        });

        assertEquals(3, attempts[0]);
        assertEquals(List.of(2000L, 2000L), sleeps);
    }

    @Test
    @DisplayName("Deve falhar após cinco tentativas de envio")
    void sendWithRetryFailsAfterFiveAttempts() {
        int[] attempts = {0};

        AI.AIChat chat = message -> {
            attempts[0]++;
            throw new RuntimeException("Falha");
        };

        List<Long> sleeps = new ArrayList<>();

        IOException exception = assertThrows(
                IOException.class,
                () -> AI.sendWithRetry(
                        chat,
                        "test",
                        milliseconds -> sleeps.add(milliseconds)));

        assertEquals(5, attempts[0]);
        assertEquals(List.of(
                        2000L,
                        2000L,
                        2000L,
                        2000L),
                sleeps);
        assertTrue(exception.getMessage().contains(
                "Não foi possível contactar a API"));
    }

    @Test
    @DisplayName("Deve restaurar a interrupção da thread quando a simulação é interrompida")
    void simulaRestoresInterruptedStatus() {
        FakeChat chat = new FakeChat(validVolley("A", 1));
        FakeChatProvider provider = new FakeChatProvider(chat);
        FakeGame game = new FakeGame(2);
        FakeScoreboard scoreboard = new FakeScoreboard();

        try {
            IOException exception = assertThrows(
                    IOException.class,
                    () -> AI.simula(
                            game,
                            scoreboard,
                            provider,
                            milliseconds -> {
                                throw new InterruptedException();
                            }));

            assertTrue(exception.getCause() instanceof InterruptedException);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            // Prevent the interrupt flag from affecting other tests.
            Thread.interrupted();
        }
    }
    private static String validVolley(String row, int column) {
        return """
                [{"row":"%s","column":%d},
                 {"row":"%s","column":%d},
                 {"row":"%s","column":%d}]
                """.formatted(
                row, column,
                row, column + 1,
                row, column + 2);
    }

    private static final class FakeChat implements AI.AIChat {
        private final Deque<String> responses;
        private final List<String> sentMessages = new ArrayList<>();

        FakeChat(String response) {
            responses = new ArrayDeque<>();
            responses.add(response);
        }

        FakeChat(Deque<String> responses) {
            this.responses = new ArrayDeque<>(responses);
        }

        @Override
        public String sendMessage(String message) {
            sentMessages.add(message);

            if (responses.isEmpty()) {
                return validVolley("A", 1);
            }

            return responses.removeFirst();
        }
    }

    private static final class FakeChatProvider
            implements AI.AIChatProvider {

        private final AI.AIChat initialChat;
        private final AI.AIChat nextChat;
        private int createChatCalls;
        private int createNewChatCalls;

        FakeChatProvider(AI.AIChat chat) {
            this(chat, chat);
        }

        FakeChatProvider(
                AI.AIChat initialChat,
                AI.AIChat nextChat) {

            this.initialChat = initialChat;
            this.nextChat = nextChat;
        }

        @Override
        public AI.AIChat createChat() {
            createChatCalls++;
            return initialChat;
        }

        @Override
        public AI.AIChat createNewChat() {
            createNewChatCalls++;
            return nextChat;
        }
    }

    private static final class FakeGame implements IGame {
        private final List<IMove> alienMoves = new ArrayList<>();
        private int remainingShips;
        private int fireShotsCalls;
        private int printCalls;
        private List<IPosition> lastShots = new ArrayList<>();
        private boolean overCalled;

        FakeGame(int remainingShips) {
            this.remainingShips = remainingShips;
        }

        @Override
        public void fireShots(List<IPosition> shots) {
            fireShotsCalls++;
            lastShots = new ArrayList<>(shots);

            if (remainingShips > 0) {
                remainingShips--;
            }

            IMove move = new Move(
                    fireShotsCalls,
                    new ArrayList<>(shots),
                    List.of(
                            new IGame.ShotResult(
                                    true, false, null, false),
                            new IGame.ShotResult(
                                    true, false, null, false),
                            new IGame.ShotResult(
                                    true, false, null, false)));

            alienMoves.add(move);
        }

        @Override
        public int getRemainingShips() {
            return remainingShips;
        }

        @Override
        public List<IMove> getAlienMoves() {
            return alienMoves;
        }

        @Override
        public void printMyBoard(
                boolean showShots,
                boolean showLegend) {

            printCalls++;
        }

        @Override
        public void over() {
            overCalled = true;
        }

        @Override
        public String randomEnemyFire() {
            return "";
        }

        @Override
        public String readEnemyFire(Scanner in) {
            return "";
        }

        @Override
        public IGame.ShotResult fireSingleShot(
                IPosition pos,
                boolean isRepeated) {

            return new IGame.ShotResult(
                    true, isRepeated, null, false);
        }

        @Override
        public IFleet getMyFleet() {
            return null;
        }

        @Override
        public IFleet getAlienFleet() {
            return null;
        }

        @Override
        public List<IMove> getMyMoves() {
            return new ArrayList<>();
        }

        @Override
        public int getRepeatedShots() {
            return 0;
        }

        @Override
        public int getInvalidShots() {
            return 0;
        }

        @Override
        public int getHits() {
            return 0;
        }

        @Override
        public int getSunkShips() {
            return 0;
        }

        @Override
        public void printAlienBoard(
                boolean showShots,
                boolean showLegend) {
        }

        @Override
        public Duration getRemainingTime() {
            return Duration.ZERO;
        }

        @Override
        public String clockStatus() {
            return "Tempo restante: 00:00";
        }

        @Override
        public void stopClock() {
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public int radar(IPosition atual) {
            return 0;
        }
    }

    private static final class FakeScoreboard
            extends ScoreboardManager {

        private boolean saved;

        @Override
        public void saveGame(
                int tirosTotais,
                String resultado) {

            saved = true;
        }
    }
}