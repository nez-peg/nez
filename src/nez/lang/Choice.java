package nez.lang;

import nez.ast.SourcePosition;
import nez.util.UList;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class Choice extends Multinary {
	Choice(SourcePosition s, UList<Expression> l, int size) {
		super(s, l, size);
	}
	@Override
	public final boolean equalsExpression(Expression o) {
		if(o instanceof Choice && this.size() == o.size()) {
			for(int i = 0; i < this.size(); i++) {
				if(!this.get(i).equalsExpression(o.get(i))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	@Override
	public String getPredicate() {
		return "/";
	}
	@Override
	public String key() {
		return "/";
	}

	protected final void format(StringBuilder sb) {
		for(int i = 0; i < this.size(); i++) {
			if(i > 0) {
				sb.append(" / ");
			}
			this.get(i).format(sb);
		}
	}
//		if(e.predictedCase != null) {
//			int c = 0;
//			boolean[] printed = ByteMap.newMap(false);
//			for(int i = 0; i < e.predictedCase.length; i++) {
//				if(e.predictedCase[i] == null || printed[i] == true) {
//					continue;
//				}
//				if(c > 0) {
//					sb.append("|");
//				}
//				sb.append("&");
//				boolean[] m = checkRange(e.predictedCase, i, printed);
//				if(m == null) {
//					sb.append(StringUtils.stringfyByte(i));
//				}
//				else {
//					sb.append(StringUtils.stringfyCharClass(m));
//				}
//				if(e.predictedCase[i] != e) {
//					visit(e.predictedCase[i]);
//				}
//				else {
//					sb.append("...");
//				}
//				c++;
//			}
//		}
//		else {
//		}

//	private boolean[] checkRange(Expression[] predictedCase, int start, boolean[] printed) {
//		Expression e = predictedCase[start];
//		boolean[] result = null;
//		for(int i = start + 1; i < predictedCase.length; i++) {
//			if(predictedCase[i] == e) {
//				if(result == null) {
//					result = ByteMap.newMap(false);
//					result[start] = true;
//				}
//				result[i] = true;
//				printed[i] = true;
//			}
//		}
//		return result;
//	}

	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeChoice(this);
	}
	
	@Override
	public boolean isConsumed() {
		boolean afterAll = true;
		for(Expression e: this) {
			if(!e.isConsumed()) {
				afterAll = false;
			}
		}
		return afterAll;
	}
	
	@Override
	public int inferTypestate(Visa v) {
		int t = Typestate.BooleanType;
		for(Expression s: this) {
			t = s.inferTypestate(v);
			if(t == Typestate.ObjectType || t == Typestate.OperationType) {
				return t;
			}
		}
		return t;
	}
	
	@Override
	public short acceptByte(int ch, int option) {
		boolean hasUnconsumed = false;
		for(int i = 0; i < this.size(); i++) {
			short r = this.get(i).acceptByte(ch, option);
			if(r == Acceptance.Accept) {
				return r;
			}
			if(r == Acceptance.Unconsumed) {
				hasUnconsumed = true;
			}
		}
		return hasUnconsumed ? Acceptance.Unconsumed : Acceptance.Reject;
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeChoice(this, next, failjump);
	}
	
	// optimize
	public Expression[] predictedCase = null;
	
	@Override
	protected int pattern(GEP gep) {
		return this.size();
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		this.get(p % size()).examplfy(gep, sb, p);
	}

}
