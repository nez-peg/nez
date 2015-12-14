package nez.parser.moz;

import java.util.HashMap;

import nez.lang.Expression;

import nez.lang.Production;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Cset;
import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pand;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Pempty;
import nez.lang.expr.Pfail;
import nez.lang.expr.Pnot;
import nez.lang.expr.Pone;
import nez.lang.expr.Poption;
import nez.lang.expr.Psequence;
import nez.lang.expr.Pzero;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Tdetree;
import nez.lang.expr.Tlfold;
import nez.lang.expr.Tlink;
import nez.lang.expr.Tnew;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.lang.expr.Xblock;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xif;
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Xon;
import nez.lang.expr.Xsymbol;
import nez.parser.Coverage;
import nez.parser.ParseFunc;
import nez.parser.ParserGrammar;
import nez.parser.ParserStrategy;
import nez.util.UList;
import nez.util.Verbose;

public class MozCompiler extends Expression.Visitor {

	public final static MozCompiler newCompiler(ParserStrategy strategy) {
		return new MozCompiler(strategy);
	}

	protected ParserStrategy strategy;
	protected ParserGrammar gg = null;

	MozCompiler(ParserStrategy strategy) {
		this.gg = null;
		this.strategy = strategy;
	}

	/* CodeMap */

	protected void setGenerativeGrammar(ParserGrammar gg) {
		this.gg = gg;
	}

	private HashMap<String, ParseFunc> funcMap = null;

	protected int getParseFuncSize() {
		if (gg != null) {
			return gg.size();
		}
		if (this.funcMap != null) {
			return funcMap.size();
		}
		return 0;
	}

	protected ParseFunc getParseFunc(Production p) {
		if (gg != null) {
			ParseFunc f = gg.getParseFunc(p.getLocalName());
			if (f == null) {
				f = gg.getParseFunc(p.getUniqueName());
			}
			if (f == null) {
				Verbose.debug("unfound parsefunc: " + p.getLocalName() + " " + p.getUniqueName());
			}
			return f;
		}
		if (this.funcMap != null) {
			return funcMap.get(p.getUniqueName());
		}
		return null;
	}

	public MozCode compile(ParserGrammar gg) {
		this.setGenerativeGrammar(gg);
		long t = System.nanoTime();
		UList<MozInst> codeList = new UList<MozInst>(new MozInst[64]);
		for (Production p : gg) {
			if (!p.isSymbolTable()) {
				this.visitProduction(codeList, p, new Moz.Ret(p));
			}
		}
		this.layoutCachedInstruction(codeList);
		for (MozInst inst : codeList) {
			if (inst instanceof Moz.Call) {
				((Moz.Call) inst).sync();
			}
			// Verbose.debug("\t" + inst.id + "\t" + inst);
		}
		long t2 = System.nanoTime();
		Verbose.printElapsedTime("CompilingTime", t, t2);
		return new MozCode(gg, codeList, gg.memoPointList);
	}

	private Production encodingProduction;

	protected final Production getEncodingProduction() {
		return this.encodingProduction;
	}

	private UList<MozInst> cachedInstruction;

	protected void addCachedInstruction(MozInst inst) {
		if (this.cachedInstruction == null) {
			this.cachedInstruction = new UList<MozInst>(new MozInst[32]);
		}
		this.cachedInstruction.add(inst);
	}

	private void layoutCachedInstruction(UList<MozInst> codeList) {
		if (this.cachedInstruction != null) {
			for (MozInst inst : this.cachedInstruction) {
				layoutCode(codeList, inst);
			}
		}
	}

	protected void visitProduction(UList<MozInst> codeList, Production p, Object next) {
		ParseFunc f = this.getParseFunc(p);
		// System.out.println("inline: " + f.inlining + " name: " +
		// p.getLocalName());
		encodingProduction = p;
		if (!f.isInlined()) {
			next = Coverage.visitExitCoverage(p, (MozInst) next);
		}
		f.setCompiled(visit(f.getExpression(), (MozInst) next, null/* failjump */));
		if (!f.isInlined()) {
			f.setCompiled(Coverage.visitEnterCoverage(p, (MozInst) f.getCompiled()));
		}
		MozInst block = new Moz.Label(p, (MozInst) f.getCompiled());
		this.layoutCode(codeList, block);
	}

