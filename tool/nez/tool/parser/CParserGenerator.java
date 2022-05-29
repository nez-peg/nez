package nez.tool.parser;

import nez.lang.Grammar;
import nez.util.StringUtils;

import java.util.Locale;

public class CParserGenerator extends CommonParserGenerator {

	public CParserGenerator() {
		super(".c");
	}

	@Override
	protected void initLanguageSpec() {
		SupportedRange = true;
		SupportedMatch2 = true;
		SupportedMatch3 = true;
		SupportedMatch4 = true;
		SupportedMatch5 = true;
		SupportedMatch6 = true;
		SupportedMatch7 = true;
		SupportedMatch8 = true;

		this.addType("$parse", "int");
		this.addType("$tag", "int");
		this.addType("$label", "int");
		this.addType("$table", "int");
		this.addType("$arity", "int");
		this.addType("$text", "const unsigned char");
		this.addType("$index", "const unsigned char");
		if (UsingBitmap) {
			this.addType("$set", "int");
		} else {
			this.addType("$set", "const unsigned char");
		}
		this.addType("$range", "const unsigned char __attribute__((aligned(16)))");
		this.addType("$string", "const char *");

		this.addType("memo", "int");
		if (UsingBitmap) {
			this.addType(_set(), "int");
		} else {
			this.addType(_set(), "const unsigned char *");/* boolean */
		}
		this.addType(_index(), "const unsigned char *");
		this.addType(_temp(), "int");/* boolean */
		this.addType(_pos(), "const unsigned char *");
		this.addType(_tree(), "size_t");
		this.addType(_log(), "size_t");
		this.addType(_table(), "size_t");
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
	protected String _text(byte[] text) {
		return super._text(text) + ", " + _int(text.length);
	}

	@Override
	protected String _text(String key) {
		if (key == null) {
			return _Null() + ", 0";
		}
		return nameMap.get(key) + ", " + _int(StringUtils.utf8(key).length);
	}

	@Override
	protected String _defun(String type, String name) {
		if (this.crossRefNames.contains(name)) {
			return type + " " + name;
		}
		return "static inline " + type + " " + name;
	}

	/* Statement */

	@Override
	protected void DeclConst(String type, String name, String expr) {
		Statement("static " + type + " " + name + " = " + expr);
	}

	// Grammar Generator

	@Override
	protected void generateHeader(Grammar g) {
		Line("/*");
		Line("* " + _basename() + " grammar parser.");
		Line("*/\n");
		importFileContent("cnez-runtime.txt");
	}

	@Override
	protected void generatePrototypes() {
		LineComment("Prototypes");
		for (String name : this.crossRefNames) {
			Statement(_defun("int", name) + "(ParserContext *c)");
		}
	}

	@Override
	protected void generateFooter(Grammar g) {
		importFileContent("cnez-utils.txt");
		//
		BeginDecl("void* " + _ns() + "parse(const char *fname, const char *text, size_t len, void *thunk, void (*ferr)(const char *, const unsigned char *, void *), void* (*fnew)(symbol_t, const unsigned char *, size_t, size_t, void *), void  (*fset)(void *, size_t, symbol_t, void *, void *), void  (*fgc)(void *, int, void *))");
		{
			VarDecl("void*", "result", _Null());
			VarDecl(_state(), "ParserContext_new((const unsigned char*)text, len)");
			Statement("c->input_fname = fname");
			Statement(_Func("initTreeFunc", "thunk", "ferr", "fnew", "fset", "fgc"));
			this.InitMemoPoint();
			If(_funccall(_funcname(g.getStartProduction())));
			{
				VarAssign("result", _Field(_state(), _tree()));
				If("result == NULL");
				{
					Statement("result = c->fnew(0, (const unsigned char*)text, (c->pos - (const unsigned char*)text), 0, c->thunk)");
				}
				EndIf();
			}
			EndIf();
			If("ferr && !ParserContext_eof(c)");
                        {
                                Statement("ferr(\"syntax error\", c->last_pos, c)");
			}
			EndIf();
			Statement(_Func("free"));
			Return("result");
		}
		EndDecl();
		BeginDecl("static void* cnez_parse(const char *fname, const char *text, size_t len)");
		{
			Return(_ns() + "parse(fname, text, len, NULL, PERR, NULL, NULL, NULL)");
		}
		EndDecl();
		BeginDecl("long " + _ns() + "match(const char *fname, const char *text, size_t len)");
		{
			VarDecl("long", "result", "-1");
			VarDecl(_state(), "ParserContext_new((const unsigned char*)text, len)");
			Statement("c->input_fname = fname");
			Statement(_Func("initNoTreeFunc"));
			this.InitMemoPoint();
			If(_funccall(_funcname(g.getStartProduction())));
			{
				VarAssign("result", _cpos() + "-" + _Field(_state(), "inputs"));
			}
			EndIf();
			Statement(_Func("free"));
			Return("result");
		}
		EndDecl();
		BeginDecl("const char* " + _ns() + "tag(symbol_t n)");
		{
			Return("_tags[n]");
		}
		EndDecl();
		BeginDecl("const char* " + _ns() + "label(symbol_t n)");
		{
			Return("_labels[n]");
		}
		EndDecl();
		Line("#ifndef UNUSE_MAIN");
		BeginDecl("int main(int ac, const char **argv)");
		{
			Return("cnez_main(ac, argv, cnez_parse)");
		}
		EndDecl();
		Line("#endif/*MAIN*/");
		Line("\n");
		Line("#ifdef __cplusplus");
		Line("}");
		Line("#endif");

		file.writeIndent("// End of File");
		generateHeaderFile();
		this.showManual("cnez-man.txt", new String[] { "$cmd$", _basename() });
	}

	private void generateHeaderFile() {
		String upcaseName = _basename().toUpperCase(Locale.ROOT) + "_H";
		this.setFileBuilder(".h");
		Line("/*");
		Line("* Header file for " + _basename() + " grammar parser.");
		Line("*/\n");
		Line("#ifndef __" + upcaseName);
		Line("#define __" + upcaseName + "\n");
		Line("#ifdef __cplusplus");
		Line("extern \"C\" {");
		Line("#endif\n");
		Statement("typedef unsigned long int symbol_t");
		int c = 1;
		for (String s : this.tagList) {
			if (s.equals("")) {
				continue;
			}
			Line("#define _" + s + " ((symbol_t)" + c + ")");
			c++;
		}
		Line("#define MAXTAG " + c);
		c = 1;
		for (String s : this.labelList) {
			if (s.equals("")) {
				continue;
			}
			Line("#define _" + s + " ((symbol_t)" + c + ")");
			c++;
		}
		Line("#define MAXLABEL " + c);
		Statement("void* " + _ns() + "parse(const char *fname, const char *text, size_t len, void *, void (*ferr)(const char *, const unsigned char *, void *), void* (*fnew)(symbol_t, const char *, size_t, size_t, void *), void  (*fset)(void *, size_t, symbol_t, void *, void *), void  (*fgc)(void *, int, void *))");
		Statement("long " + _ns() + "match(const char *fname, const char *text, size_t len)");
		Statement("const char* " + _ns() + "tag(symbol_t n)");
		Statement("const char* " + _ns() + "label(symbol_t n)");
		Line("\n");
		Line("#ifdef __cplusplus");
		Line("}");
		Line("#endif");
		Line("\n");
		Line("#endif /* __" + upcaseName + " */");
		this.file.close();
	}

}
