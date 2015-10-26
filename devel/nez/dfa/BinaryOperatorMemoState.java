package nez.dfa;


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
