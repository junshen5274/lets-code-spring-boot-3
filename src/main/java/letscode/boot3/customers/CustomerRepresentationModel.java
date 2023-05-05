package letscode.boot3.customers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.RepresentationModel;

class CustomerRepresentationModel extends RepresentationModel<CustomerRepresentationModel> {

    private final Integer id;
    private final String name;

    @JsonCreator
    CustomerRepresentationModel(@JsonProperty("id") Integer id, @JsonProperty("name") String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
