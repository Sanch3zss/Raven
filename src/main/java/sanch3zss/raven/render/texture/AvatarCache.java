package sanch3zss.raven.render.texture;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

import sanch3zss.raven.client.resource.ByteArrayResource;

public final class AvatarCache {
    public static final ConcurrentHashMap<String, GlTexture> textureCache = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, CompletableFuture<GlTexture>> pendingLoads = new ConcurrentHashMap<>();
    public static final int connectTimeoutMs = 5000;
    public static final int readTimeoutMs = 10000;
    public static final int maxSizeBytes = 5242880;

    public static CompletableFuture<GlTexture> load(String str, GlTexture fallbackTexture, Executor executor) {
        if (str == null || str.isBlank() || str.endsWith("/null")) {
            return CompletableFuture.completedFuture(fallbackTexture);
        }
        GlTexture texture = textureCache.get(str);
        if (texture != null) {
            return CompletableFuture.completedFuture(texture);
        }
        return pendingLoads.computeIfAbsent(str, url -> CompletableFuture.supplyAsync(() -> loadWithRetry(url), executor)
            .thenApply(downloaded -> {
                if (downloaded != null) {
                    textureCache.put(url, downloaded);
                }
                return downloaded;
            }).handle((result, th) -> {
                pendingLoads.remove(url);
                if (th != null) {
                    System.out.println("[AvatarCache] FAIL url=" + url + " err=" + th.getCause());
                }
                return (th != null || result == null) ? fallbackTexture : result;
            }));
    }

    public static GlTexture loadWithRetry(String str) {
        RuntimeException lastException = null;
        for (int i = 1; i <= 5; i++) {
            try {
                return download(str);
            } catch (RuntimeException e) {
                lastException = e;
                if (!isRetryable(e)) {
                    throw e;
                }
                try {
                    Thread.sleep(500L << (i - 1));
                } catch (InterruptedException ignored) {
                }
                System.out.println("[AvatarCache] RETRY " + i + "/5 url=" + str + " cause=" + e.getCause());
            }
        }
        throw lastException;
    }

    public static boolean isRetryable(Throwable th) {
        Throwable cause = th;
        while (cause != null) {
            if ((cause instanceof SocketTimeoutException) || (cause instanceof ConnectException) || (cause instanceof UnknownHostException)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    public static GlTexture download(String str) {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(str).toURL().openConnection();
            conn.setConnectTimeout(connectTimeoutMs);
            conn.setReadTimeout(readTimeoutMs);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setInstanceFollowRedirects(true);
            try {
                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    System.out.println("[AvatarCache] HTTP " + responseCode + " url=" + str + " location=" + conn.getHeaderField("Location") + " contentType=" + conn.getContentType());
                    throw new RuntimeException("HTTP " + responseCode);
                }
                try (InputStream inputStream = conn.getInputStream()) {
                    return new GlTexture(new ByteArrayResource(readAllBytes(inputStream)));
                }
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }

    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(8192);
        byte[] buffer = new byte[8192];
        int totalBytes = 0;
        while (true) {
            int read = inputStream.read(buffer);
            if (read == -1) {
                return byteArrayOutputStream.toByteArray();
            }
            totalBytes += read;
            if (totalBytes > maxSizeBytes) {
                throw new RuntimeException("Avatar too large: " + totalBytes);
            }
            byteArrayOutputStream.write(buffer, 0, read);
        }
    }
}
