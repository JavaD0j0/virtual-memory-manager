package edu.uci.ics.marior6.manager;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Manager {
    public static final int SEGMENT_SIZE            = 512;
    public static final int PAGE_SIZE               = 1024;
    public static final int WORD_SIZE               = 512;
    public static final int FRAME_SIZE              = 512;
    public static final int FRAME_NUMBER            = 1024;
    public static final int PHYSICAL_MEMORY_SIZE    = FRAME_NUMBER * FRAME_SIZE;
    public static final int PHYSICAL_ADDRESS_SIZE   = 19;
    public static final int BITMAP_SIZE             = 32;
    public static final int BITMAP_NUMBER           = FRAME_NUMBER / BITMAP_SIZE;
    public static final int PAGE_FAULT              = -1;
    public static final int EMPTY                   = 0;

    private static BufferedReader reader1           = null;
    private static BufferedReader reader2           = null;
    private static BufferedWriter writer            = null;

    private static int segment;         //s
    private static int page;            //p
    private static int word;            //w
    private static int segment_page;    //sp
    private static int[] physicalMemory = new int[PHYSICAL_MEMORY_SIZE];
    private static int[] bitMap         = new int[BITMAP_SIZE];
    private static int[] mask           = new int[BITMAP_SIZE];

    public static void main(String[] args) {
        System.out.println("Starting virtual memory manager...");
        Manager manager = new Manager();
        File file;
        Path currentRelativePath = Paths.get("");
        String path = currentRelativePath.toAbsolutePath().toString();
        System.out.println("Current relative path: " + path);
        //initialize data structures
        manager.init();

        try{
            file = new File(path + "\\output.txt\\");
            if(file.exists()) {
                if(file.createNewFile()) {
                    System.out.println("Creating new output file...");
                }
            }
            writer = new BufferedWriter(new FileWriter(file));
            System.out.println("Output handler ready...");
            if(args.length == 0) {
                System.out.println("There are no parameters! Error...");
                System.exit(0);
            } else {
                System.out.println("Input file passed as parameter to program..." + args[0]);
                reader1 = new BufferedReader(new FileReader(args[0]));
                System.out.println("Input handler ready...");
                String line;
                while ((line = reader1.readLine()) != null) {
                    if (!line.isEmpty()) {
                        manager.executeArguments(line);
                        writer.flush();
                    }
                }
                reader1.close();
                reader2.close();
                writer.close();
            }
        }catch (IOException e){
            System.out.println("Main: Error opening file!");
            e.printStackTrace();
        }
    }

    private void executeArguments(String line) {

    }

    private void init() {
        //initialize mask
        for (int i = BITMAP_SIZE - 1; i >= 0; i--) {
            mask[i] = 1 << (31 - i);
        }
        //initialize bitMap (first/second frames are reserved to ST)
        bitMap[0] = mask[0];
        bitMap[1] = mask[1];
        for (int i = 2; i < BITMAP_SIZE; i++) {
            bitMap[i] = 0;
        }
        //initialize physicalMemory
        for (int i = 0; i < PHYSICAL_MEMORY_SIZE; i++) {
            physicalMemory[i] = 0;
        }
        System.out.println("Done initializing data structures!");
    }

//    private void init(int segment, int frame) {
//        physicalMemory[segment] = frame;
//        if(frame != PAGE_FAULT) {
//            //the pageTable takes two frames
//            allocateBitmap(frame);
//            allocateBitmap(frame + 1);
//        }
//    }
//
//    private void init(int segment, int page, int frame) {
//        physicalMemory[physicalMemory[segment] + page] = frame;
//        if(frame != PAGE_FAULT) {
//            allocateBitmap(frame);
//        }
//    }

    private void VAtoSPW(int virtualAddress) {

    }

    private void SPWtoVA(int segment, int page, int word) {

    }

//    private void allocateBitmap(int frame) {
//        if(frame != PAGE_FAULT) {
//            for (int i = 0; i < FRAME_SIZE; i++) {
//                physicalMemory[frame + i] = 0;
//            }
//            frame = frame / FRAME_SIZE;
//            bitMap[frame / BITMAP_SIZE] = (bitMap[frame / BITMAP_SIZE] | mask[frame % BITMAP_SIZE]);
//        }
//    }
}
