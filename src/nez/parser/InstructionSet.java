package nez.parser;

public class InstructionSet {
	public final static byte Nop = 0; // Do nothing
	public final static byte Fail = 1; // Fail
	public final static byte Alt = 2; // Alt
	public final static byte Succ = 3; // Succ
	public final static byte Jump = 4; // Jump
	public final static byte Call = 5; // Call
	public final static byte Ret = 6; // Ret
	public final static byte Pos = 7; // Pos
	public final static byte Back = 8; // Back
	public final static byte Skip = 9; // Skip

	public final static byte Byte = 10; // match a byte character
	public final static byte Any = 11; // match any
	public final static byte Str = 12; // match string
	public final static byte Set = 13; // match set
	public final static byte NByte = 14; //
	public final static byte NAny = 15; //
	public final static byte NStr = 16; //
	public final static byte NSet = 17; //
	public final static byte OByte = 18; //
	public final static byte OAny = 19; //
	public final static byte OStr = 20; //
	public final static byte OSet = 21; //
	public final static byte RByte = 22; //
	public final static byte RAny = 23; //
	public final static byte RStr = 24; //
	public final static byte RSet = 25; //

	public final static byte Consume = 26; //
	public final static byte First = 27; //

	public final static byte Lookup = 28; // match a character
	public final static byte Memo = 29; // match a character
	public final static byte MemoFail = 30; // match a character

	public final static byte TPush = 31;
	public final static byte TPop = 32;
	public final static byte TLeftFold = 33;
	public final static byte TNew = 34;
	public final static byte TCapture = 35;
	public final static byte TTag = 36;
	public final static byte TReplace = 37;
	public final static byte TStart = 38;
	public final static byte TCommit = 39;
	public final static byte TAbort = 40;

	public final static byte TLookup = 41;
	public final static byte TMemo = 42;

	public final static byte SOpen = 43;
	public final static byte SClose = 44;
	public final static byte SMask = 45;
	public final static byte SDef = 46;
	public final static byte SIsDef = 47;
	public final static byte SExists = 48;
	public final static byte SMatch = 49;
	public final static byte SIs = 50;
	public final static byte SIsa = 51;
	public final static byte SDefNum = 52;
	public final static byte SCount = 53;
	public final static byte Exit = 54; // 7-bit only

	/* extended */
	public final static byte DFirst = 55; // Dfa
	public final static byte Cov = 56;
	public final static byte Covx = 57;

	public final static byte LRCall = 58;
	public final static byte LRGrow = 59;

	public final static byte Label = 127; // 7-bit

	public static String stringfy(byte opcode) {
		switch (opcode) {
		case Nop:
			return "nop";
		case Fail:
			return "fail";
		case Alt:
			return "alt";
		case Succ:
			return "succ";
		case Jump:
			return "jump";
		case Call:
			return "call";
		case Ret:
			return "ret";
		case Pos:
			return "pos";
		case Back:
			return "back";
		case Skip:
			return "skip";

		case Byte:
			return "byte";
		case Any:
			return "any";
		case Str:
			return "str";
		case Set:
			return "set";

		case NByte:
			return "nbyte";
		case NAny:
			return "nany";
		case NStr:
			return "nstr";
		case NSet:
			return "nset";

		case OByte:
			return "obyte";
		case OAny:
			return "oany";
		case OStr:
			return "ostr";
		case OSet:
			return "oset";

		case RByte:
			return "rbyte";
		case RAny:
			return "rany";
		case RStr:
			return "rstr";
		case RSet:
			return "rset";

		case Consume:
			return "consume";
		case First:
			return "first";

		case Lookup:
			return "lookup";
		case Memo:
			return "memo";
		case MemoFail:
			return "memofail";

		case TPush:
			return "tpush";
		case TPop:
			return "tpop";
		case TLeftFold:
			return "tswap";
		case TNew:
			return "tnew";
		case TCapture:
			return "tcap";
		case TTag:
			return "ttag";
		case TReplace:
			return "trep";
		case TStart:
			return "tstart";
		case TCommit:
			return "tcommit";
		case TAbort:
			return "tabort";

		case TLookup:
			return "tlookup";
		case TMemo:
			return "tmemo";

		case SOpen:
			return "open";
		case SClose:
			return "close";
		case SMask:
			return "mask";
		case SDef:
			return "def";
		case SIsDef:
			return "isdef";
		case SExists:
			return "exists";
		case SIs:
			return "is";
		case SIsa:
			return "isa";
		case SDefNum:
			return "defnum";
		case SCount:
			return "count";

		case Exit:
			return "exit";

		case LRCall:
			return "lrcall";
		case LRGrow:
			return "lrgrow";

		default:
			return "-";
		}
	}

}
