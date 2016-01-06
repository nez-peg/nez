package nez.parser.hachi6;

import nez.lang.Expression;
import nez.lang.Expressions;
import nez.lang.Grammar;
import nez.lang.Nez;
import nez.lang.NonTerminal;
import nez.lang.Predicate;
import nez.lang.Production;
import nez.parser.ParserStrategy;
import nez.parser.hachi6.Hachi6Code.ParseFunc;
import nez.util.StringUtils;
import nez.util.Verbose;

public class Hachi6Compiler {

	public final static Hachi6Compiler newCompiler(ParserStrategy strategy) {
		return new Hachi6Compiler(strategy);
	}

	protected ParserStrategy strategy;

	Hachi6Compiler(ParserStrategy strategy) {
		this.strategy = strategy;
	}

	public Hachi6Code compile(Grammar grammar) {
		long t = System.nanoTime();
		Hachi6Code code = new Hachi6Code(grammar);
		new CompilerVisitor(code).compileProduction(grammar.getStartProduction());
		long t2 = System.nanoTime();
		Verbose.printElapsedTime("CompilingTime", t, t2);
		return code;
	}

	class CompilerVisitor extends Expression.Visitor {

		final Hachi6Code code;

		CompilerVisitor(Hachi6Code code) {
			this.code = code;
		}

		protected ParseFunc compileProduction(Production p) {
			ParseFunc f = code.getParseFunc(p);
			if (f == null) {
				Hachi6Inst next = new Hachi6.Ret(null);
				Hachi6Inst block = new Hachi6.Label(p.getUniqueName(), next);
				f = new ParseFunc(p, block);
				code.setParseFunc(p, f);
				Production stacked = this.compilingProduction;
				this.compilingProduction = p;

				next = code.visitExitCoverage(p, next);
				next = compile(p.getExpression(), next);
				next = code.visitEnterCoverage(p, next);
				block.next = next;
				code.layoutCode(block);
				this.compilingProduction = stacked;
			}
			return f;
		}

		private Production compilingProduction = null;

		protected final Production getCompilingProduction() {
			return this.compilingProduction;
		}

		@Override
		public final Hachi6Inst visitNonTerminal(NonTerminal n, Object next) {
			Production p = n.getProduction();
			if (p == null) {
				Verbose.debug("[PANIC] unresolved: " + n.getLocalName() + " ***** ");
				return (Hachi6Inst) next;
			}
			ParseFunc f = this.compileProduction(p);
			// if (f.getMemoPoint() != null) {
			// // if (!strategy.TreeConstruction || this.grammar.typeState(p)
			// // == Typestate.Unit) {
			// // if (Verbose.PackratParsing) {
			// // Verbose.println("memoize: " + n.getLocalName() + " at " +
			// // this.getCompilingProduction().getLocalName());
			// // }
			// // return memoize(n, f, next);
			// // }
			// }
			return new Hachi6.Call(f.getCompiled(), n.getUniqueName(), (Hachi6Inst) next);
		}

		// private Hachi6Inst memoize(NonTerminal n, ParseFunc f, Object next) {
		// Hachi6Inst inside = new Hachi6.Memo(n, f.getMemoPoint(),
		// f.isStateful(), (Hachi6Inst) next);
		// inside = new Hachi6.Call(f, n.getLocalName(), inside);
		// inside = new Hachi6.Alt(n, new Hachi6.MemoFail(n, f.isStateful(),
		// f.getMemoPoint()), inside);
		// return new Hachi6.Lookup(n, f.getMemoPoint(), f.isStateful(), inside,
		// (Hachi6Inst) next);
		// }

		// encoding

		public Hachi6Inst compile(Expression e, Hachi6Inst next) {
			return (Hachi6Inst) e.visit(this, next);
		}

		public Hachi6Inst compile(Expression e, Hachi6Inst next, Object failjump) {
			return (Hachi6Inst) e.visit(this, next);
		}

