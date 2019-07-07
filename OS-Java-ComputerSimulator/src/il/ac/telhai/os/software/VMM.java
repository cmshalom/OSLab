package il.ac.telhai.os.software;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import il.ac.telhai.os.hardware.InterruptSource;
import il.ac.telhai.os.hardware.MMU;
import il.ac.telhai.os.hardware.PageFault;
import il.ac.telhai.os.hardware.PageTableEntry;
import il.ac.telhai.os.software.language.Register;

public class VMM implements InterruptHandler {
	private static final Logger logger = Logger.getLogger(VMM.class);

	private MMU mmu;
	private int numberOfRealSegments;
	private Map<Integer, Integer>  sharedMemorySegments = new HashMap<Integer, Integer>();


	public VMM (MMU mmu) {
		this.mmu = mmu;
		numberOfRealSegments = mmu.getNumberOfSegments(); // This works since the mmu is initially in real mode
		if (mmu.getSegmentSize() < numberOfRealSegments) {
			throw new IllegalStateException("Free bitmap cannot be stored in one page");
		}
		initMemoryFreeList();
	}

	private void initMemoryFreeList() {
		logger.info("Initializing Real Memory");
		mmu.enterRealMode();
		mmu.writeByte(0, 0, (byte) 1); // Page 0 is in use
		for (int i=1; i< numberOfRealSegments; i++) {
			mmu.writeByte(0, i, (byte) 0); 
		}
		mmu.exitRealMode();
		logger.info("Real Memory Initialized");
	}

	/**
	 * Side effect: exits from real mode upon exit
	 * @return
	 */
	private int getFreePage() {
		mmu.enterRealMode();
        for (int i=1; i<numberOfRealSegments; i++) {
            if (mmu.readByte(0, i) == 0) {
            	mmu.writeByte(0, i, (byte) 1); 
        		mmu.exitRealMode();
        		logger.trace("Allocating segment " + i);
        		return i;            	
            }
        }
        throw new IllegalStateException("Real Memory Full");
	}
	
	public PageTableEntry[] clonePageTable(PageTableEntry[] pageTable) {
		mmu.enterRealMode();
		PageTableEntry[] ret = new PageTableEntry[pageTable.length];
		for (int i = 0; i < pageTable.length; i++) {
			ret[i] = new PageTableEntry(pageTable[i]);
			if (ret[i].isMappedtoMemory() || ret[i].isMappedtoDisc()) {
				ret[i].setCopyOnWrite(true);			
				pageTable[i].setCopyOnWrite(true);
				incRefCount(ret[i].getSegmentNo());
			}
		}
		mmu.exitRealMode();
		return ret;
	}

	/**
	 * Increments reference count
	 * Side effect: exits from real mode upon exit
	 * @param segmentNo
	 */
	private void incRefCount(int segmentNo) {
		mmu.enterRealMode();
		byte refCount = mmu.readByte(0, segmentNo);
		if (refCount==127) {
			throw new IllegalStateException("Reference count reached 127 in page sharing");
		}
		logger.trace("Sharing segment " + segmentNo + ", refcnt=" + (refCount+1));
		mmu.writeByte(0, segmentNo, (byte) (refCount+1));
		mmu.exitRealMode();
	}

	/**
	 * Decrements reference count
	 * Side effect: exits from real mode upon exit
	 * @param segmentNo
	 */
	private void decRefCount(int segmentNo) {
		mmu.enterRealMode();
		byte refCount = mmu.readByte(0, segmentNo);
		if (refCount==0) {
			throw new IllegalStateException("Reference count reached zero in page unsharing");
		}
		logger.trace("Releasing segment " + segmentNo + " ,refcnt=" + (refCount-1));
		mmu.writeByte(0, segmentNo, (byte) (refCount-1));
		mmu.exitRealMode();
	}

	
	public void releasePageTable (PageTableEntry[] pageTable) {
		mmu.enterRealMode();
		for (int i=0; i<pageTable.length; i++) {
            PageTableEntry e = pageTable[i];
            pageTable[i] = null;
			if (e.isMappedtoMemory()) {
				decRefCount(e.getSegmentNo());
			}
		}
		mmu.exitRealMode();
	}


	@Override
	public void handle(InterruptSource source) {
		PageFault fault = (PageFault) source;
		PageTableEntry entry = fault.getEntry();
		assert(entry != null);
		if (entry.isMappedtoMemory()) {
			mmu.enterRealMode();
			byte refCount = mmu.readByte(0, entry.getSegmentNo());
			if (refCount > 1) {  // There are at least one more process sharing the page
				mmu.writeByte(0, entry.getSegmentNo(), (byte) (refCount-1));
				int newSegment = getFreePage();  // Recall that getFreePage exits realMode
				logger.trace("Copying segment " + entry.getSegmentNo() + " to segment " + newSegment);
				mmu.copySegment(newSegment, entry.getSegmentNo());
				entry.setSegmentNo(newSegment);				
			}
			mmu.exitRealMode();
			entry.setCopyOnWrite(false);
		} else {
			entry.setSegmentNo(getFreePage());
			entry.setMappedToMemory(true);			
		}
	}

	int shmGet (int key) {
		if (sharedMemorySegments.containsKey(key)) 	return sharedMemorySegments.get(key);

		int id = this.getFreePage();
		sharedMemorySegments.put(key, id);
		return id;
	}

	void shmAt (ProcessControlBlock process, int id) {
		PageTableEntry[] pageTable = process.pageTable;
		if (sharedMemorySegments.containsValue(id)) {
			for (int i= 0; i < pageTable.length; i++) {
				if (!pageTable[i].isMappedtoMemory() && !pageTable[i].isMappedtoDisc()) {
					pageTable[i].setMappedToMemory(true);
					pageTable[i].setSegmentNo(id);
					incRefCount(id);
					process.registers.set(Register.AX, 0);
					process.registers.set(Register.DS, i);
					return;
				}
			}
		}
		process.registers.set(Register.AX, -1);			
	}

	void shmDt (ProcessControlBlock process, int entryNo) {
		PageTableEntry e = process.pageTable[entryNo];
		if (!e.isMappedtoMemory() || 
		    !sharedMemorySegments.containsValue(e.getSegmentNo())) {
			process.registers.set(Register.AX, -1);
		} else {
			decRefCount(e.getSegmentNo());
			process.pageTable[entryNo] = new PageTableEntry();
			process.registers.set(Register.AX, 0);
		}
	}

	void shutdown( ) {
		mmu.enterRealMode();
		int numberOfFreePages = 0;
        for (int i=1; i<numberOfRealSegments; i++) {
            if (mmu.readByte(0, i) == 0) numberOfFreePages++;
        }
		mmu.exitRealMode();
		logger.info("Shared Memory Segments: ");
		for (Entry<Integer, Integer> e : sharedMemorySegments.entrySet()) {
			logger.info("Key:" + e.getKey() + ", ID:" + e.getValue());
		}
		logger.info("Free Memory: " + numberOfFreePages + " pages.");
	}

}
