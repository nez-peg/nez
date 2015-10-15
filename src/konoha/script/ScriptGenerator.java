package konoha.script;

import nez.ast.TreeVisitor;

public class ScriptGenerator extends TreeVisitor implements CommonSymbols {

	private ScriptBuilder builder;

	public ScriptGenerator() {
		super(TypedTree.class);
		this.builder = new ScriptBuilder();
		// this.context = context;
	}

	public void generate(TypedTree node) {

	}

	public void visit(TypedTree node) {
		visit("visit", node);
	}

	/* TopLevel */

	public void visitSource(TypedTree node) {
		for (TypedTree sub : node) {
			visit(sub);
		}
	}

	public void visitImport(TypedTree node) {

	}

	private void join(StringBuilder sb, TypedTree node) {
		TypedTree prefix = node.get(_prefix);
		if (prefix.size() == 2) {
			join(sb, prefix);
		} else {
			sb.append(prefix.toText());
		}
		sb.append(".").append(node.getText(_name, null));
	}

	/* FuncDecl */

	public void visitFuncDecl(TypedTree node) {
		// String name = node.getText(_name, null);
		// TypedTree bodyNode = node.get(_body, null);
	}

	public void visitReturn(TypedTree node) {
		this.builder.push("return");
		this.visit(node.get(_expr));
	}

	/* Statement */

	public void visitBlock(TypedTree node) {
		this.builder.openBlock("{");
		this.visitStatementList(node);
		this.builder.closeBlock("}");
	}

	public void visitStatementList(TypedTree node) {
		for (TypedTree sub : node) {
			this.visit(sub);
		}
	}

	public void visitStatement(TypedTree node) {
		if (node.is(_Block)) {
			visitBlock(node);
		} else {
			this.builder.openBlock("{");
			visit(node);
			this.builder.closeBlock("}");
		}
	}

	public void visitIf(TypedTree node) {
		this.builder.beginStatement("if");
		this.builder.push("(");
		this.visit(node.get(_cond));
		this.builder.push(")");
		this.visitStatement(node.get(_then));
		this.builder.endStatement("");
		if (node.has(_else)) {
			this.builder.beginStatement("else");
			this.visitStatement(node.get(_else));
			this.builder.endStatement("");
		}
	}

	public void visitConditional(TypedTree node) {
		this.visit(node.get(_cond));
		this.builder.push("?");
		this.visit(node.get(_then));
		this.builder.push(":");
		this.visit(node.get(_else));
	}

	public void visitWhile(TypedTree node) {
		this.builder.beginStatement("while");
		this.builder.push("(");
		this.visit(node.get(_cond));
		this.builder.push(")");
		this.visitStatement(node.get(_then));
	}

	public void visitContinue(TypedTree node) {
		this.builder.beginStatement("continue");
		this.builder.endStatement(";");
	}

	public void visitBreak(TypedTree node) {
		this.builder.beginStatement("break");
		this.builder.endStatement(";");
	}

	public void visitFor(TypedTree node) {
		// if (inFunction()) {
		// this.function.beginLocalVarScope();
		// }
		// if (node.has(_init)) {
		// type(node.get(_init));
		// }
		// if (node.has(_cond)) {
		// this.enforceType(boolean.class, node, _cond);
		// }
		// if (node.has(_iter)) {
		// type(node.get(_iter));
		// }
		// type(node.get(_body));
		// if (inFunction()) {
		// this.function.endLocalVarScope();
		// }
		// return void.class;
	}

	public void visitForEach(TypedTree node) {
		// Type req_t = null;
		// if (node.has(_type)) {
		// req_t = this.typeSystem.resolveType(node.get(_type), null);
		// }
		// String name = node.getText(_name, "");
		// req_t = typeIterator(req_t, node.get(_iter));
		// if (inFunction()) {
		// this.function.beginLocalVarScope();
		// }
		// this.function.setVarType(name, req_t);
		// type(node.get(_body));
		// if (inFunction()) {
		// this.function.endLocalVarScope();
		// }
		// return void.class;
	}

	public void visitVarDecl(TypedTree node) {
	}

	/* StatementExpression */
	public void visitExpression(TypedTree node) {
		this.builder.beginStatement("");
		this.visit(node.get(_expr));
		this.builder.beginStatement("");
	}

	/* Expression */

	public void visitName(TypedTree node) {
		String name = node.toText();
		this.builder.push(name);
	}

	public void visitAssign(TypedTree node) {
		this.visit(node.get(_left));
		this.builder.push("=");
		this.visit(node.get(_right));
	}

	/* Expression */

	public void visitCast(TypedTree node) {
		this.visit(node.get(_expr));
	}

	public void visitField(TypedTree node) {
		this.visit(node.get(_left));
		this.builder.write(",");
		this.visit(node.get(_right));
	}

	public void visitIndexer(TypedTree node) {

	}

