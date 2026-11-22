import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Modern dark-themed Swing front-end for AdamDAP (yt-dlp media downloader/converter).
 *
 * Design:
 * - Palette: Catppuccin Mocha-inspired deep dark (#181825, #1E1E2E, #252538)
 * - Accents: Vibrant purple (#A06CD5, #B482E8, #8A56BF)
 * - Geometry: Smooth rounded corners (12px radius)
 * - Typography: UDEV Gothic (bundled TTF or system) with CJK-capable fallbacks
 * - Usability: Quick paste/clear, format badges, live status pill, open in Explorer
 */
public class VideoDownloaderGUI extends JFrame {

    // ── Color Palette ──────────────────────────────────────────────────
    private static final Color BG_DARK        = new Color(0x18, 0x18, 0x25);  // window background
    private static final Color BG_CARD        = new Color(0x1E, 0x1E, 0x2E);  // card background
    private static final Color BG_SURFACE     = new Color(0x25, 0x25, 0x38);  // inner elevated containers
    private static final Color BG_INPUT       = new Color(0x2A, 0x2A, 0x3E);  // input fields
    private static final Color BG_HOVER       = new Color(0x35, 0x35, 0x4D);  // hover states
    private static final Color PURPLE_ACCENT  = new Color(0xA0, 0x6C, 0xD5);  // primary purple
    private static final Color PURPLE_HOVER   = new Color(0xB4, 0x82, 0xE8);  // accent hover
    private static final Color PURPLE_PRESSED = new Color(0x8A, 0x56, 0xBF);  // accent pressed
    private static final Color PURPLE_MUTED   = new Color(0x3B, 0x2A, 0x54);  // subtle accent fill / border
    private static final Color PURPLE_DIM     = new Color(0x52, 0x3B, 0x73);  // disabled accent
    private static final Color TEXT_PRIMARY   = new Color(0xF5, 0xF5, 0xFA);  // primary headings/text
    private static final Color TEXT_SECONDARY = new Color(0xAA, 0xAA, 0xC4);  // labels / metadata
    private static final Color TEXT_MUTED     = new Color(0x6E, 0x6E, 0x8A);  // placeholders / dim
    private static final Color BORDER_COLOR   = new Color(0x33, 0x33, 0x4A);  // standard borders
    private static final Color SUCCESS_COLOR  = new Color(0x70, 0xDF, 0x9B);  // green for success
    private static final Color WARNING_COLOR  = new Color(0xFA, 0xB3, 0x87);  // peach / warning
    private static final Color ERROR_COLOR    = new Color(0xF3, 0x8B, 0xA8);  // soft red for errors
    private static final Color LOG_BG         = new Color(0x13, 0x13, 0x1D);  // console background

    private static final int ARC = 12;

    // ── Fonts ──────────────────────────────────────────────────────────
    private static Font FONT_REGULAR;
    private static Font FONT_MEDIUM;
    private static Font FONT_BOLD;
    private static Font FONT_LIGHT;

    static {
        FONT_REGULAR = initJapaneseCapableFont(
                new String[]{
                        "fonts/UDEVGothic-Regular.ttf",
                        "fonts/UDEVGothicNF-Regular.ttf",
                        "fonts/UDEVGothicJPDOC-Regular.ttf",
                        "fonts/UDEVGothic35-Regular.ttf",
                        "fonts/UDEVGothic35NF-Regular.ttf"
                },
                Font.PLAIN, 13f
        );
        FONT_MEDIUM = initJapaneseCapableFont(
                new String[]{
                        "fonts/UDEVGothic-Medium.ttf",
                        "fonts/UDEVGothic-Regular.ttf",
                        "fonts/UDEVGothicNF-Regular.ttf",
                        "fonts/UDEVGothicJPDOC-Regular.ttf",
                        "fonts/UDEVGothic35-Regular.ttf"
                },
                Font.PLAIN, 13f
        );
        FONT_BOLD = initJapaneseCapableFont(
                new String[]{
                        "fonts/UDEVGothic-Bold.ttf",
                        "fonts/UDEVGothicNF-Bold.ttf",
                        "fonts/UDEVGothicJPDOC-Bold.ttf",
                        "fonts/UDEVGothic35-Bold.ttf",
                        "fonts/UDEVGothic35NF-Bold.ttf",
                        "fonts/UDEVGothic-Regular.ttf"
                },
                Font.BOLD, 13f
        );
        FONT_LIGHT = initJapaneseCapableFont(
                new String[]{
                        "fonts/UDEVGothic-Light.ttf",
                        "fonts/UDEVGothic-Regular.ttf",
                        "fonts/UDEVGothicNF-Regular.ttf",
                        "fonts/UDEVGothicJPDOC-Regular.ttf",
                        "fonts/UDEVGothic35-Regular.ttf"
                },
                Font.PLAIN, 13f
        );
    }

    /**
     * Loads UDEV Gothic (bundled TTF first, then installed system font variants),
     * falling back to CJK-capable system fonts and Java's logical composite font
     * so Japanese characters (Kanji, Hiragana, Katakana) render cleanly.
     */
    private static Font initJapaneseCapableFont(String[] bundledPaths, int style, float size) {
        // 1. Try bundled UDEV Gothic TTF files
        for (String path : bundledPaths) {
            Font loaded = loadBundledFont(path, style, size);
            if (loaded != null && loaded.canDisplay('あ') && loaded.canDisplay('漢')) {
                return loaded;
            }
        }

        // 2. Check installed system fonts (UDEV Gothic variants first, then OS CJK fonts)
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()
        ));

        String[] preferredFamilies = {
                "UDEV Gothic",
                "UDEV Gothic NF",
                "UDEV Gothic JPDOC",
                "UDEV Gothic 35",
                "UDEV Gothic 35NF",
                "UDEV Gothic 35JPDOC",
                "Yu Gothic UI",
                "Meiryo",
                "MS Gothic",
                "Noto Sans CJK JP",
                "Noto Sans JP",
                "Hiragino Kaku Gothic ProN",
                "Hiragino Sans",
                Font.DIALOG // Logical composite font with automatic OS CJK fallback
        };

        for (String family : preferredFamilies) {
            if (family.equals(Font.DIALOG) || available.contains(family)) {
                Font candidate = new Font(family, style, Math.round(size));
                if (candidate.canDisplay('あ') && candidate.canDisplay('漢')) {
                    return candidate.deriveFont(style, size);
                }
            }
        }

        return new Font(Font.DIALOG, style, Math.round(size)).deriveFont(style, size);
    }

    private static Font loadBundledFont(String resourcePath, int style, float size) {
        InputStream is = VideoDownloaderGUI.class.getResourceAsStream("/" + resourcePath);
        if (is == null) is = VideoDownloaderGUI.class.getResourceAsStream(resourcePath);
        if (is == null) {
            File f = new File(resourcePath);
            if (!f.exists()) f = new File("front", resourcePath);
            if (f.exists()) {
                try { is = new FileInputStream(f); } catch (FileNotFoundException ignored) {}
            }
        }
        if (is != null) {
            try {
                Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                return font.deriveFont(style, size);
            } catch (Exception ignored) {
            } finally {
                try { is.close(); } catch (IOException ignored) {}
            }
        }
        return null;
    }

    // ── Format Model ───────────────────────────────────────────────────
    public static class FormatOption {
        public final String ext;
        public final String label;
        public final boolean isAudio;

        public FormatOption(String ext, String label, boolean isAudio) {
            this.ext = ext;
            this.label = label;
            this.isAudio = isAudio;
        }

        @Override
        public String toString() {
            return (isAudio ? "🎵 " : "🎬 ") + label;
        }
    }

    private static final FormatOption[] FORMAT_OPTIONS = new FormatOption[]{
            new FormatOption("mp4",  "MP4  (Universal Video)", false),
            new FormatOption("webm", "WEBM (Web Quality)", false),
            new FormatOption("mkv",  "MKV  (Matroska)", false),
            new FormatOption("mov",  "MOV  (QuickTime)", false),
            new FormatOption("mp3",  "MP3  (Standard Audio)", true),
            new FormatOption("m4a",  "M4A  (AAC Audio)", true),
            new FormatOption("wav",  "WAV  (Lossless)", true),
            new FormatOption("opus", "OPUS (High Efficiency)", true),
            new FormatOption("aac",  "AAC  (Raw Stream)", true)
    };

    // ── UI Components ──────────────────────────────────────────────────
    private final JTextField urlField = createStyledTextField("Paste video or audio URL here...");
    private final JButton pasteButton = createSmallButton("📋 Paste");
    private final JButton clearUrlButton = createSmallButton("✕");
    private final JButton getInfoButton = createAccentButton("Get Info");

    private final JLabel statusPill = createStatusPill();

    private final JLabel titleLabel = createInfoValue("—");
    private final JLabel durationLabel = createInfoValue("—");
    private final JLabel uploaderLabel = createInfoValue("—");
    private final JLabel bitrateLabel = createInfoValue("—");

    private final JComboBox<FormatOption> formatBox = new JComboBox<>(FORMAT_OPTIONS);
    private final JButton downloadButton = createAccentButton("⬇ Download & Convert");
    private final JButton cancelButton = createSecondaryButton("✕ Cancel");
    private final JButton openFolderButton = createSecondaryButton("📂 Open Folder");

    private final JTextArea logArea = new JTextArea();
    private final JProgressBar progressBar = new JProgressBar();
    private final Timer progressAnimTimer;
    private int progressAnimOffset = 0;

    private final String pythonDir;
    private String pythonExecutable = "python";
    private File standaloneBackendExe = null;
    private volatile Process activeProcess = null;

    private String pendingTitle = "";
    private String pendingDuration = "";
    private String pendingUploader = "";
    private String pendingBitrate = "";
    private String lastSavedFile = null;

    // ═══════════════════════════════════════════════════════════════════
    // Construction
    // ═══════════════════════════════════════════════════════════════════

    public VideoDownloaderGUI(String pythonDir) {
        super("AdamDAP · Media Downloader & Converter");
        this.pythonDir = pythonDir;

        // Ensure tooltips also use UDEV Gothic
        UIManager.put("ToolTip.font", FONT_REGULAR.deriveFont(12f));
        UIManager.put("ToolTip.background", BG_SURFACE);
        UIManager.put("ToolTip.foreground", TEXT_PRIMARY);

        // Progress bar smooth animation timer
        this.progressAnimTimer = new Timer(30, e -> {
            if (progressBar.isIndeterminate()) {
                progressAnimOffset = (progressAnimOffset + 4) % Math.max(1, progressBar.getWidth() + 100);
                progressBar.repaint();
            }
        });

        applyAppIcon();
        buildUI();
        detectBackend();
    }

    private void applyAppIcon() {
        try {
            BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Purple rounded background
            g.setColor(PURPLE_ACCENT);
            g.fillRoundRect(2, 2, 28, 28, 8, 8);
            // Downward arrow
            g.setColor(Color.WHITE);
            Polygon arrow = new Polygon();
            arrow.addPoint(16, 23);
            arrow.addPoint(9, 14);
            arrow.addPoint(13, 14);
            arrow.addPoint(13, 8);
            arrow.addPoint(19, 8);
            arrow.addPoint(19, 14);
            arrow.addPoint(23, 14);
            g.fillPolygon(arrow);
            g.dispose();
            setIconImage(img);
        } catch (Exception ignored) {}
    }

    // ═══════════════════════════════════════════════════════════════════
    // UI Construction
    // ═══════════════════════════════════════════════════════════════════

    private void buildUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(780, 700);
        setMinimumSize(new Dimension(620, 560));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelActiveProcess();
            }
        });

        JPanel wrapper = darkPanel();
        wrapper.setLayout(new BorderLayout(0, 14));
        wrapper.setBorder(new EmptyBorder(18, 22, 18, 22));

        // ── Top: Title Bar & URL Input ─────────────────────────────────
        JPanel topSection = darkPanel();
        topSection.setLayout(new BorderLayout(0, 12));

        // Header with Logo, Subtitle, and Status Pill
        JPanel titleRow = darkPanel();
        titleRow.setLayout(new BorderLayout());

        JPanel titleGroup = darkPanel();
        titleGroup.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

        JLabel appTitle = new JLabel("AdamDAP");
        appTitle.setFont(FONT_BOLD.deriveFont(22f));
        appTitle.setForeground(PURPLE_ACCENT);

        JLabel appSubtitle = new JLabel("  Media Downloader & Converter");
        appSubtitle.setFont(FONT_LIGHT.deriveFont(12.5f));
        appSubtitle.setForeground(TEXT_MUTED);

        titleGroup.add(appTitle);
        titleGroup.add(appSubtitle);
        titleRow.add(titleGroup, BorderLayout.WEST);
        titleRow.add(statusPill, BorderLayout.EAST);
        topSection.add(titleRow, BorderLayout.NORTH);

        // URL input card with Paste, Clear, and Get Info
        JPanel urlCard = createCard(null);
        urlCard.setLayout(new BorderLayout(8, 0));

        JPanel urlFieldWrapper = new JPanel(new BorderLayout(4, 0));
        urlFieldWrapper.setOpaque(false);
        urlFieldWrapper.add(urlField, BorderLayout.CENTER);

        JPanel urlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        urlButtons.setOpaque(false);
        urlButtons.add(clearUrlButton);
        urlButtons.add(pasteButton);
        urlFieldWrapper.add(urlButtons, BorderLayout.EAST);

        urlCard.add(urlFieldWrapper, BorderLayout.CENTER);
        urlCard.add(getInfoButton, BorderLayout.EAST);
        topSection.add(urlCard, BorderLayout.SOUTH);

        wrapper.add(topSection, BorderLayout.NORTH);

        // ── Center Content ─────────────────────────────────────────────
        JPanel center = darkPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        // 1. Video Info Card
        JPanel infoCard = createCard("VIDEO INFORMATION");
        infoCard.setLayout(new GridLayout(2, 2, 16, 8));
        infoCard.add(createInfoRow("🎬 Title", titleLabel));
        infoCard.add(createInfoRow("⏱ Duration", durationLabel));
        infoCard.add(createInfoRow("👤 Uploader", uploaderLabel));
        infoCard.add(createInfoRow("📊 Bitrate", bitrateLabel));
        infoCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));
        infoCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(infoCard);
        center.add(Box.createVerticalStrut(12));

        // 2. Action & Options Card
        JPanel actionCard = createCard("DOWNLOAD & EXPORT");
        actionCard.setLayout(new BorderLayout(12, 0));

        JPanel formatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        formatPanel.setOpaque(false);
        JLabel fmtLabel = new JLabel("Format: ");
        fmtLabel.setFont(FONT_MEDIUM.deriveFont(12.5f));
        fmtLabel.setForeground(TEXT_SECONDARY);
        formatPanel.add(fmtLabel);
        styleComboBox(formatBox);
        formatPanel.add(formatBox);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(downloadButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(openFolderButton);

        actionCard.add(formatPanel, BorderLayout.WEST);
        actionCard.add(buttonPanel, BorderLayout.EAST);
        actionCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        actionCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(actionCard);
        center.add(Box.createVerticalStrut(10));

        // 3. Progress Bar
        styleProgressBar(progressBar);
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        center.add(progressBar);
        center.add(Box.createVerticalStrut(10));

        // 4. Activity Log Card with Clear & Copy controls
        JPanel logCard = createCard("ACTIVITY LOG");
        logCard.setLayout(new BorderLayout(0, 6));

        JPanel logControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        logControls.setOpaque(false);
        JButton copyLogButton = createSmallButton("📋 Copy");
        JButton clearLogButton = createSmallButton("🗑 Clear");
        copyLogButton.addActionListener(e -> copyLogToClipboard());
        clearLogButton.addActionListener(e -> logArea.setText(""));
        logControls.add(copyLogButton);
        logControls.add(clearLogButton);

        JPanel logHeader = new JPanel(new BorderLayout());
        logHeader.setOpaque(false);
        logHeader.add(logControls, BorderLayout.EAST);
        logCard.add(logHeader, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(FONT_LIGHT.deriveFont(11.5f));
        logArea.setBackground(LOG_BG);
        logArea.setForeground(TEXT_SECONDARY);
        logArea.setCaretColor(PURPLE_ACCENT);
        logArea.setBorder(new EmptyBorder(8, 10, 8, 10));

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createEmptyBorder());
        logScroll.getViewport().setBackground(LOG_BG);
        styleScrollBar(logScroll);
        logCard.add(logScroll, BorderLayout.CENTER);
        logCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(logCard);

        wrapper.add(center, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);

        // ── Action Handlers & Event Listeners ───────────────────────────
        getInfoButton.addActionListener(e -> onGetInfo());
        downloadButton.addActionListener(e -> onDownload());
        cancelButton.addActionListener(e -> onCancelClicked());
        openFolderButton.addActionListener(e -> onOpenFolder());

        pasteButton.addActionListener(e -> onPasteUrl());
        clearUrlButton.addActionListener(e -> {
            urlField.setText("");
            resetInfo();
            urlField.requestFocus();
        });

        // Press Enter in URL field to trigger Get Info
        urlField.addActionListener(e -> {
            if (getInfoButton.isEnabled()) onGetInfo();
        });

        downloadButton.setEnabled(false);
        cancelButton.setEnabled(false);
        openFolderButton.setEnabled(false);

        urlField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { onUrlModified(); }
            @Override public void removeUpdate(DocumentEvent e)  { onUrlModified(); }
            @Override public void changedUpdate(DocumentEvent e) { onUrlModified(); }
        });

        updateStatus("READY", SUCCESS_COLOR);
    }

    private void onUrlModified() {
        if (!titleLabel.getText().equals("—")) {
            resetInfo();
        }
        clearUrlButton.setVisible(!urlField.getText().isEmpty());
    }

    private void onPasteUrl() {
        try {
            String clip = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (clip != null && !clip.isBlank()) {
                urlField.setText(clip.trim());
                urlField.requestFocus();
                onGetInfo();
            }
        } catch (Exception ex) {
            log("⚠ Could not paste from clipboard: " + ex.getMessage());
        }
    }

    private void copyLogToClipboard() {
        try {
            java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(logArea.getText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
            log("✓ Log copied to clipboard.");
        } catch (Exception ignored) {}
    }

    private void onOpenFolder() {
        if (lastSavedFile != null) {
            File f = new File(lastSavedFile);
            if (f.exists()) {
                try {
                    // On Windows, select and highlight the downloaded file in Explorer
                    if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                        new ProcessBuilder("explorer.exe", "/select,", f.getAbsolutePath()).start();
                        return;
                    }
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                        Desktop.getDesktop().open(f.getParentFile());
                        return;
                    }
                } catch (Exception ex) {
                    log("⚠ Could not open folder: " + ex.getMessage());
                }
            }
        }
        // Fallback: open current working directory or temp directory
        try {
            File dir = new File("temp").exists() ? new File("temp") : new File(".");
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(dir);
            }
        } catch (Exception ignored) {}
    }

    // ═══════════════════════════════════════════════════════════════════
    // Styled Component Factories
    // ═══════════════════════════════════════════════════════════════════

    private JPanel darkPanel() {
        JPanel p = new JPanel();
        p.setBackground(BG_DARK);
        p.setOpaque(true);
        return p;
    }

    private JLabel createStatusPill() {
        JLabel pill = new JLabel(" ● READY ") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_SURFACE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), ARC, ARC));
                g2.setColor(BORDER_COLOR);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, ARC, ARC));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setFont(FONT_MEDIUM.deriveFont(11f));
        pill.setForeground(SUCCESS_COLOR);
        pill.setBorder(new EmptyBorder(4, 10, 4, 10));
        return pill;
    }

    private void updateStatus(String status, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusPill.setText(" ● " + status + " ");
            statusPill.setForeground(color);
            statusPill.repaint();
        });
    }

    private JPanel createCard(String title) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), ARC, ARC));
                g2.setColor(BORDER_COLOR);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, ARC, ARC));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        if (title != null) {
            card.setBorder(BorderFactory.createCompoundBorder(
                    card.getBorder(),
                    BorderFactory.createCompoundBorder(
                            new SectionTitleBorder(title),
                            new EmptyBorder(4, 0, 0, 0)
                    )
            ));
        }
        return card;
    }

    private JPanel createInfoRow(String label, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_MEDIUM.deriveFont(12f));
        lbl.setForeground(TEXT_MUTED);
        lbl.setPreferredSize(new Dimension(85, 20));
        row.add(lbl, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);
        return row;
    }

    private JLabel createInfoValue(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_REGULAR.deriveFont(12.5f));
        lbl.setForeground(TEXT_PRIMARY);
        return lbl;
    }

    private static JTextField createStyledTextField(String placeholder) {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), ARC, ARC));
                g2.dispose();
                super.paintComponent(g);

                if (getText().isEmpty() && !hasFocus()) {
                    Graphics2D g3 = (Graphics2D) g.create();
                    g3.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g3.setColor(TEXT_MUTED);
                    g3.setFont(getFont());
                    Insets ins = getInsets();
                    g3.drawString(placeholder, ins.left + 2, getHeight() / 2 + g3.getFontMetrics().getAscent() / 2 - 1);
                    g3.dispose();
                }
            }
        };
        field.setOpaque(false);
        field.setBackground(BG_INPUT);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(PURPLE_ACCENT);
        field.setFont(FONT_REGULAR.deriveFont(13f));
        field.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(BORDER_COLOR, ARC),
                new EmptyBorder(8, 12, 8, 12)
        ));
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(PURPLE_ACCENT, ARC),
                        new EmptyBorder(8, 12, 8, 12)
                ));
                field.repaint();
            }
            @Override public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(BORDER_COLOR, ARC),
                        new EmptyBorder(8, 12, 8, 12)
                ));
                field.repaint();
            }
        });
        return field;
    }

    private static JButton createAccentButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg;
                if (!isEnabled()) bg = PURPLE_DIM;
                else if (getModel().isPressed()) bg = PURPLE_PRESSED;
                else if (getModel().isRollover()) bg = PURPLE_HOVER;
                else bg = PURPLE_ACCENT;
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), ARC, ARC));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(FONT_MEDIUM.deriveFont(12.5f));
        btn.setForeground(Color.WHITE);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        return btn;
    }

    private static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg;
                if (!isEnabled()) bg = BG_CARD;
                else if (getModel().isPressed()) bg = BG_HOVER;
                else if (getModel().isRollover()) bg = BG_HOVER;
                else bg = BG_SURFACE;
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), ARC, ARC));
                g2.setColor(isEnabled() ? BORDER_COLOR : BG_CARD);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, ARC, ARC));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(FONT_REGULAR.deriveFont(12f));
        btn.setForeground(TEXT_SECONDARY);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 12, 8, 12));
        return btn;
    }

    private static JButton createSmallButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? BG_HOVER : BG_SURFACE;
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), ARC - 2, ARC - 2));
                g2.setColor(BORDER_COLOR);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, ARC - 2, ARC - 2));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(FONT_REGULAR.deriveFont(11f));
        btn.setForeground(TEXT_SECONDARY);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(4, 8, 4, 8));
        return btn;
    }

    private void styleComboBox(JComboBox<FormatOption> combo) {
        combo.setFont(FONT_REGULAR.deriveFont(12f));
        combo.setBackground(BG_INPUT);
        combo.setForeground(TEXT_PRIMARY);
        combo.setBorder(new RoundedBorder(BORDER_COLOR, ARC));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setFont(FONT_REGULAR.deriveFont(12f));
                lbl.setBorder(new EmptyBorder(5, 10, 5, 10));
                if (isSelected) {
                    lbl.setBackground(PURPLE_ACCENT);
                    lbl.setForeground(Color.WHITE);
                } else {
                    lbl.setBackground(BG_CARD);
                    lbl.setForeground(TEXT_PRIMARY);
                }
                return lbl;
            }
        });
        combo.setPreferredSize(new Dimension(240, 34));
    }

    private void styleProgressBar(JProgressBar bar) {
        bar.setStringPainted(false);
        bar.setBackground(BG_SURFACE);
        bar.setForeground(PURPLE_ACCENT);
        bar.setBorderPainted(false);
        bar.setOpaque(false);
        bar.setUI(new BasicProgressBarUI() {
            @Override
            protected void paintDeterminate(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = c.getWidth(), h = c.getHeight();
                g2.setColor(BG_SURFACE);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));
                int fillW = (int) (w * bar.getPercentComplete());
                if (fillW > 0) {
                    g2.setColor(PURPLE_ACCENT);
                    g2.fill(new RoundRectangle2D.Float(0, 0, fillW, h, h, h));
                }
                g2.dispose();
            }

            @Override
            protected void paintIndeterminate(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = c.getWidth(), h = c.getHeight();
                g2.setColor(BG_SURFACE);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));

                int boxW = Math.max(40, w / 4);
                int x = progressAnimOffset - boxW;
                g2.setColor(PURPLE_ACCENT);
                g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, h, h));
                g2.fill(new RoundRectangle2D.Float(x, 0, boxW, h, h, h));
                g2.dispose();
            }
        });
    }

    private void styleScrollBar(JScrollPane scroll) {
        scroll.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = BORDER_COLOR;
                this.trackColor = LOG_BG;
            }
            @Override protected JButton createDecreaseButton(int orientation) { return zeroButton(); }
            @Override protected JButton createIncreaseButton(int orientation) { return zeroButton(); }
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BORDER_COLOR);
                g2.fill(new RoundRectangle2D.Float(r.x + 2, r.y, r.width - 4, r.height, 6, 6));
                g2.dispose();
            }
            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
                g.setColor(LOG_BG);
                g.fillRect(r.x, r.y, r.width, r.height);
            }
        });
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
    }

    private static JButton zeroButton() {
        JButton btn = new JButton();
        btn.setPreferredSize(new Dimension(0, 0));
        return btn;
    }

    // ── Custom Borders ─────────────────────────────────────────────────

    private static class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int arc;
        RoundedBorder(Color color, int arc) { this.color = color; this.arc = arc; }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1, h - 1, arc, arc));
            g2.dispose();
        }
        @Override
        public Insets getBorderInsets(Component c) { return new Insets(0, 0, 0, 0); }
    }

    private class SectionTitleBorder extends AbstractBorder {
        private final String title;
        SectionTitleBorder(String title) { this.title = title; }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(FONT_MEDIUM.deriveFont(10.5f));
            g2.setColor(PURPLE_ACCENT);
            g2.drawString(title.toUpperCase(), x, y + g2.getFontMetrics().getAscent());
            g2.dispose();
        }
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(18, 0, 0, 0);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // State Management
    // ═══════════════════════════════════════════════════════════════════

    private void resetInfo() {
        titleLabel.setText("—");
        titleLabel.setToolTipText(null);
        durationLabel.setText("—");
        uploaderLabel.setText("—");
        uploaderLabel.setToolTipText(null);
        bitrateLabel.setText("—");
        downloadButton.setEnabled(false);
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void setBusy(boolean busy, String status, Color statusColor) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setIndeterminate(busy);
            if (busy) {
                if (!progressAnimTimer.isRunning()) progressAnimTimer.start();
            } else {
                if (progressAnimTimer.isRunning()) progressAnimTimer.stop();
                progressBar.setValue(0);
            }
            updateStatus(status, statusColor);
        });
    }

    // ═══════════════════════════════════════════════════════════════════
    // Backend Detection
    // ═══════════════════════════════════════════════════════════════════

    private void detectBackend() {
        String[] exeCandidates = new String[]{
                Paths.get(pythonDir, "backend_cli.exe").normalize().toString(),
                Paths.get(pythonDir, "..", "backend_cli.exe").normalize().toString(),
                Paths.get("backend_cli.exe").toAbsolutePath().normalize().toString(),
                Paths.get("app", "backend_cli.exe").toAbsolutePath().normalize().toString(),
                Paths.get(pythonDir, "backend_cli").normalize().toString(),
                Paths.get(pythonDir, "..", "backend_cli").normalize().toString()
        };

        for (String candidate : exeCandidates) {
            File f = new File(candidate);
            if (f.isFile() && f.canExecute()) {
                standaloneBackendExe = f;
                log("✓ Standalone backend detected: " + f.getName());
                return;
            }
        }

        String[] venvCandidates = new String[]{
                Paths.get(pythonDir, "..", ".venv", "Scripts", "python.exe").normalize().toString(),
                Paths.get(pythonDir, ".venv", "Scripts", "python.exe").normalize().toString(),
                Paths.get(pythonDir, "..", ".venv", "bin", "python").normalize().toString(),
                Paths.get(pythonDir, ".venv", "bin", "python").normalize().toString(),
                Paths.get(pythonDir, "..", "venv", "Scripts", "python.exe").normalize().toString(),
                Paths.get(pythonDir, "venv", "Scripts", "python.exe").normalize().toString()
        };

        for (String candidate : venvCandidates) {
            File f = new File(candidate);
            if (f.exists() && testExecutable(candidate)) {
                pythonExecutable = candidate;
                log("✓ Python runtime (venv): " + new File(candidate).getName());
                return;
            }
        }

        for (String candidate : new String[]{"python", "py", "python3"}) {
            if (testExecutable(candidate)) {
                pythonExecutable = candidate;
                log("✓ Python runtime: " + candidate);
                return;
            }
        }

        log("⚠ Warning: No Python runtime or standalone backend executable found.");
    }

    private boolean testExecutable(String cmd) {
        try {
            Process p = new ProcessBuilder(cmd, "--version").start();
            return p.waitFor() == 0;
        } catch (IOException | InterruptedException ignored) {
            return false;
        }
    }

    private String[] buildBackendCommand(String... args) {
        if (standaloneBackendExe != null && standaloneBackendExe.exists()) {
            String[] cmd = new String[args.length + 1];
            cmd[0] = standaloneBackendExe.getAbsolutePath();
            System.arraycopy(args, 0, cmd, 1, args.length);
            return cmd;
        } else {
            String[] cmd = new String[args.length + 3];
            cmd[0] = pythonExecutable;
            cmd[1] = "-u";
            cmd[2] = new File(pythonDir, "backend_cli.py").getAbsolutePath();
            System.arraycopy(args, 0, cmd, 3, args.length);
            return cmd;
        }
    }

    private void cancelActiveProcess() {
        Process p = activeProcess;
        if (p != null && p.isAlive()) {
            p.destroyForcibly();
            log("⚠ Process was cancelled.");
        }
    }

    private void onCancelClicked() {
        cancelActiveProcess();
        setBusy(false, "CANCELLED", WARNING_COLOR);
        cancelButton.setEnabled(false);
        getInfoButton.setEnabled(true);
        if (!titleLabel.getText().equals("—")) {
            downloadButton.setEnabled(true);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Get Info
    // ═══════════════════════════════════════════════════════════════════

    private void onGetInfo() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            showStyledMessage("Please enter or paste a media URL first.", "Missing URL", false);
            return;
        }

        log("⏳ Fetching metadata for URL...");
        setBusy(true, "FETCHING INFO", PURPLE_ACCENT);
        getInfoButton.setEnabled(false);
        downloadButton.setEnabled(false);
        cancelButton.setEnabled(true);

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() {
                runBackend(buildBackendCommand("info", url), this::publish);
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String line : chunks) handleInfoLine(line);
            }

            @Override
            protected void done() {
                setBusy(false, "READY", SUCCESS_COLOR);
                getInfoButton.setEnabled(true);
                cancelButton.setEnabled(false);
            }
        }.execute();
    }

    private void handleInfoLine(String line) {
        if (line.startsWith("TITLE:")) {
            pendingTitle = line.substring(6);
        } else if (line.startsWith("DURATION:")) {
            pendingDuration = line.substring(9);
        } else if (line.startsWith("UPLOADER:")) {
            pendingUploader = line.substring(9);
        } else if (line.startsWith("BITRATE:")) {
            pendingBitrate = line.substring(8);
        } else if (line.equals("DONE")) {
            titleLabel.setText(pendingTitle);
            titleLabel.setToolTipText(pendingTitle);
            durationLabel.setText(formatDuration(pendingDuration));
            uploaderLabel.setText(pendingUploader);
            uploaderLabel.setToolTipText(pendingUploader);
            bitrateLabel.setText(pendingBitrate + " kbps");
            downloadButton.setEnabled(true);
            log("✓ Media info loaded successfully: " + pendingTitle);
        } else if (line.startsWith("ERROR:")) {
            String msg = line.substring(6);
            log("✗ " + msg);
            updateStatus("ERROR", ERROR_COLOR);
            showStyledMessage(msg, "Error Retrieving Info", true);
        } else if (!line.isBlank()) {
            log(line);
        }
    }

    private String formatDuration(String secondsStr) {
        try {
            int t = (int) Double.parseDouble(secondsStr);
            if (t <= 0) return "Live / Unknown";
            int h = t / 3600, m = (t % 3600) / 60, s = t % 60;
            return h > 0 ? String.format("%d:%02d:%02d (%ds)", h, m, s, t) : String.format("%d:%02d (%ds)", m, s, t);
        } catch (Exception e) { return secondsStr + "s"; }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Download & Convert
    // ═══════════════════════════════════════════════════════════════════

    private void onDownload() {
        String url = urlField.getText().trim();
        FormatOption opt = (FormatOption) formatBox.getSelectedItem();
        String format = opt != null ? opt.ext : "mp4";

        if (url.isEmpty()) {
            showStyledMessage("Please enter or paste a media URL first.", "Missing URL", false);
            return;
        }

        log("⬇ Starting download & conversion → [" + format.toUpperCase() + "]...");
        setBusy(true, "DOWNLOADING", PURPLE_ACCENT);
        getInfoButton.setEnabled(false);
        downloadButton.setEnabled(false);
        cancelButton.setEnabled(true);

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() {
                runBackend(buildBackendCommand("process", url, format), this::publish);
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String line : chunks) handleProcessLine(line);
            }

            @Override
            protected void done() {
                setBusy(false, "COMPLETE", SUCCESS_COLOR);
                getInfoButton.setEnabled(true);
                downloadButton.setEnabled(true);
                cancelButton.setEnabled(false);
            }
        }.execute();
    }

    private void handleProcessLine(String line) {
        if (line.startsWith("STATUS:")) {
            String statusMsg = line.substring(7);
            log("  ⚙ " + statusMsg);
            if (statusMsg.toLowerCase().contains("converting")) {
                updateStatus("CONVERTING", WARNING_COLOR);
            }
        } else if (line.startsWith("FILE:")) {
            String path = line.substring(5);
            lastSavedFile = path;
            openFolderButton.setEnabled(true);
            log("✓ Saved successfully: " + path);
            updateStatus("COMPLETE", SUCCESS_COLOR);
            showStyledMessage("Download complete!\n\nSaved to:\n" + path, "Download Success", false);
        } else if (line.startsWith("ERROR:")) {
            String msg = line.substring(6);
            log("✗ " + msg);
            updateStatus("ERROR", ERROR_COLOR);
            showStyledMessage(msg, "Process Error", true);
        } else if (!line.isBlank()) {
            log(line);
        }
    }

    private void showStyledMessage(String msg, String title, boolean isError) {
        UIManager.put("OptionPane.background", BG_CARD);
        UIManager.put("Panel.background", BG_CARD);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("OptionPane.messageFont", FONT_REGULAR.deriveFont(12.5f));
        UIManager.put("Button.background", PURPLE_ACCENT);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.font", FONT_MEDIUM.deriveFont(12f));

        JOptionPane.showMessageDialog(this, msg, title,
                isError ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
    }

    // ═══════════════════════════════════════════════════════════════════
    // Subprocess Runner
    // ═══════════════════════════════════════════════════════════════════

    private void runBackend(String[] command, Consumer<String> onLine) {
        boolean hasErrorLine = false;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(pythonDir));
            pb.redirectErrorStream(true);

            // Force UTF-8 output from Python so Japanese titles/paths don't mojibake or fail on Windows
            pb.environment().put("PYTHONUTF8", "1");
            pb.environment().put("PYTHONIOENCODING", "utf-8");

            Process process = pb.start();
            activeProcess = process;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("ERROR:")) hasErrorLine = true;
                    onLine.accept(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0 && !hasErrorLine) {
                onLine.accept("ERROR: Backend exited with code " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            onLine.accept("ERROR: " + e.getMessage());
        } finally {
            activeProcess = null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Entry Point
    // ═══════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        String pythonDir;
        if (args.length > 0) {
            pythonDir = args[0];
        } else {
            String c1 = Paths.get("back").toAbsolutePath().toString();
            String c2 = Paths.get("..", "back").toAbsolutePath().toString();
            String c3 = Paths.get(".").toAbsolutePath().toString();
            if (new File(c1, "backend_cli.py").exists() || new File(c1, "backend_cli.exe").exists()) pythonDir = c1;
            else if (new File(c2, "backend_cli.py").exists() || new File(c2, "backend_cli.exe").exists()) pythonDir = c2;
            else if (new File(c3, "backend_cli.py").exists() || new File(c3, "backend_cli.exe").exists()) pythonDir = c3;
            else pythonDir = c1;
        }

        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch (Exception ignored) {}
            VideoDownloaderGUI gui = new VideoDownloaderGUI(pythonDir);
            gui.setVisible(true);
        });
    }
}