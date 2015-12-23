package nez.debugger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import nez.ast.CommonTree;
import nez.lang.Expression;
import nez.lang.Nez;
import nez.lang.Predicate;
import nez.lang.Production;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cset;
import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Psequence;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Tlfold;
import nez.lang.expr.Tlink;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xis;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xsymbol;
import nez.parser.ParserGrammar;
import nez.parser.ParserStrategy;
import nez.parser.moz.MozInst;

public class DebugVMCompiler extends Expression.Visitor {
	ParserGrammar peg;
	IRBuilder builder;
	GrammarAnalyzer analyzer;
	HashMap<Expression, DebugVMInstruction> altInstructionMap = new HashMap<Expression, DebugVMInstruction>();
	ParserStrategy strategy;

	public DebugVMCompiler(ParserStrategy option) {
		this.strategy = option;
		this.builder = new IRBuilder(new Module());
	}

	public Module compile(ParserGrammar grammar) {
		this.builder.setGrammar(grammar);
		this.analyzer = new GrammarAnalyzer(grammar);
		this.analyzer.analyze();
		for (Production p : grammar.getProductionList()) {
			this.visitProduction(p);
		}
		// ConsoleUtils.println(this.builder.getModule().stringfy(new
		// StringBuilder()));
		return this.builder.buildInstructionSequence();
	}

	public Module getModule() {
		return this.builder.getModule();
	}

	public MozInst visitProduction(Production p) {
		this.builder.setFunction(new Function(p));
		this.builder.setInsertPoint(new BasicBlock());
		BasicBlock fbb = new BasicBlock();
		this.builder.pushFailureJumpPoint(fbb);
		p.getExpression().visit(this, null);
		this.builder.setInsertPoint(this.builder.popFailureJumpPoint());
		this.builder.createIret(p);
		return null;
	}

	public MozInst visitExpression(Expression e, Object next) {
		return (MozInst) e.visit(this, next);
	}

	ArrayList<Byte> charList = new ArrayList<Byte>();

