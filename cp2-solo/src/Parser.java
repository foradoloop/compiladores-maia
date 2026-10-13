// Parser recursivo descendente baseado no livro Crafting Interpreters (Robert Nystrom, 2021).
// Reconhecedor: só aceita ou rejeita um programa, sem construir AST.
import java.util.List;

public class Parser {
	private final List<Token> tokens;
	private int current = 0;

	Parser(List<Token> tokens) {
		this.tokens = tokens;
	}

	// Analisa o programa inteiro. Devolve true se nenhum erro sintatico foi encontrado.
	public boolean parse() {
		while (!isAtEnd()) {
			declaration();
		}
		return !Sol.hadError;
	}

	// --- declaracoes ---

	// declaration -> function_declaration | variable_declaration | statement
	private void declaration() {
		try {
			if (match(TokenType.FN)) {
				functionDeclaration();
			} else if (match(TokenType.VAR, TokenType.LET)) {
				variableDeclaration();
			} else {
				statement();
			}
		} catch (ParseError error) {
			synchronize();
		}
	}

	// function_declaration -> "fn" IDENTIFIER "(" parameters? ")" block
	private void functionDeclaration() {
		consume(TokenType.IDENTIFIER, "Expected function name.");
		consume(TokenType.LEFT_PAREN, "Expected '(' after function name.");
		if (!check(TokenType.RIGHT_PAREN)) {
			parameters();
		}
		consume(TokenType.RIGHT_PAREN, "Expected ')' after parameters.");
		block();
	}

	// parameters -> IDENTIFIER ( "," IDENTIFIER )*
	private void parameters() {
		do {
			consume(TokenType.IDENTIFIER, "Expected parameter name.");
		} while (match(TokenType.COMMA));
	}

	// variable_declaration -> ( "var" | "let" ) IDENTIFIER "=" expression
	private void variableDeclaration() {
		consume(TokenType.IDENTIFIER, "Expected variable name.");
		consume(TokenType.EQUAL, "Expected '=' after variable name.");
		expression();
	}

	// statement -> expression_statement | if_statement | for_statement | return_statement
	private void statement() {
		try {
			if (match(TokenType.IF)) {
				ifStatement();
			} else if (match(TokenType.FOR)) {
				forStatement();
			} else if (match(TokenType.RETURN)) {
				returnStatement();
			} else {
				expressionStatement();
			}
		} catch (ParseError error) {
			synchronize();
		}
	}

	// expression_statement -> expression
	private void expressionStatement() {
		expression();
	}

	// if_statement -> "if" expression body elif_clause* else_clause? "end"
	private void ifStatement() {
		expression();
		body();
		while (match(TokenType.ELIF)) {
			expression();
			body();
		}
		if (match(TokenType.ELSE)) {
			body();
		}
		consume(TokenType.END, "Expected 'end' after if statement.");
	}

	// for_statement -> "for" expression ";" expression ";" expression block
	private void forStatement() {
		expression();
		consume(TokenType.SEMICOLON, "Expected ';' after for initializer.");
		expression();
		consume(TokenType.SEMICOLON, "Expected ';' after for condition.");
		expression();
		block();
	}

	// return_statement -> "return" expression?
	// Resolucao gulosa: a expressao so e lida quando o lookahead esta em FIRST(expression).
	private void returnStatement() {
		if (isExpressionStart(peek())) {
			expression();
		}
	}

	// block -> declaration* "end"
	private void block() {
		while (!check(TokenType.END) && !isAtEnd()) {
			declaration();
		}
		consume(TokenType.END, "Expected 'end' after block.");
	}

	// body -> declaration*  (termina em end/else/elif/EOF)
	private void body() {
		while (!check(TokenType.END) && !check(TokenType.ELSE) && !check(TokenType.ELIF) && !isAtEnd()) {
			declaration();
		}
	}

	// --- expressoes ---

	// expression -> assignment
	private void expression() {
		assignment();
	}

	// assignment -> logic_or ( "=" assignment )?
	private void assignment() {
		logicOr();
		if (match(TokenType.EQUAL)) {
			assignment();
		}
	}

	// logic_or -> logic_and ( "or" logic_and )*
	private void logicOr() {
		logicAnd();
		while (match(TokenType.OR)) {
			logicAnd();
		}
	}

	// logic_and -> equality ( "and" equality )*
	private void logicAnd() {
		equality();
		while (match(TokenType.AND)) {
			equality();
		}
	}

	// equality -> comparison ( ( "==" | "!=" ) comparison )*
	private void equality() {
		comparison();
		while (match(TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL)) {
			comparison();
		}
	}

