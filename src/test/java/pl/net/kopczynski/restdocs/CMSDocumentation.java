package pl.net.kopczynski.restdocs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import pl.net.kopczynski.restdocs.domain.Document;

import javax.servlet.http.Cookie;
import java.util.Base64;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@SpringBootTest
public class CMSDocumentation {

	@Rule
	public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

	private MockMvc mockMvc;

	private ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private WebApplicationContext context;

	private LinksSnippet pagingSnippets = links(
			halLinks(),
			linkWithRel("next")
					.optional()
					.description("The next page"),
			linkWithRel("prev")
					.optional()
					.description("The previous page"));

	@Before
	public void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
			.apply(documentationConfiguration(this.restDocumentation))
			.build();
	}

	@Test
	public void healthcheckTest() throws Exception {
		this.mockMvc.perform(
			get("/cms/healthcheck")
				.accept(MediaType.APPLICATION_JSON)
		)
		.andExpect(status().isOk())
		.andDo(
			document("healthcheck",
					preprocessRequest(
							removeHeaders("Host")
					),
					preprocessResponse(
							prettyPrint()
					),
					responseBody(beneathPath("data_summary")))
		);
	}

	@Test
	public void retrieveDocumentTest() throws Exception {
		this.mockMvc.perform(get("/cms/document/{id}", 1L))
			.andExpect(status().isOk())
			.andDo(document("retrieveDocument",
					this.pagingSnippets.and(
							linkWithRel("all")
									.description("All available documents"),
							linkWithRel("self")
									.ignored()
									.optional()),
				pathParameters(
					parameterWithName("id").description("Document's id")
				),
				responseFields(
						subsectionWithPath("_links")
								.description("Links self"),
						fieldWithPath("author")
								.description("Document's author"),
						fieldWithPath("title")
								.description("Document's title")
				)
			));
	}

    @Test
    public void relaxedRetrieveDocumentTest() throws Exception {
        this.mockMvc.perform(get("/cms/document/{id}", 1L))
                .andExpect(status().isOk())
                .andDo(document("relaxedRetrieveDocument",
                        links(halLinks(),
                                linkWithRel("all").description("All available documents"),
                                linkWithRel("self").ignored().optional()),
                        pathParameters(
                                parameterWithName("id").description("Document's id")
                        ),
                        relaxedResponseFields(
                                subsectionWithPath("_links")
										.description("Links self")
                        )
                ));
    }

	@Test
	public void createDocumentTest() throws Exception {
		Document document = new Document("Jack Tester", "Testing REST APIs");

		this.mockMvc.perform(
			post("/cms/document")
					.header(HttpHeaders.AUTHORIZATION, new String(Base64.getEncoder().encode("user:password".getBytes())))
					.content(objectMapper.writeValueAsString(document))
					.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isCreated())
			.andDo(document("createDocument",
					responseFields(
							fieldWithPath("id").description("Created document's id")),
					requestHeaders(
							headerWithName(HttpHeaders.AUTHORIZATION).description("Basic authorization")),
					responseHeaders(
							headerWithName("X-Created-Id").description("Id of a created document"))
			));
	}

	@Test
	public void cookiesTest() throws Exception {
		this.mockMvc.perform(get("/cms/document/new"))
				.andExpect(status().isOk())
				.andExpect(cookie().exists("lastSeenDocumentId"))
				.andDo(document("cookies-1"));

		this.mockMvc.perform(get("/cms/document/new")
				.cookie(new Cookie("lastSeenDocumentId", "2")))
				.andExpect(status().isOk())
				.andExpect(cookie().exists("lastSeenDocumentId"))
				.andExpect(jsonPath("$.length()", is(0)))
				.andDo(document("cookies-2"));
	}

}
