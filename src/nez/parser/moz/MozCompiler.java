package nez.parser.moz;

import nez.lang.Expression;
import nez.lang.Expressions;
import nez.lang.FunctionName;
import nez.lang.Grammar;
import nez.lang.Nez;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.lang.Typestate;
import nez.parser.MemoPoint;
import nez.parser.ParserCode.ProductionCode;
import nez.parser.ParserCompiler;
import nez.parser.ParserStrategy;
import nez.util.UList;
import nez.util.Verbose;

public class MozCompiler implements ParserCompiler {

	public final static MozCompiler newCompiler(ParserStrategy strategy) {
		return new MozCompiler(strategy);
	}

	protected ParserStrategy strategy;

	MozCompiler(ParserStrategy strategy) {
		this.strategy = strategy;
	}

	@Override
	public MozCode compile(Grammar grammar) {
		long t = System.nanoTime();
		MozCode code = new MozCode(grammar);
		if (strategy.PackratParsing) {
			code.initMemoPoint();
		}
		new CompilerVisitor(code, grammar).compile(grammar);
		long t2 = System.nanoTime();
		Verbose.printElapsedTime("CompilingTime", t, t2);
		return code;
	}

	class CompilerVisitor extends Expression.Visitor {

		final MozCode code;
		final Grammar grammar;

		CompilerVisitor(MozCode code, Grammar grammar) {
			this.code = code;
			this.grammar = grammar;
			for (Production p : grammar) {
				code.setProductionCode(p, new ProductionCode<MozInst>(null));
			}
		}

		private MozCode compile(Grammar gg) {
			long t = System.nanoTime();
			for (Production p : gg) {
				this.visitProduction(code.codeList(), p, new Moz.Ret(p));
			}
			// this.layoutCachedInstruction(code.codeList());
			for (MozInst inst : code.codeList()) {
				if (inst instanceof Moz.Call) {
					((Moz.Call) inst).sync();
				}
				// Verbose.debug("\t" + inst.id + "\t" + inst);
			}
			long t2 = System.nanoTime();
			Verbose.printElapsedTime("CompilingTime", t, t2);
			return code;
		}

		private Production encodingProduction;

		protected final Production getEncodingProduction() {
			return this.encodingProduction;
		}

