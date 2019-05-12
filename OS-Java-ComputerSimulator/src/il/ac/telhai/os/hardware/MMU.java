package il.ac.telhai.os.hardware;

import org.apache.log4j.Logger;

import il.ac.telhai.os.hardware.Memory;
import il.ac.telhai.os.hardware.RealMemory;

/**
 * 
 * @author cmshalom
 * This is a partial implementation of an MMU
 * It has a manual write-through cache of size of one segment it
 *
 */
public class MMU implements Memory {
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(MMU.class);

	private RealMemory memory;
	private PageTableEntry[] pageTable=null; 

	public MMU (RealMemory memory, int numberOfPages) {
		if (memory.getSegmentSize() < 2 * numberOfPages) {
			throw new IllegalArgumentException("Page table does not fit to one page");
		}
		this.memory = memory;
	}

	public void setRealMode () {
		pageTable = null;		
	}
	
	public void setPageTable (PageTableEntry[] pageTable) {
		this.pageTable = pageTable; 		
	}
	
	public PageTableEntry[] getPageTable() {
		return pageTable;
	}
	
	@Override
	public int getSegmentSize() {
		return memory.getSegmentSize();
	}

	@Override
	public int getNumberOfSegments() {
		return pageTable == null ? memory.getNumberOfSegments() : pageTable.length;
	}

	@Override
	public byte readByte(int pageNo, int offset) {
		// TODO: Translate the pageNumber according to pageTable (if it exists),
		//       Then perform the operation using the memory
		return memory.readByte(pageNo, offset);
	}

	@Override
	public void writeByte(int pageNo, int offset, byte value) {
		// TODO: Translate the pageNumber according to pageTable (if it exists),
		//       Then perform the operation using the memory
		memory.writeByte(pageNo, offset, value);
	}

	@Override
	public int readWord(int pageNo, int offset) {
		// TODO: Translate the pageNumber according to pageTable (if it exists),
		//       Then perform the operation using the memory
		return memory.readWord(pageNo, offset);
	}

	@Override
	public void writeWord(int pageNo, int offset, int value) {
		// TODO: Translate the pageNumber according to pageTable (if it exists),
		//       Then perform the operation using the memory
		memory.writeWord(pageNo, offset, value);
	}

}
