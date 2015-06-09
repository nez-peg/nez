package nez.lang;

import nez.ast.SourcePosition;
import nez.util.StringUtils;
import nez.util.UFlag;
import nez.util.UList;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class ByteMap extends Terminal implements Consumed {
	boolean binary = false;
	public final boolean isBinary() {
		return this.binary;
	}
	public boolean[] byteMap; // Immutable

	ByteMap(SourcePosition s, boolean binary, int beginChar, int endChar) {
		super(s);
		this.byteMap = newMap(false);
		this.binary = binary;
		appendRange(this.byteMap, beginChar, endChar);
	}
	ByteMap(SourcePosition s, boolean binary, boolean[] b) {
		super(s);
		this.binary = binary;
		this.byteMap = b;
	}
	@Override
	public final boolean equalsExpression(Expression o) {
		if(o instanceof ByteMap && this.binary == ((ByteMap)o).isBinary()) {
			ByteMap e = (ByteMap)o;
			for(int i = 0; i < this.byteMap.length; i++) {
				if(this.byteMap[i] != e.byteMap[i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	protected final void format(StringBuilder sb) {
		sb.append(StringUtils.stringfyCharacterClass(this.byteMap));
	}

	@Override
	public String getPredicate() {
		return "byte " + StringUtils.stringfyBitmap(this.byteMap);
	}

	@Override
	public String key() {
		return binary ? "b[" + StringUtils.stringfyBitmap(this.byteMap)
				      : "[" +  StringUtils.stringfyBitmap(this.byteMap);
	}

	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeByteMap(this);
	}

	@Override
	public boolean isConsumed() {
		return true;
	}

	@Override
	public short acceptByte(int ch, int option) {
		return (byteMap[ch]) ? Acceptance.Accept : Acceptance.Reject;
	}
	
	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeByteMap(this, next, failjump);
	}
	@Override
	protected int pattern(GEP gep) {
		int c = 0;
		for(boolean b: this.byteMap) {
			if(b) {
				c += 1;
			}
		}
		return c;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		int c = 0;
		for(int ch = 0; ch < 127; ch++) {
			if(this.byteMap[ch]) {
				c += 1;
			}
			if(c == p) {
				sb.append((char)ch);
			}
		}
	}
	
	// Utils
	
	public final static boolean[] newMap(boolean initValue) {
		boolean[] b = new boolean[257];
		if(initValue) {
			for(int i = 0; i < b.length; i++) {
				b[i] = initValue;
			}
		}
		return b;
	}

	public final static void clear(boolean[] byteMap) {
		for(int c = 0; c < byteMap.length; c++) {
			byteMap[c] = false;
		}
	}

	public final static void appendRange(boolean[] b, int beginChar, int endChar) {
		for(int c = beginChar; c <= endChar; c++) {
			b[c] = true;
		}
	}

	public final static void appendBitMap(boolean[] dst, boolean[] src) {
		for(int i = 0; i < 256; i++) {
			if(src[i]) {
				dst[i] = true;
			}
		}
	}

	public final static void reverse(boolean[] byteMap, int option) {
		for(int i = 0; i < 256; i++) {
			byteMap[i] = !byteMap[i];
		}
		if(!UFlag.is(option, Grammar.Binary)) {
			byteMap[0] = false;
		}
	}
}
