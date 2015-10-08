package nez.x.dfa;

public class ValidateEdge extends Edge {

	public ValidateEdge(int src, int dst) {
		super(src, dst, '$', -1);
	}

	@Override
	public boolean equals(Object obj) {
		ValidateEdge ve = (ValidateEdge) obj;
		return (ve.getSrc() == super.getSrc()) && (ve.getDst() == super.getDst());
	}

	@Override
	public String toString() {
		return "(" + super.getSrc() + "=>" + super.getDst() + ")";
	}
}
