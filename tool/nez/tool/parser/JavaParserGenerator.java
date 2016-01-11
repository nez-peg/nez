package nez.tool.parser;

import nez.lang.Grammar;

public class JavaParserGenerator extends AbstractParserGenerator {

	@Override
	protected String getFileExtension() {
		return "java";
	}

	@Override
	protected String generateHeader(Grammar g) {
		Statement("import nez.parser.Context");
		Statement("public class Parser");
		Begin();
		return null;
	}

	@Override
	protected String generateFooter(Grammar g) {
		End();
		file.writeIndent("/*EOF*/");
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void initTypeMap() {
		this.addType(_unchoiced(), "boolean");
		this.addType(_pos(), "char *");
		this.addType(_left(), "Tree<?>");
		this.addType(_log(), "int");
		this.addType(_sym(), "int");

	}

}
