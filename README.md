Virtual Memory Manager
------------------------

Description: This project implements a virtual memory (VM) system using segmentation and paging. The system manages the necessary segment and page tables (PTs) in a simulated main memory. It accepts virtual addresses (VAs) and translates them into physical addresses (PAs) according to the current contents of the segment and PTs. Two versions of the VM manager can be implemented: The simpler version assumes that the entire VA space in resident in physical memory (PM). The second supports demand paging.

*Note*: This program is implemented using ****demand paging!**** 

How to run:

1. Download the jar file
2. Open command line and run the jar file like (java -jar programName initFile inputFile): "java -jar Project2-VirtualMemoryManager.jar init.txt input.txt"
3. Output file should be created in the current working directory