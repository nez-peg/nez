package nez.parser.moz;

import nez.Verbose;
import nez.ast.ASTMachine;
import nez.ast.Source;
import nez.ast.Tree;
import nez.io.SourceStream;
import nez.parser.MemoEntry;
import nez.parser.MemoTable;
import nez.parser.ParserProfier;
import nez.parser.ParserRuntime;
import nez.parser.StackData;
import nez.parser.SymbolTable;
import nez.util.ConsoleUtils;

public final class MozMachine extends ParserRuntime {
	/* parsing position */
	Source s;
	long pos;
	long head_pos;

	public MozMachine(Source source) {
		this.s = source;
	}

	public final int prefetch() {
		return this.s.byteAt(pos);
	}

	public final boolean match(byte[] utf8) {
		return s.match(pos, utf8);
	}

	public final byte[] subbyte(long start, long end) {
		return s.subbyte(start, end);
	}

	@Override
	public long getMaximumPosition() {
		return head_pos;
	}

	@Override
	public final long getPosition() {
		return this.pos;
	}

	public final void setPosition(long pos) {
		this.pos = pos;
	}

	@Override
	public boolean hasUnconsumed() {
		return this.pos != s.length();
	}

	public final boolean consume(int length) {
		this.pos += length;
		return true;
	}

	public final void rollback(long pos) {
		if (head_pos < this.pos) {
			this.head_pos = this.pos;
		}
		this.pos = pos;
	}

	// NOTE: Added by Honda
	public int getUsedStackTopForDebugger() {
		return this.usedStackTop;
	}

	/* PEG4d : AST construction */

	ASTMachine astMachine = null;

	public final ASTMachine getAstMachine() {
		return astMachine;
	}

	public final Tree<?> getParseResult(long startpos, long endpos) {
		return astMachine.getParseResult(startpos, endpos);
	}

	private final SymbolTable symbolTable = new SymbolTable();

	public final SymbolTable getSymbolTable() {
		return this.symbolTable;
	}

	public final int getState() {
		return this.symbolTable.getState();
	}

	// ----------------------------------------------------------------------

	private static int StackSize = 64;
	private StackData[] stacks = null;
	private int usedStackTop;
	private int catchStackTop;

