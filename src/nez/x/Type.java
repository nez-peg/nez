package nez.x;

import java.util.ArrayList;

import nez.ast.Tag;
import nez.expr.And;
import nez.expr.Capture;
import nez.expr.Choice;
import nez.expr.Expression;
import nez.expr.Link;
import nez.expr.New;
import nez.expr.NonTerminal;
import nez.expr.Option;
import nez.expr.Repetition;
import nez.expr.Repetition1;
import nez.expr.Rule;
import nez.expr.Sequence;
import nez.expr.Tagging;
import nez.expr.Typestate;
import nez.util.UList;

public abstract class Type {
	abstract Type dup();
	abstract void ref(Rule p);
	abstract void tag(Tag t);
	abstract void link(Link p, Type t);
	abstract boolean isRepetition();
	abstract void startRepetition();
	abstract void endRepetition();
	abstract void stringfy(StringBuilder sb);
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		stringfy(sb);
		return sb.toString();
	}
	
	public static Type inferType(Rule name, Expression e) {
		return inferType(name, e, new AtomType());
	}

	static Type inferType(Rule name, Expression e, Type inf) {
		if(e instanceof Tagging) {
			inf.tag(((Tagging) e).tag);
			return inf;
		}
		if(e instanceof Choice && e.inferTypestate() != Typestate.BooleanType) {
			UList<AtomType> u = new UList<AtomType>(new AtomType[e.size()]);
			for(int i = 0; i < e.size(); i++) {
				addUnionType(u, inferType(name, e.get(i), inf.dup()));
			}
			return new UnionType(u);
		}
		if(e instanceof Option && e.get(0).inferTypestate() != Typestate.BooleanType) {
			UList<AtomType> u = new UList<AtomType>(new AtomType[e.size()]);
			addUnionType(u, inferType(name, e.get(0), inf.dup()));
			addUnionType(u, inf);
			return new UnionType(u);
		}
		if(e instanceof NonTerminal) {
			if(e.inferTypestate() == Typestate.ObjectType) {
				inf.ref(((NonTerminal)e).getRule());
				return inf;
			}
			if(e.inferTypestate() == Typestate.OperationType) {
				inf = inferType(name, ((NonTerminal) e).getRule().getExpression(), inf);
				return inf;
			}
		}
		if(e instanceof Link) {
			Type t2 = inferType(name, e.get(0), new AtomType());
			inf.link((Link) e, t2);
			return inf;
		}
		if(e instanceof New && ((New) e).lefted) {
//			if(((New) e).unRepeated) {
//				System.out.println("TODO: Unrepeated left new is unsupported.");
//			}
			AtomType left = new AtomType();
			left.ref(name);
			inf.link(null, left);
			left = new AtomType();
			left.right = inf;
			return left;
		}
		if(e instanceof Capture) {
			/* avoid (Expr, Expr*) in Expr {@ Expr}* */
			//System.out.println("*" + inf.isRepetition());
			if(inf.isRepetition()) {
				inf.endRepetition(); 				
			}
			else {
				// FIXME::
				//((Capture)e).begin.unRepeated = true;
			}
			return inf;
		}
		if(e instanceof Repetition1) {
			inf.startRepetition();
			inf = inferType(name, e.get(0), inf);
			inf.endRepetition();
			inf = inferType(name, e.get(0), inf);
			return inf;
		}
		if(e instanceof Repetition) {
			inf.startRepetition();
			inf = inferType(name, e.get(0), inf);
			inf.endRepetition();
			return inf;
		}
		if(e instanceof And) {
			return inferType(name, e.get(0), inf);
		}
		if(e instanceof Sequence) {
			for(int i = e.size() -1; i >= 0; i--) {
				inf = inferType(name, e.get(i), inf);
			}
		}
		return inf;
	}
	
	static void addUnionType(UList<AtomType> l, Type t) {
		if(t instanceof UnionType) {
			for(AtomType u: ((UnionType)t).union) {
				addUnionType(l, u);
			}
		}
		else {
			for(AtomType u : l) {
				if(AtomType.isA(u, (AtomType)t)) {
					return;
				}
			}
			l.add((AtomType)t);
		}
	}
	
	
}

class LinkLog {
	boolean isRepetition;
	Link p;
	Type t;
	LinkLog next;
	LinkLog(boolean isRepetition, Link p, Type t, LinkLog next) {
		this.isRepetition = isRepetition;
		this.p = p;
		this.t = t;
		this.next = next;
	}
	int index() {
		return this.p == null ? 0 : p.index;
	}
	boolean is(LinkLog l) {
		if(this.isRepetition != l.isRepetition) {
			return false;
		}
		if(this.index() != l.index()) {
			return false;
		}
		return AtomType.is(this.t, l.t);
	}
	@Override
	public String toString() {
		return t.toString() + (this.isRepetition ? "*" : "");
	}
}

