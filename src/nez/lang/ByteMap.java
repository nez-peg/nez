package nez.lang;

import nez.ast.SourcePosition;
import nez.util.StringUtils;
import nez.util.UFlag;
import nez.util.UList;
import nez.vm.Instruction;
import nez.vm.NezCompiler;

public class ByteMap extends Terminal {
	public boolean[] byteMap; // Immutable
	ByteMap(SourcePosition s, int beginChar, int endChar) {
		super(s);
		this.byteMap = newMap(false);
		appendRange(this.byteMap, beginChar, endChar);
	}
	ByteMap(SourcePosition s, boolean[] b) {
		super(s);
		this.byteMap = b;
	}

	@Override
	public String getPredicate() {
		return "byte " + StringUtils.stringfyByteMap(this.byteMap);
	}

	@Override
	public String key() { 
		return "[" +  StringUtils.stringfyByteMap(this.byteMap);
	}

	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeByteMap(this);
	}

	@Override
	public boolean isConsumed(Stacker stacker) {
		return true;
	}


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
	
	
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return true;
	}
	@Override
	public short acceptByte(int ch, int option) {
		return (byteMap[ch]) ? Prediction.Accept : Prediction.Reject;
	}
	
	@Override
	public Instruction encode(NezCompiler bc, Instruction next, Instruction failjump) {
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

}
