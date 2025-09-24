import java.io.*;

public class Git {

    public static void initRepo() throws IOException {
        File f1 = new File("git");
        if(!f1.exists()) f1.mkdir();
        File f2 = new File("git/objects");
        if(!f2.exists()) f2.mkdir();
        File f3 = new File("git/index");
        if(!f3.exists()) f3.createNewFile();
        File f4 = new File("git/HEAD");
        if(!f4.exists()) f4.createNewFile();
        if(f1.exists() && f2.exists() && f3.exists() && f4.exists()) System.out.println("Git Repository Already Exists");
        else System.out.println("Git Repository Created");
    }
    public static void main(String[] args) throws IOException {
        initRepo();
    }
}