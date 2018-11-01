package co.uk.laconic.mizuho;


import co.uk.laconic.mizuho.domain.Price;
import co.uk.laconic.mizuho.events.PriceEvent;
import co.uk.laconic.mizuho.store.impl.EmbeddedPriceStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;

import static org.awaitility.Awaitility.await;

/**
 * This suite validates the requirements at a user level, and it is intended to be black box.
 * Only actions that can be validated through the exposed interfaces are checked (just as a User would see them)
 *
 * This spec starts the application (and spring takes a couple of seconds to do so, so it is good to avoid adding many
 * specific tests at this level. It is still the absolute best way to ensure the application is working end to end.
 *
 * More complex actions like eviction are tested at a more granular level and not here
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class PriceServerTest {

    @Autowired
    private WebTestClient client;

    @Autowired
    @EndpointInject(uri="jms:queue:prices")
    private ProducerTemplate pricesProducer;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private EmbeddedPriceStore store;

    @Before
    public void setUp() {
        store.clear();
    }

    @Test
    public void whenNoPricesForInstrument_ThenAnEmptyResponseIsReturned() {
        getByVendorId("unknown").isEqualTo(new Price[] {});
    }

    @Test
    public void whenNoPricesForVendor_ThenAnEmptyResponseIsReturned() {
        // given there are no prices for the vendor `Reuters`
        // then we expect to receive an empty price list
        getByInstrumentId("unknown").isEqualTo(new Price[] {});
    }

    @Test
    public void whenPriceEventIsReceived_ThenItCanBeReadBackByInstrumentId() throws JsonProcessingException {
        // given we receive a price for `AAPL` from `Bloomberg`
        // and we receive a price for `MSFT` from `Bloomberg`
        // and we receive a price for `AAPL` from `Reuters`
        Price bloomberg_AAPL = sendMessage("Bloomberg", "AAPL");
        Price bloomberg_MSFT = sendMessage("Bloomberg", "MSFT");
        Price reuters_AAPL = sendMessage("Reuters", "AAPL");

        // then when we ask for `AAPL` prices
        // we expect to receive prices from both `Bloomberg` and `Reuters`
        await().untilAsserted(() -> getByInstrumentId("AAPL").isEqualTo(new Price[]{reuters_AAPL, bloomberg_AAPL}));

        // when we ask for `MSFT` prices
        // then we expect to receive prices from `Bloomberg` only.
        await().untilAsserted(() -> getByInstrumentId("MSFT").isEqualTo(new Price[] { bloomberg_MSFT }));
    }

    @Test
    public void whenPriceEventIsReceived_ThenItCanBeReadBackByVendorId() throws JsonProcessingException {
        // given we receive a price for `AAPL` from `Bloomberg`
        // and we receive a price for `MSFT` from `Bloomberg`
        // and we receive a price for `AAPL` from `Reuters`
        Price bloomberg_AAPL = sendMessage("Bloomberg", "AAPL");
        Price bloomberg_MSFT = sendMessage("Bloomberg", "MSFT");
        Price reuters_AAPL = sendMessage("Reuters", "AAPL");

        // then when we ask for prices submitted by `Bloomberg`
        // we expect to receive prices for `AAPL` and `MSFT`
        await().untilAsserted(() -> getByVendorId("Bloomberg").isEqualTo(new Price[]{ bloomberg_MSFT, bloomberg_AAPL }));

        // when we ask for prices submitted by `Reuters`
        // then we expect to receive prices for `AAPL` only.
        await().untilAsserted(() -> getByVendorId("Reuters").isEqualTo(new Price[] { reuters_AAPL }));
    }

    //
    // helper methods
    //

    public WebTestClient.BodySpec<Price[], ?> getByInstrumentId(String instrumentId) {
        return client.get()
                .uri(builder -> builder.path("prices/instruments/").pathSegment(instrumentId).build())
                .exchange()
                .expectBody(Price[].class);
    }

    public WebTestClient.BodySpec<Price[], ?> getByVendorId(String vendorId) {
        return client.get()
                .uri(builder -> builder.path("prices/vendors/").pathSegment(vendorId).build())
                .exchange()
                .expectBody(Price[].class);
    }

    public Price sendMessage(String vendorId, String instrumentId) throws JsonProcessingException {
        PriceEvent price = new PriceEvent(vendorId, instrumentId, 1.0, 1.0, Instant.now());
        pricesProducer.sendBody(json.writeValueAsString(price));
        return Price.fromEvent(price);
    }
}
