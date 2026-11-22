| Local | Nome do Cheiro no Código | Nome da Refabricação | Número d@ Alun@ |
| :--- | :--- | :--- | :--- |
| `Game::randomEnemyFire` | Long Method | Extract Method | 129843 |
| `Game::getRemainingShips` | Unnecessary Local Variable | Inline Variable | 129843 |
| `Game::fireSingleShot` | Duplicated Expression | Introduce Variable | 129843 |
| `Game::radar` | Magic Number | Introduce Constant | 129843 |
| `Game::radar` | Poor Naming | Rename | 129843 |
| `Game::printBoard` | Inconsistent Naming | Change Signature | 129843 |
| `Frigate (construtor)` | Duplicated Code (CC 9, LOC 27; ramos NORTH/SOUTH e EAST/WEST iguais) | Extract Method | 129834 |
| `Frigate (método extraído)` | Duplicated Code (repetido em Caravel e Carrack) | Pull Members Up | 129834 |
| `Ship::initLinearPositions` | Duplicated Code (ciclos repetidos para NORTH/SOUTH e EAST/WEST) | Find Method Duplicates and Replace with Calls | 129834 |
| `Move::processEnemyFire` | Long Method (LOC 132, CC 38) | Replace Method With Method Object | 129834 |
| `Move::processEnemyFire` | Unnecessary Local Variable (variável jsonString retornada de imediato) | Inline Variable | 129834 |
| `Ship::getTopMostPos, getBottomMostPos, getLeftMostPos e getRightMostPos` | Duplicated Code (4 ciclos quase iguais, LOC 19) | Extract Method | 129834 |
| `Ship::Ship` | Long Parameter List (NOPM 4) | Introduce Parameter Object | 129834 |
| `Tasks::menu` | Nome enganador | Rename | 129850 |
| `Tasks::menu` | Magic Number | Introduce Constant | 129850 |
| `Tasks::readTimeBank` | Condição complexa | Introduce Variable | 129850 |
| `Tasks::menu` | Long Method e Switch Statements | Extract Method | 129850 |
| `Tasks::menu` | Long Method e Switch Statements | Extract Method | 129850 |
| `Tasks::menu` | Long Method e Duplicated Code | Extract Method | 129850 |
| `Tasks::readTimeBank` | God Class | Move Members | 129850 |
| `Fleet::colisionRisk` | Nome com erro ortográfico | Rename | 129830 |
| `Fleet::printShips (parâmetro ships)` | Nome que esconde o campo `ships` da classe | Rename | 129830 |
| `Fleet::getFloatingShips, getSunkShips e getShipsLike` | Duplicated Code (LOC 19, 19 e 21: mesmo ciclo, só muda o critério) | Extract Method | 129830 |
| `Fleet (método extraído)` | Duplicated Code (só muda a condição) | Introduce Functional Parameter | 129830 |
| `Fleet::printAllShips` | Lazy Method (só chama `printShips(ships)`) | Inline Method | 129830 |
| `AI::sendWithRetry e AI::simula` | Magic Numbers (`5` repetido 3 vezes, `2000`, `5000`, `10` e nome do modelo duplicado) | Introduce Constant | 129830 |
| `AI::simula (mapper.readTree(result) calculado 2 vezes)` | Duplicated Code | Introduce Variable | 129830 |
| `AI (constante PROMPT, cerca de 215 linhas)` | Large Class | Move Members | 129830 |
| `AI::simula (conversão do JSON em posições, linhas 321 a 328)` | Long Method | Extract Method | 129830 |
| `AI::simula` | Long Method (LOC 120, CC 10, 27 variáveis) | Replace Method With Method Object | 129830 |
