package sanch3zss.raven.client.media;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.config.GamePaths;

public final class LyricsService {
    private static final Pattern TIMED_LINE = Pattern.compile("^\\[(\\d+):(\\d{2})(?:\\.(\\d+))?]\\s*(.+)$");
    private static final Pattern ISRC = Pattern.compile("[A-Z0-9]{12}");
    private static final String LRCMUX_SOURCES = "kugou,musixmatch,netease,ytmusic";
    private static final Pattern TITLE_NOISE = Pattern.compile(
            "\\s*[(\\[](?:feat|ft|with|prod)\\.?\\s[^)\\]]*[)\\]]"
                    + "|\\s+-\\s+.*(?:remaster|version|edit|live|mono|stereo|mix|bonus).*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ARTIST_SEPARATOR = Pattern.compile(
            "\\s*(?:[,;/&+]|\\s(?:feat|ft)\\.?\\s|\\s(?:x|и|and|with)\\s)\\s*", Pattern.CASE_INSENSITIVE);
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final Map<String, List<Line>> cache = new ConcurrentHashMap<>();
    private final Map<String, List<Line>> partial = new ConcurrentHashMap<>();
    private final Map<String, Integer> partialScore = new ConcurrentHashMap<>();
    private final Map<String, Long> retryAt = new ConcurrentHashMap<>();
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    private final Gson gson = new Gson();
    private volatile ActiveLyrics active = new ActiveLyrics("", List.of());
    private volatile String wantedKey = "";

    public void update(MediaPlaybackService.Snapshot snapshot) {
        if (snapshot.title().isBlank() || snapshot.artist().isBlank()) return;
        String key = trackKey(snapshot);
        this.wantedKey = key;
        if (key.equals(this.active.key()) && this.cache.containsKey(key)) return;
        List<Line> ready = this.cache.getOrDefault(key, this.partial.get(key));
        if (ready != null && !key.equals(this.active.key())) {
            this.active = new ActiveLyrics(key, ready);
        }
        if (this.cache.containsKey(key) || this.inFlight.contains(key)
                || System.currentTimeMillis() < this.retryAt.getOrDefault(key, 0L)
                || !this.inFlight.add(key)) return;
        Raven.INSTANCE.executor().execute(() -> load(key, snapshot));
    }

    private void load(String key, MediaPlaybackService.Snapshot snapshot) {
        List<Line> stored = readDisk(key);
        if (stored != null) {
            finish(key, stored);
            return;
        }
        AtomicBoolean failed = new AtomicBoolean();
        List<CompletableFuture<Void>> sources = List.of(
                source(key, "Better Lyrics", 4, () -> fetchBetterLyrics(snapshot), failed),
                source(key, "LRCLIB", 3, () -> fetchLrclib(snapshot), failed),
                source(key, "lrc.red", 2, () -> fetchLrcRed(snapshot), failed),
                source(key, "lrcmux", 1, () -> fetchLrcMux(snapshot), failed));
        CompletableFuture.allOf(sources.toArray(CompletableFuture[]::new)).whenComplete((ignored, error) -> {
            List<Line> best = this.partial.remove(key);
            this.partialScore.remove(key);
            if (best != null) {
                finish(key, best);
                writeDisk(key, best);
            } else if (failed.get()) {
                this.retryAt.put(key, System.currentTimeMillis() + 30_000L);
                this.inFlight.remove(key);
            } else {
                finish(key, List.of());
            }
        });
    }

    private CompletableFuture<Void> source(String key, String name, int rank, Callable<List<Line>> fetcher,
                                           AtomicBoolean failed) {
        return CompletableFuture.runAsync(() -> {
            try {
                offer(key, rank, fetcher.call());
            } catch (Exception error) {
                failed.set(true);
                Raven.LOGGER.debug("Unable to load timed lyrics from " + name, error);
            }
        }, Raven.INSTANCE.executor());
    }

    private synchronized void offer(String key, int rank, List<Line> lines) {
        if (lines.isEmpty()) return;
        int score = rank * 10 + quality(lines);
        if (this.partialScore.getOrDefault(key, -1) >= score) return;
        this.partialScore.put(key, score);
        this.partial.put(key, lines);
        if (key.equals(this.wantedKey)) {
            this.active = new ActiveLyrics(key, lines);
        }
    }

    private void finish(String key, List<Line> lines) {
        this.cache.put(key, lines);
        this.inFlight.remove(key);
        if (key.equals(this.wantedKey)) {
            this.active = new ActiveLyrics(key, lines);
        }
    }

