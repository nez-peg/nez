package nez.tool.parser;

import nez.lang.Grammar;

public class PythonParserGenerator extends CommonParserGenerator {

	public PythonParserGenerator() {
		super(".py");
	}

	public boolean Python3 = false;

	@Override
	protected void initLanguageSpec() {
		// this.addType("parse", "boolean");
		// this.addType("memo", "int");
		// this.addType(_byteSet_(), "boolean[]");
		// this.addType(_indexMap_(), "byte[]");
		// this.addType(_byteSeq_(), "byte[]");
		// this.addType(_unchoiced_(), "boolean");
		// this.addType(_pos_(), "int");
		// this.addType(_left_(), "Tree<?>");
		// this.addType(_log_(), "Object");
		// this.addType(_sym_(), "int");
		// this.addType(_state_(), "ParserContext");
	}

	/* Syntax */

	@Override
	protected String _LineComment() {
		return "##";
	}

	@Override
	protected String _Comment(String c) {
		return "";
	}

	@Override
	protected String _And() {
		return "and";
	}

	@Override
	protected String _Or() {
		return "or";
	}

	@Override
	protected String _Not(String expr) {
		return "not " + expr;
	}

	@Override
	protected String _Eq() {
		return "==";
	}

	@Override
	protected String _NotEq() {
		return "!=";
	}

	@Override
	protected String _True() {
		return "True";
	}

	@Override
	protected String _False() {
		return "False";
	}

	@Override
	protected String _Null() {
		return "None";
	}

	/* Expression */

	protected String _GetArray(String array, String c) {
		return array + "[" + c + "]";
	}

	@Override
	protected String _BeginArray() {
		return "[";
	}

	@Override
	protected String _EndArray() {
		return "]";
	}

	@Override
	protected String _BeginBlock() {
		return " {";
	}

	@Override
	protected String _EndBlock() {
		return "}";
	}

	@Override
	protected String _Field(String o, String name) {
		return o + "." + name;
	}

	@Override
	protected String _Func(String name, String... args) {
		StringBuilder sb = new StringBuilder();
		sb.append(_state());
		sb.append(".");
		sb.append(name);
		sb.append("(");
		for (int i = 0; i < args.length; i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append(args[i]);
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	protected String _int(int n) {
		return "" + n;
	}

	@Override
	protected String _long(long n) {
		return "" + n;
	}

	@Override
	protected String _byte(int ch) {
		// if (ch < 128 && (!Character.isISOControl(ch))) {
		// return "'" + (char) ch + "'";
		// }
		return "" + ch;
	}

	/* Expression */

	@Override
	protected String _defun(String type, String name) {
		return "def " + name;
	}

	@Override
	protected String _argument(String var, String type) {
		return var;
	}

	@Override
	protected String _argument() {
		return _argument(_state(), type(_state()));
	}

	@Override
	protected String _funccall(String name) {
		return name + "(" + _state() + ")";
	}

	/* Statement */

	@Override
	protected void BeginFunc(String type, String name, String args) {
		file.writeIndent();
		file.write(_defun(type, name));
		file.write("(");
		file.write(args);
		file.write(")");
		Begin();
	}

	@Override
	protected void EndFunc() {
		End();
	}

	@Override
	protected void Begin() {
		file.write(" :");
		file.incIndent();
	}

	@Override
	protected void End() {
		file.decIndent();
		// file.writeIndent();
		// file.write("#");
	}

	@Override
	protected void BeginLocalScope() {
		/* No local scope */
	}

	@Override
	protected void EndLocalScope() {
		/* No local scope */
	}

	@Override
	protected void Line(String stmt) {
		file.writeIndent(stmt);
	}

	@Override
	protected void Statement(String stmt) {
		file.writeIndent(stmt);
		_Semicolon();
	}

	@Override
	protected void EmptyStatement() {
		file.writeIndent("pass");
		_Semicolon();
	}

	@Override
	protected void _Semicolon() {
	}

	@Override
	protected void Return(String expr) {
		Statement("return " + expr);
	}

	@Override
	protected void If(String cond) {
		file.writeIndent("if ");
		file.write(cond);
		file.write("");
		Begin();
	}

	@Override
	protected void Else() {
		End();
		file.writeIndent("else");
		Begin();
	}

	@Override
	protected void EndIf() {
		End();
	}

	@Override
	protected void While(String cond) {
		file.writeIndent();
		file.write("while ");
		file.write(cond);
		file.write("");
		Begin();
	}

	@Override
	protected void EndWhile() {
		End();
	}

	@Override
	protected void Break() {
		file.writeIndent("break");
		_Semicolon();
	}

	@Override
	protected void VarDecl(String v, String expr) {
		VarDecl(null, v, expr);
	}

	@Override
	protected void VarDecl(String type, String name, String expr) {
		VarAssign(name, expr);
	}

	@Override
	protected void VarAssign(String name, String expr) {
		Statement(name + " = " + expr);
	}

	@Override
	protected void DeclConst(String type, String name, String expr) {
		Statement(name + " = " + expr);
	}

	// Grammar Generator

	@Override
	protected void generateHeader(Grammar g) {
		BeginFunc("parse", "text");
		{
			VarDecl(_state(), "ParserContext(text)");
			If(_funccall(_funcname(g.getStartProduction())));
			{
				Return(_Field(_state(), _tree()));
			}
			EndIf();
			Return(_Null());
		}
		EndFunc();
		BeginFunc("match", "text");
		{
			VarDecl(_state(), "ParserContext(text)");
			If(_funccall(_funcname(g.getStartProduction())));
			{
				Return(_cpos());
			}
			EndIf();
			Return("-1");
		}
		EndFunc();
	}

	@Override
	protected void generateFooter(Grammar g) {
		If("__name__ == '__main__'");
		{
			Statement("t = parse(sys.argv[1])");
			Statement("print (t)");
		}
		EndIf();
		file.writeIndent("## End of File");
	}

}
