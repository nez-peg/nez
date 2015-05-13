package nez.fsharp;

import java.util.ArrayList;
import java.util.List;

import nez.ast.CommonTree;
import nez.ast.Tag;


public class FSharpGenerator extends SourceGenerator {
	private static boolean UseExtend;
	public ArrayList<FSharpVar> varList = new ArrayList<FSharpVar>();
	public ArrayList<FSharpScope> scopeList = new ArrayList<FSharpScope>();
	private String prefixName = "";
	private int ifResultKey = 0;
	private int lambdaKey = 0;
	private boolean forFlag = false;
	private boolean objFlag = false;
	private boolean assignFlag= false;
	private boolean letFlag = false;
	private ArrayList<String> addedGetterList = new ArrayList<String>();
	private String forConunter = "";
	private FSharpScope currentScope;
	
	private final Tag TAG_LAMBDA_NAME = Tag.tag("LambdaName");

	public FSharpGenerator() {
		FSharpGenerator.UseExtend = false;
	}
	
	protected void initialSetting(CommonTree node){
		CommonTree target;
		FSharpScope topScope = new FSharpScope("TOPLEVEL", node, new ArrayList<String>());
		this.scopeList.add(topScope);
		for(int i = 0; i < node.size(); i++){
			target = node.get(i);
			if(target.is(JSTag.TAG_VAR_DECL_STMT)){
				
			}
			this.findScope(target, new ArrayList<String>(), topScope);
		}
		for(FSharpScope fs : this.scopeList){
			this.checkPrototype(node, fs);
		}
	}
	
	protected void findScope(CommonTree node, List<String> path, FSharpScope parentScope){
		boolean addScope = false;
		String scopeName = "";
		ArrayList<String> nextPath = (ArrayList<String>)path;
		boolean isFuncDecl = node.is(JSTag.TAG_FUNC_DECL);
		boolean isObject = node.is(JSTag.TAG_OBJECT);
		FSharpScope fs = parentScope;
		List<String> prototypePath = null;
		
		if(isFuncDecl){
			addScope = true;
			scopeName = node.get(2).getText();
			if(scopeName.isEmpty()){
				//sNode = superNode
				CommonTree sNode = node.getParent();
				CommonTree ssNode = sNode.getParent();
				CommonTree sssNode = ssNode.getParent();
				if(sNode.is(JSTag.TAG_ASSIGN)){
					scopeName = sNode.get(0).get(1).getText();
					prototypePath = new ArrayList<String>();
					for(String pathElement : this.getFullFieldText(sNode.get(0), fs).split("_")){
						prototypePath.add(pathElement);
					}
					FSharpScope prototypeScope = this.searchScopeFromList(prototypePath.get(0));
					nextPath = new ArrayList<String>();
					if(prototypeScope != null){
						nextPath.addAll(prototypeScope.path);
					}
					for(int i = 0; i < prototypePath.size() - 1; i++){
						nextPath.add(prototypePath.get(i));
					}
				} else if(sNode.is(JSTag.TAG_APPLY)){
					scopeName = ssNode.get(0).getText();
				} else if(sssNode.is(JSTag.TAG_VAR_DECL_STMT)){
					scopeName = sNode.get(0).getText();
				} else if(sNode.is(JSTag.TAG_PROPERTY)) {
					scopeName = sNode.get(0).getText();
				} else {
					scopeName = "lambda" + this.lambdaKey++;
					CommonTree nameNode = new CommonTree(new Tag("Name"), null, 0);
					CommonTree parent = node.getParent();
//					int i = 0;
//					for(String pathElement : path){
//						nameNode.add(new CommonTree(new Tag("Name"), null, 0));
//						nameNode.get(i).setValue(pathElement);
//						i++;
//					}
//					nameNode.add(new CommonTree(new Tag("Name"), null, 0));
					nameNode.setValue(scopeName);
					parent.insert(this.indexOf(node), nameNode);
					parent.remove(this.indexOf(node));
				}
				node.get(2).setValue(scopeName);
			} else if(node.get(2).is(this.TAG_LAMBDA_NAME)){
				node.get(2).setTag(new Tag("Name"));
				CommonTree nameNode = new CommonTree(new Tag("Name"), null, 0);
				CommonTree parent = node.getParent();
				String fullName = "";
				String lambdaName = node.get(2).getText();
				for(String pathElement : path){
					fullName += pathElement + "_";
				}
				fullName += lambdaName;
				nameNode.setValue("(new ScopeOf" + fullName + "())." + lambdaName);
				parent.insert(this.indexOf(node), nameNode);
				parent.remove(this.indexOf(node));
			}
		} else if(isObject){
			addScope = true;
			CommonTree sNode = node.getParent();
			if(sNode.is(JSTag.TAG_ASSIGN) || sNode.is(JSTag.TAG_VAR_DECL)){
				scopeName = sNode.get(0).getText();
			} else {
				scopeName = "lambda" + this.lambdaKey++;
			}
		}
		if(addScope){
			if(prototypePath == null){
				fs = new FSharpScope(scopeName, node, (ArrayList<String>)path);
			} else {
				fs = new FSharpScope(scopeName, node, nextPath);
			}
			if(parentScope != null){
				fs.parent = parentScope;
				parentScope.add(fs);
			}
			this.scopeList.add(fs);
			
			if(isFuncDecl){
				CommonTree argsNode = node.get(4);
				FSharpVar argVar;
				for(int i = 0; i < argsNode.size(); i++){
					argVar = new FSharpVar(argsNode.get(i).getText(), fs.getPathName());
					fs.varList.add(argVar);
					this.varList.add(argVar);
					fs.numOfArgs++;
				}
				this.findReturn(node.get(6), fs, false);
			} else if(isObject){
				CommonTree objNode = new CommonTree(new Tag("ObjectName"), null, 0);
				objNode.setValue("new " + fs.getScopeName()+"()");
				node.getParent().insert(this.indexOf(node), objNode);
				node.getParent().remove(node);
			}
			
			if(prototypePath == null){
				nextPath = new ArrayList<String>();
				//deep copy path -> cpath
				for(String pathElement : path){
					nextPath.add(pathElement);
				}
				nextPath.add(scopeName);
			}
			if(isFuncDecl){
				this.checkVarDecl(node.get(6), fs);
			} else if(isObject){
				this.checkProperty(node, fs);
			}
		}
		if(node.size() > 0){
			for(int i = 0; i < node.size(); i++){
				this.findScope(node.get(i), nextPath, fs);
			}
		}
	}
	
	private CommonTree lastNodeOfField(CommonTree node){
		if(node.getParent().is(JSTag.TAG_FIELD)){
			return this.lastNodeOfField(node.getParent());
		} else if(node.is(JSTag.TAG_FIELD)){
			return node.get(1);
		}
		return null;
	}
	
	private void checkPrototype(CommonTree node, FSharpScope fs){
		CommonTree parent = node.getParent();
		if(node.is(JSTag.TAG_NAME) && parent.is(JSTag.TAG_FIELD) && node.getText().contentEquals("prototype")){
			int node_i = this.indexOf(node);
			if(0 < node_i){
				if(parent.get(node_i - 1).getText().contentEquals(fs.name)){
					CommonTree pNameNode = this.lastNodeOfField(node);
					String prototypeName = pNameNode.getText();
					CommonTree valueNode = pNameNode.getParent().getParent().get(1);
					if(valueNode.is(JSTag.TAG_FUNC_DECL)){
						valueNode.get(2).setValue(prototypeName);
					}
					fs.funcList.add(new FSharpFunc(prototypeName, fs.getPathName(), false, valueNode));
				}
			}
		}
		for(int i = 0; i < node.size(); i++){
			checkPrototype(node.get(i), fs);
		}
	}
	
	public void visitObjectName(CommonTree node){
		this.currentBuilder.append(node.getText());
	}
	
	private int indexOf(CommonTree node){
		CommonTree parent = node.getParent();
		for(int i = 0; i < parent.size(); i++){
			if(parent.get(i) == node){
				return i;
			}
		}
		return -1;
	}
	
