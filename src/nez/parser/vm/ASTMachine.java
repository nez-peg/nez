package nez.parser.vm;

import nez.ast.Source;
import nez.ast.Symbol;
import nez.ast.Tree;
import nez.util.Verbose;

class ASTMachine {
	final static boolean debugMode = false;
	final static int Nop = 0;
	final static int Capture = 1;
	final static int Tag = 2;
	final static int Replace = 3;
	final static int LeftFold = 4;
	final static int Pop = 5;
	final static int Push = 6;
	final static int Link = 7;
	final static int New = 8;

	Source source;
	// TreeTransducer treeTransducer;
	Tree<?> prototype;
	ASTLog firstLog = null;
	ASTLog lastAppendedLog = null;
	ASTLog unusedDataLog = null;

	public ASTMachine(Source source, Tree<?> prototype) {
		this.source = source;
		this.prototype = prototype == null ? new EmptyTree() : prototype;
		// this.log(ASTMachine.Nop, 0, null);
		this.firstLog = new ASTLog();
		this.lastAppendedLog = this.firstLog;
	}

	private final void log(int type, long pos, Symbol label, Object value) {
		ASTLog l;
		if (this.unusedDataLog == null) {
			l = new ASTLog();
		} else {
			l = this.unusedDataLog;
			this.unusedDataLog = l.next;
		}
		l.id = lastAppendedLog.id + 1;
		l.type = type;
		l.value = pos;
		l.label = label;
		l.ref = value;
		l.next = null;
		lastAppendedLog.next = l;
		lastAppendedLog = l;
	}

	public final void logNew(long pos, Object debug) {
		log(ASTMachine.New, pos, null, debug);
	}

	public final void logCapture(long pos) {
		log(ASTMachine.Capture, pos, null, null);
	}

	public final void logTag(Symbol tag) {
		log(ASTMachine.Tag, 0, null, tag);
	}

	public final void logReplace(Object value) {
		log(ASTMachine.Replace, 0, null, value);
	}

	public final void logLeftFold(long pos, Symbol label) {
		log(ASTMachine.LeftFold, pos, label, null);
	}

	public final void logPush() {
		log(ASTMachine.Push, 0, null, null);
	}

	public final void logPop(Symbol label) {
		log(ASTMachine.Pop, 0, label, null);
	}

	private Object latestLinkedNode = null;

	public final Object getLatestLinkedNode() {
		return latestLinkedNode;
	}

	public final void logLink(Symbol label, Object node) {
		log(ASTMachine.Link, 0, label, node);
		latestLinkedNode = node;
	}

	public final Object saveTransactionPoint() {
		return lastAppendedLog;
	}

	public final void rollTransactionPoint(Object point) {
		ASTLog save = (ASTLog) point;
		if (debugMode) {
			Verbose.debug("roll" + save + " < " + this.lastAppendedLog);
		}
		if (save != lastAppendedLog) {
			lastAppendedLog.next = this.unusedDataLog;
			this.unusedDataLog = save.next;
			save.next = null;
			this.lastAppendedLog = save;
		}
		assert (lastAppendedLog.next == null);
	}

	public final void commitTransactionPoint(Symbol label, Object point) {
		ASTLog save = (ASTLog) point;
		Object node = createNode(save.next, null);
		this.rollTransactionPoint(point);
		if (node != null) {
			logLink(label, node);
		}
	}

	private void dump(ASTLog start, ASTLog end) {
		for (ASTLog cur = start; cur != null; cur = cur.next) {
			Verbose.debug(cur.toString());
		}
	}

	public final Tree<?> createNode(ASTLog start, ASTLog pushed) {
		ASTLog cur = start;
		if (debugMode) {
			Verbose.debug("createNode.start: " + start + "     pushed:" + pushed);
		}
		long spos = cur.value, epos = spos;
		Symbol tag = null;
		Object value = null;
		int objectSize = 0;
		for (cur = start; cur != null; cur = cur.next) {
			switch (cur.type) {
			case ASTMachine.New:
				spos = cur.value;
				epos = spos;
				objectSize = 0;
				tag = null;
				value = null;
				start = cur;
				break;
			case ASTMachine.Capture:
				epos = cur.value;
				break;
			case ASTMachine.Tag:
				tag = (Symbol) cur.ref;
				break;
			case ASTMachine.Replace:
				value = cur.ref;
				break;
			case ASTMachine.LeftFold:
				cur.ref = constructLeft(start, cur, spos, epos, objectSize, tag, value);
				cur.type = ASTMachine.Link;
				// cur.value = 0;
				spos = cur.value;
				tag = null;
				value = null;
				objectSize = 1;
				start = cur;
				break;
			case ASTMachine.Pop:
				assert (pushed != null);
				pushed.type = ASTMachine.Link;
				pushed.label = cur.label;
				pushed.ref = constructLeft(start, cur, spos, epos, objectSize, tag, value);
				pushed.value = cur.value;
				// TODO unused
				pushed.next = cur.next;
				return (Tree<?>) pushed.ref;
			case ASTMachine.Push:
				createNode(cur.next, cur);
				assert (cur.type == ASTMachine.Link);
			case ASTMachine.Link:
				objectSize++;
				break;
			}
		}
		assert (pushed == null);
		return constructLeft(start, null, spos, epos, objectSize, tag, value);
	}

