package il.ac.telhai.os.hardware;

import il.ac.telhai.os.software.language.*;

/**
 * 
 * @author cmshalom
 */
public class CPU implements Clockeable {

	private Clock clock;
	private Registers registers = new Registers();
	private Memory realMemory;
    private Program program;

	public CPU (Clock clock, RealMemory realMemory) {
		this.clock = clock; 
		clock.addDevice(this);
		this.realMemory = realMemory;
	}

	@Override
	public void tick() {
		if (registers.getFlag(Registers.FLAG_HALTED)) {
			clock.shutdown();
			return;
		}
		Instruction instruction = program.fetchLine(registers);
		instruction.execute(registers, realMemory);
	}

	public void execute(Instruction instruction) {
	    instruction.execute(registers, realMemory);
	}

	public void interrupt(InterruptSource source) {
	}

	public void contextSwitch (Program program) {
		this.program = program;
	}
	
	public String getRegisters() {
		return registers.toString();
	}
	
}