	protected void checkProperty(CommonTree node, FSharpScope fs){
		CommonTree propertyNode;
		FSharpVar fv;
		FSharpFunc ff;
		for(int i = 0; i < node.size(); i++){
			propertyNode = node.get(i);
			if(!propertyNode.get(1).is(JSTag.TAG_FUNC_DECL)){
				fv = new FSharpVar(propertyNode.get(0).getText(), fs.getPathName(), propertyNode.get(1));
				fs.varList.add(fv);
				this.varList.add(fv);
			} else {
				ff = new FSharpFunc(propertyNode.get(0).getText(), fs.getPathName(), false, propertyNode.get(1));
				fs.funcList.add(ff);
			}
		}
	}
	
	protected boolean checkVarDecl(CommonTree node, FSharpScope fs){
		if(node.is(JSTag.TAG_VAR_DECL_STMT)){
			CommonTree listNode = node.get(2);
			CommonTree varDeclNode;
			for(int i = 0; i < listNode.size(); i++){
				varDeclNode = listNode.get(i);
				try{
					if(!varDeclNode.get(1).is(JSTag.TAG_FUNC_DECL)){
						FSharpVar fv = new FSharpVar(varDeclNode.get(0).getText(), fs.getPathName());
						this.varList.add(fv);
						fs.varList.add(fv);
						varDeclNode.setTag(new Tag("Assign"));
						node.getParent().insert(this.indexOf(node) + i, varDeclNode);
					} else {
						FSharpFunc ff = new FSharpFunc(varDeclNode.get(0).getText(), fs.getPathName(), false, varDeclNode.get(1));
						fs.funcList.add(ff);
						return false;
					}
				} catch(ArrayIndexOutOfBoundsException e){
					return false;
				}
			}
			node.getParent().remove(this.indexOf(node));
		} else
		if(node.is(JSTag.TAG_FUNC_DECL)){
			CommonTree nameNode = node.get(2);
			if(nameNode.getText().isEmpty()){
				nameNode.setValue("lambda" + this.lambdaKey++);
				nameNode.setTag(new Tag("LambdaName"));
			}
			FSharpFunc ff = new FSharpFunc(nameNode.getText(), fs.getPathName(), false, node);
			fs.funcList.add(ff);
			return false;
		} else
		if(node.is(JSTag.TAG_ASSIGN)){
			if(node.get(0).is(JSTag.TAG_NAME)){
				String varName = this.getFieldText(node.get(0));
				boolean isExist = false;
				for(FSharpVar searchTarget : fs.varList){
					if(searchTarget.name.contentEquals(varName)){
						isExist = true;
					}
				}
				if(!isExist){
					FSharpVar fv = new FSharpVar(varName, fs.getPathName());
					this.varList.add(fv);
					fs.varList.add(fv);
				}
			}
		}
		if(node.size() > 0){
			for(int i = 0; i < node.size(); i++){
				checkVarDecl(node.get(i), fs);
			}
		}
		return true;
	}
	
	protected String typeCode(FSharpScope fs){
		String name = fs.getScopeName();
		String pathString = "";
		for(String pathElement : fs.path){
			pathString += this.currentBuilder.quoteString + pathElement + this.currentBuilder.quoteString + ";";
		}
		this.currentBuilder.appendNewLine("let " + name + "0 = " + "new " + name + "()");
		String printStr = "fsLib.fl.printObject " + this.currentBuilder.quoteString + name + this.currentBuilder.quoteString + ((fs.type==fs.type.OBJECT)? " true":" false") + " [|"+ pathString + "|] (" + name + "0.GetType().GetMethods())";
		return printStr;
	}
	
	protected String typeCode(FSharpFunc ffunc){
		String printStr = "printfn " + this.currentBuilder.quoteString + "%s" + this.currentBuilder.quoteString;
		String argBeginStr = " (" + this.currentBuilder.quoteString + ffunc.getFullname() + " : " + this.currentBuilder.quoteString + " + ";
		String argStr = "";
		if(ffunc.isMember){
			argStr = ffunc.getFullname() + ".GetType().ToString()";
		} else {
			argStr = ffunc.getFullname() + ".GetType().GetMethods().[0].ToString()";
		}
		String argEndStr = ")";
		return printStr + argBeginStr + argStr + argEndStr;
	}
	
	protected String typeCode(FSharpVar fvar){
		String printStr = "printfn " + this.currentBuilder.quoteString + "%s" + this.currentBuilder.quoteString;
		String argBeginStr = " (" + this.currentBuilder.quoteString + fvar.getTrueName() + " : " + this.currentBuilder.quoteString;
		String argStr = "";
		for(int i = 0; i <= fvar.uniqueKey; i++){
			argStr += " + " + fvar.getFullname() + i + ".GetType().ToString()";
		}
		String argEndStr = ")";
		return printStr + argBeginStr + argStr + argEndStr;
	}
	
	protected void generateTypeCode(){
		for(FSharpScope fs : this.scopeList){
			this.currentBuilder.appendNewLine(typeCode(fs));
		}
	}
	
	protected boolean checkReturn(CommonTree node, boolean result){
		if(result){
			return true;
		}
		result = node.is(JSTag.TAG_RETURN);
		if(node.size() >= 1 && !result) {
			for(int i = 0; i < node.size(); i++){
				result = checkReturn(node.get(i), result);
			}
		}
		return result;
	}
	
	protected boolean findReturn(CommonTree node, FSharpScope fs, boolean result){
		boolean res = false;
		if(result){
			res = true;
		}
		if(node.is(JSTag.TAG_RETURN)){
			res = true;
			fs.returnList.add(node);
		}
		if(node.size() >= 1 && !node.is(JSTag.TAG_FUNC_DECL)) {
			for(int i = 0; i < node.size(); i++){
				res = findReturn(node.get(i), fs, res);
			}
		}
		return res;
	}
	
	protected void checkAssignVarName(CommonTree node, FSharpVar targetVar){
		if(node.size() < 1){
			if(targetVar.getTrueName().contentEquals(node.getText())){
				node.setValue(targetVar.getCurrentName());
			}
		} else {
			for(int i = 0; i < node.size(); i++){
				if(node.get(i).size() == 0){
					if(targetVar.getTrueName().contentEquals(node.get(i).getText())){
						node.get(i).setValue(targetVar.getCurrentName());
					}
				} else {
					checkAssignVarName(node.get(i), targetVar);
				}
			}
		}
	}
	
	protected boolean checkApplyFuncName(String funcName){
		for(FSharpScope target : this.scopeList){
			if(target.getPathName().contentEquals(funcName + ".")){
				return true;
			}
			if(funcName.startsWith("_") && target.getPathName().contentEquals(funcName.substring(1) + ".")){
				return true;
			}
			if(target.getPathName().contentEquals(this.prefixName + funcName + ".")){
				return true;
			}
			if(funcName.startsWith("_") && target.getPathName().contentEquals(this.prefixName + funcName.substring(1) + ".")){
				return true;
			}
		}
		return false;
	}
	
	protected FSharpVar searchVarFromList(String varName, boolean fieldFlag){
		String prefixName = this.prefixName;
		String[] prefixNameElements = prefixName.split(".");
		if(prefixNameElements.length == 0){
			int num = prefixName.indexOf(".");			
			if(num > 0 && prefixName.length() - 1 > num){
				prefixNameElements = new String[2];
				prefixNameElements[0] = prefixName.substring(0, num);
				prefixNameElements[1] = prefixName.substring(num + 1, prefixName.length() - 1);
			} else {
				prefixNameElements = new String[1];
				prefixNameElements[0] = prefixName.substring(0, prefixName.length() - 1);
			}
		}
		for(FSharpVar element : varList){
			if(element.getFullname().contentEquals(prefixName + varName)){
				return element;
			} else if(element.getFullname().contentEquals(varName) && fieldFlag){
				return element;
			}
		}
		if(prefixNameElements != null){
			for(int i = prefixNameElements.length - 1; i >= 0; i--){
				if(prefixName.length() > 0){
					prefixName = prefixName.substring(0, prefixName.length() - (prefixNameElements[i].length() + 1));
				}
				for(FSharpVar element : varList){
					if(element.getFullname().contentEquals(prefixName + varName)){
						return element;
					} else if(element.getFullname().contentEquals(varName) && fieldFlag){
						return element;
					}
				}
			}
		}
		return null;
	}
	
	private String getFullFieldText(CommonTree node, FSharpScope fs){
		if(node.is(JSTag.TAG_FIELD)){
			if(node.get(1).getText().contentEquals("prototype")){
				return this.getFullFieldText(node.get(0), fs);
			} else {
				return this.getFullFieldText(node.get(0), fs) + "_" + node.get(1).getText();
			}
		}
		return node.getText();
	}
	
