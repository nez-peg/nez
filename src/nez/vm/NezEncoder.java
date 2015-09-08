package nez.vm;

import java.util.HashMap;
import java.util.List;

import nez.NezOption;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.GrammarOptimizer;
import nez.lang.Production;
import nez.lang.Typestate;
import nez.lang.expr.And;
import nez.lang.expr.AnyChar;
import nez.lang.expr.Block;
import nez.lang.expr.ByteChar;
import nez.lang.expr.ByteMap;
import nez.lang.expr.Capture;
import nez.lang.expr.Choice;
import nez.lang.expr.DefIndent;
import nez.lang.expr.DefSymbol;
import nez.lang.expr.ExistsSymbol;
import nez.lang.expr.IfFlag;
import nez.lang.expr.IsIndent;
import nez.lang.expr.IsSymbol;
import nez.lang.expr.Link;
import nez.lang.expr.LocalTable;
import nez.lang.expr.MatchSymbol;
import nez.lang.expr.MultiChar;
import nez.lang.expr.New;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Not;
import nez.lang.expr.OnFlag;
import nez.lang.expr.Option;
import nez.lang.expr.Repetition;
import nez.lang.expr.Repetition1;
import nez.lang.expr.Replace;
import nez.lang.expr.Sequence;
import nez.lang.expr.Tagging;
import nez.main.Verbose;

public abstract class NezEncoder {
	protected NezOption option;

	public NezEncoder(NezOption option) {
		this.option = option;
	}

	public final NezOption getOption() {
		return this.option;
	}

	/* CodeMap */

	protected HashMap<String, ProductionCode> pcodeMap = null;

	protected ProductionCode newProductionCode(Production p, Expression localExpression) {
		return new ProductionCode(p, localExpression);
	}

	protected ProductionCode getCodePoint(Production p) {
		if (this.pcodeMap != null) {
			return pcodeMap.get(p.getUniqueName());
		}
		return null;
	}

	protected Expression optimizeLocalProduction(Production p) {
		return p.getExpression();
	}

	void count(Production p) {
		String uname = p.getUniqueName();
		ProductionCode c = this.pcodeMap.get(uname);
		if (c == null) {
			Expression deref = optimizeLocalProduction(p);
			if (deref.isInterned()) {
				String key = "#" + deref.getId();
				c = this.pcodeMap.get(key);
				if (c == null) {
					c = newProductionCode(p, deref);
					pcodeMap.put(key, c);
				}
				// else {
				// Verbose.debug("alias " + uname + ", " +
				// c.production.getUniqueName());
				// }
				pcodeMap.put(uname, c);
			} else {
				c = newProductionCode(p, deref);
				pcodeMap.put(uname, c);
			}
		}
		c.ref++;
	}

	void countNonTerminalReference(Expression e) {
		if (e instanceof NonTerminal) {
			Production p = ((NonTerminal) e).getProduction();
			count(p);
		}
		for (Expression sub : e) {
			countNonTerminalReference(sub);
		}
	}

	protected void initProductionCodeMap(Grammar grammar, List<MemoPoint> memoPointList) {
		this.pcodeMap = new HashMap<String, ProductionCode>();
		Production start = grammar.getStartProduction();
		count(start);
		countNonTerminalReference(start.getExpression());
		for (Production p : grammar.getProductionList()) {
			if (p != start) {
				this.countNonTerminalReference(p.getExpression());
			}
		}
		if (option.enabledInlining) {
			for (Production p : grammar.getProductionList()) {
				ProductionCode cp = this.pcodeMap.get(p.getUniqueName());
				this.checkInlining(cp);
			}
		}
		if (memoPointList != null) {
			for (Production p : grammar.getProductionList()) {
				ProductionCode cp = this.pcodeMap.get(p.getUniqueName());
				this.checkMemoizing(cp, memoPointList);
			}
		}
	}

