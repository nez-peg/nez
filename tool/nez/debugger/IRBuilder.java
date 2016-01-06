package nez.debugger;

import nez.lang.Expression;
import nez.lang.Nez;
import nez.lang.Production;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cset;
import nez.lang.expr.Tlfold;
import nez.lang.expr.Tlink;
import nez.lang.expr.Treplace;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xis;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xsymbol;
import nez.parser.moz.ParserGrammar;

public class IRBuilder {
	private BasicBlock curBB;
	private Module module;
	private Function func;

	public IRBuilder(Module m) {
		this.module = m;
	}

	public Module buildInstructionSequence() {
		int codeIndex = 0;
		for (int i = 0; i < this.module.size(); i++) {
			Function func = this.module.get(i);
			for (int j = 0; j < func.size(); j++) {
				BasicBlock bb = func.get(j);
				bb.codePoint = codeIndex;
				codeIndex += bb.size();
			}
		}
		for (Function f : this.module.funcList) {
			DebugVMInstruction prev = null;
			for (BasicBlock bb : f.bbList) {
				for (DebugVMInstruction inst : bb.insts) {
					if (prev != null && prev.next == null) {
						prev.next = inst;
					}
					if (inst.op.equals(Opcode.Icall)) {
						Icall call = (Icall) inst;
						Function callFunc = this.module.get(call.ne.getLocalName());
						callFunc.setCaller(f);
						call.setJump(callFunc.get(0).codePoint);
						call.next = callFunc.getStartInstruction();
						bb.setSingleSuccessor(call.jumpBB);
						call.jump = call.jumpBB.getStartInstruction();
						bb.setFailSuccessor(call.failBB);
						call.failjump = call.failBB.getStartInstruction();
					} else if (inst instanceof JumpInstruction) {
						JumpInstruction jinst = (JumpInstruction) inst;
						BasicBlock jbb = jinst.jumpBB;
						jinst.jump = jbb.getStartInstruction();
					}
					prev = inst;
				}
			}
		}
		// this.dumpLastestCode();
		return this.module;
	}

	private void dumpLastestCode() {
		int codeIndex = 0;
		for (int i = 0; i < this.module.size(); i++) {
			Function func = this.module.get(i);
			for (int j = 0; j < func.size(); j++) {
				BasicBlock bb = func.get(j);
				for (int k = 0; k < bb.size(); k++) {
					DebugVMInstruction inst = bb.get(k);
					System.out.println("[" + codeIndex + "] " + inst.toString());
					codeIndex++;
				}
			}
		}
	}

	public Module getModule() {
		return this.module;
	}

	public void setGrammar(ParserGrammar g) {
		this.module.setGrammar(g);
	}

	public Function getFunction() {
		return this.func;
	}

	public void setFunction(Function func) {
		this.module.append(func);
		this.func = func;
	}

	public void setInsertPoint(BasicBlock bb) {
		this.func.append(bb);
		bb.setName("bb" + this.func.size());
		if (this.curBB != null) {
			if (bb.size() != 0) {
				DebugVMInstruction last = this.curBB.get(this.curBB.size() - 1);
				if (!(last.op.equals(Opcode.Ijump) || last.op.equals(Opcode.Icall))) {
					this.curBB.setSingleSuccessor(bb);
				}
			} else {
				this.curBB.setSingleSuccessor(bb);
			}
		}
		this.curBB = bb;
	}

	public void setCurrentBB(BasicBlock bb) {
		this.curBB = bb;
	}

	public BasicBlock getCurrentBB() {
		return this.curBB;
	}

	class FailureBB {
		BasicBlock fbb;
		FailureBB prev;

		public FailureBB(BasicBlock bb, FailureBB prev) {
			this.fbb = bb;
			this.prev = prev;
		}
	}

	FailureBB fLabel = null;

	public void pushFailureJumpPoint(BasicBlock bb) {
		this.fLabel = new FailureBB(bb, this.fLabel);
	}

	public BasicBlock popFailureJumpPoint() {
		BasicBlock fbb = this.fLabel.fbb;
		this.fLabel = this.fLabel.prev;
		return fbb;
	}

	public BasicBlock jumpFailureJump() {
		return this.fLabel.fbb;
	}

	public BasicBlock jumpPrevFailureJump() {
		return this.fLabel.prev.fbb;
	}

	public DebugVMInstruction createIexit(Expression e) {
		return this.curBB.append(new Iexit(e));
	}

