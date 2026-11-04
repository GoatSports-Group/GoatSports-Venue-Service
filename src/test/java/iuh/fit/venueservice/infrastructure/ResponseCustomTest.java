package iuh.fit.venueservice.infrastructure;

import iuh.fit.venueservice.application.dto.response.base.BaseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseCustomTest {
    private final ResponseCustom advice = new ResponseCustom();
    private final ServletServerHttpRequest request =
            new ServletServerHttpRequest(new MockHttpServletRequest("GET", "/api/v1/venues/ticket-qr"));
    private final ServletServerHttpResponse response = new ServletServerHttpResponse(new MockHttpServletResponse());

    @Test
    void leavesBinaryBodiesUnwrapped() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G'};

        Object written = advice.beforeBodyWrite(png, returnType(), MediaType.IMAGE_PNG,
                ByteArrayHttpMessageConverter.class, request, response);

        assertThat(written).isSameAs(png);
    }

    @Test
    void stillWrapsJsonBodies() throws Exception {
        Object written = advice.beforeBodyWrite(List.of(1), returnType(), MediaType.APPLICATION_JSON,
                null, request, response);

        assertThat(written).isInstanceOf(BaseResponse.class);
    }

    private static MethodParameter returnType() throws NoSuchMethodException {
        return new MethodParameter(ResponseCustomTest.class.getDeclaredMethod("returnType"), -1);
    }
}
