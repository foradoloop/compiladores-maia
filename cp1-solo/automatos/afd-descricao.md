# Descrição dos autômatos

Os diagramas foram feitos no Finite State Machine Designer (https://madebyevan.com/fsm/) e estão nesta pasta em SVG. Em cada desenho, o nome do estado corresponde ao trecho de código do Scanner.java que ele representa, e o estado de borda dupla é o estado de aceitação. O estado erro corresponde às chamadas de Sol.error.

## Identificador e palavra reservada

O estado scanToken é o switch de scanToken(), que lê o primeiro caractere. A transição letra | _ é o caso em que o caractere é letra ou _ e o código chama identifier(). O estado identifier é o laço while (isAlphaNumeric(peek())) advance() de identifier(). Terminado o lexema, identifier() consulta o mapa keywords: se o lexema está no mapa, o token é uma palavra reservada (KEYWORD), senão é IDENTIFIER.

## String

O estado string é o laço while (hasNext() && peek() != '"') de string(). A transição barra invertida é o ramo if (peek() == '\\'), que leva ao estado escape, o método escape(). De escape, as sequências válidas voltam a string e as inválidas vão a erro. O estado STRING_LITERAL é o addToken(STRING_LITERAL, value). A transição quebra ou EOF é a quebra de linha dentro da string ou o fim de arquivo sem fechar.

## Char

O estado character é character(), o estado escape é escape() e o estado fecha é a verificação if (hasNext() && peek() == '\'') que exige a aspa de fechamento. O estado CHAR_LITERAL é o addToken(CHAR_LITERAL, value). Vazio, outro e quebra são os casos de erro.

## Número

O estado scanToken chama number() ao encontrar um dígito. INTEGER_LITERAL é o consumeNumberPart seguido de addToken(INTEGER_LITERAL). A transição digito | _ digito é o consumo de dígitos com o separador _. A transição ponto digito inicia a parte fracionária, que resulta em REAL_LITERAL double. O sufixo f leva a REAL_LITERAL float.

## Operadores

Para = ! < >, o estado peek representa a chamada match('='), que aplica maximal munch. Se o próximo caractere é =, o token é composto (COMPOSTO: EQUAL_EQUAL, BANG_EQUAL, LESS_EQUAL, GREATER_EQUAL); senão é simples (SIMPLES: EQUAL, BANG, LESS, GREATER). Para + - * / % o token sai direto (UNICO).

## Comentário e espaço em branco

O caractere # leva ao estado comentario, que é o laço while (peek() != '\n' && hasNext()) de scanToken. O estado descarta representa os casos de espaço, tabulação, retorno de carro e quebra de linha, que não produzem token.
