// written by kimio kuramitsu

boolean TestFunc0() {
	int i = 0;
	int c = 0;
	while(i < 10) {
		i = i + 1;
		if(i > 5) {
			break;
		}
		c = c + 1;
	}
	
	assert(c == 5);
	return true;
}

boolean TestFunc1() {
	int i = 0;
	int c = 0;
	while(i < 10) {
		i = i + 1;
		if(i % 2 == 0) {
			continue;
		}
		c = c + 1;
	}
	// System.p("c="+c+",i="+i);
	assert(c == 5);
	return true;
}

boolean TestFunc2() {
	int i = 0;
	while(false) {
		i = i + 1;
	}
	assert(i == 0);
	return true;
}

boolean TestFunc3() {
	int i = 0;
	int c = 0;
	while(i < 10) {
		i = i + 1;
		if(i > 5) break;
		c = c + 1;
	}
	assert(c == 5);
	return true;
}

boolean TestFunc4() {
	int i = 0;
	int c = 0;
	while(i < 10) {
		i = i + 1;
		if(i % 2 == 0) continue;
		c = c + 1;
	}
	assert(c == 5);
	return true;
}

boolean TestFunc5() {
	int i = 0;
	int c = 0;
	while(i < 10) {
		int j = 0;
		while(j < 10) {
			j = j + 1;
			c = c + 1;
		}
		i = i + 1;
	}
	assert(c == 100);
	return true;
}

boolean TestFunc6() {
	int i = 0;
	while(i < 10) i = i + 1;
	assert(i == 10);
	return true;
}

assert(TestFunc0());
assert(TestFunc1());
assert(TestFunc2());
assert(TestFunc3());
assert(TestFunc4());
assert(TestFunc5());
assert(TestFunc6());
