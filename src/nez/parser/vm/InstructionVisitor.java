package nez.parser.vm;

public abstract class InstructionVisitor {

	/* Machine Control */
	public abstract void visitNop(Moz86.Nop inst); // 7-bit

	public abstract void visitExit(Moz86.Exit inst); // 7-bit only

	public abstract void visitCov(Moz86.Cov inst);

	/* Control */

	public abstract void visitPos(Moz86.Pos inst); // Pos

	public abstract void visitBack(Moz86.Back inst); // Back

	public abstract void visitMove(Moz86.Move inst); //

	public abstract void visitJump(Moz86.Jump inst); // Jump

	public abstract void visitCall(Moz86.Call inst); // Call

	public abstract void visitRet(Moz86.Ret inst); // Ret

	public abstract void visitAlt(Moz86.Alt inst); // Alt

	public abstract void visitSucc(Moz86.Succ inst); // Succ

	public abstract void visitFail(Moz86.Fail inst); // Fail

	public abstract void visitGuard(Moz86.Guard inst); // Skip

	public abstract void visitStep(Moz86.Step inst); // Skip

	/* Matching */

	public abstract void visitByte(Moz86.Byte inst); // match a byte character

	public abstract void visitAny(Moz86.Any inst); // match any

	public abstract void visitStr(Moz86.Str inst); // match string

	public abstract void visitSet(Moz86.Set inst); // match set

	public abstract void visitNByte(Moz86.NByte inst); //

	public abstract void visitNAny(Moz86.NAny inst); //

	public abstract void visitNStr(Moz86.NStr inst); //

	public abstract void visitNSet(Moz86.NSet inst); //

	public abstract void visitOByte(Moz86.OByte inst); //

	// public abstract void visitOAny(Moz.OAny inst); //

	public abstract void visitOStr(Moz86.OStr inst); //

	public abstract void visitOSet(Moz86.OSet inst); //

	public abstract void visitRByte(Moz86.RByte inst); //

	// public abstract void visitRAny(Moz.RAny inst); //

	public abstract void visitRStr(Moz86.RStr inst); //

	public abstract void visitRSet(Moz86.RSet inst); //

	/* Dispatch */

	public abstract void visitDispatch(Moz86.Dispatch inst); //

	public abstract void visitDDispatch(Moz86.DDispatch inst); // Dfa

	/* Matching */

	public abstract void visitTPush(Moz86.TPush inst);

	public abstract void visitTPop(Moz86.TPop inst);

	public abstract void visitTBegin(Moz86.TBegin inst);

	public abstract void visitTEnd(Moz86.TEnd inst);

	public abstract void visitTTag(Moz86.TTag inst);

	public abstract void visitTReplace(Moz86.TReplace inst);

	public abstract void visitTLink(Moz86.TLink inst);

	public abstract void visitTFold(Moz86.TFold inst);

	public abstract void visitTStart(Moz86.TStart inst);

	public abstract void visitTEmit(Moz86.TEmit inst);

	// public abstract void visitTAbort(Moz.TAbort inst);

	/* Symbol */

	public abstract void visitSOpen(Moz86.SOpen inst);

	public abstract void visitSClose(Moz86.SClose inst);

	public abstract void visitSMask(Moz86.SMask inst);

	public abstract void visitSDef(Moz86.SDef inst);

	public abstract void visitSIsDef(Moz86.SIsDef inst);

	public abstract void visitSExists(Moz86.SExists inst);

	public abstract void visitSMatch(Moz86.SMatch inst);

	public abstract void visitSIs(Moz86.SIs inst);

	public abstract void visitSIsa(Moz86.SIsa inst);

	/* Number */

	public abstract void visitNScan(Moz86.NScan inst);

	public abstract void visitNDec(Moz86.NDec inst);

	/* memoization */

	public abstract void visitLookup(Moz86.Lookup inst); // match a character

	public abstract void visitMemo(Moz86.Memo inst); // match a character

	public abstract void visitMemoFail(Moz86.MemoFail inst); // match a
																// character

	public abstract void visitTLookup(Moz86.TLookup inst);

	public abstract void visitTMemo(Moz86.TMemo inst);

}
