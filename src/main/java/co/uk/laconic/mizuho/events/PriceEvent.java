package co.uk.laconic.mizuho.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Represents an external event that is relevant to this system
 * This class is barebones now but it could include metadata related to the Message itself
 *
 * For the scope of this example is just to separate the Events from the actual domain even if now they look the same.
 */
public class PriceEvent {

    public final String vendorId;
    public final String instrumentId;
    public final Double bid;
    public final Double ask;
    public final Instant timestamp;

    public PriceEvent(@JsonProperty("vendorId") String vendorId,
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
}
