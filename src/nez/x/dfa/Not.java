package nez.x.dfa;


public class Not extends BooleanExpression {
	BooleanExpression inner;

	public Not() {

	}

	public Not(BooleanExpression inner) {
		this.inner = inner;
	}

	// 定数節点、LogicVariable、それ以外
	/*
	 * 本来であれば否定節を用いるが、諸事情で今回は愚直な方針を取る
	 * innerのBDDの各節点について、定数節点であれば状態を反転する（０なら１、１なら０）
	 */
	@Override
	public int traverse() {
		// System.out.println("Not");
		int address = inner.traverse();
		return this.notApply(address);
	}

	public int notApply(int address) {
		// 定数節点なら反転するだけ
		if (address == 0 || address == 1) {
			return (address == 0) ? 1 : 0;
		}
		BDDNode bn = BDD.nodeTable[address];
		int H0 = notApply(bn.zeroID);
		int H1 = notApply(bn.oneID);
		return BDD.getNode(new BDDNode(bn.variableID, H0, H1));
	}

	@Override
	public Not deepCopy() {
		return new Not(inner.deepCopy());
	}

	@Override
	public BooleanExpression assignBooleanValueToLogicVariable(boolean booleanValue, LogicVariable logicVariable) {
		BooleanExpression tmp = inner.assignBooleanValueToLogicVariable(booleanValue, logicVariable);
		if (tmp instanceof LogicVariable) {
			if (((LogicVariable) tmp).hasValue()) {
				((LogicVariable) tmp).reverseValue();
				return tmp;
			}
		}
		return new Not(tmp);
	}

	@Override
	public String toString() {
		return "!(" + inner + ")";
	}
}
