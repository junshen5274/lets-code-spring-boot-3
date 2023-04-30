package letscode.boot3.customers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

@Slf4j
@Configuration
public class CustomerInitializationConfiguration {

    @Bean
    ApplicationListener<ApplicationReadyEvent> applicationReadyEventApplicationListener(CustomerService cs) {
        return event -> {
            log.info("cs.class={}", cs.getClass());
            var maria = cs.add("Maria");
            var ernie = cs.add("Ernie");
            var all = cs.all();
            Assert.state(all.contains(maria) && all.contains(ernie), "valid results");
            all.forEach(c -> System.out.println(c.toString()));
        };
    }
}
