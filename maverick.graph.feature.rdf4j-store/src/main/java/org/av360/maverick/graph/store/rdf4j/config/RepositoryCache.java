/*
 * Copyright (c) 2024.
 *
 *  Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 *  European Commission - subsequent versions of the EUPL (the "Licence");
 *
 *  You may not use this work except in compliance with the Licence.
 *  You may obtain a copy of the Licence at:
 *
 *  https://joinup.ec.europa.eu/software/page/eupl5
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 */

package org.av360.maverick.graph.store.rdf4j.config;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.store.rdf.LabeledRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Our own Cache implementation, since the eviction policies in Caffeine are not straightforward to use (with pinning for repositories which still have open connections).
 */
@Slf4j(topic = "graph.repo.cfg.builder")
final class RepositoryCache {


    private Map<String, ManagedRepositoryItem> cache;
    public static int TIME_TO_IDLE = 60;
    public static int TIME_TO_EVICT = 120;

    public RepositoryCache() {
        this.cache = new HashMap<>();
    }

    public void init() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            cache.forEach((key, repo) -> {
                if(repo.getRepository().isInitialized() && repo.hasConnections()) {
                    repo.setActive();
                }
                else if(repo.isActive() && ! repo.hasConnections()) {
                    if(repo.getInitDate().plusSeconds(TIME_TO_IDLE).isBefore(Instant.now())) {
                        log.trace("Pausing repository: {}", key);
                        repo.setIdle();
                    }
                }
                else if(repo.isIdle() && repo.hasConnections()) {
                    log.trace("Reactivating idle repository with {} connections : {}", key, repo.getRepository().getConnectionsCount());
                    repo.setActive();
                }
                else if(repo.isIdle() && !repo.hasConnections()) {
                    if(repo.getIdleDate().plusSeconds(TIME_TO_EVICT).isBefore(Instant.now())) {
                        log.trace("Evicting repository: {}", key);
                        repo.setStale();
                    }
                } else if(repo.isStale() && repo.hasConnections()) {
                    log.trace("Reactivating stale repository with {} connections : {}", key, repo.getRepository().getConnectionsCount());
                    repo.setActive();
                } else if(repo.isStale() && ! repo.hasConnections()) {
                    if(repo.getRepository().isInitialized()) {
                        log.debug("Shutting down repository: {}", key);
                        repo.getRepository().shutDown();
                    }

                }

            });

            // we scan through all repositories, if there are not active connections we invalidate the entry
        }, 1000, 500, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        cache.values().forEach(managedRepository -> managedRepository.getRepository().shutDown());
    }

    public Collection<ManagedRepositoryItem> items() {
        return this.cache.values();
    }

    public Map<String, ManagedRepositoryItem> map() {
        return this.cache;
    }

    public void shutdown(String key) {
        if(this.cache.containsKey(key)) {
            this.cache.get(key).getRepository().shutDown();
        }
    }


    static class ManagedRepositoryItem {

        private final String label;
        private final LabeledRepository labeledRepository;
        private final Instant creationDate;
        private Instant idleDate;
        private ManagedRepositoryStatus status;

        ManagedRepositoryItem(String label, LabeledRepository labeledRepository) {
            this.label = label;
            this.labeledRepository = labeledRepository;
            this.creationDate = Instant.now();
            this.status = ManagedRepositoryStatus.ACTIVE;
        }

        public void setActive() {
            this.status = ManagedRepositoryStatus.ACTIVE;
            this.idleDate = null;
        }

        public void setStale() {
            this.status = ManagedRepositoryStatus.EVICT;
        }

        public void setIdle() {
            this.status = ManagedRepositoryStatus.IDLE;
            this.idleDate = Instant.now();
        }

        public boolean isIdle() {
            return ManagedRepositoryStatus.IDLE == this.status;
        }

        public boolean isStale() {
            return ManagedRepositoryStatus.EVICT == this.status;
        }

        public boolean isActive() {
            return ManagedRepositoryStatus.ACTIVE == this.status;
        }

        public boolean hasConnections() {
            return this.labeledRepository.getConnectionsCount() > 0;

        }

        public Instant getInitDate() {
            return this.creationDate;
        }

        public ManagedRepositoryItem activate() {
            this.setActive();
            return this;
        }


        enum ManagedRepositoryStatus {
            ACTIVE,
            IDLE,
            EVICT
        }



        public String label() {
            return label;
        }

        public LabeledRepository getRepository() {
            return labeledRepository;
        }

        public Instant creationDate() {
            return creationDate;
        }

        public Instant getIdleDate() {
            return idleDate;
        }

        public ManagedRepositoryStatus status() {
            return status;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (ManagedRepositoryItem) obj;
            return Objects.equals(this.label, that.label) &&
                    Objects.equals(this.labeledRepository, that.labeledRepository) &&
                    Objects.equals(this.creationDate, that.creationDate) &&
                    Objects.equals(this.idleDate, that.idleDate) &&
                    Objects.equals(this.status, that.status);
        }

        @Override
        public int hashCode() {
            return Objects.hash(label, labeledRepository, creationDate, idleDate, status);
        }

        @Override
        public String toString() {
            return "ManagedRepository[" +
                    "label=" + label + ", " +
                    "labeledRepository=" + labeledRepository + ", " +
                    "created=" + creationDate + ", " +
                    "idled=" + idleDate + ", " +
                    "status=" + status + ']';
        }
    }

}