		@Override
		public Hachi6Inst visitEmpty(Nez.Empty p, Object next) {
			return (Hachi6Inst) next;
		}

		protected final Hachi6Inst commonFailure = new Hachi6.Fail(null);

		public Hachi6Inst fail(Expression e) {
			return this.commonFailure;
		}

		@Override
		public Hachi6Inst visitFail(Nez.Fail p, Object next) {
			return this.commonFailure;
		}

		@Override
		public Hachi6Inst visitAny(Nez.Any p, Object next) {
			return new Hachi6.Any((Hachi6Inst) next);
		}

		@Override
		public Hachi6Inst visitByte(Nez.Byte p, Object next) {
			return new Hachi6.Byte(p.byteChar, (Hachi6Inst) next);
		}

		@Override
		public Hachi6Inst visitByteSet(Nez.ByteSet p, Object next) {
			return new Hachi6.Set(p.byteMap, (Hachi6Inst) next);
		}

		@Override
		public Hachi6Inst visitMultiByte(Nez.MultiByte p, Object next) {
			return new Hachi6.Str(p.byteSeq, (Hachi6Inst) next);
		}

		@Override
		public final Hachi6Inst visitOption(Nez.Option p, Object next) {
			if (strategy.Olex) {
				Expression inner = getInnerExpression(p);
				if (inner instanceof Nez.Byte) {
					return new Hachi6.OByte(((Nez.Byte) inner).byteChar, (Hachi6Inst) next);
				}
				if (inner instanceof Nez.ByteSet) {
					return new Hachi6.OSet(((Nez.ByteSet) inner).byteMap, (Hachi6Inst) next);
				}
				if (inner instanceof Nez.MultiByte) {
					return new Hachi6.OStr(((Nez.MultiByte) inner).byteSeq, (Hachi6Inst) next);
				}
			}
			Hachi6Inst pop = new Hachi6.Succ((Hachi6Inst) next);
			return new Hachi6.Alt((Hachi6Inst) next, compile(p.get(0), pop, next));
		}

		@Override
		public final Hachi6Inst visitZeroMore(Nez.ZeroMore p, Object next) {
			return this.compileRepetition(p, next);
		}

		public final Hachi6Inst compileRepetition(Nez.Repetition p, Object next) {
			if (strategy.Olex) {
				Expression inner = getInnerExpression((Expression) p);
				if (inner instanceof Nez.Byte) {
					return new Hachi6.RByte(((Nez.Byte) inner).byteChar, (Hachi6Inst) next);
				}
				if (inner instanceof Nez.ByteSet) {
					return new Hachi6.RSet(((Nez.ByteSet) inner).byteMap, (Hachi6Inst) next);
				}
				if (inner instanceof Nez.MultiByte) {
					return new Hachi6.RStr(((Nez.MultiByte) inner).byteSeq, (Hachi6Inst) next);
				}
			}
			Hachi6Inst skip = new Hachi6.Guard(null);
			Hachi6Inst start = compile(((Expression) p).get(0), skip);
			skip.next = start;
			return new Hachi6.Alt((Hachi6Inst) next, start);
		}

		@Override
		public Hachi6Inst visitOneMore(Nez.OneMore p, Object next) {
			return compile(p.get(0), this.compileRepetition(p, next));
		}

		@Override
		public Hachi6Inst visitAnd(Nez.And p, Object next) {
			Hachi6Inst inner = compile(p.get(0), new Hachi6.Back((Hachi6Inst) next));
			return new Hachi6.Pos(inner);
		}

