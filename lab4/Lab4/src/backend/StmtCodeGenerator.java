package backend;

import java.util.HashMap;

import soot.Unit;
import soot.Value;
import soot.jimple.EqExpr;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;
import soot.util.Chain;
import ast.Block;
import ast.BreakStmt;
import ast.ExprStmt;
import ast.IfStmt;
import ast.ReturnStmt;
import ast.Stmt;
import ast.Visitor;
import ast.WhileStmt;

/**
 * This class is in charge of creating Jimple code for a given statement (and its nested
 * statements, if applicable).
 */
public class StmtCodeGenerator extends Visitor<Void> {
	/** Cache Jimple singleton for convenience. */
	private final Jimple j = Jimple.v();

	/** The {@link FunctionCodeGenerator} that created this object. */
	private final FunctionCodeGenerator fcg;

	/** The statement list of the enclosing function body. */
	private final Chain<Unit> units;

	/** A map from while statements to their break target. */
	private final HashMap<WhileStmt, Unit> breakTargets = new HashMap<WhileStmt, Unit>();

	public StmtCodeGenerator(FunctionCodeGenerator fcg) {
		this.fcg = fcg;
		this.units = fcg.getBody().getUnits();
	}

	/** Generates code for an expression statement. */
	@Override
	public Void visitExprStmt(ExprStmt nd) {
		ExprCodeGenerator.generate(nd.getExpr(), fcg);
		return null;
	}

	/** Generates code for a break statement. */
	@Override
	public Void visitBreakStmt(BreakStmt nd) {
		/* TODO: generate code for break statement (hint: use ASTNode.getEnclosingLoop and breakTargets;
		 *       use units.add() to insert the statement into the surrounding method) */
		//break statements are used to break out of loops, therefore there should be a parent while statement

		//use getEnclosingLoop to find the while loop the break statement is in
		WhileStmt parentStatement = nd.getEnclosingLoop();		
		//find the point to break out of loop
		Unit exitPoint = breakTargets.get(parentStatement);
		
		//insert break statement into method?
		units.add(j.newGotoStmt(exitPoint));

		return null;

	}

	/** Generates code for a block of statements. */
	@Override 
	public Void visitBlock(Block nd) {
		for(Stmt stmt : nd.getStmts())
			stmt.accept(this);
		return null;
	}

	/** Generates code for a return statement. */
	@Override
	public Void visitReturnStmt(ReturnStmt nd) {
		Unit stmt;
		if(nd.hasExpr())
			stmt = j.newReturnStmt(ExprCodeGenerator.generate(nd.getExpr(), fcg));
		else
			stmt = j.newReturnVoidStmt();
		units.add(stmt);
		return null;
	}

	/** Generates code for an if statement. */
	@Override
	public Void visitIfStmt(IfStmt nd) {
		Value cond = ExprCodeGenerator.generate(nd.getExpr(), fcg);
		NopStmt join = j.newNopStmt();
		units.add(j.newIfStmt(j.newEqExpr(cond, IntConstant.v(0)), join));
		nd.getThen().accept(this);
		if(nd.hasElse()) {
			NopStmt els = join;
			join = j.newNopStmt();
			units.add(j.newGotoStmt(join));
			units.add(els);
			nd.getElse().accept(this);
		}
		units.add(join);
		return null;
	}

	/** Generates code for a while statement. */
	@Override
	public Void visitWhileStmt(WhileStmt nd) {
		/* TODO: generate code for while statement as discussed in lecture; add the NOP statement you
		 *       generate as the break target to the breakTargets map
		 */

		/*INPUT: while statement
		label0 = new NOP statement
		label1 = new NOP statement
		emit statement label0
		c = generate code for condition (storing in a temporary variable
		if condition is a complex expression)
		emit statement if c == 0 goto label1
		generate code for body
		emit statement goto label0
		emit statement label1*/
		
		NopStmt label0 = j.newNopStmt(); // start of loop || loop back to here
		NopStmt label1 = j.newNopStmt(); //where u break out of loop
		
		//use HashMaps to find the break statement
		//hashmaps uses format <key,value>
		breakTargets.put(nd, label1);// break out of loop and go to desired location
		
		units.add(label0); //go back to start of loop
		
		Value condition = ExprCodeGenerator.generate(nd.getExpr(), fcg); // condition to check for while looping

		// check if c==0
		EqExpr equals = j.newEqExpr(condition, IntConstant.v(0));
		soot.jimple.IfStmt ifCondition = j.newIfStmt(equals, label1); 
		units.add(ifCondition);
		
		//add to statement list
		nd.getBody().accept(this);
		units.add(j.newGotoStmt(label0));
		units.add(label1);
		
		
		
		
		
		return null;
	}
}
