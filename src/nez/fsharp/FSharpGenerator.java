package nez.fsharp;

import java.util.ArrayList;

public class FSharpGenerator extends SourceGenerator {

	private static boolean UseExtend;
	/**
	 * List of scopes in JavaScript. Finally, we output information which this
	 * list has.
	 **/
	private ArrayList<FSharpScope> fsClasses;
	private int lambdaIdentifier = 0;

	public FSharpGenerator() {
		FSharpGenerator.UseExtend = false;
		fsClasses = new ArrayList<FSharpScope>();
	}

	private FSharpScope addFunctionToList(ModifiableTree node,
			FSharpScope parentScope) {

		FSharpScope newScope = null; // newScope is the node which will be added
										// to List

		// case: Define by the code, such as ( function $NAME (...){...}; )
		if (node.get(2).is(JSTag.TAG_NAME)
				&& !node.getParent().is(JSTag.TAG_ASSIGN)) {
			newScope = new FSharpScope(node.get(2).getText(), node, parentScope);
		} else {
			ModifiableTree parent = node.getParent();
			// case: function is assigned to the Variable by Tag.VarDecl
			// case: function is assigned to the Property by Tag.Property
			// case: function is assigned to the Variable by Tag.Assign
			if (parent.is(JSTag.TAG_VAR_DECL) || parent.is(JSTag.TAG_PROPERTY)
					|| parent.is(JSTag.TAG_ASSIGN)) {
				// case: function is lambda
				if (!node.get(2).is(JSTag.TAG_NAME)) {
					newScope = new FSharpScope(parent.get(0).getText(), node,
							parentScope);
				}
				// case: function is not lambda. function is named local name.
				else {
					newScope = new FSharpScope(parent.get(0).getText(), node
							.get(2).getText(), node, parentScope);
				}
			}
			// case: function is lambda, and is not assigned
			else {
				newScope = new FSharpScope("lambda" + lambdaIdentifier++, node,
						parentScope);
			}
		}

		fsClasses.add(newScope);
		parentScope.add(newScope);
		parentScope.funcList.add(new FSharpFunc(newScope));
		return newScope;
	}

	private FSharpScope addObjectToList(ModifiableTree node,
			FSharpScope parentScope) {
		FSharpScope newScope = null;
		ModifiableTree parent = node.getParent();

		// case: object is not lambda.
		if (parent.is(JSTag.TAG_ASSIGN) || parent.is(JSTag.TAG_PROPERTY)
				|| parent.is(JSTag.TAG_VAR_DECL)) {
			newScope = new FSharpScope(parent.get(0).getText(), node,
					parentScope);
		}
		// case: object is lambda. example) arguments etc...
		else {
			newScope = new FSharpScope("lambda" + lambdaIdentifier++, node,
					parentScope);
		}

		fsClasses.add(newScope);
		parentScope.add(newScope);
		parentScope.varList.add(new FSharpVar(newScope.name, newScope
				.getInnerPath(), node));
		return newScope;
	}

	/**
	 * Find recursively JavaScript Function and Object from ModifiableTree, and
	 * assign them to fsClasses
	 **/
	private void findScope(ModifiableTree node, FSharpScope currentScope) {
		FSharpScope nextScope = currentScope;
		if (node.is(JSTag.TAG_FUNC_DECL)) {
			nextScope = addFunctionToList(node, currentScope);
		} else if (node.is(JSTag.TAG_OBJECT)) {
			nextScope = addObjectToList(node, currentScope);
		}

		for (int child_i = 0; child_i < node.size(); child_i++) {
			findScope(node.get(child_i), nextScope);
		}
	}

	/**
	 * format ModifiedTrees, which is the flow of curerntScope operation in
	 * JavaScript program, for F# code generation
	 **/
	private void formatTree(FSharpScope currentScope) {
		ModifiableTree node = currentScope.node;
		if (node.size() > 0) {
			for (int i = 0; i < node.size(); i++) {
				formatTree(currentScope, node.get(i));
			}
		}
		// TODO
	}

	private boolean findFieldAssignStmt(ModifiableTree node) {
		// case: object.property = value;
		if (node.size() > 0) {
			ModifiableTree nameNode = node.get(0);
			if (node.is(JSTag.TAG_ASSIGN) && nameNode.is(JSTag.TAG_FIELD)) {
				ArrayList<String> field = getFieldElements(nameNode);
				for (String element : field) {
					if (element == "prototype") {

					}
				}
				return true;
			}
		}
		return true;
	}