		@Override
		public final Hachi6Inst visitNot(Nez.Not p, Object next) {
			if (strategy.Olex) {
				Expression inner = getInnerExpression(p);
				if (inner instanceof Nez.ByteSet) {
					return new Hachi6.NSet(((Nez.ByteSet) inner).byteMap, (Hachi6Inst) next);
				}
				if (inner instanceof Nez.Byte) {
					return new Hachi6.NByte(((Nez.Byte) inner).byteChar, (Hachi6Inst) next);
				}
				if (inner instanceof Nez.MultiByte) {
					return new Hachi6.NStr(((Nez.MultiByte) inner).byteSeq, (Hachi6Inst) next);
				}
				if (inner instanceof Nez.Any) {
					return new Hachi6.NAny((Hachi6Inst) next);
				}
			}
			Hachi6Inst fail = new Hachi6.Succ(new Hachi6.Fail(null));
			return new Hachi6.Alt((Hachi6Inst) next, compile(p.get(0), fail));
		}

		@Override
		public Hachi6Inst visitPair(Nez.Pair p, Object next) {
			// return visit(p.get(0), visit(p.get(1), (Hachi6Inst)next));
			Hachi6Inst nextStart = (Hachi6Inst) next;
			for (int i = p.size() - 1; i >= 0; i--) {
				Expression e = p.get(i);
				nextStart = compile(e, nextStart);
			}
			return nextStart;
		}

		@Override
		public Hachi6Inst visitSequence(Nez.Sequence p, Object next) {
			// return visit(p.get(0), visit(p.get(1), (Hachi6Inst)next));
			Hachi6Inst nextStart = (Hachi6Inst) next;
			for (int i = p.size() - 1; i >= 0; i--) {
				Expression e = p.get(i);
				nextStart = compile(e, nextStart);
			}
			return nextStart;
		}

		@Override
		public Hachi6Inst visitChoice(Nez.Choice p, Object next) {
			Hachi6Inst nextChoice = compile(p.get(p.size() - 1), (Hachi6Inst) next);
			for (int i = p.size() - 2; i >= 0; i--) {
				Expression e = p.get(i);
				nextChoice = new Hachi6.Alt(nextChoice, compile(e, new Hachi6.Succ((Hachi6Inst) next), nextChoice));
			}
			return nextChoice;
		}

		// AST Construction

		@Override
		public Hachi6Inst visitLink(Nez.LinkTree p, Object next0) {
			Hachi6Inst next = compile(p.get(0), (Hachi6Inst) next0);
			if (strategy.TreeConstruction) {
				return new Hachi6.Link(p.label, next);
			}
			return next;
		}

		@Override
		public Hachi6Inst visitBeginTree(Nez.BeginTree p, Object next) {
			if (strategy.TreeConstruction) {
				return new Hachi6.Init(p.shift, (Hachi6Inst) next);
			}
			return (Hachi6Inst) next;
		}

		@Override
		public Hachi6Inst visitFoldTree(Nez.FoldTree p, Object next) {
			if (strategy.TreeConstruction) {
				return new Hachi6.LeftFold(p.shift, p.label, (Hachi6Inst) next);
			}
			return (Hachi6Inst) next;
		}

		@Override
		public Hachi6Inst visitEndTree(Nez.EndTree p, Object next) {
			if (strategy.TreeConstruction) {
				return new Hachi6.New(p.shift, (Hachi6Inst) next);
			}
			return (Hachi6Inst) next;
		}

		@Override
		public Hachi6Inst visitTag(Nez.Tag p, Object next) {
			if (strategy.TreeConstruction) {
				return new Hachi6.Tag(p.tag, (Hachi6Inst) next);
			}
			return (Hachi6Inst) next;
		}

		@Override
		public Hachi6Inst visitReplace(Nez.Replace p, Object next) {
			if (strategy.TreeConstruction) {
				return new Hachi6.Value(p.value, (Hachi6Inst) next);
			}
			return (Hachi6Inst) next;
		}

		@Override
		public Hachi6Inst visitDetree(Nez.Detree p, Object next) {
			return (Hachi6Inst) next;
		}

