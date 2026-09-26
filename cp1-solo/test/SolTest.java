import java.util.List;

public class SolTest {
	private static int passed = 0;
	private static int failed = 0;

	public static void main(String[] args) {
		testIdentifier();
		testKeyword();
		testString();
		testChar();
		testNumber();
		testOperator();
		testUnterminatedStringEof();
		testUnterminatedStringNewline();
		testInvalidCharacter();
		testRealisticCode();

		System.out.println();
		System.out.println("Passed: " + passed);
		System.out.println("Failed: " + failed);

		if (failed > 0) System.exit(1);
	}

	private static List<Token> scan(String source) {
		return new Scanner(source).scanTokens();
	}

	private static void check(String label, boolean condition) {
		if (condition) {
			passed++;
			System.out.println("OK   " + label);
		} else {
			failed++;
			System.out.println("FAIL " + label);
		}
	}

	private static void checkType(String label, Token token, TokenType type) {
		check(label + " -> " + token.lexeme, token.type == type);
	}

	private static void testIdentifier() {
		List<Token> tokens = scan("total x1 contaItens _nome");
		check("identificador: 4 tokens + EOF", tokens.size() == 5);
		checkType("identificador total", tokens.get(0), TokenType.IDENTIFIER);
		checkType("identificador x1", tokens.get(1), TokenType.IDENTIFIER);
		checkType("identificador contaItens", tokens.get(2), TokenType.IDENTIFIER);
		checkType("identificador _nome", tokens.get(3), TokenType.IDENTIFIER);
		check("identificador: termina em EOF", tokens.get(4).type == TokenType.EOF);
	}

	private static void testKeyword() {
		List<Token> tokens = scan("if fn var int return end");
		checkType("palavra reservada if", tokens.get(0), TokenType.IF);
		checkType("palavra reservada fn", tokens.get(1), TokenType.FN);
		checkType("palavra reservada var", tokens.get(2), TokenType.VAR);
		checkType("palavra reservada int", tokens.get(3), TokenType.INT);
		checkType("palavra reservada return", tokens.get(4), TokenType.RETURN);
		checkType("palavra reservada end", tokens.get(5), TokenType.END);

		List<Token> id = scan("ifelse");
		checkType("ifelse e identificador, nao IF", id.get(0), TokenType.IDENTIFIER);
	}

	private static void testString() {
		List<Token> tokens = scan("\"hello\"");
		check("string: 1 token + EOF", tokens.size() == 2);
		checkType("string hello", tokens.get(0), TokenType.STRING_LITERAL);
		check("string hello: literal", "hello".equals(tokens.get(0).literal));

		List<Token> empty = scan("\"\"");
		checkType("string vazia", empty.get(0), TokenType.STRING_LITERAL);
		check("string vazia: literal", "".equals(empty.get(0).literal));

		List<Token> esc = scan("\"a\\nb\"");
		checkType("string com escape", esc.get(0), TokenType.STRING_LITERAL);
		check("string com escape: \\n interpretado", "a\nb".equals(esc.get(0).literal));
	}

	private static void testChar() {
		List<Token> tokens = scan("'a'");
		checkType("char a", tokens.get(0), TokenType.CHAR_LITERAL);
		check("char a: literal", Character.valueOf('a').equals(tokens.get(0).literal));

		List<Token> esc = scan("'\\n'");
		checkType("char \\n", esc.get(0), TokenType.CHAR_LITERAL);
		check("char \\n: literal", Character.valueOf('\n').equals(esc.get(0).literal));
	}

