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

    // public static void writeFile() {
        
    
    // }

    public static void main(String[] args) throws IOException {
        initRepo();
        System.out.println(verify());
        cleanUp();
        reset();

        File sampleDir = new File("samples");
        sampleDir.mkdirs();
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("samples/file1.txt"))) {
            bufferedWriter.write("Hello world");
        }
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("samples/file2.txt"))) {
            bufferedWriter.write("Hello world again");
        }
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("samples/file3.txt"))) {
            bufferedWriter.write("Hello world again again");
        }
        blob("samples/file1.txt");
        blob("samples/file2.txt");
        blob("samples/file3.txt");
        addToIdx(hashFile("samples/file1.txt"), "samples/file1.txt");
        addToIdx(hashFile("samples/file2.txt"), "samples/file2.txt");
        addToIdx(hashFile("samples/file3.txt"), "samples/file3.txt");
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

        // add stuff to the samples folder
        File dirOne = new File("samples/dirOne");
        File dirTwo = new File("samples/dirOne/dirTwo");
        File dirThree = new File("samples/dirThree");

        dirOne.mkdir();
        dirTwo.mkdir();
        dirThree.mkdir();

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("samples/dirOne/file1.txt"))) {
            bufferedWriter.write("Hello world");

        }

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("samples/dirOne/file2.txt"))) {
            bufferedWriter.write("Hello world again");
        }

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("samples/dirOne/dirTwo/file1.txt"))) {
            bufferedWriter.write("Hello world");
        }

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("samples/dirThree/file1.txt"))) {
            bufferedWriter.write("Hello world");
        }

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("samples/dirThree/file2.txt"))) {
            bufferedWriter.write("Hello world again");
        }

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("samples/dirThree/file3.txt"))) {
            bufferedWriter.write("Hello world again again");
        }

        blob("samples/dirOne/file1.txt");
        addToIdx(hashFile("samples/dirOne/file1.txt"), "samples/dirOne/file1.txt");
        blob("samples/dirOne/file1.txt");
        addToIdx(hashFile("samples/dirOne/file2.txt"), "samples/dirOne/file2.txt");
        blob("samples/dirOne/dirTwo/file1.txt");
        addToIdx(hashFile("samples/dirOne/dirTwo/file1.txt"), "samples/dirOne/dirTwo/file1.txt");
        blob("samples/dirThree/file1.txt");
        addToIdx(hashFile("samples/dirThree/file1.txt"), "samples/dirThree/file1.txt");
        blob("samples/dirThree/file2.txt");
        addToIdx(hashFile("samples/dirThree/file2.txt"), "samples/dirThree/file2.txt");
        blob("samples/dirThree/file3.txt");
        addToIdx(hashFile("samples/dirThree/file3.txt"), "samples/dirThree/file3.txt");

        // check to see if genTree() works
        System.out.println("==genTree()==");
        System.out.println(Git.genTree("samples")); // all blobs are correct
        System.out.println();

        // check to see if genTreesFromIdx() works
        System.out.println("==genTree()==");
        System.out.println(Git.genTreesFromIdx()); // all blobs are correct
        System.out.println();

        cleanUp();

        // TESTING GIT WRAPPER NOW
        GitWrapper gw = new GitWrapper();

        System.out.println("==init()==");
        try {
            gw.init();
            System.out.println("init() works as expected.");
        } catch (Exception e) {
            System.out.println("init() did not function as expected.");
        }
        System.out.println();

        System.out.println("==add()==");
        File myProgram = new File("myProgram");
        File inner = new File("myProgram/inner");
        myProgram.mkdir();
        inner.mkdir();

        File fileHello = new File("myProgram/hello.txt");
        File fileWorld = new File("myProgram/inner/world.txt");
        fileHello.createNewFile();
        fileWorld.createNewFile();

        try {
            gw.add("myProgram/hello.txt");
            gw.add("myProgram/inner/world.txt");
            System.out.println("add() works as expected.");
        } catch (Exception e) {
            System.out.println("add() did not function as expected");
        }
        System.out.println();

        System.out.println("==commit()==");
        try {
            System.out.println("Initial commit: " + gw.commit("John Doe", "Initial commit"));

            try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("myProgram/hello.txt"))) {
                bufferedWriter.write("Hello, World!");
            }
    
            blob("myProgram/hello.txt");
            addToIdx(hashFile("myProgram/hello.txt"), "myProgram/hello.txt");

            System.out.println("Second commit: " + gw.commit("Sophia", "Second commit"));
            System.out.println("commit() works as expected.");
        } catch (Exception e) {
            System.out.println("commit() did not function as expected");
        }
        System.out.println();

    }

}
