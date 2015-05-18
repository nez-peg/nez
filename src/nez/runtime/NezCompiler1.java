package nez.runtime;

import java.util.Arrays;
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
import nez.lang.Factory;
import nez.lang.Grammar;
import nez.lang.IsIndent;
import nez.lang.IsSymbol;
import nez.lang.Link;
import nez.lang.LocalTable;
import nez.lang.New;
import nez.lang.NonTerminal;
import nez.lang.Not;
import nez.lang.Option;
import nez.lang.Prediction;
import nez.lang.Production;
import nez.lang.Repetition;
import nez.lang.Repetition1;
import nez.lang.Replace;
import nez.lang.Sequence;
import nez.lang.Tagging;
import nez.main.Verbose;
import nez.util.StringUtils;
import nez.util.UFlag;
import nez.util.UList;

public class NezCompiler1 extends NezCompiler {

	public NezCompiler1(int option) {
		super(option);
	}

	// encoding

	private Instruction failed = new IFail(null);
	
	public final Instruction encodeExpression(Expression e, Instruction next) {
		return e.encode(this, next);
	}
	
	public final Instruction encodeMatchAny(AnyChar p, Instruction next) {
		return new IAnyChar(p, next);
	}

	public final Instruction encodeByteChar(ByteChar p, Instruction next) {
		return new IByteChar(p, next);
	}

	public final Instruction encodeByteMap(ByteMap p, Instruction next) {
		return new IByteMap(p, next);
	}

	public Instruction encodeFail(Expression p) {
		return new IFail(p);
	}
	
	public final Instruction encodeOption(Option p, Instruction next) {
		Instruction pop = new IFailPop(p, next);
		return new IFailPush(p, next, encodeExpression(p.get(0), pop));
	}
	
	public final Instruction encodeRepetition(Repetition p, Instruction next) {
		IFailSkip skip = p.possibleInfiniteLoop ? new IFailCheckSkip(p) : new IFailCheckSkip(p);
		Instruction start = encodeExpression(p.get(0), skip);
		skip.next = start;
		return new IFailPush(p, next, start);
	}

	public final Instruction encodeRepetition1(Repetition1 p, Instruction next) {
		return encodeExpression(p.get(0), this.encodeRepetition(p, next));
	}

	public final Instruction encodeAnd(And p, Instruction next) {
		Instruction inner = encodeExpression(p.get(0), new IPosBack(p, next));
		return new IPosPush(p, inner);
	}

	public final Instruction encodeNot(Not p, Instruction next) {
		Instruction fail = new IFailPop(p, new IFail(p));
		return new INotFailPush(p, next, encodeExpression(p.get(0), fail));
	}

	public final Instruction encodeSequence(Expression p, Instruction next) {
		Instruction nextStart = next;
		for(int i = p.size() -1; i >= 0; i--) {
			Expression e = p.get(i);
			nextStart = encodeExpression(e, nextStart);
		}
		return nextStart;
	}

	public final Instruction encodeChoice(Choice p, Instruction next) {
		Instruction nextChoice = encodeExpression(p.get(p.size()-1), next);
		for(int i = p.size() -2; i >= 0; i--) {
			Expression e = p.get(i);
			nextChoice = new IFailPush(e, nextChoice, encodeExpression(e, new IFailPop(e, next)));
		}
		return nextChoice;
	}

	public final Instruction encodeNonTerminal(NonTerminal p, Instruction next) {
		Production r = p.getProduction();
//		Expression pp = p.optimize(option);
//		if(pp instanceof ByteChar || pp instanceof ByteMap || pp instanceof AnyChar) {
//			Verbose.noticeOptimize("Inlining", p, pp);
//			return encodeExpression(pp, next);
//		}
//		if(r.isInline() && UFlag.is(option, Grammar.Inlining)) {
//			Verbose.noticeOptimize("Inlining", p, r.getExpression());
//			return encodeExpression(r.getExpression(), next);
//		}
//		if(this.enablePackratParsing()) {
//			if(!this.enableASTConstruction() || r.isPurePEG()) {
//				Expression ref = Factory.resolveNonTerminal(r.getExpression());
//				MemoPoint m = this.issueMemoPoint(r.getUniqueName(), ref);
//				if(m != null) {
//					if(UFlag.is(option, Grammar.Tracing)) {
//						IMonitoredSwitch monitor = new IMonitoredSwitch(p, new ICallPush(p.getProduction(), next));
//						Instruction inside = new ICallPush(r, newMemoize(p, monitor, m, next));
//						monitor.setActivatedNext(newLookup(p, monitor, m, inside, next, newMemoizeFail(p, monitor, m)));
//						return monitor;
//					}
//					Instruction inside = new ICallPush(r, newMemoize(p, IMonitoredSwitch.dummyMonitor, m, next));
//					return newLookup(p, IMonitoredSwitch.dummyMonitor, m, inside, next, newMemoizeFail(p, IMonitoredSwitch.dummyMonitor, m));
//				}
//			}
//		}	
		return new ICallPush(r, next);
	}
	
//	private Instruction newLookup(Expression e, IMonitoredSwitch monitor, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
//		if(m.contextSensitive) {
//			return new IStateLookup(e, monitor, m, next, skip, failjump);
//		}
//		return new ILookup(e, monitor, m, next, skip, failjump);
//	}
//
//	private Instruction newMemoize(Expression e, IMonitoredSwitch monitor, MemoPoint m, Instruction next) {
//		if(m.contextSensitive) {
//			return new IStateMemoize(e, monitor, m, next);
//		}
//		return new IMemoize(e, monitor, m, next);
//	}
//
//	private Instruction newMemoizeFail(Expression e, IMonitoredSwitch monitor, MemoPoint m) {
//		if(m.contextSensitive) {
//			return new IStateMemoizeFail(e, monitor, m);
//		}
//		return new IMemoizeFail(e, monitor, m);
//	}
	
