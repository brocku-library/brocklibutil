package brocklibutil.controller;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.core.JsonProcessingException;

import brocklibutil.service.WiFiStatsFetchService;

@Controller
@RequestMapping("/busylib")
public class WifiLibinsightController {

    Logger logger = LoggerFactory.getLogger(WifiLibinsightController.class);

    @Autowired
    private WiFiStatsFetchService wiFiStatsFetchService;

    @Autowired
    private CacheManager cacheManager;

    @GetMapping
    String view(ModelMap model) throws JsonProcessingException {
        Cache cache = cacheManager.getCache("response");

        try {
            model.put("data", cache.get("jsonStr").get());
        } catch (Exception e) {
            model.put("data", wiFiStatsFetchService.getResponse());
        }

        return "wifi";
    }

    @PostMapping
    ResponseEntity<?> store(@RequestBody String jsonString) {
        String jsonResponse = URLDecoder.decode(jsonString, StandardCharsets.UTF_8)
                .replace("jsonString=", "")
                .replace("field_131", "Floor")
                .replace("field_132", "Count")
                .replace("ts_start", "_start_date");

        jsonResponse = String.format("{\"payload\": { \"records\": %s}}", jsonResponse);

        Cache cache = cacheManager.getCache("response");
        cache.put("jsonStr", jsonResponse);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @CacheEvict(value = "responseLib", allEntries = true)
    @Scheduled(fixedRateString = "${caching.ttl}")
    public void emptyCache() {
        logger.info("Libinsight data cache cleared");
    }
}
