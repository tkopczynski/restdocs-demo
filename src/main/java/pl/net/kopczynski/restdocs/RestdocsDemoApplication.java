package pl.net.kopczynski.restdocs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.hateoas.config.EnableHypermediaSupport;

@SpringBootApplication
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
public class RestdocsDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(RestdocsDemoApplication.class, args);
	}
}
