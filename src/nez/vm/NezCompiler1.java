package nez.vm;

import java.util.HashMap;

import nez.lang.And;
import nez.lang.AnyChar;
import nez.lang.Block;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.Capture;
import nez.lang.Choice;
import nez.lang.DefIndent;
import nez.lang.DefSymbol;
import nez.lang.ExistsSymbol;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.IsIndent;
import nez.lang.IsSymbol;
import nez.lang.Link;
import nez.lang.LocalTable;
import nez.lang.New;
import nez.lang.NonTerminal;
import nez.lang.Not;
import nez.lang.Option;
import nez.lang.Production;
import nez.lang.Repetition;
import nez.lang.Repetition1;
import nez.lang.Replace;
import nez.lang.Sequence;
import nez.lang.Tagging;
import nez.lang.Typestate;
import nez.main.Verbose;
import nez.util.UFlag;
import nez.util.UList;

public class NezCompiler1 extends NezCompiler {

	protected final Instruction commonFailure = new IFail(null);

	public NezCompiler1(int option) {
		super(option);
	}

	protected Expression optimizeLocalProduction(Production p) {
		return GrammarOptimizer.resolveNonTerminal(p.getExpression());
	}

	protected Instruction encodeMemoizingProduction(CodePoint code) {
		return null;
	}
	
	protected void encodeProduction(UList<Instruction> codeList, Production p, Instruction next) {
		String uname = p.getUniqueName();
		CodePoint code = this.codeMap.get(uname);
		if(code != null) {
			code.nonmemoStart = encodeExpression(code.localExpression, next, null/*failjump*/);
			code.start = codeList.size();
			this.layoutCode(codeList, code.nonmemoStart);
			code.end = codeList.size();
			if(code.memoPoint != null) {
				code.memoStart = this.encodeMemoizingProduction(code);
				this.layoutCode(codeList, code.memoStart);
			}
		}
	}

	@Override
	public NezCode compile(Grammar grammar) {
		long t = System.nanoTime();
		initCodeMap(grammar);
		UList<Instruction> codeList = new UList<Instruction>(new Instruction[64]);
		Production start = grammar.getStartProduction();
		this.encodeProduction(codeList, start, new IRet(start));
		for(Production p : grammar.getProductionList()) {
			if(p != start) {
				this.encodeProduction(codeList, p, new IRet(p));
			}
		}
		for(Instruction inst : codeList) {
			if(inst instanceof ICallPush) {
				CodePoint deref = this.codeMap.get(((ICallPush) inst).rule.getUniqueName());
				if(deref == null) {
					Verbose.debug("no deref: " + ((ICallPush) inst).rule.getUniqueName());
				}
				((ICallPush) inst).setResolvedJump(deref.nonmemoStart);
			}
			if(inst instanceof IMemoCall) {
				((IMemoCall) inst).resolveJumpAddress();
			}
		}
		long t2 = System.nanoTime();
		Verbose.printElapsedTime("CompilingTime", t, t2);
		this.codeMap = null;
		return new NezCode(codeList.ArrayValues[0]);
	}


	protected void optimizedUnary(Expression p) {
		Verbose.noticeOptimize("specialization", p);
	}

	protected void optimizedInline(Production p) {
		Verbose.noticeOptimize("inlining", p.getExpression());
	}

	
	
	// encoding

	public Instruction encodeExpression(Expression e, Instruction next, Instruction failjump) {
		return e.encode(this, next, failjump);
	}

	public Instruction encodeAnyChar(AnyChar p, Instruction next, Instruction failjump) {
		return new IAnyChar(p, next);
	}

	public Instruction encodeByteChar(ByteChar p, Instruction next, Instruction failjump) {
		return new IByteChar(p, next);
	}

	public Instruction encodeByteMap(ByteMap p, Instruction next, Instruction failjump) {
		return new IByteMap(p, next);
	}

	public Instruction encodeFail(Expression p) {
//		return new IFail(p);
		return this.commonFailure;
	}

	public Instruction encodeOption(Option p, Instruction next) {
		Instruction pop = new IFailPop(p, next);
		return new IFailPush(p, next, encodeExpression(p.get(0), pop, next));
	}

	public Instruction encodeRepetition(Repetition p, Instruction next) {
		IFailSkip skip = p.possibleInfiniteLoop ? new IFailCheckSkip(p) : new IFailCheckSkip(p);
		Instruction start = encodeExpression(p.get(0), skip, next/*FIXME*/);
		skip.next = start;
		return new IFailPush(p, next, start);
	}

