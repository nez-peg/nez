package nez.dfa;

public class Transition implements Comparable<Transition> {
	private State src;
	private State dst;
	private int label; // empty -> ε, otherwise -> alphabet
	private int predicate; // and predicate : 0, not predicate : 1

	public Transition() {

	}

	public Transition(int src, int dst, int label, int predicate) {
		this.src = new State(src);
		this.dst = new State(dst);
		this.label = label;
		this.predicate = predicate;
	}

	public Transition(State src, State dst, int label, int predicate) {
		this.src = new State(src.getID());
		this.dst = new State(dst.getID());
		this.label = label;
		this.predicate = predicate;
	}

	public void setSrc(int ID) {
		this.src.setID(ID);
	}

	public int getSrc() {
		return src.getID();
	}

	public void setDst(int ID) {
		this.dst.setID(ID);
	}

	public int getDst() {
		return dst.getID();
	}

	public void setLabel(int label) {
		this.label = label;
	}

	public int getLabel() {
		return this.label;
	}

	public void setPredicate(int predicate) {
		this.predicate = predicate;
	}

	public int getPredicate() {
		return this.predicate;
	}

	@Override
	public String toString() {
		if (this.label == AFA.epsilon) {
			if (predicate != -1) {
				return "((" + src + ",[predicate]" + ((predicate == 0) ? "&" : "!") + ")," + dst + ")";
			} else {
				return "((" + src + ",ε)," + dst + ")";
			}
		} else {
			return "((" + src + "," + (char) this.label + ")," + dst + ")";
		}
	}

	@Override
	public int compareTo(Transition transition) {
		if (this.src.getID() != transition.getSrc()) {
			return new Integer(this.src.getID()).compareTo(transition.getSrc());
		}
		if (this.dst.getID() != transition.getDst()) {
			return new Integer(this.dst.getID()).compareTo(transition.getDst());
		}
		if (this.label != transition.getLabel()) {
			return new Integer(this.label).compareTo(transition.getLabel());
		}
		return new Integer(this.predicate).compareTo(transition.getPredicate());
	}
}