		@Override
		public Hachi6Inst visitBlockScope(Nez.BlockScope p, Object next0) {
			Hachi6Inst next = new Hachi6.SClose((Hachi6Inst) next0);
			next = compile(p.get(0), next);
			return new Hachi6.SOpen(next);
		}

		@Override
		public Hachi6Inst visitLocalScope(Nez.LocalScope p, Object next0) {
			Hachi6Inst next = new Hachi6.SClose((Hachi6Inst) next0);
			next = compile(p.get(0), next);
			return new Hachi6.SMask(p.tableName, next);
		}

		@Override
		public Hachi6Inst visitSymbolAction(Nez.SymbolAction p, Object next) {
			return new Hachi6.Pos(compile(p.get(0), new Hachi6.SDef(p.tableName, (Hachi6Inst) next)));
		}

		@Override
		public Hachi6Inst visitSymbolExists(Nez.SymbolExists p, Object next) {
			String symbol = p.symbol;
			if (symbol == null) {
				return new Hachi6.SExists(p.tableName, (Hachi6Inst) next);
			} else {
				return new Hachi6.SIsDef(p.tableName, StringUtils.toUtf8(symbol), (Hachi6Inst) next);
			}
		}

		@Override
		public Hachi6Inst visitSymbolMatch(Nez.SymbolMatch p, Object next) {
			return new Hachi6.SMatch(p.tableName, (Hachi6Inst) next);
		}