class AtomType extends Type {
	Tag tag = null;
	Rule p = null;
	LinkLog link = null;
	int size = 0;
	Type right = null;;
	AtomType() {
	}
	@Override
	Type dup() {
		AtomType t = new AtomType();
		t.tag  = this.tag;
		t.p    = this.p;
		t.link = this.link;
		t.size = this.size;
		t.right = this.right;
		return t;
	}
	
	static boolean is(Type t, Type t2) {
		if(t == null || t2 == null) {
			return t == t2;
		}
		if(t instanceof AtomType || t2 instanceof AtomType) {
			return isA((AtomType)t, (AtomType)t2);
		}
		if(t instanceof UnionType || t2 instanceof UnionType) {
			UnionType u = (UnionType)t;
			UnionType u2 = (UnionType)t2;
			if(u.union.length == u2.union.length) {
				for(int i = 0; i < u.union.length; i++) {
					boolean found = false;
					for(int j = 0; j < u.union.length; j++) {
						if(isA(u.union[i], u2.union[j])) {
							found = true;
							break;
						}
					}
					if(!found) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	static boolean isA(AtomType t, AtomType t2) {
		if(t.tag != t2.tag) {
			return false;
		}
		if(t.p != t2.p) {
			return false;
		}		
		if(t.size != t2.size) {
			return false;
		}
		LinkLog l = t.link;
		LinkLog l2 = t2.link;
		while(l != null) { assert(l2 != null); /* t.size = t2.size */
			if(!l.is(l2)) {
				return false;
			}
			l = l.next;
			l2 = l2.next;
		}
		return true;
	}

	@Override
	void tag(Tag tag) {
		if(this.tag == null) {
			this.tag = tag;
		}
	}
	@Override
	void ref(Rule p) {
		assert(this.p == null);
		this.p = p;
	}

	boolean isRepetition = false;
	@Override
	void startRepetition() {
		this.isRepetition = true;
	}
	@Override
	void endRepetition() {
		this.isRepetition = false;
	}
	@Override
	boolean isRepetition() {
		return this.isRepetition;
	}

	@Override
	void link(Link e, Type t) {
		this.link = new LinkLog(this.isRepetition, e, t, link);
		this.size++;
	}
	
	boolean isUnreatedLeftNew() {
		return (this.right != null && this.size > 0 && this.link.t == this.right) ;
	}
	
	@Override
	void stringfy(StringBuilder sb) {
		if(this.p != null) {
			sb.append(p.getLocalName());
			if(this.tag != null) {
				sb.append("#");
				sb.append(this.tag.getName());
			}
		}
		else {
			sb.append("#");
			sb.append((this.tag == null) ? "Text" : this.tag.getName());
		}
		if(size > 0) {
			sb.append("(");
			ArrayList<String> field = makeField();
			for(int i = 0; i < field.size(); i++) {
				if(i > 0) {
					sb.append(",");
				}
				if(field.get(i) == null) {
					sb.append("_");
				}
				else {
					sb.append(field.get(i));
				}
			}
			sb.append(")");
		}
		if(this.right != null) {
			sb.append("|");
			this.right.stringfy(sb);
		}
	}
	
	ArrayList<String> makeField() {
		ArrayList<String> field = new ArrayList<String>(size);
		LinkLog l = this.link;
		int last = 0;
		while(l != null) {
			int index = l.index();
			if(index < 0) {
				index = last;
				last++;
			}
			else {
				if(!(index < last)) {
					last = index + 1;
				}
			}
			field.ensureCapacity(last);
			while (!(index < field.size())) {
		        field.add(null);
		    }
			field.set(index, l.toString());
			l = l.next;
		}
		return field;
	}
}

class UnionType extends Type {
	AtomType[] union;
	UnionType(UList<AtomType> l) {
		this.union = new AtomType[l.size()];
		int c = 0;
		for(AtomType t2 : l) {
			this.union[c] = t2; c++;
		}
	}
	private UnionType(Type[] u) {
		this.union = new AtomType[u.length];
		for(int i = 0; i < u.length; i++) {
			this.union[i] = (AtomType)u[i].dup();
		}
	}
	@Override
	Type dup() {
		return new UnionType(this.union);
	}
	@Override
	void tag(Tag tag) {
		for(AtomType t : this.union) {
			t.tag(tag);
		}
	}
	@Override
	void ref(Rule p) {
		for(AtomType t : this.union) {
			t.ref(p);
		}
	}
	@Override
	void link(Link p, Type t2) {
		for(AtomType t : this.union) {
			t.link(p, t2);
		}
	}
	@Override
	void startRepetition() {
		for(AtomType t : this.union) {
			t.startRepetition();
		}
	}
	@Override
	void endRepetition() {
		for(AtomType t : this.union) {
			t.endRepetition();
		}
	}
	@Override
	boolean isRepetition() {
		return this.union[0].isRepetition();
	}
	@Override
	void stringfy(StringBuilder sb) {
		for(int i = 0; i < union.length; i++) {
			if(i > 0) {
				sb.append("|");
			}
			this.union[i].stringfy(sb);
		}
	}
}


