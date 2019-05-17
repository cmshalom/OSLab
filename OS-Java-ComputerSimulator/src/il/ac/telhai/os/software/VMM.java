package il.ac.telhai.os.software;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.log4j.Logger;

import il.ac.telhai.os.hardware.InterruptSource;
import il.ac.telhai.os.hardware.MMU;
import il.ac.telhai.os.hardware.PageFault;
import il.ac.telhai.os.hardware.PageTableEntry;

public class VMM implements InterruptHandler {
	private static final Logger logger = Logger.getLogger(VMM.class);

	private MMU mmu;
	private int numberOfRealSegments;
	private Queue<Integer> freeMemoryPages = null;

	public VMM (MMU mmu) {
		this.mmu = mmu;
		numberOfRealSegments = mmu.getNumberOfSegments(); // This works since the mmu is initially in real mode
		initMemoryFreeList();
	}

	private void initMemoryFreeList() {
		logger.info("Initializing Real Memory");
		freeMemoryPages = new LinkedList<Integer>();
		for (int i=1; i < this.numberOfRealSegments; i++) {
			freeMemoryPages.add(i);
		}
		logger.info("Real Memory Initialized");
	}

	private int getFreePage() {
		int result = freeMemoryPages.remove();
		logger.info("Allocating segment " + result);
		return result;
	}
	
	@Override
	public void handle(InterruptSource source) {
		PageFault fault = (PageFault) source;
		PageTableEntry entry = fault.getEntry();
		entry.setSegmentNo(getFreePage());
		entry.setMappedToMemory(true);			
	}
	
	void shutdown( ) {
		logger.info("Free Memory: " + freeMemoryPages.size() + " pages.");
	}

}
