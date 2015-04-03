package nez.runtime;

import java.util.Arrays;

import nez.SourceContext;
import nez.ast.Tag;
import nez.expr.ByteChar;
import nez.expr.ByteMap;
import nez.expr.DefIndent;
import nez.expr.DefSymbol;
import nez.expr.Expression;
import nez.expr.IsIndent;
import nez.expr.IsSymbol;
import nez.expr.Link;
import nez.expr.New;
import nez.expr.Not;
import nez.expr.Prediction;
import nez.expr.Replace;
import nez.expr.Rule;
import nez.expr.Sequence;
import nez.expr.Tagging;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UList;

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
	
	short isAcceptImpl(int ch) {
		return next == null ? Prediction.Accept : this.next.isAcceptImpl(ch);
	}

	boolean isAccept(int ch) {
		return this.isAcceptImpl(ch) == Prediction.Accept;
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
			
	public static boolean run(Instruction code, SourceContext sc) {
		boolean result = false;
		try {
			while(true) {
				code = code.exec(sc);
			}
		}
		catch (TerminationException e) {
			result = e.status;
		}
		return result;
	}

	public static boolean debug(Instruction code, SourceContext sc) {
		boolean result = false;
		String u = "Start";
		UList<String> stack = new UList<String>(new String[128]);
		stack.add("Start");
		try {
			while(true) {
				if(code instanceof ICallPush) {
					stack.add(u);
					u = ((ICallPush)code).rule.getLocalName();
				}
				if(code instanceof IRet) {
					u = stack.ArrayValues[stack.size()-1];
					stack.clear(stack.size()-1);
				}
				ConsoleUtils.println(u + "(" + sc.getPosition() + ")  " + code.id + " " + code);
				code = code.exec(sc);
			}
		}
		catch (TerminationException e) {
			result = e.status;
		}
		return result;
	}
}

interface StackOperation {

}

