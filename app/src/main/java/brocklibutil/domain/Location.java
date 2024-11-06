package brocklibutil.domain;

public enum Location {

    A("Classroom A",
            "https://outlook.office365.com/owa/calendar/5f23feb742c74d13b146423a721005b1@brocku.ca/7ad69b79396b432b9a8efc002f690e6014281070077971958054/calendar.ics"),
    B("Classroom B",
            "https://outlook.office365.com/owa/calendar/03c6fd88fe3e4748ae2143b085315d30@brocku.ca/583a9579149143cd9bb30961437149f710974516195299954123/calendar.ics"),
    AUDIO("Audio Lab",
            "https://outlook.office365.com/owa/calendar/f6fd2446051242b1a5aff92fda0a0f4b@brocku.onmicrosoft.com/9f1823f7f95446bf9177c2b9e032537512783951697716062366/calendar.ics"),
    VIDEO("Video Lab",
            "https://outlook.office365.com/owa/calendar/31eed3a812354c8aa870d25362070bcf@brocku.onmicrosoft.com/0cebe67d1b394084ae46a1c158cc678916855064932815811391/calendar.ics");

    String title;
    String URI;

    Location(String title, String URI) {
        this.title = title;
        this.URI = URI;
    }

    public String getTitle() {
        return this.title;
    }

    public String getURI() {
        return this.URI;
    }
}
