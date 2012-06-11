package org.jetume.nestedinterval;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

public class JpaEntityManager {
	protected EntityManager em;

	@Inject
	public JpaEntityManager(EntityManager em) {
		this.em = em;
	}

	public JpaEntity createRoot(String name) {
		JpaEntity root = new JpaEntity();
		UUID uuid = UUID.randomUUID();
		
		root.setName(name);
		root.setA11(2);
		root.setA12(1);
		root.setA21(1);
		root.setA22(0);
		root.setRootid(uuid.toString());
		root.setChildnum(1);

		em.getTransaction().begin();
		em.persist(root);
		em.getTransaction().commit();

		return root;
	}

	public JpaEntity addChild(JpaEntity parent, String childName) {
		int[] parentMatrix = { parent.getA11(), parent.getA12(),
				parent.getA21(), parent.getA22() };
		int parentChildCount = this.getMaxChildNum(parent);

		int childMatrix[] = this.getChildMatrix(parentMatrix, parentChildCount);

		JpaEntity child = new JpaEntity();
		child.setA11(childMatrix[0]);
		child.setA12(childMatrix[1]);
		child.setA21(childMatrix[2]);
		child.setA22(childMatrix[3]);
		child.setChildnum(parentChildCount + 1);
		child.setName(childName);
		child.setRootid(parent.getRootid());

		em.getTransaction().begin();
		em.persist(child);
		em.getTransaction().commit();

		return child;
	}

	public List<JpaEntity> getChildren(JpaEntity parent) {
		Query query = em
				.createQuery(
						"select child from JpaEntity parent, JpaEntity child where parent.a11=child.a12 and parent.a21=child.a22 and parent.rootid=child.rootid and parent.id=:id")
				.setParameter("id", parent.getId());
		@SuppressWarnings("unchecked")
		List<JpaEntity> entries = query.getResultList();
		return entries;
	}

	public List<JpaEntity> getEntry(JpaEntity node) {
		Query query = em
				.createQuery("select e from JpaEntity e where e.id=:id")
				.setParameter("id", node.getId());
		@SuppressWarnings("unchecked")
		List<JpaEntity> entries = query.getResultList();
		return entries;
	}

	public List<JpaEntity> getDescendants(JpaEntity node) {
		Query query = em
				.createQuery(
						"select descendant from JpaEntity ancestor, JpaEntity descendant where (ancestor.a11-ancestor.a12)*(descendant.a21-descendant.a22) <=  (descendant.a11-descendant.a12)*(ancestor.a21-ancestor.a22) and descendant.a11*ancestor.a21 < ancestor.a11*descendant.a21 and ancestor.rootid=descendant.rootid and ancestor.id=:id")
				.setParameter("id", node.getId());
		@SuppressWarnings("unchecked")
		List<JpaEntity> entries = query.getResultList();
		return entries;
	}

	public List<JpaEntity> getAncestors(JpaEntity node) {
		Query query = em
				.createQuery(
						"select ancestor from JpaEntity descendant, JpaEntity ancestor where (ancestor.a11-ancestor.a12)*(descendant.a21-descendant.a22) <=  (descendant.a11-descendant.a12)*(ancestor.a21-ancestor.a22) and descendant.a11*ancestor.a21 < ancestor.a11*descendant.a21 and descendant.rootid=ancestor.rootid and descendant.id=:id")
				.setParameter("id", node.getId());
		@SuppressWarnings("unchecked")
		List<JpaEntity> entries = query.getResultList();
		return entries;
	}

