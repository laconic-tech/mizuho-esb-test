package co.uk.laconic.mizuho.store.impl;

import co.uk.laconic.mizuho.domain.Price;
import co.uk.laconic.mizuho.store.PriceStore;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * This Test provides a contract and specification that any implementation of PriceStore has to comply with
 * for a correct behaviour of the system.
 */
public abstract class PriceStoreBaseTest<S extends PriceStore> {

    Duration evictionThreshold = Duration.ofDays(1);
    S target;

    @Before
    public void setUp() {
        target = createStore(evictionThreshold);
    }

    @Test
    public void whenGetByNullVendor_ThenErrorIsRaised() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> target.getByInstrument(null));
    }

    @Test
    public void whenGetByNullInstrument_ThenErrorIsRaised() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> target.getByVendor(null));
    }

    @Test
    public void whenAppendingNullPrice_ThenErrorIsRaised() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> target.append(null));
    }

    @Test
    public void whenNoPricesForVendor_ThenNothingIsReturned() {
        assertThat(target.getByVendor("Bloomberg")).isEmpty();
    }

    @Test
    public void whenNoPricesForInstrument_ThenNothingIsReturned() {
        assertThat(target.getByInstrument("AAPL")).isEmpty();
    }

    @Test
    public void whenPriceIsAdded_ThenItCanBeRetrievedByVendorOrInstrument() {
        Price price = priceFor("Bloomberg", "AAPL");
        target.append(price);

        assertThat(target.getByInstrument("unknown")).isEmpty();
        assertThat(target.getByVendor("unknown")).isEmpty();

        assertThat(target.getByInstrument("AAPL")).containsExactly(price);
        assertThat(target.getByInstrument("aapl")).containsExactly(price);
        assertThat(target.getByVendor("Bloomberg")).containsExactly(price);
        assertThat(target.getByVendor("BLOOMBERG")).containsExactly(price);
    }

    @Test
    public void whenTwoPricesForInstrment_ThenTheyAreReturnedSortedByTimestamp() {
        Price first = priceFor("Bloomberg", "AAPL", Instant.now().minus(Duration.ofSeconds(1000)));
        Price second = priceFor("Bloomberg", "AAPL", Instant.now().minus(Duration.ofSeconds(100)));
        Price third = priceFor("Bloomberg", "AAPL", Instant.now().minus(Duration.ofSeconds(10)));

        // add them in a different order
        target.append(first);
        target.append(third);
        target.append(second);

        assertThat(target.getByInstrument("AAPL"))
                .isNotEmpty()
                .containsExactly(third, second, first);

        assertThat(target.getByVendor("Bloomberg"))
                .isNotEmpty()
                .containsExactly(third, second, first);
    }

    @Test
    public void whenPriceWithAgeAboveThresholdIsRequested_ThenItIsInvalidatedInTheCacheAndNeverReturned() {
        Price young = priceFor("Bloomberg", "AAPL");
        // create a price that is older than allowed by the eviction threshold, and add some leeway just in case.
        Price old = priceFor("Bloomberg", "AAPL", Instant.now().minus(evictionThreshold.plusSeconds(1)));
        target.append(young);
        target.append(old);

        assertThat(target.getByInstrument("AAPL")).hasSize(1).containsExactly(young);
        assertThat(target.getByVendor("Bloomberg")).hasSize(1).containsExactly(young);
    }

    /**
     * Given prices are expected to be invalidated when querying (but not removed)
     * this test requires access to details of the particular implementation
     * So it is left for the specific test class to implement / or ignore
     */
    @Test
    public abstract void whenEvictionRequested_ThenPricesOlderThanThresholdAreRemoved();

    //
    // helper methods
    //

    protected abstract S createStore(Duration evictionThreshold);

    protected Price priceFor(String vendor, String instrument) {
        return priceFor(vendor, instrument, Optional.empty());
    }

    protected Price priceFor(String vendor, String instrument, Instant instant) {
        return priceFor(vendor, instrument, Optional.of(instant));
    }

    protected Price priceFor(String vendor, String instrument, Optional<Instant> timestamp) {
        return new Price(
                vendor,
                instrument,
                10.5, 11.0,
                timestamp.orElse(Instant.now()));
    }
}
