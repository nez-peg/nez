package nez.x.dfa;

import java.util.Comparator;

public class BinaryOperatorMemoState implements Comparable<BinaryOperatorMemoState> {
	public char op;
	public int F, G;

	public BinaryOperatorMemoState() {

	}

	public BinaryOperatorMemoState(char op, int F, int G) {
		this.op = op;
		this.F = F;
		this.G = G;
	}

	@Override
	public int compareTo(BinaryOperatorMemoState o) {
		int result = new Character(this.op).compareTo(o.op);
		if (result == 0) {
			result = new Integer(this.F).compareTo(o.F);
			if (result == 0) {
				result = new Integer(this.G).compareTo(o.G);
			}
		}
		return result;
	}
}

class BinaryOperatorMemoStateComparator implements Comparator {
	@Override
	public int compare(Object o1, Object o2) {
		BinaryOperatorMemoState boms1 = (BinaryOperatorMemoState) o1;
		BinaryOperatorMemoState boms2 = (BinaryOperatorMemoState) o2;
		if (boms1.op != boms2.op) {
			return Character.compare(boms1.op, boms2.op);
		}
		if (boms1.F != boms2.F) {
			return Integer.compare(boms1.F, boms2.F);
		}
		return Integer.compare(boms1.G, boms2.G);
	}
}