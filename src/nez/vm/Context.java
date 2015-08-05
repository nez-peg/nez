package nez.vm;

import nez.ast.Source;
import nez.ast.Tag;
import nez.ast.TreeTransducer;
import nez.main.NezProfier;
import nez.main.Verbose;
import nez.util.ConsoleUtils;

public abstract class Context implements Source {
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
		if(head_pos < this.pos) {
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

	private TreeTransducer treeTransducer;
	private Object left;

	public final void setTreeTransducer(TreeTransducer treeTransducer) {
		this.treeTransducer = treeTransducer;
	}

	void setLeftObject(Object left) {
		this.left = left;
	}

	public final Object getLeftObject() {
		return this.left;
	}

	// private DataLog newPoint = null;
	private ASTLog lastAppendedLog = null;
	private ASTLog unusedDataLog = null;

	private final void pushDataLog(int type, long pos, Object value) {
		ASTLog l;
		if(this.unusedDataLog == null) {
			l = new ASTLog();
		}
		else {
			l = this.unusedDataLog;
			this.unusedDataLog = l.next;
		}
		l.type = type;
		l.pos = pos;
		l.value = value;
		l.prev = lastAppendedLog;
		l.next = null;
		lastAppendedLog.next = l;
		lastAppendedLog = l;
	}

	public final Object logCommit(ASTLog start) {
		assert (start.type == ASTLog.LazyNew);
		long spos = start.pos, epos = spos;
		Tag tag = null;
		Object value = null;
		int objectSize = 0;
		Object left = null;
		for(ASTLog cur = start.next; cur != null; cur = cur.next) {
			switch(cur.type) {
			case ASTLog.LazyLink:
				int index = (int)cur.pos;
				if(index == -1) {
					cur.pos = objectSize;
					objectSize++;
				}
				else if(!(index < objectSize)) {
					objectSize = index + 1;
				}
				break;
			case ASTLog.LazyCapture:
				epos = cur.pos;
				break;
			case ASTLog.LazyTag:
				tag = (Tag)cur.value;
				break;
			case ASTLog.LazyReplace:
				value = cur.value;
				break;
			case ASTLog.LazyLeftNew:
				left = commitNode(start, cur, spos, epos, objectSize, left, tag, value);
				start = cur;
				spos = cur.pos;
				epos = spos;
				tag = null;
				value = null;
				objectSize = 1;
				break;
			}
		}
		return commitNode(start, null, spos, epos, objectSize, left, tag, value);
	}

	private Object commitNode(ASTLog start, ASTLog end, long spos, long epos, int objectSize, Object left, Tag tag, Object value) {
		Object newnode = this.treeTransducer.newNode(tag, this, spos, epos, objectSize, value);
		if(left != null) {
			this.treeTransducer.link(newnode, 0, left);
		}
		if(objectSize > 0) {
			// System.out.println("PREV " + start.prev);
			// System.out.println(">>> BEGIN");
			// System.out.println("  LOG " + start);
			for(ASTLog cur = start.next; cur != end; cur = cur.next) {
				// System.out.println("  LOG " + cur);
				if(cur.type == ASTLog.LazyLink) {
					this.treeTransducer.link(newnode, (int)cur.pos, cur.value);
				}
			}
			// System.out.println("<<< END");
			// System.out.println("COMMIT " + newnode);
		}
		return this.treeTransducer.commit(newnode);
	}

	public final void logAbort(ASTLog checkPoint, boolean isFail) {
		assert (checkPoint != null);
		// if(isFail) {
		// for(DataLog cur = checkPoint.next; cur != null; cur = cur.next ) {
		// System.out.println("ABORT " + cur);
		// }
		// }
		lastAppendedLog.next = this.unusedDataLog;
		this.unusedDataLog = checkPoint.next;
		this.unusedDataLog.prev = null;
		this.lastAppendedLog = checkPoint;
		this.lastAppendedLog.next = null;
	}

	/* context-sensitivity parsing */
	/* <block e> <indent> */
	/* <def T e>, <is T>, <isa T> */

	private final SymbolTable symbolTable = new SymbolTable();

	public final SymbolTable getSymbolTable() {
		return this.symbolTable;
	}

	public final int getState() {
		return this.symbolTable.getState();
	}

	// ----------------------------------------------------------------------
	// Instruction

	private static int StackSize = 64;
	private ContextStack[] contextStacks = null;
	private int usedStackTop;
	private int catchStackTop;

	public final void initJumpStack(MemoTable memoTable) {
		this.lastAppendedLog = new ASTLog();
		this.contextStacks = new ContextStack[StackSize];
		for(int i = 0; i < StackSize; i++) {
			this.contextStacks[i] = new ContextStack();
		}
		this.contextStacks[0].jump = new IExit(false);
		// this.contextStacks[0].debugFailStackFlag = true;
		this.contextStacks[0].pos = this.getPosition();
		this.contextStacks[0].topASTLog = this.lastAppendedLog;
		this.contextStacks[1].jump = new IExit(true); // for a point of the first called nonterminal
		this.contextStacks[1].pos = this.getPosition();
		this.catchStackTop = 0;
		this.usedStackTop = 1;
		if(this.treeTransducer == null) {
			treeTransducer = new NoTreeTransducer();
		}
		this.memoTable = memoTable;
		if(Verbose.PackratParsing) {
			Verbose.println("MemoTable: " + this.memoTable.getClass().getSimpleName());
		}
	}

	public final ContextStack getUsedStackTop() {
		return contextStacks[usedStackTop];
	}

	private ContextStack newUnusedStack() {
		usedStackTop++;
		if(contextStacks.length == usedStackTop) {
			ContextStack[] newstack = new ContextStack[contextStacks.length * 2];
			System.arraycopy(contextStacks, 0, newstack, 0, contextStacks.length);
			for(int i = this.contextStacks.length; i < newstack.length; i++) {
				newstack[i] = new ContextStack();
			}
			contextStacks = newstack;
		}
		return contextStacks[usedStackTop];
	}

	public final void dumpStack(String op) {
		System.out.println(op + " F=" + this.catchStackTop + ", T=" + usedStackTop);
	}

	public final Instruction opITry(IFailPush op) {
		ContextStack stackTop = newUnusedStack();
		stackTop.prevFailCatch = catchStackTop;
		catchStackTop = usedStackTop;
		stackTop.jump = op.failjump;
		stackTop.pos = this.pos;
		stackTop.topASTLog = this.lastAppendedLog;
		assert (stackTop.topASTLog != null);
		// stackTop.debugFailStackFlag = true;
		return op.next;
	}

	public final Instruction opIFanally(Instruction op) {
		ContextStack stackTop = contextStacks[catchStackTop];
		// assert(stackTop.debugFailStackFlag);
		usedStackTop = catchStackTop - 1;
		catchStackTop = stackTop.prevFailCatch;
		return op.next;
	}

	public final Instruction opIFailCatch() {
		ContextStack stackTop = contextStacks[catchStackTop];
		// assert(stackTop.debugFailStackFlag);
		usedStackTop = catchStackTop - 1;
		catchStackTop = stackTop.prevFailCatch;
		if(this.lprof != null) {
			this.lprof.statBacktrack(stackTop.pos, this.pos);
		}
		rollback(stackTop.pos);
		if(stackTop.topASTLog != this.lastAppendedLog) {
			this.logAbort(stackTop.topASTLog, true);
		}
		return stackTop.jump;
	}

	public final Instruction opIFailSkip(IFailSkip op) {
		ContextStack stackTop = contextStacks[catchStackTop];
		// if(this.pos == stackTop.pos) {
		// return opIFail();
		// }
		stackTop.pos = this.pos;
		stackTop.topASTLog = this.lastAppendedLog;
		return op.next;
	}

	public final Instruction opIFailCheckSkip(IFailSkip op) {
		ContextStack stackTop = contextStacks[catchStackTop];
		// assert(stackTop.debugFailStackFlag);
		if(this.pos == stackTop.pos) {
			return opIFailCatch();
		}
		stackTop.pos = this.pos;
		stackTop.topASTLog = this.lastAppendedLog;
		assert (stackTop.topASTLog != null);
		return op.next;
	}

	final ContextStack newUnusedLocalStack() {
		ContextStack stackTop = newUnusedStack();
		assert (this.catchStackTop < this.usedStackTop);
		// stackTop.debugFailStackFlag = false;
		return stackTop;
	}

	final ContextStack popLocalStack() {
		ContextStack stackTop = contextStacks[this.usedStackTop];
		usedStackTop--;
		// assert(!stackTop.debugFailStackFlag);
		assert (this.catchStackTop <= this.usedStackTop);
		return stackTop;
	}

	public final Instruction opICallPush(ICallPush op) {
		ContextStack top = newUnusedLocalStack();
		top.jump = op.jump;
		return op.next;
	}

	public final Instruction opIRet() {
		Instruction jump = popLocalStack().jump;
		return jump;
	}

	public final Instruction opIPosPush(IPosPush op) {
		ContextStack top = newUnusedLocalStack();
		top.pos = pos;
		return op.next;
	}

	public final Instruction opIPopBack(IPosBack op) {
		ContextStack top = popLocalStack();
		rollback(top.pos);
		return op.next;
	}

	public final Instruction opIAnyChar(IAnyChar op) {
		if(this.hasUnconsumed()) {
			this.consume(1);
			return op.next;
		}
		return this.opIFailCatch();
	}

	public final Instruction opIByteChar(IByteChar op) {
		if(this.byteAt(this.pos) == op.byteChar) {
			this.consume(1);
			return op.next;
		}
		return this.opIFailCatch();
	}

	public final Instruction opIOptionByteChar(IByteChar op) {
		if(this.byteAt(this.pos) == op.byteChar) {
			this.consume(1);
			return op.next;
		}
		return op.next;
	}

	public final Instruction opIByteMap(IByteMap op) {
		int byteChar = this.byteAt(this.pos);
		if(op.byteMap[byteChar]) {
			this.consume(1);
			return op.next;
		}
		return this.opIFailCatch();
	}

	public final Instruction opIOptionByteMap(IByteMap op) {
		int byteChar = this.byteAt(this.pos);
		if(op.byteMap[byteChar]) {
			this.consume(1);
			return op.next;
		}
		return op.next;
	}

	public final Instruction opNodePush(Instruction op) {
		ContextStack top = newUnusedLocalStack();
		top.topASTLog = this.lastAppendedLog;
		this.left = null;
		return op.next;
	}

	public final Instruction opNodeStore(INodeStore op) {
		ContextStack top = popLocalStack();
		if(top.topASTLog.next != null) {
			Object child = this.logCommit(top.topASTLog.next);
			logAbort(top.topASTLog, false);
			if(child != null) {
				pushDataLog(ASTLog.LazyLink, op.index, child);
			}
			this.left = child;
			// System.out.println("LINK " + this.lastAppendedLog);
		}
		return op.next;
	}

	public final Instruction opICommit(Instruction op) {
		ContextStack top = popLocalStack();
		if(top.topASTLog.next != null) {
			Object child = this.logCommit(top.topASTLog.next);
			logAbort(top.topASTLog, false);
			this.left = child;
			// System.out.println("LINK " + this.lastAppendedLog);
		}
		return op.next;
	}

	public final Instruction opAbort(Instruction op) {
		ContextStack top = popLocalStack();
		if(top.topASTLog.next != null) {
			// Object child = this.logCommit(top.lastLog.next);
			logAbort(top.topASTLog, false);
			this.left = null;
		}
		return op.next;
	}

	public final Instruction opILink(ILink op) {
		if(this.left != null) {
			pushDataLog(ASTLog.LazyLink, op.index, this.left);
		}
		return op.next;
	}

	public final Instruction opINew(INew op) {
		pushDataLog(ASTLog.LazyNew, this.pos + op.shift, null); // op.e);
		return op.next;
	}

	public final Instruction opILeftNew(ILeftNew op) {
		pushDataLog(ASTLog.LazyLeftNew, this.pos + op.shift, null); // op.e);
		return op.next;
	}

	public final Object newTopLevelNode() {
		for(ASTLog cur = this.lastAppendedLog; cur != null; cur = cur.prev) {
			if(cur.type == ASTLog.LazyNew) {
				this.left = logCommit(cur);
				logAbort(cur.prev, false);
				return this.left;
			}
		}
		return null;
	}

	public final Instruction opITag(ITag op) {
		pushDataLog(ASTLog.LazyTag, 0, op.tag);
		return op.next;
	}

	public final Instruction opIReplace(IReplace op) {
		pushDataLog(ASTLog.LazyReplace, 0, op.value);
		return op.next;
	}

	public final Instruction opICapture(ICapture op) {
		pushDataLog(ASTLog.LazyCapture, this.pos, null);
		return op.next;
	}

	// Memoization
	MemoTable memoTable;

	public final void setMemo(long pos, int memoId, boolean failed, Object result, int consumed, boolean state) {
		memoTable.setMemo(pos, memoId, failed, result, consumed, state ? symbolTable.getState() : 0);
	}

	public final MemoEntry getMemo(int memoId, boolean state) {
		return state ?
				memoTable.getMemo2(this.pos, memoId, symbolTable.getState()) :
				memoTable.getMemo(this.pos, memoId);
	}

	public final Instruction opILookup(ILookup op) {
		MemoPoint mp = op.memoPoint;
		MemoEntry entry = op.state ?
				memoTable.getMemo2(this.pos, mp.id, symbolTable.getState()) :
				memoTable.getMemo(this.pos, mp.id);
		if(entry != null) {
			if(entry.failed) {
				mp.failHit();
				return opIFailCatch();
			}
			mp.memoHit(entry.consumed);
			this.consume(entry.consumed);
			if(op.node) {
				this.left = entry.result;
			}
			return op.skip;
		}
		mp.miss();
		if(op.node) {
			this.opITry(op);
			return this.opNodePush(op);
		}
		else {
			return this.opITry(op);
		}
	}

	public final Instruction opIMemoize(IMemoize op) {
		MemoPoint mp = op.memoPoint;
		if(op.node) {
			this.opICommit(op);
		}
		ContextStack stackTop = contextStacks[this.usedStackTop];
		int length = (int)(this.pos - stackTop.pos);
		memoTable.setMemo(stackTop.pos, mp.id, false, op.node ? this.left : null, length, op.state ? symbolTable.getState() : 0);
		return this.opIFanally(op);
	}

	public final Instruction opIMemoizeFail(IMemoizeFail op) {
		MemoPoint mp = op.memoPoint;
		memoTable.setMemo(pos, mp.id, true, null, 0, op.state ? symbolTable.getState() : 0);
		return opIFailCatch();
	}

	// public final Instruction opIMemoize(IStateMemoizeNode op) {
	// MemoPoint mp = op.memoPoint;
	// this.opNodeStore(op);
	// assert(this.usedStackTop == this.failStackTop);
	// ContextStack stackTop = contextStacks[this.failStackTop];
	// int length = (int)(this.pos - stackTop.pos);
	// memoTable.setMemo(stackTop.pos, mp.id, false, this.left, length, stateValue);
	// op.monitor.stored();
	// return this.opIFailPop(op);
	// }
	//
	//
	// public final Instruction opIStateLookup(IStateLookup op) {
	// MemoPoint mp = op.memoPoint;
	// MemoEntry m = memoTable.getMemo2(this.pos, mp.id, stateValue);
	// if(m != null) {
	// if(m.failed) {
	// mp.failHit();
	// return opIFail();
	// }
	// mp.memoHit(m.consumed);
	// this.consume(m.consumed);
	// return op.skip;
	// }
	// mp.miss();
	// return this.opIFailPush(op);
	// }
	//
	// public final Instruction opILookupNode(ILookupNode op) {
	// MemoPoint mp = op.memoPoint;
	// MemoEntry entry = memoTable.getMemo(this.pos, mp.id);
	// if(entry != null) {
	// op.monitor.used();
	// if(entry.failed) {
	// mp.failHit();
	// return opIFail();
	// }
	// mp.memoHit(entry.consumed);
	// this.consume(entry.consumed);
	// pushDataLog(OperationLog.LazyLink, op.index, entry.result);
	// return op.skip;
	// }
	// mp.miss();
	// this.opIFailPush(op);
	// return this.opNodePush(op);
	// }
	//
	// public final Instruction opIStateLookupNode(ILookupNode op) {
	// MemoPoint mp = op.memoPoint;
	// MemoEntry me = memoTable.getMemo2(pos, mp.id, stateValue);
	// if(me != null) {
	// op.monitor.used();
	// if(me.failed) {
	// mp.failHit();
	// return opIFail();
	// }
	// mp.memoHit(me.consumed);
	// consume(me.consumed);
	// pushDataLog(OperationLog.LazyLink, op.index, me.result);
	// return op.skip;
	// }
	// mp.miss();
	// this.opIFailPush(op);
	// return this.opNodePush(op);
	// }
	//
	// public final Instruction opIMemoize(IMemoize op) {
	// MemoPoint mp = op.memoPoint;
	// ContextStack stackTop = contextStacks[this.usedStackTop];
	// int length = (int)(this.pos - stackTop.pos);
	// memoTable.setMemo(stackTop.pos, mp.id, false, null, length, 0);
	// op.monitor.stored();
	// return this.opIFailPop(op);
	// }
	//
	// public final Instruction opIStateMemoize(IMemoize op) {
	// MemoPoint mp = op.memoPoint;
	// ContextStack stackTop = contextStacks[this.usedStackTop];
	// int length = (int)(this.pos - stackTop.pos);
	// memoTable.setMemo(stackTop.pos, mp.id, false, null, length, stateValue);
	// op.monitor.stored();
	// return this.opIFailPop(op);
	// }
	//
	// public final Instruction opIMemoizeNode(IMemoizeNode op) {
	// MemoPoint mp = op.memoPoint;
	// this.opNodeStore(op);
	// assert(this.usedStackTop == this.failStackTop);
	// ContextStack stackTop = contextStacks[this.failStackTop];
	// int length = (int)(this.pos - stackTop.pos);
	// memoTable.setMemo(stackTop.pos, mp.id, false, this.left, length, 0);
	// op.monitor.stored();
	// return this.opIFailPop(op);
	// }
	//
	// public final Instruction opIStateMemoizeNode(IStateMemoizeNode op) {
	// MemoPoint mp = op.memoPoint;
	// this.opNodeStore(op);
	// assert(this.usedStackTop == this.failStackTop);
	// ContextStack stackTop = contextStacks[this.failStackTop];
	// int length = (int)(this.pos - stackTop.pos);
	// memoTable.setMemo(stackTop.pos, mp.id, false, this.left, length, stateValue);
	// op.monitor.stored();
	// return this.opIFailPop(op);
	// }
	//
	// public final Instruction opIMemoizeFail(IMemoizeFail op) {
	// MemoPoint mp = op.memoPoint;
	// memoTable.setMemo(pos, mp.id, true, null, 0, 0);
	// op.monitor.stored();
	// return opIFail();
	// }
	//
	// public final Instruction opIStateMemoizeFail(IMemoizeFail op) {
	// MemoPoint mp = op.memoPoint;
	// memoTable.setMemo(pos, mp.id, true, null, 0, stateValue);
	// op.monitor.stored();
	// return opIFail();
	// }

	// Specialization

	public final Instruction opRepeatedByteMap(IRepeatedByteMap op) {
		while(true) {
			int c = this.byteAt(this.pos);
			if(!op.byteMap[c]) {
				break;
			}
			this.consume(1);
		}
		return op.next;
	}

	public final Instruction opNotByteMap(INotByteMap op) {
		int c = this.byteAt(this.pos);
		if(!op.byteMap[c]) {
			return op.next;
		}
		return this.opIFailCatch();
	}

	public final Instruction opNMultiChar(INotMultiChar op) {
		if(!this.match(this.pos, op.utf8)) {
			return op.next;
		}
		return this.opIFailCatch();
	}

	// public final Instruction opMultiChar(IMultiChar op) {
	// }

	// Profiling ------------------------------------------------------------

	private LocalProfiler lprof;

	public final void startProfiling(NezProfier prof) {
		if(prof != null) {
			prof.setFile("I.File", this.getResourceName());
			prof.setCount("I.Size", this.length());
			this.lprof = new LocalProfiler();
			this.lprof.init(this.getPosition());
		}
	}

	public final void doneProfiling(NezProfier prof) {
		if(prof != null) {
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
			if(Verbose.BacktrackActivity) {
				double cf = 0;
				for(int i = 0; i < 16; i++) {
					int n = 1 << i;
					double f = (double)this.BacktrackHistgrams[i] / this.BacktrackCount;
					cf += this.BacktrackHistgrams[i];
					ConsoleUtils.println(String.format("%d\t%d\t%2.3f\t%2.3f", n, this.BacktrackHistgrams[i], f, (cf / this.BacktrackCount)));
					if(n > this.LongestBacktrack)
						break;
				}
			}
		}

		public final void statBacktrack(long backed_pos, long current_pos) {
			this.FailureCount++;
			long len = current_pos - backed_pos;
			if(len > 0) {
				this.BacktrackCount = this.BacktrackCount + 1;
				this.BacktrackLength = this.BacktrackLength + len;
				if(this.HeadPostion < current_pos) {
					this.HeadPostion = current_pos;
				}
				len = this.HeadPostion - backed_pos;
				this.countBacktrackLength(len);
				if(len > this.LongestBacktrack) {
					this.LongestBacktrack = len;
				}
			}
		}

		private void countBacktrackLength(long len) {
			int n = (int)(Math.log(len) / Math.log(2.0));
			BacktrackHistgrams[n] += 1;
		}
	}

}

class ASTLog {
	final static int LazyLink = 0;
	final static int LazyCapture = 1;
	final static int LazyTag = 2;
	final static int LazyReplace = 3;
	final static int LazyLeftNew = 4;
	final static int LazyNew = 5;

	int type;
	long pos;
	Object value;
	ASTLog prev;
	ASTLog next;

	int id() {
		if(prev == null)
			return 0;
		return prev.id() + 1;
	}

	@Override
	public String toString() {
		switch(type) {
		case LazyLink:
			return "[" + id() + "] link<" + this.pos + "," + this.value + ">";
		case LazyCapture:
			return "[" + id() + "] cap<pos=" + this.pos + ">";
		case LazyTag:
			return "[" + id() + "] tag<" + this.value + ">";
		case LazyReplace:
			return "[" + id() + "] replace<" + this.value + ">";
		case LazyNew:
			return "[" + id() + "] new<pos=" + this.pos + ">" + "   ## " + this.value;
		case LazyLeftNew:
			return "[" + id() + "] leftnew<pos=" + this.pos + "," + this.value + ">";
		}
		return "[" + id() + "] nop";
	}
}

class NoTreeTransducer extends TreeTransducer {
	@Override
	public Object newNode(Tag tag, Source s, long spos, long epos, int size, Object value) {
		return null;
	}

	@Override
	public void link(Object node, int index, Object child) {
	}

	@Override
	public Object commit(Object node) {
		return null;
	}

	@Override
	public void abort(Object node) {
	}
}
