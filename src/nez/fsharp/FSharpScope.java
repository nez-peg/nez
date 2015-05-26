package nez.fsharp;

import java.util.ArrayList;

import nez.ast.Tag;

public class FSharpScope {
	public String name;
	public ArrayList<String> path;
	public ModifiableTree node;
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
	public ArrayList<ModifiableTree> returnList;
	
	public FSharpScope(String name){
		this.name = name;
	}
	
	public FSharpScope(String name, ModifiableTree node, FSharpScope parentScope){
		this.name = name;
		this.node = node;
		this.parent = parentScope;
		this.varList = new ArrayList<FSharpVar>();
		this.funcList = new ArrayList<FSharpFunc>();
		this.returnList = new ArrayList<ModifiableTree>();
		this.children = new ArrayList<FSharpScope>();
		if(parentScope != null){
			this.path = parentScope.getInnerPath();
		} else {
			this.path = new ArrayList<String>();
		}
		this.recursive = this.isRecursiveFunc(this.node, this.name, false);
		if(node.is(JSTag.TAG_FUNC_DECL)){
			this.type = ScopeType.FUNCTION;
		} else if(node.is(JSTag.TAG_OBJECT)){
			this.type = ScopeType.OBJECT;
		}
		if(node != null){
			findLocalVar(node);
		}
	}
	
	public FSharpScope(String funcName, String localName, ModifiableTree node, FSharpScope parentScope){
		this.name = funcName;
		this.node = node;
		this.parent = parentScope;
		this.varList = new ArrayList<FSharpVar>();
		this.funcList = new ArrayList<FSharpFunc>();
		this.returnList = new ArrayList<ModifiableTree>();
		this.children = new ArrayList<FSharpScope>();
		if(parentScope != null){
			this.path = parentScope.getInnerPath();
		} else {
			this.path = new ArrayList<String>();
		}
		this.recursive = this.isRecursiveFunc(this.node, localName, false);
		if(node.is(JSTag.TAG_FUNC_DECL)){
			this.type = ScopeType.FUNCTION;
		} else if(node.is(JSTag.TAG_OBJECT)){
			this.type = ScopeType.OBJECT;
		}
		if(node != null){
			findLocalVar(node);
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
	
	public ArrayList<String> getInnerPath(){
		ArrayList<String> innerPath = new ArrayList<String>();
		innerPath.addAll(path);
		innerPath.add(name);
		return innerPath;
	}
	
	public boolean isRecursive(){
		return this.recursive;
	}
	
	private boolean isRecursiveFunc(ModifiableTree node, String name, boolean result){
		boolean res = false;
		if(node.is(JSTag.TAG_APPLY)){
			res = this.getFieldText(node.get(0)).contentEquals(name);
			if(res){
				node.set(0, new ModifiableTree(node.get(0).getTag(), node.getSource(), node.getSourcePosition(), node.getSourcePosition() + node.getSource().length(), node.getLength(), "_" + name));
			}
		} else {
			res = false;
		}
		if(node.size() >= 1 && !result){
			for(int i = 0; i < node.size(); i++){
				if(node.get(i) == null){
					node.set(i, new ModifiableTree(Tag.tag("Text"), null, 0, 0, 0, null));
				} else {
					result = this.isRecursiveFunc(node.get(i), name, result);
				}
			}
		}
		if(result){
			return true;
		}
		return res;
	}
	
	protected String getFieldText(ModifiableTree node){
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
	
	//In this scope(function/object), search local variables, and set them to this.varList
	private boolean findLocalVar(ModifiableTree node){
		if(node.is(JSTag.TAG_OBJECT) || node.is(JSTag.TAG_FUNC_DECL)){
			return true;
		}
		
		if(node.is(JSTag.TAG_VAR_DECL)){
			ModifiableTree nameNode = node.get(0);
			ModifiableTree valueNode = node.get(1);
			if(!nameNode.is(JSTag.TAG_NAME) || valueNode.is(JSTag.TAG_OBJECT) || valueNode.is(JSTag.TAG_FUNC_DECL)){
				return true;
			}
			varList.add(new FSharpVar(nameNode.getText(), getInnerPath(), valueNode));
			return true;
		}
		if(node.size() > 0){
			for(int i = 0; i < node.size(); i++){
				findLocalVar(node.get(i));
			}
		}
		return true;
	}
	
	public String toString(){
		StringBuilder output = new StringBuilder();
		output.append("Name: " + path.toString() + " " + name + "\n");
		output.append("Type: ");
		if(type == ScopeType.FUNCTION){
			output.append("Function\n");
		} else{
			output.append("Object\n");
		}
		output.append("Var: " + varList.toString() + "\n");
		output.append("Function: " + funcList.toString() + "\n");
		output.append(node.toString());
		return output.toString();
	}
}