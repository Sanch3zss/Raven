package sanch3zss.raven.feature.server.funtime;

import java.util.List;

import sanch3zss.raven.client.service.ReconnectHandler;
import sanch3zss.raven.feature.server.*;

public class FunTimeServerHandler implements ReconnectHandler {
    public static final String funtimeId = "funtime";

    @Override
    public List<String> getServerIds() {
        return List.of(funtimeId, "spookytime");
    }

    @Override
    public ReconnectTask createProcess() {
        return new FunTimeProcess();
    }
}
