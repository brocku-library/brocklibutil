package brocklibutil.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;

import brocklibutil.controller.LibinsightController;
import brocklibutil.helper.Helper;

@Service
public class WiFiStatsFetchService {

    Logger logger = LoggerFactory.getLogger(WiFiStatsFetchService.class);

    static final String DATASET_ID = "12260";
    static final String LIBINSIGHT_STORE_URL = "https://brocku.libinsight.com/v1.0/custom-dataset/" + DATASET_ID
            + "/data-grid?id=" + DATASET_ID + "&from=%s&to=%s&sort=desc&page=1";

    @Autowired
    LibinsightController libinsightController;

    @Cacheable("responseLib")
    public String getResponse() throws JsonProcessingException {
        logger.info("Fetching WiFi stats from Libinsight");

        String libinsightAccessToken = libinsightController.fetchLibinsightToken().getBody();

        LocalDateTime dateTimeNow = LocalDateTime.now();
        dateTimeNow.atZone(ZoneId.of("America/Toronto"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDateTime dateTimeYesterday = dateTimeNow.minusHours(24);
        String today = dateTimeNow.format(formatter);
        String yesterday = dateTimeYesterday.format(formatter);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set(Helper.AUTH_TOKEN_KEY, Helper.AUTH_TOKEN_PREFIX + libinsightAccessToken);

        String url = String.format(LIBINSIGHT_STORE_URL, yesterday, today);

        HttpEntity<String> entity = new HttpEntity<String>(httpHeaders);
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate
                .exchange(url, HttpMethod.GET, entity, String.class)
                .getBody();

        return response;
    }
}
