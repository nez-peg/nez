// written by kimio

function fibo(n: int): int {
	if(n == 1 || n == 2) {
		return 1;
	}
	return fibo(n-1) + fibo(n-2);
}

function main() {
	println("#fibo(1): %s",  fibo(1));
	println("#fibo(2): %s",  fibo(2));
	println("#fibo(3): %s",  fibo(3));
	println("#fibo(10): %s",  fibo(10));
	assert(fibo(1) == 1);
	assert(fibo(2) == 1);
	assert(fibo(3) == 2);
	assert(fibo(10) == 55);
}

main()


