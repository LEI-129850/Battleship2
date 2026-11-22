package battleship;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


final class AISimulation {

    private final IGame game;
    private final ScoreboardManager scoreboardManager;
    private final AI.AIChatProvider chatProvider;
    private final AI.Sleeper sleeper;

    private final ObjectMapper mapper = new ObjectMapper();

    private final List<JsonNode> historico = new ArrayList<>();
    private final List<String> posicoesJogadas = new ArrayList<>();
    private final List<String> posicoesDisponiveis = new ArrayList<>();

    private AI.AIChat chat;
    private int numeroRajada = 0;

    AISimulation(
            IGame game,
            ScoreboardManager scoreboardManager,
            AI.AIChatProvider chatProvider,
            AI.Sleeper sleeper) {

        this.game = game;
        this.scoreboardManager = scoreboardManager;
        this.chatProvider = chatProvider;
        this.sleeper = sleeper;
    }

    /**
     *  executa a logica de simulacao do jogo de AI
     */
    void execute() throws IOException {

        initializePositions();

        chat = chatProvider.createChat();
        game.printMyBoard(true, true);

        try {
            String responseText = sendInitialRequest();

            while (game.getRemainingShips() > 0) {
                responseText = executeRound(responseText);
            }

            finishSimulation();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(
                    "A simulação foi interrompida.", e);
        }
    }

    private void initializePositions() {
        for (char linha = 'A'; linha <= 'J'; linha++) {
            for (int coluna = 1; coluna <= 10; coluna++) {
                posicoesDisponiveis.add("" + linha + coluna);
            }
        }
    }

    private String sendInitialRequest()
            throws IOException, InterruptedException {

        var inicio = mapper.createObjectNode();

        inicio.put("regras", AIPrompts.PROMPT);
        inicio.set(
                "posicoesAindaNaoAtiradas",
                mapper.valueToTree(posicoesDisponiveis));

        inicio.put(
                "instrucao",
                "Começa agora com a primeira rajada.");

        return AI.sendWithRetry(
                chat,
                mapper.writeValueAsString(inicio),
                sleeper);
    }

    private String executeRound(String responseText)
            throws IOException, InterruptedException {

        System.out.println(responseText);

        JsonNode tirosJson = mapper.readTree(responseText);

        List<IPosition> shots =
                convertJsonToPositions(tirosJson);

        game.fireShots(shots);

        IMove move = game.getAlienMoves().getLast();

        String result = move.processEnemyFire(false);

        JsonNode resultJson = mapper.readTree(result);

        numeroRajada++;

        var registo = mapper.createObjectNode();

        registo.put("rajada", numeroRajada);
        registo.set("tiros", tirosJson);
        registo.set("resultado", resultJson);

        historico.add(registo);

        updatePlayedPositions(tirosJson);

        game.printMyBoard(true, true);

        if (game.getRemainingShips() <= 0) {
            return null;
        }

        sleeper.sleep(AI.TIMEOUT);

        var pedido = buildNextRequest(resultJson);

        return AI.sendWithRetry(
                chat,
                mapper.writeValueAsString(pedido),
                sleeper);
    }

    private void updatePlayedPositions(JsonNode tirosJson) {

        for (JsonNode shot : tirosJson) {

            String posicao = shot.get("row").asText()
                    + shot.get("column").asInt();

            if (!posicoesJogadas.contains(posicao)) {
                posicoesJogadas.add(posicao);
            }
        }

        posicoesDisponiveis.removeAll(posicoesJogadas);
    }

    private ObjectNode buildNextRequest(JsonNode resultJson) {

        var pedido = mapper.createObjectNode();

        pedido.put("rajadasRealizadas", numeroRajada);
        pedido.put("naviosRestantes", game.getRemainingShips());
        pedido.set("resultado", resultJson);

        if (numeroRajada % AI.CONTEXT_RESET_INTERVAL == 0) {

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

        return pedido;
    }

    private void finishSimulation() {

        System.out.println(
                "Fim da simulação: toda a frota foi afundada.");

        game.over();

        scoreboardManager.saveGame(
                game.getAlienMoves().size(),
                "Frota Afundada! (Simulação AI)");
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
}