	protected String getFieldText(CommonTree node){
		String result = "";
		if(node.is(JSTag.TAG_FIELD)){
			for(int i = 0; i < node.size(); i++){
				result += node.get(i).getText();
				if(i < node.size() - 1){
					result += ".";
				}
			}
		} else if(node.is(JSTag.TAG_NAME)){
			result += node.getText();
		}
		return result;
	}
	
	protected void setVarNameInBinary(CommonTree node, boolean isAssign){
		String varName = this.getFieldText(node.get(0));
		FSharpVar targetVar = searchVarFromList(varName, node.get(0).is(JSTag.TAG_FIELD));
		if(targetVar == null && isAssign){
			this.varList.add(new FSharpVar(varName, this.prefixName));
			targetVar = this.varList.get(this.varList.size()-1);
			checkAssignVarName(node.get(1), targetVar);
			targetVar.addChild();
		}
		varName = targetVar.getCurrentName();
		node.get(0).setValue(varName);
	}

/*
	public void visitThrow(CommonTree node) {
		this.currentBuilder.append("throw ");
		this.visit(node.get(0));
	}
	*/
	
	protected int getOperatorPrecedence(Tag tagId){
		if(tagId == JSTag.TAG_INTEGER) return 0;
		if(tagId == JSTag.TAG_BINARY_INTEGER) return 0;
		if(tagId == JSTag.TAG_OCTAL_INTEGER) return 0;
		if(tagId == JSTag.TAG_HEX_INTEGER) return 0;
		if(tagId == JSTag.TAG_LONG) return 0;
		if(tagId == JSTag.TAG_BINARY_LONG) return 0;
		if(tagId == JSTag.TAG_OCTAL_LONG) return 0;
		if(tagId == JSTag.TAG_HEX_LONG) return 0;
		if(tagId == JSTag.TAG_FLOAT) return 0;
		if(tagId == JSTag.TAG_HEX_FLOAT) return 0;
		if(tagId == JSTag.TAG_DOUBLE) return 0;
		if(tagId == JSTag.TAG_HEX_DOUBLE) return 0;
		if(tagId == JSTag.TAG_STRING) return 0;
		if(tagId == JSTag.TAG_REGULAR_EXP) return 0;
		if(tagId == JSTag.TAG_NULL) return 0;
		if(tagId == JSTag.TAG_TRUE) return 0;
		if(tagId == JSTag.TAG_FALSE) return 0;
		if(tagId == JSTag.TAG_THIS) return 0;
		if(tagId == JSTag.TAG_SUPER) return 0;
		if(tagId == JSTag.TAG_NAME) return 0;
		if(tagId == JSTag.TAG_ARRAY) return 0;
		if(tagId == JSTag.TAG_HASH) return 0;
		if(tagId == JSTag.TAG_TYPE) return 0;
		if(tagId == JSTag.TAG_SUFFIX_INC) return 2;
		if(tagId == JSTag.TAG_SUFFIX_DEC) return 2;
		if(tagId == JSTag.TAG_PREFIX_INC) return 2;
		if(tagId == JSTag.TAG_PREFIX_DEC) return 2;
		if(tagId == JSTag.TAG_PLUS) return 4;
		if(tagId == JSTag.TAG_MINUS) return 4;
		if(tagId == JSTag.TAG_COMPL) return 4;
		if(tagId == JSTag.TAG_ADD) return 6;
		if(tagId == JSTag.TAG_SUB) return 6;
		if(tagId == JSTag.TAG_MUL) return 5;
		if(tagId == JSTag.TAG_DIV) return 5;
		if(tagId == JSTag.TAG_MOD) return 5;
		if(tagId == JSTag.TAG_LEFT_SHIFT) return 7;
		if(tagId == JSTag.TAG_RIGHT_SHIFT) return 7;
		if(tagId == JSTag.TAG_LOGICAL_LEFT_SHIFT) return 7;
		if(tagId == JSTag.TAG_LOGICAL_RIGHT_SHIFT) return 7;
		if(tagId == JSTag.TAG_GREATER_THAN) return 8;
		if(tagId == JSTag.TAG_GREATER_THAN_EQUALS) return 8;
		if(tagId == JSTag.TAG_LESS_THAN) return 8;
		if(tagId == JSTag.TAG_LESS_THAN_EQUALS) return 8;
		if(tagId == JSTag.TAG_EQUALS) return 9;
		if(tagId == JSTag.TAG_NOT_EQUALS) return 9;
		if(tagId == JSTag.TAG_STRICT_EQUALS) return 9;
		if(tagId == JSTag.TAG_STRICT_NOT_EQUALS) return 9;
		//if(tagId == JSTag.TAG_COMPARE) return 9;
		//if(tagId == JSTag.TAG_INSTANCE_OF) return 8;
		if(tagId == JSTag.TAG_STRING_INSTANCE_OF) return 8;
		if(tagId == JSTag.TAG_HASH_IN) return 8;
		if(tagId == JSTag.TAG_BITWISE_AND) return 10;
		if(tagId == JSTag.TAG_BITWISE_OR) return 12;
		if(tagId == JSTag.TAG_BITWISE_NOT) return 4;
		if(tagId == JSTag.TAG_BITWISE_XOR) return 11;
		if(tagId == JSTag.TAG_LOGICAL_AND) return 13;
		if(tagId == JSTag.TAG_LOGICAL_OR) return 14;
		if(tagId == JSTag.TAG_LOGICAL_NOT) return 4;
		//if(tagId == JSTag.TAG_LOGICAL_XOR) return 14;
		if(tagId == JSTag.TAG_CONDITIONAL) return 16;
		if(tagId == JSTag.TAG_ASSIGN) return 17;
		if(tagId == JSTag.TAG_ASSIGN_ADD) return 17;
		if(tagId == JSTag.TAG_ASSIGN_SUB) return 17;
		if(tagId == JSTag.TAG_ASSIGN_MUL) return 17;
		if(tagId == JSTag.TAG_ASSIGN_DIV) return 17;
		if(tagId == JSTag.TAG_ASSIGN_MOD) return 17;
		if(tagId == JSTag.TAG_ASSIGN_LEFT_SHIFT) return 17;
		if(tagId == JSTag.TAG_ASSIGN_RIGHT_SHIFT) return 17;
		if(tagId == JSTag.TAG_ASSIGN_LOGICAL_LEFT_SHIFT) return 17;
		if(tagId == JSTag.TAG_ASSIGN_LOGICAL_RIGHT_SHIFT) return 17;
		if(tagId == JSTag.TAG_ASSIGN_BITWISE_AND) return 17;
		if(tagId == JSTag.TAG_ASSIGN_BITWISE_OR) return 17;
		if(tagId == JSTag.TAG_ASSIGN_BITWISE_XOR) return 17;
		//if(tagId == JSTag.TAG_ASSIGN_LOGICAL_AND) return 0;
		//if(tagId == JSTag.TAG_ASSIGN_LOGICAL_OR) return 0;
		//if(tagId == JSTag.TAG_ASSIGN_LOGICAL_XOR) return 0;
		//if(tagId == JSTag.TAG_MULTIPLE_ASSIGN) return 0;
		if(tagId == JSTag.TAG_COMMA) return 18;
		//if(tagId == JSTag.TAG_CONCAT) return 4;
		if(tagId == JSTag.TAG_FIELD) return 1;
		if(tagId == JSTag.TAG_INDEX) return 1;
		if(tagId == JSTag.TAG_MULTI_INDEX) return 1;
		if(tagId == JSTag.TAG_APPLY) return 2;
		if(tagId == JSTag.TAG_METHOD) return 2;
		if(tagId == JSTag.TAG_TYPE_OF) return 2;
		if(tagId == JSTag.TAG_NEW) return 1;
		return Integer.MAX_VALUE;
	}
	
	protected boolean shouldExpressionBeWrapped(CommonTree node){
		int precedence = getOperatorPrecedence(node.getTag().getId());
		if(precedence == 0){
			return false;
		}else{
			CommonTree parent = node.getParent();
			if(parent != null && getOperatorPrecedence(parent.getTag().getId()) >= precedence){
				return false;
			}
		} 
		return true;
	}
	
