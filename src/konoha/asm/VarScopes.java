package konoha.asm;

import java.util.ArrayDeque;

class VarScopes extends ArrayDeque<LocalVarScope> {
	private static final long serialVersionUID = 8905256606042979610L;

	/**
	 * local variable start index. if this builder represents static method or
	 * static initializer, index = 0. if this builder represents instance method
	 * or constructor, index = 1;
	 */
	protected final int startVarIndex;

	VarScopes(int startIndex) {
		super();
		this.startVarIndex = startIndex;
	}

	public void createNewScope() {
		int startIndex = this.startVarIndex;
		if (!this.isEmpty()) {
			startIndex = this.peek().getEndIndex();
		}
		this.push(new LocalVarScope(startIndex, this.peek()));
	}

	public void removeCurrentScope() {
		this.pop();
	}

}