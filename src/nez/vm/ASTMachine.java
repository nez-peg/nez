package nez.vm;

import nez.ast.Source;
import nez.ast.Tag;
import nez.ast.TreeTransducer;

class ASTMachine {
	final static int New     = 0;
	final static int Capture = 1;
	final static int Tag     = 2;
	final static int Replace = 3;
	final static int Swap    = 4;
	final static int Push    = 5;
	final static int Pop     = 6;
	final static int Link    = 7;
	final static int Nop     = 8;

	Source source;
	TreeTransducer treeTransducer;
	ASTLog lastAppendedLog = null;
	ASTLog unusedDataLog = null;

	public ASTMachine(Source source, TreeTransducer treeTransducer) {
		this.source = source;
		this.treeTransducer = treeTransducer;
		if(this.treeTransducer == null) {
			this.treeTransducer = new NoTreeTransducer();
		}
		//this.log(ASTMachine.Nop, 0, null);
		this.lastAppendedLog = new ASTLog();
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
		l.type   = type;
		l.value  = pos;
		l.ref    = value;
		l.prev = lastAppendedLog;
		l.next = null;
		lastAppendedLog.next = l;
		lastAppendedLog = l;
	}
	
	public final void logNew(long pos) {
		log(ASTMachine.New, pos, null);
	}

	public final void logCapture(long pos) {
		log(ASTMachine.Capture, pos, null);
	}

	public final void logTag(Tag tag) {
		log(ASTMachine.Tag, 0, tag);
	}

	public final void logReplace(Object value) {
		log(ASTMachine.Tag, 0, value);
	}

	public final void logSwap(Object value) {
		log(ASTMachine.Swap, 0, null);
	}

	public final void logPush() {
		log(ASTMachine.Push, 0, null);
	}

	public final void logPop(int index) {
		log(ASTMachine.Link, index, null);
	}

	private Object latestLinkedNode = null;
	public final Object getLatestLinkedNode() {
		return latestLinkedNode;
	}

	public final void logLink(int index, Object node) {
		log(ASTMachine.Link, index, node);
		latestLinkedNode = node;
	}
	
	public final void logAbort(ASTLog checkPoint, boolean isFail) {
		assert(checkPoint != null);
//		if(isFail) {
//			for(DataLog cur = checkPoint.next; cur != null; cur = cur.next ) {
//				System.out.println("ABORT " + cur);
//			}
//		}
		lastAppendedLog.next = this.unusedDataLog;
		this.unusedDataLog = checkPoint.next;
		this.unusedDataLog.prev = null;
		this.lastAppendedLog = checkPoint;
		this.lastAppendedLog.next = null;
	}


	public final Object saveTransactionPoint() {
		return lastAppendedLog;
	}

	public final void rollTransactionPoint(Object point) {
		ASTLog save = (ASTLog)point;
		if(save != lastAppendedLog) {
			lastAppendedLog.next = this.unusedDataLog;
			this.unusedDataLog = save.next;
			this.unusedDataLog.prev = null;
			this.lastAppendedLog = save;
			this.lastAppendedLog.next = null;
		}
	}

	public final void commitTransactionPoint(int index, Object point) {
		ASTLog save = (ASTLog)point;
		Object node = createNode(save.next);
		this.rollTransactionPoint(point);
		if(node != null) {
			logLink(index, node);
		}
	}
	
	public final Object createNode(ASTLog start) {
		ASTLog cur = start;
		for(; cur != null; cur = cur.next ) {
			if(cur.type == ASTMachine.New) {
				break;
			}
		}
		if(cur == null) {
			return null;
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
			case ASTMachine.Swap:
				left = constructTree(start, cur, spos, epos, objectSize, left, tag, value);
				start = cur;
				spos = cur.value; 
				epos = spos;
				tag = null; value = null;
				objectSize = 1;
				break;
			case ASTMachine.Push:
				createNode(cur);
				cur = (ASTLog)cur.ref;
				break;
			case ASTMachine.Pop:
				start.ref = cur;
				cur.ref = constructTree(start, cur, spos, epos, objectSize, null, tag, value);
				cur.type = ASTMachine.Link;
				//break;
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
		return constructTree(start, null, spos, epos, objectSize, left, tag, value);
	}

	private Object constructTree(ASTLog start, ASTLog end, long spos, long epos, int objectSize, Object left, Tag tag, Object value) {
		Object newnode = this.treeTransducer.newNode(tag, source, spos, epos, objectSize, value);
		if(left != null) {
			this.treeTransducer.link(newnode, 0, left);
		}
		if(objectSize > 0) {
//			System.out.println("PREV " + start.prev);
//			System.out.println(">>> BEGIN");
//			System.out.println("  LOG " + start);
			for(ASTLog cur = start.next; cur != end; cur = cur.next ) {
//				System.out.println("  LOG " + cur);
				if(cur.type == ASTMachine.Push) {
					cur = (ASTLog)cur.ref;
				}
				if(cur.type == ASTMachine.Link) {
					this.treeTransducer.link(newnode, (int)cur.value, cur.ref);
				}
			}
//			System.out.println("<<< END");
//			System.out.println("COMMIT " + newnode);
		}
		return this.treeTransducer.commit(newnode);
	}

	public final Object getParseResult(long startpos, long endpos) {
		for(ASTLog cur = this.lastAppendedLog; cur != null; cur = cur.prev) {
			if(cur.type == ASTMachine.New) {
				Object node = createNode(cur);
				logAbort(cur.prev, false);
				return node;
			}
		}
		return treeTransducer.newNode(null, source, startpos, endpos, 0, null);
	}

	class ASTLog {
		int     type;
		Object  ref;
		long    value;
		
		ASTLog prev;
		ASTLog next;
		
		int id() {
			if(prev == null) return 0;
			return prev.id() + 1;
		}
		
		@Override
		public String toString() {
			switch(type) {
			case ASTMachine.Link:
				return "["+id()+"] link<" + this.value + "," + this.ref + ">";
			case ASTMachine.Capture:
				return "["+id()+"] cap<pos=" + this.value + ">";
			case ASTMachine.Tag:
				return "["+id()+"] tag<" + this.ref + ">";
			case ASTMachine.Replace:
				return "["+id()+"] replace<" + this.ref + ">";
			case ASTMachine.Swap:
				return "["+id()+"] swap<pos=" + this.value + "," + this.ref + ">";
			case ASTMachine.New:
				return "["+id()+"] new<pos=" + this.value + ">"  + "   ## " + this.ref  ;
			}
			return "["+id()+"] nop";
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



