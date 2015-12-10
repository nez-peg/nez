package nez.debugger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

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
import nez.parser.ParserGrammar;
import nez.parser.ParserStrategy;
import nez.parser.moz.MozInst;

public class DebugVMCompiler extends AbstractGenerator {
	ParserGrammar peg;
	IRBuilder builder;
	GrammarAnalyzer analyzer;
	HashMap<Expression, DebugVMInstruction> altInstructionMap = new HashMap<Expression, DebugVMInstruction>();

	public DebugVMCompiler(ParserStrategy option) {
		super(option);
		this.builder = new IRBuilder(new Module());
	}

	public Module compile(ParserGrammar grammar) {
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

	public MozInst encodeProduction(Production p) {
		this.builder.setFunction(new Function(p));
		this.builder.setInsertPoint(new BasicBlock());
		BasicBlock fbb = new BasicBlock();
		this.builder.pushFailureJumpPoint(fbb);
		p.encode(this, null, null);
		this.builder.setInsertPoint(this.builder.popFailureJumpPoint());
		this.builder.createIret(p);
		return null;
	}

	public MozInst encodeExpression(Expression e, MozInst next, MozInst failjump) {
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
	public MozInst encodeNonTerminal(NonTerminal p, MozInst next, MozInst failjump) {
		BasicBlock rbb = new BasicBlock();
		this.builder.createIcall(p, rbb, this.builder.jumpFailureJump());
		this.builder.setInsertPoint(rbb);
		return null;
	}

	@Override
	public MozInst encodeExtension(Expression p, MozInst next, MozInst failjump) {
		throw new RuntimeException("undifined encode method " + p.getClass());
	}

	@Override
	public MozInst encode(Expression e, MozInst next, MozInst failjump) {
		throw new RuntimeException("undifined encode method " + e.getClass());
	}

	@Override
	public MozInst encodePfail(Expression p) {
		this.builder.createIfail(p);
		return null;
	}

	@Override
	public MozInst encodeCany(Cany p, MozInst next, MozInst failjump) {
		this.builder.createIany(p, this.builder.jumpFailureJump());
		this.builder.setInsertPoint(new BasicBlock());
		return null;
	}

	@Override
	public MozInst encodeCbyte(Cbyte p, MozInst next, MozInst failjump) {
		this.builder.createIchar(p, this.builder.jumpFailureJump());
		this.builder.setInsertPoint(new BasicBlock());
		return null;
	}

	@Override
	public MozInst encodeCset(Cset p, MozInst next, MozInst failjump) {
		this.builder.createIcharclass(p, this.builder.jumpFailureJump());
		this.builder.setInsertPoint(new BasicBlock());
		return null;
	}

	@Override
	public MozInst encodeCmulti(Cmulti p, MozInst next, MozInst failjump) {
		this.builder.createIstr(p, this.builder.jumpFailureJump(), p.byteSeq);
		this.builder.setInsertPoint(new BasicBlock());
		return null;
	}

	@Override
	public MozInst encodePoption(Poption p, MozInst next) {
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
	public MozInst encodePzero(Pzero p, MozInst next) {
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
	public MozInst encodePone(Pone p, MozInst next, MozInst failjump) {
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
	public MozInst encodePand(Pand p, MozInst next, MozInst failjump) {
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
	public MozInst encodePnot(Pnot p, MozInst next, MozInst failjump) {
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
	public MozInst encodePsequence(Psequence p, MozInst next, MozInst failjump) {
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
	public MozInst encodePchoice(Pchoice p, MozInst next, MozInst failjump) {
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
	public MozInst encodeTnew(Tnew p, MozInst next) {
		this.leftedStack.push(false);
		if (this.strategy.TreeConstruction) {
			this.builder.createInew(p);
		}
		return null;
	}

	@Override
	public MozInst encodeTlfold(Tlfold p, MozInst next) {
		this.leftedStack.push(true);
		if (this.strategy.TreeConstruction) {
			BasicBlock fbb = new BasicBlock();
			this.builder.pushFailureJumpPoint(fbb);
			this.builder.createImark(p);
			this.builder.createIleftnew(p);
		}

		return null;
	}

	Stack<Boolean> leftedStack = new Stack<Boolean>();

	@Override
	public MozInst encodeTlink(Tlink p, MozInst next, MozInst failjump) {
		if (this.strategy.TreeConstruction) {
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
	public MozInst encodeTcapture(Tcapture p, MozInst next) {
		/* newNode is used in the debugger for rich view */
		CommonTree node = (CommonTree) p.getSourcePosition();
		int len = node.toText().length();
		CommonTree newNode = new CommonTree(node.getTag(), node.getSource(), node.getSourcePosition() + len - 1, (int) (node.getSourcePosition() + len), 0, null);
		p = (Tcapture) ExpressionCommons.newTcapture(newNode, p.shift);
		if (this.strategy.TreeConstruction) {
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
	public MozInst encodeTtag(Ttag p, MozInst next) {
		if (this.strategy.TreeConstruction) {
			this.builder.createItag(p);
		}
		return null;
	}

	@Override
	public MozInst encodeTreplace(Treplace p, MozInst next) {
		if (this.strategy.TreeConstruction) {
			this.builder.createIreplace(p);
		}
		return null;
	}

	@Override
	public MozInst encodeTdetree(Tdetree p, MozInst next, MozInst failjump) {
		throw new RuntimeException("undifined encode method " + p.getClass());
	}

	@Override
	public MozInst encodeXblock(Xblock p, MozInst next, MozInst failjump) {
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
	public MozInst encodeXsymbol(Xsymbol p, MozInst next, MozInst failjump) {
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
	public MozInst encodeXmatch(Xmatch p, MozInst next, MozInst failjump) {
		throw new RuntimeException("undifined encode method " + p.getClass());
	}

	@Override
	public MozInst encodeXis(Xis p, MozInst next, MozInst failjump) {
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
	public MozInst encodeXdefindent(Xdefindent p, MozInst next, MozInst failjump) {
		throw new RuntimeException("undifined encode method " + p.getClass());
	}

	@Override
	public MozInst encodeXindent(Xindent p, MozInst next, MozInst failjump) {
		throw new RuntimeException("undifined encode method " + p.getClass());
	}

	@Override
	public MozInst encodeXexists(Xexists existsSymbol, MozInst next, MozInst failjump) {
		this.builder.createIexists(existsSymbol, this.builder.jumpFailureJump());
		this.builder.setInsertPoint(new BasicBlock());
		return null;
	}

	@Override
	public MozInst encodeXlocal(Xlocal localTable, MozInst next, MozInst failjump) {
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
