package nez.tool.parser;

import nez.lang.Grammar;

public class JavaParserGenerator extends AbstractParserGenerator {

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
			VarDecl(_state_(), "new ParserContext(text)");
			If(_funccall(_funcname(g.getStartProduction())));
			{
				Return(_cleft_());
			}
			EndIf();
			Return(_Null());
		}
		EndDecl();
		BeginDecl("public final static int match(String text)");
		{
			VarDecl(_state_(), "new ParserContext(text)");
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

	@Override
	protected void initTypeMap() {
		this.addType("parse", "boolean");
		this.addType("memo", "int");
		this.addType(_byteSet_(), "boolean[]");
		this.addType(_indexMap_(), "byte[]");
		this.addType(_byteSeq_(), "byte[]");
		this.addType(_unchoiced_(), "boolean");
		this.addType(_pos_(), "int");
		this.addType(_left_(), "Tree<?>");
		this.addType(_log_(), "Object");
		this.addType(_sym_(), "int");
		this.addType(_state_(), "ParserContext");
	}

}
