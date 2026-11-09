package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import com.nightstudy.moa.common.exception.ApiException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

// AI작성: 임시 디렉터리와 내부 이미지로 해시·경로 제한·위장 파일 검증을 수행한다.
class SavingGroupReceiptStorageTest {
    @TempDir Path directory;
    @Test void hashesOriginalBytesIndependentOfFilenameAndStoresPrivately() throws Exception {
        var storage = new SavingGroupReceiptStorage(directory.toString());
        var out = new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", out);
        var a = storage.prepare(new MockMultipartFile("receipt", "a.png", "image/png", out.toByteArray()));
        var b = storage.prepare(new MockMultipartFile("receipt", "b.png", "image/png", out.toByteArray()));
        assertThat(a.hash()).matches("[0-9a-f]{64}").isEqualTo(b.hash());
        String name = storage.store(a); assertThat(name).doesNotContain("a.png", "/", "\\");
        assertThat(storage.read(name).getContentAsByteArray()).isEqualTo(out.toByteArray());
        storage.delete(name); assertThatThrownBy(() -> storage.read(name)).isInstanceOf(ApiException.class);
    }
    @Test void rejectsMissingFakeAndOversizedImagesAndTraversal() {
        var storage = new SavingGroupReceiptStorage(directory.toString());
        assertThatThrownBy(() -> storage.prepare(null)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> storage.prepare(new MockMultipartFile("receipt", "fake.png", "image/png", new byte[]{1,2}))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> storage.prepare(new MockMultipartFile("receipt", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1]))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> storage.read("../outside.png")).isInstanceOf(ApiException.class);
    }
}
