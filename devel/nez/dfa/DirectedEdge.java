package nez.dfa;

public class DirectedEdge {
	private int src;
	private int dst;
	private int weight;

	public DirectedEdge() {

	}

	public DirectedEdge(int src, int dst, int weight) {
		this.src = src;
		this.dst = dst;
		this.weight = weight;
	}

	public void setSrc(int src) {
		this.src = src;
	}

	public void setDst(int dst) {
		this.dst = dst;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public int getSrc() {
		return this.src;
	}

	public int getDst() {
		return this.dst;
	}

	public int getWeight() {
		return this.weight;
	}
}
