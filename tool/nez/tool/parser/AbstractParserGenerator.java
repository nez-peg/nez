package nez.tool.parser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.FunctionName;
import nez.lang.Grammar;
import nez.lang.Nez;
import nez.lang.Nez.ChoicePrediction;
import nez.lang.Nez.IfCondition;
import nez.lang.Nez.Label;
import nez.lang.Nez.LocalScope;
import nez.lang.Nez.OnCondition;
import nez.lang.Nez.SymbolAction;
import nez.lang.Nez.SymbolExists;
import nez.lang.Nez.SymbolMatch;
import nez.lang.Nez.SymbolPredicate;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.lang.SymbolDependency;
import nez.lang.SymbolDependency.SymbolDependencyAnalyzer;
import nez.lang.SymbolMutation;
import nez.lang.SymbolMutation.SymbolMutationAnalyzer;
import nez.lang.Typestate;
import nez.lang.Typestate.TypestateAnalyzer;
import nez.parser.MemoPoint;
import nez.parser.Parser;
import nez.parser.ParserCode;
import nez.parser.ParserStrategy;
import nez.parser.io.CommonSource;
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
	protected SymbolMutationAnalyzer symbolMutation = SymbolMutation.newAnalyzer();
	protected SymbolDependencyAnalyzer symbolDeps = SymbolDependency.newAnalyzer();

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

	protected final void ImportFile(String path) {
		try {
			InputStream s = CommonSource.class.getResourceAsStream(path);
			BufferedReader reader = new BufferedReader(new InputStreamReader(s));
			String line = null;
			while ((line = reader.readLine()) != null) {
				file.writeIndent(line);
			}
			reader.close();
		} catch (Exception e) {
			ConsoleUtils.exit(1, "cannot load " + path + "; " + e);
		}
	}

	protected abstract String getFileExtension();

	protected abstract void generateHeader(Grammar g);

	protected abstract void generateFooter(Grammar g);

	@Override
	public void generate() {
		Grammar g = this.parser.getGrammar();
		this.generateHeader(g);
		SymbolVisitor symbolChecker = new SymbolVisitor();
		symbolChecker.check(g);
		this.generate(g);
		this.generateFooter(g);
		file.writeNewLine();
		file.flush();
	}

	public void generate(Grammar g) {
		ParserGeneratorVisitor gen = new ParserGeneratorVisitor();
		gen.generate(g.getStartProduction(), "parse");
	}

	protected void Verbose(String stmt) {
		if (verboseMode) {
			file.writeIndent(_Comment() + " " + stmt);
		}
	}

	protected String _funcname(Production p) {
		return _funcname(p.getUniqueName());
	}

	protected String _funcname(String uname) {
		return "p" + uname.replace("!", "NOT").replace("~", "_").replace("&", "AND");
	}

	/* Variable */

	protected HashMap<String, String> typeMap = new HashMap<>();

	protected abstract void initTypeMap();

	protected void addType(String name, String type) {
		typeMap.put(name, type);
	}

	protected String getType(String name) {
		return typeMap.get(name);
	}

	/* Symbols */

	protected HashMap<String, String> symbolMap = new HashMap<>();

	private String key(boolean[] b) {
		return StringUtils.stringfyBitmap(b);
	}

	private String key(Symbol s) {
		return "_" + s.getSymbol();
	}

	private String key(String s) {
		return s;
	}

	private String key(byte[] indexMap) {
		StringBuilder sb = new StringBuilder();
		for (byte c : indexMap) {
			sb.append(c);
			sb.append(",");
		}
		return sb.toString();
	}

	private String _symbol(boolean[] b) {
		return symbolMap.get(key(b));
	}

	protected String _symbol(Symbol s) {
		if (s == null) {
			return _Null();
		}
		String sym = symbolMap.get(key(s));
		if (sym != null) {
			return sym;
		}
		return StringUtils.quoteString('"', s.toString(), '"');
	}

	protected String _symbol(String s) {
		if (s == null) {
			return _Null();
		}
		String sym = symbolMap.get(key(s));
		if (sym != null) {
			return sym;
		}
		return StringUtils.quoteString('"', s, '"');
	}

	protected String _bytes(byte[] utf8) {
		return _symbol(new String(utf8));
	}

	protected String _symbol(ChoicePrediction p) {
		return symbolMap.get(key(p.indexMap));
	}

	protected void DeclSymbol(String name, boolean b[]) {
		StringBuilder sb = new StringBuilder();
		String type = getType(_byteSet_());
		sb.append(_BeginArray());
		for (int i = 0; i < 256; i++) {
			if (b[i]) {
				sb.append(_True());
			} else {
				sb.append(_False());
			}
			if (i < 255) {
				sb.append(",");
			}
		}
		sb.append(_EndArray());
		Verbose(StringUtils.stringfyCharacterClass(b));
		ConstDecl(type, name, sb.toString());
	}

	protected void DeclSymbol(String name, String value) {
		ConstDecl("byte[]", name, "toUTF8(" + StringUtils.quoteString('"', value.toString(), '"') + ")");
	}

	protected void DeclSymbol(String name, Symbol value) {
		ConstDecl("Symbol", name, "Symbol.unique(" + StringUtils.quoteString('"', value.toString(), '"') + ")");
	}

	protected void DeclSymbol(String name, byte b[]) {
		StringBuilder sb = new StringBuilder();
		sb.append(_BeginArray());
		for (int i = 0; i < 256; i++) {
			sb.append(_int(b[i]));
			if (i < 255) {
				sb.append(",");
			}
		}
		sb.append(_EndArray());
		ConstDecl(getType(_indexMap_()), name, sb.toString());
	}

	class SymbolVisitor extends Expression.Visitor {

		void decl(boolean[] b) {
			String key = key(b);
			String sym = symbolMap.get(key);
			if (sym == null) {
				sym = _byteSet_() + symbolMap.size();
				symbolMap.put(key, sym);
				DeclSymbol(sym, b);
			}
		}

		void decl(Symbol s) {
			if (s != null) {
				String key = key(s);
				String val = symbolMap.get(key);
				if (val == null) {
					symbolMap.put(key, key);
					DeclSymbol(key, s);
				}
			}
		}

		void decl(String s) {
			if (s != null) {
				String key = key(s);
				String val = symbolMap.get(key);
				if (val == null) {
					String name = _utf8_() + symbolMap.size();
					symbolMap.put(key, name);
					DeclSymbol(name, s);
				}
			}
		}

		void decl(Nez.ChoicePrediction p) {
			String key = key(p.indexMap);
			String val = symbolMap.get(key);
			if (val == null) {
				String name = _indexMap_() + symbolMap.size();
				symbolMap.put(key, name);
				DeclSymbol(name, p.indexMap);
			}
		}

		private Object check(Grammar g) {
			for (Production p : g) {
				p.getExpression().visit(this, null);
			}
			return null;
		}

		private Object check(Expression e) {
			for (Expression sub : e) {
				sub.visit(this, null);
			}
			return null;
		}

		@Override
		public Object visitNonTerminal(NonTerminal e, Object a) {
			return check(e);
		}

		@Override
		public Object visitEmpty(Nez.Empty e, Object a) {
			return check(e);
		}

		@Override
		public Object visitFail(Nez.Fail e, Object a) {
			return check(e);
		}

		@Override
		public Object visitByte(Nez.Byte e, Object a) {
			return check(e);
		}

		@Override
		public Object visitByteSet(Nez.ByteSet e, Object a) {
			decl(e.byteMap);
			return check(e);
		}

		@Override
		public Object visitAny(Nez.Any e, Object a) {
			return check(e);
		}

		@Override
		public Object visitMultiByte(Nez.MultiByte e, Object a) {
			decl(new String(e.byteSeq));
			return check(e);
		}

		@Override
		public Object visitPair(Nez.Pair e, Object a) {
			return check(e);
		}

		@Override
		public Object visitSequence(Nez.Sequence e, Object a) {
			return check(e);
		}

		@Override
		public Object visitChoice(Nez.Choice e, Object a) {
			if (e.predicted != null) {
				decl(e.predicted);
			}
			return check(e);
		}

		@Override
		public Object visitOption(Nez.Option e, Object a) {
			return check(e);
		}

		@Override
		public Object visitZeroMore(Nez.ZeroMore e, Object a) {
			return check(e);
		}

		@Override
		public Object visitOneMore(Nez.OneMore e, Object a) {
			return check(e);
		}

		@Override
		public Object visitAnd(Nez.And e, Object a) {
			return check(e);
		}

		@Override
		public Object visitNot(Nez.Not e, Object a) {
			return check(e);
		}

		@Override
		public Object visitBeginTree(Nez.BeginTree e, Object a) {
			return check(e);
		}

		@Override
		public Object visitEndTree(Nez.EndTree e, Object a) {
			decl(e.tag);
			decl(e.value);
			return check(e);
		}

		@Override
		public Object visitFoldTree(Nez.FoldTree e, Object a) {
			decl(e.label);
			return check(e);
		}

		@Override
		public Object visitLinkTree(Nez.LinkTree e, Object a) {
			decl(e.label);
			return check(e);
		}

		@Override
		public Object visitTag(Nez.Tag e, Object a) {
			decl(e.tag);
			return check(e);
		}

		@Override
		public Object visitReplace(Nez.Replace e, Object a) {
			decl(e.value);
			return check(e);
		}

		@Override
		public Object visitDetree(Nez.Detree e, Object a) {
			return check(e);
		}

		@Override
		public Object visitBlockScope(Nez.BlockScope e, Object a) {
			return check(e);
		}

		@Override
		public Object visitLocalScope(LocalScope e, Object a) {
			return check(e);
		}

		@Override
		public Object visitSymbolAction(SymbolAction e, Object a) {
			decl(e.tableName);
			return check(e);
		}

		@Override
		public Object visitSymbolPredicate(SymbolPredicate e, Object a) {
			decl(e.tableName);
			return check(e);
		}

		@Override
		public Object visitSymbolMatch(SymbolMatch e, Object a) {
			decl(e.tableName);
			return check(e);
		}

		@Override
		public Object visitSymbolExists(SymbolExists e, Object a) {
			decl(e.tableName);
			decl(e.symbol);
			return check(e);
		}

		@Override
		public Object visitScan(Nez.Scan e, Object a) {
			return check(e);
		}

		@Override
		public Object visitRepeat(Nez.Repeat e, Object a) {
			return check(e);
		}

		@Override
		public Object visitIf(IfCondition e, Object a) {
			return check(e);
		}

		@Override
		public Object visitOn(OnCondition e, Object a) {
			return check(e);
		}

		@Override
		public Object visitLabel(Label e, Object a) {
			return check(e);
		}
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
					String memoLookup = "memoLookupStateTree";
					String memoSucc = "memoStateTreeSucc";
					String memoFail = "memoStateFail";
					if (!typeState.isTree(e)) {
						memoLookup = memoLookup.replace("Tree", "");
						memoSucc = memoSucc.replace("Tree", "");
						memoFail = memoFail.replace("Tree", "");
					}
					if (!strategy.StatefulPackratParsing || !symbolDeps.isDependent(e)) {
						memoLookup = memoLookup.replace("State", "");
						memoSucc = memoSucc.replace("State", "");
						memoFail = memoFail.replace("State", "");
					}
					InitVal("memo", _Func(memoLookup, _int(memoPoint)));
					If("memo", _Eq(), "0");
					{
						String f = makeFuncCall(e);
						String ppos = SavePoint(e);
						If(f);
						{
							Statement(_Func(memoSucc, _int(memoPoint), ppos));
							Succ();
						}
						Else();
						{
							Backtrack(e);
							Statement(_Func(memoFail, _int(memoPoint)));
							Fail();
						}
						EndIf();
					}
					EndIf();
					Return(_Binary("memo", _Eq(), "1"));
				} else {
					visit(e, null);
					Succ();
				}
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
				BeginLocalScope();
			}
		}

		protected void EndScope() {
			if (nested > 0) {
				EndLocalScope();
			}
		}

		private String local(String name) {
			return nested > 0 ? name + nested : name;
		}

		private void InitVal(String name, String expr) {
			String type = getType(name);
			VarDecl(type, local(name), expr);
		}

		private String savePos() {
			InitVal(_pos_(), _cpos_());
			return local(_pos_());
		}

		private void backPos() {
			VarAssign(_cpos_(), local(_pos_()));
		}

		private String saveTree() {
			InitVal(_left_(), _cleft_());
			return local(_left_());
		}

		private void backTree() {
			VarAssign(_cleft_(), local(_left_()));
		}

		private void saveLog() {
			InitVal(_log_(), _Func("saveLog"));
		}

		private void backLog() {
			Statement(_Func("backLog", local(_log_())));
		}

		private void saveSymbol() {
			InitVal(_sym_(), _Func("saveSymbolPoint"));
		}

		private void backSymbol() {
			Statement(_Func("backSymbolPoint", local(_sym_())));
		}

		private String SavePoint(Expression inner) {
			String pos = savePos();
			if (typeState.inferTypestate(inner) != Typestate.Unit) {
				saveTree();
				saveLog();
			}
			if (symbolMutation.isMutated(inner)) {
				saveSymbol();
			}
			return pos;
		}

		private void Backtrack(Expression inner) {
			backPos();
			if (typeState.inferTypestate(inner) != Typestate.Unit) {
				backTree();
				backLog();
			}
			if (symbolMutation.isMutated(inner)) {
				backSymbol();
			}
		}

		@Override
		public Object visitNonTerminal(NonTerminal e, Object a) {
			String f = makeFuncCall(e.getUniqueName(), e.deReference());
			If(_Not(f));
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
			If(_Func("read"), _NotEq(), _byte(e.byteChar));
			{
				Fail();
			}
			EndIf();
			return null;
		}

		@Override
		public Object visitByteSet(Nez.ByteSet e, Object a) {
			If(_Not(MatchByteArray(e.byteMap, _Func("read"))));
			{
				Fail();
			}
			EndIf();
			return null;
		}

		private String MatchByteArray(boolean[] byteMap, String c) {
			return _GetArray(_symbol(byteMap), c);
		}

		@Override
		public Object visitAny(Nez.Any e, Object a) {
			if (strategy.BinaryGrammar) {
				Statement(_Func("move", "1"));
				If(_Func("eof"));
				{
					Fail();
				}
				EndIf();
			} else {
				If(_Func("read"), _Eq(), "0");
				{
					Fail();
				}
				EndIf();
			}
			return null;
		}

		@Override
		public Object visitMultiByte(Nez.MultiByte e, Object a) {
			If(_Not(_Match(e.byteSeq)));
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
				InitVal(_unchoiced_(), _True());
				for (Expression sub : e) {
					String f = makeFuncCall(sub);
					If(local(_unchoiced_()));
					{
						SavePoint(sub);
						Verbose(sub.toString());
						If(f);
						{
							VarAssign(local(_unchoiced_()), _False());
						}
						Else();
						{
							Backtrack(sub);
						}
						EndIf();
					}
					EndIf();
				}
				If(local(_unchoiced_()));
				{
					Fail();
				}
				EndIf();
				EndScope();
			}
			return null;
		}

		private void visitPredicatedChoice(Nez.Choice choice, ChoicePrediction p) {
			Switch(_GetArray(_symbol(p), _Func("prefetch")));
			Case("0");
			Fail();
			for (int i = 0; i < choice.size(); i++) {
				Case(_int(i + 1));
				Expression sub = choice.get(i);
				String f = makeFuncCall(sub);
				if (p.striped[i]) {
					Verbose(". " + sub);
					Statement(_Func("move", "1"));
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
				If(_Not(f));
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
				While(_True());
				{
					SavePoint(sub);
					Verbose(sub.toString());
					If(_Not(f));
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
				If(_Not(f));
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
					If(_Func("prefetch"), _Eq(), _byte(e.byteChar));
					{
						Statement(_Func("move", "1"));
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.ByteSet) {
					Nez.ByteSet e = (Nez.ByteSet) inner;
					If(MatchByteArray(e.byteMap, _Func("prefetch")));
					{
						Statement(_Func("move", "1"));
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.MultiByte) {
					Nez.MultiByte e = (Nez.MultiByte) inner;
					Statement(_Match(e.byteSeq));
					return true;
				}
				if (inner instanceof Nez.Any) {
					// Nez.Any e = (Nez.Any) inner;
					If(_Not(_Func("eof")));
					{
						Statement(_Func("move", "1"));
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
					While(_Binary(_Func("prefetch"), _Eq(), _byte(e.byteChar)));
					{
						Statement(_Func("move", "1"));
					}
					EndWhile();
					return true;
				}
				if (inner instanceof Nez.ByteSet) {
					Nez.ByteSet e = (Nez.ByteSet) inner;
					While(MatchByteArray(e.byteMap, _Func("prefetch")));
					{
						Statement(_Func("move", "1"));
					}
					EndWhile();
					return true;
				}
				if (inner instanceof Nez.MultiByte) {
					Nez.MultiByte e = (Nez.MultiByte) inner;
					While(_Match(e.byteSeq));
					{
						EmptyStatement();
					}
					EndWhile();
					return true;
				}
				if (inner instanceof Nez.Any) {
					// Nez.Any e = (Nez.Any) inner;
					While(_Not(_Func("eof")));
					{
						Statement(_Func("move", "1"));
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
					If(_Func("prefetch"), _NotEq(), _byte(e.byteChar));
					{
						Fail();
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.ByteSet) {
					Nez.ByteSet e = (Nez.ByteSet) inner;
					If(_Not(MatchByteArray(e.byteMap, _Func("prefetch"))));
					{
						Fail();
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.MultiByte) {
					Nez.MultiByte e = (Nez.MultiByte) inner;
					If(_Not(_Match(e.byteSeq)));
					{
						Fail();
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.Any) {
					// Nez.Any e = (Nez.Any) inner;
					If(_Func("eof"));
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
					If(_Func("prefetch"), _Eq(), _byte(e.byteChar));
					{
						Fail();
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.ByteSet) {
					Nez.ByteSet e = (Nez.ByteSet) inner;
					If(MatchByteArray(e.byteMap, _Func("prefetch")));
					{
						Fail();
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.MultiByte) {
					Nez.MultiByte e = (Nez.MultiByte) inner;
					If(_Match(e.byteSeq));
					{
						Fail();
					}
					EndIf();
					return true;
				}
				if (inner instanceof Nez.Any) {
					// Nez.Any e = (Nez.Any) inner;
					If(_Not(_Func("eof")));
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
			Statement(_Func("beginTree", _int(e.shift)));
			return null;
		}

		@Override
		public Object visitEndTree(Nez.EndTree e, Object a) {
			Statement(_Func("endTree", _Null(), _Null(), _int(e.shift)));
			return null;
		}

		@Override
		public Object visitFoldTree(Nez.FoldTree e, Object a) {
			Statement(_Func("foldTree", _int(e.shift), _symbol(e.label)));
			return null;
		}

		@Override
		public Object visitLinkTree(Nez.LinkTree e, Object a) {
			BeginScope();
			String tree = saveTree();
			visit(e.get(0), a);
			Statement(_Func("linkTree", tree, _symbol(e.label)));
			backTree();
			EndScope();
			return null;
		}

		@Override
		public Object visitTag(Nez.Tag e, Object a) {
			Statement(_Func("tagTree", _symbol(e.tag)));
			return null;
		}

		@Override
		public Object visitReplace(Nez.Replace e, Object a) {
			Statement(_Func("valueTree", _symbol(e.value)));
			return null;
		}

		@Override
		public Object visitDetree(Nez.Detree e, Object a) {
			BeginScope();
			saveTree();
			saveLog();
			visit(e.get(0), a);
			backTree();
			backLog();
			EndScope();
			return null;
		}

		@Override
		public Object visitBlockScope(Nez.BlockScope e, Object a) {
			BeginScope();
			saveSymbol();
			visit(e.get(0), a);
			backSymbol();
			EndScope();
			return null;
		}

		@Override
		public Object visitLocalScope(LocalScope e, Object a) {
			BeginScope();
			saveSymbol();
			Statement(_Func("addSymbolMask", _symbol(e.tableName)));
			visit(e.get(0), a);
			backSymbol();
			EndScope();
			return null;
		}

		@Override
		public Object visitSymbolAction(SymbolAction e, Object a) {
			BeginScope();
			String pos = savePos();
			visit(e.get(0), a);
			Statement(_Func("addSymbol", _symbol(e.tableName), pos));
			EndScope();
			return null;
		}

		@Override
		public Object visitSymbolPredicate(SymbolPredicate e, Object a) {
			BeginScope();
			String pos = savePos();
			visit(e.get(0), a);
			if (e.op == FunctionName.is) {
				Statement(_Func("equals", _symbol(e.tableName), pos));
			} else {
				Statement(_Func("contains", _symbol(e.tableName), pos));
			}
			EndScope();
			return null;
		}

		@Override
		public Object visitSymbolMatch(SymbolMatch e, Object a) {
			If(_Not(_Func("matchSymbol", _symbol(e.tableName))));
			{
				Fail();
			}
			EndIf();
			return null;
		}

		@Override
		public Object visitSymbolExists(SymbolExists e, Object a) {
			if (e.symbol == null) {
				If(_Not(_Func("exists", _symbol(e.tableName))));
				{
					Fail();
				}
				EndIf();
			} else {
				If(_Not(_Func("existsSymbol", _symbol(e.tableName), _symbol(e.symbol))));
				{
					Fail();
				}
				EndIf();
			}
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object visitScan(Nez.Scan e, Object a) {
			BeginScope();
			String ppos = savePos();
			visit(e.get(0), a);
			Statement(_Func("scanCount", ppos, _long(e.mask), _int(e.shift)));
			EndScope();
			return null;
		}

		@Override
		public Object visitRepeat(Nez.Repeat e, Object a) {
			While(_Func("decCount"));
			{
				visit(e.get(0), a);
			}
			EndWhile();
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

		@Override
		public Object visitLabel(Label e, Object a) {
			// TODO Auto-generated method stub
			return null;
		}

	}

	/* Syntax */

	protected String _Comment() {
		return "//";
	}

	protected String _Comment(String c) {
		return "/*" + c + "*/";
	}

	protected String _And() {
		return "&&";
	}

	protected String _Or() {
		return "||";
	}

	protected String _Not(String expr) {
		return "!" + expr;
	}

	protected String _Eq() {
		return "==";
	}

	protected String _NotEq() {
		return "!=";
	}

	protected String _True() {
		return "true";
	}

	protected String _False() {
		return "false";
	}

	protected String _Null() {
		return "null";
	}

	/* Expression */

	private String _GetArray(String array, String c) {
		return array + "[" + c + "]";
	}

	protected String _BeginArray() {
		return "{";
	}

	protected String _EndArray() {
		return "}";
	}

	protected String _BeginBlock() {
		return " {";
	}

	protected String _EndBlock() {
		return "}";
	}

	protected String _Field(String o, String name) {
		return o + "." + name;
	}

	protected String _Func(String name, String... args) {
		StringBuilder sb = new StringBuilder();
		sb.append(_state_());
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

	protected String _Match(byte[] byteSeq) {
		return _Func("match", _bytes(byteSeq));
	}

	protected String _int(int n) {
		return "" + n;
	}

	protected String _long(long n) {
		return "" + n + "L";
	}

	protected String _byte(int ch) {
		// if (ch < 128 && (!Character.isISOControl(ch))) {
		// return "'" + (char) ch + "'";
		// }
		return "" + ch;
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
		return _argument(_state_(), getType(_state_()));
	}

	protected String _funccall(String name) {
		return name + "(" + _state_() + ")";
	}

	/* Statement */

	protected void BeginDecl(String line) {
		file.writeIndent(line);
		Begin();
	}

	protected void EndDecl() {
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

	protected void BeginFunc(String type, String name, String args) {
		file.writeIndent();
		file.write(_function(type));
		file.write(" ");
		file.write(name);
		file.write("(");
		file.write(args);
		file.write(")");
		Begin();
	}

	protected final void BeginFunc(String f, String args) {
		BeginFunc(getType("parse"), f, args);
	}

	protected final void BeginFunc(String f) {
		BeginFunc(getType("parse"), f, _argument());
	}

	protected void EndFunc() {
		End();
	}

	protected void BeginLocalScope() {
		file.writeIndent("{");
		file.incIndent();
	}

	protected void EndLocalScope() {
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
		file.writeIndent(_Comment() + " " + stmt);
	}

	protected void Return(String expr) {
		Statement("return " + expr);
	}

	protected void Succ() {
		Return(_True());
	}

	protected void Fail() {
		Return(_False());
	}

	protected void If(String cond) {
		file.writeIndent("if (");
		file.write(cond);
		file.write(")");
		Begin();
	}

	protected String _Binary(String a, String op, String b) {
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

	protected void VarDecl(String name, String expr) {
		VarDecl(this.getType(name), name, expr);
	}

	protected void VarDecl(String type, String name, String expr) {
		if (name == null) {
			VarAssign(name, expr);
		} else {
			Statement(type + " " + name + " = " + expr);
		}
	}

	protected void VarAssign(String v, String expr) {
		Statement(v + " = " + expr);
	}

	protected void ConstDecl(String type, String name, String val) {
		if (type == null) {
			Statement("private final static " + name + " = " + val);
		} else {
			Statement("private final static " + type + " " + name + " = " + val);
		}
	}

	/* Variables */

	protected String _state_() {
		return "c";
	}

	protected String _pos_() {
		return "pos";
	}

	protected String _cpos_() {
		return _Field(_state_(), "pos");
	}

	protected String _left_() {
		return "left";
	}

	protected String _cleft_() {
		return _Field(_state_(), "left");
	}

	protected String _log_() {
		return "log";
	}

	protected String _clog_() {
		return _Field(_state_(), "log");
	}

	protected String _sym_() {
		return "sym";
	}

	protected String _csym_() {
		return _Field(_state_(), "sym");
	}

	protected String _unchoiced_() {
		return "unchoiced";
	}

	protected String _utf8_() {
		return "u";
	}

	protected String _indexMap_() {
		return "indexMap";
	}

	protected String _byteSet_() {
		return "byteSet";
	}

	protected String _byteSeq_() {
		return "byteSeq";
	}
}
