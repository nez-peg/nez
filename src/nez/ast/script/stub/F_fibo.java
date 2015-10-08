package nez.ast.script.stub;

import konoha.Function;

public class F_fibo extends Function {
	public final int invoke(int n) {
		if (n < 3) {
			return 1;
		}
		return this.invoke(n - 1) + this.invoke(n - 2);
	}
}
