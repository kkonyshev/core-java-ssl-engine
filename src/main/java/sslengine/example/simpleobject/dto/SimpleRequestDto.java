package sslengine.example.simpleobject.dto;

import java.io.Serializable;
import java.util.Date;

public class SimpleRequestDto implements Serializable {
    public final long requestDate;
    public SimpleRequestDto(Date requestDate) {
        this.requestDate = requestDate.getTime();
    }

    @Override
    public String toString() {
        return "requestDate=" + this.requestDate;
    }
}
