package co.uk.laconic.mizuho.store.impl;

import co.uk.laconic.mizuho.domain.Price;
import co.uk.laconic.mizuho.store.PriceStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Naive append only implementation of an in memory cache.
 * Since this is a single table this would require full scans when getting by either vendor or instrument
 * but that is considered acceptable since this is just an implementation
 * when running the application locally or through unit tests
 *
 * a ConcurrentLinkedQueue can be be iterated while elements are being added or removed on different threads
 * since it returns a weakly consistent iterator.
 */
@Component
public class EmbeddedPriceStore implements PriceStore {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedPriceStore.class);
    private final Predicate<Price> evictionPredicate;
    protected final ConcurrentLinkedQueue<Price> store = new ConcurrentLinkedQueue<>();

    public EmbeddedPriceStore() {
        this(Duration.ofDays(30));
    }

    public EmbeddedPriceStore(Duration evictionThreshold) {
        this.evictionPredicate = price -> price.getTimestamp().isBefore(Instant.now().minus(evictionThreshold));
    }

    @Override
    public Stream<Price> getByVendor(String vendorId) {
        Assert.notNull(vendorId, "vendorId must not be null.");
        return query(price -> price.getVendorId().equalsIgnoreCase(vendorId.trim()));
    }

    @Override
    public Stream<Price> getByInstrument(String instrumentId) {
        Assert.notNull(instrumentId, "instrumentId must not be null.");
        return query(price -> price.getInstrumentId().equalsIgnoreCase(instrumentId.trim()));
    }

    @Override
    public void append(Price price) {
        Assert.notNull(price, "Expected non-null price.");
        logger.debug("Appending price: {}", price);
        store.add(price);
    }

    @Override
    public void evict() {
        logger.info("Evicting entries from cache...");
        // older entries than this will be removed from the cache
        store.removeIf(evictionPredicate);
        logger.info("Evicting entries from cache completed");
    }

    public void clear() {
        this.store.clear();
    }

    private Stream<Price> query(Predicate<Price> predicate) {
        return store.stream()
                .filter(predicate.and(evictionPredicate.negate()))
                .sorted(Comparator.comparing(Price::getTimestamp).reversed());
    }
}
