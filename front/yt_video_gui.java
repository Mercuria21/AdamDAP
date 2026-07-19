import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

/**
 * Swing front-end for the Python yt-dlp downloader/converter project.
 *
 * Java cannot call the Python functions directly, so this GUI shells out
 * to python/backend_cli.py, which wraps metadata.py, downloader.py and
 * converter.py and talks back over a simple line-based text protocol
 * (see backend_cli.py for the exact format).
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
    private final JTextArea logArea = new JTextArea();
    private final JProgressBar progressBar = new JProgressBar();

    private final String pythonDir;
    private String pythonExecutable = "python3";

    private String pendingTitle = "";
    private String pendingDuration = "";
    private String pendingUploader = "";
    private String pendingBitrate = "";

    public VideoDownloaderGUI(String pythonDir) {
        super("YT Downloader & Converter");
        this.pythonDir = pythonDir;
        buildUI();
        detectPythonExecutable();
    }

    private void buildUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(620, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.setBorder(new EmptyBorder(10, 10, 0, 10));
        top.add(new JLabel("Video URL:"), BorderLayout.WEST);
        top.add(urlField, BorderLayout.CENTER);
        top.add(getInfoButton, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel infoPanel = new JPanel(new GridLayout(4, 1, 2, 2));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Video Info"));
        infoPanel.add(titleLabel);
        infoPanel.add(durationLabel);
        infoPanel.add(uploaderLabel);
        infoPanel.add(bitrateLabel);
        infoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        center.add(infoPanel);
        center.add(Box.createVerticalStrut(8));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionPanel.add(new JLabel("Convert to:"));
        actionPanel.add(formatBox);
        actionPanel.add(downloadButton);
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
        downloadButton.setEnabled(false);
    }

    private void detectPythonExecutable() {
        for (String candidate : new String[]{"python3", "python"}) {
            try {
                Process p = new ProcessBuilder(candidate, "--version").start();
                if (p.waitFor() == 0) {
                    pythonExecutable = candidate;
                    return;
                }
            } catch (IOException | InterruptedException ignored) {
                // try next candidate
            }
        }
        log("WARNING: could not detect a python executable on PATH; defaulting to 'python3'.");
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

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() {
                runBackend(new String[]{pythonExecutable, "-u", "backend_cli.py", "info", url}, this::publish);
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

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() {
                runBackend(new String[]{pythonExecutable, "-u", "backend_cli.py", "process", url, format}, this::publish);
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
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(pythonDir));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    onLine.accept(line);
                }
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            onLine.accept("ERROR:" + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Optional first arg: path to the "python" folder containing backend_cli.py.
        // Defaults to a "python" folder next to wherever the app is run from.
        String pythonDir = args.length > 0 ? args[0] : Paths.get("python").toAbsolutePath().toString();

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
