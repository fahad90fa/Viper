package com.shadowproxy.core.proxy;

import com.shadowproxy.domain.http.HttpRequestRecord;
import com.shadowproxy.domain.http.HttpResponseRecord;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProxyInterceptionPipeline {
    private final CopyOnWriteArrayList<RequestInterceptor> requestInterceptors = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ResponseInterceptor> responseInterceptors = new CopyOnWriteArrayList<>();

    public HttpRequestRecord applyRequestInterceptors(HttpRequestRecord requestRecord) {
        HttpRequestRecord current = requestRecord;
        for (RequestInterceptor interceptor : requestInterceptors) {
            current = interceptor.intercept(current);
        }
        return current;
    }

    public HttpResponseRecord applyResponseInterceptors(HttpResponseRecord responseRecord) {
        HttpResponseRecord current = responseRecord;
        for (ResponseInterceptor interceptor : responseInterceptors) {
            current = interceptor.intercept(current);
        }
        return current;
    }

    public void addRequestInterceptor(RequestInterceptor interceptor) {
        requestInterceptors.add(interceptor);
    }

    public void addResponseInterceptor(ResponseInterceptor interceptor) {
        responseInterceptors.add(interceptor);
    }

    public List<RequestInterceptor> requestInterceptors() {
        return List.copyOf(requestInterceptors);
    }

    public List<ResponseInterceptor> responseInterceptors() {
        return List.copyOf(responseInterceptors);
    }
}
