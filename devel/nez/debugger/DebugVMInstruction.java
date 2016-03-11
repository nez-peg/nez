package nez.debugger;

import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.Nez;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.util.StringUtils;

public abstract class DebugVMInstruction {
	Opcode op;
	Expression expr;
	DebugVMInstruction next;

	public DebugVMInstruction(Expression e) {
		this.expr = e;
	}

	public Expression getExpression() {
		return this.expr;
	}

	public void setNextInstruction(DebugVMInstruction next) {
		this.next = next;
	}

	public abstract void stringfy(StringBuilder sb);

	@Override
	public abstract String toString();

	public abstract DebugVMInstruction exec(Context ctx) throws MachineExitException;
}

class Iexit extends DebugVMInstruction {
	public Iexit(Expression e) {
		super(e);
		this.op = Opcode.Iexit;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Iexit");
	}

	@Override
	public String toString() {
		return "Iexit";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIexit(this);
	}
}

class Inop extends DebugVMInstruction {
	Production p;

	public Inop(Production e) {
		super(null);
		this.p = e;
		this.op = Opcode.Inop;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Inop");
	}

	@Override
	public String toString() {
		return "Inop";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return this.next;
	}
}

abstract class JumpInstruction extends DebugVMInstruction {
	BasicBlock jumpBB;
	DebugVMInstruction jump;

	public JumpInstruction(Expression e, BasicBlock jump) {
		super(e);
		this.jumpBB = jump;
	}

	public BasicBlock getJumpBB() {
		return this.jumpBB;
	}
}

class Icall extends JumpInstruction {
	NonTerminal ne;
	int jumpPoint;
	BasicBlock failBB;
	DebugVMInstruction failjump;

	public Icall(NonTerminal e, BasicBlock jumpBB, BasicBlock failjumpBB) {
		super(e, jumpBB);
		this.op = Opcode.Icall;
		this.ne = e;
		this.failBB = failjumpBB;
	}

	public void setJump(int jump) {
		this.jumpPoint = jump;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Icall " + ne.getLocalName() + " jmp:" + this.jumpBB.name + " fail:" + this.failBB.name);
	}

	@Override
	public String toString() {
		return "Icall " + ne.getLocalName() + " (" + this.jumpPoint + ", " + this.failBB.codePoint + ")";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIcall(this);
	}
}

class Iret extends DebugVMInstruction {
	Production p;

	public Iret(Production e) {
		super(e.getExpression());
		this.p = e;
		this.op = Opcode.Iret;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Iret");
	}

	@Override
	public String toString() {
		return "Iret";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIret(this);
	}
}

class Ijump extends JumpInstruction {

	public Ijump(Expression e, BasicBlock jump) {
		super(e, jump);
		this.op = Opcode.Ijump;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Ijump (").append(this.jumpBB.getName()).append(")");
	}

	@Override
	public String toString() {
		return "Ijump (" + this.jumpBB.codePoint + ")";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIjump(this);
	}
}

class Iiffail extends JumpInstruction {
	public Iiffail(Expression e, BasicBlock jump) {
		super(e, jump);
		this.op = Opcode.Iiffail;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Iiffail (").append(this.jumpBB.getName()).append(")");
	}

	@Override
	public String toString() {
		return "Iiffail (" + this.jumpBB.codePoint + ")";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIiffail(this);
	}
}

class Ipush extends DebugVMInstruction {
	public Ipush(Expression e) {
		super(e);
		this.op = Opcode.Ipush;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Ipush");
	}

	@Override
	public String toString() {
		return "Ipush";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIpush(this);
	}
}

class Ipop extends DebugVMInstruction {
	public Ipop(Expression e) {
		super(e);
		this.op = Opcode.Ipop;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Ipop");
	}

	@Override
	public String toString() {
		return "Ipop";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIpop(this);
	}
}

class Ipeek extends DebugVMInstruction {
	public Ipeek(Expression e) {
		super(e);
		this.op = Opcode.Ipeek;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Ipeek");
	}

	@Override
	public String toString() {
		return "Ipeek";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIpeek(this);
	}
}

class Isucc extends DebugVMInstruction {
	public Isucc(Expression e) {
		super(e);
		this.op = Opcode.Isucc;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Isucc");
	}

	@Override
	public String toString() {
		return "Isucc";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIsucc(this);
	}
}

class Ifail extends DebugVMInstruction {
	public Ifail(Expression e) {
		super(e);
		this.op = Opcode.Ifail;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Ifail");
	}

