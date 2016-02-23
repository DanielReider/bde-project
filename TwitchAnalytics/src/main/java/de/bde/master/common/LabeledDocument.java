package de.bde.master.common;

import java.io.Serializable;

public class LabeledDocument extends Document implements Serializable {
	private static final long serialVersionUID = 1L;
	private double target;
	  
	  public LabeledDocument(Long id, String text, double target) {
	    super(id,text);
	    this.target = target;
	  }

	  public double getTarget() { return this.target; }
	  public void setTarget(double target) { this.target = target; }
	}
