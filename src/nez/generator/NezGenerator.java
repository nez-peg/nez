package nez.generator;

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
import nez.util.FileBuilder;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public abstract class NezGenerator extends NezEncoder {
	public abstract String getDesc();

	public NezGenerator() {
		super(NezOption.newDefaultOption());
		this.file = null;
	}

	protected void setOption(NezOption option) {
		this.option = option;
	}

	protected FileBuilder file;

	protected void setOutputFile(String fileName) {
		if(fileName == null) {
			this.file = new FileBuilder();
		}
		else {
			this.file = new FileBuilder(fileName);
		}
	}

	@Override
	public Instruction encodeExpression(Expression e, Instruction next, Instruction failjump) {
		return e.encode(this, next, failjump);
	}

	@Override
	public Instruction encodeAnyChar(AnyChar p, Instruction next, Instruction failjump) {
		this.visitAnyChar(p);
		return null;
	}

	@Override
	public Instruction encodeByteChar(ByteChar p, Instruction next, Instruction failjump) {
		this.visitByteChar(p);
		return null;
	}

	@Override
	public Instruction encodeByteMap(ByteMap p, Instruction next, Instruction failjump) {
		this.visitByteMap(p);
		return null;
	}

	@Override
	public Instruction encodeCharMultiByte(CharMultiByte p, Instruction next, Instruction failjump) {
		this.visitCharMultiByte(p);
		return null;
	}

	@Override
	public Instruction encodeFail(Expression p) {
		this.visitFailure(p);
		return null;
	}

	@Override
	public Instruction encodeOption(Option p, Instruction next) {
		this.visitOption(p);
		return null;
	}

	@Override
	public Instruction encodeRepetition(Repetition p, Instruction next) {
		this.visitRepetition(p);
		return null;
	}

	@Override
	public Instruction encodeRepetition1(Repetition1 p, Instruction next, Instruction failjump) {
		this.visitRepetition1(p);
		return null;
	}

	@Override
	public Instruction encodeAnd(And p, Instruction next, Instruction failjump) {
		this.visitAnd(p);
		return null;
	}

	@Override
	public Instruction encodeNot(Not p, Instruction next, Instruction failjump) {
		this.visitNot(p);
		return null;
	}

	@Override
	public Instruction encodeSequence(Sequence p, Instruction next, Instruction failjump) {
		this.visitSequence(p);
		return null;
	}

	@Override
	public Instruction encodeChoice(Choice p, Instruction next, Instruction failjump) {
		this.visitChoice(p);
		return null;
	}

	@Override
	public Instruction encodeNonTerminal(NonTerminal p, Instruction next, Instruction failjump) {
		this.visitNonTerminal(p);
		return null;
	}

	// AST Construction

	@Override
	public Instruction encodeLink(Link p, Instruction next, Instruction failjump) {
		if(option.enabledASTConstruction) {
			this.visitLink(p);
		}
		else {
			this.visitExpression(p.get(0));
		}
		return null;
	}

	@Override
	public Instruction encodeNew(New p, Instruction next) {
		if(option.enabledASTConstruction) {
			this.visitNew(p);
		}
		return null;
	}

	@Override
	public Instruction encodeCapture(Capture p, Instruction next) {
		if(option.enabledASTConstruction) {
			this.visitCapture(p);
		}
		return null;
	}

	@Override
	public Instruction encodeTagging(Tagging p, Instruction next) {
		if(option.enabledASTConstruction) {
			this.visitTagging(p);
		}
		return null;
	}

	@Override
	public Instruction encodeReplace(Replace p, Instruction next) {
		if(option.enabledASTConstruction) {
			this.visitReplace(p);
		}
		return null;
	}

	@Override
	public Instruction encodeBlock(Block p, Instruction next, Instruction failjump) {
		this.visitBlock(p);
		return null;
	}

	@Override
	public Instruction encodeLocalTable(LocalTable p, Instruction next, Instruction failjump) {
		this.visitLocalTable(p);
		return null;
	}

	@Override
	public Instruction encodeDefSymbol(DefSymbol p, Instruction next, Instruction failjump) {
		this.visitDefSymbol(p);
		return null;
	}

	@Override
	public Instruction encodeExistsSymbol(ExistsSymbol p, Instruction next, Instruction failjump) {
		this.visitExistsSymbol(p);
		return null;
	}

	@Override
	public Instruction encodeIsSymbol(IsSymbol p, Instruction next, Instruction failjump) {
		this.visitIsSymbol(p);
		return null;
	}

	@Override
	public Instruction encodeDefIndent(DefIndent p, Instruction next, Instruction failjump) {
		this.visitDefIndent(p);
		return null;
	}

	@Override
	public Instruction encodeIsIndent(IsIndent p, Instruction next, Instruction failjump) {
		this.visitIsIndent(p);
		return null;
	}

	@Override
	public Instruction encodeEmpty(Expression p, Instruction next) {
		this.visitEmpty(p);
		return null;
	}

	@Override
	public Instruction encodeOnFlag(OnFlag p, Instruction next, Instruction failjump) {
		return p.get(0).encode(this, next, failjump);
	}

	@Override
	public Instruction encodeIfFlag(IfFlag ifFlag, Instruction next, Instruction failjump) {
		return next;
	}

	@Override
	public Instruction encodeExtension(Expression p, Instruction next, Instruction failjump) {
		this.visitUndefined(p);
		return null;
	}

	public void visitExpression(Expression e) {
		e.encode(this, null, null);
	}

	public void visitUndefined(Expression p) {

	}

	public abstract void visitEmpty(Expression p);

	public abstract void visitFailure(Expression p);

	public abstract void visitAnyChar(AnyChar p);

	public abstract void visitByteChar(ByteChar p);

	public abstract void visitByteMap(ByteMap p);

	public abstract void visitOption(Option p);

	public abstract void visitRepetition(Repetition p);

	public abstract void visitRepetition1(Repetition1 p);

	public abstract void visitAnd(And p);

	public abstract void visitNot(Not p);

	public abstract void visitSequence(Sequence p);

	public abstract void visitChoice(Choice p);

	public abstract void visitNonTerminal(NonTerminal p);

	public abstract void visitCharMultiByte(CharMultiByte p);

	// AST Construction
	public abstract void visitLink(Link p);

	public abstract void visitNew(New p);

	public abstract void visitCapture(Capture p);

	public abstract void visitTagging(Tagging p);

	public abstract void visitReplace(Replace p);

	// Symbol Tables
	public abstract void visitBlock(Block p);

	public abstract void visitDefSymbol(DefSymbol p);

	public abstract void visitIsSymbol(IsSymbol p);

	public abstract void visitDefIndent(DefIndent p);

	public abstract void visitIsIndent(IsIndent p);

	public abstract void visitExistsSymbol(ExistsSymbol p);

	public abstract void visitLocalTable(LocalTable p);

	// ---------------------------------------------------------------------

	public void generate(Grammar grammar, NezOption option, String fileName) {
		this.setOption(option);
		this.setOutputFile(fileName);
		makeHeader(grammar);
		for(Production p : grammar.getProductionList()) {
			visitProduction(p);
		}
		makeFooter(grammar);
		file.writeNewLine();
		file.flush();
	}

	public void makeHeader(Grammar g) {
	}

	public abstract void visitProduction(Production r);

	public void makeFooter(Grammar g) {
	}
}
