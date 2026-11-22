package battleship;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Chat;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
                        "Tentativa " + attempt + "/" + MAX_RETRIES + " falhou: " + e.getMessage());

                if (attempt == MAX_RETRIES) {
                    throw new IOException(
                            "Não foi possível contactar a API após " + MAX_RETRIES +" tentativas.",
                            e);
                }

                sleeper.sleep(RETRY_DELAY);
            }
        }

        throw new IOException("Não foi possível contactar a API.");
    }

    /**
     * Versao real de sendWithRetry usada pela aplicacao.
     */
    private static String sendWithRetry(AIChat chat, String message)
            throws IOException, InterruptedException {
        return sendWithRetry(chat, message, REAL_SLEEPER);
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

        ObjectMapper mapper = new ObjectMapper();

        // Memória local da partida.
        List<JsonNode> historico = new ArrayList<>();
        List<String> posicoesJogadas = new ArrayList<>();
        List<String> posicoesDisponiveis = new ArrayList<>();
        int numeroRajada = 0;

        for (char linha = 'A'; linha <= 'J'; linha++) {
            for (int coluna = 1; coluna <= 10; coluna++) {
                posicoesDisponiveis.add("" + linha + coluna);
            }
        }

        AIChat chat = chatProvider.createChat();
        game.printMyBoard(true, true);

        // Regras e todas as posições disponíveis.
        var inicio = mapper.createObjectNode();
        inicio.put("regras", AIPrompts.PROMPT);
        inicio.set(
                "posicoesAindaNaoAtiradas",
                mapper.valueToTree(posicoesDisponiveis));
        inicio.put("instrucao", "Começa agora com a primeira rajada.");

        try {
            String responseText = sendWithRetry(
                    chat,
                    mapper.writeValueAsString(inicio),
                    sleeper);

            while (game.getRemainingShips() > 0) {
                System.out.println(responseText);

                // Converter o JSON do Gemini em posições do jogo.
                JsonNode tirosJson = mapper.readTree(responseText);
                List<IPosition> shots = convertJsonToPositions(tirosJson);

                // Executar a rajada e obter o resultado.
                game.fireShots(shots);

                IMove move = game.getAlienMoves().getLast();
                String result = move.processEnemyFire(false);
                JsonNode resultJson = mapper.readTree(result);
                numeroRajada++;

                // Guardar a rajada e o resultado real.
                var registo = mapper.createObjectNode();
                registo.put("rajada", numeroRajada);
                registo.set("tiros", tirosJson);
                registo.set("resultado", resultJson);
                historico.add(registo);

                // Atualizar as posições já utilizadas.
                for (JsonNode shot : tirosJson) {
                    String posicao = shot.get("row").asText()
                            + shot.get("column").asInt();

                    if (!posicoesJogadas.contains(posicao)) {
                        posicoesJogadas.add(posicao);
                    }
                }

                posicoesDisponiveis.removeAll(posicoesJogadas);
                game.printMyBoard(true, true);

                if (game.getRemainingShips() <= 0) {
                    break;
                }

                sleeper.sleep(TIMEOUT);

                // Lembretes com posições disponíveis.
                var pedido = mapper.createObjectNode();
                pedido.put("rajadasRealizadas", numeroRajada);
                pedido.put("naviosRestantes", game.getRemainingShips());
                pedido.set("resultado", resultJson);

                // A cada CONTEXT_RESET_INTERVA rajadas, relembrar também as regras e o histórico.
                if (numeroRajada % CONTEXT_RESET_INTERVAL == 0) {
                    chat = chatProvider.createNewChat();

                    pedido.put("regras", AIPrompts.PROMPT);

                    pedido.set(
                            "historico",
                            mapper.valueToTree(historico));

                    pedido.set(
                            "posicoesJaJogadas",
                            mapper.valueToTree(posicoesJogadas));

                    pedido.put("contexto", """
            Esta e uma nova conversa para continuar uma partida ja iniciada.
            O historico contem todas as rajadas executadas e os seus resultados.
            Essas rajadas ja aconteceram: nao as executes novamente.
            Usa as regras e o historico para escolher a proxima rajada.
            """);
                }

                pedido.set(
                        "posicoesAindaNaoAtiradas",
                        mapper.valueToTree(posicoesDisponiveis));

                pedido.put("instrucao", """
                        Continua esta partida, sem a reiniciar.
                        Escolhe os próximos 3 tiros EXCLUSIVAMENTE da lista
                        posicoesAindaNaoAtiradas, sem repetir posições
                        na mesma rajada.
                        Se restarem apenas 1 ou 2 posições disponíveis,
                        dispara em TODAS essas posições e só depois
                        completa os 3 tiros com posições repetidas.
                        Os resultados são agregados por rajada: não atribuas
                        resultados individuais a posições sem evidência.
                        Responde apenas com o array JSON dos próximos 3 tiros.
                        """);

                responseText = sendWithRetry(
                        chat,
                        mapper.writeValueAsString(pedido),
                        sleeper);
            }

            System.out.println(
                    "Fim da simulação: toda a frota foi afundada.");

            game.over();
            scoreboardManager.saveGame(
                    game.getAlienMoves().size(),
                    "Frota Afundada! (Simulação AI)");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("A simulação foi interrompida.", e);
        }
    }

    private static @NonNull List<IPosition> convertJsonToPositions(JsonNode tirosJson) {
        List<IPosition> shots = new ArrayList<>();

        for (JsonNode shot : tirosJson) {
            shots.add(new Position(
                    shot.get("row").asText().charAt(0),
                    shot.get("column").asInt()));
        }
        return shots;
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