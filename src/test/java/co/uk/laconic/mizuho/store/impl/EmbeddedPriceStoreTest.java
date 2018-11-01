package co.uk.laconic.mizuho.store.impl;

import co.uk.laconic.mizuho.domain.Price;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;


public class EmbeddedPriceStoreTest extends PriceStoreBaseTest<EmbeddedPriceStore> {

    @Override
    protected EmbeddedPriceStore createStore(Duration evictionThreshold) {
        return new EmbeddedPriceStore(evictionThreshold);
    }

    @Override
    public void whenEvictionRequested_ThenPricesOlderThanThresholdAreRemoved() {
        Price young = priceFor("Bloomberg", "AAPL");
        // create a price that is older than allowed by the eviction threshold, and add some leeway just in case.
        Price old = priceFor("Bloomberg", "AAPL", Instant.now().minus(evictionThreshold.plusSeconds(1)));
        // append the two entries
        target.append(young);
        target.append(old);

        // once we evict then the size is `1` and only `young` is in the list
        target.evict();

        assertThat(target.store).hasSize(1).containsExactly(young);
    }
}
