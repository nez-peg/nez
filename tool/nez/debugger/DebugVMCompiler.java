package nez.debugger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import nez.Strategy;
import nez.ast.CommonTree;
import nez.lang.Expression;
import nez.lang.Production;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Cset;
import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pand;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Pnot;
import nez.lang.expr.Pone;
import nez.lang.expr.Poption;
import nez.lang.expr.Psequence;
import nez.lang.expr.Pzero;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Tdetree;
import nez.lang.expr.Tlfold;
import nez.lang.expr.Tlink;
import nez.lang.expr.Tnew;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.lang.expr.Xblock;
import nez.lang.expr.Xdefindent;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Xsymbol;
import nez.parser.AbstractGenerator;
import nez.parser.GenerativeGrammar;
import nez.parser.Instruction;

public class DebugVMCompiler extends AbstractGenerator {
	GenerativeGrammar peg;
	IRBuilder builder;
	GrammarAnalyzer analyzer;
	HashMap<Expression, DebugVMInstruction> altInstructionMap = new HashMap<Expression, DebugVMInstruction>();

	public DebugVMCompiler(Strategy option) {
		super(option);
		this.builder = new IRBuilder(new Module());
	}

	public Module compile(GenerativeGrammar grammar) {
		this.builder.setGrammar(grammar);
		this.analyzer = new GrammarAnalyzer(grammar);
		this.analyzer.analyze();
		for (Production p : grammar.getProductionList()) {
			this.encodeProduction(p);
		}
		// ConsoleUtils.println(this.builder.getModule().stringfy(new
		// StringBuilder()));
		return this.builder.buildInstructionSequence();
	}

	public Module getModule() {
		return this.builder.getModule();
	}

	public Instruction encodeProduction(Production p) {
		this.builder.setFunction(new Function(p));
		this.builder.setInsertPoint(new BasicBlock());
		BasicBlock fbb = new BasicBlock();
		this.builder.pushFailureJumpPoint(fbb);
		p.encode(this, null, null);
		this.builder.setInsertPoint(this.builder.popFailureJumpPoint());
		this.builder.createIret(p);
		return null;
	}

	public Instruction encodeExpression(Expression e, Instruction next, Instruction failjump) {
		return e.encode(this, next, failjump);
	}

	ArrayList<Byte> charList = new ArrayList<Byte>();

