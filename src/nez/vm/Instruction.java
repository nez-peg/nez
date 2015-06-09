package nez.vm;

import java.util.Arrays;

import nez.ast.Tag;
import nez.lang.Block;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.DefIndent;
import nez.lang.DefSymbol;
import nez.lang.ExistsSymbol;
import nez.lang.Expression;
import nez.lang.IsIndent;
import nez.lang.IsSymbol;
import nez.lang.Link;
import nez.lang.LocalTable;
import nez.lang.New;
import nez.lang.NezTag;
import nez.lang.Acceptance;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.lang.Replace;
import nez.lang.Sequence;
import nez.lang.Tagging;
import nez.util.StringUtils;

public abstract class Instruction {
	protected Expression  e;
	public Instruction next;
	public int id;
	public boolean label = false;
	
	public Instruction(Expression e, Instruction next) {
		this.e = e;
		this.id = -1;
		this.next = next;
	}
	
	Instruction branch() {
		return null;
	}
	
	final short isAcceptImpl(int ch) {
		return next == null ? Acceptance.Accept : this.next.isAcceptImpl(ch);
	}

	boolean isAccept(int ch) {
		return this.isAcceptImpl(ch) == Acceptance.Accept;
	}

	abstract Instruction exec(Context sc) throws TerminationException;
	
	protected static Instruction labeling(Instruction inst) {
		if(inst != null) {
			inst.label = true;
		}
		return inst;
	}
	
	protected static String label(Instruction inst) {
		return "L"+inst.id;
	}

	public final String getName() {
		return this.getClass().getSimpleName();
	}

	protected String getOperand() {
		return null;
	}
	
	public Expression getExpression() {
		return this.e;
	}
	
	protected void stringfy(StringBuilder sb) {
		sb.append(this.getName());
		String op = getOperand();
		if(op != null) {
			sb.append(" ");
			sb.append(op);
		}
	}

	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		stringfy(sb);
		return sb.toString();
	}

	boolean debug() {
		return false;
	}
}

interface StackOperation {

}

class IFail extends Instruction implements StackOperation {
	IFail(Expression e) {
		super(e, null);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIFail();
	}
}

class IFailPush extends Instruction implements StackOperation {
	public final Instruction failjump;
	IFailPush(Expression e, Instruction failjump, Instruction next) {
		super(e, next);
		this.failjump = labeling(failjump);
	}
	@Override
	Instruction branch() {
		return this.failjump;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIFailPush(this);
	}
	@Override
	protected String getOperand() {
		return label(this.failjump) + "  ## " + e;
	}
}

class INotFailPush extends IFailPush implements StackOperation {
	INotFailPush(Expression e, Instruction failjump, Instruction next) {
		super(e, failjump, next);
	}
}

class IFailPop extends Instruction implements StackOperation {
	IFailPop(Expression e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIFailPop(this);
	}
}

class IFailSkip extends Instruction {
	IFailSkip(Expression e) {
		super(e, null);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIFailSkip(this);
	}
}

/*
 * IFailCheckSkip
 * Check unconsumed repetition
 */

class IFailCheckSkip extends IFailSkip {
	IFailCheckSkip(Expression e) {
		super(e);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIFailCheckSkip(this);
	}
}

class ICallPush extends Instruction implements StackOperation {
	Production rule;
	NonTerminal ne;
	public Instruction jump = null;
	ICallPush(Production rule, Instruction next) {
		super(rule, next);
		this.rule = rule;
	}
	ICallPush(Production rule, NonTerminal ne, Instruction next) {
		super(rule, next);
		this.rule = rule;
		this.ne = ne;
	}
	void setResolvedJump(Instruction jump) {
		assert(this.jump == null);
		this.jump = labeling(this.next);
		this.next = labeling(jump);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opICallPush(this);
	}
	
	@Override
	protected String getOperand() {
		return label(jump);
	}
}

class IRet extends Instruction implements StackOperation {
	IRet(Production e) {
		super(e, null);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIRet();
	}
}

class IMemoCall extends Instruction implements StackOperation {
	public Instruction returnPoint = null;
	public Production  production; 
	public MemoPoint   memoPoint = null;
	public Instruction nonMemoCodePoint = null;
	public Instruction memoCodePoint    = null;
	CodePoint temp;
	IMemoCall(CodePoint temp, Instruction next) {
		super(temp.production, next);
		this.temp = temp;
		this.production = temp.production;
	}
	@Override
	protected String getOperand() {
		return label(returnPoint) + "   ## " + production.getLocalName();
	}
	void resolveJumpAddress() {
		assert(this.returnPoint == null);
		assert(this.temp != null);
		this.memoCodePoint = temp.memoStart;
		this.nonMemoCodePoint = temp.nonmemoStart;
		this.memoPoint = temp.memoPoint;
		this.returnPoint = labeling(this.next);
		this.next = labeling(memoCodePoint);
		this.temp = null;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		ContextStack top = sc.newUnusedLocalStack();
		top.jump = this.returnPoint;
		return this.next;
	}
	void deactivateMemo() {
		this.next = nonMemoCodePoint;
	}
}

