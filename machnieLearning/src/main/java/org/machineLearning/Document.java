package org.machineLearning;

import java.io.Serializable;

public class Document implements Serializable {
	  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String text;
	  private Long id;
	  public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Document(Long id,String text) {
		  this.id=id;
	    this.text = text;
	  }

	  public String getText() { return this.text; }
	  public void setText(String text) { this.text = text; }
	}
