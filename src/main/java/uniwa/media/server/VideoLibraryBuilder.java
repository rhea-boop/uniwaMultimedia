package uniwa.media.server;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.FFmpegResult;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VideoLibraryBuilder {
    private static final Logger logger = LoggerFactory.getLogger(VideoLibraryBuilder.class);
    
    private static final File VIDEO_DIR = new File("videos");
    private static final List<String> RESOLUTIONS = List.of("240p", "360p", "480p", "720p", "1080p");
    private static final List<String> FORMATS = List.of("mp4", "mkv", "avi");
    private static final Map<String, Integer> HEIGHT_MAP = Map.of(
        "240p", 240,
        "360p", 360,
        "480p", 480,
        "720p", 720,
        "1080p", 1080
    );

    public static void buildLibrary() {
        if (!VIDEO_DIR.exists()) {
            VIDEO_DIR.mkdirs();
            logger.info("Created videos directory at {}", VIDEO_DIR.getAbsolutePath());
        }
        Map<String, File> existing = Collections.emptyMap();
        logger.info("Found {} source videos", existing.size());
        
        for (var entry : existing.entrySet()) {
            String base = entry.getKey();
            File sourceFile = entry.getValue();
            
            int maxHeight = extractHeight(sourceFile.getName());
            logger.debug("Processing {} with max height {}", base, maxHeight);

            for (String res : RESOLUTIONS) {
                int h = HEIGHT_MAP.get(res);
                if (h > maxHeight) continue;

                for (String fmt : FORMATS) {
                    String targetName = base + "-" + res + "." + fmt;
                    File targetFile = new File(VIDEO_DIR, targetName);
                    if (!targetFile.exists()) {
                        logger.info("Creating: {}", targetName);
                        transcode(sourceFile, targetFile, h);
                    }
                }
            }
        }
    }

    private static int extractHeight(String filename) {
        try {
            String heightPart = filename.replaceAll(".*-(\\d+)p\\.[^.]+$", "$1");
            return Integer.parseInt(heightPart);
        } catch (Exception e) {
            logger.warn("Could not extract height from filename: {}", filename);
            return 1080; 
        }
    }

    private static void transcode(File source, File target, int height) {
        try {
            FFmpeg.atPath()
              .addInput(com.github.kokorin.jaffree.ffmpeg.UrlInput.fromPath(source.toPath()))
              .addOutput(
                  UrlOutput.toPath(target.toPath())
                           .addArguments("-vf", "scale=-2:" + height)
                           .addArguments("-c:v", "libx264")  
                           .addArguments("-preset", "ultrafast") 
                           .addArguments("-c:a", "copy")  
              )
              .execute();
            logger.info("Successfully transcoded {} to height {}", target.getName(), height);
        } catch (Exception e) {
            logger.error("Transcoding failed for {}", target.getName(), e);
        }
    }

    public static List<File> getAllFiles() {
        if (!VIDEO_DIR.exists() || VIDEO_DIR.listFiles() == null) {
            logger.error("videos/ folder missing or unreadable at {}", VIDEO_DIR.getAbsolutePath());
            return Collections.emptyList();
        }
        
        List<File> result = new ArrayList<>();
        File[] files = VIDEO_DIR.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    result.add(file);
                }
            }
        }
        
        logger.debug("Found {} files in video directory", result.size());
        return result;
    }
}
