package nez.vm;


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
import nez.lang.IsIndent;
import nez.lang.IsSymbol;
import nez.lang.Link;
import nez.lang.LocalTable;
import nez.lang.New;
import nez.lang.NonTerminal;
import nez.lang.Not;
import nez.lang.Option;
import nez.lang.Repetition;
import nez.lang.Repetition1;
import nez.lang.Replace;
import nez.lang.Tagging;
import nez.util.UFlag;
import nez.util.UList;

public abstract class NezCompiler {
	final int option;
		
	public NezCompiler(int option) {
		this.option = option;
	}
	
	protected final boolean enablePackratParsing() {
		return UFlag.is(this.option, Grammar.PackratParsing);
	}

	protected final boolean enableASTConstruction() {
		return UFlag.is(this.option, Grammar.ASTConstruction);
	}
	
	public abstract NezCode encode(Grammar grammar);

	
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
	
	public abstract Instruction encodeExpression(Expression e, Instruction next, Instruction failjump);
	public abstract Instruction encodeFail(Expression p);
	public abstract Instruction encodeMatchAny(AnyChar p, Instruction next, Instruction failjump);
	public abstract Instruction encodeByteChar(ByteChar p, Instruction next, Instruction failjump);
	public abstract Instruction encodeByteMap(ByteMap p, Instruction next, Instruction failjump);
	public abstract Instruction encodeOption(Option p, Instruction next);
	public abstract Instruction encodeRepetition(Repetition p, Instruction next);
	public abstract Instruction encodeRepetition1(Repetition1 p, Instruction next, Instruction failjump);
	public abstract Instruction encodeAnd(And p, Instruction next, Instruction failjump);
	public abstract Instruction encodeNot(Not p, Instruction next, Instruction failjump);
	public abstract Instruction encodeSequence(Expression p, Instruction next, Instruction failjump);
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
	

}
