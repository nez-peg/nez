package nez.vm;

import nez.ast.Source;
import nez.ast.Tag;
import nez.ast.TreeTransducer;
import nez.lang.Expression;
import nez.main.Verbose;

class ASTMachine {
	final static boolean debugMode = true;
	final static int Nop     = 0;
	final static int Capture = 1;
	final static int Tag     = 2;
	final static int Replace = 3;
	final static int LeftFold = 4;
	final static int Pop     = 5;
	final static int Push    = 6;
	final static int Link    = 7;
	final static int New     = 8;

	Source source;
	TreeTransducer treeTransducer;
	ASTLog firstLog = null;
	ASTLog lastAppendedLog = null;
	ASTLog unusedDataLog = null;

	public ASTMachine(Source source, TreeTransducer treeTransducer) {
		this.source = source;
		this.treeTransducer = treeTransducer;
		if(this.treeTransducer == null) {
			this.treeTransducer = new NoTreeTransducer();
		}
		//this.log(ASTMachine.Nop, 0, null);
		this.firstLog = new ASTLog();
		this.lastAppendedLog = this.firstLog;
	}

	private final void log(int type, long pos, Object value) {
		ASTLog l;
		if(this.unusedDataLog == null) {
			l = new ASTLog();
		}
		else {
			l = this.unusedDataLog;
			this.unusedDataLog = l.next;
		}
		l.id     = lastAppendedLog.id + 1;
		l.type   = type;
		l.value  = pos;
		l.ref    = value;
		l.next = null;
		lastAppendedLog.next = l;
		lastAppendedLog = l;
	}
	
	public final void logNew(long pos, Object debug) {
		log(ASTMachine.New, pos, debug);
	}

	public final void logCapture(long pos) {
		log(ASTMachine.Capture, pos, null);
	}

	public final void logTag(Tag tag) {
		log(ASTMachine.Tag, 0, tag);
	}

	public final void logReplace(Object value) {
		log(ASTMachine.Replace, 0, value);
	}

	public final void logSwap(Object value) {
		log(ASTMachine.LeftFold, 0, null);
	}

	public final void logPush() {
		log(ASTMachine.Push, 0, null);
	}

	public final void logPop(int index) {
		log(ASTMachine.Pop, index, null);
	}

	private Object latestLinkedNode = null;
	public final Object getLatestLinkedNode() {
		return latestLinkedNode;
	}

	public final void logLink(int index, Object node) {
		log(ASTMachine.Link, index, node);
		latestLinkedNode = node;
	}
	
//	public final void logAbort(ASTLog checkPoint, boolean isFail) {
//		assert(checkPoint != null);
////		if(isFail) {
////			for(DataLog cur = checkPoint.next; cur != null; cur = cur.next ) {
////				System.out.println("ABORT " + cur);
////			}
////		}
//		lastAppendedLog.next = this.unusedDataLog;
//		this.unusedDataLog = checkPoint.next;
//		this.unusedDataLog.prev = null;
//		this.lastAppendedLog = checkPoint;
//		this.lastAppendedLog.next = null;
//	}

	public final Object saveTransactionPoint() {
		return lastAppendedLog;
	}

	public final void rollTransactionPoint(Object point) {
		ASTLog save = (ASTLog)point;
		//Verbose.debug("roll" + save + " < " + this.lastAppendedLog);
		if(save != lastAppendedLog) {
			lastAppendedLog.next = this.unusedDataLog;
			this.unusedDataLog = save.next;
			save.next = null;
			this.lastAppendedLog = save;
		}
		assert(lastAppendedLog.next == null);
	}

	public final void commitTransactionPoint(int index, Object point) {
		ASTLog save = (ASTLog)point;
		Object node = createNode(save.next, null);
		this.rollTransactionPoint(point);
		if(node != null) {
			logLink(index, node);
		}
	}
	
	private void dump(ASTLog start, ASTLog end) {
		for(ASTLog cur = start; cur != null; cur = cur.next ) {
			Verbose.debug(cur.toString());
		}
	}
	
