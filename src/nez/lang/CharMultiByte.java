//package nez.lang;
//
//import nez.ast.SourcePosition;
//import nez.util.StringUtils;
//import nez.vm.Instruction;
//import nez.vm.NezEncoder;
//
//public class CharMultiByte extends Char implements Consumed {
//	public byte[] byteSeq;
//	CharMultiByte(SourcePosition s, boolean binary, byte[] byteSeq) {
//		super(s, binary);
//		this.byteSeq = byteSeq;
//	}
//	@Override
//	public final boolean equalsExpression(Expression o) {
//		if(o instanceof CharMultiByte) {
//			CharMultiByte mb = (CharMultiByte)o;
//			if(mb.byteSeq.length == this.byteSeq.length) {
//				for(int i = 0; i < this.byteSeq.length; i++) {
//					if(byteSeq[i] != mb.byteSeq[i]) {
//						return false;
//					}
//				}
//				return true;
//			}
//		}
//		return false;
//	}
//	@Override
//	protected final void format(StringBuilder sb) {
//		sb.append(StringUtils.stringfyCharacter(this.byteChar));
//	}
//	@Override
//	public String getPredicate() {
//		return "byte " + byteChar;
//	}
//	@Override
//	public String key() { 
//		return binary ? "mb'" + byteChar : "m'" + byteChar;
//	}	
//	@Override
//	public Expression reshape(GrammarReshaper m) {
//		return m.reshapeByteChar(this);
//	}
//	@Override
//	public boolean isConsumed() {
//		return true;
//	}
//	@Override
//	public short acceptByte(int ch) {
//		return PossibleAcceptance.acceptByteChar(byteSeq[0] & 0xff, ch);
//	}
//	@Override
//	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
//		return bc.encodeByteChar(this, next, failjump);
//	}
//}
