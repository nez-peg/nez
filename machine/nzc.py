
# Arguments

InstructionSet = [
["Nop"],
["Fail"],
["Alt", "Jump"],
["Succ"],
["Jump", "Jump"],
["Call", "Jump", "NonTerminal"],  # NonTerminal is for debug
["Ret"],
["Pos"],
["Back"],
["Skip", "Jump"],

["Byte", "Byte"],
["Any"],
["Str", "Bstr"],
["Set", "Bset"],
["NByte", "Byte"],
["NAny"],
["NStr", "Bstr"],
["NSet", "Bset"],
["OByte", "Byte"],
["OAny"],
["OStr", "Bstr"],
["OSet", "Bset"],
["RByte", "Byte"],
["RAny"],
["RStr", "Bstr"],
["RSet", "Bset"],
	
["Consume", "Shift"],
["First", "JumpTable"],

["Lookup", "Jump", "MemoPoint"],
["Memo", "MemoPoint"],
["MemoFail", "MemoPoint"],

["TPush"],
["TPop", "Index"],
["TLeftFold", "Shift"],
["TNew", "Shift"],
["TCapture", "Shift"],
["TTag", "Tag"],
["TReplace", "Bstr"],
["TStart"],
["TCommit", "Index"],
["TAbort"],

["TLookup", "Jump", "MemoPoint", "Index"],
["TMemo", "Index"],


["SOpen"],
["SClose"],
["SMask", "Table"],
["SDef", "Table"],
["SIsDef", "Table", "Bstr"],
["SExists", "Table"],
["SMatch", "Table"],
["SIs", "Table"],
["SIsa", "Table"],
["SDefNum", "Table"],
["SCount", "Table"],
["Exit"],
["Label", "NonTerminal"],
]

Arguments = [
 ["NonTerminal", "u16",     "@NonTerminalConstPools"],
 ["Jump", "u24"],
 ["JumpTable",              "u24*257"],
 ["Byte", "u8"],
 ["Bset", "u16",            "@SetConstPools"],
 ["Bstr", "u16",            "@StrConstPools"],
 ["Shift", "i8"],
 ["MemoPoint", "u16"],
 ["Index", "i8"],
 ["Tag", "u16",             "@TagConstPools"],
 ["Table", "u16",           "@TableConstPools"]
]

