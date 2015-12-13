package nez.junks;

import java.util.HashMap;

import nez.util.FileBuilder;

public class MozSpec {
	public final static String[][] InstructionSet = { //
	{ "Nop" }, //
			{ "Fail" }, //
			{ "Alt", "Jump" }, //
			{ "Succ" }, //
			{ "Jump", "Jump" }, //
			{ "Call", "Jump", "NonTerminal" }, // # NonTerminal is for debug
			{ "Ret" }, //
			{ "Pos" }, //
			{ "Back" }, //
			{ "Skip" }, //

			{ "Byte", "Byte" }, //
			{ "Any" }, //
			{ "Str", "Bstr" }, //
			{ "Set", "Bset" }, //
			{ "NByte", "Byte" }, //
			{ "NAny" }, //
			{ "NStr", "Bstr" }, //
			{ "NSet", "Bset" }, //
			{ "OByte", "Byte" }, //
			{ "OAny" }, //
			{ "OStr", "Bstr" }, //
			{ "OSet", "Bset" }, //
			{ "RByte", "Byte" }, //
			{ "RAny" }, //
			{ "RStr", "Bstr" }, //
			{ "RSet", "Bset" }, //

			{ "Consume", "Shift" }, //
			{ "First", "JumpTable" }, //

			// { "Lookup", "Jump", "MemoPoint", "State" }, //
			// { "Memo", "MemoPoint", "State" }, //
			// { "MemoFail", "MemoPoint", "State" }, //

			{ "Lookup", "State", "MemoPoint", "Jump" }, //
			{ "Memo", "State", "MemoPoint" }, //
			{ "MemoFail", "State", "MemoPoint" }, //

			{ "TPush" }, //
			{ "TPop", "Label" }, //
			{ "TLeftFold", "Shift", "Label" }, //
			{ "TNew", "Shift" }, //
			{ "TCapture", "Shift" }, //
			{ "TTag", "Tag" }, //
			{ "TReplace", "Bstr" }, //
			{ "TStart" }, //
			{ "TCommit", "Label" }, //
			{ "TAbort" }, //

			// { "TLookup", "Jump", "MemoPoint", "State", "Label" }, //
			// { "TMemo", "MemoPoint", "State" }, //

			{ "TLookup", "State", "MemoPoint", "Jump", "Label" }, //
			{ "TMemo", "State", "MemoPoint" }, //

			{ "SOpen" }, //
			{ "SClose" }, //
			{ "SMask", "Table" }, //
			{ "SDef", "Table" }, //
			{ "SIsDef", "Table", "Bstr" }, //
			{ "SExists", "Table" }, //
			{ "SMatch", "Table" }, //
			{ "SIs", "Table" }, //
			{ "SIsa", "Table" }, //
			{ "SDefNum", "Table" }, //
			{ "SCount", "Table" }, //
			{ "Exit", "State" }, //
			{ "DFirst", "JumpTable" }, //

			{ "Label", "NonTerminal" }, //

	// { "Cov", "Id" }, //
	// { "Covx", "Id" }, //
	};

	// public static String[][] Arguments = { //
	// { "NonTerminal", "u16", "@NonTerminalConstPools" }, //
	// { "Jump", "u24" }, //
	// { "JumpTable", "u24*257" }, //
	// { "Byte", "u8" }, //
	// { "Bset", "u16", "@SetConstPools" }, //
	// { "Bstr", "u16", "@StrConstPools" }, //
	// { "Shift", "i8" }, //
	// { "MemoPoint", "u16" }, //
	// { "Label", "u16" }, //
	// { "Tag", "u16", "@TagConstPools" }, //
	// { "Table", "u16", "@TableConstPools" } //
	// };

