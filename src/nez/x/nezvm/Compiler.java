package nez.x.nezvm;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import nez.Grammar;
import nez.Production;
import nez.expr.And;
import nez.expr.AnyChar;
import nez.expr.ByteChar;
import nez.expr.ByteMap;
import nez.expr.Choice;
import nez.expr.Empty;
import nez.expr.Expression;
import nez.expr.Factory;
import nez.expr.Failure;
import nez.expr.GrammarVisitor;
import nez.expr.Link;
import nez.expr.NonTerminal;
import nez.expr.Not;
import nez.expr.Option;
import nez.expr.Prediction;
import nez.expr.Repetition;
import nez.expr.Replace;
import nez.expr.Rule;
import nez.expr.Sequence;
import nez.expr.Tagging;
import nez.expr.Unary;
import nez.util.UList;


public class Compiler extends GrammarVisitor {
	
	// Kind of optimize
	boolean O_Inlining = false;
	boolean O_MappedChoice = false;
	boolean O_FusionInstruction = false;
	boolean O_FusionOperand = false;
	boolean O_StackCaching = false;
	Optimizer optimizer;
	
	boolean optChoiceMode = true;
	boolean inlining = false;
	
	boolean PatternMatching = false;
	
	Grammar peg;
	Module module;
	Function func;
	BasicBlock currentBB;
	
	public Compiler(int level) {
		this.module = new Module();
		switch (level) {
		case 1:
			O_Inlining = true;
			break;
		case 2:
			O_FusionInstruction = true;
			break;
		case 3:
			O_StackCaching = true;
			break;
		case 4:
			O_MappedChoice = true;
			break;
		case 5:
			O_Inlining = true;
			O_FusionInstruction = true;
			O_FusionOperand = true;
			break;
		case 6:
			O_Inlining = true;
			O_FusionInstruction = true;
			O_FusionOperand = true;
			O_StackCaching = true;
			break;
		case 7:
			O_Inlining = true;
			O_FusionInstruction = true;
			O_MappedChoice = true;
			O_FusionOperand = true;
			O_StackCaching = true;
			break;
		case 8:
			O_Inlining = true;
			O_FusionInstruction = true;
			O_MappedChoice = true;
			O_FusionOperand = true;
			O_StackCaching = true;
			PatternMatching = true;
			break;
		case 9:
			O_FusionInstruction = true;
			O_MappedChoice = true;
			O_FusionOperand = true;
			O_StackCaching = true;
			break;
		case 10:
			O_FusionInstruction = true;
			O_MappedChoice = true;
			O_FusionOperand = true;
			O_StackCaching = true;
			PatternMatching = true;
			break;
		default:
			break;
		}
		this.optimizer = new Optimizer(this.module, O_Inlining, O_MappedChoice, O_FusionInstruction, O_FusionOperand, O_StackCaching);
	}
	
	int codeIndex;
	
