
function sqrt(y: float): float {
        float x = y / 2;
        int i = 0;
        while (i < 10000) {
                x = (x + y/x) / 2;
                i = i + 1;
        }
        return x;
}

println('''#sqrt(2.0): ${sqrt(2.0)}''');
assert(1.4142135623730951 - sqrt(2.0) <= 0.0001);

