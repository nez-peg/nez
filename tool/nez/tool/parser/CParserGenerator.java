package nez.tool.parser;

import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Production;

public class CParserGenerator extends AbstractParserGenerator {

	@Override
	protected void initTypeMap() {
		this.addType("$parse", "int");
		this.addType("$tag", "int");
		this.addType("$label", "int");
		this.addType("$table", "int");
		this.addType("$arity", "int");
		this.addType("$text", "const char");
		this.addType("$index", "const unsigned short");
		this.addType("$set", "const char");
		this.addType("$string", "const char *");

		this.addType("memo", "int");
		this.addType(_set(), "const char *");/* boolean */
		this.addType(_index(), "const unsigned char *");
		this.addType(_unchoiced(), "int");/* boolean */
		this.addType(_pos(), "const char *");
		this.addType(_tree(), "size_t");
		this.addType(_log(), "size_t");
		this.addType(_sym_(), "size_t");
		this.addType(_state(), "ParserContext *");
	}

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

	@Override
	protected String _Field(String o, String name) {
		return o + "->" + name;
	}

	@Override
	protected String _Func(String name, String... args) {
		StringBuilder sb = new StringBuilder();
		sb.append("ParserContext_");
		sb.append(name);
		sb.append("(");
		sb.append(_state());
		for (int i = 0; i < args.length; i++) {
			sb.append(",");
			sb.append(args[i]);
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	protected String _function(String type) {
		return type;
	}

	/* Statement */

	@Override
	protected void DeclConst(String type, String name, String expr) {
		Statement("static " + type + " " + name + " = " + expr);
	}

	// Grammar Generator

	@Override
	protected String getFileExtension() {
		return "c";
	}

	@Override
	protected void generateHeader(Grammar g) {
		ImportFile("/nez/tool/parser/ext/c-parser-runtime.txt");
		DeclPrototype(g);
	}

	@Override
	protected void generateFooter(Grammar g) {
		BeginDecl("void* " + _prefix() + "parse(const char *text, size_t len, void* (*fnew)(symbol_t, const char *, size_t, size_t, void *), void  (*fset)(void *, size_t, symbol_t, void *), void  (*fgc)(void *, int))");
		{
			VarDecl("void*", "result", _Null());
			VarDecl(_state(), "ParserContext_new(text, len)");
			Statement(_Func("initTreeFunc", "fnew", "fset", "fgc"));
			if (code.getMemoPointSize() > 0) {
				Statement(_Func("initMemoPoint", _int(strategy.SlidingWindow), _int(code.getMemoPointSize())));
			}
			If(_funccall(_funcname(g.getStartProduction())));
			{
				VarAssign("result", _Field(_state(), _tree()));
			}
			EndIf();
			Statement(_Func("free"));
			Return("result");
		}
		EndDecl();
		BeginDecl("long " + _prefix() + "match(const char *text, size_t len)");
		{
			VarDecl("long", "result", "-1");
			VarDecl(_state(), "ParserContext_new(text, len)");
			Statement(_Func("initNoTreeFunc"));
			if (code.getMemoPointSize() > 0) {
				Statement(_Func("initMemoPoint", _int(strategy.SlidingWindow), _int(code.getMemoPointSize())));
			}
			If(_funccall(_funcname(g.getStartProduction())));
			{
				VarAssign("result", _cpos_() + "-" + _Field(_state(), "inputs"));
			}
			EndIf();
			Statement(_Func("free"));
			Return("result");
		}
		EndDecl();
		BeginDecl("const char* " + _prefix() + "tag(symbol_t n)");
		{
			Return("_tags[n]");
		}
		EndDecl();
		BeginDecl("const char* " + _prefix() + "label(symbol_t n)");
		{
			Return("_labels[n]");
		}
		EndDecl();
		BeginDecl("int main(int ac, const char **argv)");
		{
			Statement("void *t = " + _prefix() + "parse(argv[1], strlen(argv[1]), NULL, NULL, NULL)");
			Statement("cnez_dump(t, stderr)");
			Return("0");
		}
		EndDecl();
		file.writeIndent("// End of File");
	}

	private void DeclPrototype(Grammar g) {
		int c = 0;
		for (Production p : g) {
			String n = _funcname(p.getUniqueName());
			DeclPrototype(n);
			c = checkInner(p.getExpression(), c);
		}
		for (int i = 0; i < c; i++) {
			DeclPrototype("e" + i);
		}
	}

	private int checkInner(Expression e, int c) {
		if (e.size() == 1) {
			return checkInner(e.get(0), c) + 1;
		}
		for (Expression sub : e) {
			c = checkInner(sub, c);
		}
		return c;
	}

	private void DeclPrototype(String name) {
		Statement(_function("int " + name) + "(ParserContext *c)");
	}

}