    private static int quality(List<Line> lines) {
        return lines.stream().anyMatch(line -> !line.words().isEmpty()) ? 2 : 1;
    }

    private Path diskPath(String key) {
        return GamePaths.configDirectory().resolve("lyrics")
                .resolve(UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)) + ".json");
    }

    private List<Line> readDisk(String key) {
        Path path = diskPath(key);
        if (!Files.isRegularFile(path)) return null;
        try {
            Line[] lines = this.gson.fromJson(Files.readString(path), Line[].class);
            return lines == null || lines.length == 0 ? null : List.of(lines);
        } catch (Exception error) {
            Raven.LOGGER.debug("Unable to read cached lyrics", error);
            return null;
        }
    }

    private void writeDisk(String key, List<Line> lines) {
        try {
            Path path = diskPath(key);
            Files.createDirectories(path.getParent());
            Files.writeString(path, this.gson.toJson(lines));
        } catch (Exception error) {
            Raven.LOGGER.debug("Unable to cache lyrics", error);
        }
    }

    private static String cleanTitle(String title) {
        String cleaned = TITLE_NOISE.matcher(title).replaceAll("").strip();
        return cleaned.isEmpty() ? title : cleaned;
    }

    public String trackKey(MediaPlaybackService.Snapshot snapshot) {
        return snapshot.artist().strip().toLowerCase(Locale.ROOT) + "\n"
                + snapshot.title().strip().toLowerCase(Locale.ROOT) + "\n" + snapshot.duration();
    }

    public Cue cueAt(MediaPlaybackService.Snapshot snapshot, long now) {
        ActiveLyrics lyrics = this.active;
        String key = trackKey(snapshot);
        if (!key.equals(lyrics.key()) || lyrics.lines().isEmpty()) return null;
        double position = snapshot.interpolatedPosition(now);
        int index = -1;
        List<Line> lines = lyrics.lines();
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).second() > position) break;
            index = i;
        }
        if (index < 0) return null;
        Line line = lines.get(index);
        double end = line.end() > line.second() ? line.end()
                : index + 1 < lines.size() ? lines.get(index + 1).second()
                : snapshot.duration() > 0L ? Math.min(snapshot.duration(), line.second() + 6.0)
                : line.second() + 6.0;
        if (position > end + 4.0) return null;
        return new Cue(line.text(), line.second(), end, position, line.words());
    }

    private static List<String> artistQueries(String artist) {
        String primary = ARTIST_SEPARATOR.split(artist.strip(), 2)[0].strip();
        return primary.isEmpty() || primary.equalsIgnoreCase(artist.strip())
                ? List.of(artist) : List.of(artist, primary);
    }

    private List<Line> fetchBetterLyrics(MediaPlaybackService.Snapshot snapshot) throws Exception {
        for (String artist : artistQueries(snapshot.artist())) {
            String uri = "https://api.betterlyrics.org/getLyrics?s=" + encode(cleanTitle(snapshot.title()))
                    + "&a=" + encode(artist)
                    + (snapshot.duration() > 0L ? "&d=" + snapshot.duration() : "");
            JsonElement response = request(uri);
            if (response == null || !response.isJsonObject()) continue;
            String ttml = string(response.getAsJsonObject(), "ttml");
            List<Line> lines = ttml.isBlank() ? List.of() : parseTtml(ttml, snapshot.duration());
            if (!lines.isEmpty()) return lines;
        }
        return List.of();
    }

    private static List<Line> parseTtml(String ttml, long expectedDuration) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        Document document = factory.newDocumentBuilder()
                .parse(new InputSource(new StringReader(ttml)));
        Element body = (Element) document.getElementsByTagName("body").item(0);
        if (body == null) return List.of();
        double total = ttmlTime(body.getAttribute("dur"));
        if (expectedDuration > 0L && Double.isFinite(total) && total > expectedDuration + 5.0) {
            return List.of();
        }
        NodeList paragraphs = document.getElementsByTagName("p");
        List<Line> lines = new ArrayList<>();
        for (int i = 0; i < paragraphs.getLength(); i++) {
            Element paragraph = (Element) paragraphs.item(i);
            double start = ttmlTime(paragraph.getAttribute("begin"));
            double end = ttmlTime(paragraph.getAttribute("end"));
            if (!Double.isFinite(start)) continue;
            StringBuilder text = new StringBuilder();
            List<WordDraft> drafts = new ArrayList<>();
            NodeList children = paragraph.getChildNodes();
            for (int c = 0; c < children.getLength(); c++) {
                Node child = children.item(c);
                if (child.getNodeType() == Node.TEXT_NODE) {
                    text.append(text.isEmpty() ? child.getTextContent().stripLeading() : child.getTextContent());
                } else if (child instanceof Element span && !span.getAttribute("ttm:role").startsWith("x-")) {
                    int from = text.length();
                    text.append(span.getTextContent());
                    double wordStart = ttmlTime(span.getAttribute("begin"));
                    if (Double.isFinite(wordStart)) {
                        drafts.add(new WordDraft(from, text.length(), wordStart, ttmlTime(span.getAttribute("end"))));
                    }
                }
            }
            String lineText = text.toString().stripTrailing();
            if (lineText.isBlank()) continue;
            lines.add(new Line(start, Double.isFinite(end) ? end : 0.0, lineText,
                    drafts.isEmpty() ? List.of() : toWords(drafts, lineText, end)));
        }
        lines.sort(Comparator.comparingDouble(Line::second));
        return List.copyOf(lines);
    }

    private static double ttmlTime(String value) {
        if (value == null || value.isBlank()) return Double.NaN;
        try {
            double seconds = 0.0;
            for (String part : value.strip().split(":")) {
                seconds = seconds * 60.0 + Double.parseDouble(part);
            }
            return seconds;
        } catch (NumberFormatException error) {
            return Double.NaN;
        }
    }

    private List<Line> fetchLrclib(MediaPlaybackService.Snapshot snapshot) throws Exception {
        List<String> artists = artistQueries(snapshot.artist());
        for (String artist : artists) {
            String uri = "https://lrclib.net/api/get?track_name=" + encode(snapshot.title())
                    + "&artist_name=" + encode(artist)
                    + (snapshot.duration() > 0L ? "&duration=" + snapshot.duration() : "");
            JsonElement exact = request(uri);
            if (exact != null && exact.isJsonObject() && matches(exact.getAsJsonObject(), snapshot)) {
                List<Line> lines = parseLines(exact.getAsJsonObject());
                if (!lines.isEmpty()) return lines;
            }
        }
        String searchTitle = cleanTitle(snapshot.title());
        JsonObject best = null;
        for (String artist : artists) {
            if (best != null) break;
            best = search("https://lrclib.net/api/search?track_name=" + encode(searchTitle)
                    + "&artist_name=" + encode(artist), snapshot);
        }
        if (best == null) {
            best = search("https://lrclib.net/api/search?track_name=" + encode(searchTitle), snapshot);
        }
        if (best == null && !romanize(searchTitle).equalsIgnoreCase(searchTitle)) {
            best = search("https://lrclib.net/api/search?track_name="
                    + encode(romanize(searchTitle)), snapshot);
        }
        return best == null ? List.of() : parseLines(best);
    }

    private List<Line> fetchLrcRed(MediaPlaybackService.Snapshot snapshot) throws Exception {
        String title = cleanTitle(snapshot.title());
        List<String> artists = artistQueries(snapshot.artist());
        List<JsonObject> candidates = searchLrcRed(title + " " + artists.getLast(), snapshot);
        if (candidates.isEmpty()) candidates = searchLrcRed(title, snapshot);
        candidates.sort(Comparator.comparingDouble(hit -> snapshot.duration() > 0L
                ? Math.abs(number(hit, "duration") - snapshot.duration()) : 0.0));
        for (int index = 0; index < Math.min(candidates.size(), 5); index++) {
            String isrc = string(candidates.get(index), "isrc");
            if (!ISRC.matcher(isrc).matches()) continue;
            String base = "https://lrc.red/s/" + isrc;
            JsonElement metadata = request(base + ".json");
            if (metadata == null || !metadata.isJsonObject()) continue;
            JsonObject song = metadata.getAsJsonObject();
            if (!matchesMetadata(song, "title", "artist", snapshot)) continue;
            String sync = string(song, "sync");
            if (!sync.equals("line") && !sync.equals("word")) continue;
            String lyricsfile = requestBody(base + ".lyricsfile.yaml");
            if (lyricsfile != null) {
                try {
                    List<Line> lines = parseLyricsfile(lyricsfile);
                    if (!lines.isEmpty()) return lines;
                } catch (RuntimeException error) {
                    Raven.LOGGER.debug("Unable to parse lrc.red lyricsfile", error);
                }
            }
            String lrc = requestBody(base + ".lrc");
            if (lrc != null) {
                List<Line> lines = parseTimedLines(lrc);
                if (!lines.isEmpty()) return lines;
            }
        }
        return List.of();
    }

    private List<JsonObject> searchLrcRed(String query, MediaPlaybackService.Snapshot snapshot) throws Exception {
        JsonElement response = request("https://lrc.red/search.json?q=" + encode(query));
        if (response == null || !response.isJsonObject()) return new ArrayList<>();
        JsonObject search = response.getAsJsonObject();
        if (!search.has("hits") || !search.get("hits").isJsonArray()) return new ArrayList<>();
        List<JsonObject> candidates = new ArrayList<>();
        for (JsonElement element : search.getAsJsonArray("hits")) {
            if (!element.isJsonObject()) continue;
            JsonObject hit = element.getAsJsonObject();
            if (matchesMetadata(hit, "title", "artist", snapshot)) candidates.add(hit);
        }
        return candidates;
    }

    private List<Line> fetchLrcMux(MediaPlaybackService.Snapshot snapshot) throws Exception {
        for (String artist : artistQueries(snapshot.artist())) {
            List<Line> lines = fetchLrcMux(snapshot, artist);
            if (!lines.isEmpty()) return lines;
        }
        return List.of();
    }

    private List<Line> fetchLrcMux(MediaPlaybackService.Snapshot snapshot, String artist) throws Exception {
        String uri = "https://api.lrcmux.dev/get?artist=" + encode(artist)
                + "&title=" + encode(cleanTitle(snapshot.title()))
                + (snapshot.duration() > 0L ? "&duration=" + snapshot.duration() : "")
                + "&sources=" + encode(LRCMUX_SOURCES);
        JsonElement response = request(uri);
        if (response == null || !response.isJsonObject()) return List.of();
        JsonObject result = response.getAsJsonObject();
        if (!result.has("track") || !result.get("track").isJsonObject()
                || !result.has("meta") || !result.get("meta").isJsonObject()
                || !result.has("lines") || !result.get("lines").isJsonArray()) return List.of();
        JsonObject track = result.getAsJsonObject("track");
        JsonObject meta = result.getAsJsonObject("meta");
        String source = meta.has("source") && meta.get("source").isJsonObject()
                ? string(meta.getAsJsonObject("source"), "id") : "";
        if (!matchesMetadata(track, "title", "artist", snapshot)
                || !List.of("kugou", "musixmatch", "netease", "ytmusic").contains(source)) return List.of();
        String level = string(meta, "level");
        if (!level.equals("line") && !level.equals("word")) return List.of();
        List<Line> lines = new ArrayList<>();
        for (JsonElement element : result.getAsJsonArray("lines")) {
            if (!element.isJsonObject()) continue;
            JsonObject entry = element.getAsJsonObject();
            String lineText = string(entry, "text");
            double start = number(entry, "start") / 1000.0;
            double end = number(entry, "end") / 1000.0;
            if (lineText.isBlank() || !Double.isFinite(start) || start < 0.0) continue;
            List<Word> words = entry.has("words") && entry.get("words").isJsonArray()
                    ? parseMuxWords(entry.getAsJsonArray("words"), lineText, end) : List.of();
            lines.add(new Line(start, end, lineText, words));
        }
        lines.sort(Comparator.comparingDouble(Line::second));
        return List.copyOf(lines);
    }

    private static List<Word> parseMuxWords(JsonArray entries, String lineText, double lineEnd) {
        if (entries.isEmpty()) return List.of();
        List<WordDraft> drafts = new ArrayList<>();
        StringBuilder combined = new StringBuilder();
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) return List.of();
            JsonObject entry = element.getAsJsonObject();
            String text = string(entry, "text");
            double start = number(entry, "start") / 1000.0;
            if (!Double.isFinite(start) || text.isEmpty()) return List.of();
            int from = combined.length();
            combined.append(text);
            drafts.add(new WordDraft(from, combined.length(), start, number(entry, "end") / 1000.0));
        }
        if (!combined.toString().equals(lineText)) return List.of();
        return toWords(drafts, lineText, lineEnd);
    }

    private static boolean matchesMetadata(JsonObject result, String titleField, String artistField,
                                           MediaPlaybackService.Snapshot snapshot) {
        String title = string(result, titleField);
        String artist = string(result, artistField);
        double duration = number(result, "duration");
        return !title.isBlank() && !artist.isBlank()
                && (comparable(title).equals(comparable(snapshot.title()))
                || comparable(cleanTitle(title)).equals(comparable(cleanTitle(snapshot.title()))))
                && artistOverlap(artist, snapshot.artist()) > 0
                && (snapshot.duration() <= 0L || Double.isFinite(duration)
                && Math.abs(duration - snapshot.duration()) <= 5.0);
    }

    private static String string(JsonObject object, String field) {
        return object.has(field) && object.get(field).isJsonPrimitive()
                ? object.get(field).getAsString() : "";
    }

    private static double number(JsonObject object, String field) {
        if (!object.has(field) || !object.get(field).isJsonPrimitive()) return Double.NaN;
        try {
            return object.get(field).getAsDouble();
        } catch (NumberFormatException error) {
            return Double.NaN;
        }
    }

    private JsonElement request(String uri) throws Exception {
        String body = requestBody(uri);
        return body == null ? null : JsonParser.parseString(body);
    }

    private String requestBody(String uri) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                .timeout(Duration.ofSeconds(5))
                .header("User-Agent", "RavenVisuals/1.0 (media lyrics display)")
                .GET().build();
        HttpResponse<String> response = this.client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        for (int attempt = 0; attempt < 2 && (response.statusCode() == 503 || response.statusCode() == 429); attempt++) {
            Thread.sleep(600L * (attempt + 1));
            response = this.client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }
        if (response.statusCode() == 200) return response.body();
        if (response.statusCode() == 204 || response.statusCode() == 401 || response.statusCode() == 404) return null;
        throw new IOException(request.uri().getHost() + " responded with HTTP " + response.statusCode());
    }

    private JsonObject search(String uri, MediaPlaybackService.Snapshot snapshot) throws Exception {
        JsonElement response = request(uri);
        if (response == null || !response.isJsonArray()) return null;
        JsonArray results = response.getAsJsonArray();
        JsonObject best = null;
        int bestScore = Integer.MIN_VALUE;
        for (JsonElement element : results) {
            if (!element.isJsonObject()) continue;
            JsonObject candidate = element.getAsJsonObject();
            if (!matches(candidate, snapshot)) continue;
            int score = (snapshot.duration() > 0L ? -(int) Math.round(Math.abs(candidate.get("duration").getAsDouble()
                    - snapshot.duration()) * 100.0) : 0)
                    + artistOverlap(candidate.get("artistName").getAsString(), snapshot.artist()) * 10
                    + (hasWordTiming(candidate) ? 5 : 0);
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private static boolean matches(JsonObject result, MediaPlaybackService.Snapshot snapshot) {
        if (!result.has("trackName") || !result.has("artistName") || !result.has("duration")
                || !hasTimedLyrics(result)) return false;
        return matchesMetadata(result, "trackName", "artistName", snapshot);
    }

    private static int artistOverlap(String candidate, String requested) {
        String left = romanize(candidate).toLowerCase(Locale.ROOT);
        String right = romanize(requested).toLowerCase(Locale.ROOT);
        List<String> candidateWords = java.util.Arrays.asList(left.split("[^a-z0-9]+"));
        int matches = 0;
        for (String word : right.split("[^a-z0-9]+")) {
            if (word.length() < 2 || word.equals("feat") || word.equals("ft") || word.equals("and")
                    || word.equals("with")) continue;
            if (candidateWords.contains(word)) matches++;
        }
        return matches;
    }

    private static boolean hasTimedLyrics(JsonObject result) {
        return result.has("syncedLyrics") && !result.get("syncedLyrics").isJsonNull()
                && !result.get("syncedLyrics").getAsString().isBlank()
                || result.has("lyricsfile") && !result.get("lyricsfile").isJsonNull()
                && result.get("lyricsfile").getAsString().contains("start_ms:");
    }

    private static boolean hasWordTiming(JsonObject result) {
        return result.has("lyricsfile") && !result.get("lyricsfile").isJsonNull()
                && result.get("lyricsfile").getAsString().contains("words:");
    }

    private static String comparable(String value) {
        return romanize(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static String romanize(String value) {
        String from = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя";
        String[] to = {"a", "b", "v", "g", "d", "e", "e", "zh", "z", "i", "i", "k", "l", "m", "n", "o",
                "p", "r", "s", "t", "u", "f", "h", "ts", "ch", "sh", "shch", "", "y", "", "e", "yu", "ya"};
        StringBuilder result = new StringBuilder(value.length());
        for (char character : value.toLowerCase(Locale.ROOT).toCharArray()) {
            int index = from.indexOf(character);
            result.append(index < 0 ? character : to[index]);
        }
        return result.toString();
    }

    private static List<Line> parseLines(JsonObject result) {
        if (result.has("lyricsfile") && !result.get("lyricsfile").isJsonNull()) {
            try {
                List<Line> lines = parseLyricsfile(result.get("lyricsfile").getAsString());
                if (!lines.isEmpty()) return lines;
            } catch (RuntimeException error) {
                Raven.LOGGER.debug("Unable to parse word-timed lyrics", error);
            }
        }
        if (!result.has("syncedLyrics") || result.get("syncedLyrics").isJsonNull()) return List.of();
        return parseTimedLines(result.get("syncedLyrics").getAsString());
    }

    private static List<Line> parseTimedLines(String source) {
        List<Line> lines = new ArrayList<>();
        for (String raw : source.split("\\R")) {
            Matcher match = TIMED_LINE.matcher(raw);
            if (!match.matches()) continue;
            double fraction = match.group(3) == null ? 0.0
                    : Double.parseDouble("0." + match.group(3));
            double second = Integer.parseInt(match.group(1)) * 60.0
                    + Integer.parseInt(match.group(2)) + fraction;
            String text = match.group(4).trim();
            if (!text.isBlank()) lines.add(new Line(second, 0.0, text, List.of()));
        }
        return List.copyOf(lines);
    }

    private static List<Line> parseLyricsfile(String source) {
        LoaderOptions options = new LoaderOptions();
        options.setCodePointLimit(1_000_000);
        Object document = new Yaml(new SafeConstructor(options)).load(source);
        if (!(document instanceof Map<?, ?> root) || !(root.get("lines") instanceof List<?> entries)) {
            return List.of();
        }
        List<Line> lines = new ArrayList<>();
        for (Object entry : entries) {
            if (!(entry instanceof Map<?, ?> line)) continue;
            Object textValue = line.get("text");
            double start = seconds(line.get("start_ms"));
            double end = seconds(line.get("end_ms"));
            if (!(textValue instanceof String text) || text.isBlank() || !Double.isFinite(start)) continue;
            lines.add(new Line(start, end, text, parseWords(line.get("words"), text, end)));
        }
        lines.sort(Comparator.comparingDouble(Line::second));
        return List.copyOf(lines);
    }

    private static List<Word> parseWords(Object value, String lineText, double lineEnd) {
        if (!(value instanceof List<?> entries) || entries.isEmpty()) return List.of();
        List<WordDraft> drafts = new ArrayList<>();
        StringBuilder combined = new StringBuilder();
        for (Object entry : entries) {
            if (!(entry instanceof Map<?, ?> word) || !(word.get("text") instanceof String text)) {
                return List.of();
            }
            double start = seconds(word.get("start_ms"));
            if (!Double.isFinite(start)) return List.of();
            int from = combined.length();
            combined.append(text);
            drafts.add(new WordDraft(from, combined.length(), start, seconds(word.get("end_ms"))));
        }
        if (!combined.toString().equals(lineText)) return List.of();
        return toWords(drafts, lineText, lineEnd);
    }

    private static List<Word> toWords(List<WordDraft> drafts, String lineText, double lineEnd) {
        List<Word> words = new ArrayList<>();
        for (int index = 0; index < drafts.size(); index++) {
            WordDraft draft = drafts.get(index);
            double end = Double.isFinite(draft.end()) ? draft.end()
                    : index + 1 < drafts.size() ? drafts.get(index + 1).start() : lineEnd;
            if (!Double.isFinite(end) || end <= draft.start()) return List.of();
            int from = draft.from();
            int to = draft.to();
            while (from < to && Character.isWhitespace(lineText.charAt(from))) from++;
            while (to > from && Character.isWhitespace(lineText.charAt(to - 1))) to--;
            if (from < to) words.add(new Word(from, to, draft.start(), end));
        }
        return List.copyOf(words);
    }

    private static double seconds(Object value) {
        return value instanceof Number number ? number.doubleValue() / 1000.0 : Double.NaN;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record Line(double second, double end, String text, List<Word> words) {
    }

    private record WordDraft(int from, int to, double start, double end) {
    }

    private record ActiveLyrics(String key, List<Line> lines) {
    }

    public record Word(int from, int to, double start, double end) {
    }

    public record Cue(String text, double start, double end, double position, List<Word> words) {
    }
}