	protected void generateExpression(CommonTree node){
		if(this.shouldExpressionBeWrapped(node)){
			this.visit(node, '(', ')');
		}else{
			this.visit(node);
		}
	}
	
	protected void genaratePrefixUnary(CommonTree node, String operator){
		this.currentBuilder.append(operator);
		generateExpression(node.get(0));
	}

	protected void genarateSuffixUnary(CommonTree node, String operator){
		generateExpression(node.get(0));
		this.currentBuilder.append(operator);
	}

	protected void generateBinary(CommonTree node, String operator){
		if(!this.assignFlag){
			this.formatRightSide(node.get(0));
			this.formatRightSide(node.get(1));
		}
		generateExpression(node.get(0));
		this.currentBuilder.append(operator);
		generateExpression(node.get(1));
	}
	
	protected void generateTrinary(CommonTree node, String operator1, String operator2){
		generateExpression(node.get(0));
		this.currentBuilder.append(operator1);
		generateExpression(node.get(1));
		this.currentBuilder.append(operator2);
		generateExpression(node.get(2));
	}
	
	protected void generateTrinaryAddHead(CommonTree node, String operator1, String operator2, String operator3){
		this.currentBuilder.append(operator1);
		generateExpression(node.get(0));
		this.currentBuilder.append(operator2);
		generateExpression(node.get(1));
		this.currentBuilder.append(operator3);
		generateExpression(node.get(2));
	}
	
	protected void generateList(List<CommonTree> node, String delim){
		boolean isFirst = true;
		for(CommonTree element : node){
			if(!isFirst){
				this.currentBuilder.append(delim);
			}else{
				isFirst = false;
			}
			this.visit(element);
		}
	}
	
	protected void generateList(List<CommonTree> node, String begin, String delim, String end){
		this.currentBuilder.append(begin);
		this.generateList(node, delim);
		this.currentBuilder.append(end);
	}
	
	protected void generateClass(CommonTree node){
		String name = node.get(0).getText();
		this.objFlag = true;
		this.currentBuilder.appendNewLine("type ClassOf" + name + "(arg_for_object:int) = class");
		this.currentBuilder.indent();
		CommonTree objNode = node.get(1);
		CommonTree varDeclStmtNode = new CommonTree(new Tag("VarDeclStmt"), null, 0);
		varDeclStmtNode.set(0, new CommonTree(new Tag("Text"), null, 0));
		varDeclStmtNode.set(1, new CommonTree(new Tag("Text"), null, 0));
		varDeclStmtNode.set(2, new CommonTree(new Tag("List"), null, 0));
		this.prefixName += name + ".";
		String varName = "";
		for(int i = 0; i < objNode.size(); i++){
			this.currentBuilder.appendNewLine();
			varName = objNode.get(i).get(0).getText();
			objNode.get(i).setTag(new Tag("VarDecl"));
			objNode.get(i).get(0).setValue(varName + "0");
			varDeclStmtNode.get(2).set(0, objNode.get(i));
			this.visit(varDeclStmtNode);
			objNode.get(i).get(0).setValue(varName);
			this.objFlag = true;
		}
		for(String addedGetterName: this.addedGetterList){
			this.currentBuilder.appendNewLine(addedGetterName);
		}
		this.prefixName = this.prefixName.substring(0, this.prefixName.length() - (name + ".").length());
		this.currentBuilder.appendNewLine("end");
		this.currentBuilder.unIndent();
		this.varList.add(new FSharpVar(name, this.prefixName));
		this.currentBuilder.appendNewLine("let " + searchVarFromList(name, false).getCurrentName() + " = new ClassOf" + name + "(0)");
		this.objFlag = false;
	}
	
	protected void generateScope(FSharpScope fs, boolean isTopLevel){
		this.currentScope = fs;
		this.prefixName = fs.getPathName();
		String classType = isTopLevel? "type" : "and";
		this.currentBuilder.appendNewLine(classType + " " + fs.getScopeName() + " () = class");
		
		this.currentBuilder.indent();
		FSharpVar fv;

		if(fs.node.is(JSTag.TAG_FUNC_DECL)){
			CommonTree argsNode = fs.node.get(4);
			CommonTree arg;
			String argsStr = "";
			for(int i = 0; i < argsNode.size(); i++){
				arg = argsNode.get(i);
				argsStr += " " + arg.getText();
				this.currentBuilder.appendNewLine("let _" + arg.getText() + "_middle = ref None");
			}
			if(fs.recursive){
				this.currentBuilder.appendNewLine("let rec _" + fs.name + argsStr + " =");
			} else {
				this.currentBuilder.appendNewLine("member self." + fs.name + argsStr + " =");
			}
			this.currentBuilder.indent();
			for(int i = 0; i < argsNode.size(); i++){
				arg = argsNode.get(i);
				this.currentBuilder.appendNewLine("_" + arg.getText() + "_middle := " + "!" + arg.getText());
			}
			this.currentBuilder.unIndent();
			this.generateBlock(fs.node.get(6));
			this.currentBuilder.indent();
			if(fs.returnList.size() > 1) {
				this.assignFlag = true;
				CommonTree firstReturnNode = fs.returnList.get(0).get(0);
				for(int i = 1; i < fs.returnList.size(); i++){
					this.currentBuilder.appendNewLine();
					this.visit(firstReturnNode);
					this.visit(fs.returnList.get(i).get(0), " = ", "");
				}
				this.currentBuilder.appendNewLine();
				this.visit(firstReturnNode, "ref(", ")");
				this.assignFlag = false;
			}
			this.currentBuilder.unIndent();
			if(fs.recursive){
				this.currentBuilder.appendNewLine("member self." + fs.name + argsStr + " = _" + fs.name + argsStr);
			}
		}
//		for(FSharpScope child : fs.children){
//			this.currentBuilder.appendNewLine("member self." + child.getFullname() + "= new " + child.getScopeName() + "()");
//		}
//		FSharpScope parentScope = fs.parent;
//		while(parentScope != null){
//			this.currentBuilder.appendNewLine("member self." + parentScope.name + "= new " + parentScope.getScopeName() + "()");
//			parentScope = parentScope.parent;
//		}
		this.letFlag = true;
		for(int i = fs.numOfArgs; i < fs.varList.size(); i++){
			fv = fs.varList.get(i);
			if(fv.initialValue == null){
				this.currentBuilder.appendNewLine("member self." + fv.getTrueName() + " = ref None");
			} else {
				this.currentBuilder.appendNewLine("member self." + fv.getTrueName() + " = ref (Some(");
				CommonTree initParentNode = fv.initialValue.getParent();
				int initIndex = this.indexOf(fv.initialValue);
				this.formatRightSide(fv.initialValue);
				this.visit(initParentNode.get(initIndex));
				this.currentBuilder.append("))");
			}
		}
		this.letFlag = false;
		for(FSharpFunc ff : fs.funcList){
			this.currentBuilder.appendNewLine("member self." + ff.name + " " + ff.argsStr + " = (new ScopeOf" + fs.getFullname() + "_" + ff.name + "())." + ff.name + " " + ff.argsStr);
		}
//		for(int i = fs.numOfArgs; i < fs.varList.size(); i++){
//			fv = fs.varList.get(i);
//			this.currentBuilder.appendNewLine("member self.g_" + fv.getTrueName() + " = self." + fv.getTrueName());
//		}
		this.currentBuilder.appendNewLine("end");
		this.currentBuilder.unIndent();
		this.prefixName = "";
		this.currentScope = null;
	}
	
	public void visitSource(CommonTree node) {
		this.initialSetting(node);
		this.generateScope(this.scopeList.get(0), true);
		for(int i = 1; i < this.scopeList.size(); i++){
			this.generateScope(this.scopeList.get(i), false);
		}
		this.generateTypeCode();
	}
	
	public void visitName(CommonTree node) {
		String varName = node.getText();
		FSharpVar targetVar = searchVarFromList(varName, false);
		FSharpScope fs = null;
		if(targetVar != null){
			varName = targetVar.getCurrentName();
			for(FSharpScope targetScope : this.scopeList){
				for(FSharpVar fv : targetScope.varList){
					if(fv == targetVar && targetScope != this.currentScope){
						fs = targetScope;
						this.currentBuilder.append("(new " + fs.getScopeName() + "())." + node.getText());
						break;
					} else if(fv == targetVar && targetScope == this.currentScope && !this.currentScope.isArgumentVar(fv.getTrueName())){
						fs = targetScope;
						this.currentBuilder.append("self." + node.getText());
						break;
					}
				}
			}
		}
		if(fs == null){
			this.currentBuilder.append(node.getText());
		}
	}
	