	public void writeByteCode(String grammerfileName, String outputFileName, Grammar peg) {
		//generateProfileCode(peg);
		//System.out.println("choiceCase: " + choiceCaseCount + "\nconstructor: " + constructorCount);
		byte[] byteCode = new byte[this.codeIndex * 256];
		int pos = 0;
		// Version of the specification (2 byte)
		byte[] version = new byte[2];
		version[0] = 0;
		version[1] = 1;
		byteCode[pos] = version[0];
		pos++;
		byteCode[pos] = version[1];
		pos++;
		
		// Length of grammerfileName (4 byte)
		int fileNamelen = grammerfileName.length();
		pos = write32(byteCode, fileNamelen, pos);
		
		// GrammerfileName (n byte)
		byte[] name = grammerfileName.getBytes();
		for (int i = 0; i < fileNamelen; i++) {
			byteCode[pos] = name[i];
			pos++;
		}
		
		// pool_size_info
		int poolSizeInfo = 1064;
		pos = write32(byteCode, poolSizeInfo, pos);
		
		// rule table
		int ruleSize = this.module.size();
		pos = write32(byteCode, ruleSize, pos);
		for(int i = 0; i < this.module.size(); i++) {
			Function func = this.module.get(i);
			byte[] ruleName = func.funcName.getBytes();
			int ruleNamelen = ruleName.length;
			long entryPoint = func.get(0).codeIndex;
			pos = write32(byteCode, ruleNamelen, pos);
			for(int j = 0; j < ruleName.length; j++) {
				byteCode[pos] = ruleName[j];
				pos++;
			}
			pos = write64(byteCode, entryPoint, pos);
		}

		int bytecodelen_pos = pos;
		pos = pos + 8;
		
		int index = 0;
		// byte code (m byte)
		for(int i = 0; i < this.module.size(); i++) {
			Function func = this.module.get(i);
			for(int j = 0; j < func.size(); j++) {
				BasicBlock bb = func.get(j);
				for(int k = 0; k < bb.size(); k++) {
					Instruction inst = bb.get(k);
					pos = writeOpcode(inst, byteCode, pos, index);
					index++;
				}
			}
		}
		// Length of byte code (8 byte) 
		long byteCodelength = this.codeIndex;
		pos = bytecodelen_pos;
		write64(byteCode, byteCodelength, pos);
		
		try {
			if (outputFileName == null) {
				System.out.println("unspecified outputfile");
				System.exit(0);
			}
			FileOutputStream fos = new FileOutputStream(outputFileName);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			bos.write(byteCode);
			bos.flush();
			bos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	private int write16(byte[] byteCode, long num, int pos) {
		byteCode[pos] = (byte) (0x000000ff & num);
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (num >> 8));
		pos++;
		return pos;
	}

	private int write32(byte[] byteCode, long num, int pos) {
		byteCode[pos] = (byte) (0x000000ff & (num));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (num >> 8));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (num >> 16));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (num >> 24));
		pos++;
		return pos;
	}
	
	private int write64(byte[] byteCode, long num, int pos) {
		pos = write32(byteCode, num, pos);
		pos = write32(byteCode, num >> 32, pos);
		return pos;
	}

	private int writeCdataByteCode(byte[] byteCode, String cdata, int pos) {
		int j = 0;
		pos = write16(byteCode, cdata.length(), pos);
		byte[] nameByte = cdata.getBytes();
		while (j < cdata.length()) {
			byteCode[pos] = nameByte[j];
			j++;
			pos++;
		}
		return pos;
	}

	private int writeOpcode(Instruction code, byte[] byteCode, int pos, int index) {
		byteCode[pos] = (byte)code.op.ordinal();
		pos++;
		switch (code.op) {
		case JUMP:
			pos = write32(byteCode, ((JUMP)code).jump.codeIndex-index, pos);
			break;
		case CALL:
			//pos = write32(byteCode, code.jump-index, pos);
			pos = write32(byteCode, ((CALL)code).jumpIndex-index, pos);
			break;
		case CONDBRANCH:
			pos = write32(byteCode, ((CONDBRANCH)code).val, pos);
			pos = write32(byteCode, ((CONDBRANCH)code).jump.codeIndex-index, pos);
			break;
		case REPCOND:
			pos = write32(byteCode, ((REPCOND)code).jump.codeIndex-index, pos);
			break;
		case CHARRANGE:
			pos = write32(byteCode, ((CHARRANGE)code).jump.codeIndex-index, pos);
			pos = write32(byteCode, ((CHARRANGE)code).getc(0), pos);
			pos = write32(byteCode, ((CHARRANGE)code).getc(1), pos);
			break;
		case CHARSET:
			CHARSET charset = (CHARSET)code;
			pos = write16(byteCode, charset.size(), pos);
			for(int j = 0; j < charset.size(); j++){
				pos = write32(byteCode, charset.getc(j), pos);
			}
			pos = write32(byteCode, charset.jump.codeIndex-index, pos);
			break;
		case STRING:
			pos = write32(byteCode, ((STRING)code).jump.codeIndex-index, pos);
			pos = write16(byteCode, ((STRING)code).size(), pos);
			for(int j = 0; j < ((STRING)code).size(); j++){
				pos = write32(byteCode, ((STRING)code).getc(j), pos);
			}
			break;
		case ANY:
			pos = write32(byteCode, ((ANY)code).jump.codeIndex-index, pos);
			break;
		case STOREflag:
			pos = write32(byteCode, ((STOREflag)code).val, pos);
			break;
		case NEWJOIN:
			pos = write32(byteCode, ((NEWJOIN)code).ndata, pos);
			break;
		case COMMIT:
			pos = write32(byteCode, ((COMMIT)code).ndata, pos);
			break;
		case TAG:
			pos = writeCdataByteCode(byteCode, ((TAG)code).cdata, pos);
			break;
		case VALUE:
			pos = writeCdataByteCode(byteCode, ((VALUE)code).cdata, pos);
			break;
		case MAPPEDCHOICE:
			MAPPEDCHOICE m = (MAPPEDCHOICE)code;
			for(int j = 0; j < 256; j++){
				pos = write32(byteCode, m.jumpList.get(j).codeIndex-index, pos);
			}
			break;
//		case SCAN:
//			pos = write32(byteCode, code.get(0), pos);
//			pos = write32(byteCode, code.get(1), pos);
//			break;
//		case CHECKEND:
//			pos = write32(byteCode, code.get(0), pos);
//			pos = write32(byteCode, code.jump-index, pos);
//			break;
//		case DEF:
//			pos = write32(byteCode, code.get(0), pos);
//			break;
//		case IS:
//			pos = write32(byteCode, code.get(0), pos);
//			break;
//		case ISA:
//			pos = write32(byteCode, code.get(0), pos);
//			break;
//		case BLOCKSTART:
//			pos = write32(byteCode, code.get(0), pos);
//			break;
//		case INDENT:
//			pos = write32(byteCode, code.get(0), pos);
//			break;
		case NOTBYTE:
			NOTCHAR nc = (NOTCHAR)code;
			pos = write32(byteCode, nc.getc(0), pos);
			pos = write32(byteCode, nc.jump.codeIndex-index, pos);
			break;
		case NOTCHARSET:
			NOTCHARSET ncs = (NOTCHARSET)code;
			pos = write16(byteCode, ncs.size(), pos);
			for(int j = 0; j < ncs.size(); j++){
				pos = write32(byteCode, ncs.getc(j), pos);
			}
			pos = write32(byteCode, ncs.jump.codeIndex-index, pos);
			break;
		case NOTBYTERANGE:
			NOTCHARRANGE ncr = (NOTCHARRANGE)code;
			pos = write32(byteCode, ncr.getc(0), pos);
			pos = write32(byteCode, ncr.getc(1), pos);
			pos = write32(byteCode, ncr.jump.codeIndex-index, pos);
			break;
		case NOTSTRING:
			NOTSTRING ns = (NOTSTRING)code;
			pos = write32(byteCode, ns.jump.codeIndex-index, pos);
			pos = write16(byteCode, ns.size(), pos);
			for(int j = 0; j < ns.size(); j++){
				pos = write32(byteCode, ns.getc(j), pos);
			}
			break;
		case OPTIONALBYTE:
			pos = write32(byteCode, ((OPTIONALCHAR)code).getc(0), pos);
			break;
		case OPTIONALCHARSET:
			OPTIONALCHARSET ocs = (OPTIONALCHARSET)code;
			pos = write16(byteCode, ocs.size(), pos);
			for(int j = 0; j < ocs.size(); j++){
				pos = write32(byteCode, ocs.getc(j), pos);
			}
			break;
		case OPTIONALBYTERANGE:
			OPTIONALCHARRANGE ocr = (OPTIONALCHARRANGE)code;
			pos = write32(byteCode, ocr.getc(0), pos);
			pos = write32(byteCode, ocr.getc(1), pos);
			break;
		case OPTIONALSTRING:
			OPTIONALSTRING os = (OPTIONALSTRING)code;
			pos = write16(byteCode, os.size(), pos);
			for(int j = 0; j < os.size(); j++){
				pos = write32(byteCode, os.getc(j), pos);
			}
			break;
		case ZEROMOREBYTERANGE:
			ZEROMORECHARRANGE zcr = (ZEROMORECHARRANGE)code;
			pos = write32(byteCode, zcr.getc(0), pos);
			pos = write32(byteCode, zcr.getc(1), pos);
			break;
		case ZEROMORECHARSET:
			ZEROMORECHARSET zcs = (ZEROMORECHARSET)code;
			pos = write16(byteCode, zcs.size(), pos);
			for(int j = 0; j < zcs.size(); j++){
				pos = write32(byteCode, zcs.getc(j), pos);
			}
			break;
		case NOTCHARANY:
			NOTCHARANY nca = (NOTCHARANY)code;
			pos = write32(byteCode, nca.getc(0), pos);
			pos = write32(byteCode, nca.jump.codeIndex-index, pos);
			break;
//		case REPEATANY:
//			pos = write32(byteCode, code.get(0), pos);
//			break;
		default:
			break;
		}
		return pos;
	}
	
	class FailureBB {
		BasicBlock fbb;
		FailureBB prev;
		public FailureBB(BasicBlock bb, FailureBB prev) {
			this.fbb = bb;
			this.prev = prev;
		}
	}
	
	FailureBB fLabel = null;
	private void pushFailureJumpPoint(BasicBlock bb) {
		this.fLabel = new FailureBB(bb, this.fLabel);
	}
	
	private void popFailureJumpPoint(Rule r) {
		this.fLabel = this.fLabel.prev;
	}
	
	private void popFailureJumpPoint(Expression e) {
		this.fLabel = this.fLabel.prev;
	}
	
	private BasicBlock jumpFailureJump() {
		return this.fLabel.fbb;
	}
	
	private BasicBlock jumpPrevFailureJump() {
		return this.fLabel.prev.fbb;
	}
	
