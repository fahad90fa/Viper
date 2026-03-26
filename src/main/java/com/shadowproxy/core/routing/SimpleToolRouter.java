package com.shadowproxy.core.routing;

import com.shadowproxy.domain.http.HttpExchangeRecord;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class SimpleToolRouter implements ToolRouter {
    private final Map<ToolType, List<HttpExchangeRecord>> queues = new EnumMap<>(ToolType.class);
    private final Map<ToolType, CopyOnWriteArrayList<ToolRouterListener>> listeners = new EnumMap<>(ToolType.class);

    public SimpleToolRouter() {
        for (ToolType type : ToolType.values()) {
            queues.put(type, new ArrayList<>());
            listeners.put(type, new CopyOnWriteArrayList<>());
        }
    }

    @Override
    public synchronized void sendToTool(ToolType toolType, HttpExchangeRecord exchangeRecord) {
        queues.get(toolType).add(exchangeRecord);
        listeners.get(toolType).forEach(listener -> listener.onExchangeRouted(toolType, exchangeRecord));
    }

    @Override
    public synchronized List<HttpExchangeRecord> getToolQueue(ToolType toolType) {
        return List.copyOf(queues.get(toolType));
    }

    @Override
    public synchronized List<HttpExchangeRecord> drainToolQueue(ToolType toolType) {
        List<HttpExchangeRecord> drained = List.copyOf(queues.get(toolType));
        queues.get(toolType).clear();
        return drained;
    }

    @Override
    public void addListener(ToolType toolType, ToolRouterListener listener) {
        listeners.get(toolType).add(listener);
    }

    @Override
    public void removeListener(ToolType toolType, ToolRouterListener listener) {
        listeners.get(toolType).remove(listener);
    }
}
