package nez.fsharp;

import java.util.ArrayList;

import nez.ast.Tag;


public class FSharpGenerator extends SourceGenerator {
	
	private static boolean UseExtend;
	/**List of scopes in JavaScript. Finally, we output information which this list has.**/
	private ArrayList<FSharpScope> fsClasses;
	private int lambdaIdentifier = 0;

	public FSharpGenerator(){
		FSharpGenerator.UseExtend = false;
		fsClasses = new ArrayList<FSharpScope>();
	}
	
	private FSharpScope addFunctionToList(ModifiableTree node, FSharpScope parentScope){

		FSharpScope newScope = null;	//newScope is the node which will be added to List
		
		//case: Define by the code, such as ( function $NAME (...){...}; )
		if(node.get(2).is(JSTag.TAG_NAME) && !node.getParent().is(JSTag.TAG_ASSIGN)){
			newScope = new FSharpScope(node.get(2).getText(), node, parentScope);
		} else {
			ModifiableTree parent = node.getParent();
			//case: function is assigned to the Variable by Tag.VarDecl
			//case: function is assigned to the Property by Tag.Property
			//case: function is assigned to the Variable by Tag.Assign
			if(parent.is(JSTag.TAG_VAR_DECL) || parent.is(JSTag.TAG_PROPERTY) || parent.is(JSTag.TAG_ASSIGN)){
				//case: function is lambda
				if(!node.get(2).is(JSTag.TAG_NAME)){
					newScope = new FSharpScope(parent.get(0).getText(), node, parentScope);
				}
				//case: function is not lambda. function is named local name.
				else {
					newScope = new FSharpScope(parent.get(0).getText(), node.get(2).getText(), node, parentScope);
				}
			}
			//case: function is lambda, and is not assigned
			else {
				newScope = new FSharpScope("lambda" + lambdaIdentifier++, node, parentScope);
			}
		}
		
		fsClasses.add(newScope);
		parentScope.add(newScope);
		parentScope.funcList.add(new FSharpFunc(newScope));
		return newScope;
	}
	
	private FSharpScope addObjectToList(ModifiableTree node, FSharpScope parentScope){
		FSharpScope newScope = null;
		ModifiableTree parent = node.getParent();
		
		//case: object is not lambda.
		if(parent.is(JSTag.TAG_ASSIGN) || parent.is(JSTag.TAG_PROPERTY) || parent.is(JSTag.TAG_VAR_DECL)){
			newScope = new FSharpScope(parent.get(0).getText(), node, parentScope);
		}
		//case: object is lambda. example) arguments etc...
		else {
			newScope = new FSharpScope("lambda" + lambdaIdentifier++, node, parentScope);
		}
		
		fsClasses.add(newScope);
		parentScope.add(newScope);
		parentScope.varList.add(new FSharpVar(newScope.name, newScope.getPathName()));
		return newScope;
	}
	
	/**Find recursively JavaScript Function and Object from ModifiableTree, and assign them to fsClasses**/
	private void findScope(ModifiableTree node, FSharpScope currentScope){
		FSharpScope nextScope = currentScope;
		if(node.is(JSTag.TAG_FUNC_DECL)){
			nextScope = addFunctionToList(node, currentScope);
		} else if (node.is(JSTag.TAG_OBJECT)){
			nextScope = addObjectToList(node, currentScope);
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
		FSharpScope topScope = new FSharpScope("TOPLEVEL", node, null);
		findScope(node, topScope);
		//print debug
		for(FSharpScope fs : fsClasses){
			System.out.println(fs.toString());
		}
		//
		for(FSharpScope fsClass : fsClasses){
			formatTree(fsClass);
		}
		for(FSharpScope fsClass : fsClasses){
			generateFSCode(fsClass);
		}
		generatePrintCode();
	}
	
}
