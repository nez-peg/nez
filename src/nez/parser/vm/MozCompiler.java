package nez.parser.vm;

import java.util.HashMap;

import nez.Strategy;
import nez.Verbose;
import nez.lang.Expression;
import nez.lang.Production;
import nez.lang.expr.ExpressionCommons;
import nez.parser.GenerativeGrammar;
import nez.parser.Instruction;
import nez.parser.ParseFunc;
import nez.parser.vm.MozCompiler.DefaultVisitor;
import nez.util.StringUtils;
import nez.util.VisitorMap;

public class MozCompiler extends VisitorMap<DefaultVisitor> {
	private Strategy strategy;
	private boolean enabledASTConstruction;
	private GenerativeGrammar gg = null;
	private HashMap<String, ParseFunc> funcMap = null;
	private Production encodingproduction;

	public MozCompiler(Strategy strategy) {
		this.init(MozCompiler.class, new DefaultVisitor());
		this.strategy = strategy;
		if (this.strategy != null) {
			this.enabledASTConstruction = strategy.isEnabled("ast", Strategy.AST);
		}
	}

	public Instruction generate(Expression e, Instruction next) {
		return find(e.getClass().getSimpleName()).accept(e, next);
	}

	public void setGenerativeGrammar(GenerativeGrammar gg) {
		this.gg = gg;
	}

