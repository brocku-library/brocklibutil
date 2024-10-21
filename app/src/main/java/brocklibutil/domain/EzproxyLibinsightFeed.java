package brocklibutil.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EzproxyLibinsightFeed {

    @JsonProperty("ts_start")
    private String dateTimeStr;

    @JsonProperty("field_97")
    private String url;

    @JsonProperty("field_98")
    private long hits;

    public EzproxyLibinsightFeed(String dateTimeStr, String url, long hits) {
        this.dateTimeStr = dateTimeStr;
        this.url = url;
        this.hits = hits;
    }

    public String getDateTimeStr() {
        return dateTimeStr;
    }

    public void setDateTimeStr(String dateTimeStr) {
        this.dateTimeStr = dateTimeStr;
    }

    public long getHits() {
        return hits;
    }

    public void setHits(long hits) {
        this.hits = hits;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
