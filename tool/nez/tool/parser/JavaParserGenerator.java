package nez.tool.parser;

import nez.lang.Grammar;

public class JavaParserGenerator extends CommonParserGenerator {

	public JavaParserGenerator() {
		super(".java");
	}

	@Override
	protected void initLanguageSpec() {
		// this.UniqueNumberingSymbol = false;
		this.addType("$parse", "boolean");
		this.addType("$tag", "int");
		this.addType("$label", "int");
		this.addType("$table", "int");
		this.addType("$text", "byte[]");
		this.addType("$index", "byte[]");
		this.addType("$set", "boolean[]");
		this.addType("$string", "String[]");

		this.addType("memo", "int");
		this.addType(_set(), "boolean[]");
		this.addType(_index(), "byte[]");
		this.addType(_temp(), "boolean");
		this.addType(_pos(), "int");
		this.addType(_tree(), "T");
		this.addType(_log(), "int");
		this.addType(_table(), "int");
		this.addType(_state(), "ParserContext<T>");
	}

	@Override
	protected void generateHeader(Grammar g) {
                Statement("import java.io.IOException");
                Statement("import java.nio.file.Files");
                Statement("import java.nio.file.Path");
                Statement("import java.nio.file.Paths");
                Statement("import java.util.stream.Collectors");
                Statement("import java.util.stream.Stream");
                Line("");
		BeginDecl("public class " + _basename());
		{
			BeginDecl("private static <T> boolean start(ParserContext<T> c)");
			{
				Return(_funccall(_funcname(g.getStartProduction())));
			}
			EndDecl();
			importFileContent("java-parser-runtime.txt");
		}
	}

	private void generateFileReader(Grammar g) {
                Statement("Path path = Paths.get(a[0])");
                Statement("Stream<String> lines = Files.lines(path)");
                Statement("String data = lines.collect(Collectors.joining(\"\\n\"))");
                Statement("lines.close()");
	}

	@Override
	protected void generateFooter(Grammar g) {
		BeginDecl("public final static void main(String[] a) throws IOException");
		{
			if (code.getMemoPointSize() > 0) {
				VarDecl("int", "w", _int(strategy.SlidingWindow));
				VarDecl("int", "n", _int(code.getMemoPointSize()));
                                generateFileReader(g);
				Statement("SimpleTree t = parse(data, w, n)");
			} else {
                                generateFileReader(g);
				Statement("SimpleTree t = parse(data, 0, 0)");
			}

			Statement("System.out.println(t)");
		}
		EndDecl();

		EndDecl(); // end of class
		file.writeIndent("/*EOF*/");
	}

	@Override
	protected String _defun(String type, String name) {
		return "private static <T> " + type + " " + name;
	}

}
