package co.uk.laconic.mizuho.integration.routes;

import co.uk.laconic.mizuho.domain.Price;
import co.uk.laconic.mizuho.events.PriceEvent;
import co.uk.laconic.mizuho.store.PriceStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@MockEndpoints
@DirtiesContext
public class InboundPricesRouteTest {

    @MockBean
    private PriceStore store;

    @Autowired
    @Produce(uri = "jms:queue:prices")
    private ProducerTemplate producer;

    @EndpointInject(uri = "mock:jms:queue:deadletters")
    protected MockEndpoint deadletters;

    @Autowired
    private ObjectMapper json;

    @Test
    public void whenValidPriceEventIsSent_ThenStoreIsNotified() throws JsonProcessingException, InterruptedException {
        // when we send a valid event
        PriceEvent event = new PriceEvent("test", "test", 0.0, 0.0, Instant.now());
        producer.sendBody("jms:queue:prices", json.writeValueAsString(event));

        // then we expect the store to receive it
        await().untilAsserted(() -> verify(store).append(Price.fromEvent(event)));
    }

    @Test
    public void whenAnInvalidMessageIsSent_ThenWeExpectItToGoToDeadLetters() throws InterruptedException {
        deadletters.expectedMessageCount(1);
        producer.sendBody("jms:queue:prices", "Not an Event");
        deadletters.assertIsSatisfied();
    }
}
