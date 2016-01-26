package sslengine.dto;

import java.io.Serializable;
import java.util.Date;

public class SimpleResponseDto implements Serializable {
    public final SimpleRequestDto requestDto;
    public final long responseDate;

    public SimpleResponseDto(SimpleRequestDto requestDto, Date responseDate) {
        this.requestDto = requestDto;
        this.responseDate = responseDate.getTime();
    }

    @Override
    public String toString() {
        return "req: " + requestDto + "; responseDate=" + this.responseDate;
    }
}
