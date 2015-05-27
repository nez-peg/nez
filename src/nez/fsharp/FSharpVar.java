package nez.fsharp;

import java.util.ArrayList;


public class FSharpVar {
	public String name;
	public String fullname;
	public FSharpVar parent = null;	
	public int uniqueKey = 0;
	ModifiableTree initialValue;
	public ArrayList<String> path;
	
	public FSharpVar(String name, ArrayList<String> path){
		this.name = name;
		this.path = path;
	}
	
	public FSharpVar(String name, ArrayList<String> path, ModifiableTree initialValue){
		this.name = name;
		this.path = path;
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
	
	public String toString(){
		return name;
	}
}