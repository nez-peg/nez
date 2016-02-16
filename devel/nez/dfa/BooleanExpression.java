package nez.dfa;

import java.util.HashSet;

public class BooleanExpression {

	public int bddID;

	public BooleanExpression deepCopy() {
		System.out.println("WARNING");
		return this.deepCopy();
	}

	// BooleanExpression内のlogicVariableにbooleanValueを代入した結果のBooleanExpressionを返す
	// Deep Copy
	public BooleanExpression assignBooleanValueToLogicVariable(boolean booleanValue, LogicVariable logicVariable) {
		System.out.println("WARNING");
		return this.assignBooleanValueToLogicVariable(booleanValue, logicVariable);
	}

	public int traverse() {
		System.out.println("WARNING");
		return -1;
	}

	public boolean eval(HashSet<State> F, HashSet<State> L) {
		System.out.println("WARNING");
		return false;
	}

	public BooleanExpression recoverPredicate() {
		System.out.println("WARNING");
		return this.recoverPredicate();
	}

}
