package nez.x.dfa;

import java.util.HashSet;

public class Or extends BooleanExpression {
	public BooleanExpression left, right;

	public Or() {

	}

	public Or(BooleanExpression left, BooleanExpression right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public int traverse() {
		// System.out.println("Or");
		int F = left.traverse();
		int G = right.traverse();
		return BDD.apply('|', F, G);
	}

	@Override
	public Or deepCopy() {
		return new Or(left.deepCopy(), right.deepCopy());
	}

	@Override
	public BooleanExpression assignBooleanValueToLogicVariable(boolean booleanValue, LogicVariable logicVariable) {
		BooleanExpression tmp_left = left.assignBooleanValueToLogicVariable(booleanValue, logicVariable);
		BooleanExpression tmp_right = right.assignBooleanValueToLogicVariable(booleanValue, logicVariable);
		if ((tmp_left instanceof LogicVariable) && (tmp_right instanceof LogicVariable)) {
			boolean leftHasValue = ((LogicVariable) tmp_left).hasValue();
			boolean rightHasValue = ((LogicVariable) tmp_right).hasValue();
			if (leftHasValue && rightHasValue) {
				return new LogicVariable(-1, ((LogicVariable) tmp_left).getValue() || ((LogicVariable) tmp_right).getValue());
			} else if (leftHasValue) {
				if (((LogicVariable) tmp_left).getValue()) {
					return new LogicVariable(-1, true);
				} else {
					return tmp_right;
				}
			} else if (rightHasValue) {
				if (((LogicVariable) tmp_right).getValue()) {
					return new LogicVariable(-1, true);
				} else {
					return tmp_left;
				}
			} else {
				return new Or(tmp_left, tmp_right);
			}
		} else if (tmp_left instanceof LogicVariable) {
			if (((LogicVariable) tmp_left).hasValue()) {
				if (((LogicVariable) tmp_left).getValue()) {
					return new LogicVariable(-1, true);
				} else {
					return tmp_right;
				}
			} else {
				return new Or(tmp_left, tmp_right);
			}
		} else if (tmp_right instanceof LogicVariable) {
			if (((LogicVariable) tmp_right).hasValue()) {
				if (((LogicVariable) tmp_right).getValue()) {
					return new LogicVariable(-1, true);
				} else {
					return tmp_left;
				}
			} else {
				return new Or(tmp_left, tmp_right);
			}
		} else {
			return new Or(tmp_left, tmp_right);
		}
	}

	@Override
	public boolean eval(HashSet<State> F, HashSet<State> L) {
		return left.eval(F, L) || right.eval(F, L);
	}

	@Override
	public String toString() {
		return "(" + left + "|" + right + ")";
	}
}