	public final void layoutCode(UList<MozInst> codeList, MozInst inst) {
		if (inst == null) {
			return;
		}
		if (inst.id == -1) {
			inst.id = codeList.size();
			codeList.add(inst);
			layoutCode(codeList, inst.next);
			if (inst.next != null && inst.id + 1 != inst.next.id) {
				MozInst.labeling(inst.next);
			}
			layoutCode(codeList, inst.branch());
			if (inst instanceof Moz.First) {
				Moz.First match = (Moz.First) inst;
				for (int ch = 0; ch < match.jumpTable.length; ch++) {
					layoutCode(codeList, match.jumpTable[ch]);
				}
			}
			// visit(inst.branch2());
		}
	}

	protected final MozInst commonFailure = new Moz.Fail(null);

	// encoding

	public MozInst visit(Expression e, Object next) {
		return (MozInst) e.visit(this, next);
	}

	public MozInst visit(Expression e, MozInst next) {
		return (MozInst) e.visit(this, next);
	}

	public MozInst visit(Expression e, MozInst next, Object failjump) {
		return (MozInst) e.visit(this, next);
	}

	@Override
	public MozInst visitPempty(Pempty p, Object next) {
		return (MozInst) next;
	}

	public MozInst fail(Expression e) {
		return this.commonFailure;
	}

	@Override
	public MozInst visitPfail(Pfail p, Object next) {
		return this.commonFailure;
	}

	@Override
	public MozInst visitCany(Cany p, Object next) {
		return new Moz.Any(p, (MozInst) next);
	}

	@Override
	public MozInst visitCbyte(Cbyte p, Object next) {
		return new Moz.Byte(p, (MozInst) next);
	}

	@Override
	public MozInst visitCset(Cset p, Object next) {
		return new Moz.Set(p, (MozInst) next);
	}

	@Override
	public MozInst visitCmulti(Cmulti p, Object next) {
		return new Moz.Str(p, (MozInst) next);
	}

	public MozInst visitUnnPoption(Poption p, Object next) {
		MozInst pop = new Moz.Succ(p, (MozInst) next);
		return new Moz.Alt(p, (MozInst) next, visit(p.get(0), pop, next));
	}

	public MozInst visitUnnPzero(Pzero p, Object next) {
		// Expression skip = p.possibleInfiniteLoop ? new Moz.Skip(p) : new
		// ISkip(p);
		MozInst skip = new Moz.Skip(p);
		MozInst start = visit(p.get(0), skip, next/* FIXME */);
		skip.next = start;
		return new Moz.Alt(p, (MozInst) next, start);
	}

	@Override
	public MozInst visitPone(Pone p, Object next) {
		return visit(p.get(0), this.visitPzero(p, next));
	}

	@Override
	public MozInst visitPand(Pand p, Object next) {
		MozInst inner = visit(p.get(0), new Moz.Back(p, (MozInst) next));
		return new Moz.Pos(p, inner);
	}

	public MozInst visitUnnPnot(Pnot p, Object next) {
		MozInst fail = new Moz.Succ(p, new Moz.Fail(p));
		return new Moz.Alt(p, (MozInst) next, visit(p.get(0), fail));
	}

	@Override
	public MozInst visitPsequence(Psequence p, Object next) {
		// return visit(p.get(0), visit(p.get(1), (MozInst)next));
		Object nextStart = next;
		for (int i = p.size() - 1; i >= 0; i--) {
			Expression e = p.get(i);
			nextStart = visit(e, nextStart);
		}
		return (MozInst) nextStart;
	}

	public MozInst visitUnnPchoice(Pchoice p, Object next) {
		Object nextChoice = visit(p.get(p.size() - 1), next);
		for (int i = p.size() - 2; i >= 0; i--) {
			Expression e = p.get(i);
			nextChoice = new Moz.Alt(e, (MozInst) nextChoice, visit(e, new Moz.Succ(e, (MozInst) next), nextChoice));
		}
		return (MozInst) nextChoice;
	}

	public MozInst visitUnnNonTerminal(NonTerminal n, Object next) {
		Production p = n.getProduction();
		ParseFunc f = this.getParseFunc(p);
		return new Moz.Call(f, p.getLocalName(), (MozInst) next);
	}

	// AST Construction

	public MozInst visitUnnTlink(Tlink p, Object next) {
		if (this.strategy.TreeConstruction) {
			next = new Moz.TPop(p, (MozInst) next);
			next = visit(p.get(0), next);
			return new Moz.TPush(p, (MozInst) next);
		}
		return visit(p.get(0), next);
	}

