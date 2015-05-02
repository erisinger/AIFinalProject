import java.util.*;
class GeneticSubsumption {
	
	private ArrayList<GSAgent> agents;
	private GSEnvironmentNode[][] grid;
	
	public GeneticSubsumption(GSEnvironmentNode[][] g, int numAgents){
		int count = numAgents;
		agents = new ArrayList<GSAgent>();
		grid = g;
		
		for (int i = 0; i < grid.length && count > 0; i++) {
			for (int j = 0; j < grid[0].length && count > 0; j++) {
				agents.add(new GSAgent(8));
			}
		}
		
		//TO DO: assign agents to nodes
		
	}
	
	public void run(int cyc){
		System.out.println("run");
		
		int CYCLES = cyc;
		int PROGENITORS = 0;
		
		//run for CYCLES generations
		for (int k = 0; k < CYCLES; k++) {
//			System.out.println("cycle " + k);
			System.out.println(toString());
			
			//run until almost all agents are dead, then cross the most successful (longevity-wise)
			while (agentsRemaining() > PROGENITORS) {
//				System.out.println("agents left: " + agentsRemaining());
				
				//for each node in the grid, check the node for agents
				for (int i = 0; i < grid.length; i++) {
					for (int j = 0; j < grid[0].length; j++) {
						GSEnvironmentNode n = grid[i][j];
						for (GSAgent a : n.agents) {
							
							//tell agent its location
							n.agents.remove(a);
							a.act(n, grid);
							
							//assign to new location
							grid[a.node.i][a.node.j].agents.add(a);
						}
					}
				}
			}
			
			//cross agents -- REPLACE WITH BETTER SELECTION ALGORITHM
			for (int i = 1; i < agents.size(); i++) {
				for (GSAgent c : agents.get(i).crossWith(agents.get(i - 1))) {
					agents.add(c);
				}
			}
			//start again...
		}
	}
	
	private int agentsRemaining(){
		int count = 0;
		for (GSAgent a : agents) {
			if (a.health > 0) {
				count++;
			}
		}
		return count;
	}
	
	public String toString(){
		String str = "";
		for (int i = 0; i < grid.length; i++) {
			for (int j = 0; j < grid[0].length; j++) {
				str += grid[i][j].agents.size();
			}
			str += "\n";
		}
		str += "\n";
		
		return str;
	}
	
	
	/* AGENT CLASS */
	
	public class GSAgent{
		
		//gene weights
//		private enum Weight {
		private int	WANDER = 0;				//probability of undirected movement
		private int	DIRECTED_SEARCH = 1;	//probability of directed movement
		private int	LAZINESS = 2;			//probability of no movement
		private int	ENDURANCE = 3;			//ability to go without rest
		private int	SENSE_OF_SMELL = 4;		//probability of detecting food in neighboring squares
		private int	APPETITE = 5;			//probability of moving toward food once detected
		private int	METABOLISM = 6;			//frequency of eating required to maintain health
		private int	RECOVERY_RATE = 7;		//amount of rest required to recover
//		}
		
		//value to maximize
		public double health = 1;
		
		//other health-related stats
		public long lifeSpan = 0;
		
		//rest
		private int cyclesSinceRest = 0;
		private int consecutiveRestCycles = 0;
		private double fatigue = 0;
		
		//food
		private int cyclesSinceEating = 0;
		private double hunger = 0;
		
		//directional stats
		private int xHeading = 0;
		private int yHeading = 0;
//		private int consecutiveDirectionalCycles = 0;

		
		//chromosome
		public ArrayList<GSAGene> genes = new ArrayList<GSAGene>();
		
		//current location
		GSEnvironmentNode node;
		
		//constructor for pre-made chromosome
		public GSAgent(ArrayList<GSAGene> g){
			genes = g;
		}
		
		//constructor for default-valued genes
		public GSAgent(int numGenes){
			for (int i = 0; i < numGenes; i++) {
				genes.add(new GSAGene());
			}
		}
		
		//constructor for empty agent
		public GSAgent(){
			 
		}
		