//	@Override
//	public void formatGrammar(Grammar peg, StringBuilder sb) {
//		this.peg = peg;
//		this.formatHeader();
//		for(Rule r: peg.getRuleList()) {
//			if (r.getLocalName().equals("File")) {
//				this.formatRule(r, sb);
//				break;
//			}
//		}
//		for(Rule r: peg.getRuleList()) {
//			if (!r.getLocalName().equals("File")) {
//				if (!r.getLocalName().startsWith("\"")) {
//					this.formatRule(r, sb);
//				}
//			}
//		}
//		this.formatFooter();
//	}
	
	StringBuilder sb;

	public void formatHeader() {
		System.out.println("\nGenerate Byte Code\n");
		//writeCode(Instruction.EXIT);
		Function func = new Function(this.module, "EXIT");
		BasicBlock bb = new BasicBlock(func);
		this.createEXIT(null, bb);
	}
	
	public void formatFooter() {
		System.out.println(this.module.stringfy(this.sb));
		this.optimizer.optimize();
		this.labeling();
		this.dumpLastestCode();
	}
	
	HashMap<String, Integer> callMap = new HashMap<String, Integer>();
	private void labeling() {
		int codeIndex = 0;
		for(int i = 0; i < this.module.size(); i++) {
			Function func = this.module.get(i);
			this.callMap.put(func.funcName, codeIndex);
			for(int j = 0; j < func.size(); j++) {
				BasicBlock bb = func.get(j);
				bb.codeIndex = codeIndex;
				codeIndex += bb.size();
			}
		}
		this.codeIndex = codeIndex;
		for(int i = 0; i < this.module.size(); i++) {
			Function func = this.module.get(i);
			for(int j = 0; j < func.size(); j++) {
				BasicBlock bb = func.get(j);
				for(int k = 0; k < bb.size(); k++) {
					Instruction inst = bb.get(k);
					if (inst instanceof CALL) {
						CALL cinst = (CALL)inst;
						cinst.jumpIndex = this.callMap.get(cinst.ruleName);
					}
				}
			}
		}
	}
	
	private void dumpLastestCode() {
		int codeIndex = 0;
		for(int i = 0; i < this.module.size(); i++) {
			Function func = this.module.get(i);
			for(int j = 0; j < func.size(); j++) {
				BasicBlock bb = func.get(j);
				for(int k = 0; k < bb.size(); k++) {
					Instruction inst = bb.get(k);
					System.out.println("[" + codeIndex + "] " + inst.toString());
					codeIndex++;
				}
			}
		}
	}
	
	public BasicBlock getCurrentBasicBlock() {
		return this.currentBB;
	}
	
	public void setCurrentBasicBlock(BasicBlock bb) {
		this.currentBB = bb;
	}
	
	private Instruction createEXIT(Expression e, BasicBlock bb) {
		return new EXIT(e, bb);
	}
	
	private Instruction createJUMP(Expression e, BasicBlock bb, BasicBlock jump) {
		return new JUMP(e, bb, jump);
	}
	
	private Instruction createCALL(Expression e, BasicBlock bb, String name) {
		return new CALL(e, bb, name);
	}
	
	private Instruction createRET(Expression e, BasicBlock bb) {
		return new RET(e, bb);
	}
	
	private Instruction createCONDBRANCH(Expression e, BasicBlock bb, BasicBlock jump, int val) {
		return new CONDBRANCH(e, bb, jump, val);
	}
	
	private Instruction createREPCOND(Expression e, BasicBlock bb, BasicBlock jump) {
		return new REPCOND(e, bb, jump);
	}
	
	private CHARRANGE createCHARRANGE(Expression e, BasicBlock bb, BasicBlock jump) {
		return new CHARRANGE(e, bb, jump);
	}
	
	private CHARSET createCHARSET(Expression e, BasicBlock bb, BasicBlock jump) {
		return new CHARSET(e, bb, jump);
	}
	
	private STRING createSTRING(Expression e, BasicBlock bb, BasicBlock jump) {
		return new STRING(e, bb, jump);
	}
	
	private Instruction createANY(Expression e, BasicBlock bb, BasicBlock jump) {
		return new ANY(e, bb, jump);
	}
	
	private Instruction createPUSHo(Expression e, BasicBlock bb) {
		return new PUSHo(e, bb);
	}
	
	private Instruction createPUSHp(Expression e, BasicBlock bb) {
		return new PUSHp(e, bb);
	}
	
	private Instruction createPOPo(Expression e, BasicBlock bb) {
		return new POPo(e, bb);
	}
	
	private Instruction createPOPp(Expression e, BasicBlock bb) {
		return new POPp(e, bb);
	}
	
	private Instruction createSTOREo(Expression e, BasicBlock bb) {
		return new STOREo(e, bb);
	}
	private Instruction createSTOREp(Expression e, BasicBlock bb) {
		return new STOREp(e, bb);
	}
	
	private Instruction createSTOREflag(Expression e, BasicBlock bb, int val) {
		return new STOREflag(e, bb, val);
	}
	
	private Instruction createSETendp(Expression e, BasicBlock bb) {
		return new SETendp(e, bb);
	}
	
	private Instruction createNEW(Expression e, BasicBlock bb) {
		return new NEW(e, bb);
	}
	
	private Instruction createLEFTJOIN(Expression e, BasicBlock bb, int ndata) {
		return new NEWJOIN(e, bb, ndata);
	}
	
	private Instruction createCOMMITLINK(Expression e, BasicBlock bb, int ndata) {
		return new COMMIT(e, bb, ndata);
	}
	
	private Instruction createABORT(Expression e, BasicBlock bb) {
		return new ABORT(e, bb);
	}
	
	private Instruction createTAG(Expression e, BasicBlock bb, String name) {
		return new TAG(e, bb, name);
	}
	
	private Instruction createVALUE(Expression e, BasicBlock bb, String name) {
		return new VALUE(e, bb, name);
	}
	
	private MAPPEDCHOICE createMAPPEDCHOICE(Expression e, BasicBlock bb) {
		return new MAPPEDCHOICE(e, bb);
	}
	
	private NOTCHAR createNOTCHAR(Expression e, BasicBlock bb, BasicBlock jump, int ...ndata) {
		return new NOTCHAR(e, bb, jump, ndata);
	}
	private NOTCHARSET createNOTCHARSET(Expression e, BasicBlock bb, BasicBlock jump) {
		return new NOTCHARSET(e, bb, jump);
	}
	private NOTCHARRANGE createNOTCHARRANGE(Expression e, BasicBlock bb, BasicBlock jump) {
		return new NOTCHARRANGE(e, bb, jump);
	}
	private NOTSTRING createNOTSTRING(Expression e, BasicBlock bb, BasicBlock jump) {
		return new NOTSTRING(e, bb, jump);
	}
	private OPTIONALCHAR createOPTIONALCHAR(Expression e, BasicBlock bb, int ...ndata) {
		return new OPTIONALCHAR(e, bb, ndata);
	}
	private OPTIONALCHARSET createOPTIONALCHARSET(Expression e, BasicBlock bb) {
		return new OPTIONALCHARSET(e, bb);
	}
	private OPTIONALCHARRANGE createOPTIONALBYTERANGE(Expression e, BasicBlock bb, int ...ndata) {
		return new OPTIONALCHARRANGE(e, bb, ndata);
	}
	private OPTIONALSTRING createOPTIONALSTRING(Expression e, BasicBlock bb) {
		return new OPTIONALSTRING(e, bb);
	}
	private ZEROMORECHARRANGE createZEROMOREBYTERANGE(Expression e, BasicBlock bb, int ...ndata) {
		return new ZEROMORECHARRANGE(e, bb, ndata);
	}
	private ZEROMORECHARSET createZEROMORECHARSET(Expression e, BasicBlock bb) {
		return new ZEROMORECHARSET(e, bb);
	}
	private ZEROMOREWS createZEROMOREWS(Expression e, BasicBlock bb) {
		return new ZEROMOREWS(e, bb);
	}
	private LOADp1 createLOADp1(Expression e, BasicBlock bb) {
		return new LOADp1(e, bb);
	}
	private LOADp2 createLOADp2(Expression e, BasicBlock bb) {
		return new LOADp2(e, bb);
	}
	private LOADp3 createLOADp3(Expression e, BasicBlock bb) {
		return new LOADp3(e, bb);
	}
	private STOREp1 createSTOREp1(Expression e, BasicBlock bb) {
		return new STOREp1(e, bb);
	}
	private STOREp2 createSTOREp2(Expression e, BasicBlock bb) {
		return new STOREp2(e, bb);
	}
	private STOREp3 createSTOREp3(Expression e, BasicBlock bb) {
		return new STOREp3(e, bb);
	}
	
	private boolean checkCharset(Choice e) {
		isWS = true;
		for (int i = 0; i < e.size(); i++) {
			Expression inner = e.get(i);
			if (inner instanceof ByteChar) {
				if (isWS) {
					if (!checkWS(((ByteChar)inner).byteChar)) {
						isWS = false;
					}
				}
			}
			else if (inner instanceof ByteMap) {
				isWS = false;
			}
			else {
				isWS = false;
				return false;
			}
		}
		return true;
	}
	
	private boolean checkWS(int byteChar) {
		if (byteChar == 32 || byteChar == 9 || byteChar == 10 || byteChar == 13) {
			return true;
		}
		return false;
	}
	
	private boolean checkString(Sequence e) {
		for(int i = 0; i < e.size(); i++) {
			if (!(e.get(i) instanceof ByteChar)) {
				return false;
			}
		}
		return true;
	}
	
	BasicBlock currentFailBB;
	private int checkWriteChoiceCharset(Choice e, int index, BasicBlock bb, BasicBlock fbb) {
		int charCount = 0;
		for (int i = index; i < e.size(); i++) {
			if (e.get(i) instanceof ByteChar || e.get(i) instanceof ByteMap) {
				charCount++;
			}
			else {
				break;
			}
		}
		if (charCount <= 1) {
			bb = this.getCurrentBasicBlock();
			fbb = new BasicBlock();
			this.pushFailureJumpPoint(fbb);
			this.createPUSHp(e, bb);
			e.get(index).visit(this);
			this.backTrackFlag = true;
			this.currentFailBB = fbb;
			return index++;
		}
		if (charCount != e.size()) {
			backTrackFlag = true;
			bb = this.getCurrentBasicBlock();
			fbb = new BasicBlock();
			this.currentFailBB = fbb;
			this.pushFailureJumpPoint(fbb);
			this.createPUSHp(e, bb);
		}
		writeCharsetCode(e, index, charCount);
		return index + charCount - 1;
	}
	
	private final int writeSequenceCode(Expression e, int index, int size) {
		int count = 0;
		for (int i = index; i < size; i++) {
			if (e.get(i) instanceof ByteChar) {
				count++;
			}
			else {
				break;
			}
		}
		if (count <= 1) {
			e.get(index).visit(this);
			return index++;
		}
		STRING str = this.createSTRING(e, this.getCurrentBasicBlock(), this.jumpFailureJump());
		for(int i = index; i < index + count; i++) {
			str.append(((ByteChar)e.get(i)).byteChar);
		}
		return index + count - 1;
	}
	
	private Expression checkChoice(Choice e, int c, Expression failed) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		checkChoice(e, c, l);
		if(l.size() == 0) {
			l.add(failed);
		}
		return Factory.newChoice(null, l);
	}
	
	private void checkChoice(Choice choice, int c, UList<Expression> l) {
		for(int i = 0; i < choice.size(); i++) {
			Expression e = choice.get(i);
			if (e instanceof Choice) {
				checkChoice((Choice)e, c, l);
			}
			else {
				// FIXME:
				// AnyChar behaves differently in cases of Binary
				short r = e.acceptByte(c, Production.Binary); 
				if(r != Prediction.Reject) {
					l.add(e);
				}
			}
		}
	}
	
	// The depth to control the stack caching optimization
		int depth = 0;
		
		private boolean checkSC(Expression e) {
			int depth = this.depth;
			if (e instanceof NonTerminal) {
				e = getNonTerminalRule(e);
			}
			if (e instanceof Unary) {
				if (this.depth++ < 2) {
					if (O_FusionInstruction) {
						if (e instanceof Not) {
							if (checkOptNot((Not)e)) {
								this.depth = depth;
								return true;
							}
						}
						else if (e instanceof Repetition) {
							if (checkOptRepetition((Repetition)e)) {
								this.depth = depth;
								return true;
							}
						}
						else if (e instanceof Option) {
							if (checkOptOptional((Option)e)) {
								this.depth = depth;
								return true;
							}
						}
					}
					boolean check = checkSC(((Unary) e).get(0));
					this.depth = depth;
					return check;
				}
				this.depth = depth;
				return false;
			}
			if(e instanceof Sequence || e instanceof Choice) {
				if (depth < 2) {
					for(int i = 0; i < e.size(); i++) {
						if (!checkSC(e.get(i))) {
							return false;
						}
					}
					return true;
				}
				return false;
			}
			return true;
		}

		private boolean checkOptRepetition(Repetition e) {
			Expression inner = e.get(0);
			if (inner instanceof NonTerminal) {
				inner = getNonTerminalRule(inner);
			}
			if (inner instanceof ByteMap) {
				return true;
			}
			if (inner instanceof Choice) {
				return checkCharset((Choice)inner);
			}
			return false;
		}

		private boolean checkOptNot(Not e) {
			Expression inner = e.get(0);
			if (inner instanceof NonTerminal) {
				inner = getNonTerminalRule(inner);
			}
			if (inner instanceof ByteChar) {
				return true;
			}
			if (inner instanceof ByteMap) {
				return true;
			}
			if(inner instanceof AnyChar) {
				return true;
			}
			if(inner instanceof Choice) {
				return checkCharset((Choice)inner);
			}
			if (inner instanceof Sequence) {
				return checkString((Sequence)inner);
			}
			return false;
		}

		private boolean checkOptOptional(Option e) {
			Expression inner = e.get(0);
			if (inner instanceof NonTerminal) {
				inner = getNonTerminalRule(inner);
			}
			if (inner instanceof ByteChar) {
				return true;
			}
			if (inner instanceof ByteMap) {
				return true;
			}
			if(inner instanceof Choice) {
				return checkCharset((Choice)inner);
			}
			if (inner instanceof Sequence) {
				return checkString((Sequence)inner);
			}
			return false;
		}

	
	private void writeCharsetCode(Expression e, int index, int charCount) {
		CHARSET inst = this.createCHARSET(e, this.getCurrentBasicBlock(), this.jumpFailureJump());
		for(int i = index; i < index + charCount; i++) {
			if (e.get(i) instanceof ByteChar) {
				inst.append(((ByteChar)e.get(i)).byteChar);
			}
			else if (e.get(i) instanceof ByteMap) {
				ByteMap br = (ByteMap)e.get(i);
				for(int c = 0; c < 256; c++) {
					if(br.byteMap[c]) {
						inst.append(c);
					}
				}
			}
			else {
				System.out.println("Error: Not Char Content in Charset");
			}
		}
	}
	
	private void optimizeChoice(Choice e) {
		if (this.checkCharset(e)) {
			writeCharsetCode(e, 0, e.size());
		}
		else {
			Expression[] matchCase = new Expression[257];
			UList<Expression> caseList = new UList<Expression>(new Expression[257]);
			Expression fails = Factory.newFailure(null);
			for(int i = 0; i < 257; i++) {
				boolean isNotGenerated = true;
				matchCase[i] = checkChoice(e, i, fails);
				for(int j = 0; j < caseList.size(); j++) {
					if (matchCase[i] == caseList.get(j)) {
						isNotGenerated = false;
					}
				}
				if (isNotGenerated) {
					caseList.add(matchCase[i]);
				}
			}
			BasicBlock bb = this.getCurrentBasicBlock();
			BasicBlock fbb = new BasicBlock();
			BasicBlock end = new BasicBlock();
			this.pushFailureJumpPoint(fbb);
			this.createPUSHp(e, bb);
			MAPPEDCHOICE inst = this.createMAPPEDCHOICE(e, bb);
			HashMap<Integer, BasicBlock> choiceMap = new HashMap<Integer, BasicBlock>();
			this.optChoiceMode = false;
			for(int i = 0; i < caseList.size(); i++) {
				bb = new BasicBlock(this.func);
				this.setCurrentBasicBlock(bb);
				Expression caseElement = caseList.get(i);
				choiceMap.put(caseElement.getId(), bb);
				caseElement.visit(this);
				bb = this.getCurrentBasicBlock();
				if (caseElement instanceof Failure) {
					this.createJUMP(caseElement, bb, this.jumpFailureJump());
				}
				else {
					this.createJUMP(caseElement, bb, end);
				}
			}
			this.optChoiceMode = true;
			this.popFailureJumpPoint(e);
			fbb.setInsertPoint(this.func);
			this.createSTOREp(e, fbb);
			this.createJUMP(e, fbb, this.jumpFailureJump());
			end.setInsertPoint(this.func);
			this.setCurrentBasicBlock(end);
			this.createPOPp(e, end);
			for(int i = 0; i < matchCase.length; i++) {
				inst.append(choiceMap.get(matchCase[i].getId()));
			}
		}
	}
	
	private Expression getNonTerminalRule(Expression e) {
		while(e instanceof NonTerminal) {
			NonTerminal nterm = (NonTerminal) e;
			e = nterm.deReference();
		}
		return e;
	}
	
	private void writeNotCode(Not e) {
		BasicBlock bb = this.getCurrentBasicBlock();
		BasicBlock fbb = new BasicBlock();
		this.pushFailureJumpPoint(fbb);
		this.createPUSHp(e, bb);
		e.get(0).visit(this);
		bb = this.getCurrentBasicBlock();
		this.createSTOREp(e, bb);
		this.createSTOREflag(e, bb, 1);
		this.createJUMP(e, bb, this.jumpPrevFailureJump());
		this.popFailureJumpPoint(e);
		fbb.setInsertPoint(this.func);
		this.setCurrentBasicBlock(fbb);
		this.createSTOREp(e, fbb);
		this.createSTOREflag(e, fbb, 0);
	}
	
	private void writeSCNotCode(Not e) {
		if (this.depth == 0) {
			BasicBlock bb = this.getCurrentBasicBlock();
			BasicBlock fbb = new BasicBlock();
			this.pushFailureJumpPoint(fbb);
			this.createLOADp1(e, bb);
			this.depth++;
			e.get(0).visit(this);
			this.depth--;
			bb = this.getCurrentBasicBlock();
			this.createSTOREp1(e, bb);
			this.createSTOREflag(e, bb, 1);
			this.createJUMP(e, bb, this.jumpPrevFailureJump());
			this.popFailureJumpPoint(e);
			fbb.setInsertPoint(this.func);
			this.setCurrentBasicBlock(fbb);
			this.createSTOREp1(e, fbb);
			this.createSTOREflag(e, fbb, 0);
		}
		else if (depth == 1) {
			BasicBlock bb = this.getCurrentBasicBlock();
			BasicBlock fbb = new BasicBlock();
			this.pushFailureJumpPoint(fbb);
			this.createLOADp2(e, bb);
			this.depth++;
			e.get(0).visit(this);
			this.depth--;
			bb = this.getCurrentBasicBlock();
			this.createSTOREp2(e, bb);
			this.createSTOREflag(e, bb, 1);
			this.createJUMP(e, bb, this.jumpPrevFailureJump());
			this.popFailureJumpPoint(e);
			fbb.setInsertPoint(this.func);
			this.setCurrentBasicBlock(fbb);
			this.createSTOREp2(e, fbb);
			this.createSTOREflag(e, fbb, 0);
		}
		else {
			BasicBlock bb = this.getCurrentBasicBlock();
			BasicBlock fbb = new BasicBlock();
			this.pushFailureJumpPoint(fbb);
			this.createLOADp3(e, bb);
			this.depth++;
			e.get(0).visit(this);
			this.depth--;
			bb = this.getCurrentBasicBlock();
			this.createSTOREp3(e, bb);
			this.createSTOREflag(e, bb, 1);
			this.createJUMP(e, bb, this.jumpPrevFailureJump());
			this.popFailureJumpPoint(e);
			fbb.setInsertPoint(this.func);
			this.setCurrentBasicBlock(fbb);
			this.createSTOREp3(e, fbb);
			this.createSTOREflag(e, fbb, 0);
		}
	}
	
	private void writeNotCharsetCode(Choice e) {
		NOTCHARSET inst = this.createNOTCHARSET(e, this.getCurrentBasicBlock(), this.jumpFailureJump());
		for(int i = 0; i < e.size(); i++) {
			if (e.get(i) instanceof ByteChar) {
				inst.append(((ByteChar)e.get(i)).byteChar);
			}
			else if (e.get(i) instanceof ByteMap) {
				ByteMap br = (ByteMap)e.get(i);
				for(int c = 0; c < 256; c++) {
					if(br.byteMap[c]) {
						inst.append(c);
					}
				}
			}
			else {
				System.out.println("Error: Not Char Content in Charset");
			}
		}
	}
	
	private void writeNotStringCode(Sequence e) {
		NOTSTRING inst = this.createNOTSTRING(e, this.getCurrentBasicBlock(), this.jumpFailureJump());
		for(int i = 0; i < e.size(); i++) {
			inst.append(((ByteChar)e.get(i)).byteChar);
		}
	}
	
	private boolean optimizeNot(Not e) {
		Expression inner = e.get(0);
		if (inner instanceof NonTerminal) {
			inner = getNonTerminalRule(inner);
		}
		if (inner instanceof ByteChar) {
			this.createNOTCHAR(inner, this.getCurrentBasicBlock(), this.jumpFailureJump(), ((ByteChar)inner).byteChar);
			return true;
		}
		if (inner instanceof ByteMap) {
			ByteMap br = (ByteMap)inner;
			NOTCHARSET inst = this.createNOTCHARSET(inner, this.getCurrentBasicBlock(), this.jumpFailureJump());
			for(int c = 0; c < 256; c++) {
				if(br.byteMap[c]) {
					inst.append(c);
				}
			}
			return true;
		}
		if(inner instanceof Choice) {
			if (checkCharset((Choice)inner)) {
				writeNotCharsetCode((Choice)inner);
				return true;
			}
		}
		if (inner instanceof Sequence) {
			if (checkString((Sequence)inner)) {
				writeNotStringCode((Sequence)inner);
				return true;
			}
		}
		return false;
	}
	
	private void writeOptionalCode(Option e) {
		BasicBlock bb = this.getCurrentBasicBlock();
		BasicBlock fbb = new BasicBlock();
		BasicBlock mergebb = new BasicBlock();
		this.pushFailureJumpPoint(fbb);
		this.createPUSHp(e, bb);
		e.get(0).visit(this);
		bb = this.getCurrentBasicBlock();
		this.createPOPp(e, bb);
		this.createJUMP(e, bb, mergebb);
		this.popFailureJumpPoint(e);
		fbb.setInsertPoint(this.func);
		this.createSTOREflag(e, fbb, 0);
		this.createSTOREp(e, fbb);
		this.setCurrentBasicBlock(mergebb);
		mergebb.setInsertPoint(this.func);
	}
	
	private void writeSCOptionalCode(Option e) {
		BasicBlock bb = this.getCurrentBasicBlock();
		BasicBlock fbb = new BasicBlock();
		BasicBlock mergebb = new BasicBlock();
		this.pushFailureJumpPoint(fbb);
		if (this.depth == 0) {
			this.createLOADp1(e, bb);
			this.depth++;
			e.get(0).visit(this);
			this.depth--;
			bb = this.getCurrentBasicBlock();
			this.createJUMP(e, bb, mergebb);
			this.popFailureJumpPoint(e);
			fbb.setInsertPoint(this.func);
			this.createSTOREflag(e, fbb, 0);
			this.createSTOREp1(e, fbb);
			this.setCurrentBasicBlock(mergebb);
			mergebb.setInsertPoint(this.func);
		}
		else if (this.depth == 1){
			this.createLOADp2(e, bb);
			this.depth++;
			e.get(0).visit(this);
			this.depth--;
			bb = this.getCurrentBasicBlock();
			this.createJUMP(e, bb, mergebb);
			this.popFailureJumpPoint(e);
			fbb.setInsertPoint(this.func);
			this.createSTOREflag(e, fbb, 0);
			this.createSTOREp2(e, fbb);
			this.setCurrentBasicBlock(mergebb);
			mergebb.setInsertPoint(this.func);
		}
		else {
			this.createLOADp3(e, bb);
			this.depth++;
			e.get(0).visit(this);
			this.depth--;
			bb = this.getCurrentBasicBlock();
			this.createJUMP(e, bb, mergebb);
			this.popFailureJumpPoint(e);
			fbb.setInsertPoint(this.func);
			this.createSTOREflag(e, fbb, 0);
			this.createSTOREp3(e, fbb);
			this.setCurrentBasicBlock(mergebb);
			mergebb.setInsertPoint(this.func);
		}
	}
	
	private void writeOptionalByteMapCode(ByteMap e) {
		OPTIONALCHARSET inst = this.createOPTIONALCHARSET(e, this.getCurrentBasicBlock());
		for(int c = 0; c < 256; c++) {
			if(e.byteMap[c]) {
				inst.append(c);
			}
		}
	}
	
	private void writeOptionalCharsetCode(Choice e) {
		OPTIONALCHARSET inst = this.createOPTIONALCHARSET(e, this.getCurrentBasicBlock());
		for(int i = 0; i < e.size(); i++) {
			if (e.get(i) instanceof ByteChar) {
				inst.append(((ByteChar)e.get(i)).byteChar);
			}
			else if (e.get(i) instanceof ByteMap) {
				ByteMap br = (ByteMap)e.get(i);
				for(int c = 0; c < 256; c++) {
					if(br.byteMap[c]) {
						inst.append(c);
					}
				}
			}
			else {
				System.out.println("Error: Not Char Content in Charset");
			}
		}
	}
	
	private void writeOptionalStringCode(Sequence e) {
		OPTIONALSTRING inst = this.createOPTIONALSTRING(e, this.getCurrentBasicBlock());
		for(int i = 0; i < e.size(); i++) {
			inst.append(((ByteChar)e.get(i)).byteChar);
		}
	}
	
	private boolean optimizeOptional(Option e) {
		Expression inner = e.get(0);
		if (inner instanceof NonTerminal) {
			inner = getNonTerminalRule(inner);
		}
		if (inner instanceof ByteChar) {
			this.createOPTIONALCHAR(inner, this.getCurrentBasicBlock(), ((ByteChar)inner).byteChar);
			return true;
		}
		if (inner instanceof ByteMap) {
			writeOptionalByteMapCode((ByteMap)inner);
			return true;
		}
		if(inner instanceof Choice) {
			if (checkCharset((Choice)inner)) {
				writeOptionalCharsetCode((Choice)inner);
				return true;
			}
		}
		if (inner instanceof Sequence) {
			if (checkString((Sequence)inner)) {
				writeOptionalStringCode((Sequence)inner);
				return true;
			}
		}
		return false;
	}
	
	private void writeRepetitionCode(Repetition e) {
		BasicBlock bb = new BasicBlock(this.func);
		BasicBlock fbb = new BasicBlock();
		BasicBlock mergebb = new BasicBlock();
		this.pushFailureJumpPoint(fbb);
		this.setCurrentBasicBlock(bb);
		this.createPUSHp(e, bb);
		e.get(0).visit(this);
		BasicBlock current = getCurrentBasicBlock();
		this.createREPCOND(e, current, mergebb);
		this.createJUMP(e, current, bb);
		this.popFailureJumpPoint(e);
		fbb.setInsertPoint(this.func);
		this.createSTOREflag(e, fbb, 0);
		this.createSTOREp(e, fbb);
		mergebb.setInsertPoint(this.func);
		this.setCurrentBasicBlock(mergebb);
	}
	
	private void writeSCRepetitionCode(Repetition e) {
		BasicBlock bb = new BasicBlock(this.func);
		BasicBlock fbb = new BasicBlock();
		BasicBlock mergebb = new BasicBlock();
		this.pushFailureJumpPoint(fbb);
		this.setCurrentBasicBlock(bb);
		if (this.depth == 0) {
			this.createLOADp1(e, bb);
			this.depth++;
			e.get(0).visit(this);
			this.depth--;
			BasicBlock current = getCurrentBasicBlock();
			this.createJUMP(e, current, bb);
			this.popFailureJumpPoint(e);
			fbb.setInsertPoint(this.func);
			this.createSTOREflag(e, fbb, 0);
			this.createSTOREp1(e, fbb);
			mergebb.setInsertPoint(this.func);
			this.setCurrentBasicBlock(mergebb);
		}
		else if (this.depth == 1) {
			this.createLOADp2(e, bb);
			this.depth++;
			e.get(0).visit(this);
			this.depth--;
			BasicBlock current = getCurrentBasicBlock();
			this.createJUMP(e, current, bb);
			this.popFailureJumpPoint(e);
			fbb.setInsertPoint(this.func);
			this.createSTOREflag(e, fbb, 0);
			this.createSTOREp2(e, fbb);
			mergebb.setInsertPoint(this.func);
			this.setCurrentBasicBlock(mergebb);
		}
		else {
			this.createLOADp3(e, bb);
			this.depth++;
			e.get(0).visit(this);
			this.depth--;
			BasicBlock current = getCurrentBasicBlock();
			this.createJUMP(e, current, bb);
			this.popFailureJumpPoint(e);
			fbb.setInsertPoint(this.func);
			this.createSTOREflag(e, fbb, 0);
			this.createSTOREp3(e, fbb);
			mergebb.setInsertPoint(this.func);
			this.setCurrentBasicBlock(mergebb);
		}
	}
	
	
	private void writeZeroMoreByteMapCode(ByteMap e, ZEROMORECHARSET inst) {
		for(int c=0; c < 256; c++) {
			if(e.byteMap[c]) {
				inst.append(c);
			}
		}
	}

	private void writeZeroMoreByteMapCode(ByteMap e) {
		ZEROMORECHARSET inst = this.createZEROMORECHARSET(e, this.getCurrentBasicBlock());
		writeZeroMoreByteMapCode(e, inst);
	}
	
	private void writeZeroMoreCharsetCode(Choice e) {
		ZEROMORECHARSET inst = this.createZEROMORECHARSET(e, this.getCurrentBasicBlock());
		for(int i = 0; i < e.size(); i++) {
			if (e.get(i) instanceof ByteChar) {
				inst.append(((ByteChar)e.get(i)).byteChar);
			}
			else if (e.get(i) instanceof ByteMap) {
				ByteMap br = (ByteMap)e.get(i);
				writeZeroMoreByteMapCode(br, inst);
			}
			else {
				System.out.println("Error: Not Char Content in Charset");
			}
		}
	}
	
	boolean isWS = false;
	private boolean optimizeRepetition(Repetition e) {
		Expression inner = e.get(0);
		if (inner instanceof NonTerminal) {
			inner = getNonTerminalRule(inner);
		}
		if (inner instanceof ByteMap) {
			writeZeroMoreByteMapCode((ByteMap)inner);
			return true;
		}
		if (inner instanceof Choice) {
			if (checkCharset((Choice)inner)) {
				if (isWS && O_FusionOperand) {
					this.createZEROMOREWS(inner, this.getCurrentBasicBlock());
					isWS = false;
				}
				else {
					writeZeroMoreCharsetCode((Choice)inner);
				}
				return true;
			}
		}
		return false;
	}

	public void visitRule(Rule e) {
		this.func = new Function(this.module, e.getLocalName());
		this.setCurrentBasicBlock(new BasicBlock(this.func));
		BasicBlock fbb = new BasicBlock();
		this.pushFailureJumpPoint(fbb);
		e.getExpression().visit(this);
		this.popFailureJumpPoint(e);
		fbb.setInsertPoint(func);
		this.createRET(e.getExpression(), fbb);
	}

	
	int ruleSize;

	public void visitNonTerminal(NonTerminal e) {
		if (inlining) {
			if (this.func.instSize() - this.ruleSize <= 20) {
				Expression ne = getNonTerminalRule(e);
				ne.visit(this);
			}
		}
		else if (O_Inlining) {
			Expression ne = getNonTerminalRule(e);
			int index = this.func.size();
			this.ruleSize = this.func.instSize();
			this.inlining = true;
			BasicBlock currentBB = this.getCurrentBasicBlock();
			this.setCurrentBasicBlock(new BasicBlock(func));
			ne.visit(this);
			this.inlining = false;
			if (this.func.instSize() - this.ruleSize > 20) {
				int size = this.func.size();
				for(int i = index; i < size; i++) {
					this.func.remove(index);
				}
				System.out.println("inlining miss: " + e.ruleName);
				BasicBlock rbb = new BasicBlock();
				this.setCurrentBasicBlock(currentBB);
				this.createCALL(e, currentBB, e.ruleName);
				rbb.setInsertPoint(this.func);
				this.createCONDBRANCH(e, rbb, this.jumpFailureJump(), 1);
				BasicBlock bb = new BasicBlock(this.func);
				this.setCurrentBasicBlock(bb);
			}
		}
		else {
			BasicBlock rbb = new BasicBlock();
			this.createCALL(e, this.getCurrentBasicBlock(), e.ruleName);
			rbb.setInsertPoint(this.func);
			this.createCONDBRANCH(e, rbb, this.jumpFailureJump(), 1);
			BasicBlock bb = new BasicBlock(this.func);
			this.setCurrentBasicBlock(bb);
		}
	}

	public void visitEmpty(Empty e) {
	}

	public void visitFailure(Failure e) {
		new STOREflag(e, this.getCurrentBasicBlock(), 1);
	}

	public void visitByteChar(ByteChar e) {
		CHARSET inst =  this.createCHARSET(e, this.getCurrentBasicBlock(), this.jumpFailureJump());
		for(int j = e.byteChar; j <= e.byteChar; j++ ) {
			inst.append(j);
		}
	}

	public void visitByteMap(ByteMap e) {
		CHARSET inst = this.createCHARSET(e, this.getCurrentBasicBlock(), this.jumpFailureJump());
		for(int c = 0; c < 256; c++) {
			if(e.byteMap[c]) {
				inst.append(c);
			}
		}
	}

	public void visitAnyChar(AnyChar e) {
		this.createANY(e, this.getCurrentBasicBlock(), this.jumpFailureJump());
	}

	public void visitNot(Not e) {
		if (O_FusionInstruction) {
			if (!optimizeNot(e)) {
				if (O_StackCaching && checkSC(e.get(0))) {
					writeSCNotCode(e);
					return;
				}
				writeNotCode(e);
			}
		}
		else if (O_StackCaching && checkSC(e.get(0))) {
			writeSCNotCode(e);
			return;
		}
		else {
			writeNotCode(e);
		}
	}

	public void visitAnd(And e) {
		BasicBlock bb = this.getCurrentBasicBlock();
		BasicBlock fbb = new BasicBlock();
		this.pushFailureJumpPoint(fbb);
		this.createPUSHp(e, bb);
		e.get(0).visit(this);
		bb = this.getCurrentBasicBlock();
		this.popFailureJumpPoint(e);
		fbb.setInsertPoint(this.func);
		this.createSTOREp(e, fbb);
		this.createCONDBRANCH(e, fbb, this.jumpFailureJump(), 1);
		this.setCurrentBasicBlock(fbb);
	}

	public void visitOption(Option e) {
		if (O_FusionInstruction) {
			if (!optimizeOptional(e)) {
				if (O_StackCaching && checkSC(e.get(0))) {
					writeSCOptionalCode(e);
					return;
				}
				writeOptionalCode(e);
			}
		}
		else if (O_StackCaching && checkSC(e.get(0))) {
			writeSCOptionalCode(e);
			return;
		}
		else {
			writeOptionalCode(e);
		}
	}

	public void visitRepetition(Repetition e) {
		if (O_FusionInstruction) {
			if (!optimizeRepetition(e)) {
				if (O_StackCaching && checkSC(e.get(0))) {
					writeSCRepetitionCode(e);
					return;
				}
				writeRepetitionCode(e);
			}
		}
		else if (O_StackCaching && checkSC(e.get(0))) {
			writeSCRepetitionCode(e);
			return;
		}
		else {
			writeRepetitionCode(e);
		}
	}

	public void visitSequence(Sequence e) {
		for(int i = 0; i < e.size(); i++) {
			if (O_FusionInstruction) {
				i = writeSequenceCode(e, i, e.size());
			}
			else {
				e.get(i).visit(this);
			}
		}
	}

	boolean backTrackFlag = false;
	
	public void visitChoice(Choice e) {
		if (O_MappedChoice && optChoiceMode) {
			this.optimizeChoice(e);
		}
		else if (O_FusionInstruction) {
			boolean backTrackFlag = this.backTrackFlag = false;
			BasicBlock bb = null;
			BasicBlock fbb = null;
			BasicBlock endbb = new BasicBlock();
			for(int i = 0; i < e.size(); i++) {
				i = checkWriteChoiceCharset(e, i, bb, fbb);
				backTrackFlag = this.backTrackFlag;
				if (backTrackFlag) {
					bb = this.getCurrentBasicBlock();
					fbb = this.currentFailBB;
					this.createJUMP(e, bb, endbb);
					this.popFailureJumpPoint(e.get(i));
					fbb.setInsertPoint(this.func);
					if (i != e.size() - 1) {
						this.createSTOREflag(e, fbb, 0);
					}
					this.createSTOREp(e, fbb);
					this.setCurrentBasicBlock(fbb);
				}
			}
			if (backTrackFlag) {
				this.createJUMP(e, fbb, this.jumpFailureJump());
				endbb.setInsertPoint(this.func);
				this.createPOPp(e, endbb);
				this.setCurrentBasicBlock(endbb);
			}
			this.backTrackFlag = false;
		}
		else {
			BasicBlock bb = null;
			BasicBlock fbb = null;
			BasicBlock endbb = new BasicBlock();
			for(int i = 0; i < e.size(); i++) {
				bb = this.getCurrentBasicBlock();
				fbb =  new BasicBlock();
				this.pushFailureJumpPoint(fbb);
				this.createPUSHp(e, bb);
				e.get(i).visit(this);
				bb = getCurrentBasicBlock();
				this.createJUMP(e, bb, endbb);
				this.popFailureJumpPoint(e.get(i));
				fbb.setInsertPoint(this.func);
				if (i != e.size() - 1) {
					this.createSTOREflag(e, fbb, 0);
				}
				this.createSTOREp(e, fbb);
				this.setCurrentBasicBlock(fbb);
			}
			this.createJUMP(e, fbb, this.jumpFailureJump());
			endbb.setInsertPoint(this.func);
			this.createPOPp(e, endbb);
			this.setCurrentBasicBlock(endbb);
		}
	}