	public final void init(MemoTable memoTable, Tree<?> prototype) {
		this.astMachine = new ASTMachine(s, prototype);
		this.stacks = new StackData[StackSize];
		for (int i = 0; i < StackSize; i++) {
			this.stacks[i] = new StackData();
		}
		this.stacks[0].ref = null;
		this.stacks[0].value = 0;
		this.stacks[1].ref = new Moz.Exit(false);
		this.stacks[1].value = this.getPosition();
		this.stacks[2].ref = astMachine.saveTransactionPoint();
		this.stacks[2].value = symbolTable.savePoint();
		this.stacks[3].ref = new Moz.Exit(true);
		this.stacks[3].value = 0;
		this.catchStackTop = 0;
		this.usedStackTop = 3;
		this.memoTable = memoTable;
		if (Verbose.PackratParsing) {
			Verbose.println("MemoTable: " + this.memoTable.getClass().getSimpleName());
		}
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

	public final void pushAlt(MozInst failjump/* op.failjump */) {
		StackData s0 = newUnusedStack();
		StackData s1 = newUnusedStack();
		StackData s2 = newUnusedStack();
		s0.value = catchStackTop;
		catchStackTop = usedStackTop - 2;
		s1.ref = failjump;
		s1.value = this.pos;
		s2.ref = astMachine.saveTransactionPoint();
		s2.value = symbolTable.savePoint();
	}

	public final long popAlt() {
		StackData s0 = stacks[catchStackTop];
		StackData s1 = stacks[catchStackTop + 1];
		long pos = s1.value;
		usedStackTop = catchStackTop - 1;
		catchStackTop = (int) s0.value;
		return pos;
	}

	public final MozInst fail() {
		StackData s0 = stacks[catchStackTop];
		StackData s1 = stacks[catchStackTop + 1];
		StackData s2 = stacks[catchStackTop + 2];
		usedStackTop = catchStackTop - 1;
		catchStackTop = (int) s0.value;
		if (s1.value < this.pos) {
			if (this.lprof != null) {
				this.lprof.statBacktrack(s1.value, this.pos);
			}
			this.rollback(s1.value);
		}
		this.astMachine.rollTransactionPoint(s2.ref);
		this.symbolTable.rollBack((int) s2.value);
		assert (s1.ref != null);
		return (MozInst) s1.ref;
	}

	public final MozInst skip(MozInst next) {
		StackData s1 = stacks[catchStackTop + 1];
		if (s1.value == this.pos) {
			return fail();
		}
		s1.value = this.pos;
		StackData s2 = stacks[catchStackTop + 2];
		s2.ref = astMachine.saveTransactionPoint();
		s2.value = symbolTable.savePoint();
		return next;
	}

	// Memoization
	MemoTable memoTable;

	public final void setMemo(long pos, int memoId, boolean failed, Object result, int consumed, boolean state) {
		memoTable.setMemo(pos, memoId, failed, result, consumed, state ? symbolTable.getState() : 0);
	}

	public final MemoEntry getMemo(int memoId, boolean state) {
		return state ? memoTable.getMemo2(this.pos, memoId, symbolTable.getState()) : memoTable.getMemo(this.pos, memoId);
	}

	// Profiling ------------------------------------------------------------

	private LocalProfiler lprof;

	public final void startProfiling(ParserProfier prof) {
		if (prof != null) {
			prof.setFile("I.File", s.getResourceName());
			prof.setCount("I.Size", s.length());
			this.lprof = new LocalProfiler();
			this.lprof.init(this.getPosition());
		}
	}

	public final void doneProfiling(ParserProfier prof) {
		if (prof != null) {
			this.lprof.parsed(prof, this.getPosition());
			this.memoTable.record(prof);
		}
	}

	class LocalProfiler {
		long startPosition = 0;
		long startingNanoTime = 0;
		long endingNanoTime = 0;

		long FailureCount = 0;
		long BacktrackCount = 0;
		long BacktrackLength = 0;

		long HeadPostion = 0;
		long LongestBacktrack = 0;
		int[] BacktrackHistgrams = null;

		public void init(long pos) {
			this.startPosition = pos;
			this.startingNanoTime = System.nanoTime();
			this.endingNanoTime = startingNanoTime;
			this.FailureCount = 0;
			this.BacktrackCount = 0;
			this.BacktrackLength = 0;
			this.LongestBacktrack = 0;
			this.HeadPostion = 0;
			this.BacktrackHistgrams = new int[32];
		}

		void parsed(ParserProfier rec, long consumed) {
			consumed -= this.startPosition;
			this.endingNanoTime = System.nanoTime();
			ParserProfier.recordLatencyMS(rec, "P.Latency", startingNanoTime, endingNanoTime);
			rec.setCount("P.Consumed", consumed);
			ParserProfier.recordThroughputKPS(rec, "P.Throughput", consumed, startingNanoTime, endingNanoTime);
			rec.setRatio("P.Failure", this.FailureCount, consumed);
			rec.setRatio("P.Backtrack", this.BacktrackCount, consumed);
			rec.setRatio("P.BacktrackLength", this.BacktrackLength, consumed);
			rec.setCount("P.LongestBacktrack", LongestBacktrack);
			if (Verbose.BacktrackActivity) {
				double cf = 0;
				for (int i = 0; i < 16; i++) {
					int n = 1 << i;
					double f = (double) this.BacktrackHistgrams[i] / this.BacktrackCount;
					cf += this.BacktrackHistgrams[i];
					ConsoleUtils.println(String.format("%d\t%d\t%2.3f\t%2.3f", n, this.BacktrackHistgrams[i], f, (cf / this.BacktrackCount)));
					if (n > this.LongestBacktrack)
						break;
				}
			}
		}

		public final void statBacktrack(long backed_pos, long current_pos) {
			this.FailureCount++;
			long len = current_pos - backed_pos;
			if (len > 0) {
				this.BacktrackCount = this.BacktrackCount + 1;
				this.BacktrackLength = this.BacktrackLength + len;
				if (this.HeadPostion < current_pos) {
					this.HeadPostion = current_pos;
				}
				len = this.HeadPostion - backed_pos;
				this.countBacktrackLength(len);
				if (len > this.LongestBacktrack) {
					this.LongestBacktrack = len;
				}
			}
		}

		private void countBacktrackLength(long len) {
			int n = (int) (Math.log(len) / Math.log(2.0));
			BacktrackHistgrams[n] += 1;
		}
	}

}
