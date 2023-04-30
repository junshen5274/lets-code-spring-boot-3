package letscode.boot3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.availability.AvailabilityChangeEvent;
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

