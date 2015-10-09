
function g() : void {
	println("#Func<void>");
}

function f() : int {
	return 1;
}

function f(n: int) : int {
	return n + 1;
}

function f(a: int, b : int) : int {
	return a + b;
}

function main() {
	g();
	println("#Func<int>: %d",f());
	println("#Func<int,int>: %d", f(1));
	println("#Func<int,int,int>: %d", f(1,2));
	assert(f() == 1);
	assert(f(1) == 2);
	assert(f(1,2) == 3);
}

main()