	public final Object createNode(ASTLog start, ASTLog pushed) {
		ASTLog cur = start;
		if(debugMode) {
			Verbose.debug("createNode.start: " + start + "     pushed:" + pushed);
		}
		for(; cur != null; cur = cur.next ) {
			if(cur.type == ASTMachine.New) {
				break;
			}
		}
		long spos = cur.value, epos = spos;
		Tag tag = null;
		Object value = null;
		int objectSize = 0;
		Object left = null;
		for(cur = cur.next; cur != null; cur = cur.next ) {
			switch(cur.type) {
			case ASTMachine.Capture:
				epos = cur.value;
				break;
			case ASTMachine.Tag:
				tag = (Tag)cur.ref;
				break;
			case ASTMachine.Replace:
				value = cur.ref;
				break;
			case ASTMachine.LeftFold:
				left = constructTree(start, cur, spos, epos, objectSize, left, tag, value);
				start = cur;
				spos = cur.value; 
				epos = spos;
				tag = null; value = null;
				objectSize = 1;
				break;
			case ASTMachine.Pop:
				assert(pushed != null);
				pushed.type = ASTMachine.Link;
				pushed.ref = constructTree(start, cur, spos, epos, objectSize, left, tag, value);
				pushed.value = cur.value;
				// TODO unused
				pushed.next = cur.next;
				return pushed.ref;
			case ASTMachine.Push:
				createNode(cur.next, cur);
				assert(cur.type == ASTMachine.Link);
			case ASTMachine.Link:
				int index = (int)cur.value;
				if(index == -1) {
					cur.value = objectSize;
					objectSize++;
				}
				else if(!(index < objectSize)) {
					objectSize = index + 1;
				}
				break;
			}
		}
		assert(pushed == null);
		return constructTree(start, null, spos, epos, objectSize, left, tag, value);
	}
	
	private void unused(ASTLog start, ASTLog pop) {
		
	}

	private Object constructTree(ASTLog start, ASTLog end, long spos, long epos, int objectSize, Object left, Tag tag, Object value) {
		Object newnode = this.treeTransducer.newNode(tag, source, spos, epos, objectSize, value);
		if(left != null) {
			this.treeTransducer.link(newnode, 0, left);
		}
		if(objectSize > 0) {
			for(ASTLog cur = start.next; cur != end; cur = cur.next ) {
				if(cur.type == ASTMachine.Link) {
					if(cur.ref == null) {
						Verbose.debug("@@ linking null child at " + cur.value);
					}
					else {
						this.treeTransducer.link(newnode, (int)cur.value, cur.ref);
					}
				}
			}
		}
		return this.treeTransducer.commit(newnode);
	}

	public final Object getParseResult(long startpos, long endpos) {
		if(debugMode) {
			dump(this.firstLog, null);
		}
		for(ASTLog cur = this.firstLog; cur != null; cur = cur.next) {
			if(cur.type == ASTMachine.New) {
				Object node = createNode(cur, null);
				//logAbort(cur.prev, false);
				//Verbose.debug("getParseResult: " + node);
				return node;
			}
		}
		//Verbose.debug("getParseResult: " + null);
		return treeTransducer.newNode(null, source, startpos, endpos, 0, null);
	}

	class ASTLog {
		int     id;
		int     type;
		Object  ref;
		long    value;
		
		ASTLog next;
				
		@Override
		public String toString() {
			switch(type) {
			case ASTMachine.Link:
				return "["+id+"] link(index=" + this.value + ")";
			case ASTMachine.Capture:
				return "["+id+"] cap(" + this.value + ")";
			case ASTMachine.Tag:
				return "["+id+"] tag(" + this.ref + ")";
			case ASTMachine.Replace:
				return "["+id+"] replace(" + this.ref + ")";
			case ASTMachine.LeftFold:
				return "["+id+"] left(" + this.value + ")";
			case ASTMachine.New:
				return "["+id+"] new(" + this.value + "," + this.ref + ")";
			case ASTMachine.Pop:
				return "["+id+"] pop(index=" + this.value + ")" ;
			case ASTMachine.Push:
				return "["+id+"] push"  ;
			}
			return "["+id+"] nop";
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
	
}



