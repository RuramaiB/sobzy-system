package com.example.sobzybackend.service;

import com.example.sobzybackend.dtos.ClassificationRequest;
import com.example.sobzybackend.dtos.ClassificationResult;
import com.example.sobzybackend.repository.BlockedSiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import smile.classification.RandomForest;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.vector.IntVector;
import jakarta.annotation.PostConstruct;
import java.io.*;

@Service
public class ClassificationService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ClassificationService.class);

    private RandomForest model;
    private final FeatureExtractionService featureExtractionService;
    private final BlockedSiteRepository blockedSiteRepository; // Added field
    private final String modelPath = "models/rf_model.ser";

    private static final java.util.Set<String> WHITELISTED_DOMAINS = java.util.Set.of(
            "google.com", "bing.com", "duckduckgo.com", "yahoo.com", "google.co.zw",
            "wikipedia.org", "britannica.com", "coursera.org", "udemy.com", "edx.org",
            "stackoverflow.com", "github.com", "gitlab.com", "bitbucket.org",
            "microsoft.com", "apple.com", "adobe.com", "npmmirror.com", "npmjs.com",
            "maven.org", "apache.org", "spring.io", "oracle.com", "java.com",
            "econet.co.zw", "netone.co.zw", "telecel.co.zw", "telone.co.zw",
            "hit.ac.zw", "uz.ac.zw", "msu.ac.zw", "nust.ac.zw", "cut.ac.zw", "zambeze.ac.zw",
            "gstatic.com", "googleapis.com", "googleusercontent.com", "ggpht.com",
            "youtube.com", "ytimg.com", "googlevideo.com", "android.com",
            "play.google.com", "chatgpt.com", "openai.com", "anthropic.com",
            "khanacademy.org", "unicef.org", "unesco.org", "phet.colorado.edu", "jstor.org"
    );

    // ─────────────────────────────────────────────────────────────────────────────
    // AI DOMAIN INTELLIGENCE: keyword token libraries with weights per category.
    // These tokens are scored against the domain name to produce a category score.
    // The approach classifies ANY domain — not just those on a hardcoded list.
    // ─────────────────────────────────────────────────────────────────────────────

    // Adult content signals — high weight for unambiguous terms, moderate for ambiguous
    private static final java.util.Map<String, Double> ADULT_SIGNALS = java.util.Map.ofEntries(
            java.util.Map.entry("porn", 1.0), java.util.Map.entry("xxx", 1.0), java.util.Map.entry("sex", 0.85),
            java.util.Map.entry("nude", 0.9), java.util.Map.entry("naked", 0.9), java.util.Map.entry("adult", 0.7),
            java.util.Map.entry("erotic", 0.95), java.util.Map.entry("hentai", 1.0), java.util.Map.entry("nsfw", 1.0),
            java.util.Map.entry("onlyfans", 1.0), java.util.Map.entry("xvideo", 1.0), java.util.Map.entry("xnxx", 1.0),
            java.util.Map.entry("redtube", 1.0), java.util.Map.entry("pornhub", 1.0), java.util.Map.entry("spankbang", 1.0),
            java.util.Map.entry("milf", 0.95), java.util.Map.entry("escort", 0.8), java.util.Map.entry("fetish", 0.9),
            java.util.Map.entry("camgirl", 1.0), java.util.Map.entry("webcam18", 1.0), java.util.Map.entry("live18", 0.95),
            java.util.Map.entry("hotgirl", 0.9), java.util.Map.entry("chaturbate", 1.0), java.util.Map.entry("brazzers", 1.0)
    );

    // Adult content TLDs
    private static final java.util.Set<String> ADULT_TLDS = java.util.Set.of(".xxx", ".sex", ".porn", ".adult", ".sexy");

    // Gambling signals
    private static final java.util.Map<String, Double> GAMBLING_SIGNALS = java.util.Map.ofEntries(
            java.util.Map.entry("casino", 1.0), java.util.Map.entry("poker", 0.95), java.util.Map.entry("betway", 1.0),
            java.util.Map.entry("bet365", 1.0), java.util.Map.entry("gambling", 1.0), java.util.Map.entry("jackpot", 0.9),
            java.util.Map.entry("slots", 0.85), java.util.Map.entry("roulette", 0.95), java.util.Map.entry("baccarat", 0.95),
            java.util.Map.entry("sporty", 0.8), java.util.Map.entry("1xbet", 1.0), java.util.Map.entry("22bet", 1.0),
            java.util.Map.entry("hollywoodbets", 1.0), java.util.Map.entry("supabets", 1.0), java.util.Map.entry("betika", 1.0),
            java.util.Map.entry("pokerstars", 1.0), java.util.Map.entry("wager", 0.85), java.util.Map.entry("lottery", 0.7),
            java.util.Map.entry("sportsbet", 0.95), java.util.Map.entry("melbet", 1.0), java.util.Map.entry("betwinner", 1.0),
            java.util.Map.entry("unibet", 1.0), java.util.Map.entry("betmgm", 1.0), java.util.Map.entry("draftkings", 1.0),
            java.util.Map.entry("fanduel", 0.9), java.util.Map.entry("punt", 0.7), java.util.Map.entry("stake", 0.65)
    );

    // Torrent/piracy signals
    private static final java.util.Map<String, Double> TORRENT_SIGNALS = java.util.Map.ofEntries(
            java.util.Map.entry("torrent", 1.0), java.util.Map.entry("piratebay", 1.0), java.util.Map.entry("pirate", 0.75),
            java.util.Map.entry("1337x", 1.0), java.util.Map.entry("rarbg", 1.0), java.util.Map.entry("nyaa", 0.85),
            java.util.Map.entry("warez", 1.0), java.util.Map.entry("cracked", 0.8), java.util.Map.entry("kickass", 0.9),
            java.util.Map.entry("eztv", 1.0), java.util.Map.entry("yts", 0.85), java.util.Map.entry("zooqle", 1.0),
            java.util.Map.entry("magnetdl", 1.0), java.util.Map.entry("limetorrent", 1.0), java.util.Map.entry("torrentfunk", 1.0),
            java.util.Map.entry("rutracker", 1.0), java.util.Map.entry("seedr", 0.75), java.util.Map.entry("debrid", 0.7),
            java.util.Map.entry("putlocker", 0.85), java.util.Map.entry("fmovies", 0.8), java.util.Map.entry("123movies", 0.9),
            java.util.Map.entry("cuevana", 0.85), java.util.Map.entry("solarmovie", 0.85), java.util.Map.entry("movierulz", 0.9)
    );

    // Gaming signals — weighted carefully to avoid catching "gameplay" on youtube.com etc.
    private static final java.util.Map<String, Double> GAMING_SIGNALS = java.util.Map.ofEntries(
            java.util.Map.entry("roblox", 1.0), java.util.Map.entry("steampowered", 1.0), java.util.Map.entry("epicgames", 1.0),
            java.util.Map.entry("battle.net", 1.0), java.util.Map.entry("blizzard", 0.9), java.util.Map.entry("miniclip", 1.0),
            java.util.Map.entry("poki", 0.95), java.util.Map.entry("friv", 1.0), java.util.Map.entry("y8", 0.85),
            java.util.Map.entry("crazygames", 1.0), java.util.Map.entry("kongregate", 1.0), java.util.Map.entry("newgrounds", 0.9),
            java.util.Map.entry("gamedistribution", 1.0), java.util.Map.entry("silvergames", 1.0), java.util.Map.entry("agame", 0.9),
            java.util.Map.entry("addictinggames", 1.0), java.util.Map.entry("coolmathgames", 1.0), java.util.Map.entry("girlsgogames", 1.0),
            java.util.Map.entry("kizi", 0.95), java.util.Map.entry("armor games", 1.0), java.util.Map.entry("unblocked games", 1.0),
            java.util.Map.entry("unblockedgames", 1.0), java.util.Map.entry("gameflare", 1.0), java.util.Map.entry("gamesjolt", 0.9)
    );

    // Classification confidence threshold — must score above this to block
    private static final double BLOCK_THRESHOLD = 0.55;

    /**
     * Scores a domain against a signal map. Returns a score 0.0 – 1.0.
     * Score = max(individual signal weights) when any token is found.
     * Multiple signals compound the score (capped at 1.0).
     */
    private double scoreAgainstSignals(String domain, java.util.Map<String, Double> signals) {
        double score = 0.0;
        for (java.util.Map.Entry<String, Double> entry : signals.entrySet()) {
            if (domain.contains(entry.getKey())) {
                // Compound scoring: first hit gives full weight, subsequent hits add diminishing returns
                score = Math.min(1.0, score + entry.getValue() * (1.0 - score * 0.5));
            }
        }
        return score;
    }

    /**
     * AI Domain Intelligence Classifier.
     * Classifies any domain into a category using multi-signal linguistic analysis.
     * No static blocklist needed — works on novel/unlisted domains.
     */
    private PredictionResult classifyByDomainIntelligence(String domain) {
        // TLD-based hard rules (highest confidence)
        for (String tld : ADULT_TLDS) {
            if (domain.endsWith(tld)) {
                return new PredictionResult("ADULT_CONTENT", 1.0);
            }
        }

        // Score each category
        double adultScore    = scoreAgainstSignals(domain, ADULT_SIGNALS);
        double gamblingScore = scoreAgainstSignals(domain, GAMBLING_SIGNALS);
        double torrentScore  = scoreAgainstSignals(domain, TORRENT_SIGNALS);
        double gamingScore   = scoreAgainstSignals(domain, GAMING_SIGNALS);

        log.debug("Domain AI Scores [{}]: adult={:.2f}, gambling={:.2f}, torrent={:.2f}, gaming={:.2f}",
                domain, adultScore, gamblingScore, torrentScore, gamingScore);

        // Pick the highest-scoring category
        double maxScore = Math.max(Math.max(adultScore, gamblingScore), Math.max(torrentScore, gamingScore));

        if (maxScore < BLOCK_THRESHOLD) {
            return null; // below threshold → no block signal
        }

        if (maxScore == adultScore)    return new PredictionResult("ADULT_CONTENT", adultScore);
        if (maxScore == gamblingScore) return new PredictionResult("GAMBLING", gamblingScore);
        if (maxScore == torrentScore)  return new PredictionResult("TORRENT", torrentScore);
        return new PredictionResult("GAMING", gamingScore);
    }

    @Autowired
    public ClassificationService(FeatureExtractionService featureExtractionService, 
                                 com.example.sobzybackend.repository.BlockedSiteRepository blockedSiteRepository) {
        this.featureExtractionService = featureExtractionService;
        this.blockedSiteRepository = blockedSiteRepository;
    }

    @PostConstruct
    public void init() {
        loadModel();
        if (model == null) {
            log.info("No pre-trained model found. Initializing skeleton model for training placeholder.");
            trainDummyModel();
        }
    }

    private static final String[] X_NAMES = { "x1", "x2", "x3", "x4", "x5", "x6", "x7", "x8", "x9", "x10" };

    public PredictionResult predict(String url) {
        String lowerUrl = url.toLowerCase();
        String domain = extractDomain(lowerUrl);

        // 1. Whitelist Check — always allow education, productivity, search engines
        if (WHITELISTED_DOMAINS.stream().anyMatch(d -> domain.equals(d) || domain.endsWith("." + d))) {
            return new PredictionResult("BENIGN", 1.0);
        }

        // 2. Database Blocklist Check (User-defined rules)
        try {
            // Check exact domain and subdomains
            java.util.List<com.example.sobzybackend.models.BlockedSite> activeBlocked = blockedSiteRepository.findByActive(true);
            for (com.example.sobzybackend.models.BlockedSite site : activeBlocked) {
                String blockedUrl = site.getUrl().toLowerCase();
                if (domain.equals(blockedUrl) || domain.endsWith("." + blockedUrl)) {
                    log.info("DB BLOCK: {} matched user rule [{}]", domain, blockedUrl);
                    return new PredictionResult("USER_BLOCKED", 1.0);
                }
            }
        } catch (Exception e) {
            log.error("Failed to check DB blocklist: {}", e.getMessage());
        }

        // 2. AI Domain Intelligence Scorer
        // Analyzes the domain's linguistic features, semantic signals, and TLD
        // to classify ANY site — including novel/unlisted ones — into a blocked category.
        PredictionResult aiResult = classifyByDomainIntelligence(domain);
        if (aiResult != null) {
            log.info("AI CLASSIFIED [{}] as {} (confidence={:.2f})", domain, aiResult.category(), aiResult.confidence());
            return aiResult;
        }

        // 3. Education signals — allow if clearly educational
        if (lowerUrl.contains("edu") || lowerUrl.contains("school") || lowerUrl.contains("academy")
                || lowerUrl.contains("learn") || lowerUrl.contains("study") || lowerUrl.contains("research")
                || lowerUrl.contains("scholar") || lowerUrl.contains("ac.zw") || lowerUrl.contains("university")
                || lowerUrl.contains("library") || lowerUrl.contains("archive.org")) {
            return new PredictionResult("EDUCATION", 0.95);
        }

        // 4. ML Model fallback (for traffic pattern analysis)
        double[] features = featureExtractionService.extractUrlFeatures(url);
        if (model == null) {
            return new PredictionResult("BENIGN", 0.5);
        }

        DataFrame df = DataFrame.of(new double[][] { features }, X_NAMES);
        df = df.merge(IntVector.of("y", new int[] { 0 }));
        int prediction = model.predict(df.get(0));

        String category = getCategoryName(prediction);
        // Only trust ML for blocked categories — default BENIGN for ambiguous cases
        boolean mlBlocked = "ADULT_CONTENT".equals(category) || "GAMBLING".equals(category)
                || "GAMING".equals(category) || "TORRENT".equals(category);
        return new PredictionResult(mlBlocked ? category : "BENIGN", 0.6);
    }

    public ClassificationResult classify(ClassificationRequest request) {
        PredictionResult prediction = predict(request.getUrl());

        // Block: adult content, gambling, gaming, torrents, and user-defined rules
        boolean isMalicious = "ADULT_CONTENT".equals(prediction.category()) ||
                "GAMBLING".equals(prediction.category()) ||
                "GAMING".equals(prediction.category()) ||
                "TORRENT".equals(prediction.category()) ||
                "USER_BLOCKED".equals(prediction.category());

        String reason = isMalicious
                ? "Blocked: site categorized as " + (prediction.category().equals("USER_BLOCKED") ? "Restricted" : prediction.category())
                : "Allowed";

        return ClassificationResult.builder()
                .url(request.getUrl())
                .domain(extractDomain(request.getUrl()))
                .category(prediction.category())
                .confidence(prediction.confidence())
                .isAllowed(!isMalicious)
                .decision(isMalicious ? "BLOCK" : "ALLOW")
                .reason(reason)
                .processingTimeMs(0L)
                .build();
    }

    private String extractDomain(String url) {
        try {
            if (url.contains("://")) {
                return new java.net.URL(url).getHost();
            }
            return url.split("/")[0];
        } catch (Exception e) {
            return "unknown";
        }
    }


    private void loadModel() {
        File file = new File(modelPath);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                model = (RandomForest) ois.readObject();
                log.info("Random Forest model loaded successfully from {}", modelPath);
            } catch (Exception e) {
                log.error("Failed to load ML model: {}", e.getMessage());
            }
        }
    }

    public synchronized void saveModel() {
        File directory = new File("models");
        if (!directory.exists())
            directory.mkdirs();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(modelPath))) {
            oos.writeObject(model);
            log.info("Random Forest model saved to {}", modelPath);
        } catch (IOException e) {
            log.error("Failed to save ML model: {}", e.getMessage());
        }
    }

    private void trainDummyModel() {
        double[][] x = new double[40][10];
        int[] y = new int[40];
        for (int i = 0; i < 40; i++) {
            int category = i / 10; // 4 categories, 10 samples each
            y[i] = category;
            for (int j = 0; j < 10; j++) {
                x[i][j] = (category + 1) * (i + 1.2) + j;
            }
        }

        DataFrame df = DataFrame.of(x, X_NAMES);
        df = df.merge(IntVector.of("y", y));

        model = RandomForest.fit(Formula.lhs("y"), df);
        log.info("More robust Random Forest dummy model trained (40 samples).");
    }

    public String getCategoryName(int index) {
        switch (index) {
            case 0:
                return "BENIGN";
            case 1:
                return "ADULT_CONTENT";
            case 2:
                return "RESEARCH";
            case 3:
                return "GAMING";
            case 4:
                return "SOCIAL_MEDIA";
            case 5:
                return "SECURITY_EVASION";
            case 6:
                return "MUSIC";
            case 7:
                return "GAMBLING";
            case 8:
                return "EDUCATION";
            default:
                return "OTHER";
        }
    }

    public boolean isWhitelisted(String host) {
        if (host == null || host.isEmpty()) return false;
        String lowerHost = host.toLowerCase();
        return WHITELISTED_DOMAINS.stream().anyMatch(d -> lowerHost.equals(d) || lowerHost.endsWith("." + d));
    }

    public static record PredictionResult(String category, double confidence) {
    }
}
