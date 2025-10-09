import java.io.*;

public class GitTester extends Git {

    public static boolean verify() {
        File f1 = new File("git");
        File f2 = new File("git/objects");
        File f3 = new File("git/index");
        File f4 = new File("git/HEAD");
        if(f1.exists() && f2.exists() && f3.exists() && f4.exists()) return true;
        return false;
    }

    public static void cleanUp() {
        File f = new File("git");
        cleanUpHelper(f);
    }

    public static void cleanUpHelper(File file) {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) cleanUpHelper(f);
            }
        }
        file.delete();
    }

    public static void reset() {
        File obj = new File("git/objects");
        if (obj.exists() && obj.isDirectory()) {
            File[] files = obj.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
        }
        File idx = new File("git/index");
        if (idx.exists()) {
            try (FileWriter fw = new FileWriter(idx)) {
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String readObjectText(String sha) throws IOException {
        File f = new File("git/objects", sha);
        if (!f.exists() || !f.isFile()) {
            return null;
        }
        FileInputStream in = new FileInputStream(f);
        byte[] buf = in.readAllBytes();
        in.close();
        return new String(buf, java.nio.charset.StandardCharsets.UTF_8);
    }
    
    private static void traceTree(String sha, String indent) throws IOException {
        String content = readObjectText(sha);
        if (content == null) {
            System.out.println(indent + "(missing tree) " + sha);
            return;
        }
        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() == 0) {
                continue;
            }
            int sp1 = line.indexOf(' ');
            if (sp1 == -1) {
                continue;
            }
            int sp2 = line.indexOf(' ', sp1 + 1);
            if (sp2 == -1) {
                continue;
            }
            String type = line.substring(0, sp1);
            String childSha = line.substring(sp1 + 1, sp2);
            String name = line.substring(sp2 + 1);
    
            if (type.equals("blob")) {
                File b = new File("git/objects", childSha);
                if (b.exists()) {
                    System.out.println(indent + "blob  " + childSha + "  " + name + "  [OK]");
                } else {
                    System.out.println(indent + "blob  " + childSha + "  " + name + "  [MISSING]");
                }
            } else if (type.equals("tree")) {
                System.out.println(indent + "tree  " + childSha + "  " + name);
                traceTree(childSha, indent + "  ");
            }
        }
    }

    

    public static void main(String[] args) throws IOException {
        initRepo();
        System.out.println(verify());
        cleanUp();
        reset();

        File sampleDir = new File("git/samples");
        sampleDir.mkdirs();
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("git/samples/file1.txt"))) {
            bufferedWriter.write("Hello world");
        }
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("git/samples/file2.txt"))) {
            bufferedWriter.write("Hello world again");
        }
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("git/samples/file3.txt"))) {
            bufferedWriter.write("Hello world again again");
        }
        blob("git/samples/file1.txt");
        blob("git/samples/file2.txt");
        blob("git/samples/file3.txt");
        addToIdx(hashFile("git/samples/file1.txt"), "git/samples/file1.txt");
        addToIdx(hashFile("git/samples/file2.txt"), "git/samples/file2.txt");
        addToIdx(hashFile("git/samples/file3.txt"), "git/samples/file3.txt");
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("git/index"))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] s = line.split(" ", 2);
                if (s.length == 2) {
                    String hash = s[0];
                    File blob = new File("git/objects", hash);
                    if (blob.exists()) System.out.println("yess");
                    else System.out.println("nooo");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // GP-4.1: identify and trace the root tree
try {
    String root = genTreesFromIdx();             // build trees from the current index
    System.out.println("ROOT TREE: " + root);    // show root tree SHA
    traceTree(root, "");                         // recursively print/verify contents
} catch (Exception e) {
    System.out.println("Failed to build/trace root tree");
    e.printStackTrace();
}

try {
    GitRekt gw = new GitRekt();

    // initialize via wrapper
    gw.reset();
    gw.init();

    // create a tiny project for the wrapper to work with
    File inner = new File("myProgram/inner");
    if (!inner.exists()) inner.mkdirs();
    try (BufferedWriter w = new BufferedWriter(new FileWriter("myProgram/hello.txt"))) {
        w.write("hello");
    }
    try (BufferedWriter w = new BufferedWriter(new FileWriter("myProgram/inner/world.txt"))) {
        w.write("world");
    }

    // stage files using the wrapper
    gw.add("myProgram/hello.txt");
    gw.add("myProgram/inner/world.txt");

    // first commit
    String c1 = gw.commit("Student", "wrapper commit 1");
    System.out.println("GitRekt commit1: " + c1);

    // modify one file
    try (BufferedWriter w = new BufferedWriter(new FileWriter("myProgram/hello.txt"))) {
        w.write("hello v2");
    }

    // restage and second commit
    gw.add("myProgram/hello.txt");
    String c2 = gw.commit("Student", "wrapper commit 2");
    System.out.println("GitRekt commit2: " + c2);

    // print HEAD to confirm latest commit
    File head = new File("git/HEAD");
    if (head.exists()) {
        BufferedReader br = new BufferedReader(new FileReader(head));
        String headSha = br.readLine();
        br.close();
        System.out.println("HEAD now points to: " + headSha);
    }
} catch (Exception ex) {
    System.out.println("GitRekt wrapper test failed");
    ex.printStackTrace();
}

    }
}