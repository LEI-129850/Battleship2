package battleship;


import com.google.genai.Chat;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import java.io.IOException;

/**
 * Permite simular um jogo de batalha naval utilizando o Gemini para escolher os tiros
 */
public class AI {

    public static final int MAX_RETRIES = 5;
    public static final int RETRY_DELAY = 2000;
    public static final int TIMEOUT = 5000;

    public static final int CONTEXT_RESET_INTERVAL = 10;
    public static final String MODEL_NAME = "gemini-3.5-flash-lite";

    /**
     * Permite substituir o envio de mensagens do Gemini em testes.
     */
    interface AIChat {
        String sendMessage(String message) throws Exception;
    }

    /**
     * Permite substituir a criação de conversas do Gemini em testes.
     */
    interface AIChatProvider {
        AIChat createChat();

        AIChat createNewChat();
    }

    /**
     * Permite substituir delays em testes.
     */
    @FunctionalInterface
    interface Sleeper {
        void sleep(long milliseconds) throws InterruptedException;
    }

    private static final Sleeper REAL_SLEEPER = Thread::sleep;


    /**
     * Envia uma mensagem ao chatbot e tenta novamente ate um maximo
     * de 5 vezes, esperando 2 segundos entre tentativas.
     *
     * @param chat O chatbot utilizado na conversa
     * @param message A mensagem a enviar ao chatbot
     * @param sleeper Funcao usada para esperar entre tentativas
     * @return A resposta textual do chatbot
     * @throws IOException Se nao for possivel obter uma resposta apos 5 tentativas
     * @throws InterruptedException Se a espera entre tentativas for interrompida
     */
    static String sendWithRetry(
            AIChat chat,
            String message,
            Sleeper sleeper)
            throws IOException, InterruptedException {

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return chat.sendMessage(message);
            } catch (Exception e) {
                System.out.println(
                        "Tentativa " + attempt + "/5 falhou: " + e.getMessage());

                if (attempt == 5) {
                    throw new IOException(
                            "Não foi possível contactar a API após 5 tentativas.",
                            e);
                }

                sleeper.sleep(RETRY_DELAY);
            }
        }

        throw new IOException("Não foi possível contactar a API.");
    }

    /**
     * Implementacao real do fornecedor de chats Gemini.
     */
    private static final class GeminiChatProvider implements AIChatProvider {
        private final Client client;

        private GeminiChatProvider(Client client) {
            this.client = client;
        }

        @Override
        public AIChat createChat() {
            return wrap(client.chats.create(MODEL_NAME));
        }

        @Override
        public AIChat createNewChat() {
            return wrap(client.chats.create(MODEL_NAME));
        }

        private static AIChat wrap(Chat chat) {
            return message -> {
                GenerateContentResponse response =
                        chat.sendMessage(message);
                return response.text();
            };
        }
    }

    /**
     * Overload testavel que nao cria um cliente gemini ou espera por tempo real
     */
    static void simula(
            IGame game,
            ScoreboardManager scoreboardManager,
            AIChatProvider chatProvider,
            Sleeper sleeper)
            throws IOException {

        new AISimulation(
                game,
                scoreboardManager,
                chatProvider,
                sleeper).execute();

    }

    /**
     * Simula um jogo em que o Gemini dispara rajadas contra a frota do jogo.
     * Guarda as rajadas e os seus resultados e envia as posicoes ainda nao
     * utilizadas em cada pedido. A cada 10 rajadas, envia tambem as regras
     * e o historico completo para relembrar o chatbot do estado do jogo.
     * A simulacao termina quando todos os navios estiverem afundados.
     *
     * @param game O jogo que contem a frota contra a qual o Gemini vai jogar
     * @param apiKey A chave utilizada para aceder a API do Gemini
     * @param scoreboardManager O gestor de pontuações utilizado para registar
     *                          o resultado da simulação
     * @throws IOException Se ocorrer um erro ao processar o JSON, se nao for
     *                     possivel contactar a API ou se a simulacao for interrompida
     */
    public static void simula(
            IGame game,
            String apiKey,
            ScoreboardManager scoreboardManager)
            throws IOException {

        try (Client client =
                     Client.builder().apiKey(apiKey).build()) {
            AIChatProvider chatProvider =
                    new GeminiChatProvider(client);

            simula(
                    game,
                    scoreboardManager,
                    chatProvider,
                    REAL_SLEEPER);
        }
    }
}