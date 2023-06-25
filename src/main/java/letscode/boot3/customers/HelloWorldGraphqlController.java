package letscode.boot3.customers;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

@Controller
public class HelloWorldGraphqlController {
    // A controller has nothing to do with the web, it is just a way to adapt the protocol to the business logic
    // The protocol can be http or socket...

    @SchemaMapping(typeName = "Query", field = "hello")
    String hello(@Argument String name) {
        return "Hello, " + name + "!";
    }
}