	public void visitInteger(CommonTree node) {
		this.currentBuilder.append(node.getText() + ".0");
	}
	
	public void visitDecimalInteger(CommonTree node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void visitOctalInteger(CommonTree node) {
		this.currentBuilder.append(node.getText());
		if(node.getText().contentEquals("0") && !forFlag){
			this.currentBuilder.append(".0");
		}
	}
	
	public void visitHexInteger(CommonTree node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void visitLong(CommonTree node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void visitDecimalLong(CommonTree node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void visitOctalLong(CommonTree node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void visitHexLong(CommonTree node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void visitFloat(CommonTree node) {
		this.currentBuilder.append(node.getText());
	}

	public void visitDouble(CommonTree node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void visitHexFloat(CommonTree node) {
		this.currentBuilder.append(node.getText());
	}

	public void visitHexDouble(CommonTree node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void visitString(CommonTree node) {
		this.currentBuilder.appendChar('"');
		this.currentBuilder.append(node.getText());
		this.currentBuilder.appendChar('"');
	}
	
	public void visitRegularExp(CommonTree node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void visitText(CommonTree node) {
		/* do nothing */
	}
	
	public void visitThis(CommonTree node) {
		this.currentBuilder.append("this");
	}
	
	public void visitTrue(CommonTree node) {
		this.currentBuilder.append("true");
	}
	
	public void visitFalse(CommonTree node) {
		this.currentBuilder.append("false");
	}
	
	public void visitNull(CommonTree node) {
		this.currentBuilder.append("null");
	}
	
	public void visitList(CommonTree node) {
		generateList(node, ", ");
	}
	
	public void visitBlock(CommonTree node) {
		for(CommonTree element : node){
			this.currentBuilder.appendNewLine();
			this.visit(element);
		}
	}
	
	public void generateBlock(CommonTree node) {
		this.currentBuilder.indent();
		this.visit(node);
		if(!this.checkReturn(node, false)){
			this.currentBuilder.appendNewLine("ref(new fsLib.fl.Void(0))");
		}
		this.currentBuilder.unIndent();
	}
	
	public void visitArray(CommonTree node){
		this.currentBuilder.append("[|");
		this.generateList(node, "; ");
		this.currentBuilder.append("|]");
	}
	
	@Deprecated
	public void visitObject(CommonTree node){
		
		//this.generateClass(node.getParent());
	}
	
	public void visitProperty(CommonTree node) {
		this.generateBinary(node, ": ");
	}

	public void visitSuffixInc(CommonTree node) {
		//this.genarateSuffixUnary(node, "++");
	}

	public void visitSuffixDec(CommonTree node) {
		//this.genarateSuffixUnary(node, "--");
	}

	public void visitPrefixInc(CommonTree node) {
		//this.genaratePrefixUnary(node, "++");
	}

	public void visitPrefixDec(CommonTree node) {
		//this.genaratePrefixUnary(node, "--");
	}

	public void visitPlus(CommonTree node) {
		this.visit(node.get(0));
	}

	public void visitMinus(CommonTree node) {
		this.genaratePrefixUnary(node, "-");
	}

	public void visitAdd(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")+(");
		this.currentBuilder.append(")");
	}

	public void visitSub(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")-(");
		this.currentBuilder.append(")");
	}

	public void visitMul(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")*(");
		this.currentBuilder.append(")");
	}

	public void visitDiv(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")/(");
		this.currentBuilder.append(")");
	}

	public void visitMod(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")%(");
		this.currentBuilder.append(")");
	}

	public void visitLeftShift(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")<<<(");
		this.currentBuilder.append(")");
	}

	public void visitRightShift(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")>>>(");
		this.currentBuilder.append(")");
	}

	public void visitLogicalLeftShift(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")<<<(");
		this.currentBuilder.append(")");
	}

	public void visitLogicalRightShift(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")>>>(");
		this.currentBuilder.append(")");
	}

