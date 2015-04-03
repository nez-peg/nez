import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

// モデル Order のテストという前提
public class JUnitSample {

    // テスト対象
    private JUnitSample target = null;

    @Before
    public void before() {
        System.out.println("setUp");
        this.target = new JUnitSample();
    }

    @After
    public void after() {
        System.out.println("tearDown");
        this.target = null;
    }

    // 全テストで１回だけ実施
    @BeforeClass
    public static void beforeAll() {
        System.out.println("beforeAll");
    }

    // 全テストで１回だけ実施
    @AfterClass
    public static void afterAll() {
        System.out.println("afterAll");
    }


// ↓テストケース
    // 通常のケース
    @Test
    public void test() throws Exception {
        assertThat(this.target, is(notNullValue()));
    }


    // 例外発生の検証
    @Test(expected=NullPointerException.class)
    public void expectError() {
        this.target = null;
        this.target.toString();
    }


    // 無視の検証
    @Test(expected=IllegalArgumentException.class)
    @Ignore("test")
    public void unexpectError() {
        this.target = null;
        this.target.toString();
        System.out.println("This case will be ignored");
    }


    // 想定時間内に処理が終了することの検証
    @Test(timeout=1000)
    public void testTimeout() {
        for (int i = 0; i < 1000; i++) {
            System.out.println("sleeping...");
        }
    }
}
