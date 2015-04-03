package nez.x.nezvm;

import java.util.ArrayList;
import java.util.List;

import nez.expr.Expression;


public abstract class Instruction {
	Opcode op;
	Expression expr;
	BasicBlock bb;
	public Instruction(Expression expr, BasicBlock bb) {
		this.expr = expr;
		this.bb = bb;
		this.bb.append(this);
	}
	
	public Instruction(Expression expr) {
		this.expr = expr;
	}
	
	protected abstract void stringfy(StringBuilder sb);
	
	@Override
	public abstract String toString();
}

class EXIT extends Instruction {
	public EXIT(Expression expr, BasicBlock bb) {
		super(expr, bb);
		this.op = Opcode.EXIT;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  EXIT");
	}

	@Override
	public String toString() {
		return "EXIT";
	}
}

class CALL extends Instruction {
	String ruleName;
	int jumpIndex;
	public CALL(Expression expr, BasicBlock bb, String ruleName) {
		super(expr, bb);
		this.op = Opcode.CALL;
		this.ruleName = ruleName;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  CALL ");
		sb.append(this.ruleName);
	}

	@Override
	public String toString() {
		return "CALL " + this.ruleName + "(" + this.jumpIndex + ")";
	}
}

class RET extends Instruction {
	public RET(Expression expr, BasicBlock bb) {
		super(expr, bb);
		this.op = Opcode.RET;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  RET");
	}

	@Override
	public String toString() {
		return "RET";
	}
}

abstract class JumpInstruction extends Instruction {
	BasicBlock jump;
	public JumpInstruction(Expression expr, BasicBlock bb, BasicBlock jump) {
		super(expr, bb);
		this.jump = jump;
	}
	
	public BasicBlock getJumpPoint() {
		return jump;
	}
}

class JUMP extends JumpInstruction {
	public JUMP(Expression expr, BasicBlock bb, BasicBlock jump) {
		super(expr, bb, jump);
		this.op = Opcode.JUMP;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  JUMP ");
		sb.append("jump:" + this.jump.getBBName());
	}

	@Override
	public String toString() {
		return "JUMP " + this.jump.codeIndex;
	}
}

class CONDBRANCH extends JumpInstruction {
	int val;
	public CONDBRANCH(Expression expr, BasicBlock bb, BasicBlock jump, int val) {
		super(expr, bb, jump);
		this.op = Opcode.CONDBRANCH;
		this.val = val;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		if (this.bb.func.funcName.equals("ATTRIBUTECONTENT")) {
			System.out.println(this.bb.func.indexOf(this.bb));
		}
		sb.append("  CONDBRANCH ");
		sb.append(this.val + " ");
		sb.append("jump:" + this.jump.getBBName());
	}

	@Override
	public String toString() {
		return "CONDBRANCH " + this.jump.codeIndex;
	}
}

class REPCOND extends JumpInstruction {
	public REPCOND(Expression expr, BasicBlock bb, BasicBlock jump) {
		super(expr, bb, jump);
		this.op = Opcode.REPCOND;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  REPCOND ");
		sb.append("jump:" + this.jump.getBBName());
	}

	@Override
	public String toString() {
		return "REPCOND " + this.jump.codeIndex;
	}
}

abstract class MatchingInstruction extends Instruction {
	List<Integer> cdata;
	public MatchingInstruction(Expression expr, BasicBlock bb, int ...cdata) {
		super(expr, bb);
		this.cdata = new ArrayList<Integer>(); 
		for(int i = 0; i < cdata.length; i++) {
			this.cdata.add(cdata[i]);
		}
	}
	
	public MatchingInstruction(Expression expr, int ...cdata) {
		super(expr);
		this.cdata = new ArrayList<Integer>(); 
		for(int i = 0; i < cdata.length; i++) {
			this.cdata.add(cdata[i]);
		}
	}
	
	public MatchingInstruction(Expression expr, BasicBlock bb) {
		super(expr, bb);
		this.cdata = new ArrayList<Integer>(); 
	}
	
