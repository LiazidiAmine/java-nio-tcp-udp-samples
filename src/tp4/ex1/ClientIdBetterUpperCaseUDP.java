package tp4.ex1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class ClientIdBetterUpperCaseUDP {

    public static void usage() {
        System.out.println("ClientIdUpperCaseUDP charset filename dest port");
    }

    public static List<String> readLinesFromFile(String filename, String charsetName) throws IOException {
        File file = new File(filename);
        try (InputStream inputStream = new FileInputStream(file);
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, charsetName);
             BufferedReader br = new BufferedReader(inputStreamReader);) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        }
    }

    public static void writeLinesToFile(List<String> lines, String filename, String charsetName) throws IOException {
        File file = new File(filename);
        try (OutputStream outputStream = new FileOutputStream(file);
             OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, charsetName);
             BufferedWriter bw = new BufferedWriter(outputStreamWriter);) {
            for (String line : lines) {
                bw.write(line+"\n");
                System.out.println("write "+line);
            }
        }
    }

    public static void checkFiles(String lowercase,String uppercase,String charsetName) throws IOException {
                List<String> lcLines = readLinesFromFile(lowercase,charsetName);
                List<String> ucLines = readLinesFromFile(uppercase,charsetName);
        if (lcLines.size()!=ucLines.size()){
            System.out.println("The two files have a different number of lines.");
            System.out.println("\t"+lowercase+" : "+lcLines.size());
            System.out.println("\t"+uppercase+" : "+ucLines.size());
           return;
        }
        boolean bug=false;
                for(int i = 0;i<lcLines.size();i++){
                    if (!lcLines.get(i).toUpperCase().equals(ucLines.get(i))) {
                        System.out.println("Problem on line "+i);
                        System.out.println("\t"+lowercase+" : "+lcLines.get(i));
                        System.out.println("\t"+uppercase+" : "+ucLines.get(i));
                        bug=true;
                    }
                }
        if (!bug) {
            System.out.println("Everything is perfect!");
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 4) {
            usage();
            return;
        }
        String txt = "/home/amine/workspace/javanetworktp/src/text.txt";
        InetSocketAddress dest = new InetSocketAddress(args[2],Integer.parseInt(args[3]));
        List<String> lines = readLinesFromFile(txt, args[0]);
        System.out.println("****************** CLIENT ********************");
        System.out.println("********* readLinesFromFile ******************");
        lines.stream().forEach(System.out::println);
        System.out.println("**********************************************");
        
        Requester requester = new OneByOneRequester(dest);
        requester.open();
        
        List<String> linesUpperCase=requester.toUpperCase(lines,Charset.forName(args[0]));
        System.out.println("[CLIENT] requester.toUpperCase() finished");
        
        
        requester.close();
        System.out.println("****************** CLIENT ********************");
        System.out.println("********* writeLinesToFile ******************");
        writeLinesToFile(linesUpperCase, txt + ".UPPERCASE", args[0]);
        linesUpperCase.stream().forEach(System.out::println);
        System.out.println("**********************************************");
        checkFiles(txt,txt+".UPPERCASE",args[0]);
    }


}
