package brocklibutil.controller;

import static brocklibutil.helper.Helper.ACCESS_TOKEN_KEY;
import static brocklibutil.helper.Helper.CLIENT_ID_KEY;
import static brocklibutil.helper.Helper.CLIENT_SECRET_KEY;
import static brocklibutil.helper.Helper.GRANT_TYPE_KEY;
import static brocklibutil.helper.Helper.GRANT_TYPE_VALUE;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.GsonJsonParser;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/libinsight")
public class LibinsightController {

    static final String FETCH_LIBINSIGHT_ACCESS_TOKEN_URL = "https://brocku.libinsight.com/v1.0/oauth/token";

    @Value("${libinsight.clientId}")
    String libinsightClientId;

    @Value("${libinsight.clientSecret}")
    String libinsightClientSecret;

    @GetMapping("/fetchLibinsightToken")
    public ResponseEntity<String> fetchLibinsightToken() throws JsonProcessingException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> bodyParamMap = new HashMap<>();
        bodyParamMap.put(CLIENT_ID_KEY, libinsightClientId);
        bodyParamMap.put(CLIENT_SECRET_KEY, libinsightClientSecret);
        bodyParamMap.put(GRANT_TYPE_KEY, GRANT_TYPE_VALUE);

        HttpEntity<String> entity = new HttpEntity<>(new ObjectMapper().writeValueAsString(bodyParamMap), httpHeaders);

        RestTemplate restTemplate = new RestTemplate();
        String resultJson = restTemplate.postForObject(FETCH_LIBINSIGHT_ACCESS_TOKEN_URL, entity, String.class);

        String accessToken = (String) new GsonJsonParser().parseMap(resultJson).get(ACCESS_TOKEN_KEY);

        return new ResponseEntity<>(accessToken, HttpStatus.OK);
    }
}
