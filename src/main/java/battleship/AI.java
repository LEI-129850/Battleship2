package battleship;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Chat;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Permite simular um jogo de batalha naval utilizando o Gemini para escolher os tiros
 */
public class AI {

    /**
     * Contem as regras do jogo e as instrucoes sobre o formato JSON das rajadas
     */
    private static final String PROMPT = """
            Vais jogar Batalha Naval contra uma frota secreta gerada pelo programa.
            O tabuleiro tem linhas A-J e colunas 1-10. Existem 4 Barcas de 1 casa,
            3 Caravelas de 2 casas, 2 Naus de 3 casas, 1 Fragata de 4 casas e
            1 Galeao de 5 casas em T (corpo de 3 casas e duas asas numa extremidade).
            Os navios retos podem ser horizontais ou verticais; o Galeao tem quatro
            orientações. Os navios não se tocam, nem na diagonal.

            Em cada resposta envia APENAS um array JSON com exatamente 3 tiros:
            [{"row":"A","column":5},{"row":"C","column":10},{"row":"F","column":5}]
            Não uses Markdown, explicações ou tabuleiros.

            Receberás o resultado agregado da rajada, por exemplo:
            {"validShots":3,"sunkBoats":[{"count":1,"type":"Barca"}],
             "repeatedShots":0,"outsideShots":0,
             "hitsOnBoats":[{"hits":1,"type":"Nau"}],"missedShots":1}

            Este resultado não identifica qual dos três tiros acertou em cada navio.
            Usa o histórico para recordar os tiros e procurar navios junto de
            possíveis acertos. Não dispares fora do tabuleiro.

            Receberás também uma lista posicoesAindaNaoAtiradas.
            Escolhe os próximos 3 tiros exclusivamente dessa lista, sem repetir.
            Se restarem apenas 1 ou 2 posições disponíveis, dispara em todas
            essas posições e só depois completa a rajada com tiros repetidos.
            
            ---IMPORTANTE----
            Abaixo encontra-se a estrategia de que utilizaras para jogar:
            
            ESTRATEGIA DE JOGO:
            
            1. REGISTO E CERTEZA
            Usa o historico e a lista de posicoes ainda nao atiradas.
            Distingue factos confirmados de hipoteses.
            O resultado de uma rajada e agregado: se houver um acerto entre
            tres tiros, nao sabes automaticamente qual das tres posicoes acertou.
            
            2. PROCURA
            Enquanto nao houver uma zona promissora por investigar, procura
            navios em zonas pouco exploradas.
            Prefere rajadas com tres casas proximas, idealmente consecutivas
            na mesma linha ou coluna, para localizar melhor a origem dos acertos.
            Escolhe apenas casas ainda nao utilizadas.
            Escolher tres casas proximas NAO e um objetivo por si so.
            Nao percorras linhas ou colunas mecanicamente.
            
            Antes de escolher cada rajada, aplica estas prioridades:
            1. Excluir posicoes ja utilizadas.
            2. Excluir casas que os resultados anteriores permitam confirmar como agua.
            3. Investigar navios atingidos que ainda nao foram afundados.
            4. So depois procurar novos navios.
            
            Exemplo:
            Se disparares em E1, E2 e E3 e o resultado indicar tres tiros validos,
            zero tiros na agua e uma Nau afundada, sem acertos noutros navios,
            essas tres casas pertencem a Nau.
            As casas adjacentes, incluindo F1, F2 e F3, sao agua.
            Nao desperdices a proxima rajada nessas casas.
            
            Se um grupo de tres casas consecutivas incluir posicoes ja utilizadas
            ou confirmadas como agua, abandona esse padrao e escolhe outras casas.
            
            
            Enquanto existirem navios maiores que uma Barca, da prioridade
            a zonas onde esses navios ainda possam caber.
            Nao uses exclusivamente um padrao de xadrez: as Barcas ocupam
            apenas uma casa e podem estar nas casas ignoradas por esse padrao.
            
            3. PERSEGUICAO
            Quando uma rajada indicar acertos sem afundar o navio, da prioridade
            a investigar a zona dessa rajada.
            Explora casas imediatamente a Norte, Sul, Este e Oeste das
            possiveis posicoes atingidas.
            Procura completar uma linha horizontal ou vertical para Caravelas,
            Naus e Fragatas. Considera tambem a forma em T do Galeao.
            Se existirem varias explicacoes possiveis, escolhe tiros que ajudem
            a distinguir essas explicacoes, sem assumir uma delas como certa.
            
            4. INTERPRETACAO DOS RESULTADOS
            Se os tres tiros forem validos e todos forem agua, as tres casas
            ficam confirmadas como agua.
            Se houver uma mistura de agua e acertos, conserva a incerteza
            sobre o resultado de cada casa.
            Se um navio afundar, nao assumas que os restantes acertos da
            rajada pertencem necessariamente a esse mesmo navio.
            
            5. NAVIOS AFUNDADOS
            So excluas as casas em redor de um navio afundado quando conseguires
            determinar todas as suas posicoes com seguranca.
            Nenhum outro navio pode tocar-lhe, nem na diagonal.
            Nao excluas automaticamente diagonais de possiveis acertos:
            essas casas podem pertencer ao proprio Galeao em T.
            
            6. FASE FINAL
            Quando so restarem Barcas, procura nas casas ainda disponiveis
            que nao estejam confirmadas como agua.
            Nao continues a perseguir um navio que ja esta confirmado como afundado.
            Se nao conseguires justificar uma escolha estrategica, escolhe
            tres casas ainda nao utilizadas em vez de repetir tiros.
            """;

