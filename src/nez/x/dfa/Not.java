package nez.x.dfa;

public class Not extends State {
	public Not() {
	}

	public Not(int stateID) {
		super(stateID);
	}

	public State inner;

	public State getInner() {
		return this.inner;
	}

	public void setInner(State inner) {
		this.inner = inner;
	}

	@Override
	// boolean accept(Context context) {
	boolean accept(int top) {
		// boolean accept(Context context, Map<ExecMemoState, Boolean> execMemo)
		// {
		// boolean accept(Context context, byte[][] execMemo) {
		// System.out.println(this);
		if (DFAConverter.execMemo[this.id][top] != -1) {
			return DFAConverter.execMemo[this.id][top] == 1;
		}
		// Context nextContext = context.getContext();
		boolean result = !this.inner.accept(top);
		DFAConverter.execMemo[this.id][top] = (result ? (byte) 1 : (byte) 0);
		return result;
		// return !this.inner.accept(nextContext, execMemo);
	}

	@Override
	public String toString() {
		return "!(" + this.inner + ")";
	}
}
