package org.jetume.nestedinterval;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class JpaEntity {
	@Id
	@GeneratedValue
	private int id;
	@Column(updatable = false)
	private int a11;
	@Column(updatable = false)
	private int a12;
	@Column(updatable = false)
	private int a21;
	@Column(updatable = false)
	private int a22;
	@Column(updatable = false)
	private int childnum;
	private String rootid;
	private String name;

	public int getId() {
		return id;
	}

	public int getA11() {
		return a11;
	}

	public void setA11(int a11) {
		this.a11 = a11;
	}

	public int getA12() {
		return a12;
	}

	public void setA12(int a12) {
		this.a12 = a12;
	}

	public int getA21() {
		return a21;
	}

	public void setA21(int a21) {
		this.a21 = a21;
	}

	public int getA22() {
		return a22;
	}

	public void setA22(int a22) {
		this.a22 = a22;
	}

	public int getChildnum() {
		return childnum;
	}

	public void setChildnum(int childnum) {
		this.childnum = childnum;
	}

	public String getRootid() {
		return rootid;
	}

	public void setRootid(String rootid) {
		this.rootid = rootid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "JpaEntity [id=" + id + ", a11=" + a11 + ", a12=" + a12
				+ ", a21=" + a21 + ", a22=" + a22 + ", childnum=" + childnum
				+ ", name=" + name + "]";
	}
	
}
