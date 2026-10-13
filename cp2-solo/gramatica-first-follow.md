# Gramática da Linguagem Sol — Checkpoint 2

## 1. Escopo

Esta gramática cobre a linguagem **Sol completa**, coerente com a especificação léxica do CP1 (mesmos tokens e palavras reservadas, sem alteração do scanner). Ela está:

- livre de recursão à esquerda (direta e indireta);
- fatorada à esquerda onde havia prefixo comum;
- verificada como LL(1) via FIRST/FOLLOW, com as três exceções inerentes ao design sem terminador documentadas na Seção 6.

O parser recursivo descendente (reconhecedor, sem AST) traduz cada não-terminal em um método, termo a termo.

## 2. Convenções

- Terminais como `fn`, `var`, `=`, `;` representam os lexemas/operadores; `IDENTIFIER`, `INTEGER_LITERAL`, `REAL_LITERAL`, `CHAR_LITERAL` e `STRING_LITERAL` são os tipos de token do CP1.
- `ε` é a produção vazia; `|` separa alternativas; `*` zero ou mais; `?` zero ou um; `( )` agrupa.
- As palavras `true`, `false`, `nil` e os tipos (`int`, `float`, `double`, `bool`, `char`, `string`, `array`, `table`, `integer`) são palavras reservadas. Os tipos **não aparecem na gramática** porque Sol não tem anotação de tipo — o tipo vem do literal (`var x = 10`).

## 3. Gramática final (EBNF)

```
program          → declaration* EOF

declaration      → function_declaration
                 | variable_declaration
                 | statement

function_declaration → "fn" IDENTIFIER "(" parameters? ")" block

parameters       → IDENTIFIER ( "," IDENTIFIER )*

variable_declaration → ( "var" | "let" ) IDENTIFIER "=" expression

statement        → expression_statement
                 | if_statement
                 | for_statement
                 | return_statement

expression_statement → expression

if_statement     → "if" expression body elif_clause* else_clause? "end"

elif_clause      → "elif" expression body

else_clause      → "else" body

for_statement    → "for" expression ";" expression ";" expression block

return_statement → "return" expression?

block            → declaration* "end"

body             → declaration*

expression       → assignment

assignment       → logic_or ( "=" assignment )?

logic_or         → logic_and ( "or" logic_and )*

logic_and        → equality ( "and" equality )*

equality         → comparison ( ( "==" | "!=" ) comparison )*

comparison       → term ( ( ">" | ">=" | "<" | "<=" ) term )*

term             → factor ( ( "+" | "-" ) factor )*

factor           → unary ( ( "*" | "/" | "%" ) unary )*

unary            → ( "-" | "not" ) unary
                 | postfix

postfix          → primary ( "(" arguments? ")" | "[" expression "]" )*

arguments        → expression ( "," expression )*

primary          → "true" | "false" | "nil"
                 | INTEGER_LITERAL | REAL_LITERAL | CHAR_LITERAL | STRING_LITERAL
                 | IDENTIFIER
                 | "(" expression ")"
                 | array_literal
                 | table_literal

array_literal    → "[" ( expression ( "," expression )* )? "]"

table_literal    → "{" ( table_entry ( "," table_entry )* )? "}"

table_entry      → expression ":" expression
```

## 4. Transformações aplicadas

### 4.1 Remoção de recursão à esquerda

A forma ingênua das expressões binárias seria recursiva à esquerda:

```
expression → expression "+" term | expression "-" term | term
term       → term "*" factor | term "/" factor | term "%" factor | factor
```

Foi convertida para a forma iterativa (não-recursiva à esquerda):

```
term   → factor ( ( "+" | "-" ) factor )*
factor → unary ( ( "*" | "/" | "%" ) unary )*
```

O mesmo padrão se aplica a `comparison`, `equality`, `logic_and` e `logic_or`. A associatividade à esquerda é preservada pelo laço.

### 4.2 Fatoração à esquerda (atribuição)

A versão natural da atribuição restringindo o alvo a identificador teria prefixo comum:

```
assignment → IDENTIFIER "=" assignment | logic_or      // não LL(1): IDENTIFIER inicia as duas
```

Foi fatorada para:

```
assignment → logic_or ( "=" assignment )?
```

O lado esquerdo é, sintaticamente, qualquer `logic_or`. A restrição de alvo válido (identificador ou `array[i]` / `table["chave"]`) é uma verificação **semântica**, adiada para a fase de AST (CP3) — este checkpoint não constrói AST. A associatividade à direita de `=` é preservada (`x = y = z`).

## 5. Tabela FIRST / FOLLOW

Abreviações:

- `EXPR` = `{ -, not, IDENTIFIER, INTEGER_LITERAL, REAL_LITERAL, CHAR_LITERAL, STRING_LITERAL, true, false, nil, (, [, { }`
- `DECL` = `{ fn, var, let, if, for, return } ∪ EXPR`
- `F_EXPR` = `{ EOF, end, else, elif, fn, var, let, if, for, return, ;, ], ,, ), :, } } ∪ EXPR`  (FOLLOW de `expression`)