class IMemoRet extends Instruction implements StackOperation {
	public IMemoCall callPoint = null;
	IMemoRet(Production p, IMemoCall callPoint) {
		super(p, null);
		this.callPoint = callPoint;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		Instruction returnPoint = sc.popLocalStack().jump;
		if(this.callPoint != null) {
			if(callPoint.memoPoint.checkDeactivation()) {
				callPoint.deactivateMemo();
				callPoint = null;
			}
		}
		return returnPoint;
	}
}



class IPosPush extends Instruction {
	IPosPush(Expression e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIPosPush(this);
	}
}

class IPosBack extends Instruction {
	public IPosBack(Expression e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIPopBack(this);
	}
}

class IExit extends Instruction {
	boolean status;
	IExit(boolean status) {
		super(null, null);
		this.status = status;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		throw new TerminationException(status);
	}
}

class IAnyChar extends Instruction {
	IAnyChar(Expression e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIAnyChar(this);
	}
}

class INotAnyChar extends Instruction {
	INotAnyChar(Expression e, boolean isBinary, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		if(sc.hasUnconsumed()) {
			return sc.opIFail();
		}
		return next;
	}
}

class IByteChar extends Instruction {
	public final int byteChar;
	IByteChar(ByteChar e, Instruction next) {
		super(e, next);
		this.byteChar = e.byteChar;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIByteChar(this);
	}
	@Override
	protected String getOperand() {
		return StringUtils.stringfyCharacter(byteChar);
	}
}

class IOptionByteChar extends IByteChar {
	IOptionByteChar(ByteChar e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIOptionByteChar(this);
	}
}

class IByteMap extends Instruction {
	public final boolean[] byteMap;
	IByteMap(ByteMap e, Instruction next) {
		super(e, next);
		this.byteMap = e.byteMap;
	}
//	IByteMap(ByteChar e, Instruction next) {
//		super(e, next);
//		this.byteMap = ByteMap.newMap(false);
//		this.byteMap[e.byteChar] = true;
//	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIByteMap(this);
	}
	@Override
	protected String getOperand() {
		return StringUtils.stringfyCharacterClass(byteMap);
	}
}

class IOptionByteMap extends IByteMap {
	IOptionByteMap(ByteMap e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIOptionByteMap(this);
	}
}

class IConsume extends Instruction {
	IConsume(Expression e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		sc.consume(1);
		return this.next;
	}
}

class IBacktrack extends Instruction {
	final int prefetched;
	IBacktrack(Expression e, int prefetched, Instruction next) {
		super(e, next);
		this.prefetched = prefetched;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		sc.consume(-1);
		return this.next;
	}
}


class IDfaDispatch extends Instruction {
	Instruction[] jumpTable;
	IDfaDispatch(Expression e, Instruction next) {
		super(e, next);
		jumpTable = new Instruction[257];
		Arrays.fill(jumpTable, next);
	}
	void setJumpTable(int ch, Instruction inst) {
		if(inst instanceof IDfaDispatch) {
			jumpTable[ch] = ((IDfaDispatch) inst).jumpTable[ch];
		}
		else {
			jumpTable[ch] = Instruction.labeling(inst);
		}
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		int ch = sc.byteAt(sc.getPosition());
		//sc.consume(1);
		//System.out.println("ch="+(char)ch + " " + jumpTable[ch]);
		return jumpTable[ch].exec(sc);
	}
}

interface Construction {
	
}

class INodePush extends Instruction {
	INodePush(Link e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opNodePush(this);
	}
}

class INodeStore extends Instruction {
	public final int index;
	INodeStore(Link e, Instruction next) {
		super(e, next);
		this.index = e.index;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opNodeStore(this);
	}
	@Override
	protected String getOperand() {
		return String.valueOf(index);
	}
}

class ICommit extends Instruction {
	ICommit(Link e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		ContextStack top = sc.popLocalStack();
		if(top.lastLog.next != null) {
			Object child = sc.logCommit(top.lastLog.next);
			sc.setLeftObject(child);
			sc.logAbort(top.lastLog, false);
		}
		return this.next;
	}
}

