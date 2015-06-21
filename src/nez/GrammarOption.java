package nez;

public class GrammarOption {

	public final static int ClassicMode = 1;
	public final static int ASTConstruction = 1 << 1;
	public final static int PackratParsing  = 1 << 2;
	public final static int Optimization    = 1 << 3;
	public final static int Specialization  = 1 << 4;
	public final static int CommonPrefix    = 1 << 5;
	public final static int Inlining        = 1 << 6;
	public final static int Prediction      = 1 << 7;
	public final static int DFA             = 1 << 8;
	public final static int Tracing         = 1 << 9;
	public final static int Binary          = 1 << 10;
	public final static int Utf8            = 1 << 11;
	public final static int Profiling       = 1 << 12;
	public final static int DefaultOption = ASTConstruction | PackratParsing | Optimization 
	| Specialization | Inlining | CommonPrefix | Prediction;
	public final static int RegexOption = ASTConstruction | PackratParsing | Optimization
	| Specialization | Prediction;
	public final static int SafeOption = ASTConstruction | Optimization;
	public final static int ExampleOption = Optimization | Specialization | Inlining | CommonPrefix | Prediction;
	public final static int DebugOption = ASTConstruction;
}
