package il.ac.telhai.os.software;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.log4j.Logger;

public class Signaller {
	private static final Logger logger = Logger.getLogger(Signaller.class);

	private ProcessControlBlock process;
	private int[] handlers = new int[Signal._NSIG];
	private Queue<Signal>  pending = new LinkedList<Signal>();

	public Signaller(ProcessControlBlock process) {
		this.process = process;
		// TODO: (not for students) Should inherit signal handling from parent
	}


//		logger.trace("Setting " + signum + " handler to " + handler);
//   	logger.info(s + " ignored");						
//	    logger.info(s + " terminating");						
//   	logger.info("Handling " + s);

}