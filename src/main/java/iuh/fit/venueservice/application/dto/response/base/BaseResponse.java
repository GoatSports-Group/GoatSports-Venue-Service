package iuh.fit.venueservice.application.dto.response.base;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> {
    private Integer statusCode;
    private T data;
    private Object message;
    private String error;
}
