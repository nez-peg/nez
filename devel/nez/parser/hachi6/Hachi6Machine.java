package nez.parser.hachi6;

import nez.ast.Source;
import nez.ast.Symbol;
import nez.ast.Tree;
import nez.lang.NonTerminal;
import nez.parser.MemoEntry;
import nez.parser.MemoTable;

// double stack
public class Hachi6Machine {

	private final static boolean FailMemo = false;

	private static int StackSize = 64;
	private int[] istacks = null;
	private Object[] rstacks = null;
	private int unused;
	private int failstack;

	public final void init(int pos, Tree<?> init, Hachi6Inst fail, Hachi6Inst ret) {
		this.pos = pos;
		this.istacks = new int[StackSize];
		this.rstacks = new Object[StackSize];
		this.unused = 0;
		this.failstack = 0;
		this.ounused = 0;
		this.oleft = init;
		if (init != null) {
			this.enabledAST = true;
			this.ostacks = new Object[StackSize];
		} else {
			this.enabledAST = false;
			this.ostacks = null;
		}
		xAlt(fail);
		push(ret);
	}

	private void extend() {
		int[] newistacks = new int[istacks.length * 2];
		System.arraycopy(istacks, 0, newistacks, 0, istacks.length);
		istacks = newistacks;
		Object[] newrstacks = new Object[rstacks.length * 2];
		System.arraycopy(rstacks, 0, newrstacks, 0, rstacks.length);
		rstacks = newrstacks;
	}

	public final void push(Object value) {
		if (rstacks.length == unused) {
			extend();
		}
		rstacks[unused] = value;
		unused++;
	}

	public final void push(int value) {
		if (istacks.length == unused) {
			extend();
		}
		istacks[unused] = value;
		unused++;
	}

	public final void pop() {
		unused--;
	}

	public final Object rpop() {
		unused--;
		return rstacks[unused];
	}

	public final int ipop() {
		unused--;
		return istacks[unused];
	}

	// public final Hachi6StackData getUsedStackTop() {
	// return stacks[usedStackTop];
	// }

	//

	private byte[] buffer;
	private int pos;

	public final int getPosition() {
		return this.pos;
	}

	public final void moveTo(int pos) {
		this.pos = pos;
	}

	public final void shift(int shift) {
		this.pos += shift;
	}

	public final int read() {
		return buffer[this.pos++] & 0xff;
	}

	public final int prefetch() {
		return buffer[this.pos] & 0xff;
	}

	public final boolean match(byte[] b) {
		int p = this.pos;
		if (this.buffer[p] != b[0]) {
			return false;
		}
		if (p + b.length < buffer.length) {
			p++;
			for (int i = 1; i < b.length; i++) {
				if (this.buffer[p] != b[i]) {
					return false;
				}
				p++;
			}
			this.pos += b.length;
			return true;
		}
		return false;
	}

	// Hachi6Inst

	public final void xNop() {
	}

	public final void xPos() {
		this.push(this.pos);
	}

	public final void xBack() {
		int pos = this.ipop();
		this.moveTo(pos);
	}

	public final Hachi6Inst xJump(Hachi6Inst jump) {
		return jump;
	}

	public final void xCall(Hachi6Inst jump, NonTerminal p) {
		this.push(jump);
		this.push(p);
	}

	public final Hachi6Inst xRet() {
		this.pop();
		return (Hachi6Inst) this.rpop();
	}

	public final void xAlt(Hachi6Inst jump) {
		failstack = unused + 1;
		push(failstack);
		push(pos);
		push(jump);
		if (enabledAST) {
			push(ounused);
			push(oleft);
		}
		// s2.value = symbolTable.savePoint();
	}

	public final void xSucc() {
		unused = failstack;
		failstack = istacks[failstack];
	}

