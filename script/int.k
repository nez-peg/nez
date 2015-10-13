// int

int x = 7;
int y = 3;

assert(x + y == 10);
assert(x - y == 4);
assert(x * y == 21);
assert(x / y == 2);
assert(x % y == 1);

assert(!(x == y));
assert(x != y);
assert(!(x < y));
assert(x > y);
assert(!(x <= y));
assert(x >= y);

void f(int x, int y) {
 assert(x + y == 10);
 assert(x - y == 4);
 assert(x * y == 21);
 assert(x / y == 2);
 assert(x % y == 1);
 assert(!(x == y));
 assert(x != y);
 assert(!(x < y));
 assert(x > y);
 assert(!(x <= y));
 assert(x >= y);
}

f(7, 3);

println("int ok");

