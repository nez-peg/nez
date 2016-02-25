package nez.tool.parser;

import nez.lang.Grammar;

public class JavaParserGenerator extends AbstractParserGenerator {

	@Override
	protected void initTypeMap() {
		this.UniqueNumberingSymbol = false;
		this.addType("$parse", "boolean");
		this.addType("$tag", "Symbol");
		this.addType("$label", "Symbol");
		this.addType("$table", "Symbol");
		this.addType("$text", "byte[]");
		this.addType("$index", "byte[]");
		this.addType("$set", "boolean[]");
		this.addType("$string", "String");

		this.addType("memo", "int");
		this.addType(_set(), "boolean[]");
		this.addType(_index(), "byte[]");
		this.addType(_unchoiced(), "boolean");
		this.addType(_pos(), "int");
		this.addType(_tree(), "Tree<?>");
		this.addType(_log(), "Object");
		this.addType(_table(), "int");
		this.addType(_state(), "ParserContext");
	}

	@Override
	protected String getFileExtension() {
		return "Parser.java";
	}

	@Override
	protected void generateHeader(Grammar g) {
		Statement("import nez.ast.Tree");
		Statement("import nez.ast.Symbol");
		Statement("import nez.ast.Source");
		Statement("import nez.ast.CommonTree");
		Statement("import nez.parser.io.StringSource");

		BeginDecl("public class Parser/**Change Here*/");
		BeginDecl("public final static Tree<?> parse(String text)");
		{
			VarDecl(_state(), "new ParserContext(text)");
			If(_funccall(_funcname(g.getStartProduction())));
			{
				Return(_Field(_state(), _tree()));
			}
			EndIf();
			Return(_Null());
		}
		EndDecl();
		BeginDecl("public final static int match(String text)");
		{
			VarDecl(_state(), "new ParserContext(text)");
			If(_funccall(_funcname(g.getStartProduction())));
			{
				Return(_cpos_());
			}
			EndIf();
			Return("-1");
		}
		EndDecl();
		ImportFile("/nez/tool/parser/ext/java-parser-runtime.txt");
	}

	@Override
	protected void generateFooter(Grammar g) {
		BeginDecl("public final static void main(String[] a)");
		{
			Statement("Tree<?> t = parse(a[0])");
			Statement("System.out.println(\"parsed:\" + t)");
		}
		EndDecl();

		EndDecl(); // end of class
		file.writeIndent("/*EOF*/");
	}

	// @Override
	// protected String _init(String value) {
	// if (value == null) {
	// return "\"\"";
	// }
	// return "toUTF8(" + StringUtils.quoteString('"', value.toString(), '"') +
	// ")";
	// }

	@Override
	protected String _initTag(int id, String s) {
		return "Symbol.unique(" + _quote(s) + ")";
	}

	@Override
	protected String _initLabel(int id, String s) {
		return "Symbol.unique(" + _quote(s) + ")";
	}

	@Override
	protected String _initTable(int id, String s) {
		return "Symbol.unique(" + _quote(s) + ")";
	}

}
