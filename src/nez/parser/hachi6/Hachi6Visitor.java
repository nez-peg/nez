package nez.parser.hachi6;

public abstract class Hachi6Visitor {

	// Control

	public abstract void visitNop(Hachi6.Nop inst);

	public abstract void visitLabel(Hachi6.Label inst);

	public abstract void visitCov(Hachi6.Cov inst);

	public abstract void visitExit(Hachi6.Exit inst);

	public abstract void visitPos(Hachi6.Pos inst);

	public abstract void visitBack(Hachi6.Back inst);

	public abstract void visitMove(Hachi6.Move inst);

	public abstract void visitJump(Hachi6.Jump inst);

	public abstract void visitCall(Hachi6.Call inst);

	public abstract void visitRet(Hachi6.Ret inst);

	public abstract void visitAlt(Hachi6.Alt inst);

	public abstract void visitSucc(Hachi6.Succ inst);

	public abstract void visitFail(Hachi6.Fail inst);

	public abstract void visitGuard(Hachi6.Guard inst);

	public abstract void visitSkip(Hachi6.Skip inst);

	// Character Matching

	public abstract void visitByte(Hachi6.Byte inst);

	public abstract void visitStr(Hachi6.Str inst);

	public abstract void visitSet(Hachi6.Set inst);

	public abstract void visitAny(Hachi6.Any inst);

	public abstract void visitNByte(Hachi6.NByte inst);

	public abstract void visitNStr(Hachi6.NStr inst);

	public abstract void visitNSet(Hachi6.NSet inst);

	public abstract void visitNAny(Hachi6.NAny inst);

	public abstract void visitOByte(Hachi6.OByte inst);

	public abstract void visitOStr(Hachi6.OStr inst);

	public abstract void visitOSet(Hachi6.OSet inst);

	// public abstract void visitOAny(Hachi6.OAny inst); //

	public abstract void visitRByte(Hachi6.RByte inst);

	public abstract void visitRSet(Hachi6.RSet inst); //

	public abstract void visitRStr(Hachi6.RStr inst); //
	// public abstract void visitRAny(Hachi6.RAny inst); //

	// Memoization

	public abstract void visitLookup(Hachi6.Lookup inst);

	public abstract void visitMemo(Hachi6.Memo inst);

	public abstract void visitFailMemo(Hachi6.FailMemo inst);

	// AST

	public abstract void visitPushTree(Hachi6.PushTree inst);

	public abstract void visitPopTree(Hachi6.PopTree inst);

	public abstract void visitInit(Hachi6.Init inst);

	public abstract void visitNew(Hachi6.New inst);

	public abstract void visitTag(Hachi6.Tag inst);

	public abstract void visitValue(Hachi6.Value inst);

	public abstract void visitLink(Hachi6.Link inst);

	public abstract void visitEmit(Hachi6.Emit inst);

	public abstract void visitLeftFold(Hachi6.LeftFold inst);

	public abstract void visitSinit(Hachi6.Sinit inst);

	public abstract void visitSnew(Hachi6.Snew inst);

	// Dispatch

	public abstract void visitDispatch(Hachi6.Dispatch inst); //

	public abstract void visitEDispatch(Hachi6.EDispatch inst); //

	// Symbol Table

	public abstract void visitSOpen(Hachi6.SOpen inst);

	public abstract void visitSClose(Hachi6.SClose inst);

	public abstract void visitSMask(Hachi6.SMask inst);

	public abstract void visitSDef(Hachi6.SDef inst);

	public abstract void visitSIsDef(Hachi6.SIsDef inst);

	public abstract void visitSExists(Hachi6.SExists inst);

	public abstract void visitSMatch(Hachi6.SMatch inst);

	public abstract void visitSIs(Hachi6.SIs inst);

	public abstract void visitSIsa(Hachi6.SIsa inst);

}
