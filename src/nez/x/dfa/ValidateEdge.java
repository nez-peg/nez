package nez.x.dfa;

public class ValidateEdge extends Edge {
	private boolean hasLeft;
	private boolean hasRight;

	public ValidateEdge(int src, int dst, boolean hasLeft, boolean hasRight) {
		super(src, dst, '$', -1);
		this.hasLeft = hasLeft;
		this.hasRight = hasRight;
		// TODO Auto-generated constructor stub
	}

	public boolean getHasLeft() {
		return this.hasLeft;
	}

	public boolean getHasRight() {
		return this.hasRight;
	}

	public void setHasLeft(boolean hasLeft) {
		this.hasLeft = hasLeft;
	}

	public void setHasRight(boolean hasRight) {
		this.hasRight = hasRight;
	}

	@Override
	public String toString() {
		return "(" + super.getSrc() + "=>" + super.getDst() + ",[" + this.hasLeft + "," + this.hasRight + "])";
	}
}
