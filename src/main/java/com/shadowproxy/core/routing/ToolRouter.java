package com.shadowproxy.core.routing;

import com.shadowproxy.domain.http.HttpExchangeRecord;

import java.util.List;

public interface ToolRouter {
    void sendToTool(ToolType toolType, HttpExchangeRecord exchangeRecord);

    List<HttpExchangeRecord> getToolQueue(ToolType toolType);

    List<HttpExchangeRecord> drainToolQueue(ToolType toolType);

    void addListener(ToolType toolType, ToolRouterListener listener);

    void removeListener(ToolType toolType, ToolRouterListener listener);
}
