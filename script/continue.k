

function WhileContinueTest() {
	int n = 0;
	int c = 0;
	while(n < 10) {
		assert(n < 10);
		if(n % 2 == 0) {
			n = n + 1;
			continue;
		}
		c = c + 1;
		n = n + 1;
	}
	println("#while/continue: %s %s",  n,  c);
	assert(c == 5);
}

function WhileNextContinue() {
	int i;
	int c;
	for(i = 0; i < 10; i = i + 1) {
		assert(i < 10);
		if(i % 2 == 0) {
			continue;
		}
		c = c + 1;
	}
	println("#for/continue %s %s",  i,  c);
	assert(c == 5);
}

WhileContinueTest();
WhileNextContinue();


