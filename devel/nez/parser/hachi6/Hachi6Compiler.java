package nez.parser.hachi6;

import java.util.Arrays;
import java.util.HashMap;

import nez.lang.Expression;
import nez.lang.Production;
import nez.lang.expr.ExpressionCommons;
import nez.parser.ParseFunc;
import nez.parser.ParserGrammar;
import nez.parser.ParserStrategy;
import nez.parser.hachi6.Hachi6Compiler.DefaultVisitor;
import nez.parser.moz.MozCode;
import nez.util.StringUtils;
import nez.util.UList;
import nez.util.Verbose;
import nez.util.VisitorMap;

public class Hachi6Compiler extends VisitorMap<DefaultVisitor> {
	private ParserStrategy strategy;
	private ParserGrammar gg = null;
	private HashMap<String, ParseFunc> funcMap = null;
	private HashMap<Hachi6.Call, ParseFunc> syncMap = new HashMap<>();
	private Production encodingproduction;
	private Hachi6Inst commonFailure = new Hachi6.Fail(null);

	public Hachi6Compiler(ParserStrategy strategy) {
		this.init(Hachi6Compiler.class, new DefaultVisitor());
		this.strategy = strategy;
	}

	public MozCode compile(ParserGrammar gg) {
		// this.setGenerativeGrammar(gg);
		// long t = System.nanoTime();
		// UList<Hachi6Inst> codeList = new UList<Hachi6Inst>(new
		// Hachi6Inst[64]);
		// for (Production p : gg) {
		// if (!p.isSymbolTable()) {
		// this.encodeProduction(codeList, p, new Hachi6.Ret(null));
		// }
		// }
		// for (Hachi6Inst inst : codeList) {
		// if (inst instanceof Hachi6.Call) {
		// if (((Hachi6.Call) inst).jump == null) {
		// ((Hachi6.Call) inst).jump = syncMap.get(inst).getCompiled();
		// }
		// }
		// }
		// long t2 = System.nanoTime();
		// Verbose.printElapsedTime("CompilingTime", t, t2);
		// return new MozCode(gg, codeList, gg.memoPointList);
		return null;
	}

	private void encodeProduction(UList<Hachi6Inst> codeList, Production p, Hachi6Inst next) {
		// ParseFunc f = this.getParseFunc(p);
		// encodingproduction = p;
		// if (!f.isInlined()) {
		// next = Coverage.encodeEnterCoverage(p, next);
		// }
		// f.setCompiled(generate(f.getExpression(), next));
		// if (!f.isInlined()) {
		// f.setCompiled(Coverage.encodeEnterCoverage(p, f.getCompiled()));
		// }
		// Hachi6Inst block = new Hachi6.Label(p.getLocalName(),
		// f.getCompiled());
		// this.layoutCode(codeList, block);
	}

	private final void layoutCode(UList<Hachi6Inst> codeList, Hachi6Inst inst) {
		if (inst == null) {
			return;
		}
		if (inst.id == -1) {
			inst.id = codeList.size();
			codeList.add(inst);
			layoutCode(codeList, inst.next);
			// if (inst.next != null && inst.id + 1 != inst.next.id) {
			// this.labeling(inst.next);
			// }
			if (inst instanceof Hachi6Branch) {
				layoutCode(codeList, ((Hachi6Branch) inst).jump);
			}
			if (inst instanceof Hachi6BranchTable) {
				Hachi6BranchTable match = (Hachi6BranchTable) inst;
				for (int ch = 0; ch < match.jumpTable.length; ch++) {
					layoutCode(codeList, match.jumpTable[ch]);
				}
			}

		}
	}

	public Hachi6Inst generate(Expression e, Hachi6Inst next) {
		return find(e.getClass().getSimpleName()).accept(e, next);
	}

