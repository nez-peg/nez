
function test(x:int, y:int) {
        assert(x == 123);
        assert(y == 122);
        println("#x=123,y=122,x+y: %d", (x+y));
        println("#x=123,y=122,x-y: %d", (x-y));
        println("#x=123,y=122,x*y: %d", (x*y));
        println("#x=123,y=122,x/y: %d", (x/y));
        println("#x=123,y=122,x%%y: %d", (x%y));
        println("#x=123,y=122,x==y: %s", (x==y));
        println("#x=123,y=122,x!=y: %s", (x!=y));
        println("#x=123,y=122,x<y: %s", (x<y));
        println("#x=123,y=122,x<=y: %s", (x<=y));
        println("#x=123,y=122,x>y: %s", (x>y));
        println("#x=123,y=122,x>=y: %s", (x>=y));
        
        println("#y=122,123+y: %d", (123+y));
        println("#y=122,123-y: %d", (123-y));
        println("#y=122,123*y: %d", (123*y));
        println("#y=122,123/y: %d", (123/y));
        println("#y=122,123%%y: %d", (123%y));
        println("#y=122,123==y: %s", (123==y));
        println("#y=122,123!=y: %s", (123!=y));
        println("#y=122,123<y: %s", (123<y));
        println("#y=122,123<=y: %s", (123<=y));
        println("#y=122,123>y: %s", (123>y));
        println("#y=122,123>=y: %s", (123>=y));
        
        println("#x=123,x+122: %d", (x+122));
        println("#x=123,x-122: %d", (x-122));
        println("#x=123,x*122: %d", (x*122));
        println("#x=123,x/122: %d", (x/122));
        println("#x=123,x%%122: %d", (x%122));
        println("#x=123,x==122: %s", (x==122));
        println("#x=123,x!=122: %s", (x!=122));
        println("#x=123,x<122: %s", (x<122));
        println("#x=123,x<=122: %s", (x<=122));
        println("#x=123,x>122: %s", (x>122));
        println("#x=123,x>=122: %s", (x>=122));

        println("#123-122: %d", (123-122));
        println("#123*122: %d", (123*122));
        println("#123/122: %d", (123/122));
        println("#123%%122: %d", (123%122));
        println("#123==122: %s", (123==122));
        println("#123!=122: %s", (123!=122));
        println("#123<122: %s", (123<122));
        println("#123<=122: %s", (123<=122));
        println("#123>122: %s", (123>122));
        println("#123>=122: %s", (123>=122));

        assert((x+y) == 245);
        assert((x-y) == 1);
        assert((x*y) == 15006);
        assert((x/y) == 1);
        assert((x%y) == 1);
        assert((x==y) == false);
        assert((x!=y) == true);
        assert((x<y) == false);
        assert((x>y) == true);
        assert((x>=y) == true);
        
        assert((123+y) == 245);
        assert((123-y) == 1);
        assert((123*y) == 15006);
        assert((123/y) == 1);
        assert((123%y) == 1);
        assert((123==y) == false);
        assert((123!=y) == true);
        assert((123<y) == false);
        assert((123>y) == true);
        assert((123>=y) == true);
        
        assert((x+122) == 245);
        assert((x-122) == 1);
        assert((x*122) == 15006);
        assert((x/122) == 1);
        assert((x%122) == 1);
        assert((x==122) == false);
        assert((x!=122) == true);
        assert((x<122) == false);
        assert((x>122) == true);
        assert((x>=122) == true);

        assert((123+122) == 245);
        assert((123-122) == 1);
        assert((123*122) == 15006);
        assert((123/122) == 1);
        assert((123%122) == 1);
        assert((123==122) == false);
        assert((123!=122) == true);
        assert((123<122) == false);
        assert((123>122) == true);
        assert((123>=122) == true);
}

test(123,122);