	public final Hachi6Inst xFail() {
		unused = failstack;
		int n = failstack;
		failstack = istacks[n++];
		int pos = istacks[n++];
		Hachi6Inst jump = (Hachi6Inst) rstacks[n++];
		if (enabledAST) {
			this.ounused = istacks[n++];
			this.oleft = rstacks[n++];
		}
		this.moveTo(pos);
		return jump;
	}

	public final Hachi6Inst xGuard(Hachi6Inst jump) {
		int n = failstack + 1;
		int pos = istacks[n];
		if (pos == this.pos) {
			return xFail();
		}
		istacks[n] = pos;
		if (enabledAST) {
			// s2.ref = astMachine.saveTransactionPoint();
			istacks[n + 2] = this.ounused;
			rstacks[n + 3] = this.oleft;
		}
		// s2.value = symbolTable.savePoint();
		return jump;
	}

	/* AST construction */
	private boolean enabledAST = false;
	private Object[] ostacks = null;
	private int ounused;
	private Source source;
	private Object oleft;

	public final void opush(Object value) {
		if (ostacks.length == ounused) {
			Object[] newostacks = new Object[ostacks.length * 2];
			System.arraycopy(ostacks, 0, newostacks, 0, ostacks.length);
			rstacks = newostacks;
		}
		ostacks[ounused] = value;
		ounused++;
	}

	// public final void xDup() {
	// push(this.oleft);
	// }

	public final void xLink(Symbol label) {
		opush(label);
		opush(oleft);
		oleft = rpop();
	}

	public final void xEmit(Symbol label) {
		oleft = rpop();
	}

	public final void xSinit(int shift) {
		push(this.oleft); // dup
		push(this.pos + shift);
	}

	public final void xSnew(int shift, Symbol tag, Object value) {
		int start = ipop() + shift;
		Tree<?> t = ((Tree<?>) oleft).newInstance(tag, this.source, start, this.pos - start, 0, value);
		this.oleft = t;
	}

	public final void xInit(int shift) {
		push(this.oleft); // dup
		push(this.ounused);
		push(this.pos + shift);
	}

	public final void xNew(int shift) {
		int start = ipop() + shift;
		int ostart = ipop();
		int n = 0;
		Symbol tag = null;
		String value = null;
		for (int i = ostart; i < ounused; i++) {
			Object o = this.ostacks[i];
			if (o instanceof Tree<?>) {
				i++;
				n++;
				continue;
			}
			if (o instanceof Symbol) {
				ostacks[i] = null;
				tag = (Symbol) o;
			}
			if (o instanceof String) {
				ostacks[i] = null;
				value = (String) o;
			}
		}
		Tree<?> t = ((Tree<?>) oleft).newInstance(tag, this.source, start, this.pos - start, n, value);
		n = 0;
		for (int i = ostart; i < ounused; i++) {
			Object o = this.ostacks[i];
			if (o instanceof Tree<?>) {
				Tree<?> sub = (Tree<?>) o;
				Symbol label = (Symbol) this.ostacks[i + 1];
				t.set(n, label, sub);
				i++;
				n++;
			}
		}
		this.ounused = ostart;
		this.oleft = t;
	}

	public void xLeftFold(int shift, Symbol label) {
		push(this.ounused);
		push(this.pos + shift);
		opush(label);
		opush(oleft);
	}

	// public final void xTag(Symbol tag) {
	// opush(tag);
	// }
	//
	// public final void xValue(String value) {
	// opush(value);
	// }

	/* Memoization */

	MemoTable memoTable;

	public final boolean xLookup(int memo) {
		MemoEntry e = memoTable.getMemo(this.pos, memo);
		if (e != null) {
			this.pos += e.consumed;
			this.oleft = e.result;
			return true;
		}
		push(this.pos);
		return false;
	}

	public final void xMemo(int memo) {
		int start = ipop();
		memoTable.setMemo(start, memo, false, this.oleft, this.pos - start, 0);
	}

}

class Hachi6StackData {
	Object ref;
	int num;
}
