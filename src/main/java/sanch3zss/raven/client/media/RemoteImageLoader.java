package sanch3zss.raven.client.media;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sanch3zss.raven.render.texture.AnimatedTexture;

public final class RemoteImageLoader {
    private static final int MAX_IMAGE_BYTES = 32 * 1024 * 1024;
    private static final int MAX_PAGE_BYTES = 4 * 1024 * 1024;
    private static final int MAX_DIMENSION = 1024;
    private static final int MAX_FRAMES = 240;
    private static final long MAX_TOTAL_PIXELS = 48_000_000L;
    private static final Pattern EMBEDDED_MEDIA = Pattern.compile(
            "https?:\\\\?/\\\\?/[^\\\"'<> ]+?\\.(?:gif|png|jpe?g|webp)(?:\\?[^\\\"'<> ]*)?",
            Pattern.CASE_INSENSITIVE);
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "raven-remote-image");
        thread.setDaemon(true);
        return thread;
    });
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(20))
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

    private RemoteImageLoader() {
    }

    public static CompletableFuture<AnimatedTexture> load(String value) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URI uri = checkedUri(value);
                Download download = download(uri.toString(), MAX_IMAGE_BYTES);
                if (isImage(download.contentType(), download.bytes())) {
                    return decode(download.bytes());
                }
                if (!isPage(download.contentType(), download.bytes())) {
                    throw new IOException("The URL did not return an image or an HTML page");
                }
                String mediaUrl = resolveMediaUrl(download);
                if (mediaUrl == null) {
                    throw new IOException("No image metadata was found on this page");
                }
                Download media = download(mediaUrl, MAX_IMAGE_BYTES);
                if (!isImage(media.contentType(), media.bytes())) {
                    throw new IOException("Resolved media URL did not return an image");
                }
                return decode(media.bytes());
            } catch (Exception exception) {
                throw new IllegalStateException(exception.getMessage(), exception);
            }
        }, EXECUTOR);
    }

    private static URI checkedUri(String value) {
        URI uri = URI.create(value == null ? "" : value.trim());
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Only http and https links are supported");
        }
        return uri;
    }

    private static Download download(String url, int limit) throws IOException {
        Request request = new Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131 Safari/537.36")
                .header("Accept", "image/gif,image/png,image/jpeg,image/webp,text/html;q=0.9,*/*;q=0.5")
                .build();
        try (Response response = HTTP.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Empty response");
            }
            long declaredLength = body.contentLength();
            if (declaredLength > limit) {
                throw new IOException("Remote file is too large");
            }
            return new Download(response.request().url().toString(),
                    response.header("Content-Type", ""), readLimited(body.byteStream(), limit));
        }
    }

    private static byte[] readLimited(InputStream input, int limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (output.size() + read > limit) {
                throw new IOException("Remote file is too large");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static String resolveMediaUrl(Download page) {
        String html = new String(page.bytes(), StandardCharsets.UTF_8);
        Document document = Jsoup.parse(html, page.url());
        Set<String> candidates = new LinkedHashSet<>();
        addMeta(document, candidates, "meta[property=og:image]", "content");
        addMeta(document, candidates, "meta[property=og:image:url]", "content");
        addMeta(document, candidates, "meta[property=og:image:secure_url]", "content");
        addMeta(document, candidates, "meta[name=twitter:image]", "content");
        addMeta(document, candidates, "meta[name=twitter:image:src]", "content");
        addMeta(document, candidates, "link[rel=image_src]", "href");
        for (Element image : document.select("img[src]")) {
            addCandidate(candidates, image.absUrl("src"));
        }
        Matcher matcher = EMBEDDED_MEDIA.matcher(html);
        while (matcher.find()) {
            addCandidate(candidates, matcher.group().replace("\\/", "/").replace("&amp;", "&"));
        }
        return candidates.stream()
                .filter(RemoteImageLoader::isHttpUrl)
                .min(Comparator.comparingInt(RemoteImageLoader::candidateRank))
                .orElse(null);
    }

    private static void addMeta(Document document, Set<String> candidates, String selector, String attribute) {
        Elements elements = document.select(selector);
        for (Element element : elements) {
            String absolute = element.absUrl(attribute);
            addCandidate(candidates, absolute.isEmpty() ? element.attr(attribute) : absolute);
        }
    }

    private static void addCandidate(Set<String> candidates, String value) {
        if (value != null && !value.isBlank()) {
            candidates.add(value.trim());
        }
    }

    private static boolean isHttpUrl(String value) {
        try {
            String scheme = URI.create(value).getScheme();
            return scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static int candidateRank(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains(".gif")) return 0;
        if (lower.contains("media.tenor.com") || lower.contains("media.giphy.com")) return 1;
        if (lower.contains("og") || lower.contains("original")) return 2;
        return 3;
    }

    private static boolean isPage(String contentType, byte[] bytes) {
        String type = contentType.toLowerCase(Locale.ROOT);
        if (type.contains("text/html") || type.contains("application/xhtml")) {
            return true;
        }
        int count = Math.min(bytes.length, 256);
        String prefix = new String(bytes, 0, count, StandardCharsets.US_ASCII).toLowerCase(Locale.ROOT);
        return prefix.contains("<html") || prefix.contains("<!doctype html");
    }

    private static boolean isImage(String contentType, byte[] bytes) {
        if (contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return true;
        }
        return isGif(bytes) || startsWith(bytes, 0x89, 0x50, 0x4E, 0x47)
                || startsWith(bytes, 0xFF, 0xD8, 0xFF)
                || startsWith(bytes, 'R', 'I', 'F', 'F');
    }

    private static AnimatedTexture decode(byte[] bytes) throws IOException {
        return isGif(bytes) ? decodeGif(bytes) : decodeStatic(bytes);
    }

    private static AnimatedTexture decodeStatic(byte[] bytes) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) {
            throw new IOException("Unsupported image format");
        }
        Size size = fittedSize(image.getWidth(), image.getHeight());
        BufferedImage fitted = AnimatedTexture.scaled(toArgb(image), size.width(), size.height());
        return new AnimatedTexture(size.width(), size.height(),
                List.of(new AnimatedTexture.DecodedFrame(fitted, 100)));
    }

    private static AnimatedTexture decodeGif(byte[] bytes) throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
        if (!readers.hasNext()) {
            throw new IOException("GIF decoder is unavailable");
        }
        ImageReader reader = readers.next();
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            reader.setInput(stream, false, false);
            int count = Math.min(reader.getNumImages(true), MAX_FRAMES);
            if (count <= 0) {
                throw new IOException("GIF has no frames");
            }
            FrameInfo firstInfo = frameInfo(reader.getImageMetadata(0));
            BufferedImage first = reader.read(0);
            int canvasWidth = Math.max(first.getWidth() + firstInfo.left(), streamDimension(reader, "logicalScreenWidth"));
            int canvasHeight = Math.max(first.getHeight() + firstInfo.top(), streamDimension(reader, "logicalScreenHeight"));
            canvasWidth = Math.max(1, canvasWidth);
            canvasHeight = Math.max(1, canvasHeight);
            Size fitted = fittedSize(canvasWidth, canvasHeight, count);
            BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
            List<AnimatedTexture.DecodedFrame> frames = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                IIOMetadata metadata = reader.getImageMetadata(index);
                FrameInfo info = frameInfo(metadata);
                BufferedImage frame = index == 0 ? first : reader.read(index);
                BufferedImage before = "restoreToPrevious".equals(info.disposal()) ? copy(canvas) : null;
                Graphics2D graphics = canvas.createGraphics();
                graphics.setComposite(AlphaComposite.SrcOver);
                graphics.drawImage(frame, info.left(), info.top(), null);
                graphics.dispose();
                BufferedImage snapshot = copy(canvas);
                frames.add(new AnimatedTexture.DecodedFrame(
                        AnimatedTexture.scaled(snapshot, fitted.width(), fitted.height()), info.delayMs()));
                if ("restoreToBackgroundColor".equals(info.disposal())) {
                    Graphics2D clear = canvas.createGraphics();
                    clear.setComposite(AlphaComposite.Clear);
                    clear.fillRect(info.left(), info.top(), frame.getWidth(), frame.getHeight());
                    clear.dispose();
                } else if (before != null) {
                    canvas = before;
                }
            }
            return new AnimatedTexture(fitted.width(), fitted.height(), frames);
        } finally {
            reader.dispose();
        }
    }

    private static int streamDimension(ImageReader reader, String attribute) {
        try {
            IIOMetadata metadata = reader.getStreamMetadata();
            Node root = metadata.getAsTree("javax_imageio_gif_stream_1.0");
            Node descriptor = child(root, "LogicalScreenDescriptor");
            return descriptor == null ? 0 : Integer.parseInt(attribute(descriptor, attribute, "0"));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static FrameInfo frameInfo(IIOMetadata metadata) {
        try {
            Node root = metadata.getAsTree("javax_imageio_gif_image_1.0");
            Node control = child(root, "GraphicControlExtension");
            Node descriptor = child(root, "ImageDescriptor");
            int delay = Integer.parseInt(attribute(control, "delayTime", "10")) * 10;
            return new FrameInfo(
                    Integer.parseInt(attribute(descriptor, "imageLeftPosition", "0")),
                    Integer.parseInt(attribute(descriptor, "imageTopPosition", "0")),
                    Math.max(20, delay), attribute(control, "disposalMethod", "none"));
        } catch (Exception ignored) {
            return new FrameInfo(0, 0, 100, "none");
        }
    }

    private static Node child(Node parent, String name) {
        if (parent == null) return null;
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (name.equals(node.getNodeName())) return node;
        }
        return null;
    }

    private static String attribute(Node node, String name, String fallback) {
        if (node == null || node.getAttributes() == null || node.getAttributes().getNamedItem(name) == null) {
            return fallback;
        }
        return node.getAttributes().getNamedItem(name).getNodeValue();
    }

    private static Size fittedSize(int width, int height) {
        return fittedSize(width, height, 1);
    }

    private static Size fittedSize(int width, int height, int frameCount) {
        float dimensionScale = Math.min(1.0f, MAX_DIMENSION / (float) Math.max(width, height));
        double totalPixels = (double) width * height * Math.max(1, frameCount);
        float memoryScale = totalPixels <= MAX_TOTAL_PIXELS ? 1.0f
                : (float) Math.sqrt(MAX_TOTAL_PIXELS / totalPixels);
        float scale = Math.min(dimensionScale, memoryScale);
        return new Size(Math.max(1, Math.round(width * scale)), Math.max(1, Math.round(height * scale)));
    }

    private static BufferedImage toArgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_ARGB) return source;
        return copy(source);
    }

    private static BufferedImage copy(BufferedImage source) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return result;
    }

    private static boolean isGif(byte[] bytes) {
        return startsWith(bytes, 'G', 'I', 'F', '8');
    }

    private static boolean startsWith(byte[] bytes, int... signature) {
        if (bytes.length < signature.length) return false;
        for (int index = 0; index < signature.length; index++) {
            if ((bytes[index] & 0xFF) != (signature[index] & 0xFF)) return false;
        }
        return true;
    }

    private record Download(String url, String contentType, byte[] bytes) {
    }

    private record Size(int width, int height) {
    }

    private record FrameInfo(int left, int top, int delayMs, String disposal) {
    }
}