	public int size() {
		return this.cdata.size();
	}
	
	public int getc(int index) {
		return this.cdata.get(index);
	}
	
	public void append(int c) {
		this.cdata.add(c);
	}
}

abstract class JumpMatchingInstruction extends MatchingInstruction {
	BasicBlock jump;
	public JumpMatchingInstruction(Expression expr, BasicBlock bb, BasicBlock jump, int ...cdata ) {
		super(expr, bb, cdata);
		this.jump = jump;
	}
	
	public JumpMatchingInstruction(Expression expr, BasicBlock jump, int ...cdata ) {
		super(expr, cdata);
		this.jump = jump;
	}
	
	public JumpMatchingInstruction(Expression expr, BasicBlock bb, BasicBlock jump) {
		super(expr, bb);
		this.jump = jump;
	}
	
	public BasicBlock getJumpPoint() {
		return jump;
	}
}

class CHARRANGE extends JumpMatchingInstruction {
	public CHARRANGE(Expression expr, BasicBlock bb, BasicBlock jump) {
		super(expr, bb, jump);
		this.op = Opcode.CHARRANGE;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  CHARRANGE ");
		for(int i = 0; i < this.size(); i++) {
			sb.append(this.getc(i) + " ");
		}
		sb.append("jump:" + this.jump.getBBName());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("CHARRANGE ");
		for(int i = 0; i < this.size(); i++) {
			sb.append(this.getc(i) + " ");
		}
		sb.append(this.jump.codeIndex);
		return sb.toString();
	}
}

class CHARSET extends JumpMatchingInstruction {
	public CHARSET(Expression expr, BasicBlock bb, BasicBlock jump) {
		super(expr, bb, jump);
		this.op = Opcode.CHARSET;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  CHARSET ");
		for(int i = 0; i < this.size(); i++) {
			sb.append(this.getc(i) + " ");
		}
		sb.append(this.jump.getBBName());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("CHARSET ");
		for(int i = 0; i < this.size(); i++) {
			sb.append(this.getc(i) + " ");
		}
		sb.append(this.jump.codeIndex);
		return sb.toString();
	}
}

class STRING extends JumpMatchingInstruction {
	public STRING(Expression expr, BasicBlock bb,  BasicBlock jump) {
		super(expr, bb, jump);
		this.op = Opcode.STRING;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  STRING ");
		for(int i = 0; i < this.size(); i++) {
			sb.append(this.getc(i) + " ");
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("STRING ");
		for(int i = 0; i < this.size(); i++) {
			sb.append(this.getc(i) + " ");
		}
		sb.append(this.jump.codeIndex);
		return sb.toString();
	}
}

class ANY extends JumpMatchingInstruction {
	public ANY(Expression expr, BasicBlock bb, BasicBlock jump, int ...cdata) {
		super(expr, bb, jump, cdata);
		this.op = Opcode.ANY;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  ANY");
	}

	@Override
	public String toString() {
		return "ANY " + this.jump.codeIndex;
	}
}

abstract class StackOperateInstruction extends Instruction {
	public StackOperateInstruction(Expression expr, BasicBlock bb) {
		super(expr, bb);
	}
}

class PUSHp extends StackOperateInstruction {
	public PUSHp(Expression expr, BasicBlock bb) {
		super(expr, bb);
		this.op = Opcode.PUSHp;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  PUSHp");
	}

	@Override
	public String toString() {
		return "PUSHp";
	}
}

class PUSHo extends StackOperateInstruction {
	public PUSHo(Expression expr, BasicBlock bb) {
		super(expr, bb);
		this.op = Opcode.PUSHconnect;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  PUSHo");
	}

	@Override
	public String toString() {
		return "PUSHo";
	}
}

class POPp extends StackOperateInstruction {
	public POPp(Expression expr, BasicBlock bb) {
		super(expr, bb);
		this.op = Opcode.POPp;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  POPp");
	}

