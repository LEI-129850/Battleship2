package battleship;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for battleship.AI.
 * Author: LEI-129830 10:57
 * Date: 2026-11-21
 * Cyclomatic Complexity:
 * - sendWithRetry(Chat, String, Sleeper): 4
 * - simula(IGame, ScoreboardManager, AIChatProvider, Sleeper): 10
 */
class AITest {

    private ObjectMapper mapper;
    private FakeScoreboard scoreboard;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        scoreboard = new FakeScoreboard();
    }

    @AfterEach
    void tearDown() {
        mapper = null;
        scoreboard = null;
    }

    @Test
    @DisplayName("sendWithRetry succeeds immediately on the first attempt")
    void sendWithRetry1() {
        int[] attempts = {0};
        List<Long> sleeps = new ArrayList<>();

        AI.AIChat chat = message -> {
            attempts[0]++;
            return "success";
        };

        assertDoesNotThrow(
                () -> {
                    String response = AI.sendWithRetry(
                            chat,
                            "test",
                            milliseconds -> sleeps.add(milliseconds));

                    assertEquals(
                            "success",
                            response,
                            "Error: expected the first response to be returned, but got "
                                    + response);
                },
                "Error: expected sendWithRetry to succeed on the first attempt");

        assertAll(
                () -> assertEquals(
                        1,
                        attempts[0],
                        "Error: expected exactly one attempt, but got "
                                + attempts[0]),
                () -> assertTrue(
                        sleeps.isEmpty(),
                        "Error: expected no retry delays after an immediate success"));
    }

    @Test
    @DisplayName("sendWithRetry succeeds after two failures")
    void sendWithRetry2() {
        int[] attempts = {0};
        List<Long> sleeps = new ArrayList<>();

        AI.AIChat chat = message -> {
            attempts[0]++;
            if (attempts[0] < 3) {
                throw new RuntimeException("Temporary failure");
            }
            return "success";
        };

        assertDoesNotThrow(
                () -> {
                    String response = AI.sendWithRetry(
                            chat,
                            "test",
                            milliseconds -> sleeps.add(milliseconds));

                    assertEquals(
                            "success",
                            response,
                            "Error: expected success after retry, but got "
                                    + response);
                },
                "Error: expected sendWithRetry to succeed after two failures");

        assertAll(
                () -> assertEquals(
                        3,
                        attempts[0],
                        "Error: expected three attempts, but got "
                                + attempts[0]),
                () -> assertEquals(
                        List.of(2000L, 2000L),
                        sleeps,
                        "Error: expected two 2000 ms delays, but got "
                                + sleeps));
    }

    @Test
    @DisplayName("sendWithRetry throws IOException after five failures")
    void sendWithRetry3() {
        int[] attempts = {0};
        List<Long> sleeps = new ArrayList<>();

        AI.AIChat chat = message -> {
            attempts[0]++;
            throw new RuntimeException("Failure");
        };

        IOException exception = assertThrows(
                IOException.class,
                () -> AI.sendWithRetry(
                        chat,
                        "test",
                        milliseconds -> sleeps.add(milliseconds)),
                "Error: expected IOException after five failed attempts");

        assertAll(
                () -> assertEquals(
                        5,
                        attempts[0],
                        "Error: expected five attempts, but got "
                                + attempts[0]),
                () -> assertEquals(
                        List.of(2000L, 2000L, 2000L, 2000L),
                        sleeps,
                        "Error: expected four 2000 ms retry delays, but got "
                                + sleeps),
                () -> assertTrue(
                        exception.getMessage().contains(
                                "Não foi possível contactar a API"),
                        "Error: expected API-contact failure message, but got "
                                + exception.getMessage()),
                () -> assertInstanceOf(
                        RuntimeException.class,
                        exception.getCause(),
                        "Error: expected the original failure as IOException cause"));
    }

    @Test
    @DisplayName("sendWithRetry propagates interruption during a retry delay")
    void sendWithRetry4() {
        AI.AIChat chat = message -> {
            throw new RuntimeException("Failure");
        };

        InterruptedException exception = assertThrows(
                InterruptedException.class,
                () -> AI.sendWithRetry(
                        chat,
                        "test",
                        milliseconds -> {
                            throw new InterruptedException("Interrupted during retry");
                        }),
                "Error: expected sendWithRetry to propagate InterruptedException");

        assertEquals(
                "Interrupted during retry",
                exception.getMessage(),
                "Error: expected the propagated interruption message to be preserved");
    }

    @Test
    @DisplayName("simula completes one volley without an API key")
    void simula1() {
        FakeChat chat = new FakeChat(validVolley("A", 1));
        FakeChatProvider provider = new FakeChatProvider(chat);
        FakeGame game = new FakeGame(1);
        List<String> sentBeforeSleeper = new ArrayList<>();

        assertDoesNotThrow(
                () -> AI.simula(
                        game,
                        scoreboard,
                        provider,
                        milliseconds -> {
                            sentBeforeSleeper.add(
                                    chat.sentMessages.isEmpty()
                                            ? "no-message"
                                            : chat.sentMessages.getLast());
                        }),
                "Error: expected simula to complete a one-volley game");

        assertAll(
                () -> assertEquals(
                        1,
                        chat.sentMessages.size(),
                        "Error: expected one initial chat message, but got "
                                + chat.sentMessages.size()),
                () -> assertTrue(
                        sentBeforeSleeper.isEmpty(),
                        "Error: expected no sleep after the final volley"),
                () -> assertEquals(
                        2,
                        game.printCalls,
                        "Error: expected board prints before and after the volley, but got "
                                + game.printCalls),
                () -> assertEquals(
                        1,
                        game.fireShotsCalls,
                        "Error: expected one volley, but got "
                                + game.fireShotsCalls),
                () -> assertEquals(
                        3,
                        game.lastShots.size(),
                        "Error: expected exactly three shots, but got "
                                + game.lastShots.size()),
                () -> assertTrue(
                        game.overCalled,
                        "Error: expected game.over() to be called"),
                () -> assertTrue(
                        scoreboard.saved,
                        "Error: expected the scoreboard result to be saved"),
                () -> assertEquals(
                        "Frota Afundada! (Simulação AI)",
                        scoreboard.result,
                        "Error: expected the AI simulation score label, but got "
                                + scoreboard.result));
    }

    @Test
    @DisplayName("simula creates a new chat and reminder at the tenth volley")
    void simula2() {
        Deque<String> responses = new ArrayDeque<>();

        for (int volley = 1; volley <= 11; volley++) {
            responses.add(validVolley(
                    String.valueOf((char) ('A' + volley - 1)),
                    1));
        }

        FakeChat initialChat = new FakeChat(responses);
        FakeChat newChat = new FakeChat(validVolley("A", 1));
        FakeChatProvider provider =
                new FakeChatProvider(initialChat, newChat);

        FakeGame game = new FakeGame(11);
        List<Long> sleeps = new ArrayList<>();

        assertDoesNotThrow(
                () -> AI.simula(
                        game,
                        scoreboard,
                        provider,
                        milliseconds -> sleeps.add(milliseconds)),
                "Error: expected simula to reach and complete the tenth-volley reminder");

        assertAll(
                () -> assertEquals(
                        1,
                        provider.createChatCalls,
                        "Error: expected one initial chat provider call, but got "
                                + provider.createChatCalls),
                () -> assertEquals(
                        1,
                        provider.createNewChatCalls,
                        "Error: expected one new chat provider call at volley ten, but got "
                                + provider.createNewChatCalls),
                () -> assertEquals(
                        11,
                        game.fireShotsCalls,
                        "Error: expected eleven volleys, but got "
                                + game.fireShotsCalls),
                () -> assertFalse(
                        newChat.sentMessages.isEmpty(),
                        "Error: expected the reminder chat to receive a message"),
                () -> assertTrue(
                        newChat.sentMessages.getFirst().contains("\"regras\""),
                        "Error: expected the tenth-volley reminder to contain rules"),
                () -> assertTrue(
                        newChat.sentMessages.getFirst().contains("\"historico\""),
                        "Error: expected the tenth-volley reminder to contain history"),
                () -> assertTrue(
                        newChat.sentMessages.getFirst().contains("\"posicoesJaJogadas\""),
                        "Error: expected the tenth-volley reminder to contain played positions"),
                () -> assertEquals(
                        11,
                        game.printCalls - 1,
                        "Error: expected each volley to update the board"),
                () -> assertEquals(
                        10,
                        sleeps.size(),
                        "Error: expected five-second sleeps after ten non-final volleys, but got "
                                + sleeps.size()),
                () -> assertTrue(
                        sleeps.stream().allMatch(value -> value == 5000L),
                        "Error: expected every inter-volley sleep to be 5000 ms"));
    }

    @Test
    @DisplayName("simula keeps a new shot from updating the played-position history twice")
    void simula3() {
        FakeChat chat = new FakeChat(validVolley("A", 1));
        FakeChatProvider provider = new FakeChatProvider(chat);
        FakeGame game = new FakeGame(1);

        assertDoesNotThrow(
                () -> AI.simula(
                        game,
                        scoreboard,
                        provider,
                        milliseconds -> { }),
                "Error: expected simula to process a volley and finish");

        assertTrue(
                chat.sentMessages.getFirst().contains(
                        "\"posicoesAindaNaoAtiradas\""),
                "Error: expected the initial request to contain available positions");
    }

    @Test
    @DisplayName("simula throws IOException and restores interruption")
    void simula4() {
        FakeChat chat = new FakeChat(validVolley("A", 1));
        FakeChatProvider provider = new FakeChatProvider(chat);
        FakeGame game = new FakeGame(2);

        try {
            IOException exception = assertThrows(
                    IOException.class,
                    () -> AI.simula(
                            game,
                            scoreboard,
                            provider,
                            milliseconds -> {
                                throw new InterruptedException(
                                        "Interrupted during simulation");
                            }),
                    "Error: expected simulation interruption to throw IOException");

            assertAll(
                    () -> assertInstanceOf(
                            InterruptedException.class,
                            exception.getCause(),
                            "Error: expected InterruptedException as the IOException cause"),
                    () -> assertTrue(
                            Thread.currentThread().isInterrupted(),
                            "Error: expected the interrupted status to remain set"));
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    @DisplayName("simula propagates IOException when the chat exhausts retries")
    void simula5() {
        AI.AIChat failingChat = message -> {
            throw new RuntimeException("Unavailable");
        };
        FakeChatProvider provider =
                new FakeChatProvider(failingChat);
        FakeGame game = new FakeGame(1);

        IOException exception = assertThrows(
                IOException.class,
                () -> AI.simula(
                        game,
                        scoreboard,
                        provider,
                        milliseconds -> { }),
                "Error: expected exhausted chat retries to throw IOException");

        assertAll(
                () -> assertTrue(
                        exception.getMessage().contains(
                                "Não foi possível contactar a API"),
                        "Error: expected the API-contact failure message, but got "
                                + exception.getMessage()),
                () -> assertEquals(
                        0,
                        game.fireShotsCalls,
                        "Error: expected no volley after chat failure, but got "
                                + game.fireShotsCalls),
                () -> assertFalse(
                        scoreboard.saved,
                        "Error: expected chat failure not to save a result"));
    }

    private String validVolley(String row, int column) {
        if (column > 8) {
            throw new IllegalArgumentException(
                    "A three-shot volley cannot use column " + column);
        }

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
                return validFallbackVolley();
            }

            return responses.removeFirst();
        }

        private String validFallbackVolley() {
            return """
                    [{"row":"A","column":1},
                     {"row":"A","column":2},
                     {"row":"A","column":3}]
                    """;
        }
    }

    private static final class FakeChatProvider implements AI.AIChatProvider {
        private final AI.AIChat initialChat;
        private final AI.AIChat nextChat;
        private int createChatCalls;
        private int createNewChatCalls;

        FakeChatProvider(AI.AIChat chat) {
            this(chat, chat);
        }

        FakeChatProvider(AI.AIChat initialChat, AI.AIChat nextChat) {
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

            assertEquals(
                    3,
                    shots.size(),
                    "Error: expected every volley to contain exactly three shots");

            remainingShips = Math.max(
                    0,
                    remainingShips - 1);

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
            throw new UnsupportedOperationException(
                    "stopClock is not used by the AI simulation");
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

    private static final class FakeScoreboard extends ScoreboardManager {
        private boolean saved;
        private int shots;
        private String result;

        @Override
        public void saveGame(int tirosTotais, String resultado) {
            saved = true;
            shots = tirosTotais;
            result = resultado;
        }
    }
}