	public void moveSubTree(JpaEntity node, JpaEntity destination) {
		Query query = em
				.createQuery(
						"select descendant from JpaEntity ancestor, JpaEntity descendant where (ancestor.a11-ancestor.a12)*(descendant.a21-descendant.a22) <=  (descendant.a11-descendant.a12)*(ancestor.a21-ancestor.a22) and descendant.a11*ancestor.a21 <= ancestor.a11*descendant.a21 and ancestor.rootid=descendant.rootid and ancestor.id=:id")
				.setParameter("id", node.getId());
		@SuppressWarnings("unchecked")
		List<JpaEntity> entries = query.getResultList();
		// get the next slot under the destination matrix, and get the new
		// matrix [N]
		int currentSlot = this.getMaxChildNum(destination);
		int newSlot = currentSlot + 1;
		int[] destinationMatrix = { destination.getA11(), destination.getA12(),
				destination.getA21(), destination.getA22() };
		int[] newNMatrix = this.getChildMatrix(destinationMatrix, newSlot);
		newNMatrix[1] *= -1;
		newNMatrix[3] *= -1;

		// Get the M matrix and inverse it
		int[] MMatrix = { node.getA11(), -node.getA12(), node.getA21(),
				-node.getA22() };
		int temp;
		temp = MMatrix[3];
		MMatrix[3] = MMatrix[0];
		MMatrix[0] = temp;
		MMatrix[1] *= -1;
		MMatrix[2] *= -1;

		// for each descendant, calculate the new matrix [Z] by multiplying the
		// old matrix [O] with [N] and inverse of node's matrix [M]^-1
		// so [O] = [N]([O] * [M]^-1)
		em.getTransaction().begin();
		for (JpaEntity entry : entries) {
			int[] ZMatrix = new int[4];
			int[] OMatrix = { entry.getA11(), -entry.getA12(), entry.getA21(),
					-entry.getA22() };
			ZMatrix = this.matrixMultiply(newNMatrix,
					this.matrixMultiply(MMatrix, OMatrix));

			ZMatrix[1] *= -1;
			ZMatrix[3] *= -1;

			entry.setA11(ZMatrix[0]);
			entry.setA12(ZMatrix[1]);
			entry.setA21(ZMatrix[2]);
			entry.setA22(ZMatrix[3]);
			
			if(entry.getId() == node.getId()) {
				entry.setChildnum(newSlot);
			} 

			updateNode(entry);
		}
		em.getTransaction().commit();
	}

	public int getChildCount(JpaEntity parent) {
		int count;

		List<JpaEntity> children = this.getChildren(parent);
		count = children.size();

		return count;
	}

	public int getMaxChildNum(JpaEntity parent) {
		int count = 0;
		Query query = em
				.createQuery(
						"select max(child.childnum) from JpaEntity parent, JpaEntity child where parent.a11=child.a12 and parent.a21=child.a22 and parent.rootid=child.rootid and parent.id=:id")
				.setParameter("id", parent.getId());
		Integer result = (Integer) query.getSingleResult();
		if (result != null) {
			count = result;
		}
		return count;
	}
	
	public void deleteSubTree(JpaEntity parentNode, boolean inclusive) {
		List<JpaEntity> descendants = this.getDescendants(parentNode);
		String s = "DELETE from JpaEntity where id=:id";
		em.getTransaction().begin();
		for(JpaEntity descendant : descendants) {
			Query q = em.createQuery(s).setParameter("id", descendant.getId());
			q.executeUpdate();
		}
		
		if(inclusive) {
			Query q = em.createQuery(s).setParameter("id", parentNode.getId());
			q.executeUpdate();
		}
		
		em.getTransaction().commit();
	}

	private void updateNode(JpaEntity update) {
		//System.out.println(update);
		String s = "UPDATE JpaEntity set a11=:a11, a12=:a12, a21=:a21, a22=:a22 where id=:id";
		Query q = em.createQuery(s).setParameter("a11", update.getA11())
				.setParameter("a12", update.getA12())
				.setParameter("a21", update.getA21())
				.setParameter("a22", update.getA22())
				.setParameter("id", update.getId());

		q.executeUpdate();
	}

	private int[] getChildMatrix(int[] parentMatrix, int childCount) {
		int[] childMatrix = new int[4];
		int[] childAtomicMatrix = { childCount + 2, -1, 1, 0 };

		parentMatrix[1] = -(parentMatrix[1]);
		parentMatrix[3] = -(parentMatrix[3]);

		childMatrix[0] = parentMatrix[0] * childAtomicMatrix[0]
				+ parentMatrix[1] * childAtomicMatrix[2];
		childMatrix[1] = -(parentMatrix[0] * childAtomicMatrix[1] + parentMatrix[1]
				* childAtomicMatrix[3]);
		childMatrix[2] = parentMatrix[2] * childAtomicMatrix[0]
				+ parentMatrix[3] * childAtomicMatrix[2];
		childMatrix[3] = -(parentMatrix[2] * childAtomicMatrix[1] + parentMatrix[3]
				* childAtomicMatrix[3]);

		return childMatrix;
	}

	private int[] matrixMultiply(int[] a, int[] b) {
		//System.out.println("a = " + a[0] + "," + a[1] + "," + a[2] + "," + a[3]);
		//System.out.println("b = " + b[0] + "," + b[1] + "," + b[2] + "," + b[3]);
		int c[] = new int[4];
		c[0] = a[0] * b[0] + a[1] * b[2];
		c[1] = a[0] * b[1] + a[1] * b[3];
		c[2] = a[2] * b[0] + a[3] * b[2];
		c[3] = a[2] * b[1] + a[3] * b[3];
		
		//System.out.println("c = " + c[0] + "," + c[1] + "," + c[2] + "," + c[3]);

		return c;
	}
}
