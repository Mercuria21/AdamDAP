import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

/**
 * Swing front-end for the yt-dlp downloader/converter project.
 *
 * Can execute either:
 * 1. A standalone packaged backend binary (backend_cli.exe), OR
 * 2. The Python script (back/backend_cli.py) via a detected Python runtime.
 */
public class VideoDownloaderGUI extends JFrame {

    private final JTextField urlField = new JTextField();
    private final JButton getInfoButton = new JButton("Get Info");
    private final JLabel titleLabel = new JLabel("Title: -");
    private final JLabel durationLabel = new JLabel("Duration: -");
    private final JLabel uploaderLabel = new JLabel("Uploader: -");
    private final JLabel bitrateLabel = new JLabel("Bitrate: -");
    private final JComboBox<String> formatBox = new JComboBox<>(new String[]{
            "mp4", "webm", "mkv", "mov", "mp3", "wav", "m4a", "aac", "opus"
    });
    private final JButton downloadButton = new JButton("Download & Convert");
    private final JButton cancelButton = new JButton("Cancel");
    private final JTextArea logArea = new JTextArea();
    private final JProgressBar progressBar = new JProgressBar();

    private final String pythonDir;
    private String pythonExecutable = "python";
    private File standaloneBackendExe = null;
    private volatile Process activeProcess = null;

    private String pendingTitle = "";
    private String pendingDuration = "";
    private String pendingUploader = "";
    private String pendingBitrate = "";

    public VideoDownloaderGUI(String pythonDir) {
        super("YT Downloader & Converter");
        this.pythonDir = pythonDir;
        buildUI();
        detectBackend();
    }

