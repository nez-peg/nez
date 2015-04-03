package nez.x.nezvm;

import java.util.ArrayList;
import java.util.List;

import nez.expr.Expression;

public class Function {
	Module module;
	String funcName;
	List<BasicBlock> bbList;
	public Function(Module m, String funcName) {
		this.module = m;
		this.module.append(this);
		this.funcName = funcName;
		this.bbList = new ArrayList<BasicBlock>(); 
	}
	
	public BasicBlock get(int index) {
		return this.bbList.get(index);
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
	
	public List<Instruction> serchInst(Expression e) {
		List<Instruction> ilist = new ArrayList<Instruction>();
		for(int i = 0; i < this.size(); i++) {
			BasicBlock bb = this.get(i);
			for(int j = 0; j < bb.size(); j++) {
				Instruction inst = bb.get(j);
				if (inst.expr.equals(e)) {
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
			sb.append("bb" + i + " {\n");
			this.get(i).stringfy(sb);
			sb.append("}\n");
		}
		sb.append("\n");
	}
}