	@Override
	public String toString() {
		return "Ifail";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIfail(this);
	}
}

class Ichar extends JumpInstruction {
	int byteChar;

	public Ichar(Nez.Byte e, BasicBlock jump) {
		super(e, jump);
		this.op = Opcode.Ichar;
		this.byteChar = e.byteChar;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Ichar ").append(StringUtils.stringfyByte(this.byteChar)).append(" ").append(this.jumpBB.getName());
	}

	@Override
	public String toString() {
		return "Ichar " + StringUtils.stringfyByte(this.byteChar) + " (" + this.jumpBB.codePoint + ")";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIchar(this);
	}
}

class Istr extends JumpInstruction {
	byte[] utf8;

	public Istr(Expression e, BasicBlock jump, byte[] utf8) {
		super(e, jump);
		this.op = Opcode.Istr;
		this.utf8 = utf8;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Istr ").append(this.jumpBB.getName());
	}

	@Override
	public String toString() {
		return "Istr " + " (" + this.jumpBB.codePoint + ")";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIstr(this);
	}
}

class Icharclass extends JumpInstruction {
	boolean[] byteMap;

	public Icharclass(Nez.ByteSet e, BasicBlock jump) {
		super(e, jump);
		this.op = Opcode.Icharclass;
		this.byteMap = e.byteset;
	}

	public Icharclass(Expression e, BasicBlock jump, boolean[] byteMap) {
		super(e, jump);
		this.op = Opcode.Icharclass;
		this.byteMap = byteMap;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Icharclass ").append(StringUtils.stringfyByteSet(this.byteMap)).append(" ").append(this.jumpBB.getName());
	}

	@Override
	public String toString() {
		return "Icharclass " + StringUtils.stringfyByteSet(this.byteMap) + " (" + this.jumpBB.codePoint + ")";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIcharclass(this);
	}
}

class Iany extends JumpInstruction {
	public Iany(Expression e, BasicBlock jump) {
		super(e, jump);
		this.op = Opcode.Iany;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Iany ").append(this.jumpBB.getName());
	}

	@Override
	public String toString() {
		return "Iany (" + this.jumpBB.codePoint + ")";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIany(this);
	}
}

class Inew extends DebugVMInstruction {
	public Inew(Expression e) {
		super(e);
		this.op = Opcode.Inew;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Inew");
	}

	@Override
	public String toString() {
		return "Inew";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opInew(this);
	}
}

class Ileftnew extends DebugVMInstruction {
	int index;

	public Ileftnew(Nez.FoldTree e) {
		super(e);
		this.op = Opcode.Ileftnew;
		this.index = e.shift;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Ileftnew ").append(this.index);
	}

	@Override
	public String toString() {
		return "Ileftnew " + this.index;
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIleftnew(this);
	}
}

class Icapture extends DebugVMInstruction {
	public Icapture(Expression e) {
		super(e);
		this.op = Opcode.Icapture;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Icapture");
	}

	@Override
	public String toString() {
		return "Icapture";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIcapture(this);
	}
}

class Imark extends DebugVMInstruction {
	public Imark(Expression e) {
		super(e);
		this.op = Opcode.Imark;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Imark");
	}

	@Override
	public String toString() {
		return "Imark";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opImark(this);
	}
}

class Itag extends DebugVMInstruction {
	Symbol tag;

	public Itag(Nez.Tag e) {
		super(e);
		this.op = Opcode.Itag;
		this.tag = e.tag;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Itag " + tag.toString());
	}

	@Override
	public String toString() {
		return "Itag " + tag.toString();
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opItag(this);
	}
}

class Ireplace extends DebugVMInstruction {
	String value;

	public Ireplace(Nez.Replace e) {
		super(e);
		this.op = Opcode.Ireplace;
		this.value = e.value;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Ireplace ").append(this.value);
	}

	@Override
	public String toString() {
		return "Ireplace " + this.value;
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIreplace(this);
	}
}

class Icommit extends DebugVMInstruction {
	int index;

	public Icommit(Nez.LinkTree e) {
		super(e);
		this.op = Opcode.Icommit;
		this.index = -1;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Icommit ").append(this.index);
	}

	@Override
	public String toString() {
		return "Icommit " + this.index;
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIcommit(this);
	}
}

class Iabort extends DebugVMInstruction {
	public Iabort(Expression e) {
		super(e);
		this.op = Opcode.Iabort;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Iabort");
	}

