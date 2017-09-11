package sample.chirper.chirp.impl;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.StreamConverters;
import com.google.common.collect.ImmutableMap;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import com.lightbend.lagom.javadsl.persistence.ReadSide;
import com.lightbend.lagom.javadsl.persistence.ReadSideProcessor;
import com.lightbend.lagom.javadsl.persistence.jpa.JpaReadSide;
import com.lightbend.lagom.javadsl.persistence.jpa.JpaSession;
import org.pcollections.PSequence;
import org.pcollections.TreePVector;
import sample.chirper.chirp.api.Chirp;
import sample.chirper.chirp.impl.ChirpTimelineEvent.ChirpAdded;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ChirpRepositoryImpl implements ChirpRepository {
    private static final int NUM_RECENT_CHIRPS = 10;
    private static final String SELECT_HISTORICAL_CHIRPS =
            "SELECT NEW sample.chirper.chirp.api.Chirp(chirp.userId, chirp.message, chirp.timestamp, chirp.uuid) " +
                    "FROM ChirpJpaEntity chirp " +
                    "WHERE userId IN :userIds " +
                    "AND timestamp >= :timestamp " +
                    "ORDER BY timestamp ASC";
    private static final String SELECT_RECENT_CHIRPS =
            "SELECT NEW sample.chirper.chirp.api.Chirp(chirp.userId, chirp.message, chirp.timestamp, chirp.uuid) " +
                    "FROM ChirpJpaEntity chirp " +
                    "WHERE userId IN :userIds " +
                    "ORDER BY timestamp DESC";

    private final JpaSession jpa;

    @Inject
    ChirpRepositoryImpl(JpaSession jpa, ReadSide readSide) {
        this.jpa = jpa;
        readSide.register(ChirpTimelineEventReadSideProcessor.class);
    }

    public Source<Chirp, ?> getHistoricalChirps(PSequence<String> userIds, long timestamp) {
        CompletionStage<Stream<Chirp>> chirps = jpa.withTransaction(entityManager ->
                entityManager.createQuery(SELECT_HISTORICAL_CHIRPS, Chirp.class)
                        .setParameter("userIds", userIds)
                        .setParameter("timestamp", new Timestamp(timestamp))
                        .getResultList()
                        .stream()
        );
        return Source.fromCompletionStage(chirps)
                .flatMapConcat(ChirpRepositoryImpl::streamToSource);
    }

    public CompletionStage<PSequence<Chirp>> getRecentChirps(PSequence<String> userIds) {
        return jpa.withTransaction(entityManager -> {
            List<Chirp> recentChirps = entityManager.createQuery(SELECT_RECENT_CHIRPS, Chirp.class)
                    .setParameter("userIds", userIds)
                    .setMaxResults(NUM_RECENT_CHIRPS)
                    .getResultList()
                    .stream()
                    .collect(Collectors.toList());
            Collections.reverse(recentChirps);
            return TreePVector.from(recentChirps);
        });
    }

    private static <T> Source<T, NotUsed> streamToSource(Stream<T> stream) {
        return StreamConverters.fromJavaStream(() -> stream);
    }

    private static class ChirpTimelineEventReadSideProcessor extends ReadSideProcessor<ChirpTimelineEvent> {
        private final JpaReadSide readSide;

        @Inject
        private ChirpTimelineEventReadSideProcessor(JpaReadSide readSide) {
            this.readSide = readSide;
        }

        @Override
        public ReadSideHandler<ChirpTimelineEvent> buildHandler() {
            return readSide.<ChirpTimelineEvent>builder("ChirpTimelineEventReadSideProcessor")
                    .setGlobalPrepare(unused -> createSchema())
                    .setEventHandler(ChirpAdded.class, this::insertChirp)
                    .build();
        }

        @Override
        public PSequence<AggregateEventTag<ChirpTimelineEvent>> aggregateTags() {
            return ChirpTimelineEvent.TAG.allTags();
        }

        private void createSchema() {
            Persistence.generateSchema("default", ImmutableMap.of("hibernate.hbm2ddl.auto", "update"));
        }

        private void insertChirp(EntityManager entityManager, ChirpAdded event) {
            entityManager.persist(ChirpJpaEntity.from(event.chirp));
        }
    }
}
