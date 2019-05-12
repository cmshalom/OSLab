package il.ac.telhai.os.software;

import org.apache.log4j.Logger;

import il.ac.telhai.os.hardware.InterruptSource;
import il.ac.telhai.os.hardware.MMU;
import il.ac.telhai.os.hardware.PageFault;
import il.ac.telhai.os.hardware.PageTableEntry;

public class VMM implements InterruptHandler {
	private static final Logger logger = Logger.getLogger(VMM.class);

	private MMU mmu;
	private int numberOfRealSegments;

	public VMM (MMU mmu) {
		this.mmu = mmu;
		numberOfRealSegments = mmu.getNumberOfSegments(); // This works since the mmu is initially in real mode
		initMemoryFreeList();
	}

	private void initMemoryFreeList() {
		// TODO: Initialize the list of free real memory segments
		logger.info("Real Memory Initialized");
	}

	@Override
	public void handle(InterruptSource source) {
		PageFault fault = (PageFault) source;
		PageTableEntry entry = fault.getEntry();
		// TODO: Get a free segment from the free list
		//       modify entry sppropriately
	}

}