	public boolean optimizeString(Nez.Pair seq) {
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

	public boolean optimizeCharSet(Nez.Choice p) {
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
	public MozInst visitNonTerminal(NonTerminal p, Object next) {
		BasicBlock rbb = new BasicBlock();
		this.builder.createIcall(p, rbb, this.builder.jumpFailureJump());
		this.builder.setInsertPoint(rbb);
		return null;
	}

	@Override
	public MozInst visitEmpty(Nez.Empty e, Object next) {
		return (MozInst) next;
	}

	@Override
	public MozInst visitFail(Nez.Fail p, Object next) {
		this.builder.createIfail(p);
		return null;
	}

	@Override
	public MozInst visitAny(Nez.Any p, Object next) {
		this.builder.createIany((Cany) p, this.builder.jumpFailureJump());
		this.builder.setInsertPoint(new BasicBlock());
		return null;
	}

	@Override
	public MozInst visitByte(Nez.Byte p, Object next) {
		this.builder.createIchar((Cbyte) p, this.builder.jumpFailureJump());
		this.builder.setInsertPoint(new BasicBlock());
		return null;
	}

	@Override
	public MozInst visitByteSet(Nez.ByteSet p, Object next) {
		this.builder.createIcharclass((Cset) p, this.builder.jumpFailureJump());
		this.builder.setInsertPoint(new BasicBlock());
		return null;
	}

	@Override
	public MozInst visitString(Nez.String p, Object next) {
		this.builder.createIstr(p, this.builder.jumpFailureJump(), p.byteSeq);
		this.builder.setInsertPoint(new BasicBlock());
		return null;
	}

	@Override
	public MozInst visitOption(Nez.Option p, Object next) {
		BasicBlock fbb = new BasicBlock();
		BasicBlock mergebb = new BasicBlock();
		this.builder.pushFailureJumpPoint(fbb);
		this.builder.createIpush(p);
		p.get(0).visit(this, next);
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
	public MozInst visitZeroMore(Nez.ZeroMore p, Object next) {
		BasicBlock topBB = new BasicBlock();
		this.builder.setInsertPoint(topBB);
		BasicBlock fbb = new BasicBlock();
		this.builder.pushFailureJumpPoint(fbb);
		this.builder.createIpush(p);
		p.get(0).visit(this, next);
		this.builder.createIpop(p);
		this.builder.createIjump(p, topBB);
		this.builder.setInsertPoint(this.builder.popFailureJumpPoint());
		this.builder.createIsucc(p);
		this.builder.createIpeek(p);
		this.builder.createIpop(p);
		return null;
	}

	@Override
	public MozInst visitOneMore(Nez.OneMore p, Object next) {
		p.get(0).visit(this, next);
		BasicBlock topBB = new BasicBlock();
		this.builder.setInsertPoint(topBB);
		BasicBlock fbb = new BasicBlock();
		this.builder.pushFailureJumpPoint(fbb);
		this.builder.createIpush(p);
		p.get(0).visit(this, next);
		this.builder.createIpop(p);
		this.builder.createIjump(p, topBB);
		this.builder.setInsertPoint(this.builder.popFailureJumpPoint());
		this.builder.createIsucc(p);
		this.builder.createIpeek(p);
		this.builder.createIpop(p);
		return null;
	}

	@Override
	public MozInst visitAnd(Nez.And p, Object next) {
		BasicBlock fbb = new BasicBlock();
		this.builder.pushFailureJumpPoint(fbb);
		this.builder.createIpush(p);
		p.get(0).visit(this, next);
		this.builder.setInsertPoint(this.builder.popFailureJumpPoint());
		this.builder.createIpeek(p);
		this.builder.createIpop(p);
		this.builder.createIiffail(p, this.builder.jumpFailureJump());
		this.builder.setInsertPoint(new BasicBlock());
		return null;
	}

	@Override
	public MozInst visitNot(Nez.Not p, Object next) {
		BasicBlock fbb = new BasicBlock();
		this.builder.pushFailureJumpPoint(fbb);
		this.builder.createIpush(p);
		p.get(0).visit(this, next);
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
	public MozInst visitPair(Nez.Pair p, Object next) {
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
			p.get(i).visit(this, next);
		}
		return null;
	}

	@Override
	public MozInst visitChoice(Nez.Choice p, Object next) {
		if (!optimizeCharSet(p)) {
			BasicBlock fbb = null;
			BasicBlock mergebb = new BasicBlock();
			this.builder.createIaltstart(p);
			this.builder.createIpush(p.get(0));
			for (int i = 0; i < p.size(); i++) {
				fbb = new BasicBlock();
				this.builder.pushFailureJumpPoint(fbb);
				this.altInstructionMap.put(p.get(i), this.builder.createIalt(p));
				p.get(i).visit(this, next);
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
	public MozInst visitPreNew(Nez.PreNew p, Object next) {
		this.leftedStack.push(false);
		if (this.strategy.TreeConstruction) {
			this.builder.createInew(p);
		}
		return null;
	}

	@Override
	public MozInst visitLeftFold(Nez.LeftFold p, Object next) {
		this.leftedStack.push(true);
		if (this.strategy.TreeConstruction) {
			BasicBlock fbb = new BasicBlock();
			this.builder.pushFailureJumpPoint(fbb);
			this.builder.createImark(p);
			this.builder.createIleftnew((Tlfold) p);
		}

		return null;
	}

	Stack<Boolean> leftedStack = new Stack<Boolean>();

	@Override
	public MozInst visitLink(Nez.Link p, Object next) {
		if (this.strategy.TreeConstruction) {
			BasicBlock fbb = new BasicBlock();
			BasicBlock endbb = new BasicBlock();
			this.builder.pushFailureJumpPoint(fbb);
			this.builder.createImark(p);
			p.get(0).visit(this, next);
			this.builder.createIcommit((Tlink) p);
			this.builder.createIjump(p, endbb);
			this.builder.setInsertPoint(this.builder.popFailureJumpPoint());
			this.builder.createIabort(p);
			this.builder.createIjump(p, this.builder.jumpFailureJump());
			this.builder.setInsertPoint(endbb);
		} else {
			p.get(0).visit(this, next);
		}
		return null;
	}

	@Override
	public MozInst visitNew(Nez.New p, Object next) {
		/* newNode is used in the debugger for rich view */
		CommonTree node = (CommonTree) p.getSourceLocation();
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
	public MozInst visitTag(Nez.Tag p, Object next) {
		if (this.strategy.TreeConstruction) {
			this.builder.createItag((Ttag) p);
		}
		return null;
	}

	@Override
	public MozInst visitReplace(Nez.Replace p, Object next) {
		if (this.strategy.TreeConstruction) {
			this.builder.createIreplace((Treplace) p);
		}
		return null;
	}

	@Override
	public MozInst visitDetree(Nez.Detree p, Object next) {
		throw new RuntimeException("undifined visit method " + p.getClass());
	}

	@Override
	public MozInst visitBlockScope(Nez.BlockScope p, Object next) {
		BasicBlock fbb = new BasicBlock();
		BasicBlock endbb = new BasicBlock();
		this.builder.pushFailureJumpPoint(fbb);
		this.builder.createIbeginscope(p);
		p.get(0).visit(this, next);
		this.builder.createIendscope(p);
		this.builder.createIjump(p, endbb);
		this.builder.setInsertPoint(this.builder.popFailureJumpPoint());
		this.builder.createIendscope(p);
		this.builder.createIjump(p, this.builder.jumpFailureJump());
		this.builder.setInsertPoint(endbb);
		return null;
	}

	@Override
	public MozInst visitSymbolAction(Nez.SymbolAction p, Object next) {
		BasicBlock fbb = new BasicBlock();
		BasicBlock endbb = new BasicBlock();
		this.builder.pushFailureJumpPoint(fbb);
		this.builder.createIpush(p);
		p.get(0).visit(this, next);
		this.builder.createIdef((Xsymbol) p);
		this.builder.createIjump(p, endbb);
		this.builder.setInsertPoint(this.builder.popFailureJumpPoint());
		this.builder.createIpop(p);
		this.builder.createIjump(p, this.builder.jumpFailureJump());
		this.builder.setInsertPoint(endbb);
		return null;
	}

	@Override
	public MozInst visitSymbolMatch(Nez.SymbolMatch p, Object next) {
		throw new RuntimeException("undifined visit method " + p.getClass());
	}

	@Override
	public MozInst visitSymbolPredicate(Nez.SymbolPredicate p, Object next) {
		if (p.op == Predicate.is) {
			this.builder.createIis((Xis) p, this.builder.jumpFailureJump());
			this.builder.setInsertPoint(new BasicBlock());
		} else {
			this.builder.pushFailureJumpPoint(new BasicBlock());
			this.builder.createIpush(p);
			p.get(0).visit(this, next);
			this.builder.setInsertPoint(this.builder.popFailureJumpPoint());
			this.builder.createIisa((Xis) p, this.builder.jumpFailureJump());
			this.builder.setInsertPoint(new BasicBlock());
		}
		return null;
	}

	@Override
	public MozInst visitSymbolExists(Nez.SymbolExists existsSymbol, Object next) {
		this.builder.createIexists((Xexists) existsSymbol, this.builder.jumpFailureJump());
		this.builder.setInsertPoint(new BasicBlock());
		return null;
	}

	@Override
	public MozInst visitLocalScope(Nez.LocalScope localTable, Object next) {
		BasicBlock fbb = new BasicBlock();
		BasicBlock endbb = new BasicBlock();
		this.builder.pushFailureJumpPoint(fbb);
		this.builder.createIbeginlocalscope((Xlocal) localTable);
		localTable.get(0).visit(this, next);
		this.builder.createIendscope(localTable);
		this.builder.createIjump(localTable, endbb);
		this.builder.setInsertPoint(this.builder.popFailureJumpPoint());
		this.builder.createIendscope(localTable);
		this.builder.createIjump(localTable, this.builder.jumpFailureJump());
		this.builder.setInsertPoint(endbb);
		return null;
	}

	@Override
	public Object visitIf(Nez.If e, Object a) {
		// TODO Auto-generated method stub
		return a;
	}

	@Override
	public Object visitOn(Nez.On e, Object a) {
		// TODO Auto-generated method stub
		return a;
	}

}
