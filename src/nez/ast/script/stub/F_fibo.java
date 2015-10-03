package nez.ast.script.stub;

import konoha.Function;

public class F_fibo implements Function {
	public final int f(int n) {
		if (n < 3) {
			return 1;
		}
		return this.f(n - 1) + this.f(n - 2);
	}
}
