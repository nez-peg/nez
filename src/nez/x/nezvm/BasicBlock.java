package nez.x.nezvm;

import java.util.ArrayList;
import java.util.List;

public class BasicBlock {
	Function func;
	int codeIndex;
	List<Instruction> instList;
	public BasicBlock(Function func) {
		this.func = func;
		this.func.append(this);
		this.instList = new ArrayList<Instruction>();
	}
	
	public BasicBlock() {
		this.instList = new ArrayList<Instruction>();
	}
	
	public Instruction get(int index) {
		return this.instList.get(index);
	}
	
	public BasicBlock append(Instruction inst) {
		this.instList.add(inst);
		return this;
	}
	
	public BasicBlock add(int index, Instruction inst) {
		this.instList.add(index, inst);
		return this;
	}
	
	public Instruction remove(int index) {
		return this.instList.remove(index);
	}
	
	public void setInsertPoint(Function func) {
		this.func = func;
		this.func.append(this);
	}
	
	public int size() {
		return this.instList.size();
	}
	
	public int indexOf(Instruction inst) {
		return this.instList.indexOf(inst);
	}
	
	public String getBBName() {
		return "bb" + this.func.indexOf(this);
	}
	
	public void stringfy(StringBuilder sb) {
		for(int i = 0; i < this.size(); i++) {
			this.get(i).stringfy(sb);
			sb.append("\n");
		}
	}
}