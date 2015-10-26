package nez.dfa;

import java.util.Comparator;

public class BDDNode implements Comparable<BDDNode> {
	public int variableID, zeroID, oneID;

	public BDDNode() {
	}

	public BDDNode(int variableID, int zeroID, int oneID) {
		this.variableID = variableID;
		this.zeroID = zeroID;
		this.oneID = oneID;
	}

	public BDDNode deepCopy() {
		return new BDDNode(this.variableID, this.zeroID, this.oneID);
	}

	public boolean equals(BDDNode o) {
		return variableID == o.variableID && zeroID == o.zeroID && oneID == o.oneID;
	}

	@Override
	public int compareTo(BDDNode o) {
		int result = new Integer(this.variableID).compareTo(o.variableID);
		if (result == 0) {
			result = new Integer(this.zeroID).compareTo(o.zeroID);
			if (result == 0) {
				result = new Integer(this.oneID).compareTo(o.oneID);
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return "(" + variableID + "," + zeroID + "," + oneID + ")";
	}
}

class BDDNodeComparator implements Comparator {
	@Override
	public int compare(Object o1, Object o2) {
		BDDNode n1 = (BDDNode) o1;
		BDDNode n2 = (BDDNode) o2;
		if (n1.variableID != n2.variableID) {
			return Integer.compare(n1.variableID, n2.variableID);
		}
		if (n1.zeroID != n2.zeroID) {
			return Integer.compare(n1.zeroID, n2.zeroID);
		}
		return Integer.compare(n1.oneID, n2.oneID);
	}

}