	static HashMap<String, String> javaMap = new HashMap<String, String>();
	static {
		// type
		javaMap.put("tNonTerminal", "String");
		javaMap.put("tJump", "Instruction");
		javaMap.put("tJumpTable", "Instruction[]");
		javaMap.put("tByte", "int");
		javaMap.put("tBset", "boolean[]");
		javaMap.put("tBstr", "byte[]");
		javaMap.put("tShift", "int");
		javaMap.put("tMemoPoint", "int");
		javaMap.put("tState", "boolean");
		javaMap.put("tLabel", "Symbol");
		javaMap.put("tTag", "Symbol");
		javaMap.put("tTable", "Symbol");

		// name
		javaMap.put("nNonTerminal", "nonTerminal");
		javaMap.put("nJump", "jump");
		javaMap.put("nJumpTable", "jumpTable");
		javaMap.put("nByte", "byteChar");
		javaMap.put("nBset", "byteMap");
		javaMap.put("nBstr", "utf8");
		javaMap.put("nShift", "shift");
		javaMap.put("nMemoPoint", "memoPoint");
		javaMap.put("nState", "state");
		javaMap.put("nLabel", "label");
		javaMap.put("nTag", "tag");
		javaMap.put("nTable", "table");

	}

	public final static void genOpcode() {
		FileBuilder fb = new FileBuilder();
		int c = 0;
		for (String[] data : InstructionSet) {
			fb.writeIndent("public final static byte %s = %d;", data[0], c);
			c++;
		}

	}

	public final static void genInstructionStruct() {
		for (String[] instData : InstructionSet) {
			genInstruction(instData);
		}
	}

	public final static void genInstruction(String[] data) {
		FileBuilder fb = new FileBuilder();
		String name = data[0];
		fb.write("// " + name);
		fb.writeIndent("class %s extends MozInst {", name);
		fb.incIndent();
		for (int i = 1; i < data.length; i++) {
			if (!data[i].equals("Jump")) {
				fb.writeIndent("private %s %s;", type(data[i]), name(data[i]));
			}
		}
		// Constructor
		fb.writeIndent("public %s (Expression e, Instruction next", name);
		for (int i = 1; i < data.length; i++) {
			fb.write(", %s %s", type(data[i]), name(data[i]));
		}
		fb.write(") {");
		fb.incIndent();
		fb.writeIndent("super(Moz.%s, e, next);", name);
		for (int i = 1; i < data.length; i++) {
			fb.writeIndent("this.%s = %s;", name(data[i]), name(data[i]));
		}
		fb.decIndent();
		fb.writeIndent("}"); // closed Constructor

		// Encoder
		fb.writeIndent("protected void encodeImpl(ByteCoder bc) {");
		fb.incIndent();
		for (int i = 1; i < data.length; i++) {
			fb.writeIndent("bc.encode%s(this.%s);", data[i], name(data[i]));
		}
		fb.decIndent();
		fb.writeIndent("}"); // closed Constructor

		// Encoder
		fb.writeIndent("protected void formatImpl(StringBuilder sb) {");
		fb.incIndent();
		for (int i = 1; i < data.length; i++) {
			fb.writeIndent("this.format%s(sb, this.%s);", data[i], name(data[i]));
			fb.writeIndent("sb.append(' ');", data[i], name(data[i]));
		}
		fb.decIndent();
		fb.writeIndent("}"); // closed Constructor

		fb.decIndent();
		fb.writeIndent("}");
		fb.writeNewLine();
		;

		fb.close();
	}

	private static String type(String n) {
		return javaMap.get("t" + n);
	}

	private static String name(String n) {
		return javaMap.get("n" + n);
	}

	public final static void genLoader() {
		FileBuilder fb = new FileBuilder();
		fb.incIndent();
		for (String[] data : InstructionSet) {
			genLoader(fb, data);
		}
		fb.decIndent();

	}

	public final static void genLoader(FileBuilder fb, String[] data) {
		String name = data[0];
		fb.writeIndent("case Moz.%s: {", name);
		fb.incIndent();
		for (int i = 1; i < data.length; i++) {
			fb.writeIndent("%s %s = this.read%s();", type(data[i]), name(data[i]), data[i]);
		}
		fb.writeIndent("return new %s (null, null", name);
		for (int i = 1; i < data.length; i++) {
			fb.write(", %s", name(data[i]));
		}
		fb.write(");");
		fb.decIndent();
		fb.writeIndent("}");
	}

}
