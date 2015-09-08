package nez.generator;

import nez.NezOption;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Production;
import nez.lang.expr.Uand;
import nez.lang.expr.Cany;
import nez.lang.expr.Xblock;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cset;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Choice;
import nez.lang.expr.Xdefindent;
import nez.lang.expr.Xdef;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xif;
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Tlink;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Tnew;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Unot;
import nez.lang.expr.Xon;
import nez.lang.expr.Uoption;
import nez.lang.expr.Uzero;
import nez.lang.expr.Uone;
import nez.lang.expr.Treplace;
import nez.lang.expr.Sequence;
import nez.lang.expr.Ttag;
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
		if (fileName == null) {
			this.file = new FileBuilder();
		} else {
			this.file = new FileBuilder(fileName);
		}
	}

	public Instruction encode(Expression e, Instruction next, Instruction failjump) {
		return e.encode(this, next, failjump);
	}

	public Instruction encodeCany(Cany p, Instruction next, Instruction failjump) {
		this.visitAnyChar(p);
		return null;
	}

	public Instruction encodeCbyte(Cbyte p, Instruction next, Instruction failjump) {
		this.visitByteChar(p);
		return null;
	}

	public Instruction encodeCset(Cset p, Instruction next, Instruction failjump) {
		this.visitByteMap(p);
		return null;
	}

	public Instruction encodeCmulti(Cmulti p, Instruction next, Instruction failjump) {
		this.visitCharMultiByte(p);
		return null;
	}

	public Instruction encodeFail(Expression p) {
		this.visitFailure(p);
		return null;
	}

	public Instruction encodeUoption(Uoption p, Instruction next) {
		this.visitOption(p);
		return null;
	}

	public Instruction encodeUzero(Uzero p, Instruction next) {
		this.visitRepetition(p);
		return null;
	}

	public Instruction encodeUone(Uone p, Instruction next, Instruction failjump) {
		this.visitRepetition1(p);
		return null;
	}

	public Instruction encodeUand(Uand p, Instruction next, Instruction failjump) {
		this.visitAnd(p);
		return null;
	}

	public Instruction encodeUnot(Unot p, Instruction next, Instruction failjump) {
		this.visitNot(p);
		return null;
	}

	public Instruction encodeSequence(Sequence p, Instruction next, Instruction failjump) {
		this.visitSequence(p);
		return null;
	}

	public Instruction encodeChoice(Choice p, Instruction next, Instruction failjump) {
		this.visitChoice(p);
		return null;
	}

	public Instruction encodeNonTerminal(NonTerminal p, Instruction next, Instruction failjump) {
		this.visitNonTerminal(p);
		return null;
	}

	// AST Construction

	public Instruction encodeTlink(Tlink p, Instruction next, Instruction failjump) {
		if (option.enabledASTConstruction) {
			this.visitLink(p);
		} else {
			this.visitExpression(p.get(0));
		}
		return null;
	}

	public Instruction encodeTnew(Tnew p, Instruction next) {
		if (option.enabledASTConstruction) {
			this.visitNew(p);
		}
		return null;
	}

	public Instruction encodeTcapture(Tcapture p, Instruction next) {
		if (option.enabledASTConstruction) {
			this.visitCapture(p);
		}
		return null;
	}

	public Instruction encodeTtag(Ttag p, Instruction next) {
		if (option.enabledASTConstruction) {
			this.visitTagging(p);
		}
		return null;
	}

	public Instruction encodeTreplace(Treplace p, Instruction next) {
		if (option.enabledASTConstruction) {
			this.visitReplace(p);
		}
		return null;
	}

	public Instruction encodeXblock(Xblock p, Instruction next, Instruction failjump) {
		this.visitBlock(p);
		return null;
	}

	public Instruction encodeXlocal(Xlocal p, Instruction next, Instruction failjump) {
		this.visitLocalTable(p);
		return null;
	}

	public Instruction encodeXdef(Xdef p, Instruction next, Instruction failjump) {
		this.visitDefSymbol(p);
		return null;
	}

	public Instruction encodeXexists(Xexists p, Instruction next, Instruction failjump) {
		this.visitExistsSymbol(p);
		return null;
	}

	public Instruction encodeXmatch(Xmatch p, Instruction next, Instruction failjump) {
		this.visitMatchSymbol(p);
		return null;
	}

	public Instruction encodeXis(Xis p, Instruction next, Instruction failjump) {
		this.visitIsSymbol(p);
		return null;
	}

	public Instruction encodeXdefindent(Xdefindent p, Instruction next, Instruction failjump) {
		this.visitDefIndent(p);
		return null;
	}

	public Instruction encodeXindent(Xindent p, Instruction next, Instruction failjump) {
		this.visitIsIndent(p);
		return null;
	}

	public Instruction encodeEmpty(Expression p, Instruction next) {
		this.visitEmpty(p);
		return null;
	}

	public Instruction encodeXon(Xon p, Instruction next, Instruction failjump) {
		return p.get(0).encode(this, next, failjump);
	}

	public Instruction encodeXif(Xif ifFlag, Instruction next, Instruction failjump) {
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

	public abstract void visitAnyChar(Cany p);

	public abstract void visitByteChar(Cbyte p);

	public abstract void visitByteMap(Cset p);

	public abstract void visitOption(Uoption p);

	public abstract void visitRepetition(Uzero p);

	public abstract void visitRepetition1(Uone p);

	public abstract void visitAnd(Uand p);

	public abstract void visitNot(Unot p);

	public abstract void visitSequence(Sequence p);

	public abstract void visitChoice(Choice p);

	public abstract void visitNonTerminal(NonTerminal p);

	public abstract void visitCharMultiByte(Cmulti p);

	// AST Construction
	public abstract void visitLink(Tlink p);

	public abstract void visitNew(Tnew p);

	public abstract void visitCapture(Tcapture p);

	public abstract void visitTagging(Ttag p);

	public abstract void visitReplace(Treplace p);

	// Symbol Tables
	public abstract void visitBlock(Xblock p);

	public abstract void visitDefSymbol(Xdef p);

	public abstract void visitMatchSymbol(Xmatch p);

	public abstract void visitIsSymbol(Xis p);

	public abstract void visitDefIndent(Xdefindent p);

	public abstract void visitIsIndent(Xindent p);

	public abstract void visitExistsSymbol(Xexists p);

	public abstract void visitLocalTable(Xlocal p);

	// ---------------------------------------------------------------------

	public void generate(Grammar grammar, NezOption option, String fileName) {
		this.setOption(option);
		this.setOutputFile(fileName);
		makeHeader(grammar);
		for (Production p : grammar.getProductionList()) {
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
