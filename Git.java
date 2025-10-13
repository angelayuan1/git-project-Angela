import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Git {

    public static void initRepo() throws IOException {
        File f1 = new File("git");
        File f2 = new File("git/objects");
        File f3 = new File("git/index");
        File f4 = new File("git/HEAD");
        if(f1.exists() && f2.exists() && f3.exists() && f4.exists()) System.out.println("Git Repository Already Exists");
        else System.out.println("Git Repository Created");
        if(!f1.exists()) f1.mkdir();
        if (!f2.exists()) f2.mkdirs();
        if(!f3.exists()) f3.createNewFile();
        if(!f4.exists()) f4.createNewFile();
    }

    public static String hashFile(String filepath) {
        try (FileInputStream f = new FileInputStream(filepath)) {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bt = new byte[8192];
            int i;
            while ((i = f.read(bt)) != -1) {
                md.update(bt, 0, i);
            }
            byte[] hashedBytes = md.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashedBytes) {
                hexString.append(String.format("%02x", b & 0xff));
            }
            return hexString.toString();
        } catch (FileNotFoundException e) {
            System.out.println("Error: File not found " + filepath);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Error while reading file: " + e.getMessage());
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error: SHA-256 algorithm not available.");
            e.printStackTrace();
        }
        return null;
    }

    public static void blob(String filepath) throws IOException {
        File f1 = new File(filepath);
        String hash = hashFile(filepath);
        File objectsDir = new File("git/objects");
        if (!objectsDir.exists()) objectsDir.mkdirs();
        File f2 = new File(objectsDir, hash);
        f2.createNewFile();
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(f2))) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(f1))) {
                String s;
                while ((s = bufferedReader.readLine()) != null) {
                    bufferedWriter.write(s);
                    bufferedWriter.newLine();
                }
            }
        }
    }

    public static void addToIdx(String hash, String filepath) {
        try {
            File f = new File("git/index");
            if (!f.exists()) f.createNewFile();
            String path = new File(filepath).getPath();
            List<String> lines = new ArrayList<>();
            boolean found = false;
            boolean updated = false;
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] parts = line.split(" ", 2);
                    if (parts.length == 2) {
                        String oldHash = parts[0];
                        String oldPath = parts[1];

                        if (oldPath.equals(path)) {
                            found = true;
                            if (!oldHash.equals(hash)) {
                                lines.add(hash + " " + path);
                                updated = true;
                            } else {
                                lines.add(line);
                            }
                        } else {
                            lines.add(line);
                        }
                    }
                }
            }
            if (!found) {
                lines.add(hash + " " + path);
            }
            try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(f))) {
                for (int i = 0; i < lines.size(); i++) {
                    bufferedWriter.write(lines.get(i));
                    if (i < lines.size() - 1) bufferedWriter.newLine();
                }
            }
            File b = new File("git/objects", hash);
            if (!b.exists() || updated) {
                blob(filepath);
                System.out.println("blob made/updated for " + path);
            } else {
                System.out.println("duplicate skipped for " + path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String genTree(String dirPath) throws IOException {
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException("Invalid dir path " + dirPath);
        }
        StringBuilder sb = new StringBuilder();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                String path = f.getPath();
                if (f.isFile()) {
                    blob(path);
                    String bhash = hashFile(path);
                    sb.append("blob ").append(bhash).append(" ").append(path).append("\n");
                } else if (f.isDirectory()) {
                    String subHash = genTree(path);
                    sb.append("tree ").append(subHash).append(" ").append(path).append("\n");
                }
            }
        }
        String str = sb.toString();
        String hash = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hashedBytes = md.digest(str.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashedBytes) {
                hexString.append(String.format("%02x", b & 0xff));
            }
            hash = hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        File f = new File("git/objects/" + hash);
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(f))) {
            bufferedWriter.write(str);
        }
        return hash;
    }

    public static File createWorkingList() throws IOException {
        File f = new File("git/index");
        File wl = new File("git/working_list.txt");
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(f));
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(wl))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                bufferedWriter.write("blob " + line);
                bufferedWriter.newLine();
            }
        }
        return wl;
    }

    static class entry {
        String type;
        String sha;
        String path;

        entry(String type, String sha, String path) {
            this.type = type;
            this.sha = sha;
            this.path = path;
        }
    }

    public static String genTreesFromIdx() throws IOException {
        File wl = createWorkingList();
        List<entry> entries = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(wl))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ", 3);
                entries.add(new entry(parts[0], parts[1], parts[2]));
            }
        }
    
        while (entries.size() > 1) {
            String leafDir = findLeafDirectory(entries);
    
            // Cyrus Fix: if no subdirs remain, collapse all top-level items into a root tree
            if (leafDir == null) {
                List<String> trLines = new ArrayList<>();
                for (entry te : entries) {
                    if (!te.path.contains("/")) {
                        trLines.add(te.type + " " + te.sha + " " + te.path);
                    }
                }
                Collections.sort(trLines);
                String content = String.join("\n", trLines) + (trLines.isEmpty() ? "" : "\n");
                String rootSha = sha1(content);
    
                File objDir = new File("git/objects");
                if (!objDir.exists()) objDir.mkdirs();
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(objDir, rootSha)))) {
                    bw.write(content);
                }
    
                // replace all top-level entries with a single root tree entry
                List<entry> next = new ArrayList<>();
                next.add(new entry("tree", rootSha, ""));  // path "" denotes root
                // keep any existing subtree entries (if any) that already represent collapsed dirs
                for (entry te : entries) {
                    if (te.path.contains("/")) next.add(te);
                }
                entries = next;
                break; // will exit loop; entries.size() may now be >1 if subtrees remain
            }
    
            List<entry> children = new ArrayList<>();
            for (entry te : entries) {
                if (te.path.startsWith(leafDir + "/")) {
                    children.add(te);
                }
            }
    
            List<String> trLines = new ArrayList<>();
            for (entry te : children) {
                String name = te.path.substring(te.path.lastIndexOf("/") + 1);
                trLines.add(te.type + " " + te.sha + " " + name);
            }
            Collections.sort(trLines);
            String c = String.join("\n", trLines) + (trLines.isEmpty() ? "" : "\n");
            String hash = sha1(c);
    
            File objDir = new File("git/objects");
            if (!objDir.exists()) objDir.mkdirs();
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(objDir, hash)))) {
                bw.write(c);
            }
    
            entries.removeAll(children);
            entries.add(new entry("tree", hash, leafDir));
        }
    
        entry root = entries.get(0);
        return root.sha;
    }
    
    private static String findLeafDirectory(List<entry> entries) {
        Set<String> dirs = new HashSet<>();
        for (entry te : entries) {
            if (te.path.contains("/")) {
                dirs.add(te.path.substring(0, te.path.lastIndexOf("/")));
            }
        }
        for (String dir : dirs) {
            boolean isLeaf = true;
            for (String other : dirs) {
                if (!other.equals(dir) && other.startsWith(dir + "/")) {
                    isLeaf = false;
                    break;
                }
            }
            if (isLeaf) return dir;
        }
        return null;
    }

    private static String sha1(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hashedBytes = md.digest(content.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String commit(String author, String message) throws IOException {
        initRepo();
    
        String parent = readHead();             // previous commit SHA, or null
        String root = genTreesFromIdx();        // build trees from index; returns root tree SHA
        String date = new java.util.Date().toString();
    
        StringBuilder sb = new StringBuilder();
        sb.append("tree: ").append(root).append("\n");
        if (parent != null && parent.length() > 0) {
            sb.append("parent: ").append(parent).append("\n");
        }
        sb.append("author: ").append(author).append("\n");
        sb.append("date: ").append(date).append("\n");
        sb.append("message: ").append(message).append("\n");
    
        String content = sb.toString();
        String sha = sha1(content);
    
        File objDir = new File("git/objects");
        if (!objDir.exists()) objDir.mkdirs();
        File out = new File(objDir, sha);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(out))) {
            bw.write(content);
        }
    
        try (BufferedWriter hw = new BufferedWriter(new FileWriter("git/HEAD"))) {
            hw.write(sha);
        }
    
        return sha;
    }
    
    private static String readHead() {
        File h = new File("git/HEAD");
        if (!h.exists() || !h.isFile()) {
            return null;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(h))) {
            String s = br.readLine();
            if (s == null) return null;
            s = s.trim();
            if (s.length() == 0) return null;
            return s;
        } catch (IOException e) {
            return null;
        }
    }

    public static void main(String[] args) throws IOException {
        initRepo();
        addToIdx(hashFile("git/testFile"), "git/testFile");
        addToIdx(hashFile("git/samples/file1.txt"), "git/samples/file1.txt");
    }
}