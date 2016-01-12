package nez.tool.parser;

import nez.lang.Grammar;

public class JavaParserGenerator extends AbstractParserGenerator {

	@Override
	protected String getFileExtension() {
		return "Parser.java";
	}

	@Override
	protected String generateHeader(Grammar g) {
		Statement("import nez.ast.Tree");
		Statement("import nez.ast.Symbol");
		Statement("import nez.tool.parser.ParserContext");
		Line("public class Parser");
		Begin();
		Line("public final static Tree<?> parse(String text)");
		Begin();
		{
			Statement("ParserContext c = new ParserContext(text)");
			If("parse(c)");
			{
				Return(_cleft());
			}
			EndIf();
			Return(_null());
		}
		End();
		Line("public final static int match(String text)");
		Begin();
		{
			Statement("ParserContext c = new ParserContext(text)");
			If("parse(c)");
			{
				Return(_cpos());
			}
			EndIf();
			Return("-1");
		}
		End();
		return null;
	}

	@Override
	protected String generateFooter(Grammar g) {
		Line("public final static void main(String[] a)");
		Begin();
		{
			Statement("Tree<?> t = parse(a[0])");
			Statement("System.out.println(\"parsed:\" + t)");
		}
		End();

		End();
		file.writeIndent("/*EOF*/");
		return null;
	}

	@Override
	protected void initTypeMap() {
		this.addType(_byteMap(), "boolean[]");
		this.addType(_unchoiced(), "boolean");
		this.addType(_pos(), "int");
		this.addType(_left(), "Tree<?>");
		this.addType(_log(), "Object");
		this.addType(_sym(), "int");
		this.addType(_state(), "ParserContext");
	}

}
