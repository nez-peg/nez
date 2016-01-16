package nez.tool.parser;

import java.util.HashMap;

import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.FunctionName;
import nez.lang.Grammar;
import nez.lang.Nez;
import nez.lang.Nez.ChoicePrediction;
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
import nez.parser.MemoPoint;
import nez.parser.Parser;
import nez.parser.ParserCode;
import nez.parser.ParserStrategy;
import nez.util.ConsoleUtils;
import nez.util.FileBuilder;
import nez.util.StringUtils;
import nez.util.UList;

public abstract class AbstractParserGenerator implements SourceGenerator {
	protected Parser parser;
	protected ParserStrategy strategy;
	protected ParserCode<?> code;
	protected String path;
	protected FileBuilder file;
	//
	protected TypestateAnalyzer typeState = Typestate.newAnalyzer();
	protected StateAnalyzer symbolState = Symbolstate.newAnalyzer();

	protected boolean verboseMode = true;

	@Override
	public final void init(Grammar g, Parser parser, String path) {
		this.parser = parser;
		this.strategy = parser.getParserStrategy();
		this.code = parser.getParserCode();
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
		this.checkSymbol(g);
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
		file.write(_function(getType("parse")));
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

	protected void Line(String stmt) {
		file.writeIndent(stmt);
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

	protected void Verbose(String stmt) {
		if (verboseMode) {
			file.writeIndent(_comment() + " " + stmt);
		}
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

	protected void Switch(String c) {
		Line("switch(" + c + ")");
		Begin();
	}

	protected void EndSwitch() {
		End();
	}

	protected void Case(String n) {
		Line("case " + n + ": ");
	}

	protected void EndCase() {
	}

	protected void VarDecl(String t, String v, String expr) {
		if (t == null) {
			VarAssign(v, expr);
		} else {
			file.writeIndent(t + " " + v + " = " + expr);
			Semicolon();
		}
	}

	protected void VarAssign(String v, String expr) {
		file.writeIndent(v + " = " + expr);
		Semicolon();
	}

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

	protected String _null() {
		return "null";
	}

	protected String _not(String expr) {
		return "!" + expr;
	}

	/* Expression */

	protected String _function(String type) {
		return "private static " + type;
	}

	protected String _argument(String var, String type) {
		if (type == null) {
			return var;
		}
		return type + " " + var;
	}

	protected String _argument() {
		return _argument(_state(), getType(_state()));
	}

	protected String _funccall(String name) {
		return name + "(" + _state() + ")";
	}

	protected String _state() {
		return "c";
	}

	protected String _beginArray() {
		return "{";
	}

	protected String _endArray() {
		return "}";
	}

	protected String _beginBlock() {
		return " {";
	}

	protected String _endBlock() {
		return "}";
	}

	protected String _field(String o, String name) {
		return o + "." + name;
	}

	protected String _int(int n) {
		return "" + n;
	}

	protected String _byte(int ch) {
		// if (ch < 128 && (!Character.isISOControl(ch))) {
		// return "'" + (char) ch + "'";
		// }
		return "" + ch;
	}

	protected String _bytes(byte[] utf8) {
		return StringUtils.quoteString('"', new String(utf8), '"');
	}

	protected String _funcname(String uname) {
		return "p" + uname.replace("!", "NoT");
	}

	private String _getarray(String array, String c) {
		return array + "[" + c + "]";
	}

	protected String ParserFunc(String name, String... args) {
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

	protected String _indexMap() {
		return "indexMap";
	}

	protected String _byteMap() {
		return "byteMap";
	}

	protected String _byteSeq() {
		return "byteSeq";
	}

	protected HashMap<String, String> typeMap = new HashMap<>();
	protected HashMap<String, String> symbolMap = new HashMap<>();

	protected void addType(String name, String type) {
		typeMap.put(name, type);
	}

	protected String getType(String name) {
		return typeMap.get(name);
	}

	protected abstract void initTypeMap();

	/* Symbols */

	private String _symbol(boolean[] b) {
		String key = StringUtils.stringfyBitmap(b);
		return symbolMap.get(key);
	}

	protected String _symbol(Symbol s) {
		if (s == null) {
			return _null();
		}
		String sym = symbolMap.get(s.getSymbol());
		if (sym != null) {
			return sym;
		}
		return s == null ? _null() : StringUtils.quoteString('"', s.toString(), '"');
	}

	protected String _symbol(ChoicePrediction p) {
		return symbolMap.get(p.indexMap.toString());
	}

	protected String _string(String s) {
		return StringUtils.quoteString('"', s, '"');
	}

	private void checkSymbol(Grammar g) {
		for (Production p : g) {
			checkSymbol(p.getExpression());
		}
	}

	private void checkSymbol(Expression e) {
		if (e instanceof Nez.ByteSet) {
			boolean[] b = ((Nez.ByteSet) e).byteMap;
			String key = StringUtils.stringfyBitmap(b);
			String sym = symbolMap.get(key);
			if (sym == null) {
				sym = _byteMap() + symbolMap.size();
				symbolMap.put(key, sym);
				DeclSymbolBitmap(sym, b);
			}
		}
		if (e instanceof Nez.Tag) {
			DeclSymbol(((Nez.Tag) e).tag);
		}
		if (e instanceof Nez.LinkTree) {
			DeclSymbol(((Nez.LinkTree) e).label);
		}
		if (e instanceof Nez.FoldTree) {
			DeclSymbol(((Nez.FoldTree) e).label);
		}
		if (e instanceof Nez.SymbolFunction) {
			DeclSymbol(((Nez.SymbolFunction) e).tableName);
		}
		if (e instanceof Nez.Choice && ((Nez.Choice) e).predicted != null) {
			DeclSymbol(((Nez.Choice) e).predicted);
		}
		for (Expression sub : e) {
			checkSymbol(sub);
		}
	}

	protected void DeclSymbol(String name, String value) {
		Statement("private final static Symbol " + name + " = Symbol.unique(" + StringUtils.quoteString('"', value, '"') + ")");
	}

	void DeclConst(String type, String name, String val) {
		if (type == null) {
			Statement("private final static " + name + " = " + val);
		} else {
			Statement("private final static " + type + " " + name + " = " + val);
		}
	}

	void DeclSymbolBitmap(String name, boolean b[]) {
		StringBuilder sb = new StringBuilder();
		String type = getType(_byteMap());
		sb.append(_beginArray());
		for (int i = 0; i < 256; i++) {
			if (b[i]) {
				sb.append(_true());
			} else {
				sb.append(_false());
			}
			if (i < 255) {
				sb.append(",");
			}
		}
		sb.append(_endArray());
		Verbose(StringUtils.stringfyCharacterClass(b));
		DeclConst(type, name, sb.toString());
	}

	void DeclSymbol(Symbol s) {
		if (s != null) {
			String key = s.getSymbol();
			String val = symbolMap.get(key);
			if (val == null) {
				symbolMap.put(key, "_" + key);
				DeclSymbol("_" + key, key);
			}
		}
	}

	void DeclSymbol(Nez.ChoicePrediction p) {
		String key = p.indexMap.toString();
		String val = symbolMap.get(key);
		if (val == null) {
			String name = _indexMap() + symbolMap.size();
			symbolMap.put(key, name);
			DeclSymbolIndexMap(getType(_indexMap()), name, p.indexMap);
		}
	}

	private void DeclSymbolIndexMap(String type, String name, byte b[]) {
		StringBuilder sb = new StringBuilder();
		sb.append(_beginArray());
		for (int i = 0; i < 256; i++) {
			sb.append(_int(b[i]));
			if (i < 255) {
				sb.append(",");
			}
		}
		sb.append(_endArray());
		DeclConst(type, name, sb.toString());
	}

	class ParserGeneratorVisitor extends Expression.Visitor {

		HashMap<String, Expression> funcMap = new HashMap<>();
		HashMap<String, String> exprMap = new HashMap<>();
		UList<String> funcList = new UList<String>(new String[128]);
		HashMap<String, Integer> memoPointMap = new HashMap<>();
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
				MemoPoint memoPoint = code.getMemoPoint(uname);
				if (memoPoint != null) {
					memoPointMap.put(key, memoPoint.id);
				}
			}
			return _funccall(key);
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
			return _funccall(f);
		}

		private void generateFunction(String name, Expression e) {
			Integer memoPoint = memoPointMap.get(name);
			Verbose(e.toString());
			BeginFunc(name);
			{
				if (memoPoint != null) {
					If(ParserFunc("lookupMemo", _int(memoPoint)));
					Succ();
					EndIf();
					String pos = savePos();
					visit(e, null);
					Statement(ParserFunc("memoSucc", _int(memoPoint), pos));
				} else {
					visit(e, null);
				}
				Succ();
			}
			EndFunc();
		}

		void initFunc(Expression e) {

		}

		int nested = 0;

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

		private String savePos() {
			initVal(_pos(), _cpos());
			return local(_pos());
		}

		private void backPos() {
			VarAssign(_cpos(), local(_pos()));
		}

		private String saveTree() {
			initVal(_left(), _cleft());
			return local(_left());
		}

		private void backTree() {
			VarAssign(_cleft(), local(_left()));
		}

		private void saveLog() {
			initVal(_log(), ParserFunc("saveLog"));
		}

		private void backLog() {
			Statement(ParserFunc("backLog", local(_log())));
		}

		private void saveSymbol() {
			initVal(_sym(), ParserFunc("saveSymbolPoint"));
		}

		private void backSymbol() {
			Statement(ParserFunc("backSymbolPoint", local(_sym())));
		}

		private void SavePoint(Expression inner) {
			savePos();
			if (typeState.inferTypestate(inner) != Typestate.Unit) {
				saveTree();
				saveLog();
			}
			if (symbolState.isStateful(inner)) {
				saveSymbol();
			}
		}

		private void Backtrack(Expression inner) {
			backPos();
			if (typeState.inferTypestate(inner) != Typestate.Unit) {
				backTree();
				backLog();
			}
			if (symbolState.isStateful(inner)) {
				backSymbol();
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
			If(ParserFunc("read"), _noteq(), _byte(e.byteChar));
			{
				Fail();
			}
			EndIf();
			return null;
		}

		@Override
		public Object visitByteSet(Nez.ByteSet e, Object a) {
			If(_not(MatchByteArray(e.byteMap, ParserFunc("read"))));
			{
				Fail();
			}
			EndIf();
			return null;
		}

		private String MatchByteArray(boolean[] byteMap, String c) {
			return _getarray(_symbol(byteMap), c);
		}

		@Override
		public Object visitAny(Nez.Any e, Object a) {
			if (strategy.BinaryGrammar) {
				Statement(ParserFunc("move", "1"));
				If(ParserFunc("eof"));
				{
					Fail();
				}
				EndIf();
			} else {
				If(ParserFunc("read"), _eq(), "0");
				{
					Fail();
				}
				EndIf();
			}
			return null;
		}

		@Override
		public Object visitMultiByte(Nez.MultiByte e, Object a) {
			If(_not(ParserFunc("match", _bytes(e.byteSeq), _int(e.byteSeq.length))));
			{
				Fail();
			}
			EndIf();
			return null;
		}

		@Override
		public Object visitPair(Nez.Pair e, Object a) {
			for (Expression sub : e) {
				visit(sub, a);
			}
			return null;
		}

		@Override
		public Object visitSequence(Nez.Sequence e, Object a) {
			for (Expression sub : e) {
				visit(sub, a);
			}
			return null;
		}

		@Override
		public Object visitChoice(Nez.Choice e, Object a) {
			if (e.predicted != null) {
				visitPredicatedChoice(e, e.predicted);
			} else {
				BeginScope();
				initVal(_unchoiced(), _true());
				for (Expression sub : e) {
					String f = makeFuncCall(sub);
					If(local(_unchoiced()));
					{
						SavePoint(sub);
						Verbose(sub.toString());
						If(f);
						{
							VarAssign(local(_unchoiced()), _false());
						}
						Else();
						{
							Backtrack(sub);
						}
						EndIf();
					}
					EndIf();
				}
				If(local(_unchoiced()));
				{
					Fail();
				}
				EndIf();
				EndScope();
			}
			return null;
		}

		private void visitPredicatedChoice(Nez.Choice choice, ChoicePrediction p) {
			Switch(_getarray(_symbol(p), ParserFunc("prefetch")));
			Case("0");
			Fail();
			for (int i = 0; i < choice.size(); i++) {
				Case(_int(i + 1));
				Expression sub = choice.get(i);
				String f = makeFuncCall(sub);
				if (p.striped[i]) {
					Verbose(". " + sub);
					Statement(ParserFunc("move", "1"));
				} else {
					Verbose(sub.toString());
				}
				Return(f);
				EndCase();
			}
			EndSwitch();
		}

		@Override
		public Object visitOption(Nez.Option e, Object a) {
			Expression sub = e.get(0);
			if (!tryOptionOptimization(sub)) {
				String f = makeFuncCall(sub);
				SavePoint(sub);
				Verbose(sub.toString());
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
					SavePoint(sub);
					Verbose(sub.toString());
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
				savePos();
				Verbose(sub.toString());
				If(_not(f));
				{
					Fail();
				}
				EndIf();
				backPos();
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
				SavePoint(sub);
				Verbose(sub.toString());
				If(f);
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
					If(ParserFunc("prefetch"), _eq(), _byte(e.byteChar));
					{
						Statement(ParserFunc("move", "1"));
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.ByteSet) {
					Nez.ByteSet e = (Nez.ByteSet) inner;
					If(MatchByteArray(e.byteMap, ParserFunc("prefetch")));
					{
						Statement(ParserFunc("move", "1"));
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.MultiByte) {
					Nez.MultiByte e = (Nez.MultiByte) inner;
					Statement(ParserFunc("match", _bytes(e.byteSeq), _int(e.byteSeq.length)));
					return true;
				}
				if (inner instanceof Nez.Any) {
					// Nez.Any e = (Nez.Any) inner;
					If(_not(ParserFunc("eof")));
					{
						Statement(ParserFunc("move", "1"));
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
					While(_op(ParserFunc("prefetch"), _eq(), _byte(e.byteChar)));
					{
						Statement(ParserFunc("move", "1"));
					}
					EndWhile();
					return true;
				}
				if (inner instanceof Nez.ByteSet) {
					Nez.ByteSet e = (Nez.ByteSet) inner;
					While(MatchByteArray(e.byteMap, ParserFunc("prefetch")));
					{
						Statement(ParserFunc("move", "1"));
					}
					EndWhile();
					return true;
				}
				if (inner instanceof Nez.MultiByte) {
					Nez.MultiByte e = (Nez.MultiByte) inner;
					While(ParserFunc("match", _bytes(e.byteSeq), _int(e.byteSeq.length)));
					{
						EmptyStatement();
					}
					EndWhile();
					return true;
				}
				if (inner instanceof Nez.Any) {
					// Nez.Any e = (Nez.Any) inner;
					While(_not(ParserFunc("eof")));
					{
						Statement(ParserFunc("move", "1"));
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
					If(ParserFunc("prefetch"), _noteq(), _byte(e.byteChar));
					{
						Fail();
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.ByteSet) {
					Nez.ByteSet e = (Nez.ByteSet) inner;
					If(_not(MatchByteArray(e.byteMap, ParserFunc("prefetch"))));
					{
						Fail();
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.MultiByte) {
					Nez.MultiByte e = (Nez.MultiByte) inner;
					If(_not(ParserFunc("match", _bytes(e.byteSeq), _int(e.byteSeq.length))));
					{
						Fail();
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.Any) {
					// Nez.Any e = (Nez.Any) inner;
					If(ParserFunc("eof"));
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
					If(ParserFunc("prefetch"), _eq(), _byte(e.byteChar));
					{
						Fail();
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.ByteSet) {
					Nez.ByteSet e = (Nez.ByteSet) inner;
					If(MatchByteArray(e.byteMap, ParserFunc("prefetch")));
					{
						Fail();
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.MultiByte) {
					Nez.MultiByte e = (Nez.MultiByte) inner;
					If(ParserFunc("match", _bytes(e.byteSeq), _int(e.byteSeq.length)));
					{
						Fail();
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.Any) {
					// Nez.Any e = (Nez.Any) inner;
					If(_not(ParserFunc("eof")));
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
			Statement(ParserFunc("beginTree", _int(e.shift)));
			return null;
		}

		@Override
		public Object visitEndTree(Nez.EndTree e, Object a) {
			Statement(ParserFunc("endTree", _null(), _null(), _int(e.shift)));
			return null;
		}

		@Override
		public Object visitFoldTree(Nez.FoldTree e, Object a) {
			Statement(ParserFunc("foldTree", _int(e.shift), _symbol(e.label)));
			return null;
		}

		@Override
		public Object visitLinkTree(Nez.LinkTree e, Object a) {
			BeginScope();
			String tree = saveTree();
			visit(e.get(0), a);
			Statement(ParserFunc("linkTree", tree, _symbol(e.label)));
			backTree();
			EndScope();
			return null;
		}

		@Override
		public Object visitTag(Nez.Tag e, Object a) {
			Statement(ParserFunc("tagTree", _symbol(e.tag)));
			return null;
		}

		@Override
		public Object visitReplace(Nez.Replace e, Object a) {
			Statement(ParserFunc("valueTree", _string(e.value)));
			return null;
		}

		@Override
		public Object visitDetree(Nez.Detree e, Object a) {
			BeginScope();
			saveTree();
			saveLog();
			visit(e, a);
			backTree();
			backLog();
			EndScope();
			return null;
		}

		@Override
		public Object visitBlockScope(Nez.BlockScope e, Object a) {
			BeginScope();
			saveSymbol();
			visit(e, a);
			backSymbol();
			EndScope();
			return null;
		}

		@Override
		public Object visitLocalScope(LocalScope e, Object a) {
			BeginScope();
			saveSymbol();
			Statement(ParserFunc("maskSymbolTable", _symbol(e.tableName)));
			visit(e, a);
			backSymbol();
			EndScope();
			return null;
		}

		@Override
		public Object visitSymbolAction(SymbolAction e, Object a) {
			BeginScope();
			String pos = savePos();
			visit(e, a);
			Statement(ParserFunc("addSymbol", _symbol(e.tableName), pos));
			EndScope();
			return null;
		}

		@Override
		public Object visitSymbolPredicate(SymbolPredicate e, Object a) {
			BeginScope();
			String pos = savePos();
			visit(e, a);
			if (e.op == FunctionName.is) {
				Statement(ParserFunc("equals", _symbol(e.tableName), pos));
			} else {
				Statement(ParserFunc("contains", _symbol(e.tableName), pos));
			}
			EndScope();
			return null;
		}

		@Override
		public Object visitSymbolMatch(SymbolMatch e, Object a) {
			If(_not(ParserFunc("match", ParserFunc("getSymbol", _symbol(e.tableName)))));
			{
				Fail();
			}
			EndIf();
			return null;
		}

		@Override
		public Object visitSymbolExists(SymbolExists e, Object a) {
			if (e.symbol == null) {
				If(_not(ParserFunc("exists", _symbol(e.tableName))));
				{
					Fail();
				}
				EndIf();
			} else {
				If(_not(ParserFunc("existsSymbol", _symbol(e.tableName), _string(e.symbol))));
				{
					Fail();
				}
				EndIf();
			}
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
