package cc.aguesuka.util.inject.help;

import cc.aguesuka.util.HexUtil;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author :yangmingyuxing
 * 2019/7/11 17:17
 */
public class ClassUtilTest {

    @Test
    public void classParent() {
        assert ClassUtil.classParent(ClassUtil.class).equals(new HashSet<>(Arrays.asList(ClassUtil.class, Object.class)));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void typeCast() {
        assertEquals(true, ClassUtil.typeCast("true", boolean.class));
        assertEquals(false, ClassUtil.typeCast("false", boolean.class));
        assertEquals(false, ClassUtil.typeCast("fa", boolean.class));
        assertEquals(false, ClassUtil.typeCast("FALSE", boolean.class));
        assertEquals(true, ClassUtil.typeCast("TRUE", boolean.class));
        assertNull(ClassUtil.typeCast(null, boolean.class));
        assertEquals("TRUE", ClassUtil.typeCast("TRUE", String.class));
        assertEquals("f", ClassUtil.typeCast("f", String.class));
        assertEquals("", ClassUtil.typeCast("", String.class));
        assertEquals("%**中文", ClassUtil.typeCast("%**中文", String.class));
        assert Arrays.equals(HexUtil.decode("919DB3D06BC430B7323BA122F622919DE18B1F41"),
                ClassUtil.typeCast("919DB3D06BC430B7323BA122F622919DE18B1F41", byte[].class));
        assert 12345 == ClassUtil.typeCast("12345", int.class);
        assert -12345 == ClassUtil.typeCast("-12345", int.class);
        assert -12345.5 == ClassUtil.typeCast("-12345.5", double.class);
        assert -12345e2 == ClassUtil.typeCast("-12345e2", double.class);
    }
}