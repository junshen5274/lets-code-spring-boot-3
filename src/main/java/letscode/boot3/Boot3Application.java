package letscode.boot3;

import letscode.boot3.customers.Customer;
import letscode.boot3.customers.CustomerCreatedEvent;
import letscode.boot3.customers.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication
public class Boot3Application {

    public static void main(String[] args) {
        SpringApplication.run(Boot3Application.class, args);
    }

    @Bean
    ApplicationListener<ApplicationReadyEvent> readyEventApplicationListener(CustomerRepository repository) {
        return new ApplicationListener<ApplicationReadyEvent>() {
            @Override
            public void onApplicationEvent(ApplicationReadyEvent event) {
                repository.save(new Customer(null, "DaShaun", true));
            }
        };
    }

    @Bean
    ApplicationListener<CustomerCreatedEvent> customerCreatedEventApplicationListener() {
        return new ApplicationListener<CustomerCreatedEvent>() {
            @Override
            public void onApplicationEvent(CustomerCreatedEvent event) {
                log.info("new customer created: {}", event.getSource().toString());
            }
        };
    }

    @Bean
    ApplicationListener<WebServerInitializedEvent> webServerInitializedEventApplicationListener() {
        return new ApplicationListener<WebServerInitializedEvent>() {
            @Override
            public void onApplicationEvent(WebServerInitializedEvent event) {
                log.info("The web server is ready to serve HTTP traffic on port {}.",
                        event.getWebServer().getPort());
            }
        };
    }

    @Bean
    ApplicationListener<AvailabilityChangeEvent<?>> availabilityChangeEventApplicationListener() {
        return new ApplicationListener<AvailabilityChangeEvent<?>>() {
            @Override
            public void onApplicationEvent(AvailabilityChangeEvent<?> event) {
                log.info("The service is healthy? {}",
                        event.getState().toString());
            }
        };
    }
}