	private ParseFunc getParseFunc(Production p) {
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

	private Instruction labeling(Instruction inst) {
		if (inst != null) {
			inst.label = true;
		}
		return inst;
	}

	private Expression getInnerExpression(Expression p) {
		Expression inner = ExpressionCommons.resolveNonTerminal(p.get(0));
		if (strategy.isEnabled("Ostr", Strategy.Ostr) && inner instanceof nez.lang.expr.Psequence) {
			inner = ((nez.lang.expr.Psequence) inner).toMultiCharSequence();
		}
		return inner;
	}

	private void optimizedUnary(Expression p) {
		Verbose.noticeOptimize("specialization", p);
	}

	private void optimizedInline(Production p) {
		Verbose.noticeOptimize("inlining", p.getExpression());
	}

	private final Production getEncodingProduction() {
		return this.encodingproduction;
	}

	public class DefaultVisitor {
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
		/**
		 * public boolean accept(Expression e, String a) { return false; }
		 **/
	}

	public class NonTerminal extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			nez.lang.expr.NonTerminal n = (nez.lang.expr.NonTerminal) e;
			Production p = n.getProduction();
			if (p == null) {
				Verbose.debug("[PANIC] unresolved: " + n.getLocalName() + " ***** ");
				return next;
			}
			ParseFunc f = getParseFunc(p);
			if (f.getInlining()) {
				optimizedInline(p);
				return generate(f.getExpression(), next);
			}
			if (f.getMemoPoint() != null) {
				if (!enabledASTConstruction || p.isNoNTreeConstruction()) {
					if (Verbose.PackratParsing) {
						Verbose.println("memoize: " + n.getLocalName() + " at " + getEncodingProduction().getLocalName());
					}
					Instruction inside = new Memo(n, next, f.getState(), f.getMemoPoint().id);
					inside = new Call(null, labeling(f.getCompiled()), labeling(inside), n.getLocalName());
					inside = new Alt(n, inside, new MemoFail(n, null, f.getState(), f.getMemoPoint().id));
					return new Lookup(n, inside, f.getState(), f.getMemoPoint().id, next);
				}
			}
			return new Call(null, labeling(f.getCompiled()), labeling(next), n.getLocalName());
		}
	}

	public class Pempty extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Pfail extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return new Fail(null, next);
		}
	}

	public class Cbyte extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			nez.lang.expr.Cbyte p = (nez.lang.expr.Cbyte) e;
			return new Byte(p, next, p.byteChar);
		}
	}

	public class Cany extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return new Any(e, next);
		}
	}

	public class Cset extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			nez.lang.expr.Cset p = (nez.lang.expr.Cset) e;
			return new Set(p, next, p.byteMap);
		}
	}

	public class Cmulti extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			nez.lang.expr.Cmulti p = (nez.lang.expr.Cmulti) e;
			return new Str(p, next, p.byteSeq);
		}
	}

	public class Pchoice extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			nez.lang.expr.Pchoice p = (nez.lang.expr.Pchoice) e;
			if (p.predictedCase != null) {
				if (p.isTrieTree && strategy.isEnabled("Odfa", Strategy.Odfa)) {
					return encodeDFirstChoice(p, next);
				}
				return encodeFirstChoice(p, next);
			}
			return encodePchoice(p, next);
		}

		private Instruction encodePchoice(nez.lang.expr.Pchoice p, Instruction next) {
			Instruction nextChoice = generate(p.get(p.size() - 1), next);
			for (int i = p.size() - 2; i >= 0; i--) {
				Expression e = p.get(i);
				nextChoice = new Alt(e, generate(e, new Succ(e, next)), nextChoice);
			}
			return nextChoice;
		}

		private Instruction encodeFirstChoice(nez.lang.expr.Pchoice choice, Instruction next) {
			Instruction[] compiled = new Instruction[choice.firstInners.length];
			Instruction[] jumpTable = new Instruction[257];
			for (int ch = 0; ch < choice.predictedCase.length; ch++) {
				Expression predicted = choice.predictedCase[ch];
				if (predicted == null) {
					continue;
				}
				int index = findIndex(choice, predicted);
				Instruction inst = compiled[index];
				if (inst == null) {
					if (predicted instanceof nez.lang.expr.Pchoice) {
						assert(((nez.lang.expr.Pchoice) predicted).predictedCase == null);
						inst = encodePchoice(choice, next);
					} else {
						inst = generate(predicted, next);
					}
					compiled[index] = inst;
				}
				jumpTable[ch] = labeling(inst);
			}
			return new First(choice, new Fail(null, next), jumpTable);
		}

		private Instruction encodeDFirstChoice(nez.lang.expr.Pchoice choice, Instruction next) {
			Instruction[] compiled = new Instruction[choice.firstInners.length];
			Instruction[] jumpTable = new Instruction[257];
			for (int ch = 0; ch < choice.predictedCase.length; ch++) {
				Expression predicted = choice.predictedCase[ch];
				if (predicted == null) {
					continue;
				}
				int index = findIndex(choice, predicted);
				Instruction inst = compiled[index];
				if (inst == null) {
					Expression next2 = predicted.getNext();
					if (next2 != null) {
						inst = generate(next2, next);
					} else {
						inst = next;
					}
					compiled[index] = inst;
				}
				jumpTable[ch] = labeling(inst);
			}
			return new DFirst(choice, new Fail(null, next), jumpTable);
		}

		private int findIndex(nez.lang.expr.Pchoice choice, Expression e) {
			for (int i = 0; i < choice.firstInners.length; i++) {
				if (choice.firstInners[i] == e) {
					return i;
				}
			}
			return -1;
		}
	}

	public class Psequence extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			nez.lang.expr.Psequence p = (nez.lang.expr.Psequence) e;
			if (strategy.isEnabled("Ostr", Strategy.Ostr)) {
				Expression inner = p.toMultiCharSequence();
				if (inner instanceof nez.lang.expr.Cmulti) {
					Cmulti cmulti = new Cmulti();
					return cmulti.accept(inner, next);
				}
			}
			Instruction nextStart = next;
			for (int i = p.size() - 1; i >= 0; i--) {
				Expression inner = p.get(i);
				nextStart = generate(inner, nextStart);
			}
			return nextStart;
		}
	}

	public class Poption extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			if (strategy.isEnabled("Olex", Strategy.Olex)) {
				Expression inner = getInnerExpression(e);
				if (inner instanceof nez.lang.expr.Cbyte) {
					optimizedUnary(e);
					return new OByte(inner, next, ((nez.lang.expr.Cbyte) inner).byteChar);
				}
				if (inner instanceof nez.lang.expr.Cset) {
					optimizedUnary(e);
					return new OSet(inner, next, ((nez.lang.expr.Cset) inner).byteMap);
				}
				if (inner instanceof nez.lang.expr.Cmulti) {
					optimizedUnary(e);
					return new OStr(inner, next, ((nez.lang.expr.Cmulti) inner).byteSeq);
				}
			}
			Instruction pop = new Succ(e, next);
			return new Alt(e, generate(e.get(0), pop), next);
		}
	}

	public class Pzero extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			if (strategy.isEnabled("Olex", Strategy.Olex)) {
				Expression inner = getInnerExpression(e);
				if (inner instanceof nez.lang.expr.Cbyte) {
					optimizedUnary(e);
					return new RByte(inner, next, ((nez.lang.expr.Cbyte) inner).byteChar);
				}
				if (inner instanceof nez.lang.expr.Cset) {
					optimizedUnary(e);
					return new RSet(inner, next, ((nez.lang.expr.Cset) inner).byteMap);
				}
				if (inner instanceof nez.lang.expr.Cmulti) {
					optimizedUnary(e);
					return new RStr(inner, next, ((nez.lang.expr.Cmulti) inner).byteSeq);
				}
			}
			Instruction skip = new Skip(e, null);
			Instruction start = generate(e.get(0), skip);
			skip.next = start;
			return new Alt(e, start, next);
		}
	}

	public class Pone extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			Pzero pzero = new Pzero();
			return generate(e.get(0), pzero.accept(e, next));
		}
	}

	public class Pand extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			Instruction inner = generate(e.get(0), new Back(e, next));
			return new Pos(e, inner);
		}
	}

	public class Pnot extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			if (strategy.isEnabled("Olex", Strategy.Olex)) {
				Expression inner = getInnerExpression(e);
				if (inner instanceof nez.lang.expr.Cset) {
					optimizedUnary(e);
					return new NSet(inner, next, ((nez.lang.expr.Cset) inner).byteMap);
				}
				if (inner instanceof nez.lang.expr.Cbyte) {
					optimizedUnary(e);
					return new NByte(inner, next, ((nez.lang.expr.Cbyte) inner).byteChar);
				}
				if (inner instanceof nez.lang.expr.Cany) {
					optimizedUnary(e);
					return new NAny(inner, next);
				}
				if (inner instanceof nez.lang.expr.Cmulti) {
					optimizedUnary(e);
					return new NStr(inner, next, ((nez.lang.expr.Cmulti) inner).byteSeq);
				}
			}
			Instruction fail = new Succ(e, new Fail(null, next));
			return new Alt(e, generate(e.get(0), fail), next);
		}
	}

	public class Tnew extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			if (enabledASTConstruction) {
				nez.lang.expr.Tnew p = (nez.lang.expr.Tnew) e;
				return new TNew(p, next, p.shift);
			}
			return next;
		}
	}

	public class Tlink extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			nez.lang.expr.Tlink p = (nez.lang.expr.Tlink) e;
			if (enabledASTConstruction && p.get(0) instanceof nez.lang.expr.NonTerminal) {
				nez.lang.expr.NonTerminal n = (nez.lang.expr.NonTerminal) p.get(0);
				ParseFunc f = getParseFunc(n.getProduction());
				if (f.getMemoPoint() != null) {
					if (Verbose.PackratParsing) {
						Verbose.println("memoize: @" + n.getLocalName() + "at" + getEncodingProduction().getLocalName());
					}
					Instruction inside = new TMemo(p, next, f.getState(), f.getMemoPoint().id);
					inside = new TCommit(p, inside, p.getLabel());
					inside = new Call(null, labeling(f.getCompiled()), labeling(inside), n.getLocalName());
					inside = new TStart(p, inside);
					inside = new Alt(p, inside, new MemoFail(p, null, f.getState(), f.getMemoPoint().id));
					return new TLookup(p, inside, f.getState(), f.getMemoPoint().id, next, p.getLabel());
				}
			}
			if (enabledASTConstruction) {
				next = new TPop(p, next, p.getLabel());
				next = generate(p.get(0), next);
				return new TPush(p, next);
			}
			return generate(e.get(0), next);
		}
	}

	public class Tlfold extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			if (enabledASTConstruction) {
				nez.lang.expr.Tlfold p = (nez.lang.expr.Tlfold) e;
				return new TLeftFold(p, next, p.shift, p.getLabel());
			}
			return next;
		}
	}

	public class Ttag extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			if (enabledASTConstruction) {
				nez.lang.expr.Ttag p = (nez.lang.expr.Ttag) e;
				return new TTag(p, next, p.tag);
			}
			return next;
		}
	}

	public class Treplace extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			if (enabledASTConstruction) {
				nez.lang.expr.Treplace p = (nez.lang.expr.Treplace) e;
				byte[] utf8 = p.value.getBytes();
				return new TReplace(p, next, utf8);
			}
			return next;
		}
	}

	public class Tcapture extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			if (enabledASTConstruction) {
				nez.lang.expr.Tcapture p = (nez.lang.expr.Tcapture) e;
				return new TCapture(p, next, p.shift);
			}
			return next;
		}
	}

	public class Tdetree extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Xblock extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			next = new SClose(e, next);
			next = generate(e.get(0), next);
			return new SOpen(e, next);
		}
	}

	public class Xlocal extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			next = new SClose(e, next);
			next = generate(e.get(0), next);
			return new SOpen(e, next);
		}
	}

	public class Xif extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Xon extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return generate(e.get(0), next);
		}
	}

	public class Xsymbol extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			nez.lang.expr.Xsymbol p = (nez.lang.expr.Xsymbol) e;
			return new Pos(p, generate(p.get(0), new SDef(p, next, p.getTable())));
		}
	}

	public class Xexists extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			nez.lang.expr.Xexists p = (nez.lang.expr.Xexists) e;
			String symbol = p.getSymbol();
			if (symbol == null) {
				return new SExists(p, next, p.getTable());
			} else {
				return new SIsDef(p, next, p.getTable(), StringUtils.toUtf8(symbol));
			}
		}
	}

	public class Xmatch extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			nez.lang.expr.Xmatch p = (nez.lang.expr.Xmatch) e;
			return new SMatch(p, next, p.getTable());
		}
	}

	public class Xis extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			nez.lang.expr.Xis p = (nez.lang.expr.Xis) e;
			if (p.is) {
				return new Pos(p, generate(p.get(0), new SIs(p, next, p.getTable())));
			} else {
				return new Pos(p, generate(p.get(0), new SIsa(p, next, p.getTable())));
			}
		}
	}

}
