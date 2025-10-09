import java.io.*;
import java.nio.charset.StandardCharsets;

public class GitRekt {

    // Initializes the repository folder structure
    public void init() {
        try {
            Git.initRepo();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Stages a file by creating its blob and adding it to the index
    public void add(String filePath) {
        try {
            String sha = Git.hashFile(filePath);
            if (sha == null) {
                throw new IOException("hashFile returned null for: " + filePath);
            }
            Git.addToIdx(sha, filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Creates a commit with author and message, updates HEAD, and returns commit SHA
    String commit(String author, String message) {
        try {
            return Git.commit(author, message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Extra Credit: Restores the working directory from a specific commit into ./restored
    public void checkout(String commitHash) {
        try {
            File commitObj = new File("git/objects", commitHash);
            if (!commitObj.exists()) {
                System.out.println("Commit not found: " + commitHash);
                return;
            }
            String content = readAll(commitObj);
            String treeSha = parseFirstValue(content, "tree:");
            if (treeSha == null || treeSha.isEmpty()) {
                System.out.println("No tree in commit: " + commitHash);
                return;
            }
            File outRoot = new File("restored");
            if (!outRoot.exists()) outRoot.mkdirs();
            restoreTree(treeSha, outRoot);
            System.out.println("Restored to ./restored from commit " + commitHash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Tests all GitRekt functions: init, add, commit
    public void testGitRekt() {
        try {
            init();
            new File("myProgram/inner").mkdirs();
            writeText("myProgram/hello.txt", "hello");
            writeText("myProgram/inner/world.txt", "world");
            writeText("myProgram/readme.md", "# demo");

            add("myProgram/hello.txt");
            add("myProgram/inner/world.txt");
            add("myProgram/readme.md");

            String c1 = commit("John Doe", "Initial commit");
            System.out.println("Commit: " + c1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Deletes git/, myProgram/, and restored/ for a clean reset
    public void reset() {
        deleteRecursively(new File("git"));
        deleteRecursively(new File("myProgram"));
        deleteRecursively(new File("restored"));
    }

    // Writes string content to a file
    private static void writeText(String path, String s) throws IOException {
        File f = new File(path);
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, false))) {
            bw.write(s);
        }
    }

    // Reads all text from a file
    private static String readAll(File f) throws IOException {
        byte[] b = java.nio.file.Files.readAllBytes(f.toPath());
        return new String(b, StandardCharsets.UTF_8);
    }

    // Finds the first value matching a key like "tree:"
    private static String parseFirstValue(String text, String key) {
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String ln = lines[i].trim();
            if (ln.startsWith(key)) {
                String v = ln.substring(key.length()).trim();
                return v;
            }
        }
        return null;
    }

    // Recursively rebuilds files and directories from a tree SHA
    private static void restoreTree(String treeSha, File outDir) throws Exception {
        File obj = new File("git/objects", treeSha);
        if (!obj.exists()) {
            throw new FileNotFoundException("Tree object not found: " + treeSha);
        }
        String data = readAll(obj);
        if (!outDir.exists()) outDir.mkdirs();

        String[] lines = data.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String ln = lines[i].trim();
            if (ln.length() == 0) continue;
            String[] parts = ln.split(" ", 3);
            if (parts.length < 3) continue;

            String type = parts[0];
            String sha = parts[1];
            String name = parts[2];

            if (type.equals("blob")) {
                File blob = new File("git/objects", sha);
                if (!blob.exists()) {
                    System.out.println("Missing blob: " + sha + " for " + name);
                    continue;
                }
                File outFile = new File(outDir, name);
                if (outFile.getParentFile() != null && !outFile.getParentFile().exists()) {
                    outFile.getParentFile().mkdirs();
                }
                try (InputStream in = new FileInputStream(blob);
                     OutputStream os = new FileOutputStream(outFile)) {
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = in.read(buf)) != -1) {
                        os.write(buf, 0, r);
                    }
                }
            } else if (type.equals("tree")) {
                File sub = new File(outDir, name);
                if (!sub.exists()) sub.mkdirs();
                restoreTree(sha, sub);
            }
        }
    }

    // Deletes directories and files recursively
    private static void deleteRecursively(File f) {
        if (!f.exists()) return;
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) {
                for (int i = 0; i < kids.length; i++) {
                    deleteRecursively(kids[i]);
                }
            }
        }
        f.delete();
    }
}