class ILink extends Instruction {
	public final int index;
	ILink(Link e, Instruction next) {
		super(e, next);
		this.index = e.index;
	}
	@Override
	protected String getOperand() {
		return String.valueOf(index);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opILink(this);
	}
}

class INew extends Instruction {
	int shift;
	INew(New e, Instruction next) {
		super(e, next);
		this.shift = e.shift;
	}
	@Deprecated
	INew(Expression e, Instruction next) {
		super(e, next);
		this.shift = 0;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opINew(this);
	}
}

class ILeftNew extends Instruction {
	int shift;
	ILeftNew(New e, Instruction next) {
		super(e, next);
		this.shift = e.shift;
	}
	@Deprecated
	ILeftNew(Expression e, Instruction next) {
		super(e, next);
		this.shift = 0;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opILeftNew(this);
	}
}

class ICapture extends Instruction {
	int shift;
	ICapture(Expression e, Instruction next) {
		super(e, next);
		this.shift = 0;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opICapture(this);
	}
}

class IReplace extends Instruction {
	public final String value;
	IReplace(Replace e, Instruction next) {
		super(e, next);
		this.value = e.value;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIReplace(this);
	}
	@Override
	protected String getOperand() {
		return StringUtils.quoteString('"', value, '"');
	}
}

class ITag extends Instruction {
	public final Tag tag;
	ITag(Tagging e, Instruction next) {
		super(e, next);
		this.tag = e.tag;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opITag(this);
	}
	@Override
	protected String getOperand() {
		return StringUtils.quoteString('"', tag.name, '"');
	}
}

interface Memoization {

}


class ILookup extends IFailPush implements Memoization {
	final MemoPoint memoPoint;
	final int memoId;
	final boolean node;
	final boolean state;
	final Instruction skip;
	ILookup(Expression e, MemoPoint m, boolean node, boolean state, Instruction next, Instruction skip, Instruction failjump) {
		super(e, failjump, next);
		this.memoPoint = m;
		this.memoId = m.id;
		this.skip = labeling(skip);
		this.node = node;
		this.state = state;
	}
	@Override
	protected String getOperand() {
		return String.valueOf(this.memoPoint.id);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opILookup(this);
	}
}

class IMemoize extends Instruction implements Memoization {
	final MemoPoint memoPoint;
	final int memoId;
	final boolean node;
	final boolean state;
	IMemoize(Expression e, MemoPoint m, boolean node, boolean state, Instruction next) {
		super(e, next);
		this.memoPoint = m;
		this.memoId = m.id;
		this.node = node;
		this.state = state;
	}
	@Override
	protected String getOperand() {
		return String.valueOf(this.memoPoint.id);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIMemoize(this);
	}
}

class IMemoizeFail extends IFail implements Memoization {
	final MemoPoint memoPoint;
	final int memoId;
	final boolean state;
	IMemoizeFail(Expression e, boolean state, MemoPoint m) {
		super(e);
		this.memoPoint = m;
		this.memoId = m.id;
		this.state = state;
	}
	@Override
	protected String getOperand() {
		return String.valueOf(this.memoPoint.id);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIMemoizeFail(this);
	}
}

//class IMonitoredSwitch extends Instruction {
//	final static IMonitoredSwitch dummyMonitor = new IMonitoredSwitch(null, null);
//	boolean isActivated;
//	Instruction activatedNext = null;
//	int used = 0;
//	int stored = 0;
//	IMonitoredSwitch(Expression e, Instruction next) {
//		super(e, next);
//		this.isActivated = true;
//	}
//	void setActivatedNext(Instruction inst) {
//		this.activatedNext = labeling(inst);
//	}
//	@Override
//	Instruction branch() {
//		return this.activatedNext;
//	}
//	final void stored() {
//		stored++;
//		this.checked();
//	}
//	final void used() {
//		used++;
//	}
//	final void checked() {
//		if(this.isActivated) {
//			if(stored % 32 == 0) {
//				double r = used / (double)stored;
//				//System.out.println("monitor: " + this.used + "/" + this.stored + ", " + r);
//				if(r < 0.0361) {  /* this is a magic number */
//					this.isActivated = false;
//				}
//			}
//		}
//	}
//	@Override
//	Instruction exec(Context sc) throws TerminationException {
//		return this.isActivated ? this.activatedNext : this.next;
//	}
//}

