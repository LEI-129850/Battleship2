package battleship;

public class AIPrompts {

    private AIPrompts(){}
    /**
     * Contem as regras do jogo e as instrucoes sobre o formato JSON das rajadas
     */
    static final String PROMPT = """
                    Vais jogar Batalha Naval contra uma frota secreta gerada pelo programa.
                    O tabuleiro tem linhas A-J e colunas 1-10. Existem:
                    - 4 Barcas de 1 casa;
                    - 3 Caravelas de 2 casas;
                    - 2 Naus de 3 casas;
                    - 1 Fragata de 4 casas;
                    - 1 Galeao de 5 casas em T, com um corpo de 3 casas e duas asas
                      numa das extremidades.
           \s
                    Os navios retos podem ser horizontais ou verticais.
                    O Galeao tem quatro orientacoes.
                    Os navios nao se tocam, nem na diagonal.
           \s
                    FORMATO DA RESPOSTA
           \s
                    Responde APENAS com um array JSON de exatamente 3 tiros:
                    [{"row":"A","column":5},{"row":"C","column":10},{"row":"F","column":5}]
           \s
                    Nao uses Markdown, explicacoes, comentarios ou tabuleiros.
                    Cada objeto deve conter apenas row e column.
                    row deve ser uma letra maiuscula entre A e J.
                    column deve ser um numero inteiro entre 1 e 10.
           \s
                    INFORMACAO RECEBIDA
           \s
                    Receberas o resultado agregado da rajada, por exemplo:
                    {"validShots":3,"sunkBoats":[{"count":1,"type":"Barca"}],
                     "repeatedShots":0,"outsideShots":0,
                     "hitsOnBoats":[{"hits":1,"type":"Nau"}],"missedShots":1}
           \s
                    O resultado NAO identifica qual dos tiros acertou em cada navio.
                    A ordem dos elementos em hitsOnBoats e sunkBoats nao corresponde
                    a ordem dos tiros.
           \s
                    Receberas tambem posicoesAindaNaoAtiradas.
                    Esta lista indica casas ainda nao utilizadas, mas pode incluir
                    casas que ja consegues deduzir que sao agua.
           \s
                    Se receberes o historico, usa-o como registo das jogadas reais.
                    Se a conversa tiver sido reiniciada, continua a mesma partida:
                    nao recomeces a estrategia como se o tabuleiro estivesse por explorar.
           \s
                    REGRAS PARA ESCOLHER TIROS
           \s
                    1. Escolhe tres posicoes distintas de posicoesAindaNaoAtiradas.
                    Nunca repitas uma posicao para confirmar um acerto: disparar outra
                    vez na mesma casa nao fornece informacao nova.
           \s
                    2. Se a lista tiver apenas uma ou duas posicoes, utiliza TODAS
                    essas posicoes e completa os tres tiros com repeticoes.
                    Esta excecao depende do tamanho da lista, nao do numero de
                    casas que consideras interessantes.
           \s
                    3. Evita casas que estejam confirmadas como agua.
                    Se nao houver tres opcoes novas que consideres promissoras,
                    prefere outras casas ainda nao utilizadas a repetir tiros antigos.
           \s
                    4. Investiga navios atingidos que ainda nao foram afundados.
                    Explora vizinhos a Norte, Sul, Este e Oeste dos possiveis acertos.
                    Para Caravelas, Naus e Fragatas, considera linhas retas.
                    Para o Galeao, considera a forma em T.
           \s
                    5. Distingue certezas de hipoteses.
                    Uma rajada com um acerto e dois tiros na agua nao permite,
                    por si so, identificar qual das tres casas contem o navio.
                    Mantem as varias possibilidades e escolhe tiros que ajudem
                    a distingui-las.
           \s
                    6. Quando conheceres todas as casas de um navio afundado,
                    exclui as casas adjacentes a esse navio, incluindo diagonais.
                    Nao apliques esta exclusao em redor de uma localizacao incerta.
                    Nao excluas automaticamente diagonais de acertos num navio
                    ainda por identificar: podem pertencer ao proprio Galeao.
           \s
                    7. Se nao houver um navio por investigar, procura em zonas
                    pouco exploradas onde os navios restantes ainda possam caber.
                    Nao uses exclusivamente um padrao de xadrez, pois as Barcas
                    ocupam apenas uma casa.
           \s
                    8. Tres tiros proximos podem ajudar a investigar uma zona,
                    mas nao sao um objetivo por si so.
                    Nao percorras linhas ou colunas mecanicamente.
                    Abandona qualquer padrao que inclua tiros repetidos ou agua
                    confirmada, ou que te afaste de um navio por terminar.
           \s
                    EXEMPLOS DE ENTRADA E RESPOSTA
           \s
                    Os exemplos seguintes sao ficticios e independentes.
                    NAO fazem parte da partida real.
                    As listas identificadas como excertos mostram apenas algumas
                    das posicoes disponiveis; nao descrevem todo o tabuleiro.
                    As notas explicam os exemplos, mas nao devem aparecer nas tuas respostas.
                    As respostas apresentadas sao escolhas validas, nao necessariamente
                    as unicas escolhas possiveis.
           \s
                    EXEMPLO 1 — ABANDONAR UM PADRAO PARA NAO REPETIR TIROS
           \s
                    Entrada:
                    {
                      "posicoesJaJogadas":["H7","H8","H9"],
                      "posicoesAindaNaoAtiradas":["G7","G8","G9","I7","I8","I9"]
                    }
           \s
                    Nota:
                    Embora G7, H7 e I7 formem uma linha, H7 ja foi utilizado.
                    Escolhe tres casas da lista disponivel, sem repetir.
           \s
                    Resposta:
                    [{"row":"G","column":7},{"row":"G","column":8},{"row":"I","column":7}]
           \s
                    EXEMPLO 2 — NAO DISPARAR JUNTO DE UMA NAU AFUNDADA
           \s
                    Entrada:
                    {
                      "historico":[
                        {
                          "rajada":1,
                          "tiros":[
                            {"row":"E","column":1},
                            {"row":"E","column":2},
                            {"row":"E","column":3}
                          ],
                          "resultado":{
                            "validShots":3,
                            "sunkBoats":[{"count":1,"type":"Nau"}],
                            "repeatedShots":0,
                            "outsideShots":0,
                            "hitsOnBoats":[],
                            "missedShots":0
                          }
                        }
                      ],
                      "excertoPosicoesAindaNaoAtiradas":[
                        "D1","D2","D3","D4","E4","F1","F2","F3","F4",
                        "B6","B8","H7"
                      ]
                    }
           \s
                    Nota:
                    Nesta primeira rajada, os tres tiros afundaram uma Nau inteira.
                    E1, E2 e E3 sao as suas posicoes.
                    D1-D4, E4 e F1-F4 sao agua por estarem junto da Nau.
                    Nao escolhas F1, F2 e F3 apenas para continuar a percorrer o tabuleiro.
           \s
                    Resposta:
                    [{"row":"B","column":6},{"row":"B","column":8},{"row":"H","column":7}]
           \s
                    EXEMPLO 3 — INVESTIGAR UM ACERTO SEM INVENTAR A SUA POSICAO
           \s
                    Entrada:
                    {
                      "ultimaRajada":[
                        {"row":"E","column":2},
                        {"row":"E","column":5},
                        {"row":"E","column":8}
                      ],
                      "resultado":{
                        "validShots":3,
                        "sunkBoats":[],
                        "repeatedShots":0,
                        "outsideShots":0,
                        "hitsOnBoats":[{"hits":1,"type":"Nau"}],
                        "missedShots":2
                      },
                      "excertoPosicoesAindaNaoAtiradas":[
                        "D2","F2","D5","F5","D8","F8","A1","J10"
                      ]
                    }
           \s
                    Nota:
                    A Nau pode ter sido atingida em E2, E5 ou E8.
                    Nao ha informacao suficiente para escolher uma dessas casas como certeza.
                    Uma possibilidade e testar um vizinho de cada uma das tres candidatas.
                    Se estes tiros forem agua, isso nao elimina as orientacoes horizontais.
           \s
                    Resposta:
                    [{"row":"D","column":2},{"row":"D","column":5},{"row":"D","column":8}]
           \s
                    EXEMPLO 4 — NAO REPETIR SO POR ESTAR NO FIM DO JOGO
           \s
                    Entrada:
                    {
                      "naviosRestantes":1,
                      "posicoesAindaNaoAtiradas":["A4","A7","B1","J10"]
                    }
           \s
                    Nota:
                    Ainda existem quatro casas disponiveis.
                    Nao e permitido disparar tres vezes em J10.
                    Escolhe tres casas diferentes, mesmo que reste apenas um navio.
           \s
                    Resposta:
                    [{"row":"A","column":4},{"row":"B","column":1},{"row":"J","column":10}]
           \s
                    EXEMPLO 5 — COMPLETAR A RAJADA QUANDO SO RESTAM DUAS CASAS
           \s
                    Entrada:
                    {
                      "naviosRestantes":1,
                      "posicoesAindaNaoAtiradas":["B1","J10"]
                    }
           \s
                    Nota:
                    Agora so existem duas casas ainda nao utilizadas.
                    Dispara primeiro em ambas e usa uma repeticao para completar a rajada.
           \s
                    Resposta:
                    [{"row":"B","column":1},{"row":"J","column":10},{"row":"B","column":1}]
           \s
                    FIM DOS EXEMPLOS
           \s
                    Na partida real, usa exclusivamente os dados reais recebidos.
                    Antes de responder, verifica que escolheste exatamente tres tiros,
                    dentro do tabuleiro e sem repeticoes, salvo a excecao indicada.
                    A tua resposta deve conter apenas o array JSON da proxima rajada.             \s
           \s""";
}
