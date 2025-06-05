package uniwa.media.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HLSBuilder {
    private static final Logger logger = LoggerFactory.getLogger(HLSBuilder.class);
    
    private static final String HLS_ROOT = "hls";   
    private static final String VIDEO_ROOT = "videos";
    private static final List<Variant> VARIANTS = List.of(
        new Variant("240p", "426x240", "400k"),
        new Variant("360p", "640x360", "750k"),
        new Variant("480p", "854x480", "1000k"),
        new Variant("720p", "1280x720", "2500k"),
        new Variant("1080p", "1920x1080","4500k")
    );

    private static class Variant {
        String res;       
        String dimensions;
        String vbitrate;  
        int height;       

        Variant(String res, String dimensions, String vbitrate) {
            this.res = res;
            this.dimensions = dimensions;
            this.vbitrate = vbitrate;
            this.height = Integer.parseInt(res.replace("p", ""));
        }
    }

    public static void buildHLSForMovie(String baseName) throws IOException, InterruptedException {
        // 1. find highest quality version of video
        File videoDir = new File(VIDEO_ROOT);
        File bestSrc = findHighestRes(videoDir, baseName);
        if (bestSrc == null) {
            logger.error("No source found for: {}", baseName);
            return;
        }

        // get resolution 
        String sourceFileName = bestSrc.getName();
        Pattern resPattern = Pattern.compile(".*-(\\d+)p\\.[^.]+$");
        Matcher matcher = resPattern.matcher(sourceFileName);
        
        int sourceHeight = 0;
        if (matcher.matches()) {
            sourceHeight = Integer.parseInt(matcher.group(1));
            logger.info("Source video height: {}p", sourceHeight);
        } else {
            logger.error("Could not determine source resolution for: {}", sourceFileName);
            return;
        }

        // 2. create hls structure
        File movieHlsDir = new File(HLS_ROOT, baseName);
        if (!movieHlsDir.exists()) {
            Files.createDirectories(movieHlsDir.toPath());
        }

        // 3. dont create resolutions higher than source
        List<Variant> applicableVariants = new ArrayList<>();
        for (Variant v : VARIANTS) {
            if (v.height <= sourceHeight) {
                applicableVariants.add(v);
                logger.info("Will create HLS variant: {}", v.res);
            } else {
                logger.debug("Skipping variant {} (higher than source)", v.res);
            }
        }

        // 4. create subfolder and run ffmpeg for each video
        for (Variant v : applicableVariants) {
            File variantDir = new File(movieHlsDir, v.res);
            if (!variantDir.exists()) {
                Files.createDirectories(variantDir.toPath());
            }
            String outPlaylist = variantDir.getPath() + "/playlist.m3u8";
            String outPattern  = variantDir.getPath() + "/seg_%03d.ts";

            List<String> cmd = List.of(
                "ffmpeg",
                "-i", bestSrc.getPath(),
                "-c:v", "libx264",
                "-b:v", v.vbitrate,
                "-s",   v.dimensions,
                "-c:a", "aac",
                "-b:a", "128k",
                "-ac",  "2",
                "-hls_time",       "4",            
                "-hls_list_size",  "0",             
                "-hls_segment_filename", outPattern,
                "-hls_flags",      "independent_segments", 
                outPlaylist
            );

            logger.debug("Running: {}", String.join(" ", cmd));
            Process p = new ProcessBuilder(cmd)
                            .inheritIO()
                            .start();
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                logger.error("FFmpeg error generating HLS for {}", v.res);
            } else {
                logger.info("Successfully created HLS variant: {}", v.res);
            }
        }

        // 5. write .m3u8 master playlist
        String masterPath = movieHlsDir.getPath() + "/master.m3u8";
        StringBuilder master = new StringBuilder("#EXTM3U\n");
        for (Variant v : applicableVariants) {
            master.append(String.format(
                "#EXT-X-STREAM-INF:BANDWIDTH=%s,RESOLUTION=%s%n",
                toIntegerBits(v.vbitrate), // convert "400k" → "400000"
                v.dimensions))
                  .append(v.res).append("/playlist.m3u8\n");
        }
        Files.writeString(Paths.get(masterPath), master.toString());
        logger.info("Master playlist written to: {}", masterPath);
    }

    private static File findHighestRes(File videoDir, String baseName) {
        File[] candidates = videoDir.listFiles((dir, name) -> name.startsWith(baseName + "-"));
        if (candidates == null || candidates.length == 0) return null;
        
        File best = null;
        int maxHeight = 0;
        for (File f : candidates) {
            String name = f.getName();
            String resPart = name.replaceAll(".*-(\\d+)p\\..*", "$1");
            try {
                int h = Integer.parseInt(resPart);
                if (h > maxHeight) {
                    maxHeight = h;
                    best = f;
                }
            } catch (NumberFormatException e) {
                logger.debug("Skipping non-matching file: {}", name);
            }
        }
        
        if (best != null) {
            logger.debug("Found highest resolution source: {}", best.getName());
        }
        return best;
    }
    
    private static String toIntegerBits(String vbit) {
        if (vbit.endsWith("k")) {
            return String.valueOf(Integer.parseInt(vbit.replace("k","")) * 1000);
        }
        return vbit;
    }
}
