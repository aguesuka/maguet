package cc.aguesuka.dht.action;

import cc.aguesuka.run.BeansFactory;
import cc.aguesuka.util.HexUtil;
import cc.aguesuka.util.inject.Injector;
import cc.aguesuka.util.stop.Timeout;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * @author :yangmingyuxing
 * 2019/7/14 10:30
 */
public class DhtActionTest {


    @Test
    public void parserGetPeersResultValue() {
    }

    @Test
    public void getPeers() {
        try (Injector injector = BeansFactory.createInjector()) {
            DhtAction dhtAction = injector.instanceByClass(DhtAction.class);
            InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("218.204.144.196"), 1373);
            dhtAction.getPeers(HexUtil.decode("25A10C0D410CB98352A8256219256FB31989B2EC"), address, Timeout.getMilliSecond(0));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}