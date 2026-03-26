package com.shadowproxy.core.routing;

import com.shadowproxy.domain.http.HttpExchangeRecord;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SimpleToolRouter implements ToolRouter {
    private final Map<ToolType, List<HttpExchangeRecord>> queues = new EnumMap<>(ToolType.class);

    public SimpleToolRouter() {
        for (ToolType type : ToolType.values()) {
            queues.put(type, new ArrayList<>());
        }
    }

    @Override
    public synchronized void sendToTool(ToolType toolType, HttpExchangeRecord exchangeRecord) {
        queues.get(toolType).add(exchangeRecord);
    }

    @Override
    public synchronized List<HttpExchangeRecord> getToolQueue(ToolType toolType) {
        return List.copyOf(queues.get(toolType));
    }
}
