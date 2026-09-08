package edu.eia.racing.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CompetitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminCanCreateACompetitor() throws Exception {
        String token = loginAndGetAccessToken("admin", "admin123");

        mockMvc.perform(post("/api/competitors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCompetitorJson("Byte", "byte-api")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Byte"))
                .andExpect(jsonPath("$.nickname").value("byte-api"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void viewerCannotCreateACompetitor() throws Exception {
        String token = loginAndGetAccessToken("viewer", "viewer123");

        mockMvc.perform(post("/api/competitors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCompetitorJson("Forbidden", "forbidden-api")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void invalidWeightReturnsStructuredValidationError() throws Exception {
        String token = loginAndGetAccessToken("admin", "admin123");
        String body = validCompetitorJson("Invalid", "invalid-weight")
                .replace("\"weight\":500.0", "\"weight\":0");

        mockMvc.perform(post("/api/competitors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("weight")));
    }

    @Test
    void duplicateNicknameReturnsConflict() throws Exception {
        String token = loginAndGetAccessToken("admin", "admin123");
        String body = validCompetitorJson("First", "duplicate-api");

        mockMvc.perform(post("/api/competitors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/competitors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCompetitorJson("Second", "DUPLICATE-API")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void authenticatedUserCanFilterCompetitorsByTypeAndPage() throws Exception {
        String token = loginAndGetAccessToken("viewer", "viewer123");

        mockMvc.perform(get("/api/competitors")
                        .param("type", "CAMEL")
                        .param("page", "0")
                        .param("size", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].type").value("CAMEL"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }


    @Test
    void invalidTypeFilterReturnsBadRequest() throws Exception {
        String token = loginAndGetAccessToken("viewer", "viewer123");

        mockMvc.perform(get("/api/competitors")
                        .param("type", "UNKNOWN_TYPE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getByIdReturnsCreatedCompetitorWithoutTeam() throws Exception {
        String token = loginAndGetAccessToken("admin", "admin123");
        long id = createAndGetId(token, "Detail", "detail-api");

        mockMvc.perform(get("/api/competitors/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nickname").value("detail-api"))
                .andExpect(jsonPath("$.teamId").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void getByIdReturnsStructuredNotFound() throws Exception {
        String token = loginAndGetAccessToken("viewer", "viewer123");

        mockMvc.perform(get("/api/competitors/" + Long.MAX_VALUE)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/competitors/" + Long.MAX_VALUE));
    }

    @Test
    void adminCanUpdateCompetitor() throws Exception {
        String token = loginAndGetAccessToken("admin", "admin123");
        long id = createAndGetId(token, "Before", "before-api");

        mockMvc.perform(put("/api/competitors/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCompetitorJson("After", "AFTER-API")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("After"))
                .andExpect(jsonPath("$.nickname").value("after-api"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void adminCanChangeCompetitorStatus() throws Exception {
        String token = loginAndGetAccessToken("admin", "admin123");
        long id = createAndGetId(token, "Status", "status-api");

        mockMvc.perform(patch("/api/competitors/" + id + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nickname").value("status-api"))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void adminCanDeleteUnreferencedCompetitor() throws Exception {
        String token = loginAndGetAccessToken("admin", "admin123");
        long id = createAndGetId(token, "Delete", "delete-api");

        mockMvc.perform(delete("/api/competitors/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/competitors/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void malformedRequestBodyReturnsStructuredBadRequest() throws Exception {
        String token = loginAndGetAccessToken("admin", "admin123");
        long id = createAndGetId(token, "Malformed", "malformed-api");

        mockMvc.perform(put("/api/competitors/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/competitors/" + id));
    }

    @Test
    void seededTeamMembershipAppearsAsTeamId() throws Exception {
        String token = loginAndGetAccessToken("viewer", "viewer123");

        mockMvc.perform(get("/api/competitors")
                        .param("nickname", "segfault")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].teamId").isNumber());
    }

    private long createAndGetId(String token, String name, String nickname) throws Exception {
        String response = mockMvc.perform(post("/api/competitors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCompetitorJson(name, nickname)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }
    private String loginAndGetAccessToken(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private String validCompetitorJson(String name, String nickname) {
        return "{\"name\":\"" + name + "\",\"nickname\":\"" + nickname
                + "\",\"type\":\"CAMEL\",\"weight\":500.0,\"height\":200.0,"
                + "\"originCountry\":\"Colombia\"}";
    }
}
