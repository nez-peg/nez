package nez.checker.js;

import nez.checker.ModifiableTree;
import nez.checker.js.FSharpScope.ScopeType;


public class FSharpFunc {
	public String name;
	public String fullname;
	public String argsStr;
	public boolean isMember = false;
	public int uniqueKey = 0;
	ModifiableTree node;
	
	public FSharpFunc(String name){
		this.name = name;
	}
	public FSharpFunc(String name, String prefixName, boolean isMember, ModifiableTree node){
		this.name = name;
		if(isMember){
			this.fullname = prefixName.substring(0, prefixName.length() - 1) + "0." + name;
		} else {
			this.fullname = prefixName + name;
		}
		this.isMember = isMember;
		this.node = node;
		this.setArgsString();
	}
	public FSharpFunc(FSharpScope scope){
		this.name = scope.getScopeName();
		this.fullname = scope.getFullname();
		this.node = scope.node;
		if(scope.parent.type == ScopeType.OBJECT){
			this.isMember = true;
		} else {
			this.isMember = false;
		}
		this.setArgsString();
	}
	
	public String addChild(){
		String name = this.name + this.uniqueKey;
		this.uniqueKey++;
		return name;
	}
	
	public String getCurrentName(){
		return this.name + this.uniqueKey;
	}
	
	public String getTrueName(){
		return this.name;
	}
	
	public String getFullname(){
		return this.fullname;
	}
	
	private void setArgsString(){
		String argsStr = "";
		ModifiableTree argsNode = this.node.get(4);
		if(argsNode.is(JSTag.TAG_LIST)){
			for(int i = 0; i < argsNode.size(); i++){
				if(i != 0){
					argsStr += " ";
				}
				argsStr += argsNode.get(i).getText();
			}
		}
		this.argsStr = argsStr;
	}
	
	public String toString(){
		return name;
	}
}