package edu.purdue.cs.toydroid.soot.util.toDex.instructions;

import org.jf.dexlib2.Opcode;

/**
 * Abstract base class for all payloads (switch, fill-array) in dex instructions
 * 
 * @author Steven Arzt
 *
 */
public abstract class AbstractPayload extends InsnWithOffset {

	public AbstractPayload() {
		super(Opcode.NOP);
	}

}
