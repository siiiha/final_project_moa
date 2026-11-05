package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.common.exception.ApiException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

// AI작성: 영수증은 공개 이미지 저장소와 분리한다. 원본 파일명·개인정보·해시를 로그에 남기지 않는다.
@Component
public class SavingGroupReceiptStorage {
    private final Path root;
    public SavingGroupReceiptStorage(@Value("${moa.upload.receipt-dir:./uploads/private-receipts}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
    }
    public record Prepared(byte[] bytes, String hash, String extension) {}

    // AI작성: 기존 프로필 이미지 정책처럼 JPG·PNG, 최대 5MB·4096픽셀을 검사한다. 해시는 업로드 원본 기준이다.
    public Prepared prepare(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 5L * 1024 * 1024)
            throw invalid("5MB 이하의 영수증 이미지를 첨부해주세요.");
        String type = file.getContentType();
        if (!"image/jpeg".equals(type) && !"image/png".equals(type)) throw invalid("JPG 또는 PNG 영수증을 첨부해주세요.");
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0 || bytes.length > 5L * 1024 * 1024) throw invalid("영수증 크기를 확인해주세요.");
            try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw invalid("유효한 영수증 이미지가 아닙니다.");
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    String format = reader.getFormatName();
                    if (!("image/png".equals(type) ? "png".equalsIgnoreCase(format) : "jpeg".equalsIgnoreCase(format)))
                        throw invalid("영수증 형식이 일치하지 않습니다.");
                    if (reader.getWidth(0) < 1 || reader.getHeight(0) < 1 || reader.getWidth(0) > 4096 || reader.getHeight(0) > 4096)
                        throw invalid("영수증 가로·세로는 4096픽셀 이하여야 합니다.");
                    reader.read(0);
                } finally { reader.dispose(); }
            }
            return new Prepared(bytes, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),
                    "image/png".equals(type) ? "png" : "jpg");
        } catch (IOException e) { throw invalid("영수증 이미지를 읽을 수 없습니다."); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e); }
    }
    public String store(Prepared prepared) {
        String name = UUID.randomUUID() + "." + prepared.extension();
        try {
            Files.createDirectories(root);
            Files.write(resolve(name), prepared.bytes(), StandardOpenOption.CREATE_NEW);
            return name;
        } catch (IOException e) {
            delete(name);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "RECEIPT_SAVE_FAILED", "영수증 저장에 실패했습니다.");
        }
    }
    public Resource read(String name) {
        try { return new ByteArrayResource(Files.readAllBytes(resolve(name))) {
            @Override public String getFilename() { return name; }
        }; } catch (IOException e) { throw new ApiException(HttpStatus.NOT_FOUND, "RECEIPT_NOT_FOUND", "영수증을 찾을 수 없습니다."); }
    }
    public void delete(String name) {
        try { Files.deleteIfExists(resolve(name)); }
        catch (IOException e) { throw new IllegalStateException("영수증 정리가 필요합니다.", e); }
    }
    private Path resolve(String name) {
        if (name == null || !name.matches("[0-9a-f-]{36}\\.(png|jpg)"))
            throw new ApiException(HttpStatus.NOT_FOUND, "RECEIPT_NOT_FOUND", "영수증을 찾을 수 없습니다.");
        return root.resolve(name);
    }
    private ApiException invalid(String message) { return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RECEIPT", message); }
}
