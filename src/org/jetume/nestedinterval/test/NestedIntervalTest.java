package org.jetume.nestedinterval.test;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.jetume.nestedinterval.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

@SuppressWarnings("unused")
public class NestedIntervalTest {
	protected EntityManagerFactory emFactory;
	protected EntityManager em;
	protected JpaEntityManager jem;
	private JpaEntity King;
	private JpaEntity Jones;
	private JpaEntity Blake;
	private JpaEntity Scott;
	private JpaEntity Ford;
	private JpaEntity Adams;
	private JpaEntity Smith;
	private JpaEntity Allen;
	private JpaEntity Ward;
	private JpaEntity Martin;

	@BeforeClass(alwaysRun = true)
	protected void createEntityManagerFactory() {
		try {
			emFactory = Persistence.createEntityManagerFactory("TestPU");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@BeforeMethod(alwaysRun = true)
	protected void createEntityManager() {
		em = emFactory.createEntityManager();
		jem = new JpaEntityManager(em);
	}

	@AfterMethod
	protected void closeEntityManager() {
		if (em != null) {
//			em.getTransaction().begin();
//			em.createQuery("delete from JpaEntity").executeUpdate();
//			em.getTransaction().commit();
			em.close();
		}
		this.jem = null;
	}

	@AfterClass
	protected void closeEntityManagerFactory() {
		if (emFactory != null) {
			emFactory.close();
		}
	}

	protected void constructSampleTree() {
		King = this.jem.createRoot("King");
		Jones = this.jem.addChild(King, "Jones");
		Blake = this.jem.addChild(King, "Blake");
		Scott = this.jem.addChild(Jones, "Scott");
		Ford = this.jem.addChild(Jones, "Ford");
		Adams = this.jem.addChild(Scott, "Adams");
		Smith = this.jem.addChild(Ford, "Smith");
		Allen = this.jem.addChild(Blake, "Allen");
		Ward = this.jem.addChild(Blake, "Ward");
		Martin = this.jem.addChild(Blake, "Martin");
	}

	@Test
	public void addRoot() {
		JpaEntity returnedNode = this.jem.createRoot("King");

		assertEquals(returnedNode.getA11(), 2);
		assertEquals(returnedNode.getA12(), 1);
		assertEquals(returnedNode.getA21(), 1);
		assertEquals(returnedNode.getA22(), 0);
		assertEquals(returnedNode.getName(), "King");
	}

	@Test
	public void addChild() {
		JpaEntity root = this.jem.createRoot("King");
		JpaEntity returnedChild = this.jem.addChild(root, "Jones");
		assertEquals(returnedChild.getA11(), 3);
		assertEquals(returnedChild.getA12(), 2);
		assertEquals(returnedChild.getA21(), 2);
		assertEquals(returnedChild.getA22(), 1);
		assertEquals(returnedChild.getName(), "Jones");
	}

	@Test
	public void getChildrenCount() {
		King = this.jem.createRoot("King");
		Jones = this.jem.addChild(King, "Jones");
		Blake = this.jem.addChild(King, "Blake");
		assertTrue(this.jem.getChildCount(King) == 2);
	}

	@Test
	public void getChildren() {
		constructSampleTree();

		assertEquals(this.jem.getChildCount(King), 2);
		assertEquals(this.jem.getChildCount(Blake), 3);
		assertEquals(this.jem.getChildCount(Jones), 2);
		assertEquals(this.jem.getChildCount(Scott), 1);
		assertEquals(this.jem.getChildCount(Ford), 1);
		assertEquals(this.jem.getChildCount(Adams), 0);
		assertEquals(this.jem.getChildCount(Smith), 0);

		System.out.println(this.jem.getChildren(Blake));
	}

	@Test
	public void getDescendants() {
		constructSampleTree();

		List<JpaEntity> descendants = this.jem.getDescendants(Blake);
		assertTrue(descendants.size() == 3);
		assertTrue(descendants.get(0).getName().equals("Allen"));
		assertTrue(descendants.get(1).getName().equals("Ward"));
		assertTrue(descendants.get(2).getName().equals("Martin"));

		descendants = this.jem.getDescendants(King);
		assertTrue(descendants.size() == 9);
		assertTrue(descendants.get(0).getName().equals("Jones"));
		assertTrue(descendants.get(1).getName().equals("Blake"));
		assertTrue(descendants.get(2).getName().equals("Scott"));
	}

	@Test
	public void getAncestors() {
		constructSampleTree();

		List<JpaEntity> ancestors = this.jem.getAncestors(Ford);
		assertTrue(ancestors.size() == 2);
		assertTrue(ancestors.get(0).getName().equals("King"));
		assertTrue(ancestors.get(1).getName().equals("Jones"));
	}

	@Test
	public void getMaxChildNum() {
		constructSampleTree();

		int count = this.jem.getMaxChildNum(Blake);
		assertEquals(count, 3);
	}

	@Test
	public void moveNodes() {
		constructSampleTree();

		this.jem.moveSubTree(Smith, Scott);
		List<JpaEntity> descendants = this.jem.getDescendants(Scott);
		assertEquals(2, descendants.size());
		this.jem.moveSubTree(Blake, Scott);
		descendants = this.jem.getDescendants(Scott);
		assertEquals(6, descendants.size());

	}

	@Test
	public void deleteSubTree() {
		constructSampleTree();

		this.jem.deleteSubTree(Jones, false);
		List<JpaEntity> descendants = this.jem.getDescendants(King);
		assertEquals(5, descendants.size());

		this.jem.deleteSubTree(Jones, true);
		descendants = this.jem.getDescendants(King);
		assertEquals(4, descendants.size());
	}
}