//class IStateLookup extends ILookup {
//	IStateLookup(Expression e, IMonitoredSwitch monitor, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
//		super(e, monitor, m, next, skip, failjump);
//	}
//	@Override
//	Instruction exec(Context sc) throws TerminationException {
//		return sc.opIStateLookup(this);
//	}
//}
//
//
//class IStateMemoize extends IMemoize {
//	IStateMemoize(Expression e, IMonitoredSwitch monitor, MemoPoint m, Instruction next) {
//		super(e, monitor, m, next);
//	}
//	@Override
//	Instruction exec(Context sc) throws TerminationException {
//		return sc.opIStateMemoize(this);
//	}
//}
//
//
//class IStateMemoizeFail extends IMemoizeFail {
//	IStateMemoizeFail(Expression e, IMonitoredSwitch monitor, MemoPoint m) {
//		super(e, monitor, m);
//	}
//	@Override
//	Instruction exec(Context sc) throws TerminationException {
//		return sc.opIStateMemoizeFail(this);
//	}
//}
//
//class ILookupNode extends ILookup {
//	final int index;
//	ILookupNode(Link e, IMonitoredSwitch monitor, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
//		super(e, monitor, m, next, skip, failjump);
//		this.index = e.index;
//	}
//	@Override
//	Instruction exec(Context sc) throws TerminationException {
//		return sc.opILookupNode(this);
//	}
//}
//
//class IStateLookupNode extends ILookupNode {
//	final int index;
//	IStateLookupNode(Link e, IMonitoredSwitch monitor, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
//		super(e, monitor, m, next, skip, failjump);
//		this.index = e.index;
//	}
//	@Override
//	Instruction exec(Context sc) throws TerminationException {
//		return sc.opIStateLookupNode(this);
//	}
//}
//
//class IMemoizeNode extends INodeStore implements Memoization {
//	final MemoPoint memoPoint;
//	final IMonitoredSwitch monitor;
//	IMemoizeNode(Link e, IMonitoredSwitch monitor, MemoPoint m, Instruction next) {
//		super(e, next);
//		this.memoPoint = m;
//		this.monitor = monitor;
//	}
//	@Override
//	protected String getOperand() {
//		return String.valueOf(this.memoPoint.id);
//	}
//	@Override
//	Instruction exec(Context sc) throws TerminationException {
//		return sc.opIMemoizeNode(this);
//	}
//}
//
//class IStateMemoizeNode extends IMemoizeNode {
//	IStateMemoizeNode(Link e, IMonitoredSwitch monitor, MemoPoint m, Instruction next) {
//		super(e, monitor, m, next);
//	}
//	@Override
//	Instruction exec(Context sc) throws TerminationException {
//		return sc.opIStateMemoizeNode(this);
//	}
//}

/* Symbol */

