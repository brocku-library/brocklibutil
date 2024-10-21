package brocklibutil.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;

import brocklibutil.helper.Helper;

@RestController
@RequestMapping("/api/libinsight/loginstat")
public class LoginLibinsightController {

    static final String DATASET_ID = "12213";
    static final String LIBINSIGHT_STORE_URL = "https://brocku.libinsight.com/post/v1.0/custom/" + DATASET_ID
            + "/type/1/save";

    static final String PAYLOAD_FORMAT = """
                [{
                    "field_92": "%s",
                    "field_93": "%s",
                    "ts_start": "%s"
                }]
            """;

    @Autowired
    private LibinsightController libinsightController;

    @GetMapping("/feed")
    public ResponseEntity<String> feedIntoLibinsight(@RequestParam String computer, @RequestParam String location)
            throws JsonProcessingException {

        LocalDateTime dateTime = LocalDateTime.now();
        dateTime.atZone(ZoneId.of("America/Toronto"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        String payload = String.format(PAYLOAD_FORMAT, computer, location, dateTime.format(formatter));

        if (payload == null || payload.contains("[]")) {
            return new ResponseEntity<>("No data", HttpStatus.NO_CONTENT);
        }

        String libinsightAccessToken = libinsightController.fetchLibinsightToken().getBody();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set(Helper.AUTH_TOKEN_KEY, Helper.AUTH_TOKEN_PREFIX + libinsightAccessToken);

        HttpEntity<String> entity = new HttpEntity<String>(payload, httpHeaders);

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate
                .exchange(LIBINSIGHT_STORE_URL, HttpMethod.POST, entity, String.class)
                .getBody();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}