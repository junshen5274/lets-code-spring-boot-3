package letscode.boot3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.Collection;

public class Boot3Application {

	public static void main(String[] args) {
		var dataSource = new DriverManagerDataSource(
				"jdbc:postgresql://localhost/postgres",
				"postgres", "postgres"
		);
		dataSource.setDriverClassName(Driver.class.getName());

		var template = new JdbcTemplate(dataSource);
		template.afterPropertiesSet();

		var cs = new DefaultCustomerService(template);
		var all = cs.all();
		all.forEach(c -> System.out.println(c.toString()));
	}

	@Slf4j
	static
	class DefaultCustomerService {
		private final JdbcTemplate template;

		DefaultCustomerService(JdbcTemplate template) {
			this.template = template;
		}

		Collection<Customer> all() {
			var listOfCustomers = new ArrayList<Customer>();
			try {
				try (var connection = this.dataSource.getConnection()) {
					try (var stmt = connection.createStatement()) {
						try (var resultSet = stmt.executeQuery("select * from customers")) {
							while (resultSet.next()) {
								var name = resultSet.getString("name");
								var id = resultSet.getInt("id");
								listOfCustomers.add(new Customer(id, name));
							}
						}
					}
				}
			} catch (Exception e) {
				log.error("something went wrong", e);
			}
			return listOfCustomers;
		}
	}

	record Customer(Integer id, String name){}

}
