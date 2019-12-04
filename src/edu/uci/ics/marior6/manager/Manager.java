package edu.uci.ics.marior6.manager;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class Manager {
    public static final int FRAME_SIZE              = 512;
    public static final int FRAME_NUMBER            = 1024;
    public static final int PHYSICAL_MEMORY_SIZE    = FRAME_NUMBER * FRAME_SIZE;
    public static final int PAGE_FAULT              = -1;
    public static final int EMPTY                   = 0;

    private static BufferedReader reader1           = null;
    private static BufferedReader reader2           = null;
    private static BufferedWriter writer            = null;

    private static int segment;         //s
    private static int page;            //p
    private static int word;            //w
    private static int frame;           //f
    private static int segment_size;    //z
    private static int[] physicalMemory = new int[PHYSICAL_MEMORY_SIZE];
    private static int[] bitMap         = new int[FRAME_NUMBER];
    private static int[][] disk         = new int[FRAME_NUMBER][FRAME_SIZE];
    private static boolean lineChange   = false;

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
            file = new File(path + "\\output-no-dp.txt\\");
            if(file.exists()) {
                if(file.createNewFile()) {
                    System.out.println("Creating new output file...");
                }
            }
            writer = new BufferedWriter(new FileWriter(file));
            System.out.println("Output handler ready...");
            if(args.length < 2) {
                System.out.println("ERROR: Wrong number of parameters! You need the init and input file...");
                System.exit(0);
            } else {
                System.out.println("Input file passed as parameter to program: " + args[0] + ", " + args[1]);
                reader1 = new BufferedReader(new FileReader(args[0]));  //Initialization file
                reader2 = new BufferedReader(new FileReader(args[1]));  //Testing file
                System.out.println("Input handlers ready...");
                String line;
                //go thru initialization file now
                while ((line = reader1.readLine()) != null) {
                    if (!line.isEmpty()) {
                        manager.executeInitialization(line);
                        writer.flush();
                    }
                }
                //System.out.println("Done initializing ST and PT...");
                //go thru input file now
                while ((line = reader2.readLine()) != null) {
                    if (!line.isEmpty()) {
                        manager.executeTranslation(line);
                        writer.flush();
                    }
                }
                manager.display();
                reader1.close();
                reader2.close();
                writer.close();
            }
        }catch (IOException e){
            System.out.println("MAIN: Error opening file!");
            e.printStackTrace();
        }
    }

    private void executeInitialization(String line) {
        String[] line1 = line.split(" ");
        System.out.println(Arrays.toString(line1));
        //initialize ST and PT from files
        if (!lineChange) {
            //initialize ST
            for (int i = 0; i <= line1.length - 3; i = i + 3) {
                segment = Integer.parseInt(line1[i]);
                segment_size = Integer.parseInt(line1[i + 1]);
                frame = Integer.parseInt(line1[i + 2]);
                System.out.println("S: " + segment + ", Z: " + segment_size + ", F: " + frame);
                if (frame < 0) System.out.println("PT is not resident!");
                initST(segment, segment_size, frame);
            }
            lineChange = true;
        } else {
            //initialize PT
            for (int j = 0; j <= line1.length - 3; j = j + 3) {
                segment = Integer.parseInt(line1[j]);
                page = Integer.parseInt(line1[j + 1]);
                frame = Integer.parseInt(line1[j + 2]);
                System.out.println("S: " + segment + ", P: " + page + ", F: " + frame);
                if (frame < 0) System.out.println("Page is not resident!");
                initPT(segment, page, frame);
            }
            lineChange = false;
        }
    }

    private void executeTranslation(String line) throws IOException{
        String[] vAddresses = line.split(" ");
        System.out.println(Arrays.toString(vAddresses));
        //translate VA to PA
        for (String vAddress : vAddresses) {
            int physicalAddress = VAtoSPW(Integer.parseInt(vAddress));
            System.out.println("PA: " + physicalAddress);
            writer.write(physicalAddress + " ");
        }
    }

    private void init() {
        //System.out.println("Starting to initialize data structures...");
        //initialize bitMap (first/second frames are reserved to ST)
        bitMap[0] = 1;
        bitMap[1] = 1;
        for (int i = 2; i < FRAME_NUMBER; i++) {
            bitMap[i] = 0;
        }
        //initialize physicalMemory
        for (int i = 0; i < PHYSICAL_MEMORY_SIZE; i++) {
            physicalMemory[i] = 0;
        }
        //initialize disk
        for (int i = 0; i < FRAME_NUMBER; i++) {
            for (int j = 0; j < FRAME_SIZE; j++) {
                disk[i][j] = 0;
            }
        }
        //System.out.println("DONE initializing data structures!");
    }

    private void initST(int segment, int segment_size, int frame) {
        physicalMemory[2 * segment] = segment_size;
        physicalMemory[(2 * segment) + 1] = frame;
        if (frame >= 0) {
            //int f = findFreeFrame();
            allocateBitmap(frame);
        }
    }

    private void initPT(int segment, int page, int frame) {
        if (physicalMemory[(2 * segment) + 1] < 0) {
            disk[Math.abs(physicalMemory[(2 * segment) + 1])][page] = frame;
        } else {
            physicalMemory[physicalMemory[(2 * segment) + 1] * FRAME_SIZE + page] = frame;
        }
        if (frame >= 0) {
            //int f = findFreeFrame();
            allocateBitmap(frame);
        }
    }

    private int VAtoSPW(int virtualAddress) {
        //System.out.println("Translating from VA: " + virtualAddress + " to S,P,W...");
        segment = virtualAddress >> 18;
        word = virtualAddress & 0x1FF;
        page = (virtualAddress >> 9) & 0x1FF;
        int pw = virtualAddress & 0x3FFFF;

        if (pw >= physicalMemory[2 * segment]) {
            System.out.println("VA is outside the segment boundary! ERROR...");
            return PAGE_FAULT;
        }
        if (physicalMemory[(2 * segment) + 1] < 0) {
            System.out.println("Page Fault - PT is not resident!");
            int f = findFreeFrame();
            if (f == PAGE_FAULT) return PAGE_FAULT;
            readBlock(physicalMemory[(2 * segment) + 1], f * FRAME_SIZE);
            //Update ST entry
            physicalMemory[(2 * segment) + 1] = f;
        }
        if (physicalMemory[physicalMemory[(2 * segment) + 1] * FRAME_SIZE + page] < 0) {
            System.out.println("Page Fault = Page is not resident!");
            int f = findFreeFrame();
            if (f == PAGE_FAULT) return PAGE_FAULT;
            readBlock(physicalMemory[(2 * segment) + 1], f * FRAME_SIZE);
            //Update PT entry
            physicalMemory[physicalMemory[(2 * segment) + 1] * FRAME_SIZE + page] = f;
        }
        return physicalMemory[physicalMemory[(2 * segment) + 1] * FRAME_SIZE + page] * FRAME_SIZE + word;

//        BASIC VERSION
//        if (pw >= physicalMemory[2 * segment]) {
//            System.out.println("VA is outside the segment boundary! ERROR...");
//            return PAGE_FAULT;
//        } else {
//            return physicalMemory[physicalMemory[(2 * segment) + 1] * FRAME_SIZE + page] * FRAME_SIZE + word;
//        }
    }

    private void readBlock(int b, int m) {
        b = Math.abs(b);
        System.arraycopy(disk[b], 0, physicalMemory, m, FRAME_SIZE);
        //System.out.println("Done reading block...");
    }

    private int findFreeFrame() {
        int freeFrame = PAGE_FAULT;
        for (int i = 0; i < bitMap.length; i++) {
            if (bitMap[i] == EMPTY) {
                freeFrame = i;
                break;
            }
        }
        allocateBitmap(freeFrame);
        return freeFrame;
    }

    private void allocateBitmap(int frame) {
        //update frame taken
        bitMap[frame] = 1;
    }

    private void display() {
        System.out.print("BitMap (frames taken): ");
        for (int i = 0; i < bitMap.length; i++) {
            if (bitMap[i] != EMPTY) {
                System.out.print(i + " ");
            }
        }
        //System.out.println("PA: " + Arrays.toString(physicalMemory));
        //System.out.println("BM: " + Arrays.toString(bitMap));
    }
}