	public boolean optimizeString(Psequence seq) {
		for (int i = 0; i < seq.size(); i++) {
			Expression e = seq.get(i);
			if (e instanceof Cbyte) {
				charList.add((byte) ((Cbyte) e).byteChar);
			} else if (e instanceof Psequence) {
				if (!this.optimizeString((Psequence) e)) {
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}

	boolean checkUnreachableChoice = true;

	public boolean optimizeCharSet(Pchoice p) {
		boolean[] map = Cset.newMap(false);
		for (int i = 0; i < p.size(); i++) {
			Expression e = p.get(i);
			if (e instanceof Cbyte) {
				map[((Cbyte) e).byteChar] = true;
			} else if (e instanceof Cset) {
				Cset bmap = (Cset) e;
				for (int j = 0; j < bmap.byteMap.length; j++) {
					if (bmap.byteMap[j]) {
						map[j] = true;
					}
				}
			} else {
				return false;
			}
		}
		this.builder.createIcharclass(p, this.builder.jumpFailureJump(), map);
		return true;
	}

	@Override
	public Instruction encodeNonTerminal(NonTerminal p, Instruction next, Instruction failjump) {
		BasicBlock rbb = new BasicBlock();
		this.builder.createIcall(p, rbb, this.builder.jumpFailureJump());
		this.builder.setInsertPoint(rbb);
		return null;
	}

	@Override
	public Instruction encodeExtension(Expression p, Instruction next, Instruction failjump) {
		throw new RuntimeException("undifined encode method " + p.getClass());
	}

	@Override
	public Instruction encode(Expression e, Instruction next, Instruction failjump) {
		throw new RuntimeException("undifined encode method " + e.getClass());
	}

	@Override
	public Instruction encodePfail(Expression p) {
		this.builder.createIfail(p);
		return null;
	}

	@Override
	public Instruction encodeCany(Cany p, Instruction next, Instruction failjump) {
		this.builder.createIany(p, this.builder.jumpFailureJump());
		this.builder.setInsertPoint(new BasicBlock());
		return null;
	}

	@Override
	public Instruction encodeCbyte(Cbyte p, Instruction next, Instruction failjump) {
		this.builder.createIchar(p, this.builder.jumpFailureJump());
		this.builder.setInsertPoint(new BasicBlock());
		return null;
	}

	@Override
	public Instruction encodeCset(Cset p, Instruction next, Instruction failjump) {
		this.builder.createIcharclass(p, this.builder.jumpFailureJump());
		this.builder.setInsertPoint(new BasicBlock());
		return null;
	}

	@Override
	public Instruction encodeCmulti(Cmulti p, Instruction next, Instruction failjump) {
		this.builder.createIstr(p, this.builder.jumpFailureJump(), p.byteSeq);
		this.builder.setInsertPoint(new BasicBlock());
		return null;
	}

	@Override
	public Instruction encodePoption(Poption p, Instruction next) {
		BasicBlock fbb = new BasicBlock();
		BasicBlock mergebb = new BasicBlock();
		this.builder.pushFailureJumpPoint(fbb);
		this.builder.createIpush(p);
		p.get(0).encode(this, next, null);
		this.builder.createIpop(p);
		this.builder.createIjump(p, mergebb);
		this.builder.setInsertPoint(this.builder.popFailureJumpPoint());
		this.builder.createIsucc(p);
		this.builder.createIpeek(p);
		this.builder.createIpop(p);
		this.builder.setInsertPoint(mergebb);
		return null;
	}

	@Override
	public Instruction encodePzero(Pzero p, Instruction next) {
		BasicBlock topBB = new BasicBlock();
		this.builder.setInsertPoint(topBB);
		BasicBlock fbb = new BasicBlock();
		this.builder.pushFailureJumpPoint(fbb);
		this.builder.createIpush(p);
		p.get(0).encode(this, next, null);
		this.builder.createIpop(p);
		this.builder.createIjump(p, topBB);
		this.builder.setInsertPoint(this.builder.popFailureJumpPoint());
		this.builder.createIsucc(p);
		this.builder.createIpeek(p);
		this.builder.createIpop(p);
		return null;
	}

	@Override
	public Instruction encodePone(Pone p, Instruction next, Instruction failjump) {
		p.get(0).encode(this, next, failjump);
		BasicBlock topBB = new BasicBlock();
		this.builder.setInsertPoint(topBB);
		BasicBlock fbb = new BasicBlock();
		this.builder.pushFailureJumpPoint(fbb);
		this.builder.createIpush(p);
		p.get(0).encode(this, next, failjump);
		this.builder.createIpop(p);
		this.builder.createIjump(p, topBB);
		this.builder.setInsertPoint(this.builder.popFailureJumpPoint());
		this.builder.createIsucc(p);
		this.builder.createIpeek(p);
		this.builder.createIpop(p);
		return null;
	}

	@Override
	public Instruction encodePand(Pand p, Instruction next, Instruction failjump) {
		BasicBlock fbb = new BasicBlock();
		this.builder.pushFailureJumpPoint(fbb);
		this.builder.createIpush(p);
		p.get(0).encode(this, next, failjump);
		this.builder.setInsertPoint(this.builder.popFailureJumpPoint());
		this.builder.createIpeek(p);
		this.builder.createIpop(p);
		this.builder.createIiffail(p, this.builder.jumpFailureJump());
		this.builder.setInsertPoint(new BasicBlock());
		return null;
	}

	@Override
	public Instruction encodePnot(Pnot p, Instruction next, Instruction failjump) {
		BasicBlock fbb = new BasicBlock();
		this.builder.pushFailureJumpPoint(fbb);
		this.builder.createIpush(p);
		p.get(0).encode(this, next, failjump);
		this.builder.createIfail(p);
		this.builder.createIpeek(p);
		this.builder.createIpop(p);
		this.builder.createIjump(p, this.builder.jumpPrevFailureJump());
		this.builder.setInsertPoint(this.builder.popFailureJumpPoint());
		this.builder.createIsucc(p);
		this.builder.createIpeek(p);
		this.builder.createIpop(p);
		return null;
	}

	@Override
	public Instruction encodePsequence(Psequence p, Instruction next, Instruction failjump) {
		this.charList.clear();
		boolean opt = this.optimizeString(p);
		if (opt) {
			byte[] utf8 = new byte[this.charList.size()];
			for (int i = 0; i < utf8.length; i++) {
				utf8[i] = this.charList.get(i);
			}
			this.builder.createIstr(p, this.builder.jumpFailureJump(), utf8);
			return null;
		}
		for (int i = 0; i < p.size(); i++) {
			p.get(i).encode(this, next, failjump);
		}
		return null;
	}

	@Override
	public Instruction encodePchoice(Pchoice p, Instruction next, Instruction failjump) {
		if (!optimizeCharSet(p)) {
			BasicBlock fbb = null;
			BasicBlock mergebb = new BasicBlock();
			this.builder.createIaltstart(p);
			this.builder.createIpush(p.get(0));
			for (int i = 0; i < p.size(); i++) {
				fbb = new BasicBlock();
				this.builder.pushFailureJumpPoint(fbb);
				this.altInstructionMap.put(p.get(i), this.builder.createIalt(p));
				p.get(i).encode(this, next, failjump);
				if (i == p.size() - 1) {
					this.builder.createIaltend(p, true, i);
				} else {
					this.builder.createIaltend(p, false, i);
				}
				this.builder.createIpop(p.get(i));
				this.builder.createIjump(p.get(i), mergebb);
				this.builder.setInsertPoint(this.builder.popFailureJumpPoint());
				if (i != p.size() - 1) {
					this.builder.createIsucc(p.get(i + 1));
					this.builder.createIpeek(p.get(i + 1));
				} else {
					this.builder.createIpop(p.get(i));
					this.builder.createIaltfin(p);
				}
			}
			this.builder.createIjump(p.get(p.size() - 1), this.builder.jumpFailureJump());
			this.builder.setInsertPoint(mergebb);
			this.builder.createIaltfin(p);
		}
		return null;
	}

	@Override
	public Instruction encodeTnew(Tnew p, Instruction next) {
		this.leftedStack.push(false);
		if (this.enabledASTConstruction) {
			this.builder.createInew(p);
		}
		return null;
	}

	@Override
	public Instruction encodeTlfold(Tlfold p, Instruction next) {
		this.leftedStack.push(true);
		if (this.enabledASTConstruction) {
			BasicBlock fbb = new BasicBlock();
			this.builder.pushFailureJumpPoint(fbb);
			this.builder.createImark(p);
			this.builder.createIleftnew(p);
		}

		return null;
	}

	Stack<Boolean> leftedStack = new Stack<Boolean>();

	@Override
	public Instruction encodeTlink(Tlink p, Instruction next, Instruction failjump) {
		if (this.enabledASTConstruction) {
			BasicBlock fbb = new BasicBlock();
			BasicBlock endbb = new BasicBlock();
			this.builder.pushFailureJumpPoint(fbb);
			this.builder.createImark(p);
			p.get(0).encode(this, next, failjump);
			this.builder.createIcommit(p);
			this.builder.createIjump(p, endbb);
			this.builder.setInsertPoint(this.builder.popFailureJumpPoint());
			this.builder.createIabort(p);
			this.builder.createIjump(p, this.builder.jumpFailureJump());
			this.builder.setInsertPoint(endbb);
		} else {
			p.get(0).encode(this, next, failjump);
		}
		return null;
	}

	@Override
	public Instruction encodeTcapture(Tcapture p, Instruction next) {
		/* newNode is used in the debugger for rich view */
		CommonTree node = (CommonTree) p.getSourcePosition();
		int len = node.toText().length();
		CommonTree newNode = new CommonTree(node.getTag(), node.getSource(), node.getSourcePosition() + len - 1, (int) (node.getSourcePosition() + len), 0, null);
		p = (Tcapture) ExpressionCommons.newTcapture(newNode, p.shift);
		if (this.enabledASTConstruction) {
			if (this.leftedStack.pop()) {
				BasicBlock endbb = new BasicBlock();
				this.builder.createIcapture(p);
				this.builder.createIpop(p);
				this.builder.createIjump(p, endbb);
				this.builder.setInsertPoint(this.builder.popFailureJumpPoint());
				this.builder.createIabort(p);
				this.builder.createIjump(p, this.builder.jumpFailureJump());
				this.builder.setInsertPoint(endbb);
			} else {
				this.builder.createIcapture(p);
			}
		}
		return null;
	}

	@Override
	public Instruction encodeTtag(Ttag p, Instruction next) {
		if (this.enabledASTConstruction) {
			this.builder.createItag(p);
		}
		return null;
	}

	@Override
	public Instruction encodeTreplace(Treplace p, Instruction next) {
		if (this.enabledASTConstruction) {
			this.builder.createIreplace(p);
		}
		return null;
	}

	@Override
	public Instruction encodeTdetree(Tdetree p, Instruction next, Instruction failjump) {
		throw new RuntimeException("undifined encode method " + p.getClass());
	}

	@Override
	public Instruction encodeXblock(Xblock p, Instruction next, Instruction failjump) {
		BasicBlock fbb = new BasicBlock();
		BasicBlock endbb = new BasicBlock();
		this.builder.pushFailureJumpPoint(fbb);
		this.builder.createIbeginscope(p);
		p.get(0).encode(this, next, failjump);
		this.builder.createIendscope(p);
		this.builder.createIjump(p, endbb);
		this.builder.setInsertPoint(this.builder.popFailureJumpPoint());
		this.builder.createIendscope(p);
		this.builder.createIjump(p, this.builder.jumpFailureJump());
		this.builder.setInsertPoint(endbb);
		return null;
	}

	@Override
	public Instruction encodeXsymbol(Xsymbol p, Instruction next, Instruction failjump) {
		BasicBlock fbb = new BasicBlock();
		BasicBlock endbb = new BasicBlock();
		this.builder.pushFailureJumpPoint(fbb);
		this.builder.createIpush(p);
		p.get(0).encode(this, next, failjump);
		this.builder.createIdef(p);
		this.builder.createIjump(p, endbb);
		this.builder.setInsertPoint(this.builder.popFailureJumpPoint());
		this.builder.createIpop(p);
		this.builder.createIjump(p, this.builder.jumpFailureJump());
		this.builder.setInsertPoint(endbb);
		return null;
	}

	@Override
	public Instruction encodeXmatch(Xmatch p, Instruction next, Instruction failjump) {
		throw new RuntimeException("undifined encode method " + p.getClass());
	}

	@Override
	public Instruction encodeXis(Xis p, Instruction next, Instruction failjump) {
		if (p.is) {
			this.builder.createIis(p, this.builder.jumpFailureJump());
			this.builder.setInsertPoint(new BasicBlock());
		} else {
			this.builder.pushFailureJumpPoint(new BasicBlock());
			this.builder.createIpush(p);
			p.get(0).encode(this, next, failjump);
			this.builder.setInsertPoint(this.builder.popFailureJumpPoint());
			this.builder.createIisa(p, this.builder.jumpFailureJump());
			this.builder.setInsertPoint(new BasicBlock());
		}
		return null;
	}

	@Override
	public Instruction encodeXdefindent(Xdefindent p, Instruction next, Instruction failjump) {
		throw new RuntimeException("undifined encode method " + p.getClass());
	}

	@Override
	public Instruction encodeXindent(Xindent p, Instruction next, Instruction failjump) {
		throw new RuntimeException("undifined encode method " + p.getClass());
	}

	@Override
	public Instruction encodeXexists(Xexists existsSymbol, Instruction next, Instruction failjump) {
		this.builder.createIexists(existsSymbol, this.builder.jumpFailureJump());
		this.builder.setInsertPoint(new BasicBlock());
		return null;
	}

	@Override
	public Instruction encodeXlocal(Xlocal localTable, Instruction next, Instruction failjump) {
		BasicBlock fbb = new BasicBlock();
		BasicBlock endbb = new BasicBlock();
		this.builder.pushFailureJumpPoint(fbb);
		this.builder.createIbeginlocalscope(localTable);
		localTable.get(0).encode(this, next, failjump);
		this.builder.createIendscope(localTable);
		this.builder.createIjump(localTable, endbb);
		this.builder.setInsertPoint(this.builder.popFailureJumpPoint());
		this.builder.createIendscope(localTable);
		this.builder.createIjump(localTable, this.builder.jumpFailureJump());
		this.builder.setInsertPoint(endbb);
		return null;
	}

}
