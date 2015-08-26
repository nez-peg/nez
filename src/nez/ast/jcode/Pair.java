package nez.ast.jcode;

public class Pair<L, R> {
	private L left;
	private R right;

	public Pair(L left, R right) {
		this.left = left;
		this.right = right;
	}

	public void setLeft(L left) {
		this.left = left;
	}

	public L getLeft() {
		return this.left;
	}

	public void setRight(R right) {
		this.right = right;
	}

	public R getRight() {
		return this.right;
	}

	@Override
	public String toString() {
		return "(" + this.left + ", " + this.right + ")";
	}
}