	@Override
	public MozInst visitTnew(Tnew p, Object next) {
		if (this.strategy.TreeConstruction) {
			return new Moz.TNew(p, (MozInst) next);
		}
		return (MozInst) next;
	}

	@Override
	public MozInst visitTlfold(Tlfold p, Object next) {
		if (this.strategy.TreeConstruction) {
			return new Moz.TLeftFold(p, (MozInst) next);
		}
		return (MozInst) next;
	}

	@Override
	public MozInst visitTcapture(Tcapture p, Object next) {
		if (this.strategy.TreeConstruction) {
			return new Moz.TCapture(p, (MozInst) next);
		}
		return (MozInst) next;
	}

	@Override
	public MozInst visitTtag(Ttag p, Object next) {
		if (this.strategy.TreeConstruction) {
			return new Moz.TTag(p, (MozInst) next);
		}
		return (MozInst) next;
	}

	@Override
	public MozInst visitTreplace(Treplace p, Object next) {
		if (this.strategy.TreeConstruction) {
			return new Moz.TReplace(p, (MozInst) next);
		}
		return (MozInst) next;
	}

	@Override
	public MozInst visitXblock(Xblock p, Object next) {
		next = new Moz.SClose(p, (MozInst) next);
		next = visit(p.get(0), next);
		return new Moz.SOpen(p, (MozInst) next);
	}

	@Override
	public MozInst visitXlocal(Xlocal p, Object next) {
		next = new Moz.SClose(p, (MozInst) next);
		next = visit(p.get(0), next);
		return new Moz.SMask(p, (MozInst) next);
	}

	@Override
	public MozInst visitXdef(Xsymbol p, Object next) {
		return new Moz.Pos(p, visit(p.get(0), new Moz.SDef(p, (MozInst) next)));
	}

	@Override
	public MozInst visitXexists(Xexists p, Object next) {
		String symbol = p.getSymbol();
		if (symbol == null) {
			return new Moz.SExists(p, (MozInst) next);
		} else {
			return new Moz.SIsDef(p, (MozInst) next);
		}
	}

	@Override
	public MozInst visitXmatch(Xmatch p, Object next) {
		return new Moz.SMatch(p, (MozInst) next);
	}

	@Override
	public MozInst visitXis(Xis p, Object next) {
		if (p.is) {
			return new Moz.Pos(p, visit(p.get(0), new Moz.SIs(p, (MozInst) next)));
		} else {
			return new Moz.Pos(p, visit(p.get(0), new Moz.SIsa(p, (MozInst) next)));
		}
	}

	@Override
	public MozInst visitTdetree(Tdetree p, Object next) {
		return (MozInst) next;
	}

	/* Optimization */

	protected void optimizedUnary(Expression p) {
		Verbose.noticeOptimize("specialization", p);
	}

	protected void optimizedInline(Production p) {
		Verbose.noticeOptimize("inlining", p.getExpression());
	}

	public final Expression getInnerExpression(Expression p) {
		Expression inner = ExpressionCommons.resolveNonTerminal(p.get(0));
		if (strategy.Ostring && inner instanceof Psequence) {
			inner = ((Psequence) inner).toMultiCharSequence();
			// System.out.println("Stringfy:" + inner);
		}
		return inner;
	}

	@Override
	public final MozInst visitPoption(Poption p, Object next) {
		if (strategy.Olex) {
			Expression inner = getInnerExpression(p);
			if (inner instanceof Cbyte) {
				this.optimizedUnary(p);
				return new Moz.OByte((Cbyte) inner, (MozInst) next);
			}
			if (inner instanceof Cset) {
				this.optimizedUnary(p);
				return new Moz.OSet((Cset) inner, (MozInst) next);
			}
			if (inner instanceof Cmulti) {
				this.optimizedUnary(p);
				return new Moz.OStr((Cmulti) inner, (MozInst) next);
			}
		}
		return visitUnnPoption(p, next);
	}

	@Override
	public final MozInst visitPzero(Pzero p, Object next) {
		if (strategy.Olex) {
			Expression inner = getInnerExpression(p);
			if (inner instanceof Cbyte) {
				this.optimizedUnary(p);
				return new Moz.RByte((Cbyte) inner, (MozInst) next);
			}
			if (inner instanceof Cset) {
				this.optimizedUnary(p);
				return new Moz.RSet((Cset) inner, (MozInst) next);
			}
			if (inner instanceof Cmulti) {
				this.optimizedUnary(p);
				return new Moz.RStr((Cmulti) inner, (MozInst) next);
			}
		}
		return visitUnnPzero(p, next);
	}

