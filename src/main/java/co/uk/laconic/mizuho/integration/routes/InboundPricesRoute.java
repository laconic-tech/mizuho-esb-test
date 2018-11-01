package co.uk.laconic.mizuho.integration.routes;

import co.uk.laconic.mizuho.domain.Price;
import co.uk.laconic.mizuho.events.PriceEvent;
import co.uk.laconic.mizuho.store.PriceStore;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Defines the main routes and processing for this service, these are:
 *
 * pricesMailbox ==> process(priceEvent) ==> store.append(price)
 *               ==> queue:deadletters (in case of errors)
 *
 * evictiontimer ==> store::evict
 */
@Component
public class InboundPricesRoute extends RouteBuilder {

    private static final Logger logger = LoggerFactory.getLogger(InboundPricesRoute.class);
    private final PriceStore store;

    @Value("${priceserver.deadletter.uri:jms:queue:deadletters}")
    private String deadlettersUri;
    @Value("${priceserver.mailbox.uri:jms:queue:prices}")
    private String mailboxUri;
    @Value("${priceserver.eviction.uri:timer://evict?fixedRate=true&period=60000}")
    private String evictionUri;

    @Autowired
    public InboundPricesRoute(PriceStore store) {
        this.store = store;
    }

    @Override
    public void configure() {
        logger.info("Configuring routes on `{}`...", this.getClass().getName());

        // redirect all errors to deadletters
        errorHandler(deadLetterChannel(deadlettersUri));

        // main queue where we expect prices to show up
        from(mailboxUri)
                .routeId("prices:mailbox")
                .unmarshal()
                    .json(JsonLibrary.Jackson, PriceEvent.class)
                .process()
                    .message(msg -> logger.info("Received message: `{}`", msg))
                .process()
                    .body(PriceEvent.class, this::processor);

        // attempt to evict every minute
        from(evictionUri)
                .routeId("prices:evicttimer")
                .log("Eviction Tick")
                .process().message(ignored -> store.evict());
    }

    /**
     * Convenience method to keep the route definition more concise/compact
     */
    private void processor(PriceEvent e) {
        store.append(Price.fromEvent(e));
    }
}
