package co.uk.laconic.mizuho;

import co.uk.laconic.mizuho.integration.routes.InboundPricesRoute;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.spring.SpringCamelContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import javax.jms.ConnectionFactory;

@SpringBootApplication
public class Application {

    /**
     * Indicates to which ActiveMQ broker we want to connect
     * By default it will attempt to connect to an embedded broker (useful for development and testing)
     */
    @Value("${activemq.broker.url:vm://localhost}")
    private String brokerURL;

    /**
     * Configure a camel context with JMS/ActiveMQ support
     * @param springContext
     * @param prices
     * @return
     * @throws Exception
     */
    @Bean
    public CamelContext camelContext(ApplicationContext springContext, InboundPricesRoute prices) throws Exception {
        SpringCamelContext context = new SpringCamelContext(springContext);
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerURL);
        context.addComponent("jms", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));
        context.addRoutes(prices);
        return context;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