		protected void visitProduction(UList<MozInst> codeList, Production p, MozInst next) {
			ProductionCode<MozInst> f = code.getProductionCode(p);
			// System.out.println("inline: " + f.inlining + " name: " +
			// p.getLocalName());
			encodingProduction = p;
			next = Coverage.visitExitCoverage(p, next);
			next = visit(p.getExpression(), next, null/* failjump */);
			next = Coverage.visitEnterCoverage(p, next);
			f.setCompiled(next);
			MozInst block = new Moz.Label(p, next);
			code.layoutCode(block);
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
		public MozInst visitEmpty(Nez.Empty p, Object next) {
			return (MozInst) next;
		}

		public MozInst fail(Expression e) {
			return this.commonFailure;
		}

		@Override
		public MozInst visitFail(Nez.Fail p, Object next) {
			return this.commonFailure;
		}

		@Override
		public MozInst visitAny(Nez.Any p, Object next) {
			return new Moz.Any(p, (MozInst) next);
		}

		@Override
		public MozInst visitByte(Nez.Byte p, Object next) {
			return new Moz.Byte(p, (MozInst) next);
		}

		@Override
		public MozInst visitByteSet(Nez.ByteSet p, Object next) {
			return new Moz.Set(p, (MozInst) next);
		}

		@Override
		public MozInst visitMultiByte(Nez.MultiByte p, Object next) {
			return new Moz.Str(p, (MozInst) next);
		}

		public MozInst visitUnnPoption(Nez.Option p, Object next) {
			MozInst pop = new Moz.Succ(p, (MozInst) next);
			return new Moz.Alt(p, (MozInst) next, visit(p.get(0), pop, next));
		}

		public MozInst visitUnnPzero(Nez.Repetition p, Object next) {
			// Expression skip = p.possibleInfiniteLoop ? new Moz.Skip(p) : new
			// ISkip(p);
			MozInst skip = new Moz.Skip((Expression) p);
			MozInst start = visit(((Expression) p).get(0), skip, next/* FIXME */);
			skip.next = start;
			return new Moz.Alt((Expression) p, (MozInst) next, start);
		}

		@Override
		public MozInst visitOneMore(Nez.OneMore p, Object next) {
			return visit(p.get(0), this.visitRepetition(p, next));
		}

		@Override
		public MozInst visitAnd(Nez.And p, Object next) {
			MozInst inner = visit(p.get(0), new Moz.Back(p, (MozInst) next));
			return new Moz.Pos(p, inner);
		}

		public MozInst visitUnnPnot(Nez.Not p, Object next) {
			MozInst fail = new Moz.Succ(p, new Moz.Fail(p));
			return new Moz.Alt(p, (MozInst) next, visit(p.get(0), fail));
		}

		@Override
		public MozInst visitPair(Nez.Pair p, Object next) {
			// return visit(p.get(0), visit(p.get(1), (MozInst)next));
			Object nextStart = next;
			for (int i = p.size() - 1; i >= 0; i--) {
				Expression e = p.get(i);
				nextStart = visit(e, nextStart);
			}
			return (MozInst) nextStart;
		}

		@Override
		public MozInst visitSequence(Nez.Sequence p, Object next) {
			// return visit(p.get(0), visit(p.get(1), (MozInst)next));
			Object nextStart = next;
			for (int i = p.size() - 1; i >= 0; i--) {
				Expression e = p.get(i);
				nextStart = visit(e, nextStart);
			}
			return (MozInst) nextStart;
		}

		public MozInst visitUnnPchoice(Nez.Choice p, Object next) {
			Object nextChoice = visit(p.get(p.size() - 1), next);
			for (int i = p.size() - 2; i >= 0; i--) {
				Expression e = p.get(i);
				nextChoice = new Moz.Alt(e, (MozInst) nextChoice, visit(e, new Moz.Succ(e, (MozInst) next), nextChoice));
			}
			return (MozInst) nextChoice;
		}

		public MozInst visitUnnNonTerminal(NonTerminal n, Object next) {
			Production p = n.getProduction();
			ProductionCode<MozInst> f = code.getProductionCode(p);
			return new Moz.Call(f, p.getLocalName(), (MozInst) next);
		}

		// AST Construction

		public MozInst visitUnnTlink(Nez.LinkTree p, Object next) {
			if (strategy.TreeConstruction) {
				next = new Moz.TPop(p, (MozInst) next);
				next = visit(p.get(0), next);
				return new Moz.TPush(p, (MozInst) next);
			}
			return visit(p.get(0), next);
		}

		@Override
		public MozInst visitBeginTree(Nez.BeginTree p, Object next) {
			if (strategy.TreeConstruction) {
				return new Moz.TNew(p, (MozInst) next);
			}
			return (MozInst) next;
		}

		@Override
		public MozInst visitFoldTree(Nez.FoldTree p, Object next) {
			if (strategy.TreeConstruction) {
				return new Moz.TLeftFold(p, (MozInst) next);
			}
			return (MozInst) next;
		}

		@Override
		public MozInst visitEndTree(Nez.EndTree p, Object next) {
			if (strategy.TreeConstruction) {
				return new Moz.TCapture(p, (MozInst) next);
			}
			return (MozInst) next;
		}

		@Override
		public MozInst visitTag(Nez.Tag p, Object next) {
			if (strategy.TreeConstruction) {
				return new Moz.TTag(p, (MozInst) next);
			}
			return (MozInst) next;
		}

		@Override
		public MozInst visitReplace(Nez.Replace p, Object next) {
			if (strategy.TreeConstruction) {
				return new Moz.TReplace(p, (MozInst) next);
			}
			return (MozInst) next;
		}

		@Override
		public MozInst visitBlockScope(Nez.BlockScope p, Object next) {
			next = new Moz.SClose(p, (MozInst) next);
			next = visit(p.get(0), next);
			return new Moz.SOpen(p, (MozInst) next);
		}

		@Override
		public MozInst visitLocalScope(Nez.LocalScope p, Object next) {
			next = new Moz.SClose(p, (MozInst) next);
			next = visit(p.get(0), next);
			return new Moz.SMask(p, (MozInst) next);
		}

		@Override
		public MozInst visitSymbolAction(Nez.SymbolAction p, Object next) {
			return new Moz.Pos(p, visit(p.get(0), new Moz.SDef(p, (MozInst) next)));
		}

		@Override
		public MozInst visitSymbolExists(Nez.SymbolExists p, Object next) {
			String symbol = p.symbol;
			if (symbol == null) {
				return new Moz.SExists(p, (MozInst) next);
			} else {
				return new Moz.SIsDef(p, (MozInst) next);
			}
		}

		@Override
		public MozInst visitSymbolMatch(Nez.SymbolMatch p, Object next) {
			return new Moz.SMatch(p, (MozInst) next);
		}

		@Override
		public MozInst visitSymbolPredicate(Nez.SymbolPredicate p, Object next) {
			if (p.op == FunctionName.is) {
				return new Moz.Pos(p, visit(p.get(0), new Moz.SIs(p, (MozInst) next)));
			} else {
				return new Moz.Pos(p, visit(p.get(0), new Moz.SIsa(p, (MozInst) next)));
			}
		}

		@Override
		public MozInst visitDetree(Nez.Detree p, Object next) {
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
			Expression inner = Expressions.resolveNonTerminal(p.get(0));
			if (strategy.Ostring) {
				inner = Expressions.tryMultiCharSequence(inner);
			}
			return inner;
		}

		@Override
		public final MozInst visitOption(Nez.Option p, Object next) {
			if (strategy.Olex) {
				Expression inner = getInnerExpression(p);
				if (inner instanceof Nez.Byte) {
					this.optimizedUnary(p);
					return new Moz.OByte((Nez.Byte) inner, (MozInst) next);
				}
				if (inner instanceof Nez.ByteSet) {
					this.optimizedUnary(p);
					return new Moz.OSet((Nez.ByteSet) inner, (MozInst) next);
				}
				if (inner instanceof Nez.MultiByte) {
					this.optimizedUnary(p);
					return new Moz.OStr((Nez.MultiByte) inner, (MozInst) next);
				}
			}
			return visitUnnPoption(p, next);
		}

		@Override
		public final MozInst visitZeroMore(Nez.ZeroMore p, Object next) {
			return this.visitRepetition(p, next);
		}

		public final MozInst visitRepetition(Nez.Repetition p, Object next) {
			if (strategy.Olex) {
				Expression inner = getInnerExpression((Expression) p);
				if (inner instanceof Nez.Byte) {
					this.optimizedUnary((Expression) p);
					return new Moz.RByte((Nez.Byte) inner, (MozInst) next);
				}
				if (inner instanceof Nez.ByteSet) {
					this.optimizedUnary((Expression) p);
					return new Moz.RSet((Nez.ByteSet) inner, (MozInst) next);
				}
				if (inner instanceof Nez.MultiByte) {
					this.optimizedUnary((Expression) p);
					return new Moz.RStr((Nez.MultiByte) inner, (MozInst) next);
				}
			}
			return visitUnnPzero(p, next);
		}

		@Override
		public final MozInst visitNot(Nez.Not p, Object next) {
			if (strategy.Olex) {
				Expression inner = getInnerExpression(p);
				if (inner instanceof Nez.ByteSet) {
					this.optimizedUnary(p);
					return new Moz.NSet((Nez.ByteSet) inner, (MozInst) next);
				}
				if (inner instanceof Nez.Byte) {
					this.optimizedUnary(p);
					return new Moz.NByte((Nez.Byte) inner, (MozInst) next);
				}
				if (inner instanceof Nez.Any) {
					this.optimizedUnary(p);
					return new Moz.NAny(inner, false, (MozInst) next);
				}
				if (inner instanceof Nez.MultiByte) {
					this.optimizedUnary(p);
					return new Moz.NStr((Nez.MultiByte) inner, (MozInst) next);
				}
			}
			return visitUnnPnot(p, next);
		}

		@Override
		public final MozInst visitChoice(Nez.Choice p, Object next) {
			if (/* strategy.isEnabled("Ofirst", Strategy.Ofirst) && */p.predictedCase != null) {
				if (p.isTrieTree && strategy.Odfa) {
					return visitDFirstChoice(p, next);
				}
				return visitFirstChoice(p, next);
			}
			return visitUnnPchoice(p, next);
		}

		private final MozInst visitFirstChoice(Nez.Choice choice, Object next) {
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
					// System.out.println("creating '" + (char)ch +
					// "'("+ch+"): " +
					// e);
					if (predicted instanceof Nez.Choice) {
						assert (((Nez.Choice) predicted).predictedCase == null);
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

		private final MozInst visitDFirstChoice(Nez.Choice choice, Object next) {
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
					Expression next2 = Expressions.next(predicted);
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

		private int findIndex(Nez.Choice choice, Expression e) {
			for (int i = 0; i < choice.firstInners.length; i++) {
				if (choice.firstInners[i] == e) {
					return i;
				}
			}
			return -1;
		}

		@Override
		public final MozInst visitNonTerminal(NonTerminal n, Object next) {
			Production p = n.getProduction();
			if (p == null) {
				Verbose.debug("[PANIC] unresolved: " + n.getLocalName() + " ***** ");
				return (MozInst) next;
			}
			ProductionCode<MozInst> f = code.getProductionCode(p);
			MemoPoint m = code.getMemoPoint(p.getUniqueName());
			if (m != null) {
				if (!strategy.TreeConstruction || m.getTypestate() == Typestate.Unit) {
					if (Verbose.PackratParsing) {
						Verbose.println("memoize: " + n.getLocalName() + " at " + this.getEncodingProduction().getLocalName());
					}
					return memoize(n, f, m, (MozInst) next);
				}
			}
			return new Moz.Call(f, n.getLocalName(), (MozInst) next);
		}

		private MozInst memoize(NonTerminal n, ProductionCode<MozInst> f, MemoPoint m, MozInst next) {
			MozInst inside = new Moz.Memo(n, m, next);
			inside = new Moz.Call(f, n.getLocalName(), inside);
			inside = new Moz.Alt(n, new Moz.MemoFail(n, m), inside);
			return new Moz.Lookup(n, m, inside, next);
		}

		// AST Construction

		@Override
		public final MozInst visitLinkTree(Nez.LinkTree p, Object next) {
			if (strategy.TreeConstruction && p.get(0) instanceof NonTerminal) {
				NonTerminal n = (NonTerminal) p.get(0);
				MemoPoint m = code.getMemoPoint(n.getUniqueName());
				if (m != null) {
					if (Verbose.PackratParsing) {
						Verbose.println("memoize: @" + n.getLocalName() + " at " + this.getEncodingProduction().getLocalName());
					}
					return memoize(p, n, m, (MozInst) next);
				}
			}
			return visitUnnTlink(p, next);
		}

		private MozInst memoize(Nez.LinkTree p, NonTerminal n, MemoPoint m, MozInst next) {
			MozInst inside = new Moz.TMemo(p, m, next);
			inside = new Moz.TCommit(p, inside);
			inside = visitUnnNonTerminal(n, inside);
			inside = new Moz.TStart(p, inside);
			inside = new Moz.Alt(p, new Moz.MemoFail(p, m), inside);
			return new Moz.TLookup(p, m, inside, next);
		}

		@Override
		public Object visitIf(Nez.IfCondition e, Object a) {
			return a;
		}

		@Override
		public Object visitOn(Nez.OnCondition e, Object a) {
			return a;
		}
	}
}
