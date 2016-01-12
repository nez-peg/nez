package nez.debugger;

import java.util.ArrayList;
import java.util.List;

import nez.lang.Expression;
import nez.lang.Production;

public class Function {
	String funcName;
	Production rule;
	List<BasicBlock> bbList;
	List<Function> callers;

	public Function(Production rule) {
		this.funcName = rule.getLocalName();
		this.rule = rule;
		this.bbList = new ArrayList<BasicBlock>();
		this.callers = new ArrayList<Function>();
	}

	public BasicBlock get(int index) {
		return this.bbList.get(index);
	}

	public void setCaller(Function func) {
		if(!callers.contains(func)) {
			this.callers.add(func);
		}
	}

	public DebugVMInstruction getStartInstruction() {
		BasicBlock bb = this.get(0);
		while(bb.size() == 0) {
			bb = bb.getSingleSuccessor();
		}
		return bb.get(0);
	}

	public Function append(BasicBlock bb) {
		this.bbList.add(bb);
		return this;
	}

	public Function add(int index, BasicBlock bb) {
		this.bbList.add(index, bb);
		return this;
	}

	public BasicBlock remove(int index) {
		return this.bbList.remove(index);
	}

	public List<DebugVMInstruction> serchInst(Expression e) {
		List<DebugVMInstruction> ilist = new ArrayList<DebugVMInstruction>();
		for(int i = 0; i < this.size(); i++) {
			BasicBlock bb = this.get(i);
			for(int j = 0; j < bb.size(); j++) {
				DebugVMInstruction inst = bb.get(j);
				if(inst.expr.equals(e)) {
					ilist.add(inst);
				}
			}
		}
		return ilist;
	}

	public int size() {
		return this.bbList.size();
	}

	public int instSize() {
		int size = 0;
		for(int i = 0; i < this.size(); i++) {
			size += this.get(i).size();
		}
		return size;
	}

	public int indexOf(BasicBlock bb) {
		return this.bbList.indexOf(bb);
	}

	public void stringfy(StringBuilder sb) {
		sb.append(this.funcName + ":\n");
		for(int i = 0; i < this.size(); i++) {
			BasicBlock bb = this.get(i);
			sb.append(bb.name + " {\n");
			bb.stringfy(sb);
			sb.append("}\n");
		}
		sb.append("\n");
	}
}