	protected void checkInlining(ProductionCode pcode) {
		if (pcode.ref == 1 || GrammarOptimizer.isSingleCharacter(pcode.localExpression)) {
			if (Verbose.PackratParsing) {
				Verbose.println("Inlining: " + pcode.getLocalName());
			}
			pcode.inlining = true;
		}
	}

	protected void checkMemoizing(ProductionCode pcode, List<MemoPoint> memoPointList) {
		if (pcode.inlining || pcode.memoPoint != null) {
			return;
		}
		Production p = pcode.production;
		if (pcode.ref > 1 && p.inferTypestate() != Typestate.OperationType) {
			int memoId = memoPointList.size();
			pcode.memoPoint = new MemoPoint(memoId, p.getLocalName(), pcode.localExpression, p.isContextual());
			memoPointList.add(pcode.memoPoint);
			if (Verbose.PackratParsing) {
				Verbose.println("MemoPoint: " + pcode.memoPoint + " ref=" + pcode.ref + " pure? " + p.isNoNTreeConstruction() + " rec? " + p.isRecursive());
			}
		}
	}

	// encoding

	// private Instruction failed = new IFail(null);

	public abstract Instruction encode(Expression e, Instruction next, Instruction failjump);

	public abstract Instruction encodeFail(Expression p);

	public abstract Instruction encodeAnyChar(AnyChar p, Instruction next, Instruction failjump);

	public abstract Instruction encodeByteChar(ByteChar p, Instruction next, Instruction failjump);

	public abstract Instruction encodeByteMap(ByteMap p, Instruction next, Instruction failjump);

	public abstract Instruction encodeMultiChar(MultiChar p, Instruction next, Instruction failjump);

	public abstract Instruction encodeOption(Option p, Instruction next);

	public abstract Instruction encodeRepetition(Repetition p, Instruction next);

	public abstract Instruction encodeRepetition1(Repetition1 p, Instruction next, Instruction failjump);

	public abstract Instruction encodeAnd(And p, Instruction next, Instruction failjump);

	public abstract Instruction encodeNot(Not p, Instruction next, Instruction failjump);

	public abstract Instruction encodeSequence(Sequence p, Instruction next, Instruction failjump);

	public abstract Instruction encodeChoice(Choice p, Instruction next, Instruction failjump);

	public abstract Instruction encodeNonTerminal(NonTerminal p, Instruction next, Instruction failjump);

	// AST Construction
	public abstract Instruction encodeLink(Link p, Instruction next, Instruction failjump);

	public abstract Instruction encodeNew(New p, Instruction next);

	public abstract Instruction encodeCapture(Capture p, Instruction next);

	public abstract Instruction encodeTagging(Tagging p, Instruction next);

	public abstract Instruction encodeReplace(Replace p, Instruction next);

	// Symbol Tables
	public abstract Instruction encodeBlock(Block p, Instruction next, Instruction failjump);

	public abstract Instruction encodeDefSymbol(DefSymbol p, Instruction next, Instruction failjump);

	public abstract Instruction encodeMatchSymbol(MatchSymbol p, Instruction next, Instruction failjump);

	public abstract Instruction encodeIsSymbol(IsSymbol p, Instruction next, Instruction failjump);

	public abstract Instruction encodeDefIndent(DefIndent p, Instruction next, Instruction failjump);

	public abstract Instruction encodeIsIndent(IsIndent p, Instruction next, Instruction failjump);

	public abstract Instruction encodeExistsSymbol(ExistsSymbol existsSymbol, Instruction next, Instruction failjump);

	public abstract Instruction encodeLocalTable(LocalTable localTable, Instruction next, Instruction failjump);

	// Extension
	public abstract Instruction encodeExtension(Expression p, Instruction next, Instruction failjump);

	public Instruction encodeEmpty(Expression empty, Instruction next) {
		return next;
	}

	public Instruction encodeOnFlag(OnFlag p, Instruction next, Instruction failjump) {
		return p.get(0).encode(this, next, failjump);
	}

	public Instruction encodeIfFlag(IfFlag ifFlag, Instruction next, Instruction failjump) {
		return next;
	}

}
