package com.shadowproxy.core.proxy;

import com.shadowproxy.domain.http.HttpRequestRecord;

public interface RequestInterceptor {
    HttpRequestRecord intercept(HttpRequestRecord requestRecord);
}
