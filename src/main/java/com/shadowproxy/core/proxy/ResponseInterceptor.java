package com.shadowproxy.core.proxy;

import com.shadowproxy.domain.http.HttpResponseRecord;

public interface ResponseInterceptor {
    HttpResponseRecord intercept(HttpResponseRecord responseRecord);
}
