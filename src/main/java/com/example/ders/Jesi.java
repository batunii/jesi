package com.example.ders;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class Jesi {

    static HashMap<String, HashMap<String,Integer>> fileIndex = new HashMap<>();
    Dexter dexter = new Dexter();
    private String parsePDF(String filePath) throws IOException, TikaException, SAXException {
        BodyContentHandler bodyContentHandler = new BodyContentHandler();
        File file = new File(filePath);
        FileInputStream inputStream = new FileInputStream(file);
        Metadata data = new Metadata();
        ParseContext contex = new ParseContext();
        PDFParser parser = new PDFParser();
        parser.parse(inputStream, bodyContentHandler, data, contex);

        return bodyContentHandler.toString();
    }

    public void index(String folderPath) throws TikaException, IOException, SAXException {
        File directory = new File(folderPath);
        File[] files = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile() &&
                        pathname.getAbsolutePath()
                                .toLowerCase().endsWith(".pdf");
            }
        });
        assert files != null;
        for (File file : files) {
            try {
                String parsed = parsePDF(file.getAbsolutePath());
                dexter.indexFile(parsed, file.getName());
                System.out.println("Indexing file: " + file.getName() + "...");
            }
            //System.out.println(fileIndex);
            catch (Exception e) {
                System.out.println("ERROR : In reading file : " +
                        file.getName() + " due to: " + e.getMessage());
            }

        }
        fileIndex = dexter.getFileIndex();
    }

    public TreeMap<String, Double> search(String searchTerm)
    {
        HashMap<String, Double>score = new HashMap<>();
        ArrayList<String> tokens = (new Alexar()).tokenize(searchTerm);

            for(String fileName : fileIndex.keySet())
            {
                System.out.println(fileName);
                for(String token : tokens)
                {
                    if(fileIndex.get(fileName).containsKey(token)) {
                        double tf = tf(fileIndex.get(fileName).get(token)
                                , fileIndex.get(fileName).size());
                        double idf = idf(token);
                        double tfIdf = tf * idf;
                        score.computeIfPresent(fileName,
                                (k, v) -> v + tfIdf);
                        score.putIfAbsent(fileName, tfIdf);
                        System.out.println(token + " => "+ tfIdf);
                    }
                }
            }

        final TreeMap<String, Double>rankings = new TreeMap<>(compare(score));
        System.out.println(score);
        rankings.putAll(score);
        return rankings;
    }

    private double tf(int t, int d )
    {
        return  (double) t/d;

    }
    private double idf(String token)
    {
        double numerator = fileIndex.size();
        double denominator = fileIndex.keySet().stream()
                .filter(e->fileIndex.get(e).containsKey(token)).count()+1;

        //System.out.println(token+"=>"+numerator + " " + denominator);

        return Math.log10(Math.max(numerator/denominator, 1));
    }

    private static Comparator<String> compare(HashMap<String, Double>map)
    {
        return ((o1, o2) -> (int)(map.get(o2)-map.get(o1)));
    }
}