class IFail extends Instruction implements StackOperation {
	IFail(Expression e) {
		super(e, null);
	}
	@Override
	short isAcceptImpl(int ch) {
		return Prediction.Reject;
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
	short isAcceptImpl(int ch) {
		if(next.isAcceptImpl(ch) != Prediction.Accept) {
			return failjump.isAcceptImpl(ch);
		}
		return Prediction.Accept;
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
	@Override
	short isAcceptImpl(int ch) {
		if(e instanceof Not) {
			short r = e.acceptByte(ch, 0);
			if(r == Prediction.Reject) {
				return r;
			}
		}
		return failjump.isAcceptImpl(ch);
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
	Rule rule;
	public Instruction jump = null;
	ICallPush(Rule rule, Instruction next) {
		super(rule, next);
		this.rule = rule;
	}
	void setResolvedJump(Instruction jump) {
		assert(this.jump == null);
		this.jump = labeling(this.next);
		this.next = labeling(jump);
	}
	@Override
	short isAcceptImpl(int ch) {
		try {
			short r = next.isAcceptImpl(ch);
			if(r == Prediction.Unconsumed) {
				return jump == null ? Prediction.Accept : jump.isAcceptImpl(ch);
			}
			return r;
		}
		catch(StackOverflowError e) {
			//System.out.println(e + " at " + this.rule.getLocalName());
			return Prediction.Accept;
		}
	}

	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opICallPush(this);
	}
	
	@Override
	protected String getOperand() {
		return label(jump) + "   ## " + rule.getLocalName();
	}
}

class IRet extends Instruction implements StackOperation {
	IRet(Rule e) {
		super(e, null);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIRet();
	}
	@Override
	short isAcceptImpl(int ch) {
		return Prediction.Unconsumed;
	}
	@Override
	protected String getOperand() {
		return "  ## " + ((Rule)e).getLocalName();
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
	short isAcceptImpl(int ch) {
		return this.status ? Prediction.Accept : Prediction.Reject;
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
	short isAcceptImpl(int ch) {
		if(ch == Prediction.TextEOF || ch == Prediction.BinaryEOF) {
			return Prediction.Reject;
		}
		return Prediction.Accept;
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
	short isAcceptImpl(int ch) {
		if(ch == Prediction.TextEOF || ch == Prediction.BinaryEOF) {
			return Prediction.Accept;
		}
		return Prediction.Reject;
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
	short isAcceptImpl(int ch) {
		return this.byteChar == ch ? Prediction.Accept : Prediction.Reject;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIByteChar(this);
	}
	@Override
	protected String getOperand() {
		return StringUtils.stringfyByte(byteChar);
	}
}

class IOptionByteChar extends IByteChar {
	IOptionByteChar(ByteChar e, Instruction next) {
		super(e, next);
	}
	@Override
	short isAcceptImpl(int ch) {
		return this.byteChar == ch ? Prediction.Accept : next.isAcceptImpl(ch);
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
	short isAcceptImpl(int ch) {
		return this.byteMap[ch] ? Prediction.Accept : Prediction.Reject;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIByteMap(this);
	}
	@Override
	protected String getOperand() {
		return StringUtils.stringfyCharClass(byteMap);
	}
}

class IOptionByteMap extends IByteMap {
	IOptionByteMap(ByteMap e, Instruction next) {
		super(e, next);
	}
	@Override
	short isAcceptImpl(int ch) {
		return this.byteMap[ch] ? Prediction.Accept : next.isAcceptImpl(ch);
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
	short isAcceptImpl(int ch) {
		return Prediction.Accept;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		sc.consume(1);
		return this.next;
	}
}

class IDfaDispatch extends Instruction {
	Instruction[] jumpTable;
	public IDfaDispatch(Expression e, Instruction next) {
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
	short isAcceptImpl(int ch) {
		return jumpTable[ch].isAcceptImpl(ch);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		int ch = sc.byteAt(sc.getPosition());
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

class IMonitoredSwitch extends Instruction {
	final static IMonitoredSwitch dummyMonitor = new IMonitoredSwitch(null, null);
	boolean isActivated;
	Instruction activatedNext = null;
	int used = 0;
	int stored = 0;
	IMonitoredSwitch(Expression e, Instruction next) {
		super(e, next);
		this.isActivated = true;
	}
	void setActivatedNext(Instruction inst) {
		this.activatedNext = labeling(inst);
	}
	@Override
	Instruction branch() {
		return this.activatedNext;
	}
	final void stored() {
		stored++;
		this.checked();
	}
	final void used() {
		used++;
	}
	final void checked() {
		if(this.isActivated) {
			if(stored % 32 == 0) {
				double r = used / (double)stored;
				//System.out.println("monitor: " + this.used + "/" + this.stored + ", " + r);
				if(r < 0.0361) {  /* this is a magic number */
					this.isActivated = false;
				}
			}
		}
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return this.isActivated ? this.activatedNext : this.next;
	}
}

class ILookup extends IFailPush implements Memoization {
	final MemoPoint memoPoint;
	final Instruction skip;
	final IMonitoredSwitch monitor;
	ILookup(Expression e, IMonitoredSwitch monitor, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
		super(e, failjump, next);
		this.memoPoint = m;
		this.skip = labeling(skip);
		this.monitor = monitor;
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

class IStateLookup extends ILookup {
	IStateLookup(Expression e, IMonitoredSwitch monitor, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
		super(e, monitor, m, next, skip, failjump);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIStateLookup(this);
	}
}

class IMemoize extends Instruction implements Memoization {
	final IMonitoredSwitch monitor;
	final MemoPoint memoPoint;
	IMemoize(Expression e, IMonitoredSwitch monitor, MemoPoint m, Instruction next) {
		super(e, next);
		this.monitor = monitor;
		this.memoPoint = m;
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

class IStateMemoize extends IMemoize {
	IStateMemoize(Expression e, IMonitoredSwitch monitor, MemoPoint m, Instruction next) {
		super(e, monitor, m, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIStateMemoize(this);
	}
}

class IMemoizeFail extends IFail implements Memoization {
	final MemoPoint memoPoint;
	final IMonitoredSwitch monitor;
	IMemoizeFail(Expression e, IMonitoredSwitch monitor, MemoPoint m) {
		super(e);
		this.memoPoint = m;
		this.monitor = monitor;
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

class IStateMemoizeFail extends IMemoizeFail {
	IStateMemoizeFail(Expression e, IMonitoredSwitch monitor, MemoPoint m) {
		super(e, monitor, m);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIStateMemoizeFail(this);
	}
}

class ILookupNode extends ILookup {
	final int index;
	ILookupNode(Link e, IMonitoredSwitch monitor, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
		super(e, monitor, m, next, skip, failjump);
		this.index = e.index;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opILookupNode(this);
	}
}

class IStateLookupNode extends ILookupNode {
	final int index;
	IStateLookupNode(Link e, IMonitoredSwitch monitor, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
		super(e, monitor, m, next, skip, failjump);
		this.index = e.index;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIStateLookupNode(this);
	}
}

class IMemoizeNode extends INodeStore implements Memoization {
	final MemoPoint memoPoint;
	final IMonitoredSwitch monitor;
	IMemoizeNode(Link e, IMonitoredSwitch monitor, MemoPoint m, Instruction next) {
		super(e, next);
		this.memoPoint = m;
		this.monitor = monitor;
	}
	@Override
	protected String getOperand() {
		return String.valueOf(this.memoPoint.id);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIMemoizeNode(this);
	}
}

class IStateMemoizeNode extends IMemoizeNode {
	IStateMemoizeNode(Link e, IMonitoredSwitch monitor, MemoPoint m, Instruction next) {
		super(e, monitor, m, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIStateMemoizeNode(this);
	}
}

/* Symbol */

class IDefSymbol extends Instruction {
	Tag tableName;
	IDefSymbol(DefSymbol e, Instruction next) {
		super(e, next);
		this.tableName = e.table;
	}
	@Override
	protected String getOperand() {
		return tableName.getName();
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIDefSymbol(this);
	}
}

class IIsSymbol extends Instruction {
	Tag tableName;
	boolean checkLastSymbolOnly;
	IIsSymbol(IsSymbol e, boolean checkLastSymbolOnly, Instruction next) {
		super(e, next);
		this.tableName = e.table;
		this.checkLastSymbolOnly = checkLastSymbolOnly;
	}
	@Override
	protected String getOperand() {
		return tableName.getName();
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIIsSymbol(this);
	}
}

class IDefIndent extends Instruction {
	IDefIndent(DefIndent e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIDefIndent(this);
	}
}

class IIsIndent extends Instruction {
	IIsIndent(IsIndent e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIIsIndent(this);
	}
}

class ITablePush extends Instruction {
	ITablePush(Expression e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opITablePush(this);
	}
}

class ITablePop extends Instruction {
	public ITablePop(Expression e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opITablePop(this);
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
	short isAcceptImpl(int ch) {
		return this.byteMap[ch] ? Prediction.Reject : next.isAcceptImpl(ch);
	}
	@Override
	protected String getOperand() {
		return StringUtils.stringfyCharClass(byteMap);
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
	short isAcceptImpl(int ch) {
		return this.byteMap[ch] ? Prediction.Accept : next.isAcceptImpl(ch);
	}
	@Override
	protected String getOperand() {
		return StringUtils.stringfyCharClass(byteMap);
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
	short isAcceptImpl(int ch) {
		return (utf8[0] & 0xff) == ch ? Prediction.Accept : Prediction.Reject;
	}
	@Override
	protected String getOperand() {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < utf8.length; i++) {
			if(i > 0) {
				sb.append(" ");
			}
			sb.append(StringUtils.stringfyByte(utf8[i] & 0xff));
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
	short isAcceptImpl(int ch) {
		return Prediction.Unconsumed;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opNMultiChar(this);
	}
}
