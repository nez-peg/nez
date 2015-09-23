package nez.x.dfa;

import java.util.Map;

public class Or extends State {
	public Or() {
		// super(bfa, id);
		// TODO Auto-generated constructor stub
	}

	State left, right;

	public State getLeft() {
		return left;
	}

	public State getRight() {
		return right;
	}

	public void setLeft(State left) {
		this.left = left;
	}

	public void setRight(State right) {
		this.right = right;
	}

	@Override
	// boolean accept(Context context) {
	boolean accept(Context context, Map<ExecMemoState, Boolean> execMemo) {
		System.out.println(this);
		Context nextContext1 = context.getContext();
		Context nextContext2 = context.getContext();
		return this.left.accept(nextContext1, execMemo) || this.right.accept(nextContext2, execMemo);
	}

	@Override
	public String toString() {
		return "(" + left + "|" + right + ")";
	}
}
