package nez.x.nezvm;

import java.util.ArrayList;
import java.util.List;

public class Module {
	List<Function> funcList;
	public Module() {
		this.funcList = new ArrayList<Function>();
	}
	
	public Function get(int index) {
		return this.funcList.get(index);
	}
	
	public void append(Function func) {
		this.funcList.add(func);
	}
	
	public int size() {
		return funcList.size();
	}
	
	public String stringfy(StringBuilder sb) {
		for(int i = 0; i < size(); i++) {
			this.get(i).stringfy(sb);
		}
		return sb.toString();
	}
	
	public Function get(String funcName) {
		for(int i = 0; i < this.size(); i++) {
			if (this.get(i).funcName.equals(funcName)) {
				return this.get(i);
			}
		}
		System.out.println("Error: Nonterminal Rule not found " + funcName);
		System.exit(0);
		return null;
	}
}
