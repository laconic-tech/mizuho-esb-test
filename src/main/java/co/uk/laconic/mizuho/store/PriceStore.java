package co.uk.laconic.mizuho.store;

import co.uk.laconic.mizuho.domain.Price;

import java.util.stream.Stream;

public interface PriceStore {
    /**
     * Get all prices for a particular vendor
     * @param vendorId
     * @return
     */
    Stream<Price> getByVendor(String vendorId);

    /**
     * Get all prices for a particular instrument (accross vendors)
     * @param instrumentId
     * @return
     */
    Stream<Price> getByInstrument(String instrumentId);

    /**
     * Append a price to the cache
     * @param price
     */
    void append(Price price);

    /**
     * Applies the eviction algorithm to remove stale/old entries.
     */
    void evict();
}