		public void act(GSEnvironmentNode n, GSEnvironmentNode[][] grid){
			System.out.println("act");
			
			//if agent is dead, do nothing
			if (health <= 0) {
				return;
			}
			
			//agent is not dead
			int nextX = 0;
			int nextY = 0;
			
			//get current location
			node = n;
			
			/* update stats */
			
			//update fatigue
			if (xHeading == 0 && yHeading == 0) {
				//agent didn't move last cycle
				cyclesSinceRest = 0;
				fatigue -= genes.get(RECOVERY_RATE).weight * consecutiveRestCycles;
			}
			else {
				cyclesSinceRest++;
				fatigue += (1 - genes.get(ENDURANCE).weight) * cyclesSinceRest;
			}
			
			//update hunger: hunger += cycles since eating scaled by metabolism
			cyclesSinceEating++;
			hunger += cyclesSinceEating * genes.get(METABOLISM).weight;
			
			//update health
			double healthMultiplier = 0.01;
			health -= healthMultiplier * (hunger + fatigue);
			
			//make a choice based on stats, location, nearby objects, etc., and behavioral weights
			
			//food detected nearby?
			int smellRadius = 5;
			boolean foodInSquare = false;
			GSEnvironmentNode nodeWithFood = null;
			
			if (n.hasFood) {
				if (genes.get(APPETITE).weight > Math.random()) {
					n.hasFood = false;
					cyclesSinceEating = 0;
					hunger -= (1 - genes.get(METABOLISM).weight);
				}
			}
			else {
				for (int i = -smellRadius; i <= smellRadius; i++) {
					for (int j = -smellRadius; j <= smellRadius; j++) {
						if (n.i + i >= 0 && n.i + i < n.height && n.j + j >= 0 && n.j + j < n.width) {
							if (grid[i][j].hasFood) {
								nodeWithFood = grid[i][j];
							}
						}
					}
				}				
			}

			//to eat or not to eat?
			if (nodeWithFood != null) {
				if (genes.get(APPETITE).weight > Math.random()) {
					//move toward food -- TO DO
				}
			}
			
//			//still moving in the same direction?
//			if (nextX == xHeading && nextY == yHeading) {
//				if (xHeading != 0 && yHeading != 0) {
//					consecutiveDirectionalCycles++;
//				}
//				else {
//					consecutiveDirectionalCycles = 0;
//				}
//			}
			
			//if agent is still alive, move and increment lifespan
			if (health > 0) {
				move();
				lifeSpan++;				
			}

		}
		
		private void move(){
			int nextX = node.j;
			int nextY = node.i;
			
			if (Math.random() > 0.5) {
				xHeading = randomDirection();
				yHeading = randomDirection();	
			}
			
			//next i
			if (node.i + yHeading >= 0 && node.i + yHeading < grid.length) {
				nextY = node.i + nextY;
			}
			
			//next j
			if (node.j + xHeading >= 0 && node.j + xHeading < grid[0].length) {
				nextX = node.j + xHeading;
			}
			
			node = grid[nextY][nextX];
			System.out.println("moved to " + nextY + ", " + nextX);
		}
		
		private int randomDirection(){
			int direction = Math.random() > 0.5 ? 1 : 0;
			return Math.random() > 0.5 ? direction : -direction;
		}
		
		public ArrayList<GSAgent> crossWith(GSAgent a){
			//children
			GSAgent c1 = new GSAgent();
			GSAgent c2 = new GSAgent();
			
			//crossover
			for (int i = 0; i < genes.size(); i++) {
				if (Math.random() > 0.5) {
					c1.genes.add(genes.get(i));
					c2.genes.add(a.genes.get(i));
				}
				else {
					c2.genes.add(genes.get(i));
					c1.genes.add(a.genes.get(i));
				}
			}
			
			//mutation -- TO DO
			
			
			//add offspring to population
			ArrayList<GSAgent> children = new ArrayList<GSAgent>();
			children.add(c1);
			children.add(c2);
			
			return children;
		}
		
		public void randomize(){
			for (GSAGene g : genes) {
				g.randomize();
			}
		}	
		
		public String toString(){
			String str = "health: " + health + "\n";
			for (GSAGene g : genes) {
				str += g.name + ": " + g.weight + "\n";
			}	
			return str;
		}	
		
	}
	
	public static class GSAGene{
		public String name = "";
		public double weight = 0.5;
		
		//assigned values
		public GSAGene(String n, int w){
			name = n;
			weight = w;
		}
		
		//default values
		public GSAGene(){
			
		}
		
		public void randomize(){
			weight = Math.random();
		}
	}
	
	public static class GSEnvironmentNode{
		public boolean hasFood = false;
		public ArrayList<GSAgent> agents = new ArrayList<GSAgent>();
		public int i;
		public int j;
		public int height, width;
		
		public GSEnvironmentNode(int m, int n, int h, int w){
			i = m;
			j = n;
			height = h;
			width = w;
		}
		
	}
	
	public static void main(String[] args) {
		int h = 100, w = 100;
		
		GSEnvironmentNode[][] grid = new GSEnvironmentNode[h][w];
		for (int i = 0; i < h; i++) {
			for (int j = 0; j < w; j++) {
				GSEnvironmentNode n = new GSEnvironmentNode(i, j, h, w);
				if (Math.random() > 0.9) {
					n.hasFood = true;
				}
				grid[i][j] = n;
			}
		}
		
		GeneticSubsumption gs = new GeneticSubsumption(grid, 10);
		gs.run(10);
		
	}
}