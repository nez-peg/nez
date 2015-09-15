package nez.dfa;

import java.util.Comparator;

public class Edge {
	private int    src;
	private int    dst;
	private char   label; // empty -> ε, otherwise -> alphabet
	private int predicate; // and predicate : 0, not predicate : 1, otherwise -1
	
	public Edge(int src,int dst,char label,int predicate){
		this.src       = src; 
		this.dst       = dst;
		this.label     = label;
		this.predicate = predicate;
	}
	
	public void setSrc(int src) {
		this.src = src;
	}
	
	public void setDst(int dst) {
		this.dst = dst;
	}
	
	public void setLabel(char label) {
		this.label = label;
	}
	
	public void setPredicate(int predicate) {
		this.predicate = predicate;
	}
	
	public int getSrc() {
		return src;
	}
			
	public int getDst() {
		return dst;
	}
	
	public char getLabel() {
		return label;
	}
	
	public int getPredicate() {
		return predicate;
	}

	@Override
	public String toString() {
		return "( (" + src + "," + ((label!=' ')?label:"epsilon") + "), " + dst + " \"" + 
				((predicate==0)?"&predicate":((predicate==1)?"!predicate":"normal")) + "\")";
	}

}

class EdgeComparator implements Comparator {
	@Override
	public int compare(Object o1,Object o2){
		Edge e1 = (Edge)o1;
		Edge e2 = (Edge)o2;
		if( e1.getSrc() != e2.getSrc() ) return Integer.compare(e1.getSrc(),e2.getSrc());
		if( e1.getDst() != e2.getDst() ) return Integer.compare(e1.getDst(),e2.getDst());
		if( e1.getLabel() != e2.getLabel() ) return Character.compare(e1.getLabel(),e2.getLabel());
		return Integer.compare(e1.getPredicate(), e2.getPredicate());
	}
}
