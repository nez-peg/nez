package nez.fsharp;

import java.util.ArrayList;

import nez.ast.Tag;


public class FSharpGenerator extends SourceGenerator {
	
	private static boolean UseExtend;
	/**List of scopes in JavaScript. Finally, we output information which this list has.**/
	private ArrayList<FSharpScope> fsClasses;
	
	public FSharpGenerator(){
		FSharpGenerator.UseExtend = false;
	}
	
	/**Find recursively JavaScript Function and Object from ModifiableTree, and assign them to fsClasses**/
	private void findScope(ModifiableTree node, FSharpScope currentScope){
		FSharpScope nextScope = currentScope;
		if(node.is(JSTag.TAG_FUNC_DECL)){
			String funcName = node.get(2).getText();
			fsClasses.add(new FSharpScope(funcName));
		} else if (node.is(JSTag.TAG_OBJECT)){
			
		}
		
		for(int child_i = 0; child_i < node.size(); child_i++){
			findScope(node.get(child_i), nextScope);
		}
		//TODO
	}
	
	/**format ModifiedTrees, which is the flow of curerntScope operation in JavaScript program, 
	 * for F# code generation**/
	private void formatTree(FSharpScope currentScope){
		//TODO
	}
	
	private void generateFSCode(FSharpScope currentScope){
		//TODO
	}
	
	private void generatePrintCode(){
		//TODO
	}
	
	public void toSource(ModifiableTree node){
		findScope(node, null);
		for(FSharpScope fsClass : fsClasses){
			formatTree(fsClass);
		}
		for(FSharpScope fsClass : fsClasses){
			generateFSCode(fsClass);
		}
		generatePrintCode();
	}
	
}
