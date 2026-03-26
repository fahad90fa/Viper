package com.shadowproxy.core.routing;

import com.shadowproxy.domain.http.HttpExchangeRecord;

@FunctionalInterface
public interface ToolRouterListener {
    void onExchangeRouted(ToolType toolType, HttpExchangeRecord exchangeRecord);
}