	private Tree<?> constructLeft(ASTLog start, ASTLog end, long spos, long epos, int objectSize, Symbol tag, Object value) {
		if (tag == null) {
			tag = Symbol.Null;
		}
		Tree<?> newnode = this.prototype.newInstance(tag, source, spos, (int) (epos - spos), objectSize, value);
		int n = 0;
		if (objectSize > 0) {
			for (ASTLog cur = start; cur != end; cur = cur.next) {
				if (cur.type == ASTMachine.Link) {
					if (cur.ref == null) {
						Verbose.debug("@@ linking null child at " + cur.value);
					} else {
						// this.treeTransducer.link(newnode, n, cur.label,
						// cur.ref);
						newnode.link(n, cur.label, cur.ref);
					}
					n++;
				}
			}
		}
		// return this.treeTransducer.commit(newnode);
		return newnode;
	}

	// private Object constructTree(ASTLog start, ASTLog end, long spos, long
	// epos, int objectSize, Object left, Tag tag, Object value) {
	// Object newnode = this.treeTransducer.newNode(tag, source, spos, epos,
	// objectSize, value);
	// if(left != null) {
	// this.treeTransducer.link(newnode, 0, left);
	// }
	// if(objectSize > 0) {
	// for(ASTLog cur = start.next; cur != end; cur = cur.next ) {
	// if(cur.type == ASTMachine.Link) {
	// if(cur.ref == null) {
	// Verbose.debug("@@ linking null child at " + cur.value);
	// }
	// else {
	// this.treeTransducer.link(newnode, (int)cur.value, cur.ref);
	// }
	// }
	// }
	// }
	// return this.treeTransducer.commit(newnode);
	// }

	private Tree<?> parseResult = null;

	public final Tree<?> getParseResult(long startpos, long endpos) {
		if (parseResult != null) {
			return parseResult;
		}
		if (debugMode) {
			dump(this.firstLog, null);
		}
		for (ASTLog cur = this.firstLog; cur != null; cur = cur.next) {
			if (cur.type == ASTMachine.New) {
				parseResult = createNode(cur, null);
				break;
			}
		}
		if (parseResult == null) {
			parseResult = prototype.newInstance(Symbol.Null, source, startpos, (int) (endpos - startpos), 0, null);
		}
		this.firstLog = null;
		this.unusedDataLog = null;
		if (debugMode) {
			Verbose.debug("getParseResult: " + parseResult);
		}
		return parseResult;
	}

	static class ASTLog {
		int id;
		int type;
		Symbol label;
		Object ref;
		long value;

		ASTLog next;

		@Override
		public String toString() {
			switch (type) {
			case ASTMachine.Link:
				return "[" + id + "] link(index=" + this.value + ")";
			case ASTMachine.Capture:
				return "[" + id + "] cap(" + this.value + ")";
			case ASTMachine.Tag:
				return "[" + id + "] tag(" + this.ref + ")";
			case ASTMachine.Replace:
				return "[" + id + "] replace(" + this.ref + ")";
			case ASTMachine.LeftFold:
				return "[" + id + "] left(" + this.value + ")";
			case ASTMachine.New:
				return "[" + id + "] new(" + this.value + "," + this.ref + ")";
			case ASTMachine.Pop:
				return "[" + id + "] pop(" + this.ref + ")";
			case ASTMachine.Push:
				return "[" + id + "] push";
			}
			return "[" + id + "] nop";
		}
	}

	static class EmptyTree extends Tree<EmptyTree> {

		public EmptyTree() {
			super(null, null, 0, 0, null, null);
		}

		@Override
		public EmptyTree newInstance(Symbol tag, Source source, long pos, int len, int size, Object value) {
			return null;
		}

		@Override
		public void link(int n, Symbol label, Object child) {
		}

		@Override
		public EmptyTree newInstance(Symbol tag, int size, Object value) {
			return null;
		}

		@Override
		protected EmptyTree dupImpl() {
			return null;
		}

	}

}