	// AST Construction
	
	public final Instruction encodeLink(Link p, Instruction next) {
		if(this.enableASTConstruction()) {
//			if(this.enablePackratParsing()) {
//				Expression inner = Factory.resolveNonTerminal(p.get(0));
//				MemoPoint m = this.issueMemoPoint(p.toString(), inner);
//				if(m != null) {
//					if(UFlag.is(option, Grammar.Tracing)) {
//						IMonitoredSwitch monitor = new IMonitoredSwitch(p, encodeExpression(p.get(0), next));
//						Instruction inside = encodeExpression(p.get(0), newMemoizeNode(p, monitor, m, next));
//						monitor.setActivatedNext(newLookupNode(p, monitor, m, inside, next, new IMemoizeFail(p, monitor, m)));
//						return monitor;
//					}
//					Instruction inside = encodeExpression(p.get(0), newMemoizeNode(p, IMonitoredSwitch.dummyMonitor, m, next));
//					return newLookupNode(p, IMonitoredSwitch.dummyMonitor, m, inside, next, new IMemoizeFail(p, IMonitoredSwitch.dummyMonitor, m));
//				}
//			}
			return new INodePush(p, encodeExpression(p.get(0), new INodeStore(p, next)));
		}
		return encodeExpression(p.get(0), next);
	}

//	private Instruction newLookupNode(Link e, IMonitoredSwitch monitor, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
//		if(m.contextSensitive) {
//			return new IStateLookupNode(e, monitor, m, next, skip, failjump);
//		}
//		return new ILookupNode(e, monitor, m, next, skip, failjump);
//	}
//
//	private Instruction newMemoizeNode(Link e, IMonitoredSwitch monitor, MemoPoint m, Instruction next) {
//		if(m.contextSensitive) {
//			return new IStateMemoizeNode(e, monitor, m, next);
//		}
//		return new IMemoizeNode(e, monitor, m, next);
//	}

	public final Instruction encodeNew(New p, Instruction next) {
		if(this.enableASTConstruction()) {
			return p.lefted ? new ILeftNew(p, next) : new INew(p, next);
		}
		return next;
	}

	public final Instruction encodeCapture(Capture p, Instruction next) {
		if(this.enableASTConstruction()) {
			return new ICapture(p, next);
		}
		return next;
	}
	
	public final Instruction encodeTagging(Tagging p, Instruction next) {
		if(this.enableASTConstruction()) {
			return new ITag(p, next);
		}
		return next;
	}

	public final Instruction encodeReplace(Replace p, Instruction next) {
		if(this.enableASTConstruction()) {
			return new IReplace(p, next);
		}
		return next;
	}
	
	public final Instruction encodeBlock(Block p, Instruction next) {
		Instruction failed = new ITablePop(p, new IFail(p));
		Instruction inner = encodeExpression(p.get(0), new IFailPop(p, new ITablePop(p, next)));
		return new ITablePush(p, new IFailPush(p, failed, inner));
	}
	
	public final Instruction encodeDefSymbol(DefSymbol p, Instruction next) {
		Instruction inner = encodeExpression(p.get(0), new IDefSymbol(p, next));
		return new IPosPush(p, inner);
	}
	
	public final Instruction encodeIsSymbol(IsSymbol p, Instruction next) {
		Instruction inner = encodeExpression(p.getSymbolExpression(), new IIsSymbol(p, p.checkLastSymbolOnly, next));
		return new IPosPush(p, inner);
	}
	
	public final Instruction encodeDefIndent(DefIndent p, Instruction next) {
		return new IDefIndent(p, next);
	}
	
	public final Instruction encodeIsIndent(IsIndent p, Instruction next) {
		return new IIsIndent(p, next);
	}

	public Instruction encodeExistsSymbol(ExistsSymbol existsSymbol, Instruction next) {
		// TODO Auto-generated method stub
		return next;
	}

	public Instruction encodeLocalTable(LocalTable localTable, Instruction next) {
		// TODO Auto-generated method stub
		return next;
	}

}
