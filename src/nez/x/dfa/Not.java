package nez.x.dfa;

import java.util.Map;

public class Not extends State {
	public Not() {
		// super(bfa, id);
		// TODO Auto-generated constructor stub
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
	boolean accept(Context context, Map<ExecMemoState, Boolean> execMemo) {
		System.out.println(this);
		Context nextContext = context.getContext();
		return !this.inner.accept(nextContext, execMemo);
	}

	@Override
	public String toString() {
		return "!(" + this.inner + ")";
	}
}
