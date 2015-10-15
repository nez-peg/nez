package konoha.script;

import nez.ast.Symbol;

public interface CommonSymbols {

	public final static Symbol _anno = Symbol.tag("anno");
	public final static Symbol _name = Symbol.tag("name");
	public final static Symbol _super = Symbol.tag("super");
	public final static Symbol _impl = Symbol.tag("impl");
	public final static Symbol _body = Symbol.tag("body");
	public final static Symbol _type = Symbol.tag("type");
	public final static Symbol _expr = Symbol.tag("expr");
	public final static Symbol _list = Symbol.tag("list");
	public final static Symbol _param = Symbol.tag("param");
	public final static Symbol _throws = Symbol.tag("throws");
	public final static Symbol _base = Symbol.tag("base");
	public final static Symbol _extends = Symbol.tag("extends");
	public final static Symbol _cond = Symbol.tag("cond");
	public final static Symbol _msg = Symbol.tag("msg");
	public final static Symbol _then = Symbol.tag("then");
	public final static Symbol _else = Symbol.tag("else");
	public final static Symbol _init = Symbol.tag("init");
	public final static Symbol _iter = Symbol.tag("iter");
	public final static Symbol _label = Symbol.tag("label");
	public final static Symbol _try = Symbol.tag("try");
	public final static Symbol _catch = Symbol.tag("catch");
	public final static Symbol _finally = Symbol.tag("finally");
	public final static Symbol _left = Symbol.tag("left");
	public final static Symbol _right = Symbol.tag("right");
	public final static Symbol _recv = Symbol.tag("recv");
	public final static Symbol _size = Symbol.tag("size");
	public final static Symbol _prefix = Symbol.tag("prefix");

	public final static Symbol _Name = Symbol.tag("Name");
	public final static Symbol _ArrayName = Symbol.tag("ArrayName");
	public final static Symbol _ArrayType = Symbol.tag("ArrayType");
	public final static Symbol _GenericType = Symbol.tag("GenericType");
	public final static Symbol _TypeOf = Symbol.tag("TypeOf");

	public final static Symbol _Cast = Symbol.tag("Cast");
	public final static Symbol _UpCast = Symbol.tag("UpCast");
	public final static Symbol _DownCast = Symbol.tag("DownCast");
	public final static Symbol _Error = Symbol.tag("Error"); // type
																// error

	public final static Symbol _Source = Symbol.tag("Source");
	public final static Symbol _Modifiers = Symbol.tag("Modifiers");
	public final static Symbol _Annotation = Symbol.tag("Annotation");
	public final static Symbol _CommaList = Symbol.tag("CommaList");
	public final static Symbol _KeyValue = Symbol.tag("KeyValue");
	public final static Symbol _List = Symbol.tag("List");
	public final static Symbol _Import = Symbol.tag("Import");
	public final static Symbol _WildCardName = Symbol.tag("WildCardName");
	public final static Symbol _ClassDecl = Symbol.tag("ClassDecl");
	public final static Symbol _Block = Symbol.tag("Block");
	public final static Symbol _InstanceInisializer = Symbol.tag("InstanceInisializer");
	public final static Symbol _StaticInitializer = Symbol.tag("StaticInitializer");
	public final static Symbol _Empty = Symbol.tag("Empty");
	public final static Symbol _InterfaceDecl = Symbol.tag("InterfaceDecl");
	public final static Symbol _EnumDecl = Symbol.tag("EnumDecl");
	public final static Symbol _Enum = Symbol.tag("Enum");
	public final static Symbol _VarDecl = Symbol.tag("VarDecl");
	public final static Symbol _MultiVarDecl = Symbol.tag("MultiVarDecl");
	public final static Symbol _ArrayDecl = Symbol.tag("ArrayDecl");
	public final static Symbol _Array = Symbol.tag("Array");
	public final static Symbol _FieldDecl = Symbol.tag("FieldDecl");
	public final static Symbol _MethodDecl = Symbol.tag("MethodDecl");
	public final static Symbol _Param = Symbol.tag("Param");
	public final static Symbol _VarParam = Symbol.tag("VarParam");
	public final static Symbol _Throws = Symbol.tag("Throws");
	public final static Symbol _Constructor = Symbol.tag("Constructor");
	public final static Symbol _ExplicitConstructorInvocation = Symbol.tag("ExplicitConstructorInvocation");
	public final static Symbol _FuncDecl = Symbol.tag("FuncDecl");
	public final static Symbol _Annotated = Symbol.tag("Annotated");
	public final static Symbol _TypeBound = Symbol.tag("TypeBound");
	public final static Symbol _TypeLists = Symbol.tag("TypeLists");
	public final static Symbol _TypeArguments = Symbol.tag("TypeArguments");
	public final static Symbol _TWildCard = Symbol.tag("TWildCard");
	public final static Symbol _UpperBound = Symbol.tag("UpperBound");
	public final static Symbol _LowerBound = Symbol.tag("LowerBound");
	public final static Symbol _VoidType = Symbol.tag("VoidType");
	public final static Symbol _Assert = Symbol.tag("Assert");
	public final static Symbol _If = Symbol.tag("If");
	public final static Symbol _While = Symbol.tag("While");
	public final static Symbol _DoWhile = Symbol.tag("DoWhile");
	public final static Symbol _For = Symbol.tag("For");
	public final static Symbol _ForEach = Symbol.tag("ForEach");
	public final static Symbol _Continue = Symbol.tag("Continue");
	public final static Symbol _Break = Symbol.tag("Break");
	public final static Symbol _Return = Symbol.tag("Return");
	public final static Symbol _Throw = Symbol.tag("Throw");
	public final static Symbol _Synchronized = Symbol.tag("Synchronized");
	public final static Symbol _Label = Symbol.tag("Label");
	public final static Symbol _Expression = Symbol.tag("Expression");
	public final static Symbol _Try = Symbol.tag("Try");
	public final static Symbol _Catch = Symbol.tag("Catch");
	public final static Symbol _Switch = Symbol.tag("Switch");
	public final static Symbol _SwitchCase = Symbol.tag("SwitchCase");
	public final static Symbol _SwitchDefault = Symbol.tag("SwitchDefault");

