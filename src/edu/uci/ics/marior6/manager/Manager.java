package edu.uci.ics.marior6.manager;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

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
            if(args.length < 2) {
                System.out.println("Wrong number of parameters! Error...");
                System.exit(0);
            } else {
                System.out.println("Input file passed as parameter to program..." + args[0]);
                reader1 = new BufferedReader(new FileReader(args[0]));  //Initialization file
                reader2 = new BufferedReader(new FileReader(args[1]));  //Testing file
                System.out.println("Input handlers ready...");
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
            System.out.println("MAIN: Error opening file!");
            e.printStackTrace();
        }
    }

    private void executeArguments(String line) {

    }

    private void init() {
        System.out.println("Starting to initialize data structures...");
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
        System.out.println("DONE initializing data structures!");
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
        System.out.println("Translating from VA to S,P,W...");
        for (int i = 0; i <= 9; i++) {
            if(i == 9 && (virtualAddress & mask[22]) != EMPTY) {
                page = page | mask[31];
                break;
            }
            //Since each entry consists of 2 integers and the ST resides in frame
            //0 and 1, slots from 0 to 3 are taken so we start at slot 4
            if((virtualAddress & mask[i + 4]) != EMPTY) {
                segment = segment | mask[i + 23];
            }
            if((virtualAddress & mask[i + 13]) != EMPTY) {
                page = page | mask[i + 22];
            }
            if((virtualAddress & mask[i + 23]) != EMPTY) {
                word = word | mask[i + 23];
            }
        }
    }

    private int read() {
        int p = physicalMemory[segment];
        int w = physicalMemory[physicalMemory[segment] + page];
        if (p == PAGE_FAULT || p == EMPTY) {    //check for pageFault or empty segment
            System.out.println("Read: Error - 'p' page fault");
            //FIXME: return error
        }
        if (w == PAGE_FAULT || w == EMPTY) {    //check for pageFault or empty page
            System.out.println("Read: Error - 'w' page fault");
            //FIXME: return error
        }
        //TODO: return physicalAddress (PM[PM[s] + p] + w)
        return w + word;
    }

    private int write() {
        int p = physicalMemory[segment];
        int w = physicalMemory[physicalMemory[segment] + page];
        if (p != PAGE_FAULT) {  //check for pageFault
            if (p == EMPTY) {
                p = allocatePageTable();
            }
        } else {
            System.out.println("Write: Error - 'p' page fault");
            //FIXME: return error
        }

        if (w != PAGE_FAULT) {  //check for pageFault
            if (w == EMPTY) {
                w = allocateData();
            }
        } else {
            System.out.println("Write: Error - 'w' page fault");
            //FIXME: return error
        }
        //TODO: return physicalAddress (PM[PM[s] + p] + w)
        return w + word;
    }

    private void allocateBitmap(int frame) {
        if(frame != PAGE_FAULT) {
            for (int i = 0; i < FRAME_SIZE; i++) {
                physicalMemory[frame + i] = 0;
            }
            frame = frame / FRAME_SIZE;
            bitMap[frame / BITMAP_SIZE] = (bitMap[frame / BITMAP_SIZE] | mask[frame % BITMAP_SIZE]);
        }
    }

    private int allocateData() {
        int frame = EMPTY;
        for (int i = 0; i < FRAME_NUMBER; i++) {
            if ((bitMap[i / BITMAP_SIZE] & mask[i % BITMAP_SIZE]) == EMPTY) {
                frame = i * FRAME_SIZE;
                allocateBitmap(frame);
                break;
            }
        }
        return frame;
    }

    private int allocatePageTable() {
        int frame1 = EMPTY;
        int frame2 = EMPTY;
        for (int i = 0; i < FRAME_NUMBER; i++) {
            //check if bit is empty
            if ((bitMap[i / BITMAP_SIZE] & mask[i % BITMAP_SIZE]) == EMPTY) {
                //now check next bit if it is empty
                if ((bitMap[(i + 1) / BITMAP_SIZE] & mask[i + 1 % BITMAP_SIZE]) == EMPTY) {
                    frame1 = i * FRAME_SIZE;
                    frame2 = (i + 1) * FRAME_SIZE;
                    allocateBitmap(frame1);
                    allocateBitmap(frame2);
                }
            }
        }
        return frame1;
    }

    private void displayBitmap() {
        System.out.println("BitMap: " + Arrays.toString(bitMap));
    }
}
