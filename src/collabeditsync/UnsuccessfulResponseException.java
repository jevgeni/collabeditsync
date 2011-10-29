package collabeditsync;

import org.apache.http.StatusLine;

public class UnsuccessfulResponseException extends Exception {
    public UnsuccessfulResponseException(StatusLine statusLine) {
        super(statusLine.toString());
    }

    public UnsuccessfulResponseException() {
    }
}