	@Override
	public String toString() {
		return "Iabort";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIabort(this);
	}
}

class Idef extends DebugVMInstruction {
	Symbol tableName;

	public Idef(Nez.SymbolAction e) {
		super(e);
		this.op = Opcode.Idef;
		this.tableName = e.tableName;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Idef ").append(this.tableName.toString());
	}

	@Override
	public String toString() {
		return "Idef " + this.tableName.toString();
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIdef(this);
	}
}

class Iis extends JumpInstruction {
	Symbol tableName;

	public Iis(Nez.SymbolPredicate e, BasicBlock jumpBB) {
		super(e, jumpBB);
		this.op = Opcode.Iis;
		this.tableName = e.tableName;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Iis ").append(this.tableName.toString()).append(" ").append(this.jumpBB.getName());
	}

	@Override
	public String toString() {
		return "Iis " + this.tableName.toString() + " (" + this.jumpBB.codePoint + ")";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIis(this);
	}
}

class Iisa extends JumpInstruction {
	Symbol tableName;

	public Iisa(Nez.SymbolPredicate e, BasicBlock jumpBB) {
		super(e, jumpBB);
		this.op = Opcode.Iisa;
		this.tableName = e.tableName;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Iisa ").append(this.tableName.toString()).append(" ").append(this.jumpBB.getName());
	}

	@Override
	public String toString() {
		return "Iisa " + this.tableName.toString() + " (" + this.jumpBB.codePoint + ")";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIisa(this);
	}
}

class Iexists extends JumpInstruction {
	Symbol tableName;

	public Iexists(Nez.SymbolExists e, BasicBlock jumpBB) {
		super(e, jumpBB);
		this.op = Opcode.Iexists;
		this.tableName = e.tableName;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Iexists ").append(this.tableName.toString());
	}

	@Override
	public String toString() {
		return "Iexists " + this.tableName.toString();
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIexists(this);
	}
}

class Ibeginscope extends DebugVMInstruction {
	public Ibeginscope(Expression e) {
		super(e);
		this.op = Opcode.Ibeginscope;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Ibeginscope");
	}

	@Override
	public String toString() {
		return "Ibeginscope";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIbeginscope(this);
	}
}

class Ibeginlocalscope extends DebugVMInstruction {
	Symbol tableName;

	public Ibeginlocalscope(Nez.LocalScope e) {
		super(e);
		this.op = Opcode.Ibeginlocalscope;
		this.tableName = e.tableName;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Ibeginlocalscope ").append(this.tableName.toString());
	}

	@Override
	public String toString() {
		return "Ibeginlocalscope " + this.tableName.toString();
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIbeginlocalscope(this);
	}
}

class Iendscope extends DebugVMInstruction {
	public Iendscope(Expression e) {
		super(e);
		this.op = Opcode.Iendscope;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Iendscope");
	}

	@Override
	public String toString() {
		return "Iendscope";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIendscope(this);
	}
}

abstract class AltInstruction extends DebugVMInstruction {

	public AltInstruction(Expression e) {
		super(e);
	}

}

class Ialtstart extends AltInstruction {

	public Ialtstart(Expression e) {
		super(e);
		this.op = Opcode.Ialtstart;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Ialtstart");
	}

	@Override
	public String toString() {
		return "Ialtstart";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIaltstart(this);
	}

}

class Ialt extends AltInstruction {

	public Ialt(Expression e) {
		super(e);
		this.op = Opcode.Ialt;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Ialt");
	}

	@Override
	public String toString() {
		return "Ialt";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIalt(this);
	}

}

class Ialtend extends AltInstruction {
	boolean last;
	int index;
	Nez.Choice c;

	public Ialtend(Nez.Choice e, boolean last, int index) {
		super(e.get(index));
		this.c = e;
		this.op = Opcode.Ialtend;
		this.last = last;
		this.index = index;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Ialtend");
	}

	@Override
	public String toString() {
		return "Ialtend";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIaltend(this);
	}

}

class Ialtfin extends AltInstruction {

	public Ialtfin(Expression e) {
		super(e);
		this.op = Opcode.Ialtfin;
	}

	@Override
	public void stringfy(StringBuilder sb) {
		sb.append("Ialtfin");
	}

	@Override
	public String toString() {
		return "Ialtfin";
	}

	@Override
	public DebugVMInstruction exec(Context ctx) throws MachineExitException {
		return ctx.opIaltfin(this);
	}

}