	private void formatField(ModifiableTree node, FSharpScope currentScope) {
		ModifiableTree fieldNode = node;
		FSharpScope classScope = null;
		ArrayList<String> fieldElements = new ArrayList<String>();
		int elementNumFromRight = 0;
		while (fieldNode.is(JSTag.TAG_FIELD) || fieldNode.is(JSTag.TAG_APPLY)
				&& classScope == null) {
			if (fieldNode.is(JSTag.TAG_FIELD)) {
				elementNumFromRight++;
				fieldElements.add(fieldNode.get(1).getText());
				classScope = searchScopeByFuncOrVarName(fieldNode.get(1)
						.getText(), currentScope);
			}
			fieldNode = fieldNode.get(0);
		}
		if (classScope != null && elementNumFromRight < 1) {
			ModifiableTree fixedNode = new ModifiableTree(JSTag.TAG_FIELD,
					node.getSource(), node.getSourcePosition(),
					node.getSourcePosition() + node.getLength(), 2, "");
			ModifiableTree newNode = new ModifiableTree(JSTag.TAG_NEW,
					node.getSource(), node.getSourcePosition(),
					node.getSourcePosition() + node.getLength(), 2, "new");
			ModifiableTree constructorNode = new ModifiableTree(JSTag.TAG_NAME,
					node.getSource(), node.getSourcePosition(), 0,
					classScope.getScopeName());
			ModifiableTree argsNode = new ModifiableTree(JSTag.TAG_LIST,
					node.getSource(), node.getSourcePosition(), 0, null);
			ArrayList<String> fieldStrings = getFieldElements(node);
			ModifiableTree callNode = new ModifiableTree(JSTag.TAG_NAME,
					node.getSource(), node.getSourcePosition(), 0,
					fieldStrings.get(fieldStrings.size() - 1));
			fixedNode.set(0, newNode);
			fixedNode.set(1, callNode);
			newNode.set(0, constructorNode);
			newNode.set(1, argsNode);
			node.getParent().set(node.getIndexInParentNode(), fixedNode);
		} else if (classScope != null && elementNumFromRight >= 1) {
			for (int i = fieldElements.size() - elementNumFromRight - 1; i < fieldElements
					.size(); i++) {
				if (searchScopeByFuncOrVarName(fieldElements.get(i),
						currentScope) == null) {
					ModifiableTree fixedNode = new ModifiableTree(
							JSTag.TAG_APPLY, node.getSource(),
							node.getSourcePosition(), 2, "");
					ModifiableTree funcNode = new ModifiableTree(
							JSTag.TAG_NAME, node.getSource(),
							node.getSourcePosition(), 0, fieldElements.get(i));

				}
			}
		}
	}

	private FSharpScope searchScopeByFuncOrVarName(String name,
			FSharpScope currentScope) {
		FSharpScope result = null;
		// search variable and function from current scope
		for (FSharpFunc ff : currentScope.funcList) {
			if (ff.getTrueName() == name) {
				result = currentScope;
			}
		}
		for (FSharpVar fv : currentScope.varList) {
			if (fv.getTrueName() == name) {
				result = currentScope;
			}
		}
		// search it from parent scope
		if (result == null && currentScope.parent != null) {
			result = searchScopeByFuncOrVarName(name, currentScope.parent);
		}
		return result;
	}

	private ArrayList<String> getFieldElements(ModifiableTree node) {
		ModifiableTree fieldNode = node;
		ArrayList<String> fieldElements = new ArrayList<String>();
		while (fieldNode.is(JSTag.TAG_FIELD)) {
			fieldElements.add(fieldNode.getText());
			fieldNode = node.get(0);
		}
		fieldElements.add(fieldNode.getText());
		return fieldElements;
	};

	private boolean formatTree(FSharpScope currentScope, ModifiableTree node) {
		if (node.is(JSTag.TAG_FIELD)) {
			formatField(node, currentScope);
		}
		if (node.size() > 0) {
			for (int i = 0; i < node.size(); i++) {
				formatTree(currentScope, node.get(i));
			}
		}
		return true;
	}

	private void generateFSCode(FSharpScope currentScope) {
		// TODO
	}

	private void generatePrintCode() {
		// TODO
	}

	public void toSource(ModifiableTree node) {
		FSharpScope topScope = new FSharpScope("TOPLEVEL", node, null);
		fsClasses.add(topScope);
		findScope(node, topScope);
		findFieldAssignStmt(node);
		// print debug
		for (FSharpScope fs : fsClasses) {
			System.out.println(fs.toString());
		}
		//
		for (FSharpScope fsClass : fsClasses) {
			formatTree(fsClass);
		}
		for (FSharpScope fsClass : fsClasses) {
			generateFSCode(fsClass);
		}
		generatePrintCode();
	}

}
