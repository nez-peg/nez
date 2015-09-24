package nez.peg.tpeg;

import nez.peg.tpeg.TypedPEG.LabeledExpr;
import nez.peg.tpeg.TypedPEG.SequenceExpr;

/**
 * Created by skgchxngsxyz-opensuse on 15/09/02.
 */
public class LabeledExprDetector extends BaseVisitor<Boolean, Void> {

	@Override
	public Boolean visitDefault(TypedPEG expr, Void param) {
		return false;
	}

	@Override
	public Boolean visitSequenceExpr(SequenceExpr expr, Void param) {
		for (TypedPEG e : expr.getExprs()) {
			if (this.visit(e)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Boolean visitLabeledExpr(LabeledExpr expr, Void param) {
		return true;
	}
}
