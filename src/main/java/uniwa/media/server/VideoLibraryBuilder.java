package uniwa.media.server;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.FFmpegResult;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

public class VideoLibraryBuilder {
    private static final File VIDEO_DIR = new File("videos");
    private static final String[] FORMATS = {"mp4", "mkv", "avi"};
    private static final String[] RESOLUTIONS = {"240p", "360p", "480p", "720p", "1080p"};
    private static final Map<String, Integer> HEIGHT_MAP = Map.of(
        "240p", 240, "360p", 360, "480p", 480, "720p", 720, "1080p", 1080
    );

     public static void buildLibrary() {
        if (!VIDEO_DIR.exists() || VIDEO_DIR.listFiles() == null) {
            System.err.println("videos/ folder missing or unreadable at " +
                VIDEO_DIR.getAbsolutePath());
            return;
        }

        // 1️⃣ Group existing filenames by base title
        Map<String, Set<String>> existing = new HashMap<>();
        for (File f : VIDEO_DIR.listFiles()) {
            String name = f.getName();
            String base = name.replaceAll("-.*", "");
            existing.computeIfAbsent(base, k -> new HashSet<>()).add(name);
        }

        // 2️⃣ For each title, find its highest‐res source, then transcode lower res/formats
        for (var entry : existing.entrySet()) {
            String base = entry.getKey();
            Set<String> names = entry.getValue();

            // a) Determine max resolution height
            int maxHeight = names.stream()
                .map(n -> {
                   var m = Pattern.compile(".*-(\\d{3,4})p\\..*").matcher(n);
                   return m.matches() ? Integer.parseInt(m.group(1)) : 0;
                })
                .max(Integer::compare).orElse(0);

            // b) Find the exact source filename (with "p" and its extension)
            String sourceName = names.stream()
                .filter(n -> n.matches(Pattern.quote(base) + "-" + maxHeight + "p\\.[^.]+"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                    "No source file for " + base + " at " + maxHeight + "p"));

            File sourceFile = new File(VIDEO_DIR, sourceName);

            // c) Transcode missing variants
            for (String res : RESOLUTIONS) {
                int h = HEIGHT_MAP.get(res);
                if (h > maxHeight) continue;

                for (String fmt : FORMATS) {
                    String targetName = base + "-" + res + "." + fmt;
                    File targetFile = new File(VIDEO_DIR, targetName);
                    if (!targetFile.exists()) {
                        System.out.println("Creating: " + targetName);
                        transcode(sourceFile, targetFile, h);
                    }
                }
            }
        }
    }

    private static void transcode(File source, File target, int height) {
        FFmpeg.atPath()
          .addInput(com.github.kokorin.jaffree.ffmpeg.UrlInput.fromPath(source.toPath()))
          .addOutput(
              UrlOutput.toPath(target.toPath())
                       .addArguments("-vf", "scale=-2:" + height)
                       .addArguments("-c:v", "libx264")  // specify codec if needed
                       .addArguments("-preset", "ultrafast") // optional
                       .addArguments("-c:a", "copy")  // copy audio as-is
          )
          .execute();
    }

    public static List<File> getAllFiles() {
        if (!VIDEO_DIR.exists() || VIDEO_DIR.listFiles() == null) {
            System.err.println("videos/ folder missing or unreadable at " + 
                VIDEO_DIR.getAbsolutePath());
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
        
        return result;
    }
}

    