	@Override
	public String toString() {
		return "POPp";
	}
}

class POPo extends StackOperateInstruction {
	public POPo(Expression expr, BasicBlock bb) {
		super(expr, bb);
		this.op = Opcode.POPo;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  POPo");
	}

	@Override
	public String toString() {
		return "POPo";
	}
}

class STOREp extends StackOperateInstruction {
	public STOREp(Expression expr, BasicBlock bb) {
		super(expr, bb);
		this.op = Opcode.STOREp;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  STOREp");
	}

	@Override
	public String toString() {
		return "STOREp";
	}
}

class STOREo extends StackOperateInstruction {
	public STOREo(Expression expr, BasicBlock bb) {
		super(expr, bb);
		this.op = Opcode.STOREo;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  STOREo");
	}

	@Override
	public String toString() {
		return "STOREo";
	}
}

class STOREflag extends Instruction {
	int val;
	public STOREflag(Expression expr, BasicBlock bb, int val) {
		super(expr, bb);
		this.op = Opcode.STOREflag;
		this.val = val;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  STOREflag");
		sb.append(this.val);
	}

	@Override
	public String toString() {
		return "STOREflag " + this.val;
	}
}

class NEW extends Instruction {
	public NEW(Expression expr, BasicBlock bb) {
		super(expr, bb);
		this.op = Opcode.NEW;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  NEW");
	}

	@Override
	public String toString() {
		return "NEW";
	}
	
}

class NEWJOIN extends Instruction {
	int ndata;
	public NEWJOIN(Expression expr, BasicBlock bb, int ndata) {
		super(expr, bb);
		this.op = Opcode.NEWJOIN;
		this.ndata = ndata;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  NEWJOIN ");
		sb.append(this.ndata);
	}

	@Override
	public String toString() {
		return "NEWJOIN " + this.ndata;
	}	
}

class COMMIT extends Instruction {
	int ndata;
	public COMMIT(Expression expr, BasicBlock bb, int ndata) {
		super(expr, bb);
		this.op = Opcode.COMMIT;
		this.ndata = ndata;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  COMMIT ");
		sb.append(this.ndata);
	}

	@Override
	public String toString() {
		return "COMMIT " + this.ndata;
	}
}

class ABORT extends Instruction {
	public ABORT(Expression expr, BasicBlock bb) {
		super(expr, bb);
		this.op = Opcode.ABORT;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  ABORT");
	}

	@Override
	public String toString() {
		return "ABORT";
	}
}

class SETendp extends Instruction {
	public SETendp(Expression expr, BasicBlock bb) {
		super(expr, bb);
		this.op = Opcode.SETendp;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  SETendp");
	}

	@Override
	public String toString() {
		return "SETendp";
	}
}

class TAG extends Instruction {
	String cdata;
	public TAG(Expression expr, BasicBlock bb, String cdata) {
		super(expr, bb);
		this.op = Opcode.TAG;
		this.cdata = cdata;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  TAG ");
		sb.append(this.cdata);
	}

	@Override
	public String toString() {
		return "TAG " + this.cdata;
	}
}

class VALUE extends Instruction {
	String cdata;
	public VALUE(Expression expr, BasicBlock bb, String cdata) {
		super(expr, bb);
		this.op = Opcode.VALUE;
		this.cdata = cdata;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  VALUE ");
		sb.append(this.cdata);
	}

	@Override
	public String toString() {
		return "VALUE " + this.cdata;
	}
}

class MAPPEDCHOICE extends Instruction {
	List<BasicBlock> jumpList;
	public MAPPEDCHOICE(Expression expr, BasicBlock bb) {
		super(expr, bb);
		this.op = Opcode.MAPPEDCHOICE;
		this.jumpList = new ArrayList<BasicBlock>();
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  MAPPEDCHOICE");
	}

	@Override
	public String toString() {
		return "MAPPEDCHOICE";
	}
	
