# Especificação Léxica da Linguagem Sol

## Alfabeto de entrada

A sintaxe usa apenas caracteres ASCII. A única exceção são os literais string e char, que podem representar caracteres Unicode.

## Case sensitivity

A linguagem é case-sensitive. Identificadores e palavras reservadas seguem a mesma regra de maiúsculas e minúsculas, e todas as palavras reservadas são escritas em minúsculas.

## Espaços em branco e comentários

São espaços em branco o espaço, a tabulação, a quebra de linha e o retorno de carro. Todos são ignorados pelo scanner e servem apenas para separar tokens. A indentação e a quebra de linha não têm significado sintático.

Comentários começam com o caractere # e seguem até o fim da linha. A primeira versão da linguagem não possui comentários de bloco, portanto não existe comentário aninhado.

## Identificadores

Um identificador segue a expressão regular `[A-Za-z_][A-Za-z0-9_]*`. Ele começa com uma letra ou com _, seguido de zero ou mais letras, dígitos ou _. O caractere _ é permitido e não há limite de tamanho definido.

São exemplos válidos: total, x1, contaItens e _nome.

São exemplos inválidos: 1numero e 123, porque começam com dígito.

## Palavras reservadas

As palavras reservadas usam o mesmo padrão dos identificadores e são reconhecidas por busca em uma tabela depois da leitura completa do lexema. A lista fechada está no final deste documento.

## Literais numéricos

Um literal inteiro segue a expressão regular `[0-9]([0-9]|_[0-9])*`. Um literal real segue `[0-9]([0-9]|_[0-9])*\.[0-9]([0-9]|_[0-9])*f?`. O caractere _ funciona como separador visual entre dígitos e é ignorado na leitura do valor. O sufixo f, quando presente, indica um valor float; sem ele, o valor é double. A primeira versão não possui notação científica nem hexadecimal.

São exemplos válidos: 10, 1_000, 3.14 e 3.14f.

São exemplos inválidos: _100, 100_, 1__000, 3.f e 1e10.

## Literais string

Um literal string é delimitado por aspas duplas e pode ser vazio. São suportadas as sequências de escape `\n`, `\t`, `\\`, `\"` e `\'`. Uma string não pode ocupar mais de uma linha. Uma string que não fecha até o fim da linha ou até o fim do arquivo é um erro léxico.

## Literais char

Um literal char é delimitado por aspas simples e contém exatamente um caractere ou uma sequência de escape válida, que pode ser `\n`, `\t`, `\\` ou `\'`. Os casos '' e 'ab' são inválidos.

## Operadores

Os operadores aritméticos são +, -, *, / e %. O operador de atribuição é =. Os operadores de comparação são ==, !=, <, >, <= e >=. Os operadores lógicos são as palavras reservadas and, or e not.

Os operadores ==, !=, <= e >= têm dois caracteres e compartilham prefixo com operadores de um caractere.

## Delimitadores

Os delimitadores são ( ) [ ] { } , : e ;.

## Regra de desambiguação

O scanner aplica a regra de maximal munch, que consiste em consumir o maior prefixo válido possível antes de decidir o token. Por isso, diante de == o scanner produz EQUAL_EQUAL e não EQUAL EQUAL, e diante de != produz BANG_EQUAL e não BANG EQUAL. Palavras reservadas são distinguidas de identificadores depois da leitura completa do lexema: if produz IF, enquanto ifelse produz IDENTIFIER.

## Palavras reservadas

A lista fechada de palavras reservadas é:

and, or, not, fn, return, end, if, elif, else, for, let, var, true, false, nil, int, integer, float, double, bool, char, string, array e table.

print não é palavra reservada. É reconhecida como identificador e associada a um builtin pelo runtime.