	private static void testNumber() {
		List<Token> tokens = scan("10 3.14 3.14f 1_000");
		checkType("inteiro 10", tokens.get(0), TokenType.INTEGER_LITERAL);
		check("inteiro 10: valor", Integer.valueOf(10).equals(tokens.get(0).literal));
		checkType("real 3.14", tokens.get(1), TokenType.REAL_LITERAL);
		check("real 3.14: double", Double.valueOf(3.14).equals(tokens.get(1).literal));
		checkType("real 3.14f", tokens.get(2), TokenType.REAL_LITERAL);
		check("real 3.14f: float", Float.valueOf(3.14f).equals(tokens.get(2).literal));
		checkType("inteiro 1_000", tokens.get(3), TokenType.INTEGER_LITERAL);
		check("inteiro 1_000: valor 1000", Integer.valueOf(1000).equals(tokens.get(3).literal));
	}

	private static void testOperator() {
		List<Token> tokens = scan("= == < <= > >= != + - * / %");
		checkType("operador =", tokens.get(0), TokenType.EQUAL);
		checkType("operador ==", tokens.get(1), TokenType.EQUAL_EQUAL);
		checkType("operador <", tokens.get(2), TokenType.LESS);
		checkType("operador <=", tokens.get(3), TokenType.LESS_EQUAL);
		checkType("operador >", tokens.get(4), TokenType.GREATER);
		checkType("operador >=", tokens.get(5), TokenType.GREATER_EQUAL);
		checkType("operador !=", tokens.get(6), TokenType.BANG_EQUAL);
		checkType("operador +", tokens.get(7), TokenType.PLUS);
		checkType("operador -", tokens.get(8), TokenType.MINUS);
		checkType("operador *", tokens.get(9), TokenType.STAR);
		checkType("operador /", tokens.get(10), TokenType.SLASH);
		checkType("operador %", tokens.get(11), TokenType.PERCENT);
	}

	private static void testUnterminatedStringEof() {
		Sol.hadError = false;
		List<Token> tokens = scan("\"abc");
		check("erro: string ate EOF reporta erro", Sol.hadError);
		check("erro: string ate EOF nao trava", tokens.size() >= 1);
	}

	private static void testUnterminatedStringNewline() {
		Sol.hadError = false;
		List<Token> tokens = scan("\"abc\nx = 1");
		check("erro: string ate fim de linha reporta erro", Sol.hadError);
		check("erro: string ate fim de linha continua tokenizando", tokens.size() >= 3);
	}

	private static void testInvalidCharacter() {
		Sol.hadError = false;
		List<Token> tokens = scan("x @ 1");
		check("erro: caractere fora do alfabeto reporta erro", Sol.hadError);
		check("erro: caractere fora do alfabeto continua tokenizando", tokens.size() >= 3);
	}

	private static void testRealisticCode() {
		List<Token> tokens = scan("fn soma(a, b) return a + b end # comentario\nvar x = 1_000\n");
		checkType("realista: fn", tokens.get(0), TokenType.FN);
		checkType("realista: soma", tokens.get(1), TokenType.IDENTIFIER);
		checkType("realista: (", tokens.get(2), TokenType.LEFT_PAREN);
		checkType("realista: a", tokens.get(3), TokenType.IDENTIFIER);
		checkType("realista: ,", tokens.get(4), TokenType.COMMA);
		checkType("realista: b", tokens.get(5), TokenType.IDENTIFIER);
		checkType("realista: )", tokens.get(6), TokenType.RIGHT_PAREN);
		checkType("realista: return", tokens.get(7), TokenType.RETURN);
		checkType("realista: a", tokens.get(8), TokenType.IDENTIFIER);
		checkType("realista: +", tokens.get(9), TokenType.PLUS);
		checkType("realista: b", tokens.get(10), TokenType.IDENTIFIER);
		checkType("realista: end", tokens.get(11), TokenType.END);
		checkType("realista: var", tokens.get(12), TokenType.VAR);
		checkType("realista: x", tokens.get(13), TokenType.IDENTIFIER);
		checkType("realista: =", tokens.get(14), TokenType.EQUAL);
		checkType("realista: 1_000", tokens.get(15), TokenType.INTEGER_LITERAL);
		checkType("realista: EOF", tokens.get(16), TokenType.EOF);
		check("realista: total de tokens", tokens.size() == 17);
	}
}