	@Override
	public final MozInst visitPnot(Pnot p, Object next) {
		if (strategy.Olex) {
			Expression inner = getInnerExpression(p);
			if (inner instanceof Cset) {
				this.optimizedUnary(p);
				return new Moz.NSet((Cset) inner, (MozInst) next);
			}
			if (inner instanceof Cbyte) {
				this.optimizedUnary(p);
				return new Moz.NByte((Cbyte) inner, (MozInst) next);
			}
			if (inner instanceof Cany) {
				this.optimizedUnary(p);
				return new Moz.NAny(inner, ((Cany) inner).isBinary(), (MozInst) next);
			}
			if (inner instanceof Cmulti) {
				this.optimizedUnary(p);
				return new Moz.NStr((Cmulti) inner, (MozInst) next);
			}
		}
		return visitUnnPnot(p, next);
	}

	@Override
	public final MozInst visitPchoice(Pchoice p, Object next) {
		if (/* strategy.isEnabled("Ofirst", Strategy.Ofirst) && */p.predictedCase != null) {
			if (p.isTrieTree && strategy.Odfa) {
				return visitDFirstChoice(p, next);
			}
			return visitFirstChoice(p, next);
		}
		return visitUnnPchoice(p, next);
	}

	private final MozInst visitFirstChoice(Pchoice choice, Object next) {
		MozInst[] compiled = new MozInst[choice.firstInners.length];
		// Verbose.debug("TrieTree: " + choice.isTrieTree + " " + choice);
		Moz.First dispatch = new Moz.First(choice, commonFailure);
		for (int ch = 0; ch < choice.predictedCase.length; ch++) {
			Expression predicted = choice.predictedCase[ch];
			if (predicted == null) {
				continue;
			}
			int index = findIndex(choice, predicted);
			MozInst inst = compiled[index];
			if (inst == null) {
				// System.out.println("creating '" + (char)ch + "'("+ch+"): " +
				// e);
				if (predicted instanceof Pchoice) {
					assert (((Pchoice) predicted).predictedCase == null);
					inst = visitUnnPchoice(choice, next);
				} else {
					inst = visit(predicted, next);
				}
				compiled[index] = inst;
			}
			dispatch.setJumpTable(ch, inst);
		}
		return dispatch;
	}

	private final MozInst visitDFirstChoice(Pchoice choice, Object next) {
		MozInst[] compiled = new MozInst[choice.firstInners.length];
		Moz.DFirst dispatch = new Moz.DFirst(choice, commonFailure);
		for (int ch = 0; ch < choice.predictedCase.length; ch++) {
			Expression predicted = choice.predictedCase[ch];
			if (predicted == null) {
				continue;
			}
			int index = findIndex(choice, predicted);
			MozInst inst = compiled[index];
			if (inst == null) {
				Expression next2 = predicted.getNext();
				if (next2 != null) {
					inst = visit(next2, next);
				} else {
					inst = (MozInst) next;
				}
				compiled[index] = inst;
			}
			dispatch.setJumpTable(ch, inst);
		}
		return dispatch;
	}

	private int findIndex(Pchoice choice, Expression e) {
		for (int i = 0; i < choice.firstInners.length; i++) {
			if (choice.firstInners[i] == e) {
				return i;
			}
		}
		return -1;
	}

	private final MozInst visitPredicatedChoice0(Pchoice choice, Object next) {
		HashMap<Integer, MozInst> m = new HashMap<Integer, MozInst>();
		Moz.First dispatch = new Moz.First(choice, commonFailure);
		for (int ch = 0; ch < choice.predictedCase.length; ch++) {
			Expression predicted = choice.predictedCase[ch];
			if (predicted == null) {
				continue;
			}
			int id = predictId(choice.predictedCase, ch, predicted);
			MozInst inst = m.get(id);
			if (inst == null) {
				// System.out.println("creating '" + (char)ch + "'("+ch+"): " +
				// e);
				if (predicted instanceof Pchoice) {
					assert (((Pchoice) predicted).predictedCase == null);
					inst = visitUnnPchoice(choice, next);
				} else {
					inst = visit(predicted, next);
				}
				m.put(id, inst);
			}
			dispatch.setJumpTable(ch, inst);
		}
		return dispatch;
	}

