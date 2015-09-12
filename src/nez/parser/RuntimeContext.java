package nez.parser;

import nez.ast.Source;
import nez.ast.TreeTransducer;
import nez.main.NezProfier;
import nez.main.Verbose;
import nez.util.ConsoleUtils;

public abstract class RuntimeContext implements Source {
	/* parsing position */
	long pos;
	long head_pos;

	public final long getPosition() {
		return this.pos;
	}

	final void setPosition(long pos) {
		this.pos = pos;
	}

	public boolean hasUnconsumed() {
		return this.pos != length();
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

	public final String getSyntaxErrorMessage() {
		return this.formatPositionLine("error", this.head_pos, "syntax error");
	}

	public final String getUnconsumedMessage() {
		return this.formatPositionLine("unconsumed", this.pos, "");
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

	public final Object getParseResult(long startpos, long endpos) {
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

	class StackData {
		Object ref;
		long value;
	}

	private static int StackSize = 64;
	private StackData[] stacks = null;
	private int usedStackTop;
	private int catchStackTop;

	public final void init(MemoTable memoTable, TreeTransducer treeTransducer) {
		this.astMachine = new ASTMachine(this, treeTransducer);
		this.stacks = new StackData[StackSize];
		for (int i = 0; i < StackSize; i++) {
			this.stacks[i] = new StackData();
		}
		this.stacks[0].ref = null;
		this.stacks[0].value = 0;
		this.stacks[1].ref = new IExit(false);
		this.stacks[1].value = this.getPosition();
		this.stacks[2].ref = astMachine.saveTransactionPoint();
		this.stacks[2].value = symbolTable.savePoint();
		this.stacks[3].ref = new IExit(true);
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

	final StackData popStack() {
		StackData s = stacks[this.usedStackTop];
		usedStackTop--;
		// assert(this.catchStackTop <= this.usedStackTop);
		return s;
	}

	public final void pushAlt(Instruction failjump/* op.failjump */) {
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

	public final Instruction fail() {
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
		return (Instruction) s1.ref;
	}

	public final Instruction skip(Instruction next) {
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

	// // ----------------------------------------------------------------------
	// // Instruction
	//
	// private ContextStack[] contextStacks = null;
	//
	// public final void initJumpStack(MemoTable memoTable) {
	// this.lastAppendedLog = new ASTLog();
	// this.contextStacks = new ContextStack[StackSize];
	// for(int i = 0; i < StackSize; i++) {
	// this.contextStacks[i] = new ContextStack();
	// }
	// this.contextStacks[0].jump = new IExit(false);
	// //this.contextStacks[0].debugFailStackFlag = true;
	// this.contextStacks[0].pos = this.getPosition();
	// this.contextStacks[0].topASTLog = this.lastAppendedLog;
	// this.contextStacks[1].jump = new IExit(true); // for a point of the first
	// called nonterminal
	// this.contextStacks[1].pos = this.getPosition();
	// this.catchStackTop = 0;
	// this.usedStackTop = 1;
	// if(this.treeTransducer == null) {
	// treeTransducer = new NoTreeTransducer();
	// }
	// this.memoTable = memoTable;
	// if(Verbose.PackratParsing) {
	// Verbose.println("MemoTable: " +
	// this.memoTable.getClass().getSimpleName());
	// }
	// }
	//
	// public final ContextStack getUsedStackTop0() {
	// return contextStacks[usedStackTop];
	// }
	//
	// private ContextStack newUnusedStack0() {
	// usedStackTop++;
	// if(contextStacks.length == usedStackTop) {
	// ContextStack[] newstack = new ContextStack[contextStacks.length*2];
	// System.arraycopy(contextStacks, 0, newstack, 0, contextStacks.length);
	// for(int i = this.contextStacks.length; i < newstack.length; i++) {
	// newstack[i] = new ContextStack();
	// }
	// contextStacks = newstack;
	// }
	// return contextStacks[usedStackTop];
	// }
	//
	// public final void dumpStack0(String op) {
	// System.out.println(op + " F="+this.catchStackTop +", T=" +usedStackTop);
	// }
	//
	// public final Instruction opIAlt(IAlt op) {
	// ContextStack stackTop = newUnusedStack0();
	// stackTop.prevFailCatch = catchStackTop;
	// catchStackTop = usedStackTop;
	// stackTop.jump = op.failjump;
	// stackTop.pos = this.pos;
	// stackTop.topASTLog = this.lastAppendedLog;
	// assert(stackTop.topASTLog != null);
	// //stackTop.debugFailStackFlag = true;
	// return op.next;
	// }
	//
	// public final Instruction opISucc(Instruction op) {
	// ContextStack stackTop = contextStacks[catchStackTop];
	// //assert(stackTop.debugFailStackFlag);
	// usedStackTop = catchStackTop - 1;
	// catchStackTop = stackTop.prevFailCatch;
	// return op.next;
	// }
	//
	// public final Instruction opIFail() {
	// ContextStack stackTop = contextStacks[catchStackTop];
	// //assert(stackTop.debugFailStackFlag);
	// usedStackTop = catchStackTop - 1;
	// catchStackTop = stackTop.prevFailCatch;
	// if(this.lprof != null) {
	// this.lprof.statBacktrack(stackTop.pos, this.pos);
	// }
	// rollback(stackTop.pos);
	// if(stackTop.topASTLog != this.lastAppendedLog) {
	// this.logAbort(stackTop.topASTLog, true);
	// }
	// return stackTop.jump;
	// }
	//
	//
	// public final Instruction opISkip(ISkip op) {
	// ContextStack stackTop = contextStacks[catchStackTop];
	// //assert(stackTop.debugFailStackFlag);
	// if(this.pos == stackTop.pos) {
	// return opIFail();
	// }
	// stackTop.pos = this.pos;
	// stackTop.topASTLog = this.lastAppendedLog;
	// assert(stackTop.topASTLog != null);
	// return op.next;
	// }
	//
	// final ContextStack newUnusedLocalStack0() {
	// ContextStack stackTop = newUnusedStack0();
	// assert(this.catchStackTop < this.usedStackTop);
	// //stackTop.debugFailStackFlag = false;
	// return stackTop;
	// }
	//
	// final ContextStack popLocalStack0() {
	// ContextStack stackTop = contextStacks[this.usedStackTop];
	// usedStackTop--;
	// //assert(!stackTop.debugFailStackFlag);
	// assert(this.catchStackTop <= this.usedStackTop);
	// return stackTop;
	// }
	//
	// public final Instruction opICall(ICall op) {
	// ContextStack top = newUnusedLocalStack0();
	// top.jump = op.jump;
	// return op.next;
	// }
	//
	// public final Instruction opIRet() {
	// Instruction jump = popLocalStack0().jump;
	// return jump;
	// }
	//
	// public final Instruction opIPos(IPos op) {
	// ContextStack top = newUnusedLocalStack0();
	// top.pos = pos;
	// return op.next;
	// }
	//
	// public final Instruction opIBack(IBack op) {
	// ContextStack top = popLocalStack0();
	// rollback(top.pos);
	// return op.next;
	// }

	// public final Instruction opNodePush(Instruction op) {
	// ContextStack top = newUnusedLocalStack0();
	// top.topASTLog = this.lastAppendedLog;
	// this.left = null;
	// return op.next;
	// }
	//
	// public final Instruction opNodeStore(INodeStore op) {
	// ContextStack top = popLocalStack0();
	// if(top.topASTLog.next != null) {
	// Object child = this.createNode(top.topASTLog.next);
	// logAbort(top.topASTLog, false);
	// if(child != null) {
	// pushDataLog(ASTLog.Link, op.index, child);
	// }
	// this.left = child;
	// //System.out.println("LINK " + this.lastAppendedLog);
	// }
	// return op.next;
	// }
	//
	// public final Instruction opICommit(Instruction op) {
	// ContextStack top = popLocalStack0();
	// if(top.topASTLog.next != null) {
	// Object child = this.createNode(top.topASTLog.next);
	// logAbort(top.topASTLog, false);
	// this.left = child;
	// //System.out.println("LINK " + this.lastAppendedLog);
	// }
	// return op.next;
	// }
	//
	// public final Instruction opAbort(Instruction op) {
	// ContextStack top = popLocalStack0();
	// if(top.topASTLog.next != null) {
	// //Object child = this.logCommit(top.lastLog.next);
	// logAbort(top.topASTLog, false);
	// this.left = null;
	// }
	// return op.next;
	// }
	//
	// public final Instruction opILink(ILink op) {
	// if(this.left != null) {
	// log(ASTLog.Link, op.index, this.left);
	// }
	// return op.next;
	// }
	//
	// public final Instruction opINew(INew op) {
	// pushDataLog(ASTLog.New, this.pos + op.shift, null); //op.e);
	// return op.next;
	// }
	//
	// public final Instruction opILeftNew(ILeftNew op) {
	// pushDataLog(ASTLog.Swap, this.pos + op.shift, null); // op.e);
	// return op.next;
	// }
	//
	//
	// public final Instruction opITag(ITag op) {
	// pushDataLog(ASTLog.Tag, 0, op.tag);
	// return op.next;
	// }
	//
	// public final Instruction opIReplace(IReplace op) {
	// pushDataLog(ASTLog.Replace, 0, op.value);
	// return op.next;
	// }
	//
	// public final Instruction opICapture(ICapture op) {
	// pushDataLog(ASTLog.Capture, this.pos, null);
	// return op.next;
	// }

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

	public final void startProfiling(NezProfier prof) {
		if (prof != null) {
			prof.setFile("I.File", this.getResourceName());
			prof.setCount("I.Size", this.length());
			this.lprof = new LocalProfiler();
			this.lprof.init(this.getPosition());
		}
	}

	public final void doneProfiling(NezProfier prof) {
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

		void parsed(NezProfier rec, long consumed) {
			consumed -= this.startPosition;
			this.endingNanoTime = System.nanoTime();
			NezProfier.recordLatencyMS(rec, "P.Latency", startingNanoTime, endingNanoTime);
			rec.setCount("P.Consumed", consumed);
			NezProfier.recordThroughputKPS(rec, "P.Throughput", consumed, startingNanoTime, endingNanoTime);
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