	public void setGenerativeGrammar(ParserGrammar gg) {
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

	private Expression getInnerExpression(Expression p) {
		Expression inner = ExpressionCommons.resolveNonTerminal(p.get(0));
		if (strategy.Ostring && inner instanceof nez.lang.expr.Psequence) {
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
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			return next;
		}
	}

	public class NonTerminal extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			nez.lang.expr.NonTerminal n = (nez.lang.expr.NonTerminal) e;
			Production p = n.getProduction();
			if (p == null) {
				Verbose.debug("[PANIC] unresolved: " + n.getLocalName() + " ***** ");
				return next;
			}
			ParseFunc f = getParseFunc(p);
			if (f.isInlined()) {
				optimizedInline(p);
				return generate(f.getExpression(), next);
			}
			if (f.getMemoPoint() != null) {
				if (!strategy.TreeConstruction || p.isNoNTreeConstruction()) {
					if (Verbose.PackratParsing) {
						Verbose.println("memoize: " + n.getLocalName() + " at " + getEncodingProduction().getLocalName());
					}
					// Hachi6Inst inside = new Memo(n, next, f.getState(),
					// f.getMemoPoint().id);
					// inside = new Call(null, inside, null, n.getLocalName());
					// syncMap.put((Call) inside, f);
					// inside = new Alt(n, inside, new MemoFail(n, null,
					// f.getState(), f.getMemoPoint().id));
					// return new Lookup(n, inside, f.getState(),
					// f.getMemoPoint().id, next);
					Hachi6Inst memo = new Hachi6.Memo(f.getMemoPoint().id, next);
					Hachi6.Call call = new Hachi6.Call(null, n.getLocalName(), memo);
					syncMap.put(call, f);
					return new Hachi6.Lookup(next, f.getMemoPoint().id, call);
				}
			}
			Hachi6.Call call = new Hachi6.Call(null, n.getLocalName(), next);
			syncMap.put(call, f);
			return call;
		}
	}

	public class Pempty extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			return next;
		}
	}

	public class Pfail extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			return commonFailure;
		}
	}

	public class _Cbyte extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			return new Hachi6.Byte(((nez.lang.expr.Cbyte) e).byteChar, next);
		}
	}

	public class Cany extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			return new Hachi6.Any(next);
		}
	}

	public class Cset extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			return new Hachi6.Set(((nez.lang.expr.Cset) e).byteMap, next);
		}
	}

	public class Cmulti extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			return new Hachi6.Str(((nez.lang.expr.Cmulti) e).byteSeq, next);
		}
	}

	public class Pchoice extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			nez.lang.expr.Pchoice p = (nez.lang.expr.Pchoice) e;
			if (p.predictedCase != null) {
				if (p.isTrieTree && strategy.Odfa) {
					return encodeDFirstChoice(p, next);
				}
				return encodeFirstChoice(p, next);
			}
			return encodePchoice(p, next);
		}

		private Hachi6Inst encodePchoice(nez.lang.expr.Pchoice p, Hachi6Inst next) {
			Hachi6Inst nextChoice = generate(p.get(p.size() - 1), next);
			for (int i = p.size() - 2; i >= 0; i--) {
				Expression e = p.get(i);
				nextChoice = new Hachi6.Alt(nextChoice, generate(e, new Hachi6.Succ(next)));
			}
			return nextChoice;
		}

		private Hachi6Inst encodeFirstChoice(nez.lang.expr.Pchoice choice, Hachi6Inst next) {
			Hachi6Inst[] compiled = new Hachi6Inst[choice.firstInners.length];
			Hachi6Inst[] jumpTable = new Hachi6Inst[257];
			Arrays.fill(jumpTable, commonFailure);
			for (int ch = 0; ch < choice.predictedCase.length; ch++) {
				Expression predicted = choice.predictedCase[ch];
				if (predicted == null) {
					continue;
				}
				int index = findIndex(choice, predicted);
				Hachi6Inst inst = compiled[index];
				if (inst == null) {
					if (predicted instanceof nez.lang.expr.Pchoice) {
						assert (((nez.lang.expr.Pchoice) predicted).predictedCase == null);
						inst = encodePchoice(choice, next);
					} else {
						inst = generate(predicted, next);
					}
					compiled[index] = inst;
				}
				jumpTable[ch] = inst;
			}
			return new Hachi6.EDispatch(jumpTable, commonFailure);
		}

		private Hachi6Inst encodeDFirstChoice(nez.lang.expr.Pchoice choice, Hachi6Inst next) {
			Hachi6Inst[] compiled = new Hachi6Inst[choice.firstInners.length];
			Hachi6Inst[] jumpTable = new Hachi6Inst[257];
			Arrays.fill(jumpTable, commonFailure);
			for (int ch = 0; ch < choice.predictedCase.length; ch++) {
				Expression predicted = choice.predictedCase[ch];
				if (predicted == null) {
					continue;
				}
				int index = findIndex(choice, predicted);
				Hachi6Inst inst = compiled[index];
				if (inst == null) {
					Expression next2 = predicted.getNext();
					if (next2 != null) {
						inst = generate(next2, next);
					} else {
						inst = next;
					}
					compiled[index] = inst;
				}
				jumpTable[ch] = inst;
			}
			return new Hachi6.Dispatch(jumpTable, commonFailure);
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
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			nez.lang.expr.Psequence p = (nez.lang.expr.Psequence) e;
			if (strategy.Ostring) {
				Expression inner = p.toMultiCharSequence();
				if (inner instanceof nez.lang.expr.Cmulti) {
					Cmulti cmulti = new Cmulti();
					return cmulti.accept(inner, next);
				}
			}
			Hachi6Inst nextStart = next;
			for (int i = p.size() - 1; i >= 0; i--) {
				Expression inner = p.get(i);
				nextStart = generate(inner, nextStart);
			}
			return nextStart;
		}
	}

	public class Poption extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			if (strategy.Olex) {
				Expression inner = getInnerExpression(e);
				if (inner instanceof nez.lang.expr.Cbyte) {
					optimizedUnary(e);
					return new Hachi6.OByte(((nez.lang.expr.Cbyte) inner).byteChar, next);
				}
				if (inner instanceof nez.lang.expr.Cset) {
					optimizedUnary(e);
					return new Hachi6.OSet(((nez.lang.expr.Cset) inner).byteMap, next);
				}
				if (inner instanceof nez.lang.expr.Cmulti) {
					optimizedUnary(e);
					return new Hachi6.OStr(((nez.lang.expr.Cmulti) inner).byteSeq, next);
				}
			}
			Hachi6Inst pop = new Hachi6.Succ(next);
			return new Hachi6.Alt(next, generate(e.get(0), pop));
		}
	}

	public class Pzero extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			if (strategy.Olex) {
				Expression inner = getInnerExpression(e);
				if (inner instanceof nez.lang.expr.Cbyte) {
					optimizedUnary(e);
					return new Hachi6.RByte(((nez.lang.expr.Cbyte) inner).byteChar, next);
				}
				if (inner instanceof nez.lang.expr.Cset) {
					optimizedUnary(e);
					return new Hachi6.RSet(((nez.lang.expr.Cset) inner).byteMap, next);
				}
				if (inner instanceof nez.lang.expr.Cmulti) {
					optimizedUnary(e);
					return new Hachi6.RStr(((nez.lang.expr.Cmulti) inner).byteSeq, next);
				}
			}
			Hachi6Inst skip = new Hachi6.Guard(null);
			Hachi6Inst start = generate(e.get(0), skip);
			skip.next = start;
			return new Hachi6.Alt(next, start);
		}
	}

	public class Pone extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			Pzero pzero = new Pzero();
			return generate(e.get(0), pzero.accept(e, next));
		}
	}

	public class Pand extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			Hachi6Inst inner = generate(e.get(0), new Hachi6.Back(next));
			return new Hachi6.Pos(inner);
		}
	}

	public class Pnot extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			if (strategy.Olex) {
				Expression inner = getInnerExpression(e);
				if (inner instanceof nez.lang.expr.Cset) {
					optimizedUnary(e);
					return new Hachi6.NSet(((nez.lang.expr.Cset) inner).byteMap, next);
				}
				if (inner instanceof nez.lang.expr.Cbyte) {
					optimizedUnary(e);
					return new Hachi6.NByte(((nez.lang.expr.Cbyte) inner).byteChar, next);
				}
				if (inner instanceof nez.lang.expr.Cany) {
					optimizedUnary(e);
					return new Hachi6.NAny(next);
				}
				if (inner instanceof nez.lang.expr.Cmulti) {
					optimizedUnary(e);
					return new Hachi6.NStr(((nez.lang.expr.Cmulti) inner).byteSeq, next);
				}
			}
			Hachi6Inst fail = new Hachi6.Succ(new Hachi6.Fail(null));
			return new Hachi6.Alt(next, generate(e.get(0), fail));
		}
	}

	public class Tnew extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			if (strategy.TreeConstruction) {
				nez.lang.expr.Tnew p = (nez.lang.expr.Tnew) e;
				return new Hachi6.Init(p.shift, next);
			}
			return next;
		}
	}

	public class Tlink extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			nez.lang.expr.Tlink p = (nez.lang.expr.Tlink) e;
			if (strategy.TreeConstruction) {
				next = new Hachi6.Link(p.getLabel(), next);
			}
			return generate(e.get(0), next);
		}
	}

	public class Tlfold extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			if (strategy.TreeConstruction) {
				nez.lang.expr.Tlfold p = (nez.lang.expr.Tlfold) e;
				return new Hachi6.LeftFold(p.shift, p.getLabel(), next);
			}
			return next;
		}
	}

	public class Ttag extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			if (strategy.TreeConstruction) {
				return new Hachi6.Tag(((nez.lang.expr.Ttag) e).tag, next);
			}
			return next;
		}
	}

	public class Treplace extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			if (strategy.TreeConstruction) {
				return new Hachi6.Value(((nez.lang.expr.Treplace) e).value, next);
			}
			return next;
		}
	}

	public class Tcapture extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			if (strategy.TreeConstruction) {
				nez.lang.expr.Tcapture p = (nez.lang.expr.Tcapture) e;
				return new Hachi6.New(p.shift, next);
			}
			return next;
		}
	}

	public class Tdetree extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			return next;
		}
	}

	public class Xblock extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			next = new Hachi6.SClose(next);
			next = generate(e.get(0), next);
			return new Hachi6.SOpen(next);
		}
	}

	public class _Xlocal extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			next = new Hachi6.SClose(next);
			next = new Hachi6.SMask(((nez.lang.expr.Xsymbol) e).getTable(), generate(e.get(0), next));
			return new Hachi6.SOpen(next);
		}
	}

	public class Xif extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			return next;
		}
	}

	public class Xon extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			return generate(e.get(0), next);
		}
	}

	public class Xsymbol extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			nez.lang.expr.Xsymbol p = (nez.lang.expr.Xsymbol) e;
			return new Hachi6.Pos(generate(p.get(0), new Hachi6.SDef(p.getTable(), next)));
		}
	}

	public class Xexists extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			nez.lang.expr.Xexists p = (nez.lang.expr.Xexists) e;
			String symbol = p.getSymbol();
			if (symbol == null) {
				return new Hachi6.SExists(p.getTable(), next);
			} else {
				return new Hachi6.SIsDef(p.getTable(), StringUtils.toUtf8(symbol), next);
			}
		}
	}

	public class Xmatch extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			nez.lang.expr.Xmatch p = (nez.lang.expr.Xmatch) e;
			return new Hachi6.SMatch(p.getTable(), next);
		}
	}

	public class Xis extends DefaultVisitor {
		@Override
		public Hachi6Inst accept(Expression e, Hachi6Inst next) {
			nez.lang.expr.Xis p = (nez.lang.expr.Xis) e;
			if (p.is) {
				return new Hachi6.Pos(generate(p.get(0), new Hachi6.SIs(p.getTable(), next)));
			} else {
				return new Hachi6.Pos(generate(p.get(0), new Hachi6.SIsa(p.getTable(), next)));
			}
		}
	}

}
