package sanch3zss.raven.feature.server.reallyworld;

import java.util.List;

import sanch3zss.raven.client.service.ReconnectHandler;
import sanch3zss.raven.feature.server.*;

public class ReallyWorldServerHandler implements ReconnectHandler {
    @Override
    public List<String> getServerIds() {
        return List.of("reallyworld");
    }

    @Override
    public ReconnectTask createProcess() {
        return new ReallyWorldProcess();
    }
}