    private void buildUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(650, 580);
        setMinimumSize(new Dimension(500, 450));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelActiveProcess();
            }
        });

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.setBorder(new EmptyBorder(10, 10, 0, 10));
        top.add(new JLabel("Video URL:"), BorderLayout.WEST);
        top.add(urlField, BorderLayout.CENTER);
        top.add(getInfoButton, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel infoPanel = new JPanel(new GridLayout(4, 1, 4, 4));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Video Info"));
        infoPanel.add(titleLabel);
        infoPanel.add(durationLabel);
        infoPanel.add(uploaderLabel);
        infoPanel.add(bitrateLabel);
        infoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        center.add(infoPanel);
        center.add(Box.createVerticalStrut(8));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actionPanel.add(new JLabel("Convert to:"));
        actionPanel.add(formatBox);
        actionPanel.add(downloadButton);
        actionPanel.add(cancelButton);
        actionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        center.add(actionPanel);

        progressBar.setStringPainted(true);
        progressBar.setString("Idle");
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        center.add(progressBar);
        center.add(Box.createVerticalStrut(8));

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log"));
        logScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(logScroll);

        add(center, BorderLayout.CENTER);

        getInfoButton.addActionListener(e -> onGetInfo());
        downloadButton.addActionListener(e -> onDownload());
        cancelButton.addActionListener(e -> onCancelClicked());

        downloadButton.setEnabled(false);
        cancelButton.setEnabled(false);

        // Reset info labels if URL text changes
        urlField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { resetInfo(); }
            @Override
            public void removeUpdate(DocumentEvent e) { resetInfo(); }
            @Override
            public void changedUpdate(DocumentEvent e) { resetInfo(); }
        });
    }

    private void resetInfo() {
        titleLabel.setText("Title: -");
        titleLabel.setToolTipText(null);
        durationLabel.setText("Duration: -");
        uploaderLabel.setText("Uploader: -");
        bitrateLabel.setText("Bitrate: -");
        downloadButton.setEnabled(false);
    }

    private void detectBackend() {
        // 1. Check for standalone compiled backend_cli executable first (bundled app mode)
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
                log("Found standalone backend executable: " + f.getName());
                return;
            }
        }

        // 2. Check local virtual environment in workspace
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
                log("Using virtualenv Python: " + pythonExecutable);
                return;
            }
        }

        // 3. Check PATH candidates
        for (String candidate : new String[]{"python", "py", "python3"}) {
            if (testExecutable(candidate)) {
                pythonExecutable = candidate;
                log("Using system Python: " + pythonExecutable);
                return;
            }
        }

        log("WARNING: could not detect a working Python runtime or standalone backend binary.");
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
            log("Process cancelled.");
        }
    }

    private void onCancelClicked() {
        cancelActiveProcess();
        setBusy(false, "Cancelled");
        cancelButton.setEnabled(false);
        getInfoButton.setEnabled(true);
        if (!titleLabel.getText().equals("Title: -")) {
            downloadButton.setEnabled(true);
        }
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void setBusy(boolean busy, String status) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setIndeterminate(busy);
            progressBar.setString(status);
        });
    }

    // ---------------------------------------------------------------
    // Get Info
    // ---------------------------------------------------------------

    private void onGetInfo() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a video URL first.",
                    "Missing URL", JOptionPane.WARNING_MESSAGE);
            return;
        }

        log("Fetching info for: " + url);
        setBusy(true, "Fetching info...");
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
                for (String line : chunks) {
                    handleInfoLine(line);
                }
            }

            @Override
            protected void done() {
                setBusy(false, "Idle");
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
            titleLabel.setText("Title: " + pendingTitle);
            titleLabel.setToolTipText(pendingTitle);
            durationLabel.setText("Duration: " + formatDuration(pendingDuration));
            uploaderLabel.setText("Uploader: " + pendingUploader);
            bitrateLabel.setText("Bitrate: " + pendingBitrate + " kbps");
            downloadButton.setEnabled(true);
            log("Info retrieved successfully.");
        } else if (line.startsWith("ERROR:")) {
            String msg = line.substring(6);
            log("Error: " + msg);
            JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        } else if (!line.isBlank()) {
            log(line);
        }
    }

    private String formatDuration(String secondsStr) {
        try {
            int totalSeconds = (int) Double.parseDouble(secondsStr);
            if (totalSeconds <= 0) {
                return "Unknown / Live";
            }
            int h = totalSeconds / 3600;
            int m = (totalSeconds % 3600) / 60;
            int s = totalSeconds % 60;
            if (h > 0) {
                return String.format("%d:%02d:%02d", h, m, s);
            }
            return String.format("%d:%02d", m, s);
        } catch (Exception e) {
            return secondsStr + "s";
        }
    }

    // ---------------------------------------------------------------
    // Download & Convert
    // ---------------------------------------------------------------

    private void onDownload() {
        String url = urlField.getText().trim();
        String format = (String) formatBox.getSelectedItem();

        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a video URL first.",
                    "Missing URL", JOptionPane.WARNING_MESSAGE);
            return;
        }

        log("Starting download & conversion to " + format + "...");
        setBusy(true, "Working...");
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
                for (String line : chunks) {
                    handleProcessLine(line);
                }
            }

            @Override
            protected void done() {
                setBusy(false, "Idle");
                getInfoButton.setEnabled(true);
                downloadButton.setEnabled(true);
                cancelButton.setEnabled(false);
            }
        }.execute();
    }

    private void handleProcessLine(String line) {
        if (line.startsWith("STATUS:")) {
            String status = line.substring(7);
            log(status);
            progressBar.setString(status);
        } else if (line.startsWith("FILE:")) {
            String path = line.substring(5);
            log("Done! Saved to: " + path);
            JOptionPane.showMessageDialog(this, "Saved to:\n" + path,
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } else if (line.startsWith("ERROR:")) {
            String msg = line.substring(6);
            log("Error: " + msg);
            JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        } else if (!line.isBlank()) {
            log(line);
        }
    }

    // ---------------------------------------------------------------
    // Subprocess plumbing
    // ---------------------------------------------------------------

    private void runBackend(String[] command, Consumer<String> onLine) {
        boolean hasErrorLine = false;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(pythonDir));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            activeProcess = process;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("ERROR:")) {
                        hasErrorLine = true;
                    }
                    onLine.accept(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0 && !hasErrorLine) {
                onLine.accept("ERROR: Backend process exited with error code " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            onLine.accept("ERROR:" + e.getMessage());
        } finally {
            activeProcess = null;
        }
    }

    public static void main(String[] args) {
        // Optional first arg: path to the folder containing backend_cli.py or backend_cli.exe.
        // Defaults to the back folder relative to the current working directory.
        String pythonDir;
        if (args.length > 0) {
            pythonDir = args[0];
        } else {
            String candidate1 = Paths.get("back").toAbsolutePath().toString();
            String candidate2 = Paths.get("..", "back").toAbsolutePath().toString();
            String candidate3 = Paths.get(".").toAbsolutePath().toString();

            if (new File(candidate1, "backend_cli.py").exists() || new File(candidate1, "backend_cli.exe").exists()) {
                pythonDir = candidate1;
            } else if (new File(candidate2, "backend_cli.py").exists() || new File(candidate2, "backend_cli.exe").exists()) {
                pythonDir = candidate2;
            } else if (new File(candidate3, "backend_cli.py").exists() || new File(candidate3, "backend_cli.exe").exists()) {
                pythonDir = candidate3;
            } else {
                pythonDir = candidate1;
            }
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // fall back to default look and feel
            }
            VideoDownloaderGUI gui = new VideoDownloaderGUI(pythonDir);
            gui.setVisible(true);
        });
    }
}