| Não-terminal | FIRST | FOLLOW |
| --- | --- | --- |
| `program` | `DECL ∪ { EOF }` | `{ EOF }` |
| `declaration` | `DECL` | `{ EOF, end } ∪ DECL` |
| `function_declaration` | `{ fn }` | `{ EOF, end } ∪ DECL` |
| `parameters` | `{ IDENTIFIER }` | `{ ) }` |
| `variable_declaration` | `{ var, let }` | `{ EOF, end } ∪ DECL` |
| `statement` | `{ if, for, return } ∪ EXPR` | `{ EOF, end } ∪ DECL` |
| `expression_statement` | `EXPR` | `{ EOF, end } ∪ DECL` |
| `if_statement` | `{ if }` | `{ EOF, end } ∪ DECL` |
| `elif_clause` | `{ elif }` | `{ elif, else, end }` |
| `else_clause` | `{ else }` | `{ end }` |
| `for_statement` | `{ for }` | `{ EOF, end } ∪ DECL` |
| `return_statement` | `{ return }` | `{ EOF, end } ∪ DECL` |
| `block` | `DECL ∪ { end }` | `{ EOF, end } ∪ DECL` |
| `body` | `DECL ∪ { ε }` | `{ elif, else, end }` |
| `expression` | `EXPR` | `F_EXPR` |
| `assignment` | `EXPR` | `F_EXPR` |
| `logic_or` | `EXPR` | `{ = } ∪ F_EXPR` |
| `logic_and` | `EXPR` | `{ or, = } ∪ F_EXPR` |
| `equality` | `EXPR` | `{ and, or, = } ∪ F_EXPR` |
| `comparison` | `EXPR` | `{ ==, !=, and, or, = } ∪ F_EXPR` |
| `term` | `EXPR` | `{ >, >=, <, <=, ==, !=, and, or, = } ∪ F_EXPR` |
| `factor` | `EXPR` | `{ +, -, >, >=, <, <=, ==, !=, and, or, = } ∪ F_EXPR` |
| `unary` | `EXPR` | `{ *, /, %, +, -, >, >=, <, <=, ==, !=, and, or, = } ∪ F_EXPR` |
| `postfix` | `EXPR \ { -, not }` | `{ *, /, %, +, -, >, >=, <, <=, ==, !=, and, or, = } ∪ F_EXPR` |
| `arguments` | `EXPR` | `{ ) }` |
| `primary` | `EXPR \ { -, not }` | `{ (, [, *, /, %, +, -, >, >=, <, <=, ==, !=, and, or, = } ∪ F_EXPR` |
| `array_literal` | `{ [ }` | `{ (, [, *, /, %, +, -, >, >=, <, <=, ==, !=, and, or, = } ∪ F_EXPR` |
| `table_literal` | `{ { }` | `{ (, [, *, /, %, +, -, >, >=, <, <=, ==, !=, and, or, = } ∪ F_EXPR` |
| `table_entry` | `EXPR` | `{ , , } }` |

## 6. Verificação LL(1)

Para cada não-terminal com alternativas ou produção anulável, verificam-se: **(a)** FIRST das alternativas disjuntos; **(b)** no máximo uma alternativa anulável; **(c)** se há alternativa anulável, o FIRST das demais é disjunto do FOLLOW do não-terminal.

### 6.1 Alternativas — todas conformes

- `declaration`: `{ fn }`, `{ var, let }`, `{ if, for, return } ∪ EXPR` — disjuntos. ✔
- `statement`: `{ if }`, `{ for }`, `{ return }`, `EXPR` — disjuntos. ✔
- `unary`: `{ -, not }` vs `EXPR \ { -, not }` — disjuntos. ✔
- `primary`: `true`/`false`/`nil`/literais/`IDENTIFIER`/`(`/`[`/`{` — todos disjuntos. ✔
- `( var | let )`: `{ var }` vs `{ let }` — disjuntos. ✔

### 6.2 Produções anuláveis — conformes

- `parameters?`, `else_clause?`, `arguments?`, `array_literal` (interior), `table_literal` (interior): a alternativa vazia só é tomada quando o lookahead está no FOLLOW e é disjunta do FIRST da alternativa não-vazia. ✔
- `assignment → logic_or ( "=" assignment )?`: FIRST do sufixo é `{ = }`, e `=` ∉ FOLLOW(assignment). ✔
- `logic_or`/`logic_and`/`equality`/`comparison`/`factor`: os operadores de continuação (`or`, `and`, `==`/`!=`, `>`/`>=`/`<`/`<=`, `*`/`/`/`%`) não pertencem ao FOLLOW do respectivo não-terminal. ✔
- `body → declaration*`: FIRST(declaration) ∩ `{ elif, else, end }` = ∅. ✔
- `block → declaration* "end"`: FIRST(declaration) ∩ `{ end }` = ∅. ✔

### 6.3 Exceções — resolução por maximal munch

A Sol não tem terminador de instrução (quebra de linha é espaço). Por isso, tokens que iniciam uma expressão também aparecem no FOLLOW de `expression` (uma declaração pode seguir outra), criando três sobreposições formais. Todas têm a mesma causa e a mesma resolução: o parser é **guloso (maximal munch)** — consome a expressão sempre que o lookahead permitir —, o que é determinístico e coincide com a intenção da spec.

1. **`term → factor ( ( "+" | "-" ) factor )*`** — `-` ∈ FIRST do sufixo e `-` ∈ FOLLOW(term) (via `-` unário em EXPR). O `-` é sempre consumido como binário (`a - b`), nunca como início de nova instrução.
2. **`postfix → primary ( "(" … | "[" … )*`** — `(` e `[` ∈ FIRST do sufixo e ∈ FOLLOW(postfix). São sempre consumidos como chamada/indexação (`x(1)`, `a[0]`), nunca como nova expressão.
3. **`return_statement → "return" expression?`** — FIRST(expression) ∩ FOLLOW(return_statement) ≠ ∅. A expressão é lida **se e somente se** o lookahead ∈ FIRST(expression); caso contrário é `return` vazio. Logo, `return` sem valor só é válido imediatamente antes de `end`/`else`/`elif`/EOF ou de uma palavra de declaração (`fn`, `var`, `let`, `if`, `for`, `return`).

Essas três são documentadas como desvio deliberado do LL(1) estrito, decorrente do design sem terminador da linguagem, e não como erro de fatoração. O reconhecedor resultante é determinístico e livre de recursão infinita.