	public MAPPEDCHOICE append(BasicBlock bb) {
		this.jumpList.add(bb);
		return this;
	}
}

class NOTCHAR extends JumpMatchingInstruction {
	public NOTCHAR(Expression expr, BasicBlock bb, BasicBlock jump,
			int... cdata) {
		super(expr, bb, jump, cdata);
		this.op = Opcode.NOTBYTE;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  NOTCHAR ");
		sb.append(this.getc(0));
		sb.append(" jump:" + this.jump.getBBName());
	}

	@Override
	public String toString() {
		return "NOTCHAR " + this.getc(0) + " " + this.jump.codeIndex;
	}
}

class NOTCHARRANGE extends JumpMatchingInstruction {
	public NOTCHARRANGE(Expression expr, BasicBlock bb, BasicBlock jump) {
		super(expr, bb, jump);
		this.op = Opcode.NOTBYTERANGE;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  NOTCHARRANGE ");
		sb.append(this.getc(0) + " ");
		sb.append(this.getc(1) + " ");
		sb.append("jump:" + this.jump.getBBName());
	}

	@Override
	public String toString() {
		return "NOTCHARRANGE " + this.getc(0) + " " + this.getc(1) + " " + this.jump.codeIndex;
	}
}

class NOTCHARSET extends JumpMatchingInstruction {
	public NOTCHARSET(Expression expr, BasicBlock bb, BasicBlock jump) {
		super(expr, bb, jump);
		this.op = Opcode.NOTCHARSET;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  NOTCHARSET ");
		for(int i = 0; i < this.size(); i++) {
			sb.append(this.getc(i) + " ");
		}
		sb.append(this.jump.getBBName());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("NOTCHARSET ");
		for(int i = 0; i < this.size(); i++) {
			sb.append(this.getc(i) + " ");
		}
		sb.append(this.jump.codeIndex);
		return sb.toString();
	}
}

class NOTSTRING extends JumpMatchingInstruction {
	public NOTSTRING(Expression expr, BasicBlock bb, BasicBlock jump) {
		super(expr, bb, jump);
		this.op = Opcode.NOTSTRING;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  NOTSTRING ");
		for(int i = 0; i < this.size(); i++) {
			sb.append(this.getc(i) + " ");
		}
		sb.append(this.jump.getBBName());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("NOTSTRING ");
		for(int i = 0; i < this.size(); i++) {
			sb.append(this.getc(i) + " ");
		}
		sb.append(this.jump.codeIndex);
		return sb.toString();
	}
}

class OPTIONALCHAR extends MatchingInstruction {
	public OPTIONALCHAR(Expression expr, BasicBlock bb, int[] cdata) {
		super(expr, bb, cdata);
		this.op = Opcode.OPTIONALBYTE;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  OPTIONALCHAR ");
		sb.append(this.getc(0));
	}

	@Override
	public String toString() {
		return "OPTIONALCHAR " + this.getc(0);
	}
}

class OPTIONALCHARRANGE extends MatchingInstruction {
	public OPTIONALCHARRANGE(Expression expr, BasicBlock bb, int[] cdata) {
		super(expr, bb, cdata);
		this.op = Opcode.OPTIONALBYTERANGE;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  OPTIONALCHARRANGE ");
		sb.append(this.getc(0) + " ");
		sb.append(this.getc(1));
	}

	@Override
	public String toString() {
		return "OPTIONALCHARRANGE " + this.getc(0) + " " + this.getc(1);
	}
}

class OPTIONALCHARSET extends MatchingInstruction {
	public OPTIONALCHARSET(Expression expr, BasicBlock bb) {
		super(expr, bb);
		this.op = Opcode.OPTIONALCHARSET;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  OPTIONALCHARSET ");
		for(int i = 0; i < this.size(); i++) {
			sb.append(this.getc(i) + " ");
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("OPTIONALCHARSET ");
		for(int i = 0; i < this.size(); i++) {
			sb.append(this.getc(i) + " ");
		}
		return sb.toString();
	}
}

class OPTIONALSTRING extends MatchingInstruction {
	public OPTIONALSTRING(Expression expr, BasicBlock bb) {
		super(expr, bb);
		this.op = Opcode.OPTIONALSTRING;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  OPTIONALSTRING ");
		for(int i = 0; i < this.size(); i++) {
			sb.append(this.getc(i) + " ");
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("OPTIONALSTRING ");
		for(int i = 0; i < this.size(); i++) {
			sb.append(this.getc(i) + " ");
		}
		return sb.toString();
	}
}

class ZEROMORECHARRANGE extends MatchingInstruction {
	public ZEROMORECHARRANGE(Expression expr, BasicBlock bb, int[] cdata) {
		super(expr, bb, cdata);
		this.op = Opcode.ZEROMOREBYTERANGE;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  ZEROMORECHARRANGE ");
		sb.append(this.getc(0) + " ");
		sb.append(this.getc(1));
	}

