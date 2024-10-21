package brocklibutil.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LibcalLibinsightFeed {

    private static final String GRAD_ROOM = "Graduate Meeting Room";
    /*
     * We have one field name from Libcal and a different name on the Libinsight
     * side.
     * To use two different names during marshalling and unmarshalling we are using
     * separate getter and setter for the same property.
     * "payload": [
     * {
     * "id": "field_88",
     * "dataset_id": 12209,
     * "name": "Booking Start",
     * "type": "DateTime"
     * },
     * {
     * "id": "field_89",
     * "dataset_id": 12209,
     * "name": "Booking End",
     * "type": "DateTime"
     * },
     * {
     * "id": "field_90",
     * "dataset_id": 12209,
     * "name": "Location",
     * "type": "Text"
     * },
     * {
     * "id": "field_91",
     * "dataset_id": 12209,
     * "name": "Room Info",
     * "type": "Text"
     * }
     * ]
     */

    private String from;
    private String to;
    private String created;
    private String location;
    private String roomInfo;
    private String individualRoomSeat;

    @JsonProperty("ts_start")
    public String getFromDate() {
        return getLibinsightFormattedTime(from);
    }

    @JsonProperty("fromDate")
    public void setFrom(String from) {
        this.from = from;
    }

    @JsonProperty("field_89")
    public String getToDate() {
        return getLibinsightFormattedTime(to);
    }

    @JsonProperty("toDate")
    public void setTo(String to) {
        this.to = to;
    }

    @JsonProperty("field_88")
    public String getStart() {
        return getLibinsightFormattedTime(created);
    }

    @JsonProperty("created")
    public void setCreated(String created) {
        this.created = created;
    }

    @JsonProperty("field_90")
    public String getLoc() {
        return location;
    }

    @JsonProperty("location_name")
    public void setLocation(String location) {
        this.location = location;
    }

    @JsonProperty("field_91")
    public String getRoomNo() {
        if (individualRoomSeat != null) {
            roomInfo = individualRoomSeat;
        }

        if (roomInfo.contains(GRAD_ROOM)) {
            String room = roomInfo.split("\\s")[0];
            roomInfo = room.substring(0, 2) + " " + room.substring(2);
        }

        return roomInfo;
    }

    @JsonProperty("item_name")
    public void setRoomInfo(String roomInfo) {
        this.roomInfo = roomInfo;
    }

    @JsonIgnore
    public String getIndividualRoomSeat() {
        return individualRoomSeat;
    }

    @JsonProperty("seat_name")
    public void setIndividualRoomSeat(String individualRoomSeat) {
        this.individualRoomSeat = individualRoomSeat;
    }

    private String getLibinsightFormattedTime(String dateStr) {
        String date = dateStr.substring(0, 10);
        String time = dateStr.substring(11, 16);

        return date + " " + time;
    }
}
