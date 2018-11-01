package co.uk.laconic.mizuho.domain;

import co.uk.laconic.mizuho.events.PriceEvent;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.time.Instant;
import java.util.Objects;

/**
 * Naive domain class representing a price that has been received.
 * This could be an interface or abstract class and then hold different type of prices if required
 *
 * But it has been considered to be out of scope of the exercise to provide one.
 *
 * Currency and Price modelling has also been done naively (assuming all prices in same currency
 * and using Double for money which isn't great)
 */
public class Price {

    public static Price fromEvent(PriceEvent event) {
        return new Price(event.vendorId, event.instrumentId, event.bid, event.ask, event.timestamp);
    }

    private final String vendorId;
    private final String instrumentId;
    private final Double bid;
    private final Double ask;
    private final Instant timestamp;

    public Price(
            @JsonProperty("vendorId") String vendorId,
            @JsonProperty("instrumentId") String instrumentId,
            @JsonProperty("bid") Double bid,
            @JsonProperty("ask") Double ask,
            @JsonProperty("timestamp") Instant timestamp) {
        this.vendorId = vendorId;
        this.instrumentId = instrumentId;
        this.bid = bid;
        this.ask = ask;
        this.timestamp = timestamp;
    }

    public String getVendorId() {
        return vendorId;
    }

    public String getInstrumentId() {
        return instrumentId;
    }

    public Double getBid() {
        return bid;
    }

    public Double getAsk() {
        return ask;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("vendorId", vendorId)
                .add("instrumentId", instrumentId)
                .add("bid", bid)
                .add("ask", ask)
                .add("timestamp", timestamp)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Price price = (Price) o;
        return Objects.equals(vendorId, price.vendorId) &&
                Objects.equals(instrumentId, price.instrumentId) &&
                Objects.equals(timestamp, price.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vendorId, instrumentId, timestamp);
    }
}