    /**
     * Envia uma mensagem ao chatbot e retorna a sua resposta.
     * Se o envio falhar, tenta novamente ate um maximo de 5 tentativas,
     * esperando 2 segundos entre cada tentativa
     *
     * @param chat O chatbot utilizado na conversa
     * @param message A mensagem a enviar ao chatbot
     * @return A resposta do chatbot
     * @throws IOException Se nao for possivel obter uma resposta apos 5 tentativas
     * @throws InterruptedException Se a espera entre tentativas for interrompida
     */
    private static GenerateContentResponse sendWithRetry(
            Chat chat, String message)
            throws IOException, InterruptedException {

        for (int attempt = 1; attempt <= 5; attempt++) {
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

                Thread.sleep(2000);
            }
        }

        throw new IOException("Não foi possível contactar a API.");
    }

    /**
     * Simula um jogo em que o Gemini dispara rajadas contra a frota do jogo.
     * Guarda as rajadas e os seus resultados e envia as posicoes ainda nao
     * utilizadas em cada pedido. A cada 10 rajadas, envia tambem as regras
     * e o historico completo para relembrar o chatbot do estado do jogo.
     * A simulacao termina quando todos os navios estiverem afundados
     *
     * @param game O jogo que contem a frota contra a qual o Gemini vai jogar
     * @param apiKey A chave utilizada para aceder a API do Gemini
     * @throws IOException Se ocorrer um erro ao processar o JSON, se nao for
     *                     possivel contactar a API ou se a simulacao for interrompida
     */
    public static void simula(IGame game, String apiKey, ScoreboardManager scoreboardManager) throws IOException {
        try (Client client = Client.builder().apiKey(apiKey).build()) {
            Chat chat = client.chats.create("gemini-3.5-flash-lite");
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

            game.printMyBoard(true, true);

            //  regras e todas as posições disponíveis.
            var inicio = mapper.createObjectNode();
            inicio.put("regras", PROMPT);
            inicio.set(
                    "posicoesAindaNaoAtiradas",
                    mapper.valueToTree(posicoesDisponiveis));
            inicio.put("instrucao", "Começa agora com a primeira rajada.");

            GenerateContentResponse response = sendWithRetry(
                    chat, mapper.writeValueAsString(inicio));

            while (game.getRemainingShips() > 0) {
                System.out.println(response.text());

                // Converter o JSON do Gemini em posições do jogo.
                JsonNode tirosJson = mapper.readTree(response.text());
                List<IPosition> shots = new ArrayList<>();

                for (JsonNode shot : tirosJson) {
                    shots.add(new Position(
                            shot.get("row").asText().charAt(0),
                            shot.get("column").asInt()));
                }

                // Executar a rajada e obter o resultado.
                game.fireShots(shots);

                IMove move = game.getAlienMoves().getLast();
                String result = move.processEnemyFire(false);
                numeroRajada++;

                // Guardar a rajada e o resultado real.
                var registo = mapper.createObjectNode();
                registo.put("rajada", numeroRajada);
                registo.set("tiros", tirosJson);
                registo.set("resultado", mapper.readTree(result));
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

                if (game.getRemainingShips() > 0) {
                    Thread.sleep(5000);

                    // lembretes com posicoes disponiveis
                    var pedido = mapper.createObjectNode();
                    pedido.put("rajadasRealizadas", numeroRajada);
                    pedido.put("naviosRestantes", game.getRemainingShips());
                    pedido.set("resultado", mapper.readTree(result));

                    // A cada 10 rajadas, relembrar também as regras e o histórico.
                    if (numeroRajada % 10 == 0) {
                        chat = client.chats.create("gemini-3.5-flash-lite");

                        pedido.put("regras", PROMPT);

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

                    response = sendWithRetry(
                            chat, mapper.writeValueAsString(pedido));
                }
            }

            System.out.println(
                    "Fim da simulação: toda a frota foi afundada.");

            game.over();
            scoreboardManager.saveGame(game.getAlienMoves().size(), "Frota Afundada! (Simulação AI)");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("A simulação foi interrompida.", e);
        }
    }
}