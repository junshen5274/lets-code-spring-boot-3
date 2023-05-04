package letscode.boot3.customers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Controller
/**
 * Since we have this controller ready, each time when a new customer is created, the customerCache (Set)
 * will be updated to include that one and we can simply access the link:
 * http://localhost:8080/customers
 * to get all customer data.
 */
class CustomerHttpController implements ApplicationListener<CustomerCreatedEvent> {

    private final Set<Customer> customerCache = new ConcurrentSkipListSet<>(Comparator.comparing(new Function<Customer, Integer>() {
        @Override
        public Integer apply(Customer customer) {
            return customer.id();
        }
    }));

    static class MyView implements View {

        @Override
        public String getContentType() {
            return MediaType.TEXT_HTML_VALUE;
        }

        @Override
        public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
            var customers = model.get("customers");
            if (customers instanceof Set<?> list) {
                var listOfCustomers = new ArrayList<Customer>();
                for (var c : list) {
                    if (c instanceof Customer customer) {
                        listOfCustomers.add(customer);
                    }
                }
                var string = listOfCustomers.stream().map(c -> String.format("""
                                <tr>
                                  <td>%s</td>
                                  <td>%s</td>
                                </tr>
                                """, c.id(), c.name()))
                        .collect(Collectors.joining());
                response.getWriter().println(String.format("""
                        <h1>Customers with customer view </h1>
                        <table>
                          %s                  
                        </table>
                        """, string));
                response.getWriter().flush();
            }
        }
    }

    @GetMapping("/customers/{id}")
    // localhost:8080/customers/1
    String renderCustomerByIdOnPath(@PathVariable Integer id, Model model) {
        model.addAttribute("customer", this.customerCache.iterator().next());
        return "customer";
    }

    @GetMapping("/customers.do")
    // localhost:8080/customers.do?id=1
    String renderCustomerByIdAsReqParm(@RequestParam Integer id, Model model) {
        log.info("The customer ID is {}", id);
        model.addAttribute("customer", this.customerCache.stream()
                .filter(f -> f.id().equals(id)).distinct().collect(Collectors.toSet()).iterator().next());
        return "customer";
    }

    @GetMapping("/customers.custom")
    ModelAndView renderCustomersWithCustomView() {
        return new ModelAndView(new MyView(), Map.of("customers", this.customerCache));
    }

    @ResponseBody
    @GetMapping("/customers")
    Collection<Customer> customers() {
        return customerCache;
    }

    @GetMapping("/customers.thymeleaf")
    ModelAndView renderCustomersWithThymeleaf() {
        return new ModelAndView("customers-th", Map.of("customers", this.customerCache));
    }

    @GetMapping("/customers.mustache")
    ModelAndView renderCustomersWithMustache() {
        return new ModelAndView("customers-mu", Map.of("customers", this.customerCache));
    }

    @Override
    public void onApplicationEvent(CustomerCreatedEvent event) {
        this.customerCache.add(event.getSource());
    }
}
