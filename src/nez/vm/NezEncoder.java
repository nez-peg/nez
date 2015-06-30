package nez.vm;


import java.util.HashMap;
import java.util.List;

import nez.NezOption;
import nez.lang.And;
import nez.lang.AnyChar;
import nez.lang.Block;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.Capture;
import nez.lang.CharMultiByte;
import nez.lang.Choice;
import nez.lang.DefIndent;
import nez.lang.DefSymbol;
import nez.lang.ExistsSymbol;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.GrammarOptimizer;
import nez.lang.IfFlag;
import nez.lang.IsIndent;
import nez.lang.IsSymbol;
import nez.lang.Link;
import nez.lang.LocalTable;
import nez.lang.New;
import nez.lang.NonTerminal;
import nez.lang.Not;
import nez.lang.OnFlag;
import nez.lang.Option;
import nez.lang.Production;
import nez.lang.Repetition;
import nez.lang.Repetition1;
import nez.lang.Replace;
import nez.lang.Sequence;
import nez.lang.Tagging;
import nez.lang.Typestate;
import nez.main.Verbose;

public abstract class NezEncoder {
	protected NezOption option;
		
	public NezEncoder(NezOption option) {
		this.option = option;
	}

	/* CodeMap */
	
	protected HashMap<String, CodePoint> codePointMap = null;
		
	protected CodePoint newCodePoint(Production p, Expression localExpression) {
		return new CodePoint(p, localExpression);
	}
	
	protected CodePoint getCodePoint(Production p) {
		if(this.codePointMap != null) {
			return codePointMap.get(p.getUniqueName());
		}
		return null;
	}

	protected Expression optimizeLocalProduction(Production p) {
		return p.getExpression();
	}
	
	void count(Production p) {
		String uname = p.getUniqueName();
		CodePoint c = this.codePointMap.get(uname);
		if(c == null) {
			Expression deref = optimizeLocalProduction(p);
			String key = "#" + deref.getId();
			c = this.codePointMap.get(key);
			if(c == null) {
				c = newCodePoint(p, deref);
				codePointMap.put(key, c);
			}
//			else {
//				Verbose.debug("alias " + uname + ", " + c.production.getUniqueName());
//			}
			codePointMap.put(uname, c);
		}
		c.ref++;
	}

	void countNonTerminalReference(Expression e) {
		if(e instanceof NonTerminal) {
			Production p = ((NonTerminal) e).getProduction();
			count(p);
		}
		for(Expression sub : e) {
			countNonTerminalReference(sub);
		}
	}

	protected void initCodeMap(Grammar grammar, List<MemoPoint> memoPointList) {
		this.codePointMap = new HashMap<String, CodePoint>();
		Production start = grammar.getStartProduction();
		count(start);
		countNonTerminalReference(start.getExpression());
		for(Production p : grammar.getProductionList()) {
			if(p != start) {
				this.countNonTerminalReference(p.getExpression());
			}
		}
		if(option.enabledInlining) {
			for(Production p : grammar.getProductionList()) {
				CodePoint cp = this.codePointMap.get(p.getUniqueName());
				this.checkInlining(cp);
//				System.out.println(p.getUniqueName() + " cp = " + cp);
//				if(cp != null) {
//					this.checkInlining(cp);
//				}
			}
		}
		if(memoPointList != null) {
			for(Production p : grammar.getProductionList()) {
				CodePoint cp = this.codePointMap.get(p.getUniqueName());
				//System.out.println(p.getUniqueName() + " cp = " + cp);
				this.checkMemoizing(cp, memoPointList);
//				if(cp != null) {
//					this.checkMemoizing(cp, memoPointList);
//				}
			}
		}
	}
	
	protected void checkInlining(CodePoint cp) {
		//Verbose.debug("ref " + cp.production.getLocalName() + " " + cp.ref);
		if(cp.ref == 1 || GrammarOptimizer.isSingleCharacter(cp.localExpression)) {
			cp.inlining = true;
		}
	}

	protected void checkMemoizing(CodePoint cp, List<MemoPoint> memoPointList) {
		if(cp.inlining || cp.memoPoint != null) {
			return ;
		}
		Production p = cp.production;
		if(cp.ref > 2 && p.inferTypestate() != Typestate.OperationType) {
			int memoId = memoPointList.size();
			cp.memoPoint = new MemoPoint(memoId, p.getLocalName(), cp.localExpression, p.isContextual());
			memoPointList.add(cp.memoPoint);
			if(Verbose.PackratParsing) {
				Verbose.debug("memo " + cp.memoPoint + " ref="+ cp.ref + " pure? " + p.isNoNTreeConstruction() + " rec? " + p.isRecursive());
			}
		}
	}

	// encoding

//	private Instruction failed = new IFail(null);
	
	public abstract Instruction encodeExpression(Expression e, Instruction next, Instruction failjump);
	
	public abstract Instruction encodeFail(Expression p);
	public abstract Instruction encodeAnyChar(AnyChar p, Instruction next, Instruction failjump);
	public abstract Instruction encodeByteChar(ByteChar p, Instruction next, Instruction failjump);
	public abstract Instruction encodeByteMap(ByteMap p, Instruction next, Instruction failjump);
	public abstract Instruction encodeCharMultiByte(CharMultiByte p, Instruction next, Instruction failjump);

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
