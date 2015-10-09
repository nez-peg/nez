
function WhileTest() {
	var n : int = 0;
	while(n < 10) {
		assert(n < 10);
		n = n + 1;
	}
	println("#while: %d", n);
	assert(n == 10);
}

function NestedWhileTest() {
	var n : int = 0;
	while(n < 10) {
		var m : int = 0;
		while(m < 10) {
			assert(m < 10);
			m = m + 1;
		}
		assert(m == 10);
		n = n + 1;
	}
	println("#while/while: %d", n);
	assert(n == 10);
}

function WhileNtimes(n: int) {
	var count = 0;
	while(count < n) {
		count = count + 1;
	}
	println("#count: %d", count);
	assert(count == n);
}


WhileTest();
NestedWhileTest();
WhileNtimes(100);
WhileNtimes(1000);
WhileNtimes(10000);
WhileNtimes(100000);
WhileNtimes(1000000);
WhileNtimes(10000000);



