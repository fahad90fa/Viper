package com.shadowproxy.persistence;

import com.shadowproxy.domain.http.HttpExchangeRecord;

public interface HistoryListener {
    void onExchangeSaved(HttpExchangeRecord exchangeRecord);
}