	public final static Symbol _Assign = Symbol.tag("Assign");
	public final static Symbol _AssignMul = Symbol.tag("AssignMul");
	public final static Symbol _AssignDiv = Symbol.tag("AssignDiv");
	public final static Symbol _AssignMod = Symbol.tag("AssignMod");
	public final static Symbol _AssignAdd = Symbol.tag("AssignAdd");
	public final static Symbol _AssignSub = Symbol.tag("AssignSub");
	public final static Symbol _AssignLeftShift = Symbol.tag("AssignLeftShift");
	public final static Symbol _AssignRightShift = Symbol.tag("AssignRightShift");
	public final static Symbol _AssignArithmeticRightShift = Symbol.tag("AssignArithmeticRightShift");
	public final static Symbol _AssignLogicalRightShift = Symbol.tag("AssignLogicalRightShift");
	public final static Symbol _AssignBitwiseAnd = Symbol.tag("AssignBitwiseAnd");
	public final static Symbol _AssignBitwiseXOr = Symbol.tag("AssignBitwiseXOr");
	public final static Symbol _AssignBitwiseOr = Symbol.tag("AssignBitwiseOr");

	public final static Symbol _Conditional = Symbol.tag("Conditional");
	public final static Symbol _Or = Symbol.tag("Or");
	public final static Symbol _And = Symbol.tag("And");
	public final static Symbol _BitwiseNot = Symbol.tag("BitwiseNot");
	public final static Symbol _BitwiseOr = Symbol.tag("BitwiseOr");
	public final static Symbol _BitwiseXor = Symbol.tag("BitwiseXor");
	public final static Symbol _BitwiseAnd = Symbol.tag("BitwiseAnd");
	public final static Symbol _LogicalNot = Symbol.tag("LogicalNot");
	public final static Symbol _LogicalAnd = Symbol.tag("LogicalAnd");
	public final static Symbol _LogicalOr = Symbol.tag("LogicalOr");
	public final static Symbol _Equals = Symbol.tag("Equals");
	public final static Symbol _NotEquals = Symbol.tag("NotEquals");
	public final static Symbol _LessThanEquals = Symbol.tag("LessThanEquals");
	public final static Symbol _GreaterThanEquals = Symbol.tag("GreaterThanEquals");
	public final static Symbol _LessThan = Symbol.tag("LessThan");
	public final static Symbol _GreaterThan = Symbol.tag("GreaterThan");

