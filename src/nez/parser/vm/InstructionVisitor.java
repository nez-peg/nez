package nez.parser.vm;

public abstract class InstructionVisitor {

	/* Machine Control */
	public abstract void visitNop(Moz.Nop inst); // 7-bit

	public abstract void visitExit(Moz.Exit inst); // 7-bit only

	public abstract void visitCov(Moz.Cov inst);

	/* Control */

	public abstract void visitPos(Moz.Pos inst); // Pos

	public abstract void visitBack(Moz.Back inst); // Back

	public abstract void visitMove(Moz.Move inst); //

	public abstract void visitJump(Moz.Jump inst); // Jump

	public abstract void visitCall(Moz.Call inst); // Call

	public abstract void visitRet(Moz.Ret inst); // Ret

	public abstract void visitAlt(Moz.Alt inst); // Alt

	public abstract void visitSucc(Moz.Succ inst); // Succ

	public abstract void visitFail(Moz.Fail inst); // Fail

	public abstract void visitGuard(Moz.Guard inst); // Skip

	public abstract void visitStep(Moz.Step inst); // Skip

	/* Matching */

	public abstract void visitByte(Moz.Byte inst); // match a byte character

	public abstract void visitAny(Moz.Any inst); // match any

	public abstract void visitStr(Moz.Str inst); // match string

	public abstract void visitSet(Moz.Set inst); // match set

	public abstract void visitNByte(Moz.NByte inst); //

	public abstract void visitNAny(Moz.NAny inst); //

	public abstract void visitNStr(Moz.NStr inst); //

	public abstract void visitNSet(Moz.NSet inst); //

	public abstract void visitOByte(Moz.OByte inst); //

	// public abstract void visitOAny(Moz.OAny inst); //

	public abstract void visitOStr(Moz.OStr inst); //

	public abstract void visitOSet(Moz.OSet inst); //

	public abstract void visitRByte(Moz.RByte inst); //

	// public abstract void visitRAny(Moz.RAny inst); //

	public abstract void visitRStr(Moz.RStr inst); //

	public abstract void visitRSet(Moz.RSet inst); //

	/* Dispatch */

	public abstract void visitDispatch(Moz.Dispatch inst); //

	public abstract void visitDDispatch(Moz.DDispatch inst); // Dfa

	/* Matching */

	public abstract void visitTPush(Moz.TPush inst);

	public abstract void visitTPop(Moz.TPop inst);

	public abstract void visitTBegin(Moz.TBegin inst);

	public abstract void visitTEnd(Moz.TEnd inst);

	public abstract void visitTTag(Moz.TTag inst);

	public abstract void visitTReplace(Moz.TReplace inst);

	public abstract void visitTLink(Moz.TLink inst);

	public abstract void visitTFold(Moz.TFold inst);

	public abstract void visitTStart(Moz.TStart inst);

	public abstract void visitTEmit(Moz.TEmit inst);

	// public abstract void visitTAbort(Moz.TAbort inst);

	/* Symbol */

	public abstract void visitSOpen(Moz.SOpen inst);

	public abstract void visitSClose(Moz.SClose inst);

	public abstract void visitSMask(Moz.SMask inst);

	public abstract void visitSDef(Moz.SDef inst);

	public abstract void visitSIsDef(Moz.SIsDef inst);

	public abstract void visitSExists(Moz.SExists inst);

	public abstract void visitSMatch(Moz.SMatch inst);

	public abstract void visitSIs(Moz.SIs inst);

	public abstract void visitSIsa(Moz.SIsa inst);

	/* Number */

	public abstract void visitNScan(Moz.NScan inst);

	public abstract void visitNDec(Moz.NDec inst);

	/* memoization */

	public abstract void visitLookup(Moz.Lookup inst); // match a character

	public abstract void visitMemo(Moz.Memo inst); // match a character

	public abstract void visitMemoFail(Moz.MemoFail inst); // match a character

	public abstract void visitTLookup(Moz.TLookup inst);

	public abstract void visitTMemo(Moz.TMemo inst);

}
