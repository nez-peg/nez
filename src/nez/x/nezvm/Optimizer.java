package nez.x.nezvm;

import java.util.List;

public class Optimizer {
	
	Module module;
	boolean O_Inlining = false;
	boolean O_MappedChoice = false;
	boolean O_FusionInstruction = false;
	boolean O_FusionOperand = false;
	boolean O_StackCaching = false;
	
	public Optimizer(Module module, boolean O_Inlining, boolean O_MappedChoice, boolean O_FusionInstruction, boolean O_FusionOperand, boolean O_StackCaching ) {
		this.module = module;
		this.O_Inlining = O_Inlining;
		this.O_MappedChoice = O_MappedChoice;
		this.O_FusionInstruction = O_FusionInstruction;
		this.O_FusionOperand = O_FusionOperand;
		this.O_StackCaching = O_StackCaching;
	}
	
	public void optimize() {
		for(int i = 0; i < this.module.size(); i++) {
			this.optimizeFunction(this.module.get(i));
		}
	}
	
	public void optimizeFunction(Function func) {
		for(int i = 0; i < func.size(); i++) {
			BasicBlock bb = func.get(i);
			
			for(int j = 0; j < bb.size(); j++) {
				Instruction inst = bb.get(j);
				if (inst instanceof NOTCHAR) {
					if (bb.get(j+1) instanceof ANY) {
						NOTCHAR ir = (NOTCHAR)bb.remove(j);
						bb.remove(j);
						NOTCHARANY nca = new NOTCHARANY(ir.expr, ir.jump, ir.getc(0));
						nca.addBasicBlock(j, bb);
						inst = nca;
					}
				}
				if (inst instanceof JumpInstruction) {
					JumpInstruction jinst = (JumpInstruction)inst;
					optimizeJump(func, bb, jinst.jump, jinst, j);
				}
				else if (inst instanceof JumpMatchingInstruction) {
					JumpMatchingInstruction jinst = (JumpMatchingInstruction)inst;
					optimizeJumpMatching(func, bb, jinst.jump, jinst, j);
				}
				else if (inst instanceof PUSHp || inst instanceof LOADp1 || inst instanceof LOADp2 || inst instanceof LOADp3) {
					if (j != bb.size()-1) {
						optimizeStackOperation(func, bb, j+1);
					}
				}
			}
		}
	}
	
	public void optimizeJump(Function func, BasicBlock bb, BasicBlock jump, JumpInstruction jinst, int index) {
		if (jump.size() == 0) {
			jump = func.get(func.indexOf(jump)+1);
			optimizeJump(func, bb, jump, jinst, index);
			return;
		}
		int currentIndex = func.indexOf(bb)+1;
		while (func.get(currentIndex).size() == 0) {
			currentIndex++;
		}
		if (func.indexOf(jump) == currentIndex && index == bb.size()-1) {
			bb.remove(index);
		}
		else if (jump.get(0) instanceof JUMP) {
			JUMP tmp = (JUMP)jump.get(0);
			jinst.jump = tmp.jump;
			optimizeJump(func, bb, tmp.jump, jinst, currentIndex);
		}
		else if (jump.get(0) instanceof RET && jinst instanceof JUMP) {
			Instruction ret = jump.get(0);
			bb.remove(index);
			bb.add(index, ret);
		}
	}
	
	public void optimizeJumpMatching(Function func, BasicBlock bb, BasicBlock jump, JumpMatchingInstruction jinst, int index) {
		if (jump.size() == 0) {
			jump = func.get(func.indexOf(jump)+1);
			optimizeJumpMatching(func, bb, jump, jinst, index);
			return;
		}
		if (jump.get(0) instanceof JUMP) {
			JUMP tmp = (JUMP)jump.get(0);
			jinst.jump = tmp.jump;
			optimizeJumpMatching(func, bb, tmp.jump, jinst, index);
		}
	}
	
	public void optimizeStackOperation(Function func, BasicBlock bb, int index) {
		Instruction inst = bb.get(index);
		if (inst instanceof PUSHp) {
			List<Instruction> ilist = func.serchInst(inst.expr);
			boolean isMappedChoiceExpr = false;
			for(int i = 0; i < ilist.size(); i++) {
				if (ilist.get(i) instanceof MAPPEDCHOICE) {
					isMappedChoiceExpr = true;
					bb.remove(index);
					break;
				}
			}
			if (isMappedChoiceExpr) {
				for(int i = 0; i < ilist.size(); i++) {
					Instruction ir = ilist.get(i);
					if (ir instanceof POPp || ir instanceof STOREp) {
						ir.bb.remove(ir.bb.indexOf(ir));
					}
				}
			}
		}
		else if (inst instanceof LOADp1) {
			bb.remove(index);
			List<Instruction> ilist = func.serchInst(inst.expr);
			for(int i = 0; i < ilist.size(); i++) {
				Instruction ir = ilist.get(i);
				if (ir instanceof STOREp1) {
					ir.bb.remove(ir.bb.indexOf(ir));
				}
			}
		}
		else if (inst instanceof LOADp2) {
			bb.remove(index);
			List<Instruction> ilist = func.serchInst(inst.expr);
			for(int i = 0; i < ilist.size(); i++) {
				Instruction ir = ilist.get(i);
				if (ir instanceof STOREp2) {
					ir.bb.remove(ir.bb.indexOf(ir));
				}
			}
		}
		else if (inst instanceof LOADp3) {
			bb.remove(index);
			List<Instruction> ilist = func.serchInst(inst.expr);
			for(int i = 0; i < ilist.size(); i++) {
				Instruction ir = ilist.get(i);
				if (ir instanceof STOREp3) {
					ir.bb.remove(ir.bb.indexOf(ir));
				}
			}
		}
	}
}