	public final static Symbol _InstanceOf = Symbol.tag("InstanceOf");

	public final static Symbol _LeftShift = Symbol.tag("LeftShift");
	public final static Symbol _RightShift = Symbol.tag("RightShift");
	public final static Symbol _ArithmeticRightShift = Symbol.tag("ArithmeticRightShift");
	public final static Symbol _LogicalRightShift = Symbol.tag("LogicalRightShift");

	public final static Symbol _Add = Symbol.tag("Add");
	public final static Symbol _Sub = Symbol.tag("Sub");
	public final static Symbol _Mul = Symbol.tag("Mul");
	public final static Symbol _Div = Symbol.tag("Div");
	public final static Symbol _Mod = Symbol.tag("Mod");

	public final static Symbol _Inc = Symbol.tag("Inc");
	public final static Symbol _Dec = Symbol.tag("Dec");
	public final static Symbol _SuffixInc = Symbol.tag("SuffixInc");
	public final static Symbol _SuffixDec = Symbol.tag("SuffixDec");
	public final static Symbol _PreInc = Symbol.tag("PreInc");
	public final static Symbol _PreDec = Symbol.tag("PreDec");
	public final static Symbol _PrefixInc = Symbol.tag("PrefixInc");
	public final static Symbol _PrefixDec = Symbol.tag("PrefixDec");
	public final static Symbol _Plus = Symbol.tag("Plus");
	public final static Symbol _Minus = Symbol.tag("Minus");
	public final static Symbol _Compl = Symbol.tag("Compl");
	public final static Symbol _Not = Symbol.tag("Not");

	public final static Symbol _MethodApply = Symbol.tag("MethodApply");
	public final static Symbol _Indexer = Symbol.tag("Indexer");
	public final static Symbol _Field = Symbol.tag("Field");

	public final static Symbol _This = Symbol.tag("This");
	public final static Symbol _Super = Symbol.tag("Super");
	public final static Symbol _Final = Symbol.tag("Final");
	public final static Symbol _Apply = Symbol.tag("Apply");
	public final static Symbol _New = Symbol.tag("New");
	public final static Symbol _NewArray = Symbol.tag("NewArray");
	public final static Symbol _Lambda = Symbol.tag("Lambda");
	public final static Symbol _AddArgumentExpressionList = Symbol.tag("AddArgumentExpressionList");
	public final static Symbol _LambdaParameters = Symbol.tag("LambdaParameters");
	public final static Symbol _InferredFormalParameterList = Symbol.tag("InferredFormalParameterList");

	public final static Symbol _QualifiedName = Symbol.tag("QualifiedName");
	public final static Symbol _Null = Symbol.tag("Null");
	public final static Symbol _True = Symbol.tag("True");
	public final static Symbol _False = Symbol.tag("False");
	public final static Symbol _Long = Symbol.tag("Long");
	public final static Symbol _Float = Symbol.tag("Float");
	public final static Symbol _Integer = Symbol.tag("Integer");
	public final static Symbol _Double = Symbol.tag("Double");
	public final static Symbol _String = Symbol.tag("String");
	public final static Symbol _Character = Symbol.tag("Character");

	public final static Symbol _PackageDeclaration = Symbol.tag("PackageDeclaration");
	public final static Symbol _ImportDeclaration = Symbol.tag("ImportDeclaration");
	public final static Symbol _StaticImportDeclaration = Symbol.tag("StaticImportDeclaration");
	public final static Symbol _VarName = Symbol.tag("VarName");
	public final static Symbol _Tvoid = Symbol.tag("Tvoid");
	public final static Symbol _Tbyte = Symbol.tag("Tbyte");
	public final static Symbol _Tshort = Symbol.tag("Tshort");
	public final static Symbol _Tint = Symbol.tag("Tint");
	public final static Symbol _Tlong = Symbol.tag("Tlong");
	public final static Symbol _Tfloat = Symbol.tag("Tfloat");
	public final static Symbol _Tdouble = Symbol.tag("Tdouble");
	public final static Symbol _Tarray = Symbol.tag("Tarray");
	public final static Symbol _Comma = Symbol.tag("Comma");
	public final static Symbol _HashIn = Symbol.tag("HashIn");

	public final static Symbol _Object = Symbol.tag("Object");
	public final static Symbol _Property = Symbol.tag("Property");

}
