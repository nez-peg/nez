
function WhileTest() {
	int n = 0;
	while(n < 10) {
		assert(n < 10);
		if(n == 5) {
			break;
		}
		n = n + 1;
	}
	println("#while/break: %s",  n);
	assert(n == 5);
}

function WhileWhateverTest() {
        int n = 0;
        for(int n = 0; n < 10 ; n = n + 1) {
                assert(n < 10);
                if(n == 5) {
                        break;
                }
        }
        println("#while/whatever/break: %s",  n);
        assert(n == 5);
}


function NestedWhileTest() {
	int n = 0;
	while(n < 10) {
		int n = 0;
		while(m < 10) {
			assert(m < 10);
			if(m == 5) {
				break;
			}
			m = m + 1;
		}
		assert(m == 5);
		if(n == 5) {
			break;
		}
		n = n + 1;
	}
	println("#while/while/break: %s",  n);
	assert(n == 5);
}


WhileTest();
WhileWhateverTest();
NestedWhileTest();