	@Override
	public String toString() {
		return "ZEROMORECHARRANGE " + this.getc(0) + " " + this.getc(1);
	}
}

class ZEROMORECHARSET extends MatchingInstruction {
	public ZEROMORECHARSET(Expression expr, BasicBlock bb) {
		super(expr, bb);
		this.op = Opcode.ZEROMORECHARSET;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  ZEROMORECHARSET ");
		for(int i = 0; i < this.size(); i++) {
			sb.append(this.getc(i) + " ");
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ZEROMORECHARSET ");
		for(int i = 0; i < this.size(); i++) {
			sb.append(this.getc(i) + " ");
		}
		return sb.toString();
	}
}

class ZEROMOREWS extends Instruction {
	public ZEROMOREWS(Expression expr, BasicBlock bb) {
		super(expr, bb);
		this.op = Opcode.ZEROMOREWS;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  ZEROMOREWS");
	}

	@Override
	public String toString() {
		return "ZEROMOREWS";
	}
}

class NOTCHARANY extends JumpMatchingInstruction {
	public NOTCHARANY(Expression expr, BasicBlock jump,
			int... cdata) {
		super(expr, jump, cdata);
		this.op = Opcode.NOTCHARANY;
	}
	
	public void addBasicBlock(int index, BasicBlock bb) {
		this.bb = bb;
		this.bb.add(index, this);
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  NOTCHARANY ");
		sb.append(this.getc(0));
		sb.append(" jump:" + this.jump.getBBName());
	}

	@Override
	public String toString() {
		return "NOTCHARANY " + this.getc(0) + " " + this.jump.codeIndex;
	}
}

class LOADp1 extends Instruction {
	public LOADp1(Expression expr, BasicBlock bb) {
		super(expr, bb);
		this.op = Opcode.LOADp1;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  LOADp1");
	}

	@Override
	public String toString() {
		return "LOADp1";
	}
}

class LOADp2 extends Instruction {
	public LOADp2(Expression expr, BasicBlock bb) {
		super(expr, bb);
		this.op = Opcode.LOADp2;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  LOADp2");
	}

	@Override
	public String toString() {
		return "LOADp2";
	}
}

class LOADp3 extends Instruction {
	public LOADp3(Expression expr, BasicBlock bb) {
		super(expr, bb);
		this.op = Opcode.LOADp3;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  LOADp3");
	}

	@Override
	public String toString() {
		return "LOADp3";
	}
}

class STOREp1 extends Instruction {
	public STOREp1(Expression expr, BasicBlock bb) {
		super(expr, bb);
		this.op = Opcode.STOREp1;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  STOREp1");
	}

	@Override
	public String toString() {
		return "STOREp1";
	}
}

class STOREp2 extends Instruction {
	public STOREp2(Expression expr, BasicBlock bb) {
		super(expr, bb);
		this.op = Opcode.STOREp2;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  STOREp2");
	}

	@Override
	public String toString() {
		return "STOREp2";
	}
}

class STOREp3 extends Instruction {
	public STOREp3(Expression expr, BasicBlock bb) {
		super(expr, bb);
		this.op = Opcode.STOREp3;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  STOREp3");
	}

	@Override
	public String toString() {
		return "STOREp3";
	}
}