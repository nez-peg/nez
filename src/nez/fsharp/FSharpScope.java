package nez.fsharp;

import java.util.ArrayList;

import nez.ast.CommonTree;

public class FSharpScope {
	public String name;
	public ArrayList<String> path;
	public CommonTree node;
	public ArrayList<FSharpVar> varList;
	public ArrayList<FSharpFunc> funcList;
	public FSharpScope parent;
	public ArrayList<FSharpScope> children;
	public int numOfArgs = 0;
	public boolean recursive = false;
	public enum ScopeType{
		OBJECT,
		FUNCTION
	}
	public ScopeType type;
	public ArrayList<CommonTree> returnList;
	
	public FSharpScope(String name){
		this.name = name;
	}
	
	public FSharpScope(String name, CommonTree node, ArrayList<String> path){
		this.name = name;
		this.node = node;
		this.varList = new ArrayList<FSharpVar>();
		this.funcList = new ArrayList<FSharpFunc>();
		this.returnList = new ArrayList<CommonTree>();
		this.children = new ArrayList<FSharpScope>();
		this.path = new ArrayList<String>();
		//deep copy
		for(int i = 0; i < path.size(); i++){
			this.path.add(path.get(i));
		}
		this.recursive = this.isRecursiveFunc(this.node, this.name, false);
		if(node.is(JSTag.TAG_FUNC_DECL)){
			this.type = ScopeType.FUNCTION;
		} else if(node.is(JSTag.TAG_OBJECT)){
			this.type = ScopeType.OBJECT;
		}
	}
	
	public String getScopeName(){
		return "ScopeOf" + this.getFullname();
	}
	
	public String getFullname(){
		String fullname = "";
		for(int i = 0; i < this.path.size(); i++){
			fullname += this.path.get(i) + "_";
		}
		fullname += this.name;
		return fullname;
	}
	
	public String getPathName(){
		String pathName = "";
		for(int i = 0; i < this.path.size(); i++){
			pathName += this.path.get(i) + ".";
		}
		pathName += this.name + ".";
		return pathName;
	}
	
	public boolean isRecursive(){
		return this.recursive;
	}
	
	private boolean isRecursiveFunc(CommonTree node, String name, boolean result){
		boolean res = false;
		if(node.is(JSTag.TAG_APPLY)){
			res = this.getFieldText(node.get(0)).contentEquals(name);
			if(res){
				node.get(0).setValue("_" + name);
			}
		} else {
			res = false;
		}
		if(node.size() >= 1 && !result){
			for(int i = 0; i < node.size(); i++){
				result = this.isRecursiveFunc(node.get(i), name, result);
			}
		}
		if(result){
			return true;
		}
		return res;
	}
	
	protected String getFieldText(CommonTree node){
		String result = "";
		if(node.is(JSTag.TAG_FIELD)){
			for(int i = 0; i < node.size(); i++){
				result += node.get(i).getText();
				if(i == node.size() - 1){
					result += ".";
				}
			}
		} else if(node.is(JSTag.TAG_NAME)){
			result += node.getText();
		}
		return result;
	}
	
	public FSharpVar searchVar(String name){
		for(FSharpVar fv : this.varList){
			if(fv.getTrueName().contentEquals(name)){
				return fv;
			}
		}
		return null;
	}
	
	public FSharpFunc searchFunc(String name){
		for(FSharpFunc ff : this.funcList){
			if(ff.getTrueName().contentEquals(name)){
				return ff;
			}
		}
		if(this.name.contentEquals(name)){
			return  new FSharpFunc("ok");
		}
		return null;
	}
	
	public FSharpVar getAvailableVar(String name){
		FSharpVar fv = this.searchVar(name);
		if(fv != null){
			return fv;
		}
		if(this.parent != null){
			return this.parent.getAvailableVar(name);
		} else {
			return null;
		}
	}
	
	public FSharpFunc getAvailableFunc(String name){
		FSharpFunc ff = this.searchFunc(name);
		if(ff != null){
			return ff;
		}
		if(this.parent != null){
			return this.parent.getAvailableFunc(name);
		} else {
			return null;
		}
	}
	
	public FSharpScope getAvailableScope(String name){
		for(FSharpScope fs : this.children){
			if(name.contentEquals(fs.name)){
				return fs;
			}
		}
		String scopeName;
		FSharpScope result = null;
		FSharpScope parent = this.parent;
		while(parent != null){
			if(name.contentEquals(parent.name)){
				return parent;
			}
			scopeName = parent.name.split("_")[parent.name.split("_").length - 1];
			if(name.contentEquals(scopeName)){
				return parent;
			}
			result = parent.getAvailableScope(name);
			if(result != null){
				return result;
			}
			for(FSharpScope fs : parent.children){
				if(fs.name.contentEquals(name)){
					return fs;
				}
			}
			parent = parent.parent;
		}
		parent = this.parent;
		if(parent != null){
			while(parent.parent != null){
				parent = parent.parent;
			}
			return searchChildren(parent, name);
		}
		return searchChildren(this, name);
	}
	
	private FSharpScope searchChildren(FSharpScope parent, String name){
		if(parent.children != null){
			for(FSharpScope child : parent.children){
				if(child.name.contentEquals(name)){
					return child;
				}
				return this.searchChildren(child, name);
			}	
		}
		return null;
	}
	
	public boolean add(FSharpScope child){
		return this.children.add(child);
	}
	
	public boolean remove(FSharpScope child){
		return this.children.remove(child);
	}
	
	public FSharpScope remove(int index){
		return this.children.remove(index);
	}
	
	public boolean isArgumentVar(String name){
		for(int i = 0; i < this.numOfArgs; i++){
			if(this.varList.get(i).getTrueName().contentEquals(name)){
				return true;
			}
		}
		return false;
	}
}