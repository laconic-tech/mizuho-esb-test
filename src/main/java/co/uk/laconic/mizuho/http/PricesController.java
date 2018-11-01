package co.uk.laconic.mizuho.http;

import co.uk.laconic.mizuho.domain.Price;
import co.uk.laconic.mizuho.store.PriceStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Main entry point to read prices out of the cache through the exposed rest interface.
 */
@RestController
@RequestMapping("/prices/")
public class PricesController {

    private static final Logger logger = LoggerFactory.getLogger(PricesController.class);
    private final PriceStore prices;

    @Autowired
    public PricesController(PriceStore prices) {
        this.prices = prices;
    }

    @RequestMapping(value="vendors/{vendorId}", method = RequestMethod.GET)
    public Flux<Price> getByVendorId(@PathVariable String vendorId) {
        logger.info("Requesting prices for vendor: `{}`", vendorId);
        return Flux.fromStream(prices.getByVendor(vendorId));
    }

    @RequestMapping(value="instruments/{instrumentId}", method = RequestMethod.GET)
    public Flux<Price> getByInstrument(@PathVariable String instrumentId) {
        logger.info("Requesting prices for instrument: `{}`", instrumentId);
        return Flux.fromStream(prices.getByInstrument(instrumentId));
    }
}
