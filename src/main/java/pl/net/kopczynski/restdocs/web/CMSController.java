package pl.net.kopczynski.restdocs.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import pl.net.kopczynski.restdocs.domain.Document;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * Created by Tomasz Kopczynski
 */
@RestController
@RequestMapping("/cms")
public class CMSController {

    public static final String LAST_SEEN_DOCUMENT_ID_COOKIE_NAME = "lastSeenDocumentId";
    private ConcurrentMap<Long, Document> data = new ConcurrentHashMap<>();
    private AtomicLong counter = new AtomicLong(3);

    @PostConstruct
    public void init() {
        data.put(1L, new Document("Harry Smith", "Meeting report"));
        data.put(2L, new Document("Jack Williams", "Board meeting presentation"));
    }

    @RequestMapping(value = "/document", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> createDocument(@RequestBody Document document) {
        if (document != null) {
            long index = counter.getAndIncrement();
            data.put(index, document);

            MultiValueMap<String, String> headers = new HttpHeaders();
            headers.put("X-Created-Id", Collections.singletonList(String.valueOf(index)));

            return new ResponseEntity<>(Collections.singletonMap("id", index), headers, HttpStatus.CREATED);
        }

        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @RequestMapping(value = "/document", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<Document> retrieveAllDocuments() {
        return data.values();
    }

    @RequestMapping(value = "/document/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Document> retrieveDocument(@PathVariable("id") Long id) {
        Document document = data.get(id);

        if (document == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Document resultDocument = new Document(document.getAuthor(), document.getTitle());

        resultDocument.add(linkTo(methodOn(CMSController.class).retrieveDocument(id)).withSelfRel());
        resultDocument.add(linkTo(methodOn(CMSController.class).retrieveAllDocuments()).withRel("all"));

        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.put(HttpHeaders.CONTENT_TYPE, Collections.singletonList("application/hal+json"));

        return new ResponseEntity<>(resultDocument, headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/document/new", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Document> newDocuments(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] requestCookies = request.getCookies();

        Long documentId;

        if (requestCookies != null) {
            Optional<Long> lastSeenDocumentId = Stream.of(requestCookies)
                    .filter(cookie -> cookie.getName().equals(LAST_SEEN_DOCUMENT_ID_COOKIE_NAME))
                    .findFirst()
                    .map(cookie -> Long.valueOf(cookie.getValue()));

            documentId = lastSeenDocumentId.orElse(-1L);
        } else {
            documentId = -1L;
        }

        List<Map.Entry<Long, Document>> entries = data.entrySet().stream()
                .filter(entry -> entry.getKey() > documentId)
                .collect(Collectors.toList());

        Long maximumSeenDocumentId = entries.stream()
                .map(Map.Entry::getKey)
                .max(Long::compareTo)
                .orElse(documentId);

        List<Document> documents = entries.stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        response.addCookie(
                new Cookie(
                        LAST_SEEN_DOCUMENT_ID_COOKIE_NAME,
                        String.valueOf(maximumSeenDocumentId)));

        return documents;

    }

    @RequestMapping(value = "/healthcheck", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> healthcheck() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "OK");

        if (data == null) {
            result.put("db_connection", "down");
        } else {
            result.put("db_connection", "up");

            Map<String, String> dataSummary = new HashMap<>();
            dataSummary.put("entries", String.valueOf(data.size()));
            dataSummary.put("authors", String.valueOf(data.values().stream().map(Document::getAuthor).distinct().count()));

            result.put("data_summary", dataSummary);
        }

        return result;
    }
}
