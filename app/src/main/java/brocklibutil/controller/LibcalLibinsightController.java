package brocklibutil.controller;

import static brocklibutil.helper.Helper.ACCESS_TOKEN_KEY;
import static brocklibutil.helper.Helper.AUTH_TOKEN_KEY;
import static brocklibutil.helper.Helper.AUTH_TOKEN_PREFIX;
import static brocklibutil.helper.Helper.CLIENT_ID_KEY;
import static brocklibutil.helper.Helper.CLIENT_SECRET_KEY;
import static brocklibutil.helper.Helper.GRANT_TYPE_KEY;
import static brocklibutil.helper.Helper.GRANT_TYPE_VALUE;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.GsonJsonParser;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import brocklibutil.domain.LibcalLibinsightFeed;

@RestController
@RequestMapping("/api/libinsight/libcal")
public class LibcalLibinsightController {

    static final String FETCH_LIBCAL_ACCESS_TOKEN_URL = "https://calendar.library.brocku.ca/1.1/oauth/token";
    static final String FETCH_BOOKINGS_URL = "https://calendar.library.brocku.ca/1.1/space/bookings?date={date}&days=0&limit=500";

    static final String DATASET_ID = "12209";
    static final String LIBINSIGHT_STORE_URL = "https://brocku.libinsight.com/post/v1.0/custom/" + DATASET_ID
            + "/type/1/save";

    @Value("${libcal.clientId}")
    String libcalClientId;

    @Value("${libcal.clientSecret}")
    String libcalClientSecret;

    @Value("${server.port}")
    public int port;

    @Autowired
    private LibinsightController libinsightController;

    @Scheduled(cron = "0 30 0 * * *")
    public void exportDataToLibinsight() throws JsonProcessingException {
        String date = LocalDate.now().minusDays(1).toString();
        feedLibinsight(date);
    }

    @GetMapping("/fetchLibcalToken")
    public ResponseEntity<String> fetchLibcalToken() throws JsonProcessingException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> bodyParamMap = new HashMap<>();
        bodyParamMap.put(CLIENT_ID_KEY, libcalClientId);
        bodyParamMap.put(CLIENT_SECRET_KEY, libcalClientSecret);
        bodyParamMap.put(GRANT_TYPE_KEY, GRANT_TYPE_VALUE);

        HttpEntity<String> entity = new HttpEntity<>(new ObjectMapper().writeValueAsString(bodyParamMap), httpHeaders);

        RestTemplate restTemplate = new RestTemplate();
        String resultJson = restTemplate.postForObject(FETCH_LIBCAL_ACCESS_TOKEN_URL, entity, String.class);

        String accessToken = (String) new GsonJsonParser().parseMap(resultJson).get(ACCESS_TOKEN_KEY);

        return new ResponseEntity<>(accessToken, HttpStatus.OK);
    }

    @GetMapping("/booking")
    public ResponseEntity<List<LibcalLibinsightFeed>> fetchBookings(
            @RequestParam(name = "date", defaultValue = "2022-01-01") String dateStr)
            throws JsonProcessingException {

        if (!dateStr.matches("([0-9]{4})-([0-9]{2})-([0-9]{2})")) {
            throw new IllegalArgumentException();
        }

        String libcalAccessToken = fetchLibcalToken().getBody();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set(AUTH_TOKEN_KEY, AUTH_TOKEN_PREFIX + libcalAccessToken);

        HttpEntity<String> entity = new HttpEntity<String>(httpHeaders);

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate
                .exchange(FETCH_BOOKINGS_URL, HttpMethod.GET, entity, String.class, dateStr)
                .getBody();

        ObjectMapper mapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        List<LibcalLibinsightFeed> mappedList = mapper.readValue(response,
                new TypeReference<List<LibcalLibinsightFeed>>() {
                });

        return new ResponseEntity<>(mappedList, HttpStatus.OK);
    }

    @GetMapping("/feedIntoLibinsight")
    public ResponseEntity<String> feedLibinsight(@RequestParam(name = "date") String dateStr)
            throws JsonProcessingException {

        if (!dateStr.matches("([0-9]{4})-([0-9]{2})-([0-9]{2})")) {
            throw new IllegalArgumentException();
        }

        String payload = new RestTemplate()
                .getForObject("http://localhost:{port}/api/libinsight/libcal/booking?date={date}",
                        String.class, port, dateStr);

        if (payload == null || payload.contains("[]")) {
            return new ResponseEntity<>("No data", HttpStatus.NO_CONTENT);
        }

        String libinsightAccessToken = libinsightController.fetchLibinsightToken().getBody();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set(AUTH_TOKEN_KEY, AUTH_TOKEN_PREFIX + libinsightAccessToken);

        HttpEntity<String> entity = new HttpEntity<String>(payload, httpHeaders);

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate
                .exchange(LIBINSIGHT_STORE_URL, HttpMethod.POST, entity, String.class)
                .getBody();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