	public void visitApply(TypedTree node) {
	}

	// private Type[] visitArguments(TypedTree args) {
	// Type[] types = new Type[args.size()];
	// for (int i = 0; i < args.size(); i++) {
	// types[i] = type(args.get(i));
	// }
	// return types;
	// }

	public void visitMethodApply(TypedTree node) {

	}

	private void visitUnary(TypedTree node, String name) {
		this.builder.write(name);
		this.visit(node.get(_expr));
	}

	private void visitBinary(TypedTree node, String name) {
		this.visit(node.get(_left));
		this.builder.push(name);
		this.visit(node.get(_right));
	}

	public void visitAnd(TypedTree node) {
		this.visitBinary(node, "and");
	}

	public void visitOr(TypedTree node) {
		this.visitBinary(node, "or");
	}

	public void visitNot(TypedTree node) {
		this.visitUnary(node, "not ");
	}

	public void visitAdd(TypedTree node) {
		this.visitBinary(node, "+");
	}

	public void visitSub(TypedTree node) {
		this.visitBinary(node, "-");
	}

	public void visitMul(TypedTree node) {
		this.visitBinary(node, "*");
	}

	public void visitDiv(TypedTree node) {
		this.visitBinary(node, "/");
	}

	public void visitPlus(TypedTree node) {
		this.visitUnary(node, "+");
	}

	public void visitMinus(TypedTree node) {
		this.visitUnary(node, "-");
	}

	public void visitEquals(TypedTree node) {
		this.visitBinary(node, "==");
	}

	public void visitNotEquals(TypedTree node) {
		this.visitBinary(node, "!=");
	}

	public void visitLessThan(TypedTree node) {
		this.visitBinary(node, "<");
	}

	public void visitLessThanEquals(TypedTree node) {
		this.visitBinary(node, "<=");
	}

	public void visitGreaterThan(TypedTree node) {
		this.visitBinary(node, ">");
	}

	public void visitGreaterThanEquals(TypedTree node) {
		this.visitBinary(node, ">=");
	}

	public void visitLeftShift(TypedTree node) {
		this.visitBinary(node, "<<");
	}

	public void visitRightShift(TypedTree node) {
		this.visitBinary(node, ">>");
	}

	public void visitLogicalRightShift(TypedTree node) {
		this.visitBinary(node, ">>>");
	}

	public void visitBitwiseAnd(TypedTree node) {
		this.visitBinary(node, "&");
	}

	public void visitBitwiseOr(TypedTree node) {
		this.visitBinary(node, "|");
	}

	public void visitBitwiseXor(TypedTree node) {
		this.visitBinary(node, "^");
	}

	public void visitCompl(TypedTree node) {
		this.visitUnary(node, "~");
	}

	public void visitNull(TypedTree node) {
		this.builder.push("None");
	}

	public void visitTrue(TypedTree node) {
		this.builder.push("True");
	}

	public void visitFalse(TypedTree node) {
		this.builder.push("False");
	}

	public void visitShort(TypedTree node) {
		// return typeInteger(node);
	}

	public void visitInteger(TypedTree node) {
		// return node.setConst(int.class, 0);
	}

	public void visitLong(TypedTree node) {

	}

	public void visitFloat(TypedTree node) {

	}

	public void visitDouble(TypedTree node) {

	}

	public void visitText(TypedTree node) {
		// return node.setConst(String.class, node.toText());
	}

	public void visitString(TypedTree node) {
		// String t = node.toText();
		// return node.setConst(String.class, StringUtils.unquoteString(t));
	}

	public void visitCharacter(TypedTree node) {

	}

	public void visitInterpolation(TypedTree node) {

	}

	/* array */

	public void visitArray(TypedTree node) {
	}

	// Syntax Sugar

	public void visitAssignAdd(TypedTree node) {
		this.visitBinary(node, "+=");
	}

	public void visitAssignSub(TypedTree node) {
		this.visitBinary(node, "-=");
	}

	public void visitAssignMul(TypedTree node) {
		this.visitBinary(node, "*=");
	}

	public void visitAssignDiv(TypedTree node) {
		this.visitBinary(node, "/=");
	}

	public void visitAssignMod(TypedTree node) {
		this.visitBinary(node, "%=");
	}

	public void visitAssignLeftShift(TypedTree node) {
		this.visitBinary(node, "<<=");
	}

	public void visitAssignRightShift(TypedTree node) {
		this.visitBinary(node, ">>=");
	}

	public void visitAssignLogicalRightShift(TypedTree node) {
		this.visitBinary(node, ">>>=");
	}

	public void visitAssignBitwiseAnd(TypedTree node) {
		this.visitBinary(node, "&=");
	}

	public void visitAssignBitwiseXOr(TypedTree node) {
		this.visitBinary(node, "^=");
	}

	public void visitAssignBitwiseOr(TypedTree node) {
		this.visitBinary(node, "|=");
	}

}
