package nez.x.dfa;

public class BooleanExpression {
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
}