	public DebugVMInstruction createInop(Production e) {
		return this.curBB.append(new Inop(e));
	}

	public DebugVMInstruction createIcall(nez.lang.NonTerminal e, BasicBlock jump, BasicBlock failjump) {
		return this.curBB.append(new Icall(e, jump, failjump));
	}

	public DebugVMInstruction createIret(Production e) {
		return this.curBB.append(new Iret(e));
	}

	public DebugVMInstruction createIjump(Expression e, BasicBlock jump) {
		return this.curBB.append(new Ijump(e, jump));
	}

	public DebugVMInstruction createIiffail(Expression e, BasicBlock jump) {
		return this.curBB.append(new Iiffail(e, jump));
	}

	public DebugVMInstruction createIpush(Expression e) {
		return this.curBB.append(new Ipush(e));
	}

	public DebugVMInstruction createIpop(Expression e) {
		return this.curBB.append(new Ipop(e));
	}

	public DebugVMInstruction createIpeek(Expression e) {
		return this.curBB.append(new Ipeek(e));
	}

	public DebugVMInstruction createIsucc(Expression e) {
		return this.curBB.append(new Isucc(e));
	}

	public DebugVMInstruction createIfail(Expression e) {
		return this.curBB.append(new Ifail(e));
	}

	public DebugVMInstruction createIchar(Cbyte e, BasicBlock jump) {
		return this.curBB.append(new Ichar(e, jump));
	}

	public DebugVMInstruction createIstr(Expression e, BasicBlock jump, byte[] utf8) {
		return this.curBB.append(new Istr(e, jump, utf8));
	}

	public DebugVMInstruction createIcharclass(Cset e, BasicBlock jump) {
		return this.curBB.append(new Icharclass(e, jump));
	}

	public DebugVMInstruction createIcharclass(Expression e, BasicBlock jump, boolean[] byteMap) {
		return this.curBB.append(new Icharclass(e, jump, byteMap));
	}

	public DebugVMInstruction createIany(Cany e, BasicBlock jump) {
		return this.curBB.append(new Iany(e, jump));
	}

	public DebugVMInstruction createInew(Expression e) {
		return this.curBB.append(new Inew(e));
	}

	public DebugVMInstruction createIleftnew(Tlfold e) {
		return this.curBB.append(new Ileftnew(e));
	}

	public DebugVMInstruction createIcapture(Expression e) {
		return this.curBB.append(new Icapture(e));
	}

	public DebugVMInstruction createImark(Expression e) {
		return this.curBB.append(new Imark(e));
	}

	public DebugVMInstruction createItag(Nez.Tag e) {
		return this.curBB.append(new Itag(e));
	}

	public DebugVMInstruction createIreplace(Treplace e) {
		return this.curBB.append(new Ireplace(e));
	}

	public DebugVMInstruction createIcommit(Tlink e) {
		return this.curBB.append(new Icommit(e));
	}

	public DebugVMInstruction createIabort(Expression e) {
		return this.curBB.append(new Iabort(e));
	}

	public DebugVMInstruction createIdef(Xsymbol e) {
		return this.curBB.append(new Idef(e));
	}

	public DebugVMInstruction createIis(Xis e, BasicBlock jump) {
		return this.curBB.append(new Iis(e, jump));
	}

	public DebugVMInstruction createIisa(Xis e, BasicBlock jump) {
		return this.curBB.append(new Iisa(e, jump));
	}

	public DebugVMInstruction createIexists(Xexists e, BasicBlock jump) {
		return this.curBB.append(new Iexists(e, jump));
	}

	public DebugVMInstruction createIbeginscope(Expression e) {
		return this.curBB.append(new Ibeginscope(e));
	}

	public DebugVMInstruction createIbeginlocalscope(Xlocal e) {
		return this.curBB.append(new Ibeginlocalscope(e));
	}

	public DebugVMInstruction createIendscope(Expression e) {
		return this.curBB.append(new Iendscope(e));
	}

	public DebugVMInstruction createIaltstart(Expression e) {
		return this.curBB.append(new Ialtstart(e));
	}

	public DebugVMInstruction createIalt(Expression e) {
		return this.curBB.append(new Ialt(e));
	}

	public DebugVMInstruction createIaltend(Nez.Choice e, boolean last, int index) {
		return this.curBB.append(new Ialtend(e, last, index));
	}

	public DebugVMInstruction createIaltfin(Expression e) {
		return this.curBB.append(new Ialtfin(e));
	}
}
