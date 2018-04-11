package il.ac.telhai.os.hardware;

import java.util.HashMap;

import il.ac.telhai.os.software.InterruptHandler;
import il.ac.telhai.os.software.OperatingSystem;
import il.ac.telhai.os.software.Software;
import il.ac.telhai.os.software.language.*;

/**
 * 
 * @author cmshalom
 */
public class CPU implements Clockeable {

	private Clock clock;
	private Registers registers = new Registers();
	private Memory realMemory;
	private Software running;
	private HashMap<Class<? extends InterruptSource>, InterruptHandler> interruptVector = 
			new HashMap<Class<? extends InterruptSource>, InterruptHandler>(); 
	private InterruptSource pendingInterrupt;

	public CPU (Clock clock, RealMemory realMemory) {
		this.clock = clock; 
		clock.addDevice(this);
		this.realMemory = realMemory;
	}

	private boolean halted() {
		boolean ret = registers.getFlag(Registers.FLAG_HALTED);
		if (ret) clock.shutdown();
		return ret;
	}


	@Override
	public void tick() {
		if (halted()) return;

		if (pendingInterrupt != null) {
			InterruptSource source = pendingInterrupt;
			pendingInterrupt = null;
			InterruptHandler handler = getHandler(source);
			if (handler != null) {
				registers.setFlag(Registers.FLAG_USER_MODE, false);
				handler.handle(source);
				if (halted()) return;
			}
		}

		if (running != null) {
			// This allows us to run either an Operating system written in Java,
			// or a program written in an Assembly Language
			if (running instanceof OperatingSystem) { 
				((OperatingSystem) running).step();
			} else {
				try {
					ProgramLine programLine = ((Program)running).fetchLine(registers);
					programLine.execute(registers, realMemory);
				} catch (SystemCall call) {
					interrupt(call);
				}
			}
		}
	}

	public void execute(Instruction instruction) {
		instruction.execute(registers, realMemory);
	}

	public void setInterruptHandler(Class<? extends InterruptSource> cls, InterruptHandler handler) {
		interruptVector.put(cls , handler);
	}

	private InterruptHandler getHandler(InterruptSource source) {
		InterruptHandler result = null;
		@SuppressWarnings("rawtypes")
		Class cls = (Class) source.getClass();
		while (result == null && cls != null) {
			result = interruptVector.get(cls);
			cls = cls.getSuperclass();
		}
		return result;
	}

	public void interrupt(InterruptSource source) {
		pendingInterrupt = source;
	}

	public void contextSwitch (Software software, Registers registers) {
		running = software;
		if (registers != null) this.registers = registers;
	}

	public String getRegisters() {
		return registers.toString();
	}

}