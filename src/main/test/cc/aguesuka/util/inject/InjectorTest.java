package cc.aguesuka.util.inject;

import cc.aguesuka.util.inject.annotation.Config;
import cc.aguesuka.util.inject.annotation.Init;
import cc.aguesuka.util.inject.annotation.Inject;
import org.junit.Test;

/**
 * @author :yangmingyuxing
 * 2019/7/10 22:56
 */
public class InjectorTest {

    @Test
    public void t1() {
        Injector injector = new Injector();
        injector.addClass(A.class, B.class, C.class);
        injector.addProperties("config.properties");
        injector.build();
        injector.instanceByClass(B.class);
        A a = injector.instanceByName("a");
        B b = injector.instanceByClass(B.class);
        C c = injector.instanceByClass(C.class);
        assert a.a == b;
        assert a.b == b;
        assert a.c == c;
        assert b.a == a;
        assert b.b == b;
        assert b.c == null;
        assert c.a == a;
        assert c.b == b;
        assert c.c == c;
        assert a.timeout == 1000;
        assert a.init == 'a';
        assert b.init == 'b';
    }

    @Inject("a")
    private static class A {
        @Inject("b")
        private A a;
        @Inject("b")
        private B b;
        @Inject
        private C c;
        @Config("udp.timeout")
        private int timeout;
        private char init;

        @Init
        private void p() {
            init = 'a';
        }
    }

    @Inject("b")
    private static class B extends A {
        @Inject("b")
        private B b;
        @Inject("a")
        private A a;
        private C c;
        private char init;

        @Init
        private void p() {
            init = 'b';
        }
    }

    private static class C {
        @Inject("a")
        private A a;
        @Inject
        private B b;
        @Inject
        private C c;
        private C() {
        }
        private C(int a) {

        }
    }
}