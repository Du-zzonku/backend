package com.test.dosa_backend.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageInputsTest {

    @Test
    void toDataUrl_encodes_png() {
        MockMultipartFile f = new MockMultipartFile(
                "images",
                "a.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        String url = ImageInputs.toDataUrl(f);
        assertThat(url).startsWith("data:image/png;base64,");
    }

    @Test
    void toDataUrl_rejects_large_file() {
        byte[] bytes = new byte[(int) (ImageInputs.MAX_IMAGE_BYTES + 1)];
        MockMultipartFile f = new MockMultipartFile(
                "images",
                "big.png",
                "image/png",
                bytes
        );

        assertThatThrownBy(() -> ImageInputs.toDataUrl(f))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too large");
    }

    @Test
    void toDataUrl_rejects_unsupported_content_type() {
        MockMultipartFile f = new MockMultipartFile(
                "images",
                "a.txt",
                "text/plain",
                "nope".getBytes()
        );

        assertThatThrownBy(() -> ImageInputs.toDataUrl(f))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported image content type");
    }
}

