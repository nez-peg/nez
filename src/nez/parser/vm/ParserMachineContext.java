package nez.parser.vm;

import nez.ast.Source;
import nez.ast.Symbol;
import nez.ast.Tree;
import nez.parser.ParserContext;

public class ParserMachineContext extends ParserContext {

	public ParserMachineContext(String s) {
		super(s);
	}

	public ParserMachineContext(Source source, Tree<?> proto) {
		super(source);
		if (proto != null) {
			this.left = proto;
		}
		initVM();
	}

	@Override
	public final boolean eof() {
		return this.source.eof(pos);
	}

	@Override
	public final int read() {
		return this.source.byteAt(pos++);
	}

	@Override
	public final int prefetch() {
		return this.source.byteAt(pos);
	}

	@Override
	public final boolean match(byte[] utf8) {
		if (source.match(pos, utf8)) {
			this.move(utf8.length);
			return true;
		}
		return false;
	}

	@Override
	public final byte[] subByte(int start, int end) {
		return source.subByte(start, end);
	}

	private int head_pos = 0;

	@Override
	public final void back(int pos) {
		if (head_pos < this.pos) {
			this.head_pos = this.pos;
		}
		this.pos = pos;
	}

	public final long getPosition() {
		return this.pos;
	}

	public final long getMaximumPosition() {
		return head_pos;
	}

	public final void setPosition(long pos) {
		this.pos = (int) pos;
	}

	// ----------------------------------------------------------------------

	public static class StackData {
		public Object ref;
		public int value;
	}

	private static int StackSize = 64;
	private StackData[] stacks = null;
	private int usedStackTop;
	private int catchStackTop;

	public final void initVM() {
		this.stacks = new StackData[StackSize];
		for (int i = 0; i < StackSize; i++) {
			this.stacks[i] = new StackData();
		}
		this.stacks[0].ref = null;
		this.stacks[0].value = 0;
		this.stacks[1].ref = new Moz86.Exit(false);
		this.stacks[1].value = pos;
		this.stacks[2].ref = this.saveLog();
		this.stacks[2].value = this.saveSymbolPoint();
		this.stacks[3].ref = new Moz86.Exit(true);
		this.stacks[3].value = 0;
		this.catchStackTop = 0;
		this.usedStackTop = 3;
	}

	public final StackData getUsedStackTop() {
		return stacks[usedStackTop];
	}

	public final StackData newUnusedStack() {
		usedStackTop++;
		if (stacks.length == usedStackTop) {
			StackData[] newstack = new StackData[stacks.length * 2];
			System.arraycopy(stacks, 0, newstack, 0, stacks.length);
			for (int i = this.stacks.length; i < newstack.length; i++) {
				newstack[i] = new StackData();
			}
			stacks = newstack;
		}
		return stacks[usedStackTop];
	}

	public final StackData popStack() {
		StackData s = stacks[this.usedStackTop];
		usedStackTop--;
		// assert(this.catchStackTop <= this.usedStackTop);
		return s;
	}

	// Instruction

	public final void xPos() {
		StackData s = this.newUnusedStack();
		s.value = this.pos;
	}

	public final int xPPos() {
		StackData s = this.popStack();
		return s.value;
	}

	public final void xBack() {
		StackData s = this.popStack();
		this.back(s.value);
	}

	public final void xCall(String name, MozInst jump) {
		StackData s = this.newUnusedStack();
		s.ref = jump;
	}

	public final MozInst xRet() {
		StackData s = this.popStack();
		return (MozInst) s.ref;
	}

	public final void xAlt(MozInst failjump/* op.failjump */) {
		StackData s0 = newUnusedStack();
		StackData s1 = newUnusedStack();
		StackData s2 = newUnusedStack();
		s0.value = catchStackTop;
		catchStackTop = usedStackTop - 2;
		s1.ref = failjump;
		s1.value = this.pos;
		s2.ref = this.saveLog();
		s2.value = this.saveSymbolPoint();
	}

	public final void xSucc() {
		StackData s0 = stacks[catchStackTop];
		// StackData s1 = stacks[catchStackTop + 1];
		usedStackTop = catchStackTop - 1;
		catchStackTop = s0.value;
	}

	public final int xSuccPos() {
		StackData s0 = stacks[catchStackTop];
		StackData s1 = stacks[catchStackTop + 1];
		usedStackTop = catchStackTop - 1;
		catchStackTop = s0.value;
		return s1.value;
	}

	public final MozInst xFail() {
		StackData s0 = stacks[catchStackTop];
		StackData s1 = stacks[catchStackTop + 1];
		StackData s2 = stacks[catchStackTop + 2];
		usedStackTop = catchStackTop - 1;
		catchStackTop = s0.value;
		if (s1.value < this.pos) {
			// if (this.lprof != null) {
			// this.lprof.statBacktrack(s1.value, this.pos);
			// }
			this.back(s1.value);
		}
		this.backLog(s2.ref);
		this.backSymbolPoint(s2.value);
		assert (s1.ref != null);
		return (MozInst) s1.ref;
	}

	public final MozInst xStep(MozInst next) {
		StackData s1 = stacks[catchStackTop + 1];
		if (s1.value == this.pos) {
			return xFail();
		}
		s1.value = this.pos;
		StackData s2 = stacks[catchStackTop + 2];
		s2.ref = this.saveLog();
		s2.value = this.saveSymbolPoint();
		return next;
	}

	public final void xTPush() {
		StackData s = this.newUnusedStack();
		s.ref = this.left;
		s = this.newUnusedStack();
		s.ref = this.saveLog();
	}

	public final void xTLink(Symbol label) {
		StackData s = this.popStack();
		this.backLog(s.ref);
		s = this.popStack();
		this.linkTree((Tree<?>) s.ref, label);
		this.left = (Tree<?>) s.ref;
	}

	public final void xTPop() {
		StackData s = this.popStack();
		this.backLog(s.ref);
		s = this.popStack();
		this.left = (Tree<?>) s.ref;
	}

	public final void xSOpen() {
		StackData s = this.newUnusedStack();
		s.value = this.saveSymbolPoint();
	}

	public final void xSClose() {
		StackData s = this.popStack();
		this.backSymbolPoint(s.value);
	}

}
