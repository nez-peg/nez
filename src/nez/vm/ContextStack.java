package nez.vm;

class ContextStack {
	boolean debugFailStackFlag;
	Instruction jump;
	long pos;
	int  prevFailTop;
	ASTLog lastLog;
}