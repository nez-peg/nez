package nez.vm;

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
import nez.lang.IsIndent;
import nez.lang.IsSymbol;
import nez.lang.Link;
import nez.lang.LocalTable;
import nez.lang.New;
import nez.lang.NonTerminal;
import nez.lang.Not;
import nez.lang.Option;
import nez.lang.Production;
import nez.lang.Repetition;
import nez.lang.Repetition1;
import nez.lang.Replace;
import nez.lang.Sequence;
import nez.lang.Tagging;
import nez.main.Verbose;
import nez.util.UList;

public class PlainCompiler extends NezCompiler {

	protected final Instruction commonFailure = new IFail(null);

	public PlainCompiler(NezOption option) {
		super(option);
	}

	protected Expression optimizeLocalProduction(Production p) {
		return GrammarOptimizer.resolveNonTerminal(p.getExpression());
	}

	protected Instruction encodeMemoizingProduction(ProductionCode code) {
		return null;
	}
	
	private Production encodingProduction;
	protected Production getEncodingProduction() {
		return this.encodingProduction;
	}
	
	protected void encodeProduction(UList<Instruction> codeList, Production p, Instruction next) {
		String uname = p.getUniqueName();
		ProductionCode pcode = this.pcodeMap.get(uname);
		if(pcode != null) {
			encodingProduction = p;
			pcode.compiled = encode(pcode.localExpression, next, null/*failjump*/);
			//pcode.start = codeList.size();
			this.layoutCode(codeList, pcode.compiled);
			//pcode.end = codeList.size();
//			if(code.memoPoint != null) {
//				code.memoStart = this.encodeMemoizingProduction(code);
//				this.layoutCode(codeList, code.memoStart);
//			}
		}
	}

	@Override
	public NezCode compile(Grammar grammar) {
		return this.compile(grammar, null);
	}
	
	public NezCode compile(Grammar grammar, ByteCoder coder) {
		long t = System.nanoTime();
		List<MemoPoint> memoPointList = null;
		if(option.enabledMemoization || option.enabledPackratParsing) {
			memoPointList = new UList<MemoPoint>(new MemoPoint[4]);
		}
		initProductionCodeMap(grammar, memoPointList);
		UList<Instruction> codeList = new UList<Instruction>(new Instruction[64]);
//		Production start = grammar.getStartProduction();
//		this.encodeProduction(codeList, start, new IRet(start));
//		for(Production p : grammar.getProductionList()) {
//			if(p != start) {
//				this.encodeProduction(codeList, p, new IRet(p));
//			}
//		}
		for(Production p : grammar.getProductionList()) {
			this.encodeProduction(codeList, p, new IRet(p));
		}
		for(Instruction inst : codeList) {
			if(inst instanceof ICall) {
				ProductionCode deref = this.pcodeMap.get(((ICall) inst).rule.getUniqueName());
				if(deref == null) {
					Verbose.debug("no deref: " + ((ICall) inst).rule.getUniqueName());
				}
				((ICall) inst).setResolvedJump(deref.compiled);
			}
//			Verbose.debug("\t" + inst.id + "\t" + inst);
		}
		long t2 = System.nanoTime();
		Verbose.printElapsedTime("CompilingTime", t, t2);
		if(coder != null) {
			coder.setHeader(codeList.size(), pcodeMap.size(), memoPointList == null ? 0 : memoPointList.size());
		}
		this.pcodeMap = null;
		return new NezCode(codeList.ArrayValues[0], codeList.size(), memoPointList);
	}
	
	protected void optimizedUnary(Expression p) {
		Verbose.noticeOptimize("specialization", p);
	}

	protected void optimizedInline(Production p) {
		Verbose.noticeOptimize("inlining", p.getExpression());
	}
	
	// encoding

	public Instruction encode(Expression e, Instruction next, Instruction failjump) {
		return e.encode(this, next, failjump);
	}

	public Instruction encodeAnyChar(AnyChar p, Instruction next, Instruction failjump) {
		return new IAny(p, next);
	}

	public Instruction encodeByteChar(ByteChar p, Instruction next, Instruction failjump) {
		return new IByte(p, next);
	}

	public Instruction encodeByteMap(ByteMap p, Instruction next, Instruction failjump) {
		return new ISet(p, next);
	}

	@Override
	public Instruction encodeCharMultiByte(CharMultiByte p, Instruction next, Instruction failjump) {
		return new IStr(p, next);
	}
	
	public Instruction encodeFail(Expression p) {
		return this.commonFailure;
	}

	public Instruction encodeOption(Option p, Instruction next) {
		Instruction pop = new ISucc(p, next);
		return new IAlt(p, next, encode(p.get(0), pop, next));
	}

