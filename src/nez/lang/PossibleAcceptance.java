package nez.lang;

import nez.NezOption;
import nez.ast.Source;
import nez.util.UFlag;

public class PossibleAcceptance {
	public final static int TextEOF   = 0;
	public final static int BinaryEOF = 256;
	public final static short Accept = 0;
	public final static short Unconsumed = 1;
	public final static short Reject = 2;
		
	public static short acceptByteChar(int byteChar, int ch) {
		return (byteChar == ch) ? PossibleAcceptance.Accept : PossibleAcceptance.Reject;
	}

	public static short acceptByteMap(boolean[] byteMap, int ch) {
		return (byteMap[ch]) ? PossibleAcceptance.Accept : PossibleAcceptance.Reject;
	}

	public static short acceptAny(boolean binary, int ch) {
		if(binary) {
			return (ch == Source.BinaryEOF) ? PossibleAcceptance.Reject : PossibleAcceptance.Accept;
		}
		else {
			return (ch == Source.BinaryEOF || ch == 0) ? PossibleAcceptance.Reject : PossibleAcceptance.Accept;
		}
	}


	public static short acceptUnary(Unary e, int ch) {
		return e.get(0).acceptByte(ch);
	}
	
	public static short acceptOption(Expression e, int ch) {
		short r = e.get(0).acceptByte(ch);
		return (r == PossibleAcceptance.Accept) ? r : PossibleAcceptance.Unconsumed;
	}

	public static short acceptAnd(And e, int ch) {
		short r = e.get(0).acceptByte(ch);
		return (r == PossibleAcceptance.Reject) ? r : PossibleAcceptance.Unconsumed;
	}

	public static short acceptNot(Not e, int ch) {
		Expression inner = e.get(0);
		if(inner instanceof ByteChar || inner instanceof ByteMap || inner instanceof AnyChar) {
			return inner.acceptByte(ch) == PossibleAcceptance.Accept ? PossibleAcceptance.Reject : PossibleAcceptance.Unconsumed;
		}
		/* The code below works only if a single character in !(e) */
		/* we must accept 'i' for !'int' 'i' */
		return PossibleAcceptance.Unconsumed;
	}
			
}
