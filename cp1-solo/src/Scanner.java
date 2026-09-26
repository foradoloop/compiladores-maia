// Estrutura do scanner baseada no livro Crafting Interpreters (Robert Nystrom, 2021).
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {
	private final String source;
	private final List<Token> tokens = new ArrayList<>();
	private int start = 0;
	private int current = 0;
	private int line = 1;
	private int column = 1;
	private int startColumn = 1;

	private static final Map<String, TokenType> keywords;

	static {
		keywords = new HashMap<>();
		keywords.put("and", TokenType.AND);
		keywords.put("or", TokenType.OR);
		keywords.put("not", TokenType.NOT);
		keywords.put("fn", TokenType.FN);
		keywords.put("return", TokenType.RETURN);
		keywords.put("end", TokenType.END);
		keywords.put("if", TokenType.IF);
		keywords.put("elif", TokenType.ELIF);
		keywords.put("else", TokenType.ELSE);
		keywords.put("for", TokenType.FOR);
		keywords.put("let", TokenType.LET);
		keywords.put("var", TokenType.VAR);
		keywords.put("true", TokenType.TRUE);
		keywords.put("false", TokenType.FALSE);
		keywords.put("nil", TokenType.NIL);
		keywords.put("int", TokenType.INT);
		keywords.put("integer", TokenType.INTEGER);
		keywords.put("float", TokenType.FLOAT);
		keywords.put("double", TokenType.DOUBLE);
		keywords.put("bool", TokenType.BOOL);
		keywords.put("char", TokenType.CHAR);
		keywords.put("string", TokenType.STRING);
		keywords.put("array", TokenType.ARRAY);
		keywords.put("table", TokenType.TABLE);
	}

	Scanner(String source) {
		this.source = source;
	}

	public List<Token> scanTokens() {
		while (hasNext()) {
			this.start = this.current;
			this.startColumn = this.column;
			scanToken();
		}

		tokens.add(new Token(TokenType.EOF, "", null, this.line, this.column));
		return tokens;
	}

	private boolean hasNext() {
		return this.current < source.length();
	}

	private void scanToken() {
		char c = advance();
		switch (c) {
			case '(': addToken(TokenType.LEFT_PAREN); break;
			case ')': addToken(TokenType.RIGHT_PAREN); break;

			case '[': addToken(TokenType.LEFT_BRACKET); break;
			case ']': addToken(TokenType.RIGHT_BRACKET); break;

			case '{': addToken(TokenType.LEFT_BRACE); break;
			case '}': addToken(TokenType.RIGHT_BRACE); break;

			case ',': addToken(TokenType.COMMA); break;
			case ':': addToken(TokenType.COLON); break;
			case ';': addToken(TokenType.SEMICOLON); break;

			case '+': addToken(TokenType.PLUS); break;
			case '-': addToken(TokenType.MINUS); break;
			case '*': addToken(TokenType.STAR); break;
			case '/': addToken(TokenType.SLASH); break;
			case '%': addToken(TokenType.PERCENT); break;

			case '!':
				addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
				break;
			case '=':
				addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
				break;
			case '<':
				addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
				break;
			case '>':
				addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
				break;

			case '#':
				while (peek() != '\n' && hasNext()) {
					advance();
				}
				break;

			case ' ':
			case '\r':
			case '\t':
				break;

			case '\n':
				this.line++;
				this.column = 1;
				break;

			case '"': string(); break;
			case '\'': character(); break;

			default:
				if (isDigit(c)) {
					number();
				} else if (isAlpha(c)) {
					identifier();
				} else {
					Sol.error(line, startColumn, "Unexpected character");
				}
				break;
		}
	}

	private char advance() {
		this.current++;
		this.column++;
		return source.charAt(this.current - 1);
	}

	private void addToken(TokenType type) {
		addToken(type, null);
	}

	private void addToken(TokenType type, Object literal) {
		String text = source.substring(this.start, this.current);
		this.tokens.add(new Token(type, text, literal, this.line, this.startColumn));
	}

	private boolean match(char expected) {
		if (!hasNext()) return false;
		if (this.source.charAt(current) != expected) return false;

		this.current++;
		this.column++;
		return true;
	}

	private char peek() {
		if (!hasNext()) return '\0';
		return this.source.charAt(current);
	}

	private char peekNext() {
		if (this.current + 1 >= source.length()) return '\0';
		return this.source.charAt(this.current + 1);
	}

	private void string() {
		StringBuilder value = new StringBuilder();

		while (hasNext() && peek() != '"') {
			if (peek() == '\n') {
				Sol.error(line, startColumn, "Missing terminating \" character");
				return;
			}

			if (peek() == '\\') {
				advance();
				if (!hasNext()) {
					Sol.error(line, startColumn, "Unterminated string");
					return;
				}
				value.append(escape());
			} else {
				value.append(advance());
			}
		}

		if (!hasNext()) {
			Sol.error(line, startColumn, "Unterminated string");
			return;
		}

		advance();
		addToken(TokenType.STRING_LITERAL, value.toString());
	}

	private void character() {
		if (!hasNext()) {
			Sol.error(line, startColumn, "Unterminated character literal");
			return;
		}

		if (peek() == '\'') {
			Sol.error(line, startColumn, "Empty character literal");
			advance();
			return;
		}

		char value;
		if (peek() == '\\') {
			advance();
			if (!hasNext()) {
				Sol.error(line, startColumn, "Unterminated character literal");
				return;
			}
			value = escape();
		} else {
			value = advance();
		}

		if (hasNext() && peek() == '\'') {
			advance();
			addToken(TokenType.CHAR_LITERAL, value);
		} else {
			Sol.error(line, startColumn, "Character literal must contain exactly one character");
			while (hasNext() && peek() != '\'' && peek() != '\n') {
				advance();
			}
			if (hasNext() && peek() == '\'') {
				advance();
			}
		}
	}

	private char escape() {
		char c = advance();
		switch (c) {
			case 'n': return '\n';
			case 't': return '\t';
			case '\\': return '\\';
			case '\'': return '\'';
			case '"': return '"';
			default:
				Sol.error(line, startColumn, "Invalid escape sequence");
				return c;
		}
	}

	private void number() {
		consumeNumberPart();

		boolean isReal = false;
		if (peek() == '.' && isDigit(peekNext())) {
			isReal = true;
			advance();
			consumeNumberPart();
		}

		boolean hasSuffix = false;
		if (isReal && peek() == 'f') {
			hasSuffix = true;
			advance();
		}

		String text = source.substring(this.start, this.current);
		String clean = text.replace("_", "");

		if (isReal) {
			if (hasSuffix) {
				clean = clean.substring(0, clean.length() - 1);
				addToken(TokenType.REAL_LITERAL, Float.parseFloat(clean));
			} else {
				addToken(TokenType.REAL_LITERAL, Double.parseDouble(clean));
			}
		} else {
			addToken(TokenType.INTEGER_LITERAL, Integer.parseInt(clean));
		}
	}

	private void consumeNumberPart() {
		while (isDigit(peek()) || (peek() == '_' && isDigit(peekNext()))) {
			advance();
		}
	}

	private void identifier() {
		while (isAlphaNumeric(peek())) {
			advance();
		}

		String text = source.substring(this.start, this.current);
		TokenType type = keywords.get(text);
		if (type == null) type = TokenType.IDENTIFIER;

		addToken(type);
	}

	private boolean isDigit(char c) {
		return '0' <= c && c <= '9';
	}

	private boolean isAlpha(char c) {
		return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || c == '_';
	}

	private boolean isAlphaNumeric(char c) {
		return isAlpha(c) || isDigit(c);
	}
}
