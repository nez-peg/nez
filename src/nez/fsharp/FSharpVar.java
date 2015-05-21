package nez.fsharp;


public class FSharpVar {
	public String name;
	public String fullname;
	public FSharpVar parent = null;	
	public int uniqueKey = 0;
	ModifiableTree initialValue;
	
	public FSharpVar(String name, String prefixName){
		this.name = name;
		this.fullname = prefixName + name;
	}
	
	public FSharpVar(String name, String prefixName, ModifiableTree initialValue){
		this.name = name;
		this.fullname = prefixName + name;
		this.initialValue = initialValue;
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
}