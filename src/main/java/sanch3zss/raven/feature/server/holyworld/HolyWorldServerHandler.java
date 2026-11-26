package sanch3zss.raven.feature.server.holyworld;

import java.util.List;

import sanch3zss.raven.client.service.ReconnectHandler;
import sanch3zss.raven.feature.server.*;

public class HolyWorldServerHandler implements ReconnectHandler {
    @Override
    public List<String> getServerIds() {
        return List.of("holyworld");
    }

    @Override
    public ReconnectTask createProcess() {
        return new HolyWorldProcess();
    }
}
