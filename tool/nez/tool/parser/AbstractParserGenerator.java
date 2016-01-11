package nez.tool.parser;

import java.util.HashMap;

import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Nez;
import nez.lang.Nez.IfCondition;
import nez.lang.Nez.LocalScope;
import nez.lang.Nez.OnCondition;
import nez.lang.Nez.SymbolAction;
import nez.lang.Nez.SymbolExists;
import nez.lang.Nez.SymbolMatch;
import nez.lang.Nez.SymbolPredicate;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.lang.Symbolstate;
import nez.lang.Symbolstate.StateAnalyzer;
import nez.lang.Typestate;
import nez.lang.Typestate.TypestateAnalyzer;
import nez.parser.Parser;
import nez.parser.ParserStrategy;
import nez.util.ConsoleUtils;
import nez.util.FileBuilder;
import nez.util.StringUtils;
import nez.util.UList;

public abstract class AbstractParserGenerator implements SourceGenerator {
	protected Parser parser;
	protected ParserStrategy strategy;
	protected String path;
	protected FileBuilder file;
	protected TypestateAnalyzer typeState = Typestate.newAnalyzer();
	protected StateAnalyzer symbolState = Symbolstate.newAnalyzer();

	@Override
	public final void init(Grammar g, Parser parser, String path) {
		this.parser = parser;
		this.strategy = parser.getParserStrategy();
		if (path == null) {
			this.file = new FileBuilder(null);
		} else {
			this.path = FileBuilder.extractFileName(path);
			String filename = FileBuilder.changeFileExtension(path, this.getFileExtension());
			this.file = new FileBuilder(filename);
			ConsoleUtils.println("generating " + filename + " ... ");
		}
		this.initTypeMap();
	}

	@Override
	public void doc(String command, String urn, String outputFormat) {
		// file.writeIndent(LineComment + "Translated by nez " + command +
		// " -g " + urn + " --format " + outputFormat);
	}

	protected abstract String getFileExtension();

	protected abstract String generateHeader(Grammar g);

	protected abstract String generateFooter(Grammar g);

	@Override
	public void generate() {
		Grammar g = this.parser.getParserGrammar();
		this.generateHeader(g);
		this.generate(g);
		this.generateFooter(g);
		file.writeNewLine();
		file.flush();
	}

	public void generate(Grammar g) {
		ParserGeneratorVisitor gen = new ParserGeneratorVisitor();
		gen.generate(g.getStartProduction(), "parse");
	}

	/* Statment */

	protected void BeginFunc(String f) {
		file.writeIndent();
		file.write(_defun());
		file.write(" ");
		file.write(f);
		file.write("(");
		file.write(_argument());
		file.write(")");
		Begin();
	}

	protected void EndFunc() {
		End();
	}

	protected void Begin() {
		file.write(" {");
		file.incIndent();
	}

	protected void End() {
		file.decIndent();
		file.writeIndent();
		file.write("}");
	}

	protected void BeginLocalScopeBlock() {
		file.writeIndent("{");
		file.incIndent();
	}

	protected void EndLocalScopeBlock() {
		file.decIndent();
		file.writeIndent();
		file.write("}");
	}

	protected void Statement(String stmt) {
		file.writeIndent(stmt);
		Semicolon();
	}

	protected void EmptyStatement() {
		file.writeIndent();
		Semicolon();
	}

	protected void Semicolon() {
		file.write(";");
	}

	protected void LineComment(String stmt) {
		file.writeIndent(_comment() + " " + stmt);
	}

	protected void Return(String expr) {
		Statement("return " + expr);
	}

	protected void Succ() {
		Return(_true());
	}

	protected void Fail() {
		Return(_false());
	}

	protected void If(String cond) {
		file.writeIndent("if (");
		file.write(cond);
		file.write(")");
		Begin();
	}

	protected String _op(String a, String op, String b) {
		return a + " " + op + " " + b;
	}

	protected void If(String a, String op, String b) {
		If(a + " " + op + " " + b);
	}

	protected void Else() {
		End();
		file.write(" else");
		Begin();
	}

	protected void EndIf() {
		End();
	}

	protected void While(String cond) {
		file.writeIndent();
		file.write("while (");
		file.write(cond);
		file.write(")");
		Begin();
	}

