package nez.parser.vm;

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

public class ParserMachineCompiler implements ParserCompiler {

	public final static ParserMachineCompiler newCompiler(ParserStrategy strategy) {
		return new ParserMachineCompiler(strategy);
	}

	protected ParserStrategy strategy;

	ParserMachineCompiler(ParserStrategy strategy) {
		this.strategy = strategy;
	}

	@Override
	public MozCode compile(Grammar grammar) {
		long t = System.nanoTime();
		MozCode code = new MozCode(grammar);
		if (strategy.PackratParsing) {
			code.initMemoPoint(strategy);
		}
		code.initCoverage(strategy);
		new CompilerVisitor(code, grammar).compile();
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

		private MozCode compile() {
			for (Production p : grammar) {
				this.visitProduction(code.codeList(), p, new Moz86.Ret(p));
			}
			for (MozInst inst : code.codeList()) {
				if (inst instanceof Moz86.Call) {
					((Moz86.Call) inst).sync();
				}
				// Verbose.debug("\t" + inst.id + "\t" + inst);
			}
			return code;
		}

		private Production encodingProduction;

		protected final Production getEncodingProduction() {
			return this.encodingProduction;
		}

		protected void visitProduction(UList<MozInst> codeList, Production p, MozInst next) {
			ProductionCode<MozInst> f = code.getProductionCode(p);
			encodingProduction = p;
			if (strategy.Moz) {
				// next = Coverage.visitExitCoverage(p, next);
				next = compile(p.getExpression(), next, null/* failjump */);
				// next = Coverage.visitEnterCoverage(p, next);
				f.setCompiled(next);
			} else {
				MemoPoint memoPoint = code.getMemoPoint(p.getUniqueName());
				next = compile(memoPoint, p.getExpression(), next);
				f.setCompiled(next);
			}
			MozInst block = new Moz86.Nop(p.getLocalName(), next);
			code.layoutCode(block);
		}

		private MozInst compile(MemoPoint memoPoint, Expression p, MozInst next) {
			if (memoPoint != null) {
				if (memoPoint.typeState == Typestate.Unit) {
					MozInst memo = new Moz86.Memo(null, memoPoint, next);
					MozInst inside = compile(p, memo);
					MozInst failmemo = new Moz86.MemoFail(null, memoPoint);
					inside = new Moz86.Alt(failmemo, inside);
					return new Moz86.Lookup(null, memoPoint, inside, next);
				} else {
					MozInst memo = new Moz86.TMemo(null, memoPoint, next);
					MozInst inside = compile(p, memo);
					MozInst failmemo = new Moz86.MemoFail(null, memoPoint);
					inside = new Moz86.Alt(failmemo, inside);
					return new Moz86.TLookup(memoPoint, inside, next);
				}
			}
			return compile(p, next);
		}

		// encoding

		private MozInst compile(Expression e, Object next) {
			return (MozInst) e.visit(this, next);
		}

		private MozInst compile(Expression e, MozInst next) {
			return (MozInst) e.visit(this, next);
		}

		private MozInst compile(Expression e, MozInst next, Object failjump) {
			return (MozInst) e.visit(this, next);
		}

		@Override
		public MozInst visitEmpty(Nez.Empty p, Object next) {
			return (MozInst) next;
		}

		protected final MozInst commonFailure = new Moz86.Fail(null);

		public MozInst fail(Expression e) {
			return this.commonFailure;
		}

		@Override
		public MozInst visitFail(Nez.Fail p, Object next) {
			return this.commonFailure;
		}

		@Override
		public MozInst visitByte(Nez.Byte p, Object next) {
			if (strategy.BinaryGrammar && p.byteChar == 0) {
				return new Moz86.BinaryByte((MozInst) next);
			}
			return new Moz86.Byte(p.byteChar, (MozInst) next);
		}

		@Override
		public MozInst visitByteSet(Nez.ByteSet p, Object next) {
			if (strategy.BinaryGrammar && p.byteMap[0]) {
				return new Moz86.BinarySet(p.byteMap, (MozInst) next);
			}
			return new Moz86.Set(p.byteMap, (MozInst) next);
		}

		@Override
		public MozInst visitAny(Nez.Any p, Object next) {
			return new Moz86.Any(p, (MozInst) next);
		}

		@Override
		public MozInst visitMultiByte(Nez.MultiByte p, Object next) {
			return new Moz86.Str(p, (MozInst) next);
		}

		@Override
		public final MozInst visitNonTerminal(NonTerminal n, Object next) {
			Production p = n.getProduction();
			if (strategy.Moz) {
				MemoPoint m = code.getMemoPoint(p.getUniqueName());
				ProductionCode<MozInst> f = code.getProductionCode(p);
				if (m != null) {
					if (!strategy.TreeConstruction || m.getTypestate() == Typestate.Unit) {
						if (Verbose.PackratParsing) {
							Verbose.println("memoize: " + n.getLocalName() + " at " + this.getEncodingProduction().getLocalName());
						}
						return memoize(n, f, m, (MozInst) next);
					}
				}
			}
			return this.compileNonTerminal(n, next);
		}

		private MozInst compileNonTerminal(NonTerminal n, Object next) {
			Production p = n.getProduction();
			ProductionCode<MozInst> f = code.getProductionCode(p);
			return new Moz86.Call(f, p.getLocalName(), (MozInst) next);
		}

		private MozInst memoize(NonTerminal n, ProductionCode<MozInst> f, MemoPoint m, MozInst next) {
			MozInst inside = new Moz86.Memo(n, m, next);
			inside = new Moz86.Call(f, n.getLocalName(), inside);
			inside = new Moz86.Alt(n, new Moz86.MemoFail(n, m), inside);
			return new Moz86.Lookup(n, m, inside, next);
		}

		@Override
		public final MozInst visitOption(Nez.Option p, Object next) {
			if (strategy.Olex) {
				Expression inner = getInnerExpression(p);
				if (inner instanceof Nez.Byte) {
					if (strategy.BinaryGrammar && ((Nez.Byte) inner).byteChar == 0) {
						return new Moz86.BinaryOByte((MozInst) next);
					}
					return new Moz86.OByte(((Nez.Byte) inner).byteChar, (MozInst) next);
				}
				if (inner instanceof Nez.ByteSet) {
					if (strategy.BinaryGrammar && ((Nez.ByteSet) inner).byteMap[0]) {
						return new Moz86.BinaryOSet(((Nez.ByteSet) inner).byteMap, (MozInst) next);
					}
					return new Moz86.OSet(((Nez.ByteSet) inner).byteMap, (MozInst) next);
				}
				if (inner instanceof Nez.MultiByte) {
					return new Moz86.OStr(((Nez.MultiByte) inner).byteSeq, (MozInst) next);
				}
			}
			MozInst pop = new Moz86.Succ(p, (MozInst) next);
			return new Moz86.Alt(p, (MozInst) next, compile(p.get(0), pop, next));
		}

		@Override
		public final MozInst visitZeroMore(Nez.ZeroMore p, Object next) {
			return this.visitRepetition(p, next);
		}

		@Override
		public MozInst visitOneMore(Nez.OneMore p, Object next) {
			return compile(p.get(0), this.visitRepetition(p, next));
		}

		private MozInst visitRepetition(Nez.Repetition p, Object next) {
			if (strategy.Olex) {
				Expression inner = getInnerExpression((Expression) p);
				if (inner instanceof Nez.Byte) {
					if (strategy.BinaryGrammar && ((Nez.Byte) inner).byteChar == 0) {
						return new Moz86.BinaryRByte((MozInst) next);
					}
					return new Moz86.RByte(((Nez.Byte) inner).byteChar, (MozInst) next);
				}
				if (inner instanceof Nez.ByteSet) {
					if (strategy.BinaryGrammar && ((Nez.ByteSet) inner).byteMap[0]) {
						return new Moz86.BinaryRSet(((Nez.ByteSet) inner).byteMap, (MozInst) next);
					}
					return new Moz86.RSet(((Nez.ByteSet) inner).byteMap, (MozInst) next);
				}
				if (inner instanceof Nez.MultiByte) {
					return new Moz86.RStr(((Nez.MultiByte) inner).byteSeq, (MozInst) next);
				}
			}
			MozInst skip = new Moz86.Step((Expression) p);
			MozInst start = compile(((Expression) p).get(0), skip, next/* FIXME */);
			skip.next = start;
			return new Moz86.Alt((Expression) p, (MozInst) next, start);
		}

		@Override
		public MozInst visitAnd(Nez.And p, Object next) {
			MozInst inner = compile(p.get(0), new Moz86.Back(p, (MozInst) next));
			return new Moz86.Pos(p, inner);
		}

		@Override
		public final MozInst visitNot(Nez.Not p, Object next) {
			if (strategy.Olex) {
				Expression inner = getInnerExpression(p);
				if (inner instanceof Nez.Byte) {
					if (strategy.BinaryGrammar && ((Nez.Byte) inner).byteChar != 0) {
						return new Moz86.BinaryNByte(((Nez.Byte) inner).byteChar, (MozInst) next);
					}
					return new Moz86.NByte(((Nez.Byte) inner).byteChar, (MozInst) next);
				}
				if (inner instanceof Nez.ByteSet) {
					if (strategy.BinaryGrammar && !((Nez.ByteSet) inner).byteMap[0]) {
						return new Moz86.BinaryNSet(((Nez.ByteSet) inner).byteMap, (MozInst) next);
					}
					return new Moz86.NSet(((Nez.ByteSet) inner).byteMap, (MozInst) next);
				}
				if (inner instanceof Nez.MultiByte) {
					return new Moz86.NStr(((Nez.MultiByte) inner).byteSeq, (MozInst) next);
				}
				if (inner instanceof Nez.Any) {
					return new Moz86.NAny(inner, false, (MozInst) next);
				}
			}
			MozInst fail = new Moz86.Succ(p, new Moz86.Fail(p));
			return new Moz86.Alt(p, (MozInst) next, compile(p.get(0), fail));
		}

		@Override
		public MozInst visitPair(Nez.Pair p, Object next) {
			Object nextStart = next;
			for (int i = p.size() - 1; i >= 0; i--) {
				Expression e = p.get(i);
				nextStart = compile(e, nextStart);
			}
			return (MozInst) nextStart;
		}

		@Override
		public MozInst visitSequence(Nez.Sequence p, Object next) {
			// return visit(p.get(0), visit(p.get(1), (MozInst)next));
			Object nextStart = next;
			for (int i = p.size() - 1; i >= 0; i--) {
				Expression e = p.get(i);
				nextStart = compile(e, nextStart);
			}
			return (MozInst) nextStart;
		}

		@Override
		public final MozInst visitChoice(Nez.Choice p, Object next) {
			if (p.predicted != null) {
				return visitPredictedChoice(p, p.predicted, next);
			}
			return visitUnoptimizedChoice(p, next);
		}

		private final MozInst visitPredictedChoice(Nez.Choice choice, Nez.ChoicePrediction p, Object next) {
			Moz86.Dispatch dispatch = new Moz86.Dispatch(choice, commonFailure);
			MozInst[] compiled = new MozInst[choice.size()];
			for (int i = 0; i < choice.size(); i++) {
				Expression predicted = choice.get(i);
				MozInst inst;
				if (predicted instanceof Nez.Choice) {
					inst = visitUnoptimizedChoice((Nez.Choice) predicted, next);
				} else {
					inst = compile(predicted, next);
				}
				if (p.striped[i]) {
					inst = new Moz86.Move(predicted, 1, inst);
				}
				compiled[i] = inst;
			}
			// Verbose.debug("TrieTree: " + choice.isTrieTree + " " + choice);
			for (int ch = 0; ch < p.indexMap.length; ch++) {
				if (p.indexMap[ch] == 0) {
					continue;
				}
				dispatch.setJumpTable(ch, compiled[p.indexMap[ch] - 1]);
			}
			return dispatch;
		}

		private MozInst visitUnoptimizedChoice(Nez.Choice p, Object next) {
			Object nextChoice = compile(p.get(p.size() - 1), next);
			for (int i = p.size() - 2; i >= 0; i--) {
				Expression e = p.get(i);
				nextChoice = new Moz86.Alt(e, (MozInst) nextChoice, compile(e, new Moz86.Succ(e, (MozInst) next), nextChoice));
			}
			return (MozInst) nextChoice;
		}

		@Override
		public MozInst visitBeginTree(Nez.BeginTree p, Object next) {
			if (strategy.TreeConstruction) {
				return new Moz86.TBegin(p, (MozInst) next);
			}
			return (MozInst) next;
		}

		@Override
		public MozInst visitFoldTree(Nez.FoldTree p, Object next) {
			if (strategy.TreeConstruction) {
				return new Moz86.TFold(p, (MozInst) next);
			}
			return (MozInst) next;
		}

		@Override
		public MozInst visitEndTree(Nez.EndTree p, Object next) {
			if (strategy.TreeConstruction) {
				return new Moz86.TEnd(p, (MozInst) next);
			}
			return (MozInst) next;
		}

		@Override
		public MozInst visitTag(Nez.Tag p, Object next) {
			if (strategy.TreeConstruction) {
				return new Moz86.TTag(p, (MozInst) next);
			}
			return (MozInst) next;
		}

		@Override
		public MozInst visitReplace(Nez.Replace p, Object next) {
			if (strategy.TreeConstruction) {
				return new Moz86.TReplace(p, (MozInst) next);
			}
			return (MozInst) next;
		}

		// Tree

		@Override
		public final MozInst visitLinkTree(Nez.LinkTree p, Object next) {
			if (strategy.TreeConstruction && strategy.Moz && p.get(0) instanceof NonTerminal) {
				NonTerminal n = (NonTerminal) p.get(0);
				MemoPoint m = code.getMemoPoint(n.getUniqueName());
				if (m != null) {
					if (Verbose.PackratParsing) {
						Verbose.println("memoize: @" + n.getLocalName() + " at " + this.getEncodingProduction().getLocalName());
					}
					return memoize(p, n, m, (MozInst) next);
				}
			}
			if (strategy.TreeConstruction) {
				next = new Moz86.TLink(p, (MozInst) next);
				next = compile(p.get(0), next);
				return new Moz86.TPush(p, (MozInst) next);
			}
			return compile(p.get(0), next);
		}

		private MozInst memoize(Nez.LinkTree p, NonTerminal n, MemoPoint m, MozInst next) {
			MozInst inside = new Moz86.TMemo(p, m, next);
			inside = new Moz86.TEmit(p, inside);
			inside = compileNonTerminal(n, inside);
			inside = new Moz86.TStart(p, inside);
			inside = new Moz86.Alt(p, new Moz86.MemoFail(p, m), inside);
			return new Moz86.TLookup(p, m, inside, next);
		}

		@Override
		public MozInst visitDetree(Nez.Detree p, Object next) {
			if (strategy.TreeConstruction) {
				next = new Moz86.TPop(p, (MozInst) next);
				next = compile(p.get(0), next);
				return new Moz86.TPush(p, (MozInst) next);
			}
			return compile(p.get(0), next);
		}

		/* Symbol */

		@Override
		public MozInst visitBlockScope(Nez.BlockScope p, Object next) {
			next = new Moz86.SClose(p, (MozInst) next);
			next = compile(p.get(0), next);
			return new Moz86.SOpen(p, (MozInst) next);
		}

		@Override
		public MozInst visitLocalScope(Nez.LocalScope p, Object next) {
			next = new Moz86.SClose(p, (MozInst) next);
			next = compile(p.get(0), next);
			return new Moz86.SMask(p, (MozInst) next);
		}

		@Override
		public MozInst visitSymbolAction(Nez.SymbolAction p, Object next) {
			return new Moz86.Pos(p, compile(p.get(0), new Moz86.SDef(p, (MozInst) next)));
		}

		@Override
		public MozInst visitSymbolExists(Nez.SymbolExists p, Object next) {
			String symbol = p.symbol;
			if (symbol == null) {
				return new Moz86.SExists(p, (MozInst) next);
			} else {
				return new Moz86.SIsDef(p, (MozInst) next);
			}
		}

		@Override
		public MozInst visitSymbolMatch(Nez.SymbolMatch p, Object next) {
			return new Moz86.SMatch(p, (MozInst) next);
		}

		@Override
		public MozInst visitSymbolPredicate(Nez.SymbolPredicate p, Object next) {
			if (p.op == FunctionName.is) {
				return new Moz86.Pos(p, compile(p.get(0), new Moz86.SIs(p, (MozInst) next)));
			} else {
				return new Moz86.Pos(p, compile(p.get(0), new Moz86.SIsa(p, (MozInst) next)));
			}
		}

		@Override
		public MozInst visitScan(Nez.Scan p, Object next) {
			if (!strategy.Moz) {
				return new Moz86.Pos(p, compile(p.get(0), new Moz86.NScan(p.mask, p.shift, (MozInst) next)));
			}
			return (MozInst) next;
		}

		@Override
		public MozInst visitRepeat(Nez.Repeat p, Object next) {
			MozInst check = new Moz86.NDec((MozInst) next, null);
			MozInst repeated = compile(p.get(0), check);
			check.next = repeated;
			return check;
		}

		@Override
		public MozInst visitLabel(Nez.Label p, Object next) {
			return code.compileCoverage(p.label, p.start, (MozInst) next);
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
			if (strategy.Ostring && inner instanceof Nez.Pair) {
				inner = Expressions.tryConvertingMultiCharSequence((Nez.Pair) inner);
			}
			return inner;
		}

		// AST Construction

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
