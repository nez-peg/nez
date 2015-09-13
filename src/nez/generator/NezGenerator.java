package nez.generator;

import nez.NezOption;
import nez.Parser;
import nez.lang.Expression;
import nez.lang.Production;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Cset;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pand;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Pnot;
import nez.lang.expr.Pone;
import nez.lang.expr.Poption;
import nez.lang.expr.Psequence;
import nez.lang.expr.Pzero;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Tlink;
import nez.lang.expr.Tnew;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.lang.expr.Xblock;
import nez.lang.expr.Xdef;
import nez.lang.expr.Xdefindent;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xif;
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Xon;
import nez.parser.Instruction;
import nez.parser.AbstractGenerator;
import nez.util.FileBuilder;

public abstract class NezGenerator extends AbstractGenerator {
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

	@Override
	public Instruction encode(Expression e, Instruction next, Instruction failjump) {
		return e.encode(this, next, failjump);
	}

	@Override
	public Instruction encodeCany(Cany p, Instruction next, Instruction failjump) {
		this.visitCany(p);
		return null;
	}

	@Override
	public Instruction encodeCbyte(Cbyte p, Instruction next, Instruction failjump) {
		this.visitCbyte(p);
		return null;
	}

	@Override
	public Instruction encodeCset(Cset p, Instruction next, Instruction failjump) {
		this.visitCset(p);
		return null;
	}

	@Override
	public Instruction encodeCmulti(Cmulti p, Instruction next, Instruction failjump) {
		this.visitCmulti(p);
		return null;
	}

	@Override
	public Instruction encodePfail(Expression p) {
		this.visitPfail(p);
		return null;
	}

	@Override
	public Instruction encodePoption(Poption p, Instruction next) {
		this.visitPoption(p);
		return null;
	}

	@Override
	public Instruction encodePzero(Pzero p, Instruction next) {
		this.visitPzero(p);
		return null;
	}

	@Override
	public Instruction encodePone(Pone p, Instruction next, Instruction failjump) {
		this.visitPone(p);
		return null;
	}

	@Override
	public Instruction encodePand(Pand p, Instruction next, Instruction failjump) {
		this.visitPand(p);
		return null;
	}

	@Override
	public Instruction encodePnot(Pnot p, Instruction next, Instruction failjump) {
		this.visitPnot(p);
		return null;
	}

	@Override
	public Instruction encodePsequence(Psequence p, Instruction next, Instruction failjump) {
		this.visitPsequence(p);
		return null;
	}

	@Override
	public Instruction encodePchoice(Pchoice p, Instruction next, Instruction failjump) {
		this.visitPchoice(p);
		return null;
	}

	@Override
	public Instruction encodeNonTerminal(NonTerminal p, Instruction next, Instruction failjump) {
		this.visitNonTerminal(p);
		return null;
	}

	// AST Construction

	@Override
	public Instruction encodeTlink(Tlink p, Instruction next, Instruction failjump) {
		if (option.enabledASTConstruction) {
			this.visitTlink(p);
		} else {
			this.visitExpression(p.get(0));
		}
		return null;
	}

	@Override
	public Instruction encodeTnew(Tnew p, Instruction next) {
		if (option.enabledASTConstruction) {
			this.visitTnew(p);
		}
		return null;
	}

	@Override
	public Instruction encodeTcapture(Tcapture p, Instruction next) {
		if (option.enabledASTConstruction) {
			this.visitTcapture(p);
		}
		return null;
	}

	@Override
	public Instruction encodeTtag(Ttag p, Instruction next) {
		if (option.enabledASTConstruction) {
			this.visitTtag(p);
		}
		return null;
	}

	@Override
	public Instruction encodeTreplace(Treplace p, Instruction next) {
		if (option.enabledASTConstruction) {
			this.visitTreplace(p);
		}
		return null;
	}

	@Override
	public Instruction encodeXblock(Xblock p, Instruction next, Instruction failjump) {
		this.visitXblock(p);
		return null;
	}

	@Override
	public Instruction encodeXlocal(Xlocal p, Instruction next, Instruction failjump) {
		this.visitXlocal(p);
		return null;
	}

	@Override
	public Instruction encodeXdef(Xdef p, Instruction next, Instruction failjump) {
		this.visitXdef(p);
		return null;
	}

	@Override
	public Instruction encodeXexists(Xexists p, Instruction next, Instruction failjump) {
		this.visitXexists(p);
		return null;
	}

	@Override
	public Instruction encodeXmatch(Xmatch p, Instruction next, Instruction failjump) {
		this.visitXmatch(p);
		return null;
	}

	@Override
	public Instruction encodeXis(Xis p, Instruction next, Instruction failjump) {
		this.visitXis(p);
		return null;
	}

	@Override
	public Instruction encodeXdefindent(Xdefindent p, Instruction next, Instruction failjump) {
		this.visitXdefindent(p);
		return null;
	}

	@Override
	public Instruction encodeXindent(Xindent p, Instruction next, Instruction failjump) {
		this.visitXindent(p);
		return null;
	}

	@Override
	public Instruction encodeTempty(Expression p, Instruction next) {
		this.visitPempty(p);
		return null;
	}

	@Override
	public Instruction encodeXon(Xon p, Instruction next, Instruction failjump) {
		return p.get(0).encode(this, next, failjump);
	}

	@Override
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

	public abstract void visitPempty(Expression p);

	public abstract void visitPfail(Expression p);

	public abstract void visitCany(Cany p);

	public abstract void visitCbyte(Cbyte p);

	public abstract void visitCset(Cset p);

	public abstract void visitPoption(Poption p);

	public abstract void visitPzero(Pzero p);

	public abstract void visitPone(Pone p);

	public abstract void visitPand(Pand p);

	public abstract void visitPnot(Pnot p);

	public abstract void visitPsequence(Psequence p);

	public abstract void visitPchoice(Pchoice p);

	public abstract void visitNonTerminal(NonTerminal p);

	public abstract void visitCmulti(Cmulti p);

	// AST Construction
	public abstract void visitTlink(Tlink p);

	public abstract void visitTnew(Tnew p);

	public abstract void visitTcapture(Tcapture p);

	public abstract void visitTtag(Ttag p);

	public abstract void visitTreplace(Treplace p);

	// Symbol Tables
	public abstract void visitXblock(Xblock p);

	public abstract void visitXlocal(Xlocal p);

	public abstract void visitXdef(Xdef p);

	public abstract void visitXexists(Xexists p);

	public abstract void visitXmatch(Xmatch p);

	public abstract void visitXis(Xis p);

	public abstract void visitXdefindent(Xdefindent p);

	public abstract void visitXindent(Xindent p);

	// ---------------------------------------------------------------------

	public void generate(Parser grammar, NezOption option, String fileName) {
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

	public void makeHeader(Parser g) {
	}

	public abstract void visitProduction(Production p);

	public void makeFooter(Parser g) {
	}
}
