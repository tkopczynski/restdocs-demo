package pl.net.kopczynski.restdocs.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.hateoas.ResourceSupport;

/**
 * Created by Tomasz Kopczynski.
 */
@AllArgsConstructor
@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Document extends ResourceSupport {

    private final String author;
    private final String title;

}