class IBeginSymbolScope extends IFailPush {
	IBeginSymbolScope(Block e, Instruction failjump, Instruction next) {
		super(e, failjump, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		sc.opIFailPush(this);
		ContextStack top = sc.newUnusedLocalStack();
		top.pos = sc.getSymbolTable().savePoint();
		return this.next;
	}
}

class IBeginLocalScope extends IFailPush {
	final Tag tableName;
	IBeginLocalScope(LocalTable e, Instruction failjump, Instruction next) {
		super(e, failjump, next);
		this.tableName = e.getTable();
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		sc.opIFailPush(this);
		ContextStack top = sc.newUnusedLocalStack();
		top.pos = sc.getSymbolTable().saveHiddenPoint(tableName);
		return this.next;
	}
}

class IEndSymbolScope extends Instruction {
	final boolean fail;
	IEndSymbolScope(Expression e, boolean fail, Instruction next) {
		super(e, next);
		this.fail = fail;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		ContextStack top = sc.popLocalStack();
		sc.getSymbolTable().rollBack((int)top.pos);
		return (fail) ? sc.opIFail() : sc.opIFailPop(this);
	}
}

class IDefSymbol extends Instruction {
	Tag tableName;
	IDefSymbol(DefSymbol e, Instruction next) {
		super(e, next);
		this.tableName = e.tableName;
	}
	@Override
	protected String getOperand() {
		return tableName.getName();
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		ContextStack top = sc.popLocalStack();
		byte[] captured = sc.subbyte(top.pos, sc.getPosition());
		sc.getSymbolTable().addTable(this.tableName, captured);
		return this.next;
	}
}

class IExistsSymbol extends Instruction {
	Tag tableName;
	IExistsSymbol(ExistsSymbol e, Instruction next) {
		super(e, next);
		this.tableName = e.tableName;
	}
	@Override
	protected String getOperand() {
		return tableName.getName();
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		byte[] t = sc.getSymbolTable().getSymbol(tableName);
		return t != null ? this.next : sc.opIFail();
	}
}

class IIsSymbol extends Instruction {
	Tag tableName;
	IIsSymbol(IsSymbol e, Instruction next) {
		super(e, next);
		this.tableName = e.tableName;
	}
	@Override
	protected String getOperand() {
		return tableName.getName();
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		byte[] t = sc.getSymbolTable().getSymbol(tableName);
		if(t != null && sc.match(sc.getPosition(), t)) {
			sc.consume(t.length);
			return this.next;
		}
		return sc.opIFail();
	}
}

class IIsaSymbol extends Instruction {
	Tag tableName;
	IIsaSymbol(IsSymbol e, Instruction next) {
		super(e, next);
		this.tableName = e.tableName;
	}
	@Override
	protected String getOperand() {
		return tableName.getName();
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		ContextStack top = sc.popLocalStack();
		byte[] captured = sc.subbyte(top.pos, sc.getPosition());
		if(sc.getSymbolTable().contains2(this.tableName, captured)) {
			sc.consume(captured.length);
			return this.next;
			
		}
		return sc.opIFail();
	}
}



class IDefIndent extends Instruction {
	IDefIndent(DefIndent e, Instruction next) {
		super(e, next);
	}
	final long getLineStartPosition(Context sc, long fromPostion) {
		long startIndex = fromPostion;
		if(!(startIndex < sc.length())) {
			startIndex = sc.length() - 1;
		}
		if(startIndex < 0) {
			startIndex = 0;
		}
		while(startIndex > 0) {
			int ch = sc.byteAt(startIndex);
			if(ch == '\n') {
				startIndex = startIndex + 1;
				break;
			}
			startIndex = startIndex - 1;
		}
		return startIndex;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		long pos = sc.getPosition();
		long spos = getLineStartPosition(sc, pos);
		byte[] b = sc.subbyte(spos, pos);
		for(int i = 0; i < b.length; i++) {
			if(b[i] != '\t') {
				b[i] = ' ';
			}
		}
		sc.getSymbolTable().addTable(NezTag.Indent, b);
		return this.next;
	}
}

class IIsIndent extends Instruction {
	IIsIndent(IsIndent e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		long pos = sc.getPosition();
		if(pos > 0) {
			if(sc.byteAt(pos-1) != '\n') {
				return sc.opIFail();
			}
		}
		byte[] b = sc.getSymbolTable().getSymbol(NezTag.Indent);
		if(b != null) {
			if(sc.match(pos, b)) {
				sc.consume(b.length);
				return this.next;
			}
			return sc.opIFail();
		}
		return this.next;  // empty entry is allowable
	}
}



/* Specialization */

class INotByteMap extends Instruction {
	public final boolean[] byteMap;
	INotByteMap(ByteMap e, Instruction next) {
		super(e, next);
		this.byteMap = e.byteMap;
	}
	INotByteMap(ByteChar e, Instruction next) {
		super(e, next);
		this.byteMap = ByteMap.newMap(false);
		this.byteMap[e.byteChar] = true;
	}
	@Override
	protected String getOperand() {
		return StringUtils.stringfyCharacterClass(byteMap);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opNotByteMap(this);
	}
}

class IRepeatedByteMap extends Instruction {
	public final boolean[] byteMap;
	IRepeatedByteMap(ByteMap e, Instruction next) {
		super(e, next);
		this.byteMap = e.byteMap;
	}
	IRepeatedByteMap(ByteChar e, Instruction next) {
		super(e, next);
		this.byteMap = ByteMap.newMap(false);
		this.byteMap[e.byteChar] = true;
	}
	@Override
	protected String getOperand() {
		return StringUtils.stringfyCharacterClass(byteMap);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opRepeatedByteMap(this);
	}
}


class IMultiChar extends Instruction {
	boolean optional = false;
	final byte[] utf8;
	final int    len;
	public IMultiChar(Sequence e, boolean optional, Instruction next) {
		super(e, next);
		this.utf8 = e.extractMultiChar(0, e.size());
		this.len = this.utf8.length;
		this.optional = optional;
	}
	@Override
	protected String getOperand() {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < utf8.length; i++) {
			if(i > 0) {
				sb.append(" ");
			}
			sb.append(StringUtils.stringfyCharacter(utf8[i] & 0xff));
		}
		return sb.toString();
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opMultiChar(this);
	}
}

class INotMultiChar extends IMultiChar {
	INotMultiChar(Sequence e, Instruction next) {
		super(e, false, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opNMultiChar(this);
	}
}
