package com.shadowproxy;

import com.shadowproxy.core.routing.SimpleToolRouter;
import com.shadowproxy.core.routing.ToolType;
import com.shadowproxy.domain.http.HttpExchangeRecord;
import com.shadowproxy.domain.http.HttpRequestRecord;
import com.shadowproxy.domain.http.HttpResponseRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleToolRouterTest {
    @Test
    void routesExchangeToRequestedQueue() {
        SimpleToolRouter router = new SimpleToolRouter();
        HttpExchangeRecord record = new HttpExchangeRecord(
                UUID.randomUUID(),
                new HttpRequestRecord("POST", "/login", Map.of(), new byte[0], Instant.now()),
                new HttpResponseRecord(401, "Unauthorized", Map.of(), new byte[0], Instant.now()),
                "PROXY"
        );

        router.sendToTool(ToolType.REPEATER, record);
        assertEquals(1, router.getToolQueue(ToolType.REPEATER).size());
        assertEquals(0, router.getToolQueue(ToolType.SCANNER).size());
    }
}