	public Instruction encodeRepetition(Repetition p, Instruction next) {
		//Expression skip = p.possibleInfiniteLoop ? new ISkip(p) : new ISkip(p);
		Instruction skip = new ISkip(p);
		Instruction start = encode(p.get(0), skip, next/*FIXME*/);
		skip.next = start;
		return new IAlt(p, next, start);
	}

	public Instruction encodeRepetition1(Repetition1 p, Instruction next, Instruction failjump) {
		return encode(p.get(0), this.encodeRepetition(p, next), failjump);
	}

	public Instruction encodeAnd(And p, Instruction next, Instruction failjump) {
		Instruction inner = encode(p.get(0), new IBack(p, next), failjump);
		return new IPos(p, inner);
	}

	public Instruction encodeNot(Not p, Instruction next, Instruction failjump) {
		Instruction fail = new ISucc(p, new IFail(p));
		return new IAlt(p, next, encode(p.get(0), fail, failjump));
	}

	public Instruction encodeSequence(Sequence p, Instruction next, Instruction failjump) {
//		return encode(p.get(0), encode(p.get(1), next, failjump), failjump);
		Instruction nextStart = next;
		for(int i = p.size() - 1; i >= 0; i--) {
			Expression e = p.get(i);
			nextStart = encode(e, nextStart, failjump);
		}
		return nextStart;
	}

	public Instruction encodeChoice(Choice p, Instruction next, Instruction failjump) {
		Instruction nextChoice = encode(p.get(p.size() - 1), next, failjump);
		for(int i = p.size() - 2; i >= 0; i--) {
			Expression e = p.get(i);
			nextChoice = new IAlt(e, nextChoice, encode(e, new ISucc(e, next), nextChoice));
		}
		return nextChoice;
	}

	public Instruction encodeNonTerminal(NonTerminal p, Instruction next, Instruction failjump) {
		Production r = p.getProduction();
		return new ICall(r, next);
	}

	// AST Construction

	public Instruction encodeLink(Link p, Instruction next, Instruction failjump) {
		if(this.option.enabledASTConstruction) {
			next = new ITPop(p, next);
			next = encode(p.get(0), next, failjump);
			return new ITPush(p, next);
		}
		return encode(p.get(0), next, failjump);
	}

	public Instruction encodeNew(New p, Instruction next) {
		if(this.option.enabledASTConstruction) {
			return p.lefted ? new ITLeftFold(p, next) : new INew(p, next);
		}
		return next;
	}

	public Instruction encodeCapture(Capture p, Instruction next) {
		if(this.option.enabledASTConstruction) {
			return new ICapture(p, next);
		}
		return next;
	}

	public Instruction encodeTagging(Tagging p, Instruction next) {
		if(this.option.enabledASTConstruction) {
			return new ITag(p, next);
		}
		return next;
	}

	public Instruction encodeReplace(Replace p, Instruction next) {
		if(this.option.enabledASTConstruction) {
			return new IReplace(p, next);
		}
		return next;
	}

	public Instruction encodeBlock(Block p, Instruction next, Instruction failjump) {
		next = new IEndSymbolScope(p, next);
		next = encode(p.get(0), next, failjump);
		return new IBeginSymbolScope(p, next);
	}

	public Instruction encodeLocalTable(LocalTable p, Instruction next, Instruction failjump) {
		next = new IEndSymbolScope(p, next);
		next = encode(p.get(0), next, failjump);
		return new IBeginLocalScope(p, next);
	}

	public Instruction encodeDefSymbol(DefSymbol p, Instruction next, Instruction failjump) {
		return new IPos(p, encode(p.get(0), new IDefSymbol(p, next), failjump));
	}

	public Instruction encodeExistsSymbol(ExistsSymbol p, Instruction next, Instruction failjump) {
		return new IExistsSymbol(p, next);
	}

	public Instruction encodeIsSymbol(IsSymbol p, Instruction next, Instruction failjump) {
		if(p.checkLastSymbolOnly) {
			return new IsMatch(p, next);
		}
		else {
			return new IPos(p, encode(p.getSymbolExpression(), new IIsaSymbol(p, next), failjump));
		}
	}

	public Instruction encodeDefIndent(DefIndent p, Instruction next, Instruction failjump) {
		return new IDefIndent(p, next);
	}

	public Instruction encodeIsIndent(IsIndent p, Instruction next, Instruction failjump) {
		return new IIsIndent(p, next);
	}

	@Override
	public Instruction encodeExtension(Expression p, Instruction next, Instruction failjump) {
		return next;
	}

}