	public void visitGreaterThan(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")>(");
		this.currentBuilder.append(")");
	}

	public void visitGreaterThanEquals(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")>=(");
		this.currentBuilder.append(")");
	}

	public void visitLessThan(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")<(");
		this.currentBuilder.append(")");
	}

	public void visitLessThanEquals(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")<=(");
		this.currentBuilder.append(")");
	}

	public void visitEquals(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")=(");
		this.currentBuilder.append(")");
	}

	public void visitNotEquals(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")<>(");
		this.currentBuilder.append(")");
	}
	
	public void visitStrictEquals(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")=(");
		this.currentBuilder.append(")");
	}

	public void visitStrictNotEquals(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")<>(");
		this.currentBuilder.append(")");
	}

	//none
	public void visitCompare(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")-(");
		this.currentBuilder.append(")");
	}
	
	public void visitInstanceOf(CommonTree node) {
		this.currentBuilder.append("(");
		this.visit(node.get(0));
		this.currentBuilder.append(").constructor.name === ");
		this.visit(node.get(1));
		this.currentBuilder.append(".name");
	}
	
	public void visitStringInstanceOf(CommonTree node) {
		//this.generateBinary(node, " instanceof ");
	}
	
	public void visitHashIn(CommonTree node) {
		//this.generateBinary(node, " in ");
	}

	public void visitBitwiseAnd(CommonTree node) {
		this.generateBinary(node, "&&&");
	}

	public void visitBitwiseOr(CommonTree node) {
		this.generateBinary(node, "|||");
	}

	public void visitBitwiseNot(CommonTree node) {
		this.generateBinary(node, "not");
	}

	public void visitBitwiseXor(CommonTree node) {
		this.generateBinary(node, "^");
	}

	public void visitLogicalAnd(CommonTree node) {
		this.generateBinary(node, "&&");
	}

	public void visitLogicalOr(CommonTree node) {
		this.generateBinary(node, "||");
	}

	public void visitLogicalNot(CommonTree node) {
		this.genaratePrefixUnary(node, "not");
	}

	public void visitLogicalXor(CommonTree node) {
		this.generateBinary(node, "^^^");
	}

	public void visitConditional(CommonTree node) {
		this.generateTrinaryAddHead(node, "if", "then", "else");
	}
	
	protected FSharpScope searchScopeFromList(String name){
		String pathName;
		String prefixName = this.prefixName;
		String[] prefixNameElements = prefixName.split(".");
		if(prefixNameElements.length == 0){
			prefixNameElements = new String[1];
			prefixNameElements[0] = prefixName.substring(0, prefixName.length() - 1);
		}
		for(FSharpScope element : this.scopeList){
			pathName = element.getPathName();
			if(pathName.substring(0, pathName.length()-1).contentEquals(prefixName + name)){
				return element;
			}
		}
		if(prefixNameElements != null){
			for(int i = prefixNameElements.length - 1; i >= 0; i--){
				if(prefixName.length() > 0){
					prefixName = prefixName.substring(0, prefixName.length() - (prefixNameElements[i].length() + 1));
				}
				for(FSharpScope element : this.scopeList){
					pathName = element.getPathName();
					if(pathName.substring(0, pathName.length()-1).contentEquals(prefixName + name)){
						return element;
					}
				}
			}
		}
		return null;
	}
	
	protected void formatRightSide(CommonTree node){
		if(node.is(JSTag.TAG_APPLY)){
			this.formatRightSide(node.get(1));
		} else if(node.is(JSTag.TAG_FIELD)){
			String fieldValue = "(!(";
			FSharpScope fs;
			FSharpVar targetVar = null;
			for(int i = 0; i < node.size()-1; i++){
				fs = this.currentScope.getAvailableScope(node.get(i).getText());
				if(fs != null){
					fieldValue += "(new " + fs.getScopeName() + "()).";
					targetVar = fs.searchVar(node.get(node.size()-1).getText());
				}
				fs = null;
			}
			
			if(targetVar != null){
				fieldValue += targetVar.getTrueName() + ")).Value";
			} else {
				fieldValue += node.get(0).getText() + ")).Value";
			}
			CommonTree nameNode = new CommonTree(new Tag("Name"), null, 0);
			nameNode.setValue(fieldValue);
			node.getParent().insert(this.indexOf(node), nameNode);
			node.getParent().remove(node);
		} else if(node.is(JSTag.TAG_NAME)){
			String name = node.getText();
//			FSharpScope target = this.searchVarOrFuncFromScopeList(this.currentScope, name);
//			if(target == null){
//				node.setValue("(!(" + name + ")).Value");
//			} else {
//				node.setValue("(!(" + target.getPathName() + name + ")).Value");
//			}
			if(!node.getParent().is(JSTag.TAG_FIELD)){
				FSharpVar targetVar = searchVarFromList(name, false);
				FSharpScope fs = null;
				if(targetVar == null){
					targetVar = this.currentScope.getAvailableVar(name);
				}
				if(targetVar != null){
					name = targetVar.getTrueName();
					String trueName = name;
					for(FSharpScope targetScope : this.scopeList){
						for(FSharpVar fv : targetScope.varList){
							if(fv == targetVar && targetScope != this.currentScope){
								fs = targetScope;
								name = "(new " + fs.getScopeName() + "())." + name;
							}
						}
					}
					if(this.currentScope.searchVar(trueName) != null && !this.currentScope.isArgumentVar(trueName)){
						name = "self." + name;
					}
					node.setValue("(!(" + name + ")).Value");
				} else {
					FSharpScope tScope = this.currentScope.getAvailableScope(name);
					if(tScope != null){
						node.setValue("new " + tScope.getScopeName() + "()");
					}
				}
			}
		} else {
			for(int i = 0; i < node.size(); i++){
				this.formatRightSide(node.get(i));
			}
		}
	}
	
	protected FSharpScope searchVarOrFuncFromScopeList(FSharpScope targetScope, String targetName){
		ArrayList<String> scopePath = new ArrayList<String>();
		ArrayList<String> pathes = new ArrayList<String>();
		FSharpScope result = null;
		for(String element : targetScope.path){
			scopePath.add(element);
		}
		
		while(scopePath.size() > 0){
			pathes.add(scopePath.toString());
			scopePath.remove(scopePath.size() - 1);
		}
		pathes.add("[]");
		
		for(int scope_i = 0; scope_i < this.scopeList.size(); scope_i++){
			if(!pathes.isEmpty()){
				for(int path_i = 0; path_i < pathes.size(); path_i++){
					if(pathes.get(path_i).contentEquals(this.scopeList.get(scope_i).path.toString())){
						if(this.scopeList.get(scope_i).searchFunc(targetName) != null || this.scopeList.get(scope_i).searchVar(targetName) != null || this.scopeList.get(scope_i).name.contentEquals(targetName)){
							result = this.scopeList.get(scope_i);
						}
					}
				}
			} else {
				if(("[]").contentEquals(this.scopeList.get(scope_i).path.toString())){
					if(this.scopeList.get(scope_i).searchFunc(targetName) != null || this.scopeList.get(scope_i).searchVar(targetName) != null || this.scopeList.get(scope_i).name.contentEquals(targetName)){
						result = this.scopeList.get(scope_i);
					}
				}
			}
		}
		return result;
	}
	
	public void visitAssign(CommonTree node) {
		this.assignFlag = true;
		this.formatRightSide(node.get(1));
		//this.setVarNameInBinary(node, true);
		String varName = this.getFieldText(node.get(0));
		FSharpVar targetVar = searchVarFromList(varName, node.get(0).is(JSTag.TAG_FIELD));
		if(targetVar == null){
			this.varList.add(new FSharpVar(varName, this.prefixName));
			targetVar = this.varList.get(this.varList.size()-1);
			this.currentBuilder.append("let ");
		}
		//checkAssignVarName(node.get(1), targetVar);
		targetVar.addChild();
		
		this.generateBinary(node, " := Some(");
		this.currentBuilder.append(")");
		this.assignFlag = false;
	}

	public void visitMultiAssign(CommonTree node) {
		CommonTree lhs = node.get(0);
		CommonTree rhs = node.get(1);
		if(lhs.size() == 1 && rhs.size() == 1 && !rhs.get(0).is(JSTag.TAG_APPLY) && !rhs.get(0).is(JSTag.TAG_APPLY)){
			this.visit(lhs.get(0));
			this.currentBuilder.append(" = ");
			this.visit(rhs.get(0));
		}else{
			this.currentBuilder.append("multiAssign (");
			generateList(lhs, ", ");
			this.currentBuilder.append(") = (");
			generateList(rhs, ", ");
			this.currentBuilder.appendChar(')');
		}
	}
	
	private void generateAssignCalc(CommonTree node, String tagName){
		CommonTree rexpr = new CommonTree(new Tag(tagName), null, 0);
		CommonTree lexpr = node.get(0).dup();
		rexpr.add(lexpr);
		rexpr.add(node.get(1));
		node.setTag(new Tag("Assign"));
		node.set(1, rexpr);
		this.visitAssign(node);
	}
	
	public void visitAssignAdd(CommonTree node) {
		this.generateAssignCalc(node, "Add");
	}

	public void visitAssignSub(CommonTree node) {
		this.generateAssignCalc(node, "Sub");
	}

	public void visitAssignMul(CommonTree node) {
		this.generateAssignCalc(node, "Mul");
	}

	public void visitAssignDiv(CommonTree node) {
		this.generateAssignCalc(node, "Div");
	}

	public void visitAssignMod(CommonTree node) {
		this.generateAssignCalc(node, "Mod");
	}

	public void visitAssignLeftShift(CommonTree node) {
		this.generateBinary(node, "<<=");
	}

	public void visitAssignRightShift(CommonTree node) {
		this.generateBinary(node, ">>=");
	}

	public void visitAssignLogicalLeftShift(CommonTree node) {
		this.generateBinary(node, "<<<=");
	}

	public void visitAssignLogicalRightShift(CommonTree node) {
		this.generateBinary(node, ">>>=");
	}

	public void visitAssignBitwiseAnd(CommonTree node) {
		this.generateBinary(node, "&=");
	}

	public void visitAssignBitwiseOr(CommonTree node) {
		this.generateBinary(node, "|=");
	}

	public void visitAssignBitwiseXor(CommonTree node) {
		this.generateBinary(node, "^=");
	}

	public void visitAssignLogicalAnd(CommonTree node) {
		this.generateBinary(node, "&&=");
	}

	public void visitAssignLogicalOr(CommonTree node) {
		this.generateBinary(node, "||=");
	}

	public void visitAssignLogicalXor(CommonTree node) {
		this.generateBinary(node, "^=");
	}

