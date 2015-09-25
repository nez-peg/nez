package nez.x.dfa;

public class And extends State {
	public And() {
	}

	public And(int stateID) {
		super(stateID);
	}

	private State left, right;

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
	// boolean accept(Context context, Map<ExecMemoState, Boolean> execMemo) {
	boolean accept(Context context, byte[][] execMemo) {
		// System.out.println(this);
		if (execMemo[this.id][context.getTop()] != -1) {
			return execMemo[this.id][context.getTop()] == 1;
		}

		Context nextContext1 = context.getContext();
		Context nextContext2 = context.getContext();
		boolean result = this.left.accept(nextContext1, execMemo) && this.right.accept(nextContext2, execMemo);
		execMemo[this.id][context.getTop()] = (result ? (byte) 1 : (byte) 0);
		return result;
		// return this.left.accept(nextContext1, execMemo) &&
		// this.right.accept(nextContext2, execMemo);
	}

	@Override
	public String toString() {
		return "(" + left + "&" + right + ")";
	}
}