//  FIXME
//	public void visitNew(NewClosure e) {
//		if (PatternMatching) {
//			for(int i = 0; i < e.size(); i++) {
//				if (O_FusionInstruction) {
//					i = writeSequenceCode(e, i, e.size());
//				}
//				else {
//					e.get(i).visit(this);
//				}
//			}
//		}
//		else {
//			BasicBlock bb = this.getCurrentBasicBlock();
//			BasicBlock fbb = new BasicBlock();
//			BasicBlock mergebb = new BasicBlock();
//			this.pushFailureJumpPoint(fbb);
////			if (e.leftJoin) {
////				this.createPUSHo(e, bb);
////				this.createLEFTJOIN(e, bb, 0);
////			}
////			else {
//				this.createNEW(e, bb);
////			}
//			for(int i = 0; i < e.size(); i++) {
//				if (O_FusionInstruction) {
//					i = writeSequenceCode(e, i, e.size());
//				}
//				else {
//					e.get(i).visit(this);
//				}
//			}
//			bb = this.getCurrentBasicBlock();
//			createSETendp(e, bb);
//			createPOPp(e, bb);
////			if (e.leftJoin) {
////				this.createPOPo(e, bb);
////			}
//			createJUMP(e, bb, mergebb);
//			this.popFailureJumpPoint(e);
//			fbb.setInsertPoint(this.func);
//			createABORT(e, fbb);
////			if (e.leftJoin) {
////				this.createSTOREo(e, fbb);
////			}
//			this.createJUMP(e, fbb, this.jumpFailureJump());
//			mergebb.setInsertPoint(this.func);
//			this.setCurrentBasicBlock(mergebb);
//		}
//	}

