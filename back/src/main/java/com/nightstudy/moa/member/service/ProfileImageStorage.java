package com.nightstudy.moa.member.service;

import com.nightstudy.moa.common.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

// 프로필 이미지의 검사와 로컬 파일 저장을 담당합니다.
@Component
public class ProfileImageStorage {

    private static final Logger log =
            LoggerFactory.getLogger(ProfileImageStorage.class);

    private static final int MAX_DIMENSION = 4096;

    private final Path rootDirectory;
    private final DataSize maxFileSize;

    public ProfileImageStorage(
            @Value("${moa.upload.profile-dir}") String directory,
            @Value("${spring.servlet.multipart.max-file-size}") DataSize maxFileSize

    ) {
        this.rootDirectory = Path.of(directory)
                .toAbsolutePath()
                .normalize();

        this.maxFileSize = maxFileSize;
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalidImage("사진 파일을 선택해주세요.");
        }

        if (file.getSize() > maxFileSize.toBytes()) {
            throw invalidImage("사진이 허용된 최대 크기를 초과했습니다.");
        }

        String contentType = file.getContentType();

        if (!"image/jpeg".equals(contentType)
                && !"image/png".equals(contentType)) {
            throw invalidImage("JPG 또는 PNG 사진만 올릴 수 있습니다.");
        }

        BufferedImage image = readImage(file, contentType);
        String extension = "image/png".equals(contentType) ? "png" : "jpg";

        // 사용자 파일명을 사용하지 않고 서버에서 새 이름을 만듦
        String fileName = UUID.randomUUID() + "." + extension;
        Path target = resolve(fileName);

        try {
            Files.createDirectories(rootDirectory);

            try (OutputStream output = Files.newOutputStream(
                    target,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            )) {
                if (!ImageIO.write(image, extension, output)) {
                    throw new IOException("이미지 저장 형식을 지원하지 않습니다.");
                }
            }

            return fileName;
        } catch (IOException e) {
            delete(fileName);

            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "PROFILE_IMAGE_SAVE_FAILED",
                    "사진 저장에 실패했습니다."
            );
        }
    }

    private BufferedImage readImage(
            MultipartFile file,
            String contentType
    ) {
        try (InputStream input = file.getInputStream();
             ImageInputStream imageInput =
                     ImageIO.createImageInputStream(input)) {

            if (imageInput == null) {
                throw invalidImage("올바른 이미지 파일이 아닙니다.");
            }

            Iterator<ImageReader> readers =
                    ImageIO.getImageReaders(imageInput);

            if (!readers.hasNext()) {
                throw invalidImage("올바른 이미지 파일이 아닙니다.");
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(imageInput, true, true);

                String format = reader.getFormatName()
                        .toLowerCase(Locale.ROOT);

                boolean validFormat =
                        ("image/png".equals(contentType)
                                && "png".equals(format))
                                || ("image/jpeg".equals(contentType)
                                && ("jpeg".equals(format)
                                || "jpg".equals(format)));

                if (!validFormat) {
                    throw invalidImage("파일의 실제 형식이 JPG 또는 PNG가 아닙니다.");
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                // 압축된 파일 읽을 때 과도한 메모리 사용 제한.
                if (width <= 0 || height <= 0
                        || width > MAX_DIMENSION
                        || height > MAX_DIMENSION) {
                    throw invalidImage("사진의 가로와 세로는 각각 4096픽셀 이하여야 합니다.");
                }

                BufferedImage image = reader.read(0);

                if (image == null) {
                    throw invalidImage("사진을 읽을 수 없습니다.");
                }

                return image;
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw invalidImage("손상됐거나 읽을 수 없는 사진입니다.");
        }
    }

    public Resource load(String fileName) {
        Path target = resolve(fileName);

        if (!Files.isRegularFile(target) || !Files.isReadable(target)) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "PROFILE_IMAGE_NOT_FOUND",
                    "프로필 사진을 찾을 수 없습니다."
            );
        }

        return new FileSystemResource(target);
    }

    public void delete(String fileName) {
        if (fileName == null) {
            return;
        }

        try {
            Files.deleteIfExists(resolve(fileName));
        } catch (IOException e) {
            // 파일 경로나 회원정보를 로그에 출력하지 않음
            log.warn("프로필 사진 파일 정리에 실패했습니다.");
        }
    }

    private Path resolve(String fileName) {
        if (fileName == null
                || !fileName.matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-"
                        + "[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png)"
        )) {
            throw invalidImage("올바른 사진 경로가 아닙니다.");
        }

        return rootDirectory.resolve(fileName).normalize();
    }

    private ApiException invalidImage(String message) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_PROFILE_IMAGE",
                message
        );
    }
}