	protected void EndWhile() {
		End();
	}

	protected void Break() {
		file.writeIndent("break");
		Semicolon();
	}

	protected void VarDecl(String t, String v, String expr) {
		if (t == null) {
			Val(v, expr);
		} else {
			file.writeIndent(t + " " + v + " = " + expr);
			Semicolon();
		}
	}

	protected void Val(String v, String expr) {
		file.writeIndent(v + " = " + expr);
		Semicolon();
	}

	/* Backtrack */

	/* Syntax */

	protected String _comment() {
		return "//";
	}

	protected String _and() {
		return "&&";
	}

	protected String _eq() {
		return "==";
	}

	protected String _noteq() {
		return "!=";
	}

	protected String _true() {
		return "true";
	}

	protected String _false() {
		return "false";
	}

	protected String _istrue(String expr) {
		return expr;
	}

	protected String _not(String expr) {
		return "!" + expr;
	}

	/* Expression */

	protected String _defun() {
		return "private static boolean";
	}

	protected String _state() {
		return "c";
	}

	protected String _call(String f) {
		return f + "(" + _state() + ")";
	}

	protected String _argument() {
		return "Context " + _state();
	}

	protected String _field(String o, String name) {
		return o + "." + name;
	}

	protected String _int(int n) {
		return "" + n;
	}

	protected String _byte(int ch) {
		if (ch < 128 && (!Character.isISOControl(ch))) {
			return "'" + (char) ch + "'";
		}
		return "" + ch;
	}

	protected String _bytes(byte[] utf8) {
		return StringUtils.quoteString('"', new String(utf8), '"');
	}

	protected String _null() {
		return "null";
	}

	protected String _symbol(Symbol s) {
		return s == null ? _null() : StringUtils.quoteString('"', s.toString(), '"');
	}

	protected String _string(String s) {
		return StringUtils.quoteString('"', s, '"');
	}

	protected String _funcname(String uname) {
		return "p" + uname;
	}