// FIXME:
//	public void visitLeftNew(LeftNewClosure e) {
//		if (PatternMatching) {
//			for(int i = 0; i < e.size(); i++) {
//				if (O_FusionInstruction) {
//					i = writeSequenceCode(e, i, e.size());
//				}
//				else {
//					e.get(i).visit(this);
//				}
//			}
//		}
//		else {
//			BasicBlock bb = this.getCurrentBasicBlock();
//			BasicBlock fbb = new BasicBlock();
//			BasicBlock mergebb = new BasicBlock();
//			this.pushFailureJumpPoint(fbb);
//			//if (e.leftJoin) {
//				this.createPUSHo(e, bb);
//				this.createLEFTJOIN(e, bb, 0);
////			}
////			else {
////				this.createNEW(e, bb);
////			}
//			for(int i = 0; i < e.size(); i++) {
//				if (O_FusionInstruction) {
//					i = writeSequenceCode(e, i, e.size());
//				}
//				else {
//					e.get(i).visit(this);
//				}
//			}
//			bb = this.getCurrentBasicBlock();
//			createSETendp(e, bb);
//			createPOPp(e, bb);
////			if (e.leftJoin) {
//				this.createPOPo(e, bb);
////			}
//			createJUMP(e, bb, mergebb);
//			this.popFailureJumpPoint(e);
//			fbb.setInsertPoint(this.func);
//			createABORT(e, fbb);
////			if (e.leftJoin) {
//				this.createSTOREo(e, fbb);
////			}
//			this.createJUMP(e, fbb, this.jumpFailureJump());
//			mergebb.setInsertPoint(this.func);
//			this.setCurrentBasicBlock(mergebb);
//		}
//	}

	public void visitLink(Link e) {
		if (PatternMatching) {
			e.get(0).visit(this);
		}
		else {
			BasicBlock bb = this.getCurrentBasicBlock();
			BasicBlock fbb = new BasicBlock();
			BasicBlock mergebb = new BasicBlock(); 
			this.pushFailureJumpPoint(fbb);
			this.createPUSHo(e, bb);
			e.get(0).visit(this);
			bb = this.getCurrentBasicBlock();
			this.createCOMMITLINK(e, bb, e.index);
			this.createJUMP(e, bb, mergebb);
			this.popFailureJumpPoint(e);
			fbb.setInsertPoint(this.func);
			this.createABORT(e, fbb);
			this.createSTOREo(e, fbb);
			this.createJUMP(e, fbb, this.jumpFailureJump());
			mergebb.setInsertPoint(this.func);
			this.setCurrentBasicBlock(mergebb);
		}
	}

	public void visitTagging(Tagging e) {
		createTAG(e, this.getCurrentBasicBlock(), "#" + e.tag.toString());
	}

	public void visitReplace(Replace e) {
		createVALUE(e, this.getCurrentBasicBlock(), e.value);
	}

	@Override
	public void visitExpression(Expression e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}
}
