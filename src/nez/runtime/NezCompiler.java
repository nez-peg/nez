package nez.runtime;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

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
import nez.lang.Factory;
import nez.lang.Grammar;
import nez.lang.IsIndent;
import nez.lang.IsSymbol;
import nez.lang.Link;
import nez.lang.LocalTable;
import nez.lang.Manipulator;
import nez.lang.New;
import nez.lang.NonTerminal;
import nez.lang.Not;
import nez.lang.Option;
import nez.lang.Prediction;
import nez.lang.Production;
import nez.lang.Repetition;
import nez.lang.Repetition1;
import nez.lang.Replace;
import nez.lang.Sequence;
import nez.lang.Tagging;
import nez.main.Verbose;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UFlag;
import nez.util.UList;
import nez.util.UMap;

public abstract class NezCompiler {
	final int option;
	
//	public UList<Instruction> codeList;
//	UMap<CodeBlock> ruleMap;
//	HashMap<Integer, MemoPoint> memoMap;
	
	public NezCompiler(int option) {
		this.option = option;
//		this.codeList = new UList<Instruction>(new Instruction[64]);
//		this.ruleMap = new UMap<CodeBlock>();
//		if(this.enablePackratParsing()) {
//			this.memoMap = new HashMap<Integer, MemoPoint>();
//			this.visitedMap = new UMap<String>();
//		}
	}
	
	protected final boolean enablePackratParsing() {
		return UFlag.is(this.option, Grammar.PackratParsing);
	}

	protected final boolean enableASTConstruction() {
		return UFlag.is(this.option, Grammar.ASTConstruction);
	}

//	public final Instruction encode(UList<Production> ruleList) {
//		long t = System.nanoTime();
//		for(Production r : ruleList) {
//			String uname = r.getUniqueName();
//			if(Verbose.Debug) {
//				Verbose.debug("compiling .. " + r);
//			}
//			Expression e = r.getExpression();
//			if(UFlag.is(option, Grammar.Inlining)  && this.ruleMap.size() > 0 && r.isInline() ) {
//				//System.out.println("skip .. " + r.getLocalName() + "=" + e);
//				continue;
//			}
//			if(!UFlag.is(option, Grammar.ASTConstruction)) {
//				e = e.reshape(Manipulator.RemoveAST);
//			}
//			CodeBlock block = new CodeBlock();
//			block.head = encodeExpression(e, new IRet(r));
//			block.start = codeList.size();
//			this.ruleMap.put(uname, block);
//			verify(block.head);
//			block.end = codeList.size();
//		}
//		for(Instruction inst : codeList) {
//			if(inst instanceof ICallPush) {
//				CodeBlock deref = this.ruleMap.get(((ICallPush) inst).rule.getUniqueName());
//				((ICallPush) inst).setResolvedJump(deref.head);
//			}
//		}
//		long t2 = System.nanoTime();
//		//Verbose.printElapsedTime("CompilingTime", t, t2);
//		return this.codeList.ArrayValues[0];
//	}
	
	
	
	public final void layoutCode(UList<Instruction> codeList, Instruction inst) {
		if(inst == null) {
			return;
		}
		if(inst.id == -1) {
			inst.id = codeList.size();
			codeList.add(inst);
			layoutCode(codeList, inst.next);
			if(inst.next != null && inst.id + 1 != inst.next.id) {
				Instruction.labeling(inst.next);
			}
			layoutCode(codeList, inst.branch());
			if(inst instanceof IDfaDispatch) {
				IDfaDispatch match = (IDfaDispatch)inst;
				for(int ch = 0; ch < match.jumpTable.length; ch ++) {
					layoutCode(codeList, match.jumpTable[ch]);
				}
			}
			//encode(inst.branch2());
		}
	}
		
	// encoding

//	private Instruction failed = new IFail(null);
	
	public abstract Instruction encodeExpression(Expression e, Instruction next);
	
	public abstract Instruction encodeFail(Expression p);
	
	public abstract Instruction encodeMatchAny(AnyChar p, Instruction next);

	public abstract Instruction encodeByteChar(ByteChar p, Instruction next);
	

	public abstract Instruction encodeByteMap(ByteMap p, Instruction next);


	
	public abstract Instruction encodeOption(Option p, Instruction next);
	
	public abstract Instruction encodeRepetition(Repetition p, Instruction next);

	public abstract Instruction encodeRepetition1(Repetition1 p, Instruction next);

	public abstract Instruction encodeAnd(And p, Instruction next);

	public abstract Instruction encodeNot(Not p, Instruction next);

	public abstract Instruction encodeSequence(Expression p, Instruction next);

	public abstract Instruction encodeChoice(Choice p, Instruction next);
	
	
	public abstract Instruction encodeNonTerminal(NonTerminal p, Instruction next);
	
	// AST Construction
	
	public abstract Instruction encodeLink(Link p, Instruction next);

	public abstract Instruction encodeNew(New p, Instruction next);

	public abstract Instruction encodeCapture(Capture p, Instruction next);
	
	public abstract Instruction encodeTagging(Tagging p, Instruction next);

	public abstract Instruction encodeReplace(Replace p, Instruction next);
	
	public abstract Instruction encodeBlock(Block p, Instruction next);
	
	public abstract Instruction encodeDefSymbol(DefSymbol p, Instruction next);
	
	public abstract Instruction encodeIsSymbol(IsSymbol p, Instruction next);	
	public abstract Instruction encodeDefIndent(DefIndent p, Instruction next);
	public abstract Instruction encodeIsIndent(IsIndent p, Instruction next);
	public abstract Instruction encodeExistsSymbol(ExistsSymbol existsSymbol, Instruction next);
	public abstract Instruction encodeLocalTable(LocalTable localTable, Instruction next);
	

}
