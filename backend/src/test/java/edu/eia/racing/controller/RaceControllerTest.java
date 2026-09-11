package edu.eia.racing.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import edu.eia.racing.repository.UserRepository;
import java.time.LocalDateTime;
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
class RaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private String loginAndGetAccessToken(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private String validRaceJson() {
        LocalDateTime scheduledAt = LocalDateTime.now().plusDays(30);
        LocalDateTime deadline = scheduledAt.minusDays(2);
        Long organizerId = userRepository.findByUsername("organizer").orElseThrow().getId();
        return """
                {
                  "name":"Authorization race",
                  "description":"Controller authorization coverage",
                  "scheduledAt":"%s",
                  "startLocation":"Start",
                  "endLocation":"Finish",
                  "distanceMeters":1000,
                  "maxParticipants":10,
                  "type":"INDIVIDUAL",
                  "organizerId":%d,
                  "registrationDeadline":"%s"
                }
                """.formatted(scheduledAt, organizerId, deadline);
    }

    @Test
    void viewerCannotCreateRace() throws Exception {
        String token = loginAndGetAccessToken("viewer", "viewer123");

        mockMvc.perform(post("/api/races")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRaceJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminCanCreateRace() throws Exception {
        String token = loginAndGetAccessToken("admin", "admin123");

        mockMvc.perform(post("/api/races")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRaceJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Authorization race"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }
}