//	public void visitMultipleAssign(CommonTree node) {
//		
//	}

	public void visitComma(CommonTree node) {
		if(node.size() > 2){
			this.currentBuilder.appendChar('(');
			this.generateList(node, ", ");
			this.currentBuilder.appendChar(')');	
		}else{
			this.generateBinary(node, ", ");
		}
	}
	
	//none
	public void visitConcat(CommonTree node) {
		this.generateBinary(node, " + ");
	}

	public void visitField(CommonTree node) {
		CommonTree field;
		FSharpScope fs;
		for(int i = 0; i < node.size()-1; i++){
			field = node.get(i);
			fs = this.searchScopeFromList(field.getText());
			if(fs != null){
				field.setValue(fs.getScopeName());
			}
			fs = null;
		}
		this.generateBinary(node, ".");
	}

	public void visitIndex(CommonTree node) {
		generateExpression(node.get(0));
		this.visit(node.get(1), ".[(int (", "))]");
	}

	//none
	public void visitMultiIndex(CommonTree node) {
		generateExpression(node.get(0));
		for(CommonTree indexNode : node.get(1)){
			this.visit(indexNode, '[', ']');
		}
	}
	
	private boolean containsVariadicValue(CommonTree list){
		for(CommonTree item : list){
			if(item.is(JSTag.TAG_VARIADIC_PARAMETER)
					|| item.is(JSTag.TAG_APPLY)
					|| item.is(JSTag.TAG_MULTIPLE_RETURN_APPLY)
					|| item.is(JSTag.TAG_METHOD)
					|| item.is(JSTag.TAG_MULTIPLE_RETURN_METHOD)){
				return true;
			}
		}
		return false;
	}
	
	protected void formatApplyFuncName(CommonTree node){
		if(node.is(JSTag.TAG_FIELD)){
			CommonTree field;
			FSharpScope fs;
			String fieldValue = "";
			CommonTree child = node.get(0);
			if(child.is(JSTag.TAG_APPLY)){
				this.formatApplyFuncName(child.get(0));
				this.formatRightSide(child.get(1));
				fieldValue += "(!(" + child.get(0).getText() + "(ref(Some(" + child.get(1).getText() + "))))).";
			} else {
				fs = this.searchScopeFromList(child.getText());
				if(fs != null){
					fieldValue += "(new " + fs.getScopeName() + "()).";
				}
				//field.setValue(fs.getScopeName());
			}
			this.formatRightSide(node.get(1));
			fieldValue += node.get(1).getText();
			CommonTree nameNode = new CommonTree(new Tag("Name"), null, 0);
			nameNode.setValue(fieldValue);
			node.getParent().insert(this.indexOf(node), nameNode);
			node.getParent().remove(node);
		} else if(node.is(JSTag.TAG_NAME)){
			FSharpScope fs;
			fs = this.searchVarOrFuncFromScopeList(this.currentScope, node.getText());
			fs = this.currentScope.getAvailableScope(node.getText());
			if(fs != null){
				node.setValue("(new " + fs.getScopeName() + "())" + "." + node.getText());
			}
		}
	}
	
	public void visitApply(CommonTree node) {
		CommonTree func = node.get(0);
		CommonTree arguments = node.get(1);
		if(this.assignFlag){
			this.currentBuilder.append("!(");
		}
		//if(this.checkApplyFuncName(getFieldText(func))){
		this.formatApplyFuncName(func);
		this.formatRightSide(arguments);
		func = node.get(0);
		boolean asFlag = this.assignFlag;
		this.assignFlag = true;
		this.visit(func);
		this.assignFlag = asFlag;
		if(arguments.size() > 0){
			this.currentBuilder.appendSpace();
			this.currentBuilder.append("(ref(Some(");
			this.generateList(arguments, "))) (ref(Some(");
			this.currentBuilder.append(")))");
		}
		//}
		if(this.assignFlag){
			this.currentBuilder.append(")");
		}
	}

	//none
	public void visitMethod(CommonTree node) {
		this.generateBinary(node, ".");
		this.currentBuilder.appendChar('(');
		this.generateList(node.get(2), ", ");
		this.currentBuilder.appendChar(')');
	}
	
	public void visitTypeOf(CommonTree node) {
		this.currentBuilder.append("(");
		generateExpression(node.get(0));
		this.currentBuilder.append(")");
		this.currentBuilder.append(".GetType().GetMethod().[0].toString()");
	}

	public void visitIf(CommonTree node) {
		String thenBlock;
		this.assignFlag = true;
		this.visit(node.get(0), "if (", ")");
		this.assignFlag = false;
		this.currentBuilder.appendNewLine("then");
		int start = this.currentBuilder.getPosition();
		this.generateBlock(node.get(1));
		if(this.checkReturn(node.get(1), false)){
			if(!isNullOrEmpty(node, 2)){
				CommonTree elseBlock = node.get(2);
				if(!this.checkReturn(elseBlock, false)){
					CommonTree parent = node.getParent();
					CommonTree element;
					long currentPosition = node.getSourcePosition();
					for(int i = 0; i < parent.size(); i++){
						element = parent.get(i);
						if(currentPosition < element.getSourcePosition()){
							elseBlock.add(element);
							parent.remove(i);
						}
					}
				}
			} else {
				CommonTree elseBlock = new CommonTree(new Tag("Block"), null, 0);
				node.set(2, elseBlock);
				CommonTree parent = node.getParent();
				CommonTree element;
				long currentPosition = node.getSourcePosition();
				for(int i = 0; i < parent.size(); i++){
					element = parent.get(i);
					if(currentPosition < element.getSourcePosition()){
						elseBlock.add(element);
						parent.remove(i);
					}
				}
				if(elseBlock.size() < 1){
					node.remove(2);
				}
			}
		}
		thenBlock = this.currentBuilder.substring(start, this.currentBuilder.getPosition());
		if(!isNullOrEmpty(node, 2)){
			this.currentBuilder.appendNewLine("else");
			this.generateBlock(node.get(2));
		} else {
			this.currentBuilder.appendNewLine("else");
			this.currentBuilder.append(thenBlock);
		}
	}

	public void visitWhile(CommonTree node) {
		int begin, end;
		String thenBlock;
		this.visit(node.get(0), "if ", "");
		this.currentBuilder.appendNewLine("then");
		begin = this.currentBuilder.getPosition();
		this.generateBlock(node.get(1));
		end = this.currentBuilder.getPosition();
		this.currentBuilder.appendNewLine("else");
		this.currentBuilder.append(this.currentBuilder.substring(begin, end));
//		this.currentBuilder.indent();
//		this.currentBuilder.append("printfn " + this.currentBuilder.quoteString + "dammy" + this.currentBuilder.quoteString);
//		this.currentBuilder.appendNewLine("done");
//		this.currentBuilder.unIndent();
	}

	public void visitFor(CommonTree node) {
		int begin, end;
		String thenBlock;
		this.forFlag = true;
		CommonTree exp1 = node.get(0).get(0);
		this.currentBuilder.append("for ");
		if(exp1.is(JSTag.TAG_VAR_DECL)){
			this.currentBuilder.append(exp1.get(0).getText());
			this.forConunter = exp1.get(0).getText();
			this.formatForCounter(node.get(1));
			this.formatForCounter(node.get(3));
		}
		
		this.forFlag = false;
		this.currentBuilder.append("=0 to 1 do");
		this.currentBuilder.indent();
		this.currentBuilder.appendNewLine();
		this.visit(node.get(1), "if (", ") ");
		this.currentBuilder.appendNewLine("then");
		begin = this.currentBuilder.getPosition();
		this.generateBlock(node.get(3));
		end = this.currentBuilder.getPosition();
		thenBlock = this.currentBuilder.substring(begin, end);
		this.currentBuilder.appendNewLine("else");
		this.currentBuilder.indent();
		this.currentBuilder.append(thenBlock);
		this.currentBuilder.unIndent();
		this.currentBuilder.unIndent();
		this.currentBuilder.appendNewLine("done");	
		this.forConunter = "";
	}
	
	public void visitForCounter(CommonTree node){
		this.currentBuilder.append(node.getText());
	}
	
	protected void formatForCounter(CommonTree node){
		if(node.is(JSTag.TAG_NAME)){
			if(node.getText().contentEquals(this.forConunter)){
				node.setValue("double " + this.forConunter);
				node.setTag(new Tag("ForCounter"));
			}
		} else {
			for(int i = 0; i < node.size(); i++){
				this.formatForCounter(node.get(i));
			}
		}
	}
	
	public void visitJSForeach(CommonTree node) {
		CommonTree param1 = node.get(0);
		if(param1.is(JSTag.TAG_LIST)){
			this.currentBuilder.append("for " + param1.get(0).get(0).getText() + " in ");
		} else if(param1.is(JSTag.TAG_NAME)){
			this.currentBuilder.append("for " + param1.getText() + " in ");
		}
		this.visit(node.get(1));
		this.currentBuilder.append(" do");
		this.generateBlock(node.get(2));
		this.currentBuilder.appendNewLine(this.currentBuilder.indentString + "done");
		if(!isNullOrEmpty(node, 3)){
			this.currentBuilder.appendNewLine();
			this.visit(node.get(3));
		}
	}

	public void visitDoWhile(CommonTree node) {
		this.currentBuilder.append("do");
		this.generateBlock(node.get(0));
		this.visit(node.get(1), "while (", ")");
	}
	
	protected void generateJump(CommonTree node, String keyword){
		if(!isNullOrEmpty(node, 0)){
			this.currentBuilder.appendSpace();
			CommonTree returnValue = node.get(0);
			if(returnValue.is(JSTag.TAG_LIST)){
				this.currentBuilder.append("multiple m");
				this.currentBuilder.append(keyword);
				this.generateList(returnValue, " (", ", ", ")");
			}else{
				this.currentBuilder.append(keyword);
				this.visit(returnValue);
			}
		}
	}

	public void visitReturn(CommonTree node) {
		this.assignFlag = true;
		this.formatRightSide(node.get(0));
		this.visit(node.get(0), "ref(", ")");
		this.assignFlag = false;
	}

	public void visitBreak(CommonTree node) {
		//this.generateJump(node, "break");
	}

	public void visitYield(CommonTree node) {
		//this.generateJump(node, "yield");
	}

	public void visitContinue(CommonTree node) {
		//this.generateJump(node, "continue");
	}

	public void visitRedo(CommonTree node) {
		this.generateJump(node, "/*redo*/");
	}

	public void visitSwitch(CommonTree node) {
		this.visit(node.get(0), "match ", " with");
		
		this.currentBuilder.indent();
		
		for(CommonTree element : node.get(1)){
			this.currentBuilder.appendNewLine();
			this.visit(element);
		}
		
		this.currentBuilder.unIndent();
	}

	public void visitCase(CommonTree node) {
		this.visit(node.get(0), "| ", "->");
		this.currentBuilder.indent();
		this.currentBuilder.appendNewLine();
		if(!isNullOrEmpty(node, 1)){
			this.visit(node.get(1));
		}
		if(this.checkReturn(node, false)){
			this.currentBuilder.appendNewLine(this.currentBuilder.indentString + "ref(new fsLib.fl.Void(0))");
		}
		this.currentBuilder.unIndent();
	}

	public void visitDefault(CommonTree node) {
		this.currentBuilder.append("| _ ->");
		this.currentBuilder.indent();
		this.visit(node.get(0));
		this.currentBuilder.unIndent();
	}

	public void visitTry(CommonTree node) {
		this.currentBuilder.append("try");
		this.generateBlock(node.get(0));
		
		if(!isNullOrEmpty(node, 1)){
			for(CommonTree element : node.get(1)){
				this.visit(element);
			}
		}
		if(!isNullOrEmpty(node, 2)){
			this.currentBuilder.append("finally");
			this.visit(node.get(2));
		}
	}

	//TODO
	public void visitCatch(CommonTree node) {
		this.visit(node.get(0), "with(", ")");
		this.generateBlock(node.get(1));
	}
	
	public void visitVarDeclStmt(CommonTree node) {
		boolean objLet = this.objFlag;
		CommonTree listNode = node.get(2);
		CommonTree varDeclNode = listNode.get(0);
		try{
			CommonTree varStmtNode = varDeclNode.get(1);
			if(!varStmtNode.is(JSTag.TAG_FUNC_DECL) && !varStmtNode.is(JSTag.TAG_OBJECT)){
				this.currentBuilder.append("let ");
				this.objFlag = false;
				this.visit(listNode);
				String name = varDeclNode.get(0).getText();
				if(objLet){
					this.addedGetterList.add("member this." + name + " = " + this.searchVarFromList(name, false).getCurrentName());
				}
			} else if(varStmtNode.is(JSTag.TAG_OBJECT)){
				this.visit(varStmtNode);
			} else {	
				varStmtNode.set(2, varDeclNode.get(0));
				this.visit(varStmtNode);
			}
		} catch(ArrayIndexOutOfBoundsException e){
			this.currentBuilder.append("//let " + varDeclNode.getText() + "0");
			this.varList.add(new FSharpVar(varDeclNode.get(0).getText(), this.prefixName));
		}
	}
	
	public void visitVarDecl(CommonTree node) {
		this.varList.add(new FSharpVar(node.get(0).getText(), this.prefixName));
		this.visit(node.get(0));
		if(node.size() > 1){
			this.currentBuilder.append(" = ");
			this.visit(node.get(1), "ref(", ")");
		}
	}
	
	private boolean isRecursiveFunc(CommonTree node, String name, boolean result){
		if(result){
			return true;
		}
		boolean res = false;
		if(node.is(JSTag.TAG_APPLY)){
			res = this.getFieldText(node.get(0)).contentEquals(name);
		} else {
			res = false;
		}
		if(node.size() >= 1 && !result){
			for(int i = 0; i < node.size(); i++){
				res = this.isRecursiveFunc(node.get(i), name, result);
			}
		}
		return res;
	}
	
	public void visitFuncDecl(CommonTree node) {
		//boolean mustWrap = this.currentBuilder.isStartOfLine();
//		boolean mustWrap = false;
//		boolean notLambda = node.get(2).is(JSTag.TAG_NAME);
//		boolean memberFlag = objFlag;	
//		
//		if(mustWrap){
//			this.currentBuilder.appendChar('(');
//		}
//		if(notLambda && !objFlag){
//			this.currentBuilder.append("let");
//			if(this.isRecursiveFunc(node.get(6), this.prefixName + node.get(2).getText(), false)){
//				this.currentBuilder.append(" rec");
//			}
//		} else if(!notLambda && !objFlag){
//			this.currentBuilder.append("fun");
//		} else if(objFlag){
//			this.currentBuilder.append("member");
//		}
//		String addName = node.get(2).getText() + ".";
//		this.prefixName += addName;
//		if(!isNullOrEmpty(node, 2)){
//			this.currentBuilder.appendSpace();
//			if(this.objFlag){
//				this.currentBuilder.append("this.");
//			}
//			this.visit(node.get(2));
//		}
//		
//		CommonTree parameters = node.get(4);
//		boolean containsVariadicParameter = false;
//		boolean isFirst = true;
//		int sizeOfParametersBeforeValiadic = 0;
//		int sizeOfParametersAfterValiadic = 0;
//		
//		this.currentBuilder.appendChar(' ');
//		//this.prefixName = this.nameList.get(this.nameList.size() - 1);
//		
//		for(CommonTree param : parameters){
//			if(param.is(JSTag.TAG_VARIADIC_PARAMETER)){
//				containsVariadicParameter = true;
//				sizeOfParametersAfterValiadic = 0;
//				continue;
//			}
//			if(containsVariadicParameter){
//				sizeOfParametersAfterValiadic++;
//			}else{
//				sizeOfParametersBeforeValiadic++;
//			}
//			if(!isFirst){
//				this.currentBuilder.append(" ");
//			}
//			this.varList.add(new FSharpVar(param.getText(), this.prefixName));
//			this.visit(param);
//			isFirst = false;
//		}
//		this.currentBuilder.appendChar(' ');
//		
//		if(notLambda){
//			this.currentBuilder.appendChar('=');
//		} else {
//			this.currentBuilder.append("->");
//		}
//		this.currentBuilder.indent();
//		if(containsVariadicParameter){
//			this.currentBuilder.appendNewLine("var __variadicParams = __markAsVariadic([]);");
//			this.currentBuilder.appendNewLine("for (var _i = ");
//			this.currentBuilder.appendNumber(sizeOfParametersBeforeValiadic);
//			this.currentBuilder.append(", _n = arguments.length - ");
//			this.currentBuilder.appendNumber(sizeOfParametersAfterValiadic);
//			this.currentBuilder.append("; _i < _n; ++_i){ __variadicParams.push(arguments[_i]); }");
//		}
//		this.objFlag = false;
//		this.visit(node.get(6));
//		if(!checkReturn(node.get(6), false)){
//			if(!memberFlag){
//				this.currentBuilder.appendNewLine(this.currentBuilder.indentString + "new fsLib.fl.Void(0)");
//			} else {
//				this.currentBuilder.appendNewLine("new fsLib.fl.Void(0)");
//			}
//		}
//		this.currentBuilder.unIndent();
//		if(mustWrap){
//			this.currentBuilder.appendChar(')');
//		}
//		
//		this.prefixName = this.prefixName.substring(0, this.prefixName.length() - addName.length());
	}
	
	public void visitDeleteProperty(CommonTree node) {
		this.currentBuilder.append("delete ");
		this.visit(node.get(0));
	}
	
	public void visitVoidExpression(CommonTree node) {
		this.currentBuilder.append("void(");
		this.visit(node.get(0));
		this.currentBuilder.append(")");
	}
	
	public void visitThrow(CommonTree node) {
		this.currentBuilder.append("throw ");
		this.visit(node.get(0));
	}
	
	public void visitNew(CommonTree node) {
		this.currentBuilder.append("new ");
		this.visit(node.get(0));
		this.currentBuilder.appendChar('(');
		if(!isNullOrEmpty(node, 1)){
			this.generateList(node.get(1), ", ");
		}
		this.currentBuilder.appendChar(')');
	}

	public void visitEmpty(CommonTree node) {
		this.currentBuilder.appendChar(';');
	}
	
	public void visitVariadicParameter(CommonTree node){
		this.currentBuilder.append("__variadicParams");
	}
	
	public void visitCount(CommonTree node){
		this.visit(node.get(0));
		this.currentBuilder.append(".length");
	}

}