	private int predictId(Expression[] predictedCase, int max, Expression predicted) {
		// if (predicted.isInterned()) {
		// return predicted.getId();
		// }
		for (int i = 0; i < max; i++) {
			if (predictedCase[i] != null && predicted.equalsExpression(predictedCase[i])) {
				return i;
			}
		}
		return max;
	}

	// public final MozInst visitUnoptimizedChoice(Pchoice p, Object next) {
	// return super.visitPchoice(p, next);
	// }

	@Override
	public final MozInst visitNonTerminal(NonTerminal n, Object next) {
		Production p = n.getProduction();
		if (p == null) {
			Verbose.debug("[PANIC] unresolved: " + n.getLocalName() + " ***** ");
			return (MozInst) next;
		}
		ParseFunc f = this.getParseFunc(p);
		if (f.isInlined()) {
			this.optimizedInline(p);
			return visit(f.getExpression(), next);
		}
		if (f.getMemoPoint() != null) {
			if (!strategy.TreeConstruction || p.isNoNTreeConstruction()) {
				if (Verbose.PackratParsing) {
					Verbose.println("memoize: " + n.getLocalName() + " at " + this.getEncodingProduction().getLocalName());
				}
				return memoize(n, f, next);
			}
		}
		return new Moz.Call(f, n.getLocalName(), (MozInst) next);
	}

	private MozInst memoize(NonTerminal n, ParseFunc f, Object next) {
		MozInst inside = new Moz.Memo(n, f.getMemoPoint(), f.isStateful(), (MozInst) next);
		inside = new Moz.Call(f, n.getLocalName(), inside);
		inside = new Moz.Alt(n, new Moz.MemoFail(n, f.isStateful(), f.getMemoPoint()), inside);
		return new Moz.Lookup(n, f.getMemoPoint(), f.isStateful(), inside, (MozInst) next);
	}

	// private Instruction memoize2(NonTerminal n, ParseFunc f, Instruction
	// next) {
	// if (f.compiled_memo == null) {
	// f.compiled_memo = memoize(n, f, new Moz.Ret(n));
	// this.addCachedInstruction(f.compiled_memo);
	// }
	// return new Moz.Call(f, n.getLocalName(), f.compiled_memo, (MozInst)next);
	// }

	// AST Construction

	@Override
	public final MozInst visitTlink(Tlink p, Object next) {
		if (strategy.TreeConstruction && p.get(0) instanceof NonTerminal) {
			NonTerminal n = (NonTerminal) p.get(0);
			ParseFunc f = this.getParseFunc(n.getProduction());
			if (f.getMemoPoint() != null) {
				if (Verbose.PackratParsing) {
					Verbose.println("memoize: @" + n.getLocalName() + " at " + this.getEncodingProduction().getLocalName());
				}
				return memoize(p, n, f, next);
			}
		}
		return visitUnnTlink(p, next);
	}

	private MozInst memoize(Tlink p, NonTerminal n, ParseFunc f, Object next) {
		MozInst inside = new Moz.TMemo(p, f.getMemoPoint(), f.isStateful(), (MozInst) next);
		inside = new Moz.TCommit(p, inside);
		inside = visitUnnNonTerminal(n, inside);
		inside = new Moz.TStart(p, inside);
		inside = new Moz.Alt(p, new Moz.MemoFail(p, f.isStateful(), f.getMemoPoint()), inside);
		return new Moz.TLookup(p, f.getMemoPoint(), f.isStateful(), inside, (MozInst) next);
	}

	@Override
	public Object visitXindent(Xindent e, Object a) {
		// TODO Auto-generated method stub
		return a;
	}

	@Override
	public Object visitXif(Xif e, Object a) {
		// TODO Auto-generated method stub
		return a;
	}

	@Override
	public Object visitXon(Xon e, Object a) {
		// TODO Auto-generated method stub
		return a;
	}

	// private Instruction memoize2(Tlink p, NonTerminal n, ParseFunc f,
	// Instruction next) {
	// if (f.compiled_memoAST == null) {
	// f.compiled_memoAST = memoize(p, n, f, new Moz.Ret(p));
	// this.addCachedInstruction(f.compiled_memoAST);
	// }
	// return new Moz.Call(f, n.getLocalName(), f.compiled_memoAST,
	// (MozInst)next);
	// }

}
