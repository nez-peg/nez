package nez.tool.parser;

import nez.lang.Grammar;

public class CParserGenerator extends AbstractParserGenerator {

	@Override
	protected String _True() {
		return "1";
	}

	@Override
	protected String _False() {
		return "0";
	}

	@Override
	protected String _Null() {
		return "NULL";
	}

	/* Expression */

	protected String _GetArray(String array, String c) {
		return array + "[" + c + "]";
	}

	@Override
	protected String _Field(String o, String name) {
		return o + "->" + name;
	}

	@Override
	protected String _Func(String name, String... args) {
		StringBuilder sb = new StringBuilder();
		sb.append("Parser_");
		sb.append(name);
		sb.append("(");
		sb.append(_state_());
		for (int i = 0; i < args.length; i++) {
			sb.append(",");
			sb.append(args[i]);
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	protected String _Match(byte[] byteSeq) {
		return _Func("match", _bytes(byteSeq), _int(byteSeq.length));
	}

	@Override
	protected String _function(String type) {
		return "static";
	}

	/* Statement */

	@Override
	protected void ConstDecl(String type, String name, String expr) {
		Statement("static " + type + " " + name + " = " + expr);
	}

	// Grammar Generator

	@Override
	protected String getFileExtension() {
		return "c";
	}

	@Override
	protected void generateHeader(Grammar g) {
		BeginDecl("struct Tree* parse(const char *text)");
		{
			VarDecl(_state_(), "ParserContext(text)");
			If(_funccall(_funcname(g.getStartProduction())));
			{
				Return(_cleft_());
			}
			EndIf();
			Return(_Null());
		}
		EndDecl();
		BeginDecl("long match(const char *text)");
		{
			VarDecl(_state_(), "ParserContext(text)");
			If(_funccall(_funcname(g.getStartProduction())));
			{
				Return(_cpos_());
			}
			EndIf();
			Return("-1");
		}
		EndDecl();
	}

	@Override
	protected void generateFooter(Grammar g) {
		BeginDecl("int main(const char **argv)");
		{
			Statement("struct Tree *t = parse(rgv[1])");
			Return("0");
		}
		EndDecl();
		file.writeIndent("// End of File");
	}

	@Override
	protected void initTypeMap() {
		this.addType("parse", "int");/* boolean */
		this.addType("memo", "int");
		this.addType(_byteSet_(), "const char[]");/* boolean */
		this.addType(_indexMap_(), "const char[]");
		this.addType(_byteSeq_(), "const char[]");
		this.addType(_unchoiced_(), "int");/* boolean */
		this.addType(_pos_(), "char *");
		this.addType(_left_(), "struct Tree *");
		this.addType(_log_(), "void *");
		this.addType(_sym_(), "int");
		this.addType(_state_(), "Parser *");
	}

}