		@Override
		public Hachi6Inst visitSymbolPredicate(Nez.SymbolPredicate p, Object next) {
			if (p.op == Predicate.is) {
				return new Hachi6.Pos(compile(p.get(0), new Hachi6.SIs(p.tableName, (Hachi6Inst) next)));
			} else {
				return new Hachi6.Pos(compile(p.get(0), new Hachi6.SIsa(p.tableName, (Hachi6Inst) next)));
			}
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

		// @Override
		// public final Hachi6Inst visitChoice(Nez.Choice p, Object next) {
		// if (/* strategy.isEnabled("Ofirst", Strategy.Ofirst) &&
		// */p.predictedCase != null) {
		// if (p.isTrieTree && strategy.Odfa) {
		// return visitDFirstChoice(p, next);
		// }
		// return visitFirstChoice(p, next);
		// }
		// return visitUnnPchoice(p, next);
		// }
		//
		// private final Hachi6Inst visitFirstChoice(Nez.Choice choice, Object
		// next) {
		// Hachi6Inst[] compiled = new Hachi6Inst[choice.firstInners.length];
		// // Verbose.debug("TrieTree: " + choice.isTrieTree + " " + choice);
		// Hachi6.First dispatch = new Hachi6.First(choice, commonFailure);
		// for (int ch = 0; ch < choice.predictedCase.length; ch++) {
		// Expression predicted = choice.predictedCase[ch];
		// if (predicted == null) {
		// continue;
		// }
		// int index = findIndex(choice, predicted);
		// Hachi6Inst inst = compiled[index];
		// if (inst == null) {
		// // System.out.println("creating '" + (char)ch +
		// // "'("+ch+"): " +
		// // e);
		// if (predicted instanceof Nez.Choice) {
		// assert (((Nez.Choice) predicted).predictedCase == null);
		// inst = visitUnnPchoice(choice, next);
		// } else {
		// inst = compile(predicted, next);
		// }
		// compiled[index] = inst;
		// }
		// dispatch.setJumpTable(ch, inst);
		// }
		// return dispatch;
		// }
		//
		// private final Hachi6Inst visitDFirstChoice(Nez.Choice choice, Object
		// next) {
		// Hachi6Inst[] compiled = new Hachi6Inst[choice.firstInners.length];
		// Hachi6.DFirst dispatch = new Hachi6.DFirst(choice, commonFailure);
		// for (int ch = 0; ch < choice.predictedCase.length; ch++) {
		// Expression predicted = choice.predictedCase[ch];
		// if (predicted == null) {
		// continue;
		// }
		// int index = findIndex(choice, predicted);
		// Hachi6Inst inst = compiled[index];
		// if (inst == null) {
		// Expression next2 = Expressions.next(predicted);
		// if (next2 != null) {
		// inst = compile(next2, next);
		// } else {
		// inst = (Hachi6Inst) next;
		// }
		// compiled[index] = inst;
		// }
		// dispatch.setJumpTable(ch, inst);
		// }
		// return dispatch;
		// }
		//
		// private int findIndex(Nez.Choice choice, Expression e) {
		// for (int i = 0; i < choice.firstInners.length; i++) {
		// if (choice.firstInners[i] == e) {
		// return i;
		// }
		// }
		// return -1;
		// }
		//
		// private final Hachi6Inst visitPredicatedChoice0(Nez.Choice choice,
		// Object next) {
		// HashMap<Integer, Hachi6Inst> m = new HashMap<Integer, Hachi6Inst>();
		// Hachi6.First dispatch = new Hachi6.First(choice, commonFailure);
		// for (int ch = 0; ch < choice.predictedCase.length; ch++) {
		// Expression predicted = choice.predictedCase[ch];
		// if (predicted == null) {
		// continue;
		// }
		// int id = predictId(choice.predictedCase, ch, predicted);
		// Hachi6Inst inst = m.get(id);
		// if (inst == null) {
		// // System.out.println("creating '" + (char)ch +
		// // "'("+ch+"): " +
		// // e);
		// if (predicted instanceof Nez.Choice) {
		// assert (((Nez.Choice) predicted).predictedCase == null);
		// inst = visitUnnPchoice(choice, next);
		// } else {
		// inst = compile(predicted, next);
		// }
		// m.put(id, inst);
		// }
		// dispatch.setJumpTable(ch, inst);
		// }
		// return dispatch;
		// }
		//
		// private int predictId(Expression[] predictedCase, int max, Expression
		// predicted) {
		// // if (predicted.isInterned()) {
		// // return predicted.getId();
		// // }
		// for (int i = 0; i < max; i++) {
		// if (predictedCase[i] != null && predicted.equals(predictedCase[i])) {
		// return i;
		// }
		// }
		// return max;
		// }
		//
		// // public final Hachi6Inst visitUnoptimizedChoice(Nez.Choice p,
		// Object
		// // next) {
		// // return super.visitChoice(p, next);
		// // }
		//
		// // AST Construction
		//
		// @Override
		// public final Hachi6Inst visitLink(Nez.Link p, Object next) {
		// if (strategy.TreeConstruction && p.get(0) instanceof NonTerminal) {
		// NonTerminal n = (NonTerminal) p.get(0);
		// ParseFunc f = this.getParseFunc(n.getProduction());
		// if (f.getMemoPoint() != null) {
		// if (Verbose.PackratParsing) {
		// Verbose.println("memoize: @" + n.getLocalName() + " at " +
		// this.getCompilingProduction().getLocalName());
		// }
		// return memoize(p, n, f, next);
		// }
		// }
		// return visitUnnTlink(p, next);
		// }
		//
		// private Hachi6Inst memoize(Nez.Link p, NonTerminal n, ParseFunc f,
		// Object next) {
		// Hachi6Inst inside = new Hachi6.TMemo(p, f.getMemoPoint(),
		// f.isStateful(), (Hachi6Inst) next);
		// inside = new Hachi6.TCommit(p, inside);
		// inside = visitUnnNonTerminal(n, inside);
		// inside = new Hachi6.TStart(p, inside);
		// inside = new Hachi6.Alt(p, new Hachi6.MemoFail(p, f.isStateful(),
		// f.getMemoPoint()), inside);
		// return new Hachi6.TLookup(p, f.getMemoPoint(), f.isStateful(),
		// inside, (Hachi6Inst) next);
		// }

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