	public Instruction encodeRepetition1(Repetition1 p, Instruction next, Instruction failjump) {
		return encodeExpression(p.get(0), this.encodeRepetition(p, next), failjump);
	}

	public Instruction encodeAnd(And p, Instruction next, Instruction failjump) {
		Instruction inner = encodeExpression(p.get(0), new IPosBack(p, next), failjump);
		return new IPosPush(p, inner);
	}

	public Instruction encodeNot(Not p, Instruction next, Instruction failjump) {
		Instruction fail = new IFailPop(p, new IFail(p));
		return new INotFailPush(p, next, encodeExpression(p.get(0), fail, failjump));
	}

	public Instruction encodeSequence(Sequence p, Instruction next, Instruction failjump) {
		Instruction nextStart = next;
		for(int i = p.size() - 1; i >= 0; i--) {
			Expression e = p.get(i);
			nextStart = encodeExpression(e, nextStart, failjump);
		}
		return nextStart;
	}

	public Instruction encodeChoice(Choice p, Instruction next, Instruction failjump) {
		Instruction nextChoice = encodeExpression(p.get(p.size() - 1), next, failjump);
		for(int i = p.size() - 2; i >= 0; i--) {
			Expression e = p.get(i);
			nextChoice = new IFailPush(e, nextChoice, encodeExpression(e, new IFailPop(e, next), nextChoice));
		}
		return nextChoice;
	}

	public Instruction encodeNonTerminal(NonTerminal p, Instruction next, Instruction failjump) {
		Production r = p.getProduction();
		return new ICallPush(r, next);
	}

	// AST Construction

	public Instruction encodeLink(Link p, Instruction next, Instruction failjump) {
		if(UFlag.is(this.option, Grammar.ASTConstruction)) {
			return new INodePush(p, encodeExpression(p.get(0), new INodeStore(p, next), failjump));
		}
		return encodeExpression(p.get(0), next, failjump);
	}

	public Instruction encodeNew(New p, Instruction next) {
		if(UFlag.is(this.option, Grammar.ASTConstruction)) {
			return p.lefted ? new ILeftNew(p, next) : new INew(p, next);
		}
		return next;
	}

	public Instruction encodeCapture(Capture p, Instruction next) {
		if(UFlag.is(this.option, Grammar.ASTConstruction)) {
			return new ICapture(p, next);
		}
		return next;
	}

	public Instruction encodeTagging(Tagging p, Instruction next) {
		if(UFlag.is(this.option, Grammar.ASTConstruction)) {
			return new ITag(p, next);
		}
		return next;
	}

	public Instruction encodeReplace(Replace p, Instruction next) {
		if(UFlag.is(this.option, Grammar.ASTConstruction)) {
			return new IReplace(p, next);
		}
		return next;
	}

	public Instruction encodeBlock(Block p, Instruction next, Instruction failjump) {
		Instruction failed = new IEndSymbolScope(p, /*fail*/true, null);
		next = new IEndSymbolScope(p, /*fail*/false, next);
		Instruction inner = encodeExpression(p.get(0), next, failjump);
		return new IBeginSymbolScope(p, failed, inner);
	}

	public Instruction encodeLocalTable(LocalTable p, Instruction next, Instruction failjump) {
		Instruction failed = new IEndSymbolScope(p, /*fail*/true, null);
		next = new IEndSymbolScope(p, /*fail*/false, next);
		Instruction inner = encodeExpression(p.get(0), next, failjump);
		return new IBeginLocalScope(p, failed, inner);
	}

	public Instruction encodeDefSymbol(DefSymbol p, Instruction next, Instruction failjump) {
		return new IPosPush(p, encodeExpression(p.get(0), new IDefSymbol(p, next), failjump));
	}

	public Instruction encodeExistsSymbol(ExistsSymbol p, Instruction next, Instruction failjump) {
		return new IExistsSymbol(p, next);
	}

	public Instruction encodeIsSymbol(IsSymbol p, Instruction next, Instruction failjump) {
		if(p.checkLastSymbolOnly) {
			return new IIsSymbol(p, next);
		}
		else {
			return new IPosPush(p, encodeExpression(p.getSymbolExpression(), new IIsaSymbol(p, next), failjump));
		}
	}

	public Instruction encodeDefIndent(DefIndent p, Instruction next, Instruction failjump) {
		return new IDefIndent(p, next);
	}

	public Instruction encodeIsIndent(IsIndent p, Instruction next, Instruction failjump) {
		return new IIsIndent(p, next);
	}

	@Override
	public Instruction encodeExtension(Expression p, Instruction next, Instruction failjump) {
		return next;
	}

}
