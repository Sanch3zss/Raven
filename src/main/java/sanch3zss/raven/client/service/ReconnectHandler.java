package sanch3zss.raven.client.service;

import java.util.List;

import sanch3zss.raven.feature.server.ReconnectTask;

public interface ReconnectHandler {
    List<String> getServerIds();

    ReconnectTask createProcess();
}
