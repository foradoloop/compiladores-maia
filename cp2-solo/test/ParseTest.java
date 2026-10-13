import java.util.List;

public class ParseTest {
	private static int passed = 0;
	private static int failed = 0;

	public static void main(String[] args) {
		testValidFunction();
		testValidIfElse();
		testValidForArray();
		testValidTable();
		testValidTypes();

		testInvalidMissingEnd();
		testInvalidMissingParen();
		testInvalidMissingSemicolonFor();

		testMultipleErrors();

		System.out.println();
		System.out.println("Passed: " + passed);
		System.out.println("Failed: " + failed);

		if (failed > 0) System.exit(1);
	}

	// Escaneia e analisa a fonte, devolvendo o numero de erros reportados.
	private static int errors(String source) {
		Sol.hadError = false;
		Sol.errorCount = 0;
		List<Token> tokens = new Scanner(source).scanTokens();
		new Parser(tokens).parse();
		return Sol.errorCount;
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

	private static void testValidFunction() {
		check("valido: funcao com parametros e return", errors("fn soma(a, b) return a + b end") == 0);
	}

	private static void testValidIfElse() {
		check("valido: if/elif/else", errors("if idade >= 18 print(\"adulto\") elif idade >= 16 print(\"jovem\") else print(\"menor\") end") == 0);
	}

	private static void testValidForArray() {
		check("valido: for com array e indexacao", errors("var numeros = [10, 20, 30]\nfor i = 0; i < 3; i = i + 1\n    print(numeros[i])\nend") == 0);
	}

	private static void testValidTable() {
		check("valido: table com acesso por chave", errors("var pessoa = { \"nome\": \"Maria\", \"idade\": 22 }\nprint(pessoa[\"nome\"])") == 0);
	}

	private static void testValidTypes() {
		check("valido: literais bool/char/string/real/nil", errors("var a = true\nvar b = 'x'\nvar c = \"oi\"\nvar d = 3.14\nvar e = 0.5f\nvar f = nil") == 0);
	}

	private static void testInvalidMissingEnd() {
		check("invalido: 'end' ausente reporta erro", errors("fn f(a)\n    return a") >= 1);
	}

	private static void testInvalidMissingParen() {
		check("invalido: ')' ausente reporta erro", errors("var x = soma(10, 20") >= 1);
	}

	private static void testInvalidMissingSemicolonFor() {
		check("invalido: ';' ausente no for reporta erro", errors("for i = 0 i < 3; i = i + 1\n    print(i)\nend") >= 1);
	}

	private static void testMultipleErrors() {
		check("multiplos erros: reporta 2+ erros sem travar", errors("var a = 10\nvar = 20\nvar b = 30\nvar c = 40\nx = = 50") >= 2);
	}
}
