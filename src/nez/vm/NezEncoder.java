package nez.vm;


import java.util.HashMap;
import java.util.TreeMap;

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
import nez.lang.GrammarFactory;
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
import nez.util.UFlag;
import nez.util.UList;

public abstract class NezEncoder {
	protected int option;
		
	public NezEncoder(int option) {
		this.option = option;
	}
	
	public final boolean is(int grammarOption) {
		return UFlag.is(this.option, grammarOption);
	}

	/* CodeMap */
	
	protected HashMap<String, CodePoint> codeMap = null;
		
	protected CodePoint newCodePoint(Production p, Expression localExpression) {
		return new CodePoint(p, localExpression);
	}
	
	protected CodePoint getCodePoint(Production p) {
		if(this.codeMap != null) {
			return codeMap.get(p.getUniqueName());
		}
		return null;
	}

	protected Expression optimizeLocalProduction(Production p) {
		return p.getExpression();
	}
	
	protected void analyizeProductionInlining(CodePoint pcode) {
		//Verbose.debug("ref " + pcode.production.getLocalName() + " " + pcode.ref);
		if(pcode.ref == 1 || GrammarOptimizer.isCharacterTerminal(pcode.localExpression)) {
			pcode.inlining = true;
		}
	}

	protected int analyizeProductionMemoizing(CodePoint pcode, int memoId) {
		if(pcode.inlining) {
			return memoId;
		}
		Production p = pcode.production;
		if(pcode.ref > 3 && p.inferTypestate() != Typestate.OperationType) {
			pcode.memoPoint = new MemoPoint(memoId++, p.getLocalName(), pcode.localExpression, false);
			Verbose.debug("memo " + p.getLocalName() + " " + pcode.memoPoint.id + " pure? " + p.isNoNTreeConstruction());
		}
		return memoId;
	}

	void count(Production p) {
		String uname = p.getUniqueName();
		CodePoint c = this.codeMap.get(uname);
		if(c == null) {
			Expression deref = optimizeLocalProduction(p);
			String key = "#" + deref.getId();
			c = this.codeMap.get(key);
			if(c == null) {
				c = newCodePoint(p, deref);
				codeMap.put(key, c);
			}
//			else {
//				Verbose.debug("alias " + uname + ", " + c.production.getUniqueName());
//			}
			codeMap.put(uname, c);
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

	protected void initCodeMap(Grammar grammar) {
		this.codeMap = new HashMap<String, CodePoint>();
		Production start = grammar.getStartProduction();
		count(start);
		countNonTerminalReference(start.getExpression());
		for(Production p : grammar.getProductionList()) {
			if(p != start) {
				this.countNonTerminalReference(p.getExpression());
			}
		}
		if(UFlag.is(option, Grammar.Inlining)) {
			for(Production p : grammar.getProductionList()) {
				CodePoint pcode = this.codeMap.get(p.getUniqueName());
				if(pcode != null) {
					this.analyizeProductionInlining(pcode);
				}
			}
		}
		if(UFlag.is(option, Grammar.PackratParsing)) {
			int memoId = 0;
			for(Production p : grammar.getProductionList()) {
				CodePoint pcode = this.codeMap.get(p.getUniqueName());
				if(pcode != null) {
					memoId = this.analyizeProductionMemoizing(pcode, memoId);
				}
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
