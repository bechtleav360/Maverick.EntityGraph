package org.av360.maverick.graph.feature.navigation.domain.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.Optional;

/**
 * Simple cache which to store previous session information
 */
@Service
public class SessionHistory {

    final Cache<String, History> cache;

    public SessionHistory() {
        this.cache = Caffeine.newBuilder().expireAfterAccess(Duration.of(10, ChronoUnit.MINUTES)).build();
    }

    public Optional<History> get(String cookie) {
        if(StringUtils.hasLength(cookie)) {
            return Optional.ofNullable(cache.getIfPresent(cookie));
        } else return Optional.empty();

    }

    private void init(String cookie, String url) {
        cache.put(cookie, new History(url));
    }

    public void add(String cookie, String path) {
        if(StringUtils.hasLength(cookie)) {
            this.get(cookie).ifPresentOrElse(history -> history.add(path), () -> cache.put(cookie, new History(path)));
        }

    }

    public Optional<String> previous(String cookie) {
        return this.get(cookie).map(history -> history.get(1));
    }


    private class History {
        private final LinkedList<String> urls;
        public History(String start) {
            this.urls = new LinkedList<>();
            this.urls.add(0, start);
        }

        public void add(String path) {
            this.urls.add(0, path);
        }


        public String get(int index) {
            if(this.urls.size() > index) {
                return this.urls.get(index);
            } else return null;

        }




    }
}