	// comparison -> term ( ( ">" | ">=" | "<" | "<=" ) term )*
	private void comparison() {
		term();
		while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
			term();
		}
	}

	// term -> factor ( ( "+" | "-" ) factor )*
	private void term() {
		factor();
		while (match(TokenType.PLUS, TokenType.MINUS)) {
			factor();
		}
	}

	// factor -> unary ( ( "*" | "/" | "%" ) unary )*
	private void factor() {
		unary();
		while (match(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
			unary();
		}
	}

	// unary -> ( "-" | "not" ) unary | postfix
	private void unary() {
		if (match(TokenType.MINUS, TokenType.NOT)) {
			unary();
		} else {
			postfix();
		}
	}

	// postfix -> primary ( "(" arguments? ")" | "[" expression "]" )*
	private void postfix() {
		primary();
		while (true) {
			if (match(TokenType.LEFT_PAREN)) {
				if (!check(TokenType.RIGHT_PAREN)) {
					arguments();
				}
				consume(TokenType.RIGHT_PAREN, "Expected ')' after arguments.");
			} else if (match(TokenType.LEFT_BRACKET)) {
				expression();
				consume(TokenType.RIGHT_BRACKET, "Expected ']' after index.");
			} else {
				break;
			}
		}
	}

	// arguments -> expression ( "," expression )*
	private void arguments() {
		do {
			expression();
		} while (match(TokenType.COMMA));
	}

	// primary -> "true" | "false" | "nil" | literais | IDENTIFIER | "(" expression ")" | array_literal | table_literal
	private void primary() {
		if (match(TokenType.TRUE, TokenType.FALSE, TokenType.NIL,
				TokenType.INTEGER_LITERAL, TokenType.REAL_LITERAL,
				TokenType.CHAR_LITERAL, TokenType.STRING_LITERAL,
				TokenType.IDENTIFIER)) {
			return;
		}
		if (match(TokenType.LEFT_PAREN)) {
			expression();
			consume(TokenType.RIGHT_PAREN, "Expected ')' after expression.");
			return;
		}
		if (match(TokenType.LEFT_BRACKET)) {
			arrayLiteral();
			return;
		}
		if (match(TokenType.LEFT_BRACE)) {
			tableLiteral();
			return;
		}
		throw error(peek(), "Expected expression.");
	}

	// array_literal -> "[" ( expression ( "," expression )* )? "]"
	private void arrayLiteral() {
		if (!check(TokenType.RIGHT_BRACKET)) {
			do {
				expression();
			} while (match(TokenType.COMMA));
		}
		consume(TokenType.RIGHT_BRACKET, "Expected ']' after array.");
	}

	// table_literal -> "{" ( table_entry ( "," table_entry )* )? "}"
	private void tableLiteral() {
		if (!check(TokenType.RIGHT_BRACE)) {
			do {
				tableEntry();
			} while (match(TokenType.COMMA));
		}
		consume(TokenType.RIGHT_BRACE, "Expected '}' after table.");
	}

	// table_entry -> expression ":" expression
	private void tableEntry() {
		expression();
		consume(TokenType.COLON, "Expected ':' between key and value.");
		expression();
	}

	// --- suporte ---

	// Indica se o token pode iniciar uma expressao (FIRST(expression)).
	private boolean isExpressionStart(Token token) {
		switch (token.type) {
			case MINUS: case NOT: case IDENTIFIER:
			case INTEGER_LITERAL: case REAL_LITERAL: case CHAR_LITERAL: case STRING_LITERAL:
			case TRUE: case FALSE: case NIL:
			case LEFT_PAREN: case LEFT_BRACKET: case LEFT_BRACE:
				return true;
			default:
				return false;
		}
	}

	private boolean match(TokenType... types) {
		for (TokenType type : types) {
			if (check(type)) {
				advance();
				return true;
			}
		}
		return false;
	}

	private boolean check(TokenType type) {
		if (isAtEnd()) return false;
		return peek().type == type;
	}

	private Token advance() {
		if (!isAtEnd()) this.current++;
		return previous();
	}

	private boolean isAtEnd() {
		return peek().type == TokenType.EOF;
	}

	private Token peek() {
		return this.tokens.get(this.current);
	}

	private Token previous() {
		return this.tokens.get(this.current - 1);
	}

	private Token consume(TokenType type, String message) {
		if (check(type)) return advance();
		throw error(peek(), message);
	}

	// Erro interno: reporta e sinaliza que o chamador deve sincronizar.
	private ParseError error(Token token, String message) {
		if (token.type == TokenType.EOF) {
			Sol.error(token.line, token.column, "At end: " + message);
		} else {
			Sol.error(token.line, token.column, "At '" + token.lexeme + "': " + message);
		}
		return new ParseError();
	}

	// Recuperacao em modo panico: descarta tokens ate uma fronteira de instrucao.
	private void synchronize() {
		advance();
		while (!isAtEnd()) {
			if (previous().type == TokenType.SEMICOLON) return;
			switch (peek().type) {
				case END: case ELSE: case ELIF:
				case FN: case VAR: case LET: case IF: case FOR: case RETURN:
					return;
				default:
					advance();
			}
		}
	}

	private static class ParseError extends RuntimeException {
	}
}
