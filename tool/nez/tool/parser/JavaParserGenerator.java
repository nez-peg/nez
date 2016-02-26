package nez.tool.parser;

import nez.lang.Grammar;

public class JavaParserGenerator extends AbstractParserGenerator {

	@Override
	protected void initTypeMap() {
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
		this.addType(_unchoiced(), "boolean");
		this.addType(_pos(), "int");
		this.addType(_tree(), "T");
		this.addType(_log(), "int");
		this.addType(_table(), "int");
		this.addType(_state(), "ParserContext<T>");
	}

	@Override
	protected String getFileExtension() {
		return "java";
	}

	@Override
	protected void generateHeader(Grammar g) {
		// Statement("import nez.ast.Tree");
		// Statement("import nez.ast.Symbol");
		// Statement("import nez.ast.Source");
		// Statement("import nez.ast.CommonTree");
		// Statement("import nez.parser.io.StringSource");
		// Statement("import nez.util.StringUtils");

		BeginDecl("public class " + _basename());
		// BeginDecl("public final static Tree<?> parse(String text)");
		// {
		// VarDecl(_tree(), _Null());
		// VarDecl(_state(), "new ParserContext(text)");
		// InitMemoPoint();
		// If(_funccall(_funcname(g.getStartProduction())));
		// {
		// VarAssign(_tree(), _Field(_state(), _tree()));
		// If(_tree(), _Eq(), _Null());
		// VarAssign(_tree(), _Func("newTree", _Null(), "0", _cpos(), "0",
		// _Null()));
		// EndIf();
		// }
		// EndIf();
		// Return(_Null());
		// }
		// EndDecl();
		// BeginDecl("public final static int match(String text)");
		// {
		// VarDecl(_state(), "new ParserContext(text)");
		// InitMemoPoint();
		// If(_funccall(_funcname(g.getStartProduction())));
		// {
		// Return(_cpos());
		// }
		// EndIf();
		// Return("-1");
		// }
		// EndDecl();
		ImportFile("/nez/tool/parser/ext/java-parser-runtime.txt");
	}

	@Override
	protected void generateFooter(Grammar g) {
		BeginDecl("public final static void main(String[] a)");
		{
			Statement("SimpleTree t = parse(a[0])");
			Statement("System.out.println(\"parsed:\" + t)");
		}
		EndDecl();

		EndDecl(); // end of class
		file.writeIndent("/*EOF*/");
	}

	@Override
	protected String _function(String type) {
		return "private static <T> " + type;
	}

	// @Override
	// protected String _initTag(int id, String s) {
	// return "Symbol.unique(" + _quote(s) + ")";
	// }
	//
	// @Override
	// protected String _initLabel(int id, String s) {
	// return "Symbol.unique(" + _quote(s) + ")";
	// }
	//
	// @Override
	// protected String _initTable(int id, String s) {
	// return "Symbol.unique(" + _quote(s) + ")";
	// }

}