	protected String PFunc(String name, String... args) {
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

	/* Variable */

	protected String _pos() {
		return "pos";
	}

	protected String _cpos() {
		return _field(_state(), "pos");
	}

	protected String _left() {
		return "left";
	}

	protected String _cleft() {
		return _field(_state(), "left");
	}

	protected String _log() {
		return "log";
	}

	protected String _clog() {
		return _field(_state(), "log");
	}

	protected String _sym() {
		return "sym";
	}

	protected String _csym() {
		return _field(_state(), "sym");
	}

	protected String _unchoiced() {
		return "unchoiced";
	}

	protected HashMap<String, String> typeMap = new HashMap<>();

	protected void addType(String name, String type) {
		typeMap.put(name, type);
	}

	protected String getType(String name) {
		return typeMap.get(name);
	}

	protected abstract void initTypeMap();

	class ParserGeneratorVisitor extends Expression.Visitor {

		HashMap<String, Expression> funcMap = new HashMap<>();
		HashMap<String, String> exprMap = new HashMap<>();
		UList<String> funcList = new UList<String>(new String[128]);
		int generated = 0;

		void generate(Production start, String func) {
			String f = makeFuncCall(start.getUniqueName(), start.getExpression());
			BeginFunc(func);
			{
				Return(f);
			}
			EndFunc();
			int size = funcList.size();
			while (generated < size) {
				String key = funcList.get(generated);
				generateFunction(key, funcMap.get(key));
				size = funcList.size();
				generated++;
			}
		}

		private String makeFuncCall(String uname, Expression e) {
			String key = _funcname(uname);
			if (!funcMap.containsKey(key)) {
				funcList.add(key);
				funcMap.put(key, e);
			}
			return _call(key);
		}

		private String makeFuncCall(Expression e) {
			String key = e.toString();
			String f = exprMap.get(key);
			if (f == null) {
				f = "e" + exprMap.size();
				exprMap.put(key, f);
				funcList.add(f);
				funcMap.put(f, e);
			}
			return _call(f);
		}

		private void generateFunction(String name, Expression e) {
			BeginFunc(name);
			{
				initFunc(e);
				visit(e, null);
				Succ();
			}
			EndFunc();
		}

		void initFunc(Expression e) {

		}

		int nested = -1;

		private void visit(Expression e, Object a) {
			int lnested = this.nested;
			this.nested++;
			e.visit(this, a);
			this.nested--;
			this.nested = lnested;
		}

		protected void BeginScope() {
			if (nested > 0) {
				BeginLocalScopeBlock();
			}
		}

		protected void EndScope() {
			if (nested > 0) {
				EndLocalScopeBlock();
			}
		}

		private String local(String name) {
			return nested > 0 ? name + nested : name;
		}

		void initVal(String name, String expr) {
			String type = getType(name);
			VarDecl(type, local(name), expr);
		}

		protected void Pos() {
			initVal(_pos(), _cpos());
		}

		protected void Back() {
			Val(_cpos(), local(_pos()));
		}

		protected void Store(Expression inner) {
			initVal(_pos(), _cpos());
			if (typeState.inferTypestate(inner) != Typestate.Unit) {
				initVal(_left(), _cleft());
				initVal(_log(), _clog());
			}
			if (symbolState.isStateful(inner)) {
				initVal(_sym(), _csym());
			}
		}

		protected void Backtrack(Expression inner) {
			Val(_cpos(), local(_pos()));
			if (typeState.inferTypestate(inner) != Typestate.Unit) {
				Val(_cleft(), local(_left()));
				Val(_clog(), local(_log()));
			}
			if (symbolState.isStateful(inner)) {
				Val(_csym(), local(_sym()));
			}
		}

		@Override
		public Object visitNonTerminal(NonTerminal e, Object a) {
			String f = makeFuncCall(e.getUniqueName(), e.deReference());
			If(_not(f));
			{
				Fail();
			}
			EndIf();
			return null;
		}

		@Override
		public Object visitEmpty(Nez.Empty e, Object a) {
			return null;
		}

		@Override
		public Object visitFail(Nez.Fail e, Object a) {
			Fail();
			return null;
		}

		@Override
		public Object visitByte(Nez.Byte e, Object a) {
			If(PFunc("read"), _noteq(), _byte(e.byteChar));
			{
				Fail();
			}
			EndIf();
			return null;
		}

		@Override
		public Object visitByteSet(Nez.ByteSet e, Object a) {
			If(MatchByteArray(e.byteMap, PFunc("read")));
			{
				Fail();
			}
			EndIf();
			return null;
		}

		private String MatchByteArray(boolean[] byteMap, String c) {
			return GetArray(StringUtils.stringfyBitmap(byteMap), c);
		}

		private String GetArray(String array, String c) {
			return array + "[" + c + "]";
		}

		@Override
		public Object visitAny(Nez.Any e, Object a) {
			if (strategy.Binary) {
				Statement(PFunc("move", "1"));
				If(PFunc("eof"));
				{
					Fail();
				}
				EndIf();
			} else {
				If(PFunc("read"), _eq(), "0");
				{
					Fail();
				}
				EndIf();
			}
			return null;
		}

		@Override
		public Object visitMultiByte(Nez.MultiByte e, Object a) {
			If(_not(PFunc("match", _bytes(e.byteSeq), _int(e.byteSeq.length))));
			{
				Fail();
			}
			EndIf();
			return null;
		}

		@Override
		public Object visitPair(Nez.Pair e, Object a) {
			for (Expression sub : e) {
				visit(sub, e);
			}
			return null;
		}

		@Override
		public Object visitSequence(Nez.Sequence e, Object a) {
			for (Expression sub : e) {
				visit(sub, e);
			}
			return null;
		}

		@Override
		public Object visitChoice(Nez.Choice e, Object a) {
			BeginScope();
			initVal(_unchoiced(), _true());
			for (Expression sub : e) {
				String f = makeFuncCall(sub);
				If(_istrue(local(_unchoiced())));
				{
					Store(sub);
					If(_istrue(f));
					{
						Val(local(_unchoiced()), _false());
					}
					Else();
					{
						Backtrack(sub);
					}
					EndIf();
				}
				EndIf();
			}
			If(_istrue(local(_unchoiced())));
			{
				Fail();
			}
			EndIf();
			EndScope();
			return null;
		}

		@Override
		public Object visitOption(Nez.Option e, Object a) {
			Expression sub = e.get(0);
			if (!tryOptionOptimization(sub)) {
				String f = makeFuncCall(sub);
				Store(sub);
				If(_not(f));
				{
					Backtrack(sub);
				}
				EndIf();
			}
			return null;
		}

		@Override
		public Object visitZeroMore(Nez.ZeroMore e, Object a) {
			visitRepetition(e, a);
			return null;
		}

		@Override
		public Object visitOneMore(Nez.OneMore e, Object a) {
			visit(e.get(0), a);
			visitRepetition(e, a);
			return null;
		}

		private void visitRepetition(Expression e, Object a) {
			Expression sub = e.get(0);
			if (!this.tryRepetitionOptimization(sub)) {
				String f = makeFuncCall(sub);
				While(_true());
				{
					Store(sub);
					If(_not(f));
					{
						Backtrack(sub);
						Break();
					}
					EndIf();
				}
				EndWhile();
			}
		}

		@Override
		public Object visitAnd(Nez.And e, Object a) {
			Expression sub = e.get(0);
			if (!this.tryAndOptimization(sub)) {
				String f = makeFuncCall(sub);
				BeginScope();
				Pos();
				If(_not(f));
				{
					Fail();
				}
				EndIf();
				Back();
				EndScope();
			}
			return null;
		}

		@Override
		public Object visitNot(Nez.Not e, Object a) {
			Expression sub = e.get(0);
			if (!this.tryNotOptimization(sub)) {
				String f = makeFuncCall(sub);
				BeginScope();
				Store(sub);
				If(_istrue(f));
				{
					Fail();
				}
				EndIf();
				Backtrack(sub);
				EndScope();
			}
			return null;
		}

		private boolean tryOptionOptimization(Expression inner) {
			if (strategy.Olex) {
				if (inner instanceof Nez.Byte) {
					Nez.Byte e = (Nez.Byte) inner;
					If(PFunc("prefetch"), _eq(), _byte(e.byteChar));
					{
						PFunc("move", "1");
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.ByteSet) {
					Nez.ByteSet e = (Nez.ByteSet) inner;
					If(MatchByteArray(e.byteMap, PFunc("prefetch")));
					{
						PFunc("move", "1");
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.MultiByte) {
					Nez.MultiByte e = (Nez.MultiByte) inner;
					Statement(PFunc("match", _bytes(e.byteSeq), _int(e.byteSeq.length)));
					return true;
				}
				if (inner instanceof Nez.Any) {
					// Nez.Any e = (Nez.Any) inner;
					If(_not(PFunc("eof")));
					{
						PFunc("move", "1");
					}
					EndIf();
					return true;
				}
			}
			return false;
		}

		private boolean tryRepetitionOptimization(Expression inner) {
			if (strategy.Olex) {
				if (inner instanceof Nez.Byte) {
					Nez.Byte e = (Nez.Byte) inner;
					While(_op(PFunc("prefetch"), _eq(), _byte(e.byteChar)));
					{
						PFunc("move", "1");
					}
					EndWhile();
					return true;
				}
				if (inner instanceof Nez.ByteSet) {
					Nez.ByteSet e = (Nez.ByteSet) inner;
					While(MatchByteArray(e.byteMap, PFunc("prefetch")));
					{
						PFunc("move", "1");
					}
					EndWhile();
					return true;
				}
				if (inner instanceof Nez.MultiByte) {
					Nez.MultiByte e = (Nez.MultiByte) inner;
					While(PFunc("match", _bytes(e.byteSeq), _int(e.byteSeq.length)));
					{
						EmptyStatement();
					}
					EndWhile();
					return true;
				}
				if (inner instanceof Nez.Any) {
					// Nez.Any e = (Nez.Any) inner;
					While(_not(PFunc("eof")));
					{
						PFunc("move", "1");
					}
					EndWhile();
					return true;
				}
			}
			return false;
		}

		private boolean tryAndOptimization(Expression inner) {
			if (strategy.Olex) {
				if (inner instanceof Nez.Byte) {
					Nez.Byte e = (Nez.Byte) inner;
					If(PFunc("prefetch"), _noteq(), _byte(e.byteChar));
					{
						Fail();
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.ByteSet) {
					Nez.ByteSet e = (Nez.ByteSet) inner;
					If(_not(MatchByteArray(e.byteMap, PFunc("prefetch"))));
					{
						Fail();
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.MultiByte) {
					Nez.MultiByte e = (Nez.MultiByte) inner;
					If(_not(PFunc("match", _bytes(e.byteSeq), _int(e.byteSeq.length))));
					{
						Fail();
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.Any) {
					// Nez.Any e = (Nez.Any) inner;
					If(PFunc("eof"));
					{
						Fail();
					}
					EndIf();
					return true;
				}
			}
			return false;
		}

		private boolean tryNotOptimization(Expression inner) {
			if (strategy.Olex) {
				if (inner instanceof Nez.Byte) {
					Nez.Byte e = (Nez.Byte) inner;
					If(PFunc("prefetch"), _eq(), _byte(e.byteChar));
					{
						Fail();
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.ByteSet) {
					Nez.ByteSet e = (Nez.ByteSet) inner;
					If(MatchByteArray(e.byteMap, PFunc("prefetch")));
					{
						Fail();
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.MultiByte) {
					Nez.MultiByte e = (Nez.MultiByte) inner;
					If(PFunc("match", _bytes(e.byteSeq), _int(e.byteSeq.length)));
					{
						Fail();
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.Any) {
					// Nez.Any e = (Nez.Any) inner;
					If(_not(PFunc("eof")));
					{
						Fail();
					}
					EndIf();
					return true;
				}
			}
			return false;
		}

		/* Tree Construction */

		@Override
		public Object visitBeginTree(Nez.BeginTree e, Object a) {
			Statement(PFunc("beginTree", _int(e.shift)));
			return null;
		}

		@Override
		public Object visitEndTree(Nez.EndTree e, Object a) {
			Statement(PFunc("endTree", _int(e.shift)));
			return null;
		}

		@Override
		public Object visitFoldTree(Nez.FoldTree e, Object a) {
			Statement(PFunc("foldTree", _int(e.shift), _symbol(e.label)));
			return null;
		}

		@Override
		public Object visitLinkTree(Nez.LinkTree e, Object a) {
			BeginScope();
			initVal(_left(), _cleft());
			visit(e.get(0), a);
			Statement(PFunc("linkTree", local(_left())));
			Val(_cleft(), local(_left()));
			EndScope();
			return null;
		}

		@Override
		public Object visitTag(Nez.Tag e, Object a) {
			Statement(PFunc("tagTree", _symbol(e.tag)));
			return null;
		}

		@Override
		public Object visitReplace(Nez.Replace e, Object a) {
			Statement(PFunc("valueTree", _string(e.value)));
			return null;
		}

		@Override
		public Object visitDetree(Nez.Detree e, Object a) {
			BeginScope();
			initVal(_left(), _cleft());
			visit(e, a);
			Val(_cleft(), local(_left()));
			EndScope();
			return null;
		}

		@Override
		public Object visitBlockScope(Nez.BlockScope e, Object a) {
			BeginScope();
			initVal(_sym(), _csym());
			visit(e, a);
			Statement(PFunc("rollbackSymbolTable", local(_sym())));
			EndScope();
			return null;
		}

		@Override
		public Object visitLocalScope(LocalScope e, Object a) {
			BeginScope();
			initVal(_sym(), _csym());
			Statement(PFunc("maskSymbolTable", _symbol(e.tableName)));
			visit(e, a);
			Statement(PFunc("rollbackSymbolTable", local(_sym())));
			EndScope();
			return null;
		}

		@Override
		public Object visitSymbolAction(SymbolAction e, Object a) {
			BeginScope();
			initVal(_pos(), _cpos());
			visit(e, a);
			Statement(PFunc("addSymbol", _symbol(e.tableName), local(_pos())));
			EndScope();
			return null;
		}

		@Override
		public Object visitSymbolPredicate(SymbolPredicate e, Object a) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object visitSymbolMatch(SymbolMatch e, Object a) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object visitSymbolExists(SymbolExists e, Object a) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object visitIf(IfCondition e, Object a) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object visitOn(OnCondition e, Object a) {
			// TODO Auto-generated method stub
			return null;